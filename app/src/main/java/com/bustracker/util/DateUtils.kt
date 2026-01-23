package com.bustracker.util

import java.text.SimpleDateFormat
import java.util.*

/**
 * Date formatting helpers used for trip IDs and for human-readable display
 */
object DateUtils {
    // Display format requested: dd:MM:yyyy HH:mm:ss
    private val displayFormat = SimpleDateFormat("dd:MM:yyyy HH:mm:ss", Locale.getDefault())
    // Compact ID-friendly format: yyyyMMdd_HHmmss
    private val idFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())

    fun formatForDisplay(epochMillis: Long): String {
        return displayFormat.format(Date(epochMillis))
    }

    fun formatForId(epochMillis: Long): String {
        return idFormat.format(Date(epochMillis))
    }
}
