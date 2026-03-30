package com.karroh.bussathi.data.repository

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import com.karroh.bussathi.data.model.LocationPoint
import com.karroh.bussathi.util.DistanceCalculator
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

        // 30 meters per second is ~108 km/h.
        // A bus in J&K mountains will not travel faster than this.
        private const val MAX_SPEED_METERS_PER_SECOND = 30.0

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
                result.lastLocation?.let { newLocation ->

                    // ==========================================
                    // GPS MULTIPATH (SPIKE) FILTER LOGIC
                    // ==========================================
                    var isValidPoint = true
                    val lastPoint = locationPoints.lastOrNull()
                    val currentTime = System.currentTimeMillis()

                    if (lastPoint != null) {
                        // 1. Calculate time difference in seconds
                        val timeDeltaSeconds = (currentTime - lastPoint.timestamp) / 1000.0

                        if (timeDeltaSeconds > 0) {
                            // 2. Convert the last recorded point back into an Android Location object to calculate distance
                            val prevLocation = Location("").apply {
                                latitude = lastPoint.latitude
                                longitude = lastPoint.longitude
                            }

                            // 3. Get exact distance in meters
                            val distanceMeters = prevLocation.distanceTo(newLocation)

                            // 4. Calculate speed: meters per second
                            val speedMps = distanceMeters / timeDeltaSeconds

                            // 5. If it requires moving faster than our max speed, it's a GPS glitch. Throw it out!
                            if (speedMps > MAX_SPEED_METERS_PER_SECOND) {
                                isValidPoint = false
                            }
                        }
                    }

                    // Optional extra mountain protection: Ignore points with horrible accuracy (e.g., > 100 meters)
                    // When a signal bounces off a mountain, accuracy usually drops significantly.
                    if (newLocation.accuracy > 100f) {
                        isValidPoint = false
                    }
                    // ==========================================

                    // If the point passed the tests, save it and send it to the UI
                    if (isValidPoint) {
                        val point = LocationPoint(
                            latitude = newLocation.latitude,
                            longitude = newLocation.longitude,
                            timestamp = currentTime,
                            accuracy = newLocation.accuracy
                        )
                        locationPoints.add(point)
                        trySend(point)
                    }
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
        }
    }
}