package com.bustracker.data.repository

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import com.bustracker.data.model.LocationPoint
import com.bustracker.util.DistanceCalculator
import com.google.android.gms.location.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Repository for managing GPS location tracking
 * Singleton to ensure data is shared across components
 */
class LocationRepository private constructor(private val context: Context) {
    
    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)
    
    private val locationPoints = mutableListOf<LocationPoint>()
    
    companion object {
        @Volatile
        private var INSTANCE: LocationRepository? = null
        
        fun getInstance(context: Context): LocationRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: LocationRepository(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
    }
    
    /**
     * Get location updates as a Flow
     */
    @SuppressLint("MissingPermission")
    fun getLocationUpdates(): Flow<LocationPoint> = callbackFlow {
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            5000L // Desired update interval: 5 seconds (requested)
        ).apply {
            // Encourage the provider to deliver updates at ~5s and avoid batching
            setMinUpdateIntervalMillis(5000L) // Fastest update: 5 seconds
            setMaxUpdateDelayMillis(0L) // Disable batching/delays; deliver promptly
        }.build()
        
        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    val point = LocationPoint(
                        latitude = location.latitude,
                        longitude = location.longitude,
                        timestamp = System.currentTimeMillis(),
                        accuracy = location.accuracy
                    )
                    locationPoints.add(point)
                    trySend(point)
                }
            }
        }
        
        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
        
        awaitClose {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
    }
    
    /**
     * Get all recorded location points
     */
    fun getRecordedPoints(): List<LocationPoint> {
        return locationPoints.toList()
    }
    
    /**
     * Get count of recorded points
     */
    fun getPointCount(): Int {
        return locationPoints.size
    }
    
    /**
     * Calculate total distance traveled
     */
    fun getTotalDistance(): Double {
        return DistanceCalculator.calculateTotalDistance(locationPoints)
    }
    
    /**
     * Clear all recorded points (call after trip ends and upload completes)
     */
    fun clearPoints() {
        locationPoints.clear()
    }
    
    /**
     * Get last known location
     */
    @SuppressLint("MissingPermission")
    suspend fun getLastLocation(): Location? {
        return try {
            fusedLocationClient.lastLocation.await()
        } catch (e: Exception) {
            null
    }}
}
