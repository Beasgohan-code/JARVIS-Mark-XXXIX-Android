package com.jarvis.mark39.service

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Lightweight web search via DuckDuckGo Instant Answer API (no key required).
 * Falls back to a plain text summary when structured fields are empty.
 */
@Singleton
class WebSearchService @Inject constructor() {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    suspend fun search(query: String): String = withContext(Dispatchers.IO) {
        try {
            val encoded = URLEncoder.encode(query, "UTF-8")
            val url = "https://api.duckduckgo.com/?q=$encoded&format=json&no_html=1&skip_disambig=1"
            val request = Request.Builder().url(url).header("User-Agent", "JARVIS-Mark39/1.0").build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext "Search failed: HTTP ${response.code}"
                val body = response.body?.string() ?: return@withContext "Empty search response"
                parseDdg(body, query)
            }
        } catch (e: Exception) {
            "Search error: ${e.message}"
        }
    }

    private fun parseDdg(json: String, query: String): String {
        val obj = JSONObject(json)
        val abstract = obj.optString("AbstractText").orEmpty()
        val abstractSource = obj.optString("AbstractSource").orEmpty()
        val answer = obj.optString("Answer").orEmpty()
        val definition = obj.optString("Definition").orEmpty()

        val related = mutableListOf<String>()
        val topics = obj.optJSONArray("RelatedTopics")
        if (topics != null) {
            for (i in 0 until minOf(topics.length(), 5)) {
                val t = topics.optJSONObject(i) ?: continue
                val text = t.optString("Text")
                if (text.isNotBlank()) related.add("• $text")
            }
        }

        val sb = StringBuilder()
        sb.appendLine("Search results for: \"$query\"")
        if (answer.isNotBlank()) sb.appendLine("Answer: $answer")
        if (abstract.isNotBlank()) {
            sb.appendLine("Summary: $abstract")
            if (abstractSource.isNotBlank()) sb.appendLine("Source: $abstractSource")
        }
        if (definition.isNotBlank()) sb.appendLine("Definition: $definition")
        if (related.isNotEmpty()) {
            sb.appendLine("Related:")
            related.forEach { sb.appendLine(it) }
        }
        if (sb.lines().size <= 2) {
            return "No instant answer for \"$query\". Try a more specific query, or open a browser search."
        }
        return sb.toString().trim()
    }
}
