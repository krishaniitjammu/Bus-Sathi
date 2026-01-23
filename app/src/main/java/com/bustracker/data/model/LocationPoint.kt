package com.bustracker.data.model

/**
 * Represents a single GPS location point
 */
data class LocationPoint(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val timestamp: Long = 0L,
    val accuracy: Float = 0f
) {
    /**
     * Convert to a Map for Firebase storage
     */
    fun toMap(): Map<String, Any> {
        return mapOf(
            "latitude" to latitude,
            "longitude" to longitude,
            "timestamp" to timestamp,
            "accuracy" to accuracy
        )
    }
}
