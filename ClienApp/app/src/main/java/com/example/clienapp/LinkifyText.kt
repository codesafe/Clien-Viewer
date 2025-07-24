package com.example.clienapp

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale

@Composable
fun LinkifyText(
    text: String,
    modifier: Modifier = Modifier,
    fontSize: Int = 16,
    lineHeight: Int = 24
) {
    val context = LocalContext.current
    
    // URL 패턴 정규식
    val urlPattern = Regex(
        "(https?://[\\w\\-._~:/?#\\[\\]@!$&'()*+,;=]+)"
    )
    
    val annotatedString = buildAnnotatedString {
        var lastIndex = 0
        
        urlPattern.findAll(text).forEach { matchResult ->
            val url = matchResult.value
            val startIndex = matchResult.range.first
            val endIndex = matchResult.range.last + 1
            
            // URL 이전의 일반 텍스트 추가
            if (startIndex > lastIndex) {
                append(text.substring(lastIndex, startIndex))
            }
            
            // URL 추가 with annotation
            pushStringAnnotation(tag = "URL", annotation = url)
            pushStyle(
                SpanStyle(
                    color = MaterialTheme.colorScheme.primary,
                    textDecoration = TextDecoration.Underline
                )
            )
            append(url)
            pop()
            pop()
            
            lastIndex = endIndex
        }
        
        // 마지막 남은 텍스트 추가
        if (lastIndex < text.length) {
            append(text.substring(lastIndex))
        }
    }
    
    Column(modifier = modifier) {
        ClickableText(
            text = annotatedString,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = fontSize.sp,
                lineHeight = lineHeight.sp
            ),
            onClick = { offset ->
                annotatedString.getStringAnnotations(tag = "URL", start = offset, end = offset)
                    .firstOrNull()?.let { annotation ->
                        val url = annotation.item
                        
                        // YouTube URL 체크
                        if (isYouTubeUrl(url)) {
                            // YouTube는 별도 처리 (아래에서 미리보기로 표시됨)
                        } else {
                            // 일반 URL은 브라우저로 열기
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                            context.startActivity(intent)
                        }
                    }
            }
        )
        
        // YouTube 미리보기 표시
        val youtubeUrls = urlPattern.findAll(text)
            .map { it.value }
            .filter { isYouTubeUrl(it) }
            .toList()
        
        youtubeUrls.forEach { youtubeUrl ->
            Spacer(modifier = Modifier.height(8.dp))
            YouTubePreview(url = youtubeUrl)
        }
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

@Composable
fun YouTubePreview(url: String) {
    val videoId = extractYouTubeVideoId(url) ?: return
    val context = LocalContext.current
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .clickable {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/watch?v=$videoId"))
                context.startActivity(intent)
            },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // YouTube 썸네일 이미지
            AsyncImage(
                model = "https://img.youtube.com/vi/$videoId/hqdefault.jpg",
                contentDescription = "YouTube 비디오 썸네일",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            
            // 재생 버튼 오버레이
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f))
            )
            
            // 재생 아이콘
            Icon(
                imageVector = Icons.Filled.PlayArrow,
                contentDescription = "재생",
                modifier = Modifier
                    .size(60.dp)
                    .align(Alignment.Center),
                tint = Color.White
            )
        }
    }
}