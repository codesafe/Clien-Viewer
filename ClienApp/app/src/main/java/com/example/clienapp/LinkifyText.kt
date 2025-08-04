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
    
    // 텍스트를 URL과 일반 텍스트로 분리
    val parts = mutableListOf<TextPart>()
    var lastIndex = 0
    
    urlPattern.findAll(text).forEach { matchResult ->
        val url = matchResult.value
        val startIndex = matchResult.range.first
        val endIndex = matchResult.range.last + 1
        
        // URL 이전의 일반 텍스트 추가
        if (startIndex > lastIndex) {
            parts.add(TextPart.Text(text.substring(lastIndex, startIndex)))
        }
        
        // URL 추가
        parts.add(TextPart.Url(url))
        
        lastIndex = endIndex
    }
    
    // 마지막 남은 텍스트 추가
    if (lastIndex < text.length) {
        parts.add(TextPart.Text(text.substring(lastIndex)))
    }
    
    // 텍스트와 미리보기를 순서대로 표시
    Column(modifier = modifier) {
        parts.forEachIndexed { index, part ->
            when (part) {
                is TextPart.Text -> {
                    if (part.content.isNotEmpty()) {
                        androidx.compose.material3.Text(
                            text = part.content,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = fontSize.sp,
                                lineHeight = lineHeight.sp
                            )
                        )
                    }
                }
                is TextPart.Url -> {
                    // 클릭 가능한 URL 텍스트
                    ClickableText(
                        text = buildAnnotatedString {
                            pushStyle(
                                SpanStyle(
                                    color = MaterialTheme.colorScheme.primary,
                                    textDecoration = TextDecoration.Underline
                                )
                            )
                            append(part.url)
                            pop()
                        },
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = fontSize.sp,
                            lineHeight = lineHeight.sp
                        ),
                        onClick = {
                            if (isYouTubeUrl(part.url)) {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(part.url))
                                context.startActivity(intent)
                            } else {
                                val intent = WebViewActivity.createIntent(context, part.url)
                                context.startActivity(intent)
                            }
                        }
                    )
                    
                    // URL 미리보기 표시 (바로 아래)
                    Spacer(modifier = Modifier.height(4.dp))
                    if (isYouTubeUrl(part.url)) {
                        YouTubePreview(url = part.url)
                    }
//                    else if (shouldShowLinkPreview(part.url)) {
//                        LinkPreview(url = part.url)
//                    }
                    
                    // 다음 요소와의 간격
                    if (index < parts.size - 1) {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

// 텍스트 부분을 나타내는 sealed class
sealed class TextPart {
    data class Text(val content: String) : TextPart()
    data class Url(val url: String) : TextPart()
}

fun isYouTubeUrl(url: String): Boolean {
    return url.contains("youtube.com/watch") || url.contains("youtu.be/")
}

fun shouldShowLinkPreview(url: String): Boolean {
    // 미리보기를 지원할 사이트들 (성능을 위해 제한)
    val supportedSites = listOf(
        "naver.com", "daum.net", "google.com", "github.com",
        "stackoverflow.com", "wikipedia.org", "reddit.com",
        "twitter.com", "facebook.com", "instagram.com",
        "news.mt.co.kr", "chosun.com", "donga.com", "joongang.co.kr"
    )
    
    return supportedSites.any { site -> url.contains(site, ignoreCase = true) }
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