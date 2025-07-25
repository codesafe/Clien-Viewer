package com.example.clienapp

import android.util.Log
import okhttp3.logging.HttpLoggingInterceptor

object NetworkLogger {
    enum class LogLevel {
        NONE,
        BASIC,
        HEADERS,
        BODY
    }
    
    private var currentLogLevel = LogLevel.BODY
    
    fun setLogLevel(level: LogLevel) {
        currentLogLevel = level
        Log.d("ClienApp-NetworkLogger", "Log level changed to: $level")
    }
    
    fun getHttpLoggingLevel(): HttpLoggingInterceptor.Level {
        return when (currentLogLevel) {
            LogLevel.NONE -> HttpLoggingInterceptor.Level.NONE
            LogLevel.BASIC -> HttpLoggingInterceptor.Level.BASIC
            LogLevel.HEADERS -> HttpLoggingInterceptor.Level.HEADERS
            LogLevel.BODY -> HttpLoggingInterceptor.Level.BODY
        }
    }
    
    fun shouldLogPackets(): Boolean {
        return currentLogLevel != LogLevel.NONE
    }
    
    fun logDebug(tag: String, message: String) {
        if (shouldLogPackets()) {
            Log.d(tag, message)
        }
    }
    
    fun logInfo(tag: String, message: String) {
        if (shouldLogPackets()) {
            Log.i(tag, message)
        }
    }
    
    fun logError(tag: String, message: String, throwable: Throwable? = null) {
        if (shouldLogPackets()) {
            if (throwable != null) {
                Log.e(tag, message, throwable)
            } else {
                Log.e(tag, message)
            }
        }
    }
}