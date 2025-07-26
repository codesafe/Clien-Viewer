package com.example.clienapp

import android.util.Base64
import java.net.URLDecoder
import java.net.URLEncoder

object UrlUtils {
    fun encodeUrl(url: String): String {
        return try {
            // Base64로 인코딩하여 특수문자 문제 회피
            Base64.encodeToString(url.toByteArray(), Base64.URL_SAFE or Base64.NO_WRAP)
        } catch (e: Exception) {
            // 실패 시 원본 반환
            url
        }
    }
    
    fun decodeUrl(encodedUrl: String): String {
        return try {
            // Base64 디코딩
            String(Base64.decode(encodedUrl, Base64.URL_SAFE or Base64.NO_WRAP))
        } catch (e: Exception) {
            // Base64 디코딩 실패 시 URLDecoder 시도
            try {
                URLDecoder.decode(encodedUrl, "UTF-8")
            } catch (e2: Exception) {
                // 모든 디코딩 실패 시 원본 반환
                encodedUrl
            }
        }
    }
}