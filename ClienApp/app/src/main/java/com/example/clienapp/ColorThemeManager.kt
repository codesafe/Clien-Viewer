package com.example.clienapp

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ColorTheme(
    val topBarBackgroundColor: Color = Color(0xFF2196F3),
    val postTitleTextColor: Color = Color(0xFF000000),
    val postTitleBackgroundColor: Color = Color(0xFFFFFFFF),
    val postDetailTitleTextColor: Color = Color(0xFF000000),
    val postDetailTitleBackgroundColor: Color = Color(0xFFFFFFFF),
    val noticeTextColor: Color = Color(0xFF1976D2),
    val noticeBackgroundColor: Color = Color(0xFFE3F2FD),
    val visitedTextColor: Color = Color(0xFF757575),
    val visitedBackgroundColor: Color = Color(0xFFF5F5F5),
    val commentCountTextColor: Color = Color(0xFFFFEB3B),
    val commentCountBackgroundColor: Color = Color(0xFF757575)
)

object ColorThemeManager {
    private const val PREFS_NAME = "color_theme_preferences"
    private const val KEY_TOP_BAR_BG = "top_bar_background"
    private const val KEY_POST_TITLE_TEXT = "post_title_text"
    private const val KEY_POST_TITLE_BG = "post_title_background"
    private const val KEY_POST_DETAIL_TITLE_TEXT = "post_detail_title_text"
    private const val KEY_POST_DETAIL_TITLE_BG = "post_detail_title_background"
    private const val KEY_NOTICE_TEXT = "notice_text"
    private const val KEY_NOTICE_BG = "notice_background"
    private const val KEY_VISITED_TEXT = "visited_text"
    private const val KEY_VISITED_BG = "visited_background"
    private const val KEY_COMMENT_COUNT_TEXT = "comment_count_text"
    private const val KEY_COMMENT_COUNT_BG = "comment_count_background"
    
    private lateinit var prefs: SharedPreferences
    private val _currentTheme = MutableStateFlow(ColorTheme())
    val currentTheme: StateFlow<ColorTheme> = _currentTheme.asStateFlow()
    
    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        loadTheme()
    }
    
    private fun loadTheme() {
        _currentTheme.value = ColorTheme(
            topBarBackgroundColor = Color(prefs.getInt(KEY_TOP_BAR_BG, Color(0xFF2196F3).toArgb())),
            postTitleTextColor = Color(prefs.getInt(KEY_POST_TITLE_TEXT, Color(0xFF000000).toArgb())),
            postTitleBackgroundColor = Color(prefs.getInt(KEY_POST_TITLE_BG, Color(0xFFFFFFFF).toArgb())),
            postDetailTitleTextColor = Color(prefs.getInt(KEY_POST_DETAIL_TITLE_TEXT, Color(0xFF000000).toArgb())),
            postDetailTitleBackgroundColor = Color(prefs.getInt(KEY_POST_DETAIL_TITLE_BG, Color(0xFFFFFFFF).toArgb())),
            noticeTextColor = Color(prefs.getInt(KEY_NOTICE_TEXT, Color(0xFF1976D2).toArgb())),
            noticeBackgroundColor = Color(prefs.getInt(KEY_NOTICE_BG, Color(0xFFE3F2FD).toArgb())),
            visitedTextColor = Color(prefs.getInt(KEY_VISITED_TEXT, Color(0xFF757575).toArgb())),
            visitedBackgroundColor = Color(prefs.getInt(KEY_VISITED_BG, Color(0xFFF5F5F5).toArgb())),
            commentCountTextColor = Color(prefs.getInt(KEY_COMMENT_COUNT_TEXT, Color(0xFFFFEB3B).toArgb())),
            commentCountBackgroundColor = Color(prefs.getInt(KEY_COMMENT_COUNT_BG, Color(0xFF757575).toArgb()))
        )
    }
    
    fun updateTheme(newTheme: ColorTheme) {
        prefs.edit().apply {
            putInt(KEY_TOP_BAR_BG, newTheme.topBarBackgroundColor.toArgb())
            putInt(KEY_POST_TITLE_TEXT, newTheme.postTitleTextColor.toArgb())
            putInt(KEY_POST_TITLE_BG, newTheme.postTitleBackgroundColor.toArgb())
            putInt(KEY_POST_DETAIL_TITLE_TEXT, newTheme.postDetailTitleTextColor.toArgb())
            putInt(KEY_POST_DETAIL_TITLE_BG, newTheme.postDetailTitleBackgroundColor.toArgb())
            putInt(KEY_NOTICE_TEXT, newTheme.noticeTextColor.toArgb())
            putInt(KEY_NOTICE_BG, newTheme.noticeBackgroundColor.toArgb())
            putInt(KEY_VISITED_TEXT, newTheme.visitedTextColor.toArgb())
            putInt(KEY_VISITED_BG, newTheme.visitedBackgroundColor.toArgb())
            putInt(KEY_COMMENT_COUNT_TEXT, newTheme.commentCountTextColor.toArgb())
            putInt(KEY_COMMENT_COUNT_BG, newTheme.commentCountBackgroundColor.toArgb())
            apply()
        }
        _currentTheme.value = newTheme
    }
    
    fun resetToDefault() {
        updateTheme(ColorTheme())
    }
}