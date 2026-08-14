package com.jarvis.mark39.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import com.jarvis.mark39.domain.model.JarvisUiEvent
import com.jarvis.mark39.ui.viewmodels.JarvisViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ShareReceiverActivity : ComponentActivity() {

    private val viewModel: JarvisViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        when (intent?.action) {
            Intent.ACTION_SEND -> {
                val uri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
                uri?.let {
                    viewModel.onEvent(JarvisUiEvent.ShareFile(it.toString()))
                    Toast.makeText(this, "JARVIS is analyzing your file…", Toast.LENGTH_SHORT).show()
                }
            }
            Intent.ACTION_SEND_MULTIPLE -> {
                val uris = intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)
                uris?.forEach { uri ->
                    viewModel.onEvent(JarvisUiEvent.ShareFile(uri.toString()))
                }
                Toast.makeText(this, "JARVIS received ${uris?.size ?: 0} files", Toast.LENGTH_SHORT).show()
            }
        }
        finish()
    }
}
