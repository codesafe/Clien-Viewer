package com.example.clienapp

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import android.content.Intent
import android.net.Uri

data class LinkPreviewData(
    val title: String,
    val description: String,
    val imageUrl: String,
    val siteName: String,
    val url: String
)

@Composable
fun LinkPreview(url: String, modifier: Modifier = Modifier) {
    var previewData by remember { mutableStateOf<LinkPreviewData?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var hasError by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    
    LaunchedEffect(url) {
        scope.launch {
            try {
                isLoading = true
                hasError = false
                val data = fetchLinkPreview(url)
                previewData = data
            } catch (e: Exception) {
                NetworkLogger.logError("LinkPreview", "Error fetching preview for $url: ${e.message}", e)
                hasError = true
            } finally {
                isLoading = false
            }
        }
    }
    
    if (isLoading) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .height(80.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            }
        }
    } else if (hasError || previewData == null) {
        // 간단한 링크 표시
        Card(
            modifier = modifier
                .fillMaxWidth()
                .clickable {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    context.startActivity(intent)
                },
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Text(
                    text = "🔗 링크",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = url,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    } else {
        // 풀 미리보기
        Card(
            modifier = modifier
                .fillMaxWidth()
                .clickable {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    context.startActivity(intent)
                },
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                // 이미지 (있는 경우)
                if (previewData!!.imageUrl.isNotEmpty()) {
                    AsyncImage(
                        model = previewData!!.imageUrl,
                        contentDescription = "링크 미리보기 이미지",
                        modifier = Modifier
                            .size(80.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                }
                
                // 텍스트 정보
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    // 사이트 이름
                    if (previewData!!.siteName.isNotEmpty()) {
                        Text(
                            text = previewData!!.siteName,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                    }
                    
                    // 제목
                    Text(
                        text = previewData!!.title.ifEmpty { "제목 없음" },
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    // 설명
                    if (previewData!!.description.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = previewData!!.description,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

suspend fun fetchLinkPreview(url: String): LinkPreviewData = withContext(Dispatchers.IO) {
    try {
        val client = SSLHelper.getUnsafeOkHttpClient()
        val request = okhttp3.Request.Builder()
            .url(url)
            .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 13; SM-G991B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/112.0.0.0 Mobile Safari/537.36")
            .build()
        
        val response = client.newCall(request).execute()
        val html = response.body?.string() ?: ""
        val doc = Jsoup.parse(html, url)
        
        // 메타 태그에서 정보 추출
        val title = doc.select("meta[property=og:title]").attr("content").ifEmpty {
            doc.select("meta[name=twitter:title]").attr("content").ifEmpty {
                doc.title()
            }
        }
        
        val description = doc.select("meta[property=og:description]").attr("content").ifEmpty {
            doc.select("meta[name=twitter:description]").attr("content").ifEmpty {
                doc.select("meta[name=description]").attr("content")
            }
        }
        
        val imageUrl = doc.select("meta[property=og:image]").attr("content").ifEmpty {
            doc.select("meta[name=twitter:image]").attr("content")
        }
        
        val siteName = doc.select("meta[property=og:site_name]").attr("content").ifEmpty {
            Uri.parse(url).host ?: ""
        }
        
        LinkPreviewData(
            title = title.trim(),
            description = description.trim(),
            imageUrl = imageUrl.trim(),
            siteName = siteName.trim(),
            url = url
        )
    } catch (e: Exception) {
        throw e
    }
}