package com.example.clienapp

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed // Add this import
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.compose.ui.Alignment

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import kotlinx.coroutines.launch
import org.jsoup.Jsoup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.util.Log
import okhttp3.Request
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import coil.ImageLoader
import coil.compose.LocalImageLoader
import androidx.compose.runtime.CompositionLocalProvider

data class MenuItem(
    val title: String,
    val url: String,
    val description: String = ""
)

data class PostItem(
    val title: String,
    val url: String,
    val author: String = "",
    val date: String = "",
    val views: String = "",
    val likes: Int = 0
)

data class Comment(
    val author: String,
    val content: String,
    val date: String,
    val isReply: Boolean = false,
    val images: List<String> = emptyList()
)

data class PostDetail(
    val title: String,
    val content: String,
    val htmlContent: String = "",
    val images: List<String> = emptyList(),
    val youtubeVideoIds: List<String> = emptyList(),
    val author: String = "",
    val date: String = "",
    val views: String = "",
    val comments: List<Comment> = emptyList(),
    val sourceUrl: String = "",
    val nextPageUrl: String? = null
)

class ClienRepository {
    private val allowedBoards = listOf(
        "모두의공원",
        //"추천글계시판",
        "아무거나질문",
        "정보와자료",
        "새로운소식",
        "사고팔고",
        "알뜰구매",
        "회원중고장터",
        "강좌/사용기"
    )

