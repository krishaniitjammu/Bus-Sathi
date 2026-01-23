package com.bustracker.data.repository

import com.bustracker.data.model.Trip
import com.bustracker.data.model.TripStatus
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Repository for managing trip data and Firebase operations
 */
class TripRepository {
    
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val tripsCollection = firestore.collection("trips")
    
    /**
     * Upload a completed trip to Firestore
     */
    suspend fun uploadTrip(trip: Trip): Result<String> {
        return try {
            val tripData = trip.toMap()
            tripsCollection.document(trip.id).set(tripData).await()
            Result.success(trip.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Create a new trip document in Firestore
     */
    suspend fun createTrip(trip: Trip): Result<String> {
        return try {
            val tripData = trip.toMap()
            tripsCollection.document(trip.id).set(tripData).await()
            Result.success(trip.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Update trip status
     */
    suspend fun updateTripStatus(tripId: String, status: TripStatus): Result<Unit> {
        return try {
            tripsCollection.document(tripId)
                .update("status", status.name)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get all trips for a specific driver
     */
    suspend fun getDriverTrips(driverId: String): Result<List<Trip>> {
        return try {
            val snapshot = tripsCollection
                .whereEqualTo("driverId", driverId)
                .get()
                .await()
            
            val trips = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Trip::class.java)
            }
            Result.success(trips)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
