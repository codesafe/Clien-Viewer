package com.example.clienapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import coil.ImageLoader
import coil.compose.LocalImageLoader
import androidx.compose.runtime.CompositionLocalProvider
import android.util.Log

class PostDetailActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val postUrl = UrlUtils.decodeUrl(intent.getStringExtra("postUrl") ?: "")
        val postTitle = UrlUtils.decodeUrl(intent.getStringExtra("postTitle") ?: "")

        Log.d("PostDetailActivity", "Received postUrl: $postUrl, postTitle: $postTitle")

        // Coil ImageLoader with unsafe SSL settings
        val imageLoader = ImageLoader.Builder(this)
            .okHttpClient(SSLHelper.getUnsafeOkHttpClient())
            .build()
        coil.Coil.setImageLoader(imageLoader)

        setContent {
            MaterialTheme {
                CompositionLocalProvider(LocalImageLoader provides imageLoader) {
                    PostDetailScreen(postUrl = postUrl, postTitle = postTitle, onBack = { finish() })
                }
            }
        }
    }
}
