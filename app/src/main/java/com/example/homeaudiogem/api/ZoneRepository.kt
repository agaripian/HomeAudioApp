package com.example.homeaudiogem.api

import android.content.Context
import com.example.homeaudiogem.models.Zone
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ZoneRepository(private val context: Context) {
    private val apiService by lazy { ApiClient.getApiService(context) }

    suspend fun getZones(): List<Zone> = withContext(Dispatchers.IO) {
        try {
            apiService.getZones()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun getZone(zoneId: String): Zone? = withContext(Dispatchers.IO) {
        try {
            apiService.getZone(zoneId)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun setZoneAttribute(zoneId: String, attribute: String, value: String): Zone? = 
        withContext(Dispatchers.IO) {
            try {
                apiService.setZoneAttribute(zoneId, attribute, value)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }

    suspend fun setAllZonesAttribute(attribute: String, value: String): List<Zone> = 
        withContext(Dispatchers.IO) {
            try {
                apiService.setAllZonesAttribute(attribute, value)
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        }

    suspend fun getAmpCount(): Int = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getAmpCount()
            response["AmpCount"] ?: 1
        } catch (e: Exception) {
            e.printStackTrace()
            1 // Default value if there's an error
        }
    }

    suspend fun setAmpCount(count: Int): Boolean = withContext(Dispatchers.IO) {
        try {
            val response = apiService.setAmpCount(count.toString())
            response.isSuccessful
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun saveZoneOrder(zones: List<Zone>): Boolean = withContext(Dispatchers.IO) {
        try {
            val orderMap = zones.mapIndexed { index, zone ->
                zone.zone to index
            }.toMap()
            
            val response = apiService.saveZoneOrder(orderMap)
            response.isSuccessful
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    // Helper function to pad 2-digit numbers
    fun pad2(value: String): String {
        val numericValue = value.toIntOrNull() ?: return value
        return if (numericValue < 10) "0$numericValue" else numericValue.toString()
    }
} 