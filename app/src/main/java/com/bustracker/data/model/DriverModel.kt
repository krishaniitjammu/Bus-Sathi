package com.bustracker.data.model

import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class DriverModel(
    val name: String = "",
    val licenseNumber: String = "",
    val totalKm: Double = 0.0
)