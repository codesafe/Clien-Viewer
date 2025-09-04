package com.example.clienapp

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import coil.compose.AsyncImage
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share

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
    
    // 컨텍스트 메뉴 상태
    var showContextMenu by remember { mutableStateOf(false) }
    var contextMenuUrl by remember { mutableStateOf<String?>(null) }
    var menuPosition by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
    
    // 텍스트와 미리보기를 순서대로 표시
    Box(modifier = modifier) {
        Column {
            SelectionContainer {
                Column {
                    parts.forEach { part ->
                        when (part) {
                            is TextPart.Text -> {
                                if (part.content.isNotEmpty()) {
                                    androidx.compose.material3.Text(
                                        text = part.content,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontSize = fontSize.sp,
                                            lineHeight = lineHeight.sp,
                                            lineBreak = androidx.compose.ui.text.style.LineBreak.Simple
                                        )
                                    )
                                }
                            }
                            is TextPart.Url -> {
                                // URL은 선택 가능한 텍스트로 표시 (링크 기능은 긴 클릭으로만)
                                androidx.compose.material3.Text(
                                    text = part.url,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontSize = fontSize.sp,
                                        lineHeight = lineHeight.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    ),
                                    modifier = Modifier.pointerInput(part.url) {
                                        detectTapGestures(
                                            onTap = {
                                                if (isYouTubeUrl(part.url)) {
                                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(part.url))
                                                    context.startActivity(intent)
                                                } else {
                                                    val intent = WebViewActivity.createIntent(context, part.url)
                                                    context.startActivity(intent)
                                                }
                                            },
                                            onLongPress = { offset ->
                                                contextMenuUrl = part.url
                                                menuPosition = offset
                                                showContextMenu = true
                                            }
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }
            
            // URL 미리보기들은 SelectionContainer 밖에 표시
            parts.forEachIndexed { index, part ->
                if (part is TextPart.Url) {
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
        
        // 컨텍스트 메뉴
        if (showContextMenu && contextMenuUrl != null) {
            UrlContextMenu(
                url = contextMenuUrl!!,
                onDismiss = { 
                    showContextMenu = false
                    contextMenuUrl = null 
                },
                onCopyUrl = { url ->
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText("링크", url)
                    clipboard.setPrimaryClip(clip)
                    Toast.makeText(context, "링크 복사됨", Toast.LENGTH_SHORT).show()
                    showContextMenu = false
                    contextMenuUrl = null
                }
            )
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

@Composable
fun UrlContextMenu(
    url: String,
    onDismiss: () -> Unit,
    onCopyUrl: (String) -> Unit
) {
    Popup(
        onDismissRequest = onDismiss,
        properties = PopupProperties(
            focusable = true,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Card(
            modifier = Modifier.width(150.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(4.dp)
            ) {
                TextButton(
                    onClick = { onCopyUrl(url) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "복사",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "링크 복사",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}

