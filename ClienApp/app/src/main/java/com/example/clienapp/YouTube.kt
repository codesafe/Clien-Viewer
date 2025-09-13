package com.example.clienapp

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView

@Composable
fun YouTubePreview(url: String) {
    val videoId = extractYouTubeVideoId(url) ?: return

    Column {
        AndroidView(factory = {
            YouTubePlayerView(it).apply {
                addYouTubePlayerListener(object : AbstractYouTubePlayerListener() {
                    override fun onReady(youTubePlayer: YouTubePlayer) {
                        youTubePlayer.cueVideo(videoId, 0f)
                    }
                })
            }
        }, modifier = Modifier
            .fillMaxWidth()
            .height(200.dp))

        Text(
            text = url,
            modifier = Modifier.padding(top = 8.dp),
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray
        )
    }
}

fun isYouTubeUrl(url: String): Boolean {
    return url.contains("youtube.com/watch") || url.contains("youtu.be/")
}

fun extractYouTubeVideoId(url: String): String? {
    return when {
        url.contains("youtube.com/watch") -> {
            val uri = Uri.parse(url)
            uri.getQueryParameter("v")
        }
        url.contains("youtu.be/") -> {
            url.substringAfter("youtu.be/").substringBefore("?")
        }
        else -> null
    }
}
