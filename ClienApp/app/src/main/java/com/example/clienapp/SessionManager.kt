package com.example.clienapp

import android.content.Context
import android.content.SharedPreferences
import android.util.Log

object SessionManager {
    private const val PREF_NAME = "ClienSession"
    private const val KEY_IS_LOGGED_IN = "isLoggedIn"
    private const val KEY_USERNAME = "username"
    private const val KEY_USER_INFO = "userInfo"
    private const val KEY_SESSION_COOKIES = "sessionCookies"
    private const val KEY_LOGIN_TIME = "loginTime"
    
    private lateinit var prefs: SharedPreferences
    
    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }
    
    fun saveLoginSession(username: String, userInfo: String, sessionCookies: List<String>) {
        prefs.edit()
            .putBoolean(KEY_IS_LOGGED_IN, true)
            .putString(KEY_USERNAME, username)
            .putString(KEY_USER_INFO, userInfo)
            .putStringSet(KEY_SESSION_COOKIES, sessionCookies.toSet())
            .putLong(KEY_LOGIN_TIME, System.currentTimeMillis())
            .apply()
        
        Log.d("SessionManager", "Login session saved for user: $username")
    }
    
    fun isLoggedIn(): Boolean {
        val isLoggedIn = prefs.getBoolean(KEY_IS_LOGGED_IN, false)
        val loginTime = prefs.getLong(KEY_LOGIN_TIME, 0)
        val currentTime = System.currentTimeMillis()
        
        // 세션이 24시간 이상 지났으면 로그아웃 처리
        if (isLoggedIn && (currentTime - loginTime) > 24 * 60 * 60 * 1000) {
            logout()
            return false
        }
        
        return isLoggedIn
    }
    
    fun getUsername(): String {
        return prefs.getString(KEY_USERNAME, "") ?: ""
    }
    
    fun getUserInfo(): String {
        return prefs.getString(KEY_USER_INFO, "") ?: ""
    }
    
    fun getSessionCookies(): List<String> {
        return prefs.getStringSet(KEY_SESSION_COOKIES, emptySet())?.toList() ?: emptyList()
    }
    
    fun logout() {
        prefs.edit()
            .putBoolean(KEY_IS_LOGGED_IN, false)
            .remove(KEY_USERNAME)
            .remove(KEY_USER_INFO)
            .remove(KEY_SESSION_COOKIES)
            .remove(KEY_LOGIN_TIME)
            .apply()
        
        Log.d("SessionManager", "User logged out")
    }
    
    fun updateLoginTime() {
        if (isLoggedIn()) {
            prefs.edit()
                .putLong(KEY_LOGIN_TIME, System.currentTimeMillis())
                .apply()
        }
    }
}