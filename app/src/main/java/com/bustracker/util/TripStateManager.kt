package com.karroh.bussathi.util

import android.content.Context
import android.content.SharedPreferences

/**
 * Manages trip state persistence using SharedPreferences
 */
class TripStateManager(context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )
    
    companion object {
        private const val PREFS_NAME = "trip_state_prefs"
        private const val KEY_IS_TRIP_ACTIVE = "is_trip_active"
        private const val KEY_CURRENT_TRIP_ID = "current_trip_id"
        private const val KEY_TRIP_START_TIME = "trip_start_time"
        
        @Volatile
        private var instance: TripStateManager? = null
        
        fun getInstance(context: Context): TripStateManager {
            return instance ?: synchronized(this) {
                instance ?: TripStateManager(context.applicationContext).also {
                    instance = it
                }
            }
        }
    }
    
    /**
     * Save trip state when starting a trip
     */
    fun saveTripState(tripId: String, startTime: Long) {
        prefs.edit().apply {
            putBoolean(KEY_IS_TRIP_ACTIVE, true)
            putString(KEY_CURRENT_TRIP_ID, tripId)
            putLong(KEY_TRIP_START_TIME, startTime)
            apply()
        }
    }
    
    /**
     * Clear trip state when ending a trip
     */
    fun clearTripState() {
        prefs.edit().apply {
            putBoolean(KEY_IS_TRIP_ACTIVE, false)
            remove(KEY_CURRENT_TRIP_ID)
            remove(KEY_TRIP_START_TIME)
            apply()
        }
    }
    
    /**
     * Check if a trip is currently active
     */
    fun isTripActive(): Boolean {
        return prefs.getBoolean(KEY_IS_TRIP_ACTIVE, false)
    }
    
    /**
     * Get the current trip ID
     */
    fun getCurrentTripId(): String? {
        return prefs.getString(KEY_CURRENT_TRIP_ID, null)
    }
    
    /**
     * Get the trip start time
     */
    fun getTripStartTime(): Long {
        return prefs.getLong(KEY_TRIP_START_TIME, 0L)
    }
    
    /**
     * Get complete trip state
     */
    fun getTripState(): TripState {
        return TripState(
            isActive = isTripActive(),
            tripId = getCurrentTripId(),
            startTime = getTripStartTime()
        )
    }
    
    /**
     * Data class representing trip state
     */
    data class TripState(
        val isActive: Boolean,
        val tripId: String?,
        val startTime: Long
    )
}
