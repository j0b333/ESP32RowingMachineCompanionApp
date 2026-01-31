package com.example.rowingsync.data

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.http.*

/**
 * Retrofit API interface for ESP32 rowing monitor
 */
interface Esp32Api {
    
    companion object {
        /**
         * Empty request body for POST requests that don't require a body.
         * This ensures the HTTP method is properly recognized by the server.
         */
        val EMPTY_BODY: RequestBody by lazy {
            "".toRequestBody("application/json".toMediaType())
        }
    }
    
    @GET("api/status")
    suspend fun getStatus(): StatusResponse
    
    @GET("api/sessions")
    suspend fun getSessions(): SessionsResponse
    
    @GET("api/sessions/{id}")
    suspend fun getSession(@Path("id") id: Int): SessionDetail
    
    @POST("api/sessions/{id}/synced")
    suspend fun markSynced(@Path("id") id: Int, @Body body: RequestBody = EMPTY_BODY): GenericResponse
    
    @DELETE("api/sessions/{id}")
    suspend fun deleteSession(@Path("id") id: Int): GenericResponse
    
    @POST("workout/start")
    suspend fun startWorkout(): WorkoutResponse
    
    @POST("workout/stop")
    suspend fun stopWorkout(): WorkoutResponse
    
    @GET("live")
    suspend fun getLiveData(): LiveData
    
    @GET("hr")
    suspend fun getHeartRate(): String
}
