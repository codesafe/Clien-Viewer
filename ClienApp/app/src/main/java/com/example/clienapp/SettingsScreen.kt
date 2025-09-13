package com.example.clienapp

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onNavigateToPreview: () -> Unit
) {
    val currentTheme by ColorThemeManager.currentTheme.collectAsState()
    var showResetDialog by remember { mutableStateOf(false) }
    
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("색상 초기화") },
            text = { Text("모든 색상 설정을 기본값으로 되돌리시겠습니까?") },
            confirmButton = {
                Button(
                    onClick = {
                        ColorThemeManager.resetToDefault()
                        showResetDialog = false
                    }
                ) {
                    Text("초기화")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showResetDialog = false }) {
                    Text("취소")
                }
            }
        )
    }
    
    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { 
                        Text(
                            text = "색상 설정",
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
                    actions = {
                        IconButton(onClick = onNavigateToPreview) {
                            Icon(
                                Icons.Filled.ArrowForward,
                                contentDescription = "미리보기",
                                tint = Color.White
                            )
                        }
                        IconButton(onClick = { showResetDialog = true }) {
                            Icon(
                                Icons.Filled.Refresh,
                                contentDescription = "초기화",
                                tint = Color.White
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = currentTheme.topBarBackgroundColor
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
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = "색상 조정",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "앱의 색상을 원하는 대로 변경하고 미리보기로 확인해보세요.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            item {
                ColorThemePreview(theme = currentTheme)
            }
            
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Filled.Settings,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "색상 설정",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        ColorSettingSection(
                            title = "최상단 글제목 배경색",
                            description = "상단 타이틀바의 배경 색상",
                            selectedColor = currentTheme.topBarBackgroundColor,
                            onColorSelected = { color ->
                                val newTheme = currentTheme.copy(topBarBackgroundColor = color)
                                ColorThemeManager.updateTheme(newTheme)
                            }
                        )
                        
                        ColorSettingSection(
                            title = "글제목 글자색",
                            description = "게시글 제목의 글자 색상",
                            selectedColor = currentTheme.postTitleTextColor,
                            onColorSelected = { color ->
                                val newTheme = currentTheme.copy(postTitleTextColor = color)
                                ColorThemeManager.updateTheme(newTheme)
                            }
                        )
                        
                        ColorSettingSection(
                            title = "글제목 배경색",
                            description = "게시글 제목의 배경 색상",
                            selectedColor = currentTheme.postTitleBackgroundColor,
                            onColorSelected = { color ->
                                val newTheme = currentTheme.copy(postTitleBackgroundColor = color)
                                ColorThemeManager.updateTheme(newTheme)
                            }
                        )
                        
                        Divider()
                        
                        ColorSettingSection(
                            title = "글내용 상단 제목 글자색",
                            description = "글 상세보기 상단 제목의 글자 색상",
                            selectedColor = currentTheme.postDetailTitleTextColor,
                            onColorSelected = { color ->
                                val newTheme = currentTheme.copy(postDetailTitleTextColor = color)
                                ColorThemeManager.updateTheme(newTheme)
                            }
                        )
                        
                        ColorSettingSection(
                            title = "글내용 상단 제목 배경색",
                            description = "글 상세보기 상단 제목의 배경 색상",
                            selectedColor = currentTheme.postDetailTitleBackgroundColor,
                            onColorSelected = { color ->
                                val newTheme = currentTheme.copy(postDetailTitleBackgroundColor = color)
                                ColorThemeManager.updateTheme(newTheme)
                            }
                        )
                        
                        Divider()
                        
                        ColorSettingSection(
                            title = "공지사항 글자색",
                            description = "공지사항 제목의 글자 색상",
                            selectedColor = currentTheme.noticeTextColor,
                            onColorSelected = { color ->
                                val newTheme = currentTheme.copy(noticeTextColor = color)
                                ColorThemeManager.updateTheme(newTheme)
                            }
                        )
                        
                        ColorSettingSection(
                            title = "공지사항 배경색",
                            description = "공지사항 항목의 배경 색상",
                            selectedColor = currentTheme.noticeBackgroundColor,
                            onColorSelected = { color ->
                                val newTheme = currentTheme.copy(noticeBackgroundColor = color)
                                ColorThemeManager.updateTheme(newTheme)
                            }
                        )
                        
                        Divider()
                        
                        ColorSettingSection(
                            title = "읽은글 목록 글자색",
                            description = "방문한 게시글의 글자 색상",
                            selectedColor = currentTheme.visitedTextColor,
                            onColorSelected = { color ->
                                val newTheme = currentTheme.copy(visitedTextColor = color)
                                ColorThemeManager.updateTheme(newTheme)
                            }
                        )
                        
                        ColorSettingSection(
                            title = "읽은글 목록 배경색",
                            description = "방문한 게시글의 배경 색상",
                            selectedColor = currentTheme.visitedBackgroundColor,
                            onColorSelected = { color ->
                                val newTheme = currentTheme.copy(visitedBackgroundColor = color)
                                ColorThemeManager.updateTheme(newTheme)
                            }
                        )
                        
                        Divider()
                        
                        ColorSettingSection(
                            title = "댓글 갯수 글자색",
                            description = "댓글 수 표시의 글자 색상",
                            selectedColor = currentTheme.commentCountTextColor,
                            onColorSelected = { color ->
                                val newTheme = currentTheme.copy(commentCountTextColor = color)
                                ColorThemeManager.updateTheme(newTheme)
                            }
                        )
                        
                        ColorSettingSection(
                            title = "댓글 갯수 배경색",
                            description = "댓글 수 표시의 배경 색상",
                            selectedColor = currentTheme.commentCountBackgroundColor,
                            onColorSelected = { color ->
                                val newTheme = currentTheme.copy(commentCountBackgroundColor = color)
                                ColorThemeManager.updateTheme(newTheme)
                            }
                        )
                    }
                }
            }
            
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "도움말",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Text(
                            text = "• 색상을 선택하려면 각 항목을 탭하세요.\n" +
                                  "• 기본 색상 또는 사용자 정의 색상을 선택할 수 있습니다.\n" +
                                  "• 미리보기 버튼으로 실제 적용 모습을 확인하세요.\n" +
                                  "• 초기화 버튼으로 모든 설정을 기본값으로 되돌릴 수 있습니다.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}