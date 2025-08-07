package com.example.clienapp

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemePreviewScreen(
    theme: ColorTheme,
    onBack: () -> Unit
) {
    val samplePosts = listOf(
        PostItem("공지사항: 게시판 이용 안내", "", "관리자", "", "2024-01-01", "1000", 5, true),
        PostItem("읽은 글: 이미 방문한 게시글입니다", "", "사용자1", "", "2024-01-02", "500", 12, false),
        PostItem("일반 글: 새로운 게시글입니다", "", "사용자2", "", "2024-01-03", "200", 3, false),
        PostItem("댓글 많은 글: 활발한 토론이 진행중", "", "사용자3", "", "2024-01-04", "800", 25, false)
    )
    
    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { 
                        Text(
                            text = "색상 미리보기",
                            color = Color.White
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.Filled.ArrowBack, 
                                contentDescription = "뒤로가기",
                                tint = Color.White
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = theme.topBarBackgroundColor
                    )
                )
                Divider(thickness = 2.dp, color = Color.Black)
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(paddingValues),
            contentPadding = PaddingValues(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "미리보기",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "선택한 색상이 실제 앱에서 어떻게 보이는지 확인해보세요.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            items(samplePosts) { post ->
                PreviewPostItemCard(
                    post = post,
                    theme = theme,
                    isVisited = post.title.contains("읽은 글")
                )
                
                if (samplePosts.indexOf(post) < samplePosts.size - 1) {
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

@Composable
fun PreviewPostItemCard(
    post: PostItem,
    theme: ColorTheme,
    isVisited: Boolean
) {
    val backgroundColor = when {
        post.isNotice -> theme.noticeBackgroundColor
        isVisited -> theme.visitedBackgroundColor
        else -> theme.postTitleBackgroundColor
    }
    
    val titleColor = when {
        post.isNotice -> theme.noticeTextColor
        isVisited -> theme.visitedTextColor
        else -> theme.postTitleTextColor
    }
    
    val metaColor = if (isVisited && !post.isNotice) theme.visitedTextColor else Color.DarkGray

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(color = backgroundColor)
            .padding(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (post.commentCount > 0 && !post.isNotice) {
                Box(
                    modifier = Modifier
                        .background(
                            color = theme.commentCountBackgroundColor,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = post.commentCount.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        color = theme.commentCountTextColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

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
                    color = MaterialTheme.colorScheme.primary
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

@Composable
fun ColorThemePreview(
    theme: ColorTheme,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "미리보기",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // 상단바 미리보기
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        theme.topBarBackgroundColor,
                        RoundedCornerShape(4.dp)
                    )
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "게시판 제목",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 공지사항 미리보기 (댓글수 없음)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        theme.noticeBackgroundColor,
                        RoundedCornerShape(4.dp)
                    )
                    .padding(8.dp)
            ) {
                Text(
                    text = "공지사항 제목",
                    color = theme.noticeTextColor,
                    fontSize = 12.sp,
                    modifier = Modifier.weight(1f)
                )
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // 일반 글 미리보기
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        theme.postTitleBackgroundColor,
                        RoundedCornerShape(4.dp)
                    )
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .background(
                            theme.commentCountBackgroundColor,
                            RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "5",
                        color = theme.commentCountTextColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = "일반 글 제목",
                    color = theme.postTitleTextColor,
                    fontSize = 12.sp,
                    modifier = Modifier.weight(1f)
                )
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // 읽은 글 미리보기
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        theme.visitedBackgroundColor,
                        RoundedCornerShape(4.dp)
                    )
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .background(
                            theme.commentCountBackgroundColor,
                            RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "12",
                        color = theme.commentCountTextColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = "읽은 글 제목",
                    color = theme.visitedTextColor,
                    fontSize = 12.sp,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}