package com.example.rowingsync.data

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import okhttp3.logging.HttpLoggingInterceptor.Level

// Helper to detect debug build without a compile-time dependency on generated BuildConfig
private fun isDebugBuild(): Boolean {
    return try {
        val cls = Class.forName("com.example.rowingsync.BuildConfig")
        val field = cls.getField("DEBUG")
        field.getBoolean(null)
    } catch (e: Throwable) {
        // If BuildConfig isn't available for some reason, default to false (safe for release)
        false
    }
}

/**
 * Singleton API client for ESP32 communication
 */
object ApiClient {
    private var currentBaseUrl: String = "http://rower.local/"  // ESP32 mDNS hostname
    private var retrofit: Retrofit? = null
    private var api: Esp32Api? = null
    
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        // Only log full bodies in debug builds to avoid performance/security issues
        level = if (isDebugBuild()) {
            Level.BODY
        } else {
            Level.BASIC
        }
    }
    
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .addInterceptor(loggingInterceptor)
        .build()
    
    /**
     * Get API instance for the specified base URL
     * 
     * @param baseUrl The ESP32 address (e.g., "rower.local" or "192.168.4.1")
     * @return Esp32Api instance
     */
    fun getApi(baseUrl: String = currentBaseUrl): Esp32Api {
        val normalizedUrl = normalizeUrl(baseUrl)
        
        if (api == null || currentBaseUrl != normalizedUrl) {
            currentBaseUrl = normalizedUrl
            retrofit = Retrofit.Builder()
                .baseUrl(normalizedUrl)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
            api = retrofit!!.create(Esp32Api::class.java)
        }
        
        return api!!
    }
    
    /**
     * Normalize URL to ensure it has proper format
     */
    private fun normalizeUrl(url: String): String {
        var normalized = url.trim()
        
        // Add http:// if missing
        if (!normalized.startsWith("http://") && !normalized.startsWith("https://")) {
            normalized = "http://$normalized"
        }
        
        // Only add trailing slash if URL doesn't contain query parameters
        if (!normalized.endsWith("/") && !normalized.contains("?")) {
            normalized = "$normalized/"
        }
        
        return normalized
    }
    
    /**
     * Get current base URL
     */
    fun getCurrentBaseUrl(): String = currentBaseUrl
    
    /**
     * Reset the API client (useful when changing connection)
     */
    fun reset() {
        api = null
        retrofit = null
    }
}
