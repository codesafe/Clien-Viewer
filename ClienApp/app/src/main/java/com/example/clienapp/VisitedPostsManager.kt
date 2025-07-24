package com.example.clienapp

import android.content.Context
import android.content.SharedPreferences

object VisitedPostsManager {
    private const val PREF_NAME = "visited_posts"
    private const val KEY_VISITED_POSTS = "visited_post_urls"
    private lateinit var sharedPreferences: SharedPreferences
    private val visitedPosts = mutableSetOf<String>()
    
    fun init(context: Context) {
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        loadVisitedPosts()
    }
    
    private fun loadVisitedPosts() {
        val savedPosts = sharedPreferences.getStringSet(KEY_VISITED_POSTS, emptySet()) ?: emptySet()
        visitedPosts.clear()
        visitedPosts.addAll(savedPosts)
    }
    
    fun markAsVisited(postUrl: String) {
        visitedPosts.add(postUrl)
        saveVisitedPosts()
    }
    
    fun isVisited(postUrl: String): Boolean {
        return visitedPosts.contains(postUrl)
    }
    
    private fun saveVisitedPosts() {
        sharedPreferences.edit().putStringSet(KEY_VISITED_POSTS, visitedPosts).apply()
    }
    
    fun clearAll() {
        visitedPosts.clear()
        saveVisitedPosts()
    }
}