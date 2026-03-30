package com.karroh.bussathi.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Repository for handling Firebase Authentication
 */
class AuthRepository {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    /**
     * Get currently logged in user
     */
    fun getCurrentUser(): FirebaseUser? {
        return auth.currentUser
    }

    /**
     * Sign in with email and password
     */
    suspend fun signIn(email: String, password: String): Result<FirebaseUser> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            result.user?.let {
                Result.success(it)
            } ?: Result.failure(Exception("Sign in failed"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Create a new account with email/password and store driver info in Firestore
     */
    suspend fun createAccount(
        email: String,
        password: String,
        name: String,
        phone: String,
        vehicleNumber: String,
        licenseNumber: String,
        region: String,          // Added: Matches 'Jammu' or 'Srinagar'
        vehicleCapacity: String  // Added: Matches '5 to 13 seater...', etc.
    ): Result<FirebaseUser> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val user = result.user ?: return Result.failure(Exception("Account creation failed"))

            // Update display name
            val profileUpdates = UserProfileChangeRequest.Builder()
                .setDisplayName(name)
                .build()
            user.updateProfile(profileUpdates).await()

            // Create a driver document in Firestore with full details
            val firestore = FirebaseFirestore.getInstance()

            // Map keys must match your Firestore Security Rules exactly
            val driverData = mapOf(
                "uid" to user.uid,
                "name" to name,
                "email" to email,
                "phone" to phone,
                "vehicleNumber" to vehicleNumber,
                "licenseNumber" to licenseNumber,
                "region" to region,                       // SAVING REGION
                "vehicleCapacity" to vehicleCapacity,     // SAVING CAPACITY
                "createdAt" to System.currentTimeMillis()
            )

            firestore.collection("drivers").document(user.uid).set(driverData).await()

            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get driver profile document for given uid (or current user when uid is null)
     */
    suspend fun getDriver(uid: String? = auth.currentUser?.uid): Result<com.karroh.bussathi.data.model.Driver> {
        return try {
            val actualUid = uid ?: return Result.failure(Exception("No uid provided and no current user."))
            val firestore = FirebaseFirestore.getInstance()
            val snapshot = firestore.collection("drivers").document(actualUid).get().await()
            val driver = snapshot.toObject(com.karroh.bussathi.data.model.Driver::class.java)
            if (driver != null) Result.success(driver) else Result.failure(Exception("Driver not found"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Sign out current user
     */
    fun signOut() {
        auth.signOut()
    }

    /**
     * Check if user is logged in
     */
    fun isUserLoggedIn(): Boolean {
        return auth.currentUser != null
    }
}