    suspend fun fetchMenuItems(): List<MenuItem> = withContext(Dispatchers.IO) {
        // 캐시 확인
        val cached = CacheManager.getCachedMenuItems("main_menu")
        if (cached != null) {
            NetworkLogger.logDebug("ClienApp", "Using cached menu items")
            return@withContext cached
        }
        
        try {
            // 하드코딩된 게시판 목록 (실제 URL 패턴에 맞게 수정 필요)
            val menuItems = listOf(
                MenuItem("모두의공원", "/service/board/park"),
                //MenuItem("추천글계시판", "/service/service/recommend"),
                MenuItem("아무거나질문", "/service/board/kin"),
                MenuItem("정보와자료", "/service/board/lecture"),
                MenuItem("새로운소식", "/service/board/news"),
                MenuItem("사고팔고", "/service/board/sold"),
                MenuItem("알뜰구매", "/service/board/jirum"),
                MenuItem("회원중고장터", "/service/board/sold"),
                MenuItem("강좌/사용기", "/service/board/use")
            )
            
            menuItems.forEach { item ->
                Log.d("ClienApp", "Board: ${item.title} -> ${item.url}")
            }
            
            // 캐시에 저장
            CacheManager.cacheMenuItems("main_menu", menuItems)
            
            menuItems
        } catch (e: Exception) {
            e.printStackTrace()
            NetworkLogger.logError("ClienApp", "Error fetching menu items: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun fetchBoardPosts(boardUrl: String, page: Int = 0, forceRefresh: Boolean = false): List<PostItem> = withContext(Dispatchers.IO) {
        // 페이지 URL 생성 - 첫 페이지는 po 파라미터 없음, 2페이지부터 po=1, 3페이지는 po=2...
        val pageUrl = if (page > 0) {
            if (boardUrl.contains("?")) {
                "$boardUrl&od=T31&category=0&po=$page"
            } else {
                "$boardUrl?&od=T31&category=0&po=$page"
            }
        } else {
            boardUrl
        }
        
        // 캐시 확인 (forceRefresh가 true이면 캐시 무시)
        if (!forceRefresh) {
            val cached = CacheManager.getCachedPosts(pageUrl)
            if (cached != null) {
                Log.d("ClienApp", "Using cached posts for $pageUrl")
                return@withContext cached
            }
        }
        
        try {
            val fullUrl = if (pageUrl.startsWith("http")) pageUrl else "https://m.clien.net$pageUrl"
            NetworkLogger.logDebug("ClienApp", "Fetching posts from: $fullUrl (page: $page)")
            
            val client = SSLHelper.getUnsafeOkHttpClient()
            val request = Request.Builder()
                .url(fullUrl)
                .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 13; SM-G991B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/112.0.0.0 Mobile Safari/537.36")
                .build()
            
            val response = client.newCall(request).execute()
            val html = response.body?.string() ?: ""
            val doc = Jsoup.parse(html, fullUrl)

            val posts = mutableListOf<PostItem>()
            
            // 게시글 목록 파싱
            doc.select(".list_item, .post-list li, article, .board-list li").forEach { element ->
                // 제목 파싱 - span[data-role="list-title-text"] 찾기
                val titleSpan = element.select("span[data-role='list-title-text']").first()
                var title = titleSpan?.attr("title")?.trim() ?: titleSpan?.text()?.trim() ?: ""
                
                // 제목이 없으면 기존 방식으로 찾기
                if (title.isEmpty()) {
                    val titleElement = element.select("a, .list_title, .title, h3").first()
                    title = titleElement?.text()?.trim() ?: ""
                }
                
                // URL 찾기
                val url = element.select("a").first()?.attr("href") ?: ""
                
                // 공감수 파싱 - 제목 앞의 숫자 패턴 찾기
                var likes = 0
                val likesPattern = Regex("^(\\d+)\\s+(.+)")
                val matchResult = likesPattern.find(title)
                if (matchResult != null) {
                    likes = matchResult.groupValues[1].toIntOrNull() ?: 0
                    title = matchResult.groupValues[2].trim()
                }
                
                if (title.isNotEmpty() && url.isNotEmpty()) {
                    posts.add(PostItem(
                        title = title,
                        url = if (url.startsWith("http")) url else "https://m.clien.net$url",
                        author = element.select(".author, .nickname, .writer").text().trim(),
                        date = element.select(".date, .time, .timestamp").text().trim(),
                        views = element.select(".hit, .view, .count").text().trim(),
                        likes = likes
                    ))
                    
                    if (likes > 0) {
                        Log.d("ClienApp", "Post with likes: $likes - $title")
                    }
                }
            }
            
            Log.d("ClienApp", "Fetched ${posts.size} posts from $boardUrl")
            posts.forEach { post ->
                Log.d("ClienApp", "Post: ${post.title}")
            }
            
            val postsWithoutNotice = posts
            
            Log.d("ClienApp", "Posts after removing notices: ${postsWithoutNotice.size}")
            
            // 캐시에 저장
            CacheManager.cachePosts(pageUrl, postsWithoutNotice)
            
            postsWithoutNotice
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("ClienApp", "Error fetching board posts: ${e.message}")
            emptyList()
        }
    }

    suspend fun fetchPostDetail(postUrl: String, forceRefresh: Boolean = false): PostDetail? = withContext(Dispatchers.IO) {
        // 캐시 확인 (forceRefresh가 true이면 캐시 무시)
        if (!forceRefresh) {
            val cached = CacheManager.getCachedPostDetail(postUrl)
            if (cached != null) {
                Log.d("ClienApp", "Using cached post detail for $postUrl")
                return@withContext cached
            }
        }
        
        try {
            val fullUrl = if (postUrl.startsWith("http")) postUrl else "https://m.clien.net$postUrl"
            NetworkLogger.logDebug("ClienApp", "Fetching post detail from: $fullUrl")
            
            val client = SSLHelper.getUnsafeOkHttpClient()
            val request = Request.Builder()
                .url(fullUrl)
                .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 13; SM-G991B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/112.0.0.0 Mobile Safari/537.36")
                .build()
            
            val response = client.newCall(request).execute()
            val html = response.body?.string() ?: ""
            val doc = Jsoup.parse(html, fullUrl)

            // 디버깅을 위한 HTML 구조 로그
            Log.d("ClienApp", "========== POST DETAIL HTML STRUCTURE ==========")
            Log.d("ClienApp", "HTML Title: ${doc.title()}")
            
            // 제목 파싱 - doc.xml 구조에 맞게
            var title = ""
            val titleElement = doc.select(".post_subject span").first()
            if (titleElement != null) {
                title = titleElement.text().trim()
                NetworkLogger.logDebug("ClienApp", "Found title: $title")
            } else {
                // 대체 선택자들
                val titleSelectors = listOf(".post_title", ".post-title", ".title", "h1.title", "h2.title")
                for (selector in titleSelectors) {
                    val element = doc.select(selector).first()
                    if (element != null) {
                        title = element.text().trim()
                        NetworkLogger.logDebug("ClienApp", "Found title with fallback selector '$selector': $title")
                        break
                    }
                }
            }
            
            // 글 내용 파싱 - doc.xml 구조에 맞게
            var contentElement: org.jsoup.nodes.Element? = null
            contentElement = doc.select(".post_content article").first()
            if (contentElement != null) {
                NetworkLogger.logDebug("ClienApp", "Found content with .post_content article")
            } else {
                // 대체 선택자들
                val contentSelectors = listOf(
                    ".post_content", ".post-content", ".content", ".post_article",
                    ".post_view", ".view_content", "article .content", ".memo_content"
                )
                for (selector in contentSelectors) {
                    val element = doc.select(selector).first()
                    if (element != null) {
                        contentElement = element
                        NetworkLogger.logDebug("ClienApp", "Found content with fallback selector '$selector'")
                        break
                    }
                }
            }
            
            val content = contentElement?.text()?.trim() ?: ""
            val htmlContent = contentElement?.html() ?: ""
            
            // post_source의 attached_text 찾기 (출처 링크)
            var sourceUrl = ""
            val postSourceElement = doc.select(".post_source .attached_text").first()
            if (postSourceElement != null) {
                sourceUrl = postSourceElement.text().trim()
                NetworkLogger.logDebug("ClienApp", "Found source URL: $sourceUrl")
            }
            
            // 이미지 추출 (개선된 로직)
            val images = mutableListOf<String>()
            contentElement?.select("img")?.forEach { img ->
                val src = img.attr("src")
                val dataSrc = img.attr("data-src") // lazy loading 이미지
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
                    images.add(fullImageUrl)
                    NetworkLogger.logDebug("ClienApp", "Found image: $fullImageUrl")
                }
            }
            
            // YouTube 영상 추출
            val youtubeVideoIds = mutableListOf<String>()
            
            // iframe에서 YouTube embed URL 찾기
            contentElement?.select("iframe")?.forEach { iframe ->
                val src = iframe.attr("src")
                if (src.contains("youtube.com/embed/")) {
                    val videoId = src.substringAfter("youtube.com/embed/").substringBefore("?")
                    if (videoId.isNotEmpty()) {
                        youtubeVideoIds.add(videoId)
                        Log.d("ClienApp", "Found YouTube video: $videoId")
                    }
                }
            }
            
            // HTML에서 YouTube URL 패턴 찾기 (추가 파싱)
            val youtubePattern = Regex("https?://(?:www\\.)?youtube\\.com/embed/([a-zA-Z0-9_-]+)")
            youtubePattern.findAll(htmlContent).forEach { matchResult ->
                val videoId = matchResult.groupValues[1]
                if (videoId.isNotEmpty() && !youtubeVideoIds.contains(videoId)) {
                    youtubeVideoIds.add(videoId)
                    Log.d("ClienApp", "Found YouTube video from pattern: $videoId")
                }
            }
            
            // 작성자 파싱 - doc.xml 구조에 맞게
            var author = ""
            val authorElement = doc.select(".post_view .post_contact .nickname").first()
            if (authorElement != null) {
                author = authorElement.text().trim()
                NetworkLogger.logDebug("ClienApp", "Found author: $author")
            } else {
                // 대체 선택자들
                val authorSelectors = listOf(".post_author", ".author", ".nickname", ".writer", ".user_info .nickname")
                for (selector in authorSelectors) {
                    val element = doc.select(selector).first()
                    if (element != null) {
                        author = element.text().trim()
                        NetworkLogger.logDebug("ClienApp", "Found author with fallback selector '$selector': $author")
                        break
                    }
                }
            }
            val date = doc.select(".post_date, .date, .time, .post_time").first()?.text()?.trim() ?: ""
            val views = doc.select(".post_view, .hit, .view, .view_count").first()?.text()?.trim() ?: ""
            
            Log.d("ClienApp", "========== PARSED POST DETAIL ==========")
            Log.d("ClienApp", "Title: $title")
            Log.d("ClienApp", "Content length: ${content.length}")
            Log.d("ClienApp", "Images found: ${images.size}")
            Log.d("ClienApp", "Author: $author")
            Log.d("ClienApp", "Date: $date")
            Log.d("ClienApp", "Views: $views")
            
            // 댓글 파싱 - 완전히 재구성
            val comments = mutableListOf<Comment>()
            val processedComments = mutableSetOf<String>() // 중복 방지를 위한 Set
            
            NetworkLogger.logDebug("ClienApp", "========== COMMENT PARSING START ==========")
            
            // 전체 댓글 영역 확인
            val commentArea = doc.select(".post_comment, #comment-div, .comment_area").first()
            if (commentArea != null) {
                NetworkLogger.logDebug("ClienApp", "Found comment area")
                
                // 모든 댓글 관련 요소 찾기 (더 포괄적인 선택자)
                val allCommentElements = commentArea.select(
                    ".comment, .comment_row, " +
                    "[data-role='comment'], [data-role='comment-row'], " +
                    "li:has(.nickname), div:has(.nickname)"
                )
                
                NetworkLogger.logDebug("ClienApp", "Found ${allCommentElements.size} potential comment elements")
                
                allCommentElements.forEach { element ->
                    try {
                        // 대댓글인지 확인
//                        val isReply = element.hasClass("re") ||
//                                     element.hasClass("reply") ||
//                                     element.hasClass("comment_row") ||
//                                     element.parent()?.hasClass("comment") == true

                        val isReply = element.hasClass("comment_row  re") ||
                                    element.hasClass("comment_row by-author re") == true

                        // 작성자 찾기
                        val authorElement = element.select(".nickname").first()
                        val author = authorElement?.text()?.trim() ?: ""
                        
                        if (author.isEmpty()) {
                            NetworkLogger.logDebug("ClienApp", "Skipping element - no author found")
                            return@forEach
                        }
                        
                        // 댓글 내용 찾기 (여러 방법 시도)
                        var content = ""
                        val commentImages = mutableListOf<String>()
                        
                        // 방법 1: .comment_view에서 찾기
                        val commentViewElement = element.select(".comment_view").first()
                        if (commentViewElement != null) {
                            content = cleanCommentContent(commentViewElement.text().trim())
                            
                            // 댓글 내 이미지 추출 (개선된 로직)
                            commentViewElement.select("img").forEach { img ->
                                val src = img.attr("src")
                                val dataSrc = img.attr("data-src")
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
                                    commentImages.add(fullImageUrl)
                                    NetworkLogger.logDebug("ClienApp", "Found comment image: $fullImageUrl")
                                }
                            }
                            
                            NetworkLogger.logDebug("ClienApp", "Found content in .comment_view: ${content.take(50)}")
                        }
                        
                        // 유효한 댓글인지 확인하고 추가
                        if (author.isNotEmpty() && content.isNotEmpty() && isValidCommentContent(content)) {
                            val commentKey = "$author|$content"
                            if (!processedComments.contains(commentKey)) {
                                processedComments.add(commentKey)
                                comments.add(Comment(
                                    author = author,
                                    content = content,
                                    date = "",
                                    isReply = isReply,
                                    images = commentImages
                                ))
                                NetworkLogger.logDebug("ClienApp", "Added comment: $author (reply: $isReply)")
                            }
                        } else {
                            NetworkLogger.logDebug("ClienApp", "Skipped invalid comment - author: '$author', content: '$content'")
                        }
                    } catch (e: Exception) {
                        NetworkLogger.logError("ClienApp", "Error parsing comment: ${e.message}", e)
                    }
                }
            } else {
                NetworkLogger.logDebug("ClienApp", "No comment area found")
            }
            
            NetworkLogger.logDebug("ClienApp", "Found ${comments.size} comments total")
            
            val nextPageUrl = doc.select("a.button-next").first()?.attr("href")

            val postDetail = PostDetail(
                title = title,
                content = content,
                htmlContent = htmlContent,
                images = images,
                youtubeVideoIds = youtubeVideoIds,
                author = author,
                date = date,
                views = views,
                comments = comments,
                sourceUrl = sourceUrl,
                nextPageUrl = nextPageUrl
            )
            
            // 캐시에 저장
            CacheManager.cachePostDetail(postUrl, postDetail)
            
            postDetail
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("ClienApp", "Error fetching post detail: ${e.message}")
            null
        }
    }
}

