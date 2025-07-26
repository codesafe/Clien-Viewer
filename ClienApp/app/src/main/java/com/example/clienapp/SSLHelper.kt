package com.example.clienapp

import android.util.Log
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.security.cert.X509Certificate
import javax.net.ssl.*

object SSLHelper {
    fun getUnsafeOkHttpClient(): OkHttpClient {
        return try {
            // Create a trust manager that does not validate certificate chains
            val trustAllCerts = arrayOf<TrustManager>(
                object : X509TrustManager {
                    override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                    override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                    override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
                }
            )

            // Install the all-trusting trust manager
            val sslContext = SSLContext.getInstance("SSL")
            sslContext.init(null, trustAllCerts, java.security.SecureRandom())
            
            // Create custom logging interceptor for m.clien.net packets
            val loggingInterceptor = HttpLoggingInterceptor { message ->
                NetworkLogger.logDebug("ClienApp-Network", message)
            }.apply {
                level = NetworkLogger.getHttpLoggingLevel()
            }
            
            // Create custom interceptor for detailed packet logging
            val packetLoggingInterceptor = Interceptor { chain ->
                val request = chain.request()
                val startTime = System.currentTimeMillis()
                
                if (NetworkLogger.shouldLogPackets()) {
                    NetworkLogger.logDebug("ClienApp-Packet", "========== REQUEST START ==========")
                    NetworkLogger.logDebug("ClienApp-Packet", "URL: ${request.url}")
                    NetworkLogger.logDebug("ClienApp-Packet", "Method: ${request.method}")
                    NetworkLogger.logDebug("ClienApp-Packet", "Headers: ${request.headers}")
                    if (request.body != null) {
                        NetworkLogger.logDebug("ClienApp-Packet", "Request Body Size: ${request.body!!.contentLength()} bytes")
                    }
                }
                
                val response = chain.proceed(request)
                val endTime = System.currentTimeMillis()
                
/*                if (NetworkLogger.shouldLogPackets()) {
                    NetworkLogger.logDebug("ClienApp-Packet", "========== RESPONSE START ==========")
                    NetworkLogger.logDebug("ClienApp-Packet", "Response Code: ${response.code}")
                    NetworkLogger.logDebug("ClienApp-Packet", "Response Message: ${response.message}")
                    NetworkLogger.logDebug("ClienApp-Packet", "Response Headers: ${response.headers}")
                    NetworkLogger.logDebug("ClienApp-Packet", "Content-Type: ${response.header("Content-Type")}")
                    NetworkLogger.logDebug("ClienApp-Packet", "Content-Length: ${response.header("Content-Length")}")
                    NetworkLogger.logDebug("ClienApp-Packet", "Response Time: ${endTime - startTime}ms")
                    
                    // Log response body for m.clien.net
                    if (request.url.host.contains("clien.net")) {
                        val responseBody = response.peekBody(Long.MAX_VALUE)
                        val bodyString = responseBody.string()
                        NetworkLogger.logDebug("ClienApp-Packet", "Response Body Length: ${bodyString.length} characters")
                        NetworkLogger.logDebug("ClienApp-Packet", "Response Body Preview: ${bodyString.take(500)}")
                        if (bodyString.length > 500) {
                            NetworkLogger.logDebug("ClienApp-Packet", "... (truncated)")
                        }
                    }
                    
                    NetworkLogger.logDebug("ClienApp-Packet", "========== RESPONSE END ==========")
                }*/
                response
            }
            
            OkHttpClient.Builder()
                .sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
                .hostnameVerifier { _, _ -> true }
                .addInterceptor(packetLoggingInterceptor)
                .addInterceptor(loggingInterceptor)
                .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .followRedirects(true)
                .followSslRedirects(true)
                .build()
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }
    
    fun getSocketFactory(): SSLSocketFactory {
        return try {
            val trustAllCerts = arrayOf<TrustManager>(
                object : X509TrustManager {
                    override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                    override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                    override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
                }
            )

            val sslContext = SSLContext.getInstance("SSL")
            sslContext.init(null, trustAllCerts, java.security.SecureRandom())
            sslContext.socketFactory
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }
}