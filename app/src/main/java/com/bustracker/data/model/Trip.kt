package com.bustracker.data.model

/**
 * Represents a complete bus trip with route data
 */
data class Trip(
    val id: String = "",
    val driverId: String = "",
    val driverEmail: String = "",
    val driverName: String = "",
    val startTime: Long = 0L,
    val startTimeString: String = "",
    val endTime: Long? = null,
    val endTimeString: String = "",
    val routePoints: List<LocationPoint> = emptyList(),
    val totalDistance: Double = 0.0,
    val status: TripStatus = TripStatus.ACTIVE
) {
    /**
     * Convert to a Map for Firebase storage
     */
    fun toMap(): Map<String, Any> {
        return hashMapOf(
            "id" to id,
            "driverId" to driverId,
            "driverName" to driverName,
            // Keep driverEmail for backward compatibility (may be empty)
            "driverEmail" to driverEmail,
            "startTime" to startTime,
            "startTimeString" to startTimeString,
            "endTime" to (endTime ?: 0L),
            "endTimeString" to endTimeString,
            "routePoints" to routePoints.map { it.toMap() },
            "totalDistance" to totalDistance,
            "status" to status.name
        )
    }
}