// 댓글 내용에서 불필요한 메타데이터 제거
fun cleanCommentContent(content: String): String {
    var cleaned = content.trim()
    
    // 끝에 붙은 메타데이터 패턴 제거
    val metadataPatterns = listOf(
        Regex("\\s+(date|time|timestamp|메모|memo)\\s*:?\\s*$", RegexOption.IGNORE_CASE),
        Regex("\\s+\\d{4}[-./]\\d{1,2}[-./]\\d{1,2}\\s*$"), // 날짜 패턴
        Regex("\\s+\\d{1,2}:\\d{2}(:\\d{2})?\\s*$"), // 시간 패턴
        Regex("\\s+(수정|삭제|신고|답글|댓글)\\s*$"), // 액션 버튼 텍스트
        Regex("\\s+\\[.*?\\]\\s*$"), // [메모] 같은 대괄호 텍스트
        Regex("\\s+date\\s*$", RegexOption.IGNORE_CASE)
    )
    
    for (pattern in metadataPatterns) {
        cleaned = pattern.replace(cleaned, "")
    }
    
    // 시작 부분의 메타데이터도 제거
    val startPatterns = listOf(
        Regex("^(date|time|timestamp|메모|memo)\\s*:?\\s*", RegexOption.IGNORE_CASE),
        Regex("^\\[.*?\\]\\s+") // [메모] 같은 대괄호 텍스트
    )
    
    for (pattern in startPatterns) {
        cleaned = pattern.replace(cleaned, "")
    }
    
    return cleaned.trim()
}

