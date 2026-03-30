package com.karroh.bussathi.util

import com.karroh.bussathi.data.model.LocationPoint
import kotlin.math.*

/**
 * Utility class for calculating distance between GPS coordinates
 */
object DistanceCalculator {
    
    private const val EARTH_RADIUS_KM = 6371.0
    
    /**
     * Calculate distance between two location points using Haversine formula
     * @return distance in kilometers
     */
    fun calculateDistance(point1: LocationPoint, point2: LocationPoint): Double {
        val lat1Rad = Math.toRadians(point1.latitude)
        val lat2Rad = Math.toRadians(point2.latitude)
        val deltaLat = Math.toRadians(point2.latitude - point1.latitude)
        val deltaLon = Math.toRadians(point2.longitude - point1.longitude)
        
        val a = sin(deltaLat / 2).pow(2) +
                cos(lat1Rad) * cos(lat2Rad) *
                sin(deltaLon / 2).pow(2)
        
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        
        return EARTH_RADIUS_KM * c
    }
    
    /**
     * Calculate total distance for a list of location points
     * @return total distance in kilometers
     */
    fun calculateTotalDistance(points: List<LocationPoint>): Double {
        if (points.size < 2) return 0.0
        
        var totalDistance = 0.0
        for (i in 0 until points.size - 1) {
            totalDistance += calculateDistance(points[i], points[i + 1])
        }
        
        return totalDistance
    }
}
