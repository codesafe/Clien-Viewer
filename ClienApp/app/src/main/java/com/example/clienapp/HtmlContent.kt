package com.example.clienapp

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode
import androidx.compose.ui.platform.LocalContext
import coil.ImageLoader
import coil.compose.LocalImageLoader

@Composable
fun HtmlContent(
    htmlContent: String,
    modifier: Modifier = Modifier,
    fontSize: Int = 15,
    lineHeight: Int = 20
) {
    val doc = Jsoup.parse(htmlContent)
    val body = doc.body()
    
    Column(modifier = modifier) {
        body.childNodes().forEach { node ->
            RenderNode(node, fontSize, lineHeight)
        }
    }
}

@Composable
fun RenderNode(
    node: Node,
    fontSize: Int,
    lineHeight: Int
) {
    when (node) {
        is TextNode -> {
            val text = node.text().trim()
            if (text.isNotEmpty()) {
                LinkifyText(
                    text = text,
                    fontSize = fontSize,
                    lineHeight = lineHeight
                )
            }
        }
        is Element -> {
            when (node.tagName()) {
                "img" -> {
                    val src = node.attr("src")
                    val dataSrc = node.attr("data-src") // lazy loading 이미지
                    val actualSrc = when {
                        src.isNotEmpty() && !src.contains("transparent") && !src.contains("blank") -> src
                        dataSrc.isNotEmpty() -> dataSrc
                        else -> ""
                    }
                    
                    if (actualSrc.isNotEmpty()) {
                        val fullImageUrl = when {
                            actualSrc.startsWith("http://") || actualSrc.startsWith("https://") -> actualSrc
                            actualSrc.startsWith("//") -> "https:$actualSrc"
                            actualSrc.startsWith("/") -> "https://m.clien.net$actualSrc"
                            else -> "https://m.clien.net/$actualSrc"
                        }
                        
                        NetworkLogger.logDebug("HtmlContent", "Loading image: $fullImageUrl")
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            AsyncImage(
                                model = fullImageUrl,
                                contentDescription = "글 이미지",
                                modifier = Modifier.fillMaxWidth(),
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
                            RenderNode(childNode, fontSize, lineHeight)
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
                            lineHeight = lineHeight.sp
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
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                        )
                    )
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
                else -> {
                    // 기타 태그는 내부 콘텐츠만 렌더링
                    node.childNodes().forEach { childNode ->
                        RenderNode(childNode, fontSize, lineHeight)
                    }
                }
            }
        }
    }
}