// 유효한 댓글 내용인지 검증하는 함수
fun isValidCommentContent(content: String): Boolean {
    val trimmedContent = content.trim()
    
    // 빈 문자열 또는 공백만 있는 경우
    if (trimmedContent.isEmpty()) return false
    
    // "1", "0", "true", "false" 같은 단순한 값들 제외
    if (trimmedContent in listOf("1", "0", "true", "false", "null", "undefined")) return false
    
    // 숫자만 있는 경우 (ID나 플래그 값일 가능성)
    if (trimmedContent.matches(Regex("^\\d+$"))) return false
    
    // 너무 짧은 내용 (1-2글자) 제외 (단, 이모지나 특수문자는 허용)
    if (trimmedContent.length <= 2 && trimmedContent.matches(Regex("^[a-zA-Z0-9]+$"))) return false
    
    // HTML 태그만 있는 경우
    if (trimmedContent.matches(Regex("^<[^>]*>$"))) return false
    
    // 메타데이터만 있는 경우
    if (trimmedContent.matches(Regex("^(date|time|timestamp|메모|memo|수정|삭제|신고|답글|댓글)$", RegexOption.IGNORE_CASE))) return false
    
    return true
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClienApp() {
    val navController = rememberNavController()
    val context = LocalContext.current
    
    // Coil ImageLoader with unsafe SSL settings
    val imageLoader = ImageLoader.Builder(context)
        .okHttpClient(SSLHelper.getUnsafeOkHttpClient())
        .build()
    
    CompositionLocalProvider(LocalImageLoader provides imageLoader) {
        NavHost(navController = navController, startDestination = "boardList") {
        composable(
            "boardList",
            enterTransition = { slideInHorizontally(animationSpec = tween(0)) },
            exitTransition = { slideOutHorizontally(animationSpec = tween(0)) },
            popEnterTransition = { slideInHorizontally(animationSpec = tween(0)) },
            popExitTransition = { slideOutHorizontally(animationSpec = tween(0)) }
        ) {
            BoardListScreen()
        }
    }
    }
}

///////////////////////////////////////////////////////////////////////////////////////////////////


// 첫화면
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BoardListScreen() {
    var menuItems by remember { mutableStateOf<List<MenuItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val repository = remember { ClienRepository() }
    val scope = rememberCoroutineScope()
    
    LaunchedEffect(Unit) {
        scope.launch {
            isLoading = true
            menuItems = repository.fetchMenuItems()
            isLoading = false
        }
    }
    
    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Clien 게시판") }
                )
                Divider(thickness = 2.dp, color = Color.Black)
            }
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.align(androidx.compose.ui.Alignment.Center)
                )
            }
        } else {
            LazyColumn(
                // 첫화면의 배경
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                
                contentPadding = PaddingValues(8.dp),  // 16dp에서 8dp로 축소
                verticalArrangement = Arrangement.spacedBy(4.dp)  // 8dp에서 4dp로 축소
            ) {
                itemsIndexed(menuItems) { index, item ->
                    val context = LocalContext.current
                    MenuItemCard(item) {
                        val encodedUrl = UrlUtils.encodeUrl(item.url)
                        val encodedTitle = UrlUtils.encodeUrl(item.title)
                        val intent = Intent(context, BoardDetailActivity::class.java).apply {
                            putExtra("boardUrl", encodedUrl)
                            putExtra("boardTitle", encodedTitle)
                        }
                        context.startActivity(intent)
                    }
                    if (index < menuItems.size - 1) {
                        Divider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                        )
                    }
                }
            }
        }
    }
}





