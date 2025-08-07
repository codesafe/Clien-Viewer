package com.example.clienapp

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.concurrent.ConcurrentHashMap

object CacheManager {
    private val menuCache = ConcurrentHashMap<String, Pair<List<MenuItem>, Long>>()
    private val postsCache = ConcurrentHashMap<String, Pair<List<PostItem>, Long>>()
    private val postDetailCache = ConcurrentHashMap<String, Pair<PostDetail, Long>>()
    
    private const val CACHE_DURATION = 60 * 60 * 1000L * 10// 10시간
    private const val DISK_CACHE_DURATION = 24 * 60 * 60 * 1000L // 24시간 (디스크 캐시)
    
    private var sharedPrefs: SharedPreferences? = null
    private val gson = Gson()
    
    fun init(context: Context) {
        sharedPrefs = context.getSharedPreferences("clien_cache", Context.MODE_PRIVATE)
    }
    
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
        // 1. 메모리 캐시 확인
        val cached = postDetailCache[key]
        if (cached != null && System.currentTimeMillis() - cached.second < CACHE_DURATION) {
            return cached.first
        } else {
            postDetailCache.remove(key)
        }
        
        // 2. 디스크 캐시 확인
        return getDiskCachedPostDetail(key)
    }
    
    fun cachePostDetail(key: String, detail: PostDetail) {
        // 메모리 캐시 저장
        postDetailCache[key] = Pair(detail, System.currentTimeMillis())
        
        // 디스크 캐시 저장
        saveToDiskCache(key, detail)
    }
    
    private fun getDiskCachedPostDetail(key: String): PostDetail? {
        return try {
            val prefs = sharedPrefs ?: return null
            val jsonString = prefs.getString("post_detail_$key", null) ?: return null
            val timestamp = prefs.getLong("post_detail_time_$key", 0L)
            
            if (System.currentTimeMillis() - timestamp < DISK_CACHE_DURATION) {
                gson.fromJson(jsonString, PostDetail::class.java)
            } else {
                // 만료된 캐시 삭제
                prefs.edit()
                    .remove("post_detail_$key")
                    .remove("post_detail_time_$key")
                    .apply()
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    private fun saveToDiskCache(key: String, detail: PostDetail) {
        try {
            val prefs = sharedPrefs ?: return
            val jsonString = gson.toJson(detail)
            prefs.edit()
                .putString("post_detail_$key", jsonString)
                .putLong("post_detail_time_$key", System.currentTimeMillis())
                .apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    fun clearCache() {
        menuCache.clear()
        postsCache.clear()
        postDetailCache.clear()
    }
}