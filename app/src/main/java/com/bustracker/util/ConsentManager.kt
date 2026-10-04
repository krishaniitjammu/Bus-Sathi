package com.karroh.bussathi.util

import android.content.Context
import android.content.SharedPreferences

/**
 * Stores the user's research data consent choice using SharedPreferences
 */
class ConsentManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    companion object {
        private const val PREFS_NAME = "consent_prefs"
        private const val KEY_CONSENT_ANSWERED = "research_consent_answered"
        private const val KEY_CONSENT_GRANTED = "research_consent_granted"
        private const val KEY_CONSENT_TIMESTAMP = "research_consent_timestamp"

        @Volatile
        private var instance: ConsentManager? = null

        fun getInstance(context: Context): ConsentManager {
            return instance ?: synchronized(this) {
                instance ?: ConsentManager(context.applicationContext).also {
                    instance = it
                }
            }
        }
    }

    /**
     * Save the user's choice so the popup is not shown again
     */
    fun setResearchConsent(granted: Boolean) {
        prefs.edit().apply {
            putBoolean(KEY_CONSENT_ANSWERED, true)
            putBoolean(KEY_CONSENT_GRANTED, granted)
            putLong(KEY_CONSENT_TIMESTAMP, System.currentTimeMillis())
            apply()
        }
    }

    /**
     * Check if the user has already answered the consent popup
     */
    fun hasAnsweredConsent(): Boolean {
        return prefs.getBoolean(KEY_CONSENT_ANSWERED, false)
    }

    /**
     * Check if the user allowed their data to be used for research.
     * Returns false until the user has accepted.
     */
    fun isResearchConsentGranted(): Boolean {
        return prefs.getBoolean(KEY_CONSENT_GRANTED, false)
    }

    /**
     * Get the time the choice was made (0 if not answered yet)
     */
    fun getConsentTimestamp(): Long {
        return prefs.getLong(KEY_CONSENT_TIMESTAMP, 0L)
    }
}
