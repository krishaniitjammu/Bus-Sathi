package com.karroh.bussathi.data.model

/**
 * Simple model to represent driver profile stored under drivers/{uid}
 */
data class Driver(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val vehicleNumber: String = "",
    val licenseNumber: String = "",
    val createdAt: Long = 0L
)
