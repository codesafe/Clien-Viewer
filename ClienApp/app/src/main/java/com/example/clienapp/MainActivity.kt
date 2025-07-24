package com.example.clienapp

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import kotlinx.coroutines.flow.distinctUntilChanged
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import kotlinx.coroutines.launch
import org.jsoup.Jsoup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import android.webkit.WebView
import android.webkit.WebViewClient

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
    val isReply: Boolean = false
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
    val comments: List<Comment> = emptyList()
)

class ClienRepository {
    private val allowedBoards = listOf(
        "모두의공원",
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
            Log.d("ClienApp", "Using cached menu items")
            return@withContext cached
        }
        
        try {
            // 하드코딩된 게시판 목록 (실제 URL 패턴에 맞게 수정 필요)
            val menuItems = listOf(
                MenuItem("모두의공원", "/service/board/park"),
                MenuItem("아무거나질문", "/service/board/kin"),
                MenuItem("정보와자료", "/service/board/lecture"),
                MenuItem("새로운소식", "/service/board/news"),
                MenuItem("사고팔고", "/service/board/sold"),
                MenuItem("알뜰구매", "/service/board/jirum"),
                MenuItem("회원중고장터", "/service/board/used"),
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
            Log.e("ClienApp", "Error fetching menu items: ${e.message}")
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
            Log.d("ClienApp", "Fetching posts from: $fullUrl (page: $page)")
            
            val doc = Jsoup.connect(fullUrl)
                .userAgent("Mozilla/5.0 (Linux; Android 13; SM-G991B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/112.0.0.0 Mobile Safari/537.36")
                .timeout(10000)
                .ignoreHttpErrors(true)
                .ignoreContentType(true)
                .sslSocketFactory(SSLHelper.getUnsafeOkHttpClient())
                .get()

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
            
            // 첫 페이지에서만 처음 2개 게시글(공지사항) 제외
            val postsWithoutNotice = if (page == 0 && posts.size > 2) {
                posts.drop(2)
            } else {
                posts
            }
            
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
            Log.d("ClienApp", "Fetching post detail from: $fullUrl")
            
            val doc = Jsoup.connect(fullUrl)
                .userAgent("Mozilla/5.0 (Linux; Android 13; SM-G991B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/112.0.0.0 Mobile Safari/537.36")
                .timeout(10000)
                .ignoreHttpErrors(true)
                .ignoreContentType(true)
                .sslSocketFactory(SSLHelper.getUnsafeOkHttpClient())
                .get()

            // 디버깅을 위한 HTML 구조 로그
            Log.d("ClienApp", "========== POST DETAIL HTML STRUCTURE ==========")
            Log.d("ClienApp", "HTML Title: ${doc.title()}")
            
            // 가능한 모든 제목 선택자 시도
            val titleSelectors = listOf(
                ".post_title", ".post-title", ".title", "h1.title", "h2.title", 
                ".view_title", ".subject", ".post_subject"
            )
            var title = ""
            for (selector in titleSelectors) {
                val element = doc.select(selector).first()
                if (element != null) {
                    title = element.text().trim()
                    Log.d("ClienApp", "Found title with selector '$selector': $title")
                    break
                }
            }
            
            // 가능한 모든 내용 선택자 시도
            val contentSelectors = listOf(
                ".post_content", ".post-content", ".content", ".post_article",
                ".post_view", ".view_content", "article .content", ".memo_content"
            )
            var contentElement: org.jsoup.nodes.Element? = null
            for (selector in contentSelectors) {
                val element = doc.select(selector).first()
                if (element != null) {
                    contentElement = element
                    Log.d("ClienApp", "Found content with selector '$selector'")
                    break
                }
            }
            
            val content = contentElement?.text()?.trim() ?: ""
            val htmlContent = contentElement?.html() ?: ""
            
            // 이미지 추출
            val images = mutableListOf<String>()
            contentElement?.select("img")?.forEach { img ->
                val src = img.attr("src")
                if (src.isNotEmpty()) {
                    val fullImageUrl = if (src.startsWith("http")) src else "https://m.clien.net$src"
                    images.add(fullImageUrl)
                    Log.d("ClienApp", "Found image: $fullImageUrl")
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
            
            // 작성자, 날짜, 조회수 파싱
            val author = doc.select(".post_author, .author, .nickname, .writer, .user_info .nickname").first()?.text()?.trim() ?: ""
            val date = doc.select(".post_date, .date, .time, .post_time").first()?.text()?.trim() ?: ""
            val views = doc.select(".post_view, .hit, .view, .view_count").first()?.text()?.trim() ?: ""
            
            Log.d("ClienApp", "========== PARSED POST DETAIL ==========")
            Log.d("ClienApp", "Title: $title")
            Log.d("ClienApp", "Content length: ${content.length}")
            Log.d("ClienApp", "Images found: ${images.size}")
            Log.d("ClienApp", "Author: $author")
            Log.d("ClienApp", "Date: $date")
            Log.d("ClienApp", "Views: $views")
            
            // 댓글 파싱
            val comments = mutableListOf<Comment>()
            val processedComments = mutableSetOf<String>() // 중복 방지를 위한 Set
            
            // 디버깅을 위해 전체 HTML 구조 확인
            Log.d("ClienApp", "========== COMMENT AREA DEBUG ==========")
            val allNicknames = doc.select("span.nickname")
            Log.d("ClienApp", "Total nicknames found in document: ${allNicknames.size}")
            
            // 모든 댓글 관련 요소 찾기 - 더 넓은 범위로
            val commentElements = doc.select("li:has(span.nickname), div:has(span.nickname), tr:has(span.nickname)")
            Log.d("ClienApp", "Found ${commentElements.size} potential comment elements")
            
            // 가장 작은 댓글 컨테이너만 선택 (중첩된 요소 제외)
            val filteredElements = commentElements.filter { element ->
                // 현재 요소 내부에 다른 nickname을 가진 자식 댓글 요소가 있는지 확인
                val innerNicknames = element.select("span.nickname")
                //val hasNestedComments = innerNicknames.size > 1
                val hasNestedComments = false

                    if (hasNestedComments) {
                    // 중첩된 댓글이 있으면 이 요소는 컨테이너일 가능성이 높음
                    Log.d("ClienApp", "Skipping container element with ${innerNicknames.size} nicknames")
                    false
                } else {
                    true
                }
            }
            
            Log.d("ClienApp", "Filtered to ${filteredElements.size} comment elements")
            
            filteredElements.forEach { commentElement ->
                try {
                    // 작성자 찾기 - span.nickname
                    val authorElement = commentElement.select("span.nickname").first()
                    val commentAuthor = authorElement?.text()?.trim() ?: ""
                    
                    Log.d("ClienApp", "Processing comment by: $commentAuthor")
                    
                    // 댓글 내용 찾기 - comment_content 내부의 input value 또는 comment_view의 텍스트
                    var commentText = ""
                    
                    // 먼저 input의 value 속성에서 찾기
                    val inputElements = commentElement.select("input[type='hidden']")
                    Log.d("ClienApp", "Found ${inputElements.size} hidden inputs")
                    
                    inputElements.forEach { input ->
                        val value = input.attr("value").trim()
                        if (value.isNotEmpty()) {
                            commentText = value
                            Log.d("ClienApp", "Found comment in input value: $commentText")
                            return@forEach
                        }
                    }
                    
                    // input에서 못 찾았으면 comment_view의 텍스트에서 찾기
                    if (commentText.isEmpty()) {
                        val viewElement = commentElement.select(".comment_view").first()
                        if (viewElement != null) {
                            // input 태그를 제외한 텍스트만 가져오기
                            val tempElement = viewElement.clone()
                            tempElement.select("input").remove()
                            commentText = tempElement.text().trim()
                            Log.d("ClienApp", "Found comment in view text: $commentText")
                        }
                    }
                    
                    // 그래도 없으면 comment_content 전체에서 찾기
                    if (commentText.isEmpty()) {
                        val contentElement = commentElement.select(".comment_content").first()
                        if (contentElement != null) {
                            val tempElement = contentElement.clone()
                            tempElement.select("input").remove()
                            commentText = tempElement.text().trim()
                        }
                    }
                    
                    // 날짜 찾기
                    val dateElement = commentElement.select(".timestamp, .time, .date").first()
                    val commentDate = dateElement?.text()?.trim() ?: ""
                    
                    // 대댓글 여부 확인
                    val isReply = commentElement.hasClass("re_comment") || 
                                 commentElement.parent()?.hasClass("comment_re") == true ||
                                 commentElement.select(".re_comment").isNotEmpty()
                    
                    if (commentAuthor.isNotEmpty() && commentText.isNotEmpty()) {
                        // 중복 체크를 위한 고유 키 생성
                        val commentKey = "$commentAuthor|$commentText"
                        
                        if (!processedComments.contains(commentKey)) {
                            processedComments.add(commentKey)
                            comments.add(Comment(
                                author = commentAuthor,
                                content = commentText,
                                date = commentDate,
                                isReply = isReply
                            ))
                            Log.d("ClienApp", "Added comment: $commentAuthor - $commentText (Reply: $isReply)")
                        } else {
                            Log.d("ClienApp", "Skipped duplicate comment: $commentAuthor - $commentText")
                        }
                    }
                } catch (e: Exception) {
                    Log.e("ClienApp", "Error parsing comment: ${e.message}")
                }
            }
            
            Log.d("ClienApp", "Found ${comments.size} comments")
            
            val postDetail = PostDetail(
                title = title,
                content = content,
                htmlContent = htmlContent,
                images = images,
                youtubeVideoIds = youtubeVideoIds,
                author = author,
                date = date,
                views = views,
                comments = comments
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClienApp() {
    val navController = rememberNavController()
    
    NavHost(navController = navController, startDestination = "boardList") {
        composable(
            "boardList",
            enterTransition = { slideInHorizontally(animationSpec = tween(0)) },
            exitTransition = { slideOutHorizontally(animationSpec = tween(0)) },
            popEnterTransition = { slideInHorizontally(animationSpec = tween(0)) },
            popExitTransition = { slideOutHorizontally(animationSpec = tween(0)) }
        ) {
            BoardListScreen(navController)
        }
        composable(
            "boardDetail/{boardUrl}/{boardTitle}",
            enterTransition = { slideInHorizontally(animationSpec = tween(0)) { it } },
            exitTransition = { slideOutHorizontally(animationSpec = tween(0)) { -it } },
            popEnterTransition = { slideInHorizontally(animationSpec = tween(0)) { -it } },
            popExitTransition = { slideOutHorizontally(animationSpec = tween(0)) { it } }
        ) { backStackEntry ->
            val boardUrl = UrlUtils.decodeUrl(backStackEntry.arguments?.getString("boardUrl") ?: "")
            val boardTitle = UrlUtils.decodeUrl(backStackEntry.arguments?.getString("boardTitle") ?: "")
            BoardDetailScreen(navController, boardUrl, boardTitle)
        }
        composable(
            "postDetail/{postUrl}/{postTitle}",
            enterTransition = { slideInHorizontally(animationSpec = tween(0)) { it } },
            exitTransition = { slideOutHorizontally(animationSpec = tween(0)) { -it } },
            popEnterTransition = { slideInHorizontally(animationSpec = tween(0)) { -it } },
            popExitTransition = { slideOutHorizontally(animationSpec = tween(0)) { it } }
        ) { backStackEntry ->
            val postUrl = UrlUtils.decodeUrl(backStackEntry.arguments?.getString("postUrl") ?: "")
            val postTitle = UrlUtils.decodeUrl(backStackEntry.arguments?.getString("postTitle") ?: "")
            PostDetailScreen(navController, postUrl, postTitle)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BoardListScreen(navController: NavController) {
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
            TopAppBar(
                title = { Text("Clien 게시판") }
            )
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
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(8.dp),  // 16dp에서 8dp로 축소
                verticalArrangement = Arrangement.spacedBy(4.dp)  // 8dp에서 4dp로 축소
            ) {
                items(menuItems) { item ->
                    MenuItemCard(item) {
                        val encodedUrl = UrlUtils.encodeUrl(item.url)
                        val encodedTitle = UrlUtils.encodeUrl(item.title)
                        navController.navigate("boardDetail/$encodedUrl/$encodedTitle")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BoardDetailScreen(navController: NavController, boardUrl: String, boardTitle: String) {
    var posts by remember { mutableStateOf<List<PostItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isRefreshing by remember { mutableStateOf(false) }
    var isLoadingMore by remember { mutableStateOf(false) }
    var currentPage by remember { mutableStateOf(0) }
    var hasMorePages by remember { mutableStateOf(true) }
    val repository = remember { ClienRepository() }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val swipeRefreshState = rememberSwipeRefreshState(isRefreshing)
    
    // 초기 로드
    LaunchedEffect(boardUrl) {
        scope.launch {
            isLoading = true
            posts = repository.fetchBoardPosts(boardUrl, page = 0, forceRefresh = true)
            currentPage = 0
            hasMorePages = posts.size >= 20
            isLoading = false
        }
    }
    
    // Pull to refresh
    LaunchedEffect(isRefreshing) {
        if (isRefreshing) {
            scope.launch {
                val newPosts = repository.fetchBoardPosts(boardUrl, page = 0, forceRefresh = true)
                posts = newPosts
                currentPage = 0
                hasMorePages = newPosts.size >= 20
                isRefreshing = false
            }
        }
    }
    
    // 무한 스크롤을 위한 마지막 아이템 감지
    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo }
            .collect { layoutInfo ->
                val lastVisibleItemIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                val totalItemsCount = layoutInfo.totalItemsCount
                
                // 마지막에서 3번째 아이템이 보이고, 더 로드할 수 있고, 현재 로딩 중이 아닐 때
                if (lastVisibleItemIndex >= totalItemsCount - 3 && 
                    hasMorePages && 
                    !isLoadingMore && 
                    !isLoading && 
                    totalItemsCount > 0) {
                    
                    isLoadingMore = true
                    scope.launch {
                        val nextPage = currentPage + 1
                        Log.d("ClienApp", "Loading page $nextPage (URL param po=$nextPage)")
                        val morePosts = repository.fetchBoardPosts(boardUrl, page = nextPage)
                        
                        if (morePosts.isNotEmpty()) {
                            posts = posts + morePosts
                            currentPage = nextPage
                            hasMorePages = morePosts.size >= 20
                            Log.d("ClienApp", "Loaded ${morePosts.size} posts from page $nextPage, total: ${posts.size}")
                        } else {
                            hasMorePages = false
                            Log.d("ClienApp", "No more posts found on page $nextPage")
                        }
                        isLoadingMore = false
                    }
                }
            }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(boardTitle) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "뒤로가기")
                    }
                }
            )
        },
        modifier = Modifier.swipeBackGesture {
            navController.popBackStack()
        }
    ) { paddingValues ->
        SwipeRefresh(
            state = swipeRefreshState,
            onRefresh = { isRefreshing = true },
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize()
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(posts) { post ->
                        PostItemCard(post) {
                            val encodedUrl = UrlUtils.encodeUrl(post.url)
                            val encodedTitle = UrlUtils.encodeUrl(post.title)
                            navController.navigate("postDetail/$encodedUrl/$encodedTitle")
                        }
                    }
                    
                    // 로딩 인디케이터
                    if (isLoadingMore) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.align(androidx.compose.ui.Alignment.Center)
                                )
                            }
                        }
                    }
                    
                    // 더 이상 글이 없을 때 메시지
                    if (!hasMorePages && posts.isNotEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                Text(
                                    text = "더 이상 글이 없습니다",
                                    modifier = Modifier.align(androidx.compose.ui.Alignment.Center),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostDetailScreen(navController: NavController, postUrl: String, postTitle: String) {
    var postDetail by remember { mutableStateOf<PostDetail?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isRefreshing by remember { mutableStateOf(false) }
    val repository = remember { ClienRepository() }
    val scope = rememberCoroutineScope()
    val swipeRefreshState = rememberSwipeRefreshState(isRefreshing)
    
    // 초기 로드
    LaunchedEffect(postUrl) {
        scope.launch {
            isLoading = true
            postDetail = repository.fetchPostDetail(postUrl)
            isLoading = false
            
            // 글을 성공적으로 로드했으면 방문 기록에 추가
            if (postDetail != null) {
                VisitedPostsManager.markAsVisited(postUrl)
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
            TopAppBar(
                title = { Text(postTitle, maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "뒤로가기")
                    }
                }
            )
        },
        modifier = Modifier.swipeBackGesture {
            navController.popBackStack()
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
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp)
                    ) {
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
                        text = "${postDetail!!.author} : ${postDetail!!.date}",
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
                
                // 4. 내용
                LinkifyText(
                    text = postDetail!!.content,
                    fontSize = 16,
                    lineHeight = 24
                )
                
                // YouTube 영상 표시
                if (postDetail!!.youtubeVideoIds.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    postDetail!!.youtubeVideoIds.forEach { videoId ->
                        val context = LocalContext.current
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .padding(vertical = 8.dp)
                                .clickable {
                                    // YouTube 앱 또는 브라우저로 열기
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
                                        .size(72.dp)
                                        .align(Alignment.Center),
                                    tint = Color.White
                                )
                                
                                // YouTube 로고
                                Text(
                                    text = "YouTube",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .background(
                                            color = Color.Red,
                                            shape = RoundedCornerShape(4.dp)
                                        )
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                        .align(Alignment.TopEnd)
                                        .padding(8.dp)
                                )
                            }
                        }
                    }
                }
                
                // 이미지 표시
                if (postDetail!!.images.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    postDetail!!.images.forEach { imageUrl ->
                        AsyncImage(
                            model = imageUrl,
                            contentDescription = "게시글 이미지",
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            contentScale = ContentScale.FillWidth
                        )
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
    }
}

@Composable
fun MenuItemCard(item: MenuItem, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)  // 16dp에서 8dp로 축소
        ) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleLarge,  // titleMedium에서 titleLarge로 증가
                fontSize = 18.sp  // 14sp에서 18sp로 (약 30% 증가)
            )
            if (item.description.isNotEmpty()) {
                Spacer(modifier = Modifier.height(3.dp))  // 2dp에서 3dp로 증가
                Text(
                    text = item.description,
                    style = MaterialTheme.typography.bodyMedium,  // bodySmall에서 bodyMedium으로 증가
                    fontSize = 15.sp  // 12sp에서 15sp로 (약 25% 증가)
                )
            }
        }
    }
}

@Composable
fun PostItemCard(post: PostItem, onClick: () -> Unit) {
    val isVisited = VisitedPostsManager.isVisited(post.url)
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .then(
                if (isVisited) {
                    Modifier.alpha(0.6f)
                } else {
                    Modifier
                }
            ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isVisited) 0.5.dp else 1.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)  // 더 줄임
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
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onError,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                // 제목
                Text(
                    text = post.title,
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 12.sp,
                    maxLines = 2,
                    color = MaterialTheme.colorScheme.onSurface,
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
                        fontSize = 10.sp,  // 11sp에서 10sp로
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (post.date.isNotEmpty()) {
                    Text(
                        text = post.date,
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 10.sp,  // 11sp에서 10sp로
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 방문한 글 관리자 초기화
        VisitedPostsManager.init(this)
        
        setContent {
            MaterialTheme {
                ClienApp()
            }
        }
    }
}