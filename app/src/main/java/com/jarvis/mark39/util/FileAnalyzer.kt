package com.jarvis.mark39.util

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import com.jarvis.mark39.ai.GeminiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Full ContentResolver-based file analysis with Gemini multimodal.
 * Supports: images, text/code, PDF (metadata + note), small binary samples.
 */
@Singleton
class FileAnalyzer @Inject constructor(
    private val contentResolver: ContentResolver
) {
    data class FileMeta(
        val name: String,
        val mime: String,
        val size: Long
    )

    suspend fun analyze(
        uri: Uri,
        gemini: GeminiClient,
        userHint: String = ""
    ): String = withContext(Dispatchers.IO) {
        val meta = readMeta(uri)
        val mime = meta.mime

        when {
            mime.startsWith("image/") -> analyzeImage(uri, meta, gemini, userHint)
            mime.startsWith("text/") || mime in TEXT_MIMES -> analyzeText(uri, meta, gemini, userHint)
            mime == "application/pdf" -> analyzePdfPlaceholder(uri, meta, gemini, userHint)
            mime.startsWith("audio/") -> "Audio file \"${meta.name}\" (${formatSize(meta.size)}). " +
                "Transcribe with a speech API or share a text transcript for analysis."
            mime.startsWith("video/") -> analyzeVideoFirstFrame(uri, meta, gemini, userHint)
            else -> analyzeGeneric(uri, meta, gemini, userHint)
        }
    }

    private fun readMeta(uri: Uri): FileMeta {
        var name = uri.lastPathSegment ?: "file"
        var size = -1L
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (cursor.moveToFirst()) {
                if (nameIdx >= 0) name = cursor.getString(nameIdx) ?: name
                if (sizeIdx >= 0) size = cursor.getLong(sizeIdx)
            }
        }
        val mime = contentResolver.getType(uri) ?: "application/octet-stream"
        if (size < 0) {
            size = contentResolver.openAssetFileDescriptor(uri, "r")?.use { it.length } ?: -1
        }
        return FileMeta(name, mime, size)
    }

    private suspend fun analyzeImage(
        uri: Uri,
        meta: FileMeta,
        gemini: GeminiClient,
        hint: String
    ): String {
        val bitmap = decodeBitmap(uri) ?: return "Could not decode image ${meta.name}"
        val prompt = buildString {
            append("Analyze this image file named \"${meta.name}\" (${formatSize(meta.size)}).\n")
            append("Describe scene, objects, text (OCR), and suggest useful actions.\n")
            if (hint.isNotBlank()) append("User request: $hint")
        }
        return gemini.analyzeImage(bitmap, prompt)
    }

    private suspend fun analyzeText(
        uri: Uri,
        meta: FileMeta,
        gemini: GeminiClient,
        hint: String
    ): String {
        val text = contentResolver.openInputStream(uri)?.use { stream ->
            BufferedReader(InputStreamReader(stream)).readText()
        } ?: return "Could not read text file ${meta.name}"

        val truncated = if (text.length > 6_000) text.take(6_000) + "\n…[truncated at 6k chars]" else text
        val prompt = buildString {
            append("Analyze text file \"${meta.name}\" (${formatSize(meta.size)}, ${text.length} chars).\n")
            if (hint.isNotBlank()) append("User request: $hint\n")
            append("Summarize in clear sections. Extract key points. Keep under 400 words. If truncated file, note that..\n")
            append("If code: describe structure, main bugs/risks, how to run.\n---\n")
            append(truncated)
        }
        return gemini.sendMessage(prompt)
    }

    private suspend fun analyzePdfPlaceholder(
        uri: Uri,
        meta: FileMeta,
        gemini: GeminiClient,
        hint: String
    ): String {
        // Lightweight: send metadata + optional first bytes as context.
        // Full text extraction would need PdfBox / Android PdfRenderer bitmaps.
        val pagesHint = try {
            val pfd = contentResolver.openFileDescriptor(uri, "r")
            if (pfd != null) {
                val renderer = android.graphics.pdf.PdfRenderer(pfd)
                val count = renderer.pageCount
                val bitmaps = mutableListOf<Bitmap>()
                val maxPages = minOf(count, 3)
                for (i in 0 until maxPages) {
                    val page = renderer.openPage(i)
                    val bmp = Bitmap.createBitmap(
                        page.width.coerceAtMost(1024),
                        (page.height * (page.width.coerceAtMost(1024).toFloat() / page.width)).toInt(),
                        Bitmap.Config.ARGB_8888
                    )
                    page.render(bmp, null, null, android.graphics.pdf.PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    page.close()
                    bitmaps.add(bmp)
                }
                renderer.close()
                pfd.close()
                if (bitmaps.isNotEmpty()) {
                    val prompt = buildString {
                        append("These are the first $maxPages page(s) of PDF \"${meta.name}\" ")
                        append("(${formatSize(meta.size)}, $count total pages).\n")
                        if (hint.isNotBlank()) append("User request: $hint\n")
                        append("OCR/extract key text and summarize.")
                    }
                    return gemini.analyzeImages(bitmaps, prompt)
                }
                "PDF has $count pages."
            } else "Could not open PDF."
        } catch (e: Exception) {
            "PDF open note: ${e.message}"
        }
        return gemini.sendMessage(
            "PDF file \"${meta.name}\" (${formatSize(meta.size)}). $pagesHint. " +
                "User: ${hint.ifBlank { "Summarize if possible" }}"
        )
    }

    private suspend fun analyzeVideoFirstFrame(
        uri: Uri,
        meta: FileMeta,
        gemini: GeminiClient,
        hint: String
    ): String {
        return try {
            val retriever = android.media.MediaMetadataRetriever()
            contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                retriever.setDataSource(pfd.fileDescriptor)
            }
            val frame = retriever.getFrameAtTime(0)
            retriever.release()
            if (frame != null) {
                gemini.analyzeImage(
                    frame,
                    "This is the first frame of video \"${meta.name}\" (${formatSize(meta.size)}). " +
                        "Describe it. ${hint}"
                )
            } else {
                "Video \"${meta.name}\" — could not extract a frame."
            }
        } catch (e: Exception) {
            "Video analysis limited: ${e.message}. File: ${meta.name} (${formatSize(meta.size)})"
        }
    }

    private suspend fun analyzeGeneric(
        uri: Uri,
        meta: FileMeta,
        gemini: GeminiClient,
        hint: String
    ): String {
        val head = try {
            contentResolver.openInputStream(uri)?.use { it.readBytes().take(512) } ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
        val hex = head.joinToString(" ") { "%02X".format(it) }.take(200)
        return gemini.sendMessage(
            "File \"${meta.name}\" type=${meta.mime} size=${formatSize(meta.size)}. " +
                "Header bytes: $hex. ${hint.ifBlank { "What can you infer?" }}"
        )
    }

    private fun decodeBitmap(uri: Uri): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, bounds)
        }
        var sample = 1
        val maxDim = 1280
        while ((bounds.outWidth / sample) > maxDim || (bounds.outHeight / sample) > maxDim) sample *= 2
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        return contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, opts)
        }
    }

    private fun formatSize(bytes: Long): String = when {
        bytes < 0 -> "unknown size"
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "%.1f KB".format(bytes / 1024.0)
        else -> "%.1f MB".format(bytes / (1024.0 * 1024))
    }

    companion object {
        private val TEXT_MIMES = setOf(
            "application/json", "application/xml", "application/javascript",
            "application/x-javascript", "application/typescript",
            "application/csv", "application/sql"
        )
    }
}
