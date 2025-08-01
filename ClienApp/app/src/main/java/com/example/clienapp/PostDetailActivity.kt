package com.example.clienapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import coil.ImageLoader
import coil.compose.LocalImageLoader
import coil.decode.GifDecoder
import androidx.compose.runtime.CompositionLocalProvider
import android.util.Log

class PostDetailActivity : ComponentActivity() {
    
    companion object {
        private const val KEY_POST_URL = "key_post_url"
        private const val KEY_POST_TITLE = "key_post_title"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val postUrl = savedInstanceState?.getString(KEY_POST_URL) 
            ?: UrlUtils.decodeUrl(intent.getStringExtra("postUrl") ?: "")
        val postTitle = savedInstanceState?.getString(KEY_POST_TITLE)
            ?: UrlUtils.decodeUrl(intent.getStringExtra("postTitle") ?: "")

        Log.d("PostDetailActivity", "Received postUrl: $postUrl, postTitle: $postTitle")

        // Coil ImageLoader with unsafe SSL settings
        val imageLoader = ImageLoader.Builder(this)
            .components {
                add(GifDecoder.Factory())
            }
            .okHttpClient(SSLHelper.getUnsafeOkHttpClient())
            .build()
        coil.Coil.setImageLoader(imageLoader)

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.White
                ) {
                    CompositionLocalProvider(LocalImageLoader provides imageLoader) {
                        PostDetailScreen(postUrl = postUrl, postTitle = postTitle, onBack = { finish() })
                    }
                }
            }
        }
    }
    
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        intent.getStringExtra("postUrl")?.let { 
            outState.putString(KEY_POST_URL, it)
        }
        intent.getStringExtra("postTitle")?.let { 
            outState.putString(KEY_POST_TITLE, it)
        }
    }
}