@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostDetailScreen(postUrl: String, postTitle: String, onBack: () -> Unit) {
    var postDetail by remember { mutableStateOf<PostDetail?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isRefreshing by remember { mutableStateOf(false) }
    val repository = remember { ClienRepository() }
    val scope = rememberCoroutineScope()
    val swipeRefreshState = rememberSwipeRefreshState(isRefreshing)

    // 초기 로드
    LaunchedEffect(postUrl) {
        Log.d("PostDetailScreen", "Loading post detail for URL: $postUrl")
        scope.launch {
            isLoading = true
            postDetail = repository.fetchPostDetail(postUrl)
            isLoading = false

            if (postDetail != null) {
                Log.d("PostDetailScreen", "Post detail loaded: ${postDetail!!.title}")
                Log.d("PostDetailScreen", "Content length: ${postDetail!!.content.length}, HTML content length: ${postDetail!!.htmlContent.length}")
                VisitedPostsManager.markAsVisited(postUrl)
            } else {
                Log.e("PostDetailScreen", "Failed to load post detail for URL: $postUrl")
            }
        }
    }

    // Pull to refresh
    LaunchedEffect(isRefreshing) {
        if (isRefreshing) {
            scope.launch {
                postDetail = repository.fetchPostDetail(postUrl, forceRefresh = true)
                isRefreshing = false
            }
        }
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text(postTitle, maxLines = 1) },
                    navigationIcon = {
                        IconButton(onClick = { onBack() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "뒤로가기")
                        }
                    }
                )
                Divider(thickness = 2.dp, color = Color.Black)
            }
        },
        modifier = Modifier.smartSwipeBack {
            onBack()
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.align(androidx.compose.ui.Alignment.Center)
                )
            }
        } else if (postDetail != null) {
            SwipeRefresh(
                state = swipeRefreshState,
                onRefresh = { isRefreshing = true },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            if (VisitedPostsManager.isVisited(postUrl))
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                            else
                                MaterialTheme.colorScheme.background
                        )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(8.dp)
                    ) {
                // 0. 출처 링크 미리보기 (있는 경우 최상단에 표시)
                if (postDetail!!.sourceUrl.isNotEmpty()) {
                    LinkPreview(url = postDetail!!.sourceUrl)
                    Spacer(modifier = Modifier.height(12.dp))
                }
                
                // 1. 제목
                Text(
                    text = postDetail!!.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // 2. Author : date
                if (postDetail!!.author.isNotEmpty() || postDetail!!.date.isNotEmpty()) {
                    Text(
                        text = buildAnnotatedString {
                            withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)) {
                                append(postDetail!!.author)
                            }
                            append(" : ${postDetail!!.date}")
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // 3. views
                if (postDetail!!.views.isNotEmpty()) {
                    Text(
                        text = "조회 ${postDetail!!.views}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // 내용 전 가로 라인
                Divider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    thickness = 1.dp
                )
                
                // 4. 내용 (HTML 내용이 있으면 HTML 렌더링, 없으면 일반 텍스트)
                if (postDetail!!.htmlContent.isNotEmpty()) {
                    HtmlContent(
                        htmlContent = postDetail!!.htmlContent,
                        fontSize = 15,
                        lineHeight = 20
                    )
                } else {
                    LinkifyText(
                        text = postDetail!!.content,
                        fontSize = 15,
                        lineHeight = 20
                    )
                }
                
                
                // 6. 다음 페이지 버튼
                postDetail?.nextPageUrl?.let {
                    Spacer(modifier = Modifier.height(16.dp))
                    var isLoadingNextPage by remember { mutableStateOf(false) }

                    Button(
                        onClick = {
                            if (!isLoadingNextPage) {
                                scope.launch {
                                    isLoadingNextPage = true
                                    val nextPageDetail = repository.fetchPostDetail(it)
                                    if (nextPageDetail != null) {
                                        postDetail = postDetail?.copy(
                                            htmlContent = postDetail!!.htmlContent + "<br><br>" + nextPageDetail.htmlContent,
                                            comments = postDetail!!.comments + nextPageDetail.comments,
                                            nextPageUrl = nextPageDetail.nextPageUrl
                                        )
                                    }
                                    isLoadingNextPage = false
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoadingNextPage
                    ) {
                        if (isLoadingNextPage) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        } else {
                            Text("다음 페이지")
                        }
                    }
                }

                // 내용 끝 가로 라인
                Divider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    thickness = 1.dp
                )
                
                // 5. 댓글
                if (postDetail!!.comments.isNotEmpty()) {
                    
                    Text(
                        text = "댓글 (${postDetail!!.comments.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    postDetail!!.comments.forEachIndexed { index, comment ->
                        CommentItem(comment)
                        if (index < postDetail!!.comments.size - 1) {
                            Divider(
                                modifier = Modifier.padding(vertical = 4.dp),
                                thickness = 0.5.dp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                            )
                        }
                    }
                }
                    }
                }
            }
        }
    }
}

