package com.example.homeaudiogem.api

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.preference.PreferenceManager
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Converter
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.lang.reflect.Type
import java.util.concurrent.TimeUnit

object ApiClient {
    private const val DEFAULT_SERVER_URL = "http://192.168.1.23:3000/api/"
    private const val DEFAULT_TIMEOUT = 10L
    private const val TAG = "ApiClient"

    private fun getServerUrl(context: Context): String {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        return sharedPreferences.getString("server_url", DEFAULT_SERVER_URL) ?: DEFAULT_SERVER_URL
    }

    fun getApiService(context: Context): ApiService {
        val baseUrl = getServerUrl(context)

        val gson = GsonBuilder()
            .setLenient()
            .create()

        // Create a logging interceptor
        val loggingInterceptor = HttpLoggingInterceptor { message ->
            Log.d(TAG, message)
        }.apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        // Create a custom timing interceptor
        val timingInterceptor = Interceptor { chain ->
            val request = chain.request()
            val startTime = System.currentTimeMillis()
            
            Log.i(TAG, "API Request: ${request.method} ${request.url}")
            
            val response: Response
            try {
                response = chain.proceed(request)
                val endTime = System.currentTimeMillis()
                val duration = endTime - startTime
                
                when (response.code) {
                    in 200..299 -> {
                        Log.i(TAG, "API Response: ${request.url} - ${response.code} (${duration}ms)")
                    }
                    500 -> {
                        Log.e(TAG, "API ERROR 500: ${request.url} - Internal Server Error (${duration}ms)")
                    }
                    else -> {
                        Log.w(TAG, "API Response: ${request.url} - ${response.code} (${duration}ms)")
                    }
                }
            } catch (e: IOException) {
                val endTime = System.currentTimeMillis()
                val duration = endTime - startTime
                Log.e(TAG, "API Call Failed: ${request.url} (${duration}ms) - ${e.message}")
                throw e
            }
            
            response
        }

        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS)
            .addInterceptor(timingInterceptor)
            .addInterceptor(loggingInterceptor)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(StringConverterFactory())
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()

        return retrofit.create(ApiService::class.java)
    }

    // Custom converter factory for plain text
    private class StringConverterFactory : Converter.Factory() {
        override fun requestBodyConverter(
            type: Type,
            parameterAnnotations: Array<out Annotation>,
            methodAnnotations: Array<out Annotation>,
            retrofit: Retrofit
        ): Converter<String, RequestBody>? {
            return if (type == String::class.java) {
                Converter { value -> value.toRequestBody("text/plain".toMediaType()) }
            } else {
                null
            }
        }
    }
}