package com.example.homeaudiogem.api

import com.example.homeaudiogem.models.Zone
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface ApiService {
    // Get all zones
    @GET("zones")
    suspend fun getZones(): List<Zone>

    // Get a specific zone
    @GET("zones/{zone}")
    suspend fun getZone(@Path("zone") zone: String): Zone

    // Set a zone attribute
    @POST("zones/{zone}/{attribute}")
    @Headers("Content-Type: text/plain")
    suspend fun setZoneAttribute(
        @Path("zone") zone: String,
        @Path("attribute") attribute: String,
        @Body value: String
    ): Zone

    // Set an attribute for all zones
    @POST("allzones/{attribute}")
    @Headers("Content-Type: text/plain")
    suspend fun setAllZonesAttribute(
        @Path("attribute") attribute: String,
        @Body value: String
    ): List<Zone>

    // Get amp count
    @GET("ampCount")
    suspend fun getAmpCount(): Map<String, Int>

    // Set amp count
    @POST("ampCount")
    @Headers("Content-Type: text/plain")
    suspend fun setAmpCount(@Body count: String): Response<ResponseBody>

    // Save zone order
    @POST("sortOrder")
    suspend fun saveZoneOrder(@Body orderMap: Map<String, Int>): Response<ResponseBody>
} 