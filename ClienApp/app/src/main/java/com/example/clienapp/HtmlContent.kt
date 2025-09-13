package com.example.clienapp

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode
import androidx.compose.foundation.clickable

import coil.compose.LocalImageLoader
import coil.ImageLoader
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import coil.request.ImageRequest
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import android.os.Build
import com.example.clienapp.YouTubePreview
import com.example.clienapp.VideoPlayer

@Composable
fun HtmlContent(
    htmlContent: String,
    modifier: Modifier = Modifier,
    fontSize: Int = 15,
    lineHeight: Int = 20,
    onImageClick: ((String) -> Unit)? = null
) {
    val doc = Jsoup.parse(htmlContent)
    val body = doc.body()
    
    SelectionContainer {
        Column(modifier = modifier) {
            body.childNodes().forEach { node ->
                RenderNode(node, fontSize, lineHeight, onImageClick)
            }
        }
    }
}

@Composable
fun RenderNode(
    node: Node,
    fontSize: Int,
    lineHeight: Int,
    onImageClick: ((String) -> Unit)? = null
) {
    when (node) {
        is TextNode -> {
            val text = node.text().trim()
            if (text.isNotEmpty()) {
                // GIF 텍스트는 표시하지 않음
                if (text.equals("GIF", ignoreCase = true)) {
                    NetworkLogger.logDebug("HtmlContent", "Skipping GIF text node: parent=${node.parent()?.toString()}")
                    return
                }
                androidx.compose.material3.Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = fontSize.sp,
                        lineHeight = lineHeight.sp,
                        lineBreak = androidx.compose.ui.text.style.LineBreak.Simple
                    )
                )
            }
        }
        is Element -> {
            when (node.tagName()) {
                "img" -> {
                    val src = node.attr("src")
                    val dataSrc = node.attr("data-src") // lazy loading 이미지
                    val alt = node.attr("alt")
                    val className = node.attr("class")
                    
                    // 모든 img 태그 로깅
                    //NetworkLogger.logDebug("HtmlContent", "IMG tag - src: $src, data-src: $dataSrc, alt: $alt, class: $className")
                    
                    val actualSrc = when {
                        src.isNotEmpty() && !src.contains("transparent") && !src.contains("blank") -> src
                        dataSrc.isNotEmpty() -> dataSrc
                        else -> ""
                    }
                    
                    // alt가 "GIF"인 경우에도 이미지가 있는지 다시 확인
                    if (actualSrc.isEmpty() && alt.equals("GIF", ignoreCase = true)) {
                        // data-original 또는 다른 속성 확인
                        val dataOriginal = node.attr("data-original")
                        val dataLazySrc = node.attr("data-lazy-src")
                        
                        if (dataOriginal.isNotEmpty()) {
                            NetworkLogger.logDebug("HtmlContent", "Found GIF with data-original: $dataOriginal")
                        }
                        if (dataLazySrc.isNotEmpty()) {
                            NetworkLogger.logDebug("HtmlContent", "Found GIF with data-lazy-src: $dataLazySrc")
                        }
                    }
                    
                    if (actualSrc.isNotEmpty()) {
                        val fullImageUrl = when {
                            actualSrc.startsWith("http://") || actualSrc.startsWith("https://") -> actualSrc
                            actualSrc.startsWith("//") -> "https:$actualSrc"
                            actualSrc.startsWith("/") -> "https://m.clien.net$actualSrc"
                            else -> "https://m.clien.net/$actualSrc"
                        }
                        
                        NetworkLogger.logDebug("HtmlContent", "Loading image: $fullImageUrl")
                        
                        // GIF 이미지 추가 로깅
                        if (fullImageUrl.contains(".gif", ignoreCase = true)) {
                            NetworkLogger.logDebug("HtmlContent", "Loading GIF image: $fullImageUrl")
                            NetworkLogger.logDebug("HtmlContent", "IMG tag attributes - src: $src, data-src: $dataSrc")
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.White)
                                .padding(vertical = 4.dp)
                                .let { modifier ->
                                    if (onImageClick != null) {
                                        modifier.clickable { onImageClick(fullImageUrl) }
                                    } else {
                                        modifier
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            val context = LocalContext.current
                            val configuration = LocalConfiguration.current
                            val screenWidthDp = configuration.screenWidthDp.dp
                            
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(fullImageUrl)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "글 이미지",
                                modifier = Modifier
                                    .widthIn(max = screenWidthDp - 16.dp) // 화면 너비를 넘지 않도록
                                    .fillMaxWidth(1.5f), // 현재 크기의 1.5배
                                contentScale = ContentScale.FillWidth,
                                onError = { error ->
                                    NetworkLogger.logError("HtmlContent", "Image load failed: $fullImageUrl", error.result.throwable)
                                },
                                onSuccess = {
                                    NetworkLogger.logDebug("HtmlContent", "Image loaded successfully: $fullImageUrl")
                                }
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
                "p", "div" -> {
                    Column {
                        node.childNodes().forEach { childNode ->
                            RenderNode(childNode, fontSize, lineHeight, onImageClick)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
                "br" -> {
                    Spacer(modifier = Modifier.height(4.dp))
                }
                "strong", "b" -> {
                    Text(
                        text = node.text(),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = fontSize.sp,
                            lineHeight = lineHeight.sp,
                            lineBreak = androidx.compose.ui.text.style.LineBreak.Simple
                        ),
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    )
                }
                "em", "i" -> {
                    Text(
                        text = node.text(),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = fontSize.sp,
                            lineHeight = lineHeight.sp,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                            lineBreak = androidx.compose.ui.text.style.LineBreak.Simple
                        )
                    )
                }
                "video" -> {
                    // 비디오 태그 처리
                    val src = node.attr("src")
                    val poster = node.attr("poster") // 썸네일 이미지
                    
                    // source 태그에서 비디오 URL 찾기
                    val videoUrl = if (src.isNotEmpty()) {
                        src
                    } else {
                        node.select("source").firstOrNull()?.attr("src") ?: ""
                    }
                    
                    if (videoUrl.isNotEmpty()) {
                        val fullVideoUrl = when {
                            videoUrl.startsWith("http://") || videoUrl.startsWith("https://") -> videoUrl
                            videoUrl.startsWith("//") -> "https:$videoUrl"
                            videoUrl.startsWith("/") -> "https://m.clien.net$videoUrl"
                            else -> "https://m.clien.net/$videoUrl"
                        }
                        
                        NetworkLogger.logDebug("HtmlContent", "Found video: $fullVideoUrl, poster: $poster")
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        VideoPlayer(
                            videoUrl = fullVideoUrl,
                            posterUrl = poster.takeIf { it.isNotEmpty() }?.let { posterUrl ->
                                when {
                                    posterUrl.startsWith("http://") || posterUrl.startsWith("https://") -> posterUrl
                                    posterUrl.startsWith("//") -> "https:$posterUrl"
                                    posterUrl.startsWith("/") -> "https://m.clien.net$posterUrl"
                                    else -> "https://m.clien.net/$posterUrl"
                                }
                            }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
                "iframe" -> {
                    // YouTube iframe 처리
                    val src = node.attr("src")
                    if (src.contains("youtube.com/embed/")) {
                        val videoId = src.substringAfter("youtube.com/embed/").substringBefore("?")
                        if (videoId.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            YouTubePreview(url = "https://www.youtube.com/watch?v=$videoId")
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
                "a" -> {
                    // 동영상 파일 링크 확인
                    val href = node.attr("href")
                    if (href.endsWith(".mp4", ignoreCase = true) || 
                        href.endsWith(".webm", ignoreCase = true) ||
                        href.endsWith(".mov", ignoreCase = true) ||
                        href.endsWith(".avi", ignoreCase = true)) {
                        
                        val fullVideoUrl = when {
                            href.startsWith("http://") || href.startsWith("https://") -> href
                            href.startsWith("//") -> "https:$href"
                            href.startsWith("/") -> "https://m.clien.net$href"
                            else -> "https://m.clien.net/$href"
                        }
                        
                        //NetworkLogger.logDebug("HtmlContent", "Found video link: $fullVideoUrl")
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        VideoPlayer(videoUrl = fullVideoUrl)
                        Spacer(modifier = Modifier.height(8.dp))
                    } else {
                        // 일반 링크
                        val linkText = node.text()
                        val context = LocalContext.current
                        Text(
                            text = linkText,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = fontSize.sp,
                                lineHeight = lineHeight.sp,
                                color = MaterialTheme.colorScheme.primary,
                                textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline
                            ),
                            modifier = Modifier.clickable {
                                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(href))
                                context.startActivity(intent)
                            }
                        )
                    }
                }
                else -> {
                    // 기타 태그는 내부 콘텐츠만 렌더링
                    node.childNodes().forEach { childNode ->
                        RenderNode(childNode, fontSize, lineHeight, onImageClick)
                    }
                }
            }
        }
    }
}