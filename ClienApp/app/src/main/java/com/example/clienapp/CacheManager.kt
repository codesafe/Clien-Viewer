package com.example.clienapp

import java.util.concurrent.ConcurrentHashMap

object CacheManager {
    private val menuCache = ConcurrentHashMap<String, Pair<List<MenuItem>, Long>>()
    private val postsCache = ConcurrentHashMap<String, Pair<List<PostItem>, Long>>()
    private val postDetailCache = ConcurrentHashMap<String, Pair<PostDetail, Long>>()
    
    private const val CACHE_DURATION = 5 * 60 * 1000L // 5분
    
    fun getCachedMenuItems(key: String): List<MenuItem>? {
        val cached = menuCache[key]
        return if (cached != null && System.currentTimeMillis() - cached.second < CACHE_DURATION) {
            cached.first
        } else {
            menuCache.remove(key)
            null
        }
    }
    
    fun cacheMenuItems(key: String, items: List<MenuItem>) {
        menuCache[key] = Pair(items, System.currentTimeMillis())
    }
    
    fun getCachedPosts(key: String): List<PostItem>? {
        val cached = postsCache[key]
        return if (cached != null && System.currentTimeMillis() - cached.second < CACHE_DURATION) {
            cached.first
        } else {
            postsCache.remove(key)
            null
        }
    }
    
    fun cachePosts(key: String, posts: List<PostItem>) {
        postsCache[key] = Pair(posts, System.currentTimeMillis())
    }
    
    fun getCachedPostDetail(key: String): PostDetail? {
        val cached = postDetailCache[key]
        return if (cached != null && System.currentTimeMillis() - cached.second < CACHE_DURATION) {
            cached.first
        } else {
            postDetailCache.remove(key)
            null
        }
    }
    
    fun cachePostDetail(key: String, detail: PostDetail) {
        postDetailCache[key] = Pair(detail, System.currentTimeMillis())
    }
    
    fun clearCache() {
        menuCache.clear()
        postsCache.clear()
        postDetailCache.clear()
    }
}