@Composable
fun CommentItem(comment: Comment) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = if (comment.isReply) 24.dp else 0.dp,
                top = 4.dp,
                bottom = 4.dp
            )
    ) {
        // 1. ID
        Text(
            text = comment.author,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        
        // 2. 내용
        LinkifyText(
            text = comment.content,
            fontSize = 14,
            lineHeight = 20,
            modifier = Modifier.padding(top = 2.dp)
        )
        
        // 3. 이미지 (있는 경우)
        if (comment.images.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            comment.images.forEach { imageUrl ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = "댓글 이미지",
                        modifier = Modifier.fillMaxWidth(),
                        contentScale = ContentScale.FillWidth,
                        onError = { error ->
                            NetworkLogger.logError("CommentImage", "Failed to load: $imageUrl", error.result.throwable)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun MenuItemCard(item: MenuItem, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .background(Color.White) // 흰색 배경
            .padding(12.dp),
    ) {
        Text(
            text = item.title,
            style = MaterialTheme.typography.titleLarge,
            color = Color.Black, // 검은색 글씨
            fontSize = 20.sp
        )
        if (item.description.isNotEmpty()) {
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = item.description,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Black, // 검은색 글씨
                fontSize = 15.sp
            )
        }
    }
}

@Composable
fun PostItemCard(post: PostItem, isVisited: Boolean, onClick: () -> Unit) {
    val backgroundColor = if (isVisited) Color(0xFFF0F0F0) else Color.White
    val titleColor = if (isVisited) Color.DarkGray else Color.Black
    val metaColor = if (isVisited) Color.Gray else Color.DarkGray

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .background(
                color = backgroundColor
            )
            .padding(8.dp) // 더 줄임
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // 공감수 표시
            if (post.likes > 0) {
                Box(
                    modifier = Modifier
                        .background(
                            color = MaterialTheme.colorScheme.error,
                            shape = RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = post.likes.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onError,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // 제목
            Text(
                text = post.title,
                style = MaterialTheme.typography.bodySmall,
                fontSize = 14.sp,
                maxLines = 2,
                color = titleColor,
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (post.author.isNotEmpty()) {
                Text(
                    text = post.author,
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary // 파란색으로 변경
                )
            }
            if (post.date.isNotEmpty()) {
                Text(
                    text = post.date,
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 12.sp,
                    color = metaColor
                )
            }
        }
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 방문한 글 관리자 초기화
        VisitedPostsManager.init(this)
        
        // Coil 기본 이미지 로더 설정
        val imageLoader = ImageLoader.Builder(this)
            .okHttpClient(SSLHelper.getUnsafeOkHttpClient())
            .build()
        coil.Coil.setImageLoader(imageLoader)
        
        setContent {
            MaterialTheme {
                ClienApp()
            }
        }
    }
}