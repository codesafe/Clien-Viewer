package com.example.clienapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import coil.compose.LocalImageLoader
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import kotlinx.coroutines.launch


class BoardDetailActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val boardUrl = UrlUtils.decodeUrl(intent.getStringExtra("boardUrl") ?: "")
        val boardTitle = UrlUtils.decodeUrl(intent.getStringExtra("boardTitle") ?: "")

        Log.d("BoardDetailActivity", "Received boardUrl: $boardUrl, boardTitle: $boardTitle")

        // Coil ImageLoader with unsafe SSL settings
        val imageLoader = ImageLoader.Builder(this)
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
                        BoardDetailScreen(boardUrl = boardUrl, boardTitle = boardTitle, onBack = { finish() })
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BoardDetailScreen(boardUrl: String, boardTitle: String, onBack: () -> Unit) {
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
    val context = LocalContext.current // Add context for Intent
    var dragDistance by remember { mutableStateOf(0f) }
    val density = LocalDensity.current
    val swipeThreshold = with(density) { 100.dp.toPx() }

    // 초기 로드
    LaunchedEffect(boardUrl) {
        if (posts.isEmpty()) { // Only fetch if posts are not already loaded
            scope.launch {
                isLoading = true
                posts = repository.fetchBoardPosts(boardUrl, page = 0, forceRefresh = false)
                currentPage = 0
                hasMorePages = true
                isLoading = false
            }
        }
    }

    // Pull to refresh
    LaunchedEffect(isRefreshing) {
        if (isRefreshing) {
            scope.launch {
                val newPosts = repository.fetchBoardPosts(boardUrl, page = 0, forceRefresh = true)
                posts = newPosts
                currentPage = 0
                hasMorePages = true
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
                            hasMorePages = true
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .smartSwipeBack(
                onBack = onBack,
                onDrag = { distance ->
                    dragDistance = distance
                }
            )
    ) {
        Scaffold(
            topBar = {
                Column {
                    TopAppBar(
                        title = { Text(boardTitle) },
                        navigationIcon = {
                            IconButton(onClick = { onBack() }) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "뒤로가기")
                            }
                        }
                    )
                    Divider(thickness = 2.dp, color = Color.Black)
                }
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
                        modifier = Modifier.fillMaxSize().background(Color.White)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize().background(Color.White),
                        contentPadding = PaddingValues(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(posts.size) { index ->
                            val post = posts[index]
                            val currentIsVisited = VisitedPostsManager.isVisited(post.url)

                            PostItemCard(post, currentIsVisited) {
                                if (!post.isNotice) {
                                    // Mark as visited immediately
                                    VisitedPostsManager.markAsVisited(post.url)
                                }

                                val encodedUrl = UrlUtils.encodeUrl(post.url)
                                val encodedTitle = UrlUtils.encodeUrl(post.title)
                                val intent = Intent(context, PostDetailActivity::class.java).apply {
                                    putExtra("postUrl", encodedUrl)
                                    putExtra("postTitle", encodedTitle)
                                }
                                context.startActivity(intent)
                            }
                            if (index < posts.size - 1) {
                                Divider(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    thickness = 0.5.dp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                                )
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
        val iconProgress = (dragDistance / swipeThreshold).coerceIn(0f, 1f)

        if (dragDistance > 0) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .offset(x = (dragDistance * 0.1f).dp)
                    .padding(start = 16.dp)
                    .size(56.dp * iconProgress) // Animate size
                    .background(
                        //color = MaterialTheme.colorScheme.primary.copy(alpha = iconProgress * 0.8f),
                        color = Color.Gray.copy(iconProgress * 0.8f),
                        shape = CircleShape
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "뒤로가기",
                    tint = Color.White.copy(alpha = iconProgress),
                    modifier = Modifier.size(28.dp * iconProgress) // Animate size
                )
            }
        }
    }
}
