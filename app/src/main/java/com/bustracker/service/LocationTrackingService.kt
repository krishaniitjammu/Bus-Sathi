package com.bustracker.service

import android.app.*
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.bustracker.R
import com.bustracker.data.repository.LocationRepository
import com.bustracker.ui.main.MainActivity
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.channels.BufferOverflow

/**
 * Foreground service for continuous GPS location tracking
 */
class LocationTrackingService : Service() {
    
    private val binder = LocalBinder()
    private lateinit var locationRepository: LocationRepository
    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var locationJob: Job? = null
    
    // Flag to prevent duplicate tracking when service is reconnected
    private var isTrackingActive = false
    
    private var pointCount = 0
    private var totalDistance = 0.0
    
    companion object {
        const val CHANNEL_ID = "LocationTrackingChannel"
        const val NOTIFICATION_ID = 1
        const val ACTION_LOCATION_UPDATE = "com.bustracker.LOCATION_UPDATE"
        const val EXTRA_POINT_COUNT = "point_count"
        const val EXTRA_DISTANCE = "distance"
    }

    // SharedFlow to emit updates to bound clients
    private val _serviceStats = kotlinx.coroutines.flow.MutableSharedFlow<Pair<Int, Double>>(
        replay = 1, 
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val serviceStats: kotlinx.coroutines.flow.SharedFlow<Pair<Int, Double>> = _serviceStats.asSharedFlow()
    
    inner class LocalBinder : Binder() {
        fun getService(): LocationTrackingService = this@LocationTrackingService
    }
    
    override fun onCreate() {
        super.onCreate()
        locationRepository = LocationRepository.getInstance(applicationContext)
        createNotificationChannel()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, createNotification())
        startLocationTracking()
        return START_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder {
        return binder
    }
    
    override fun onDestroy() {
        super.onDestroy()
        stopLocationTracking()
        serviceScope.cancel()
        // Reset stats when service is destroyed
        pointCount = 0
        totalDistance = 0.0
    }
    
    /**
     * Start tracking location updates
     */
    private fun startLocationTracking() {
        // Prevent starting multiple tracking jobs
        if (isTrackingActive) {
            return
        }
        
        // Cancel any existing job first to prevent duplicates
        locationJob?.cancel()
        
        isTrackingActive = true
        locationJob = serviceScope.launch {
            try {
                locationRepository.getLocationUpdates()
                    .catch { e ->
                        // Handle errors
                        e.printStackTrace()
                    }
                    .collect {
                        handleLocationUpdate()
                    }
            } finally {
                isTrackingActive = false
            }
        }
    }
    
    /**
     * Stop tracking location updates
     */
    private fun stopLocationTracking() {
        locationJob?.cancel()
        locationJob = null
        isTrackingActive = false
    }
    
    /**
     * Handle new location point
     */
    private fun handleLocationUpdate() {
        pointCount = locationRepository.getPointCount()
        totalDistance = locationRepository.getTotalDistance()
        
        // Update notification
        updateNotification()
        
        // Emit update to bound clients
        _serviceStats.tryEmit(Pair(pointCount, totalDistance))
        
        // Broadcast update to UI (kept for backward compatibility, but Flow is preferred)
        broadcastLocationUpdate()
    }
    
    /**
     * Create notification channel (required for Android 8.0+)
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_channel_description)
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    /**
     * Create notification for foreground service
     */
    private fun createNotification(): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_text, pointCount, totalDistance))
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }
    
    /**
     * Update notification with current stats
     */
    private fun updateNotification() {
        val notification = createNotification()
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
    
    /**
     * Broadcast location update to UI
     */
    private fun broadcastLocationUpdate() {
        val intent = Intent(ACTION_LOCATION_UPDATE).apply {
            putExtra(EXTRA_POINT_COUNT, pointCount)
            putExtra(EXTRA_DISTANCE, totalDistance)
        }
        sendBroadcast(intent)
    }
    
    /**
     * Get current statistics
     */
    fun getStatistics(): Pair<Int, Double> {
        return Pair(pointCount, totalDistance)
    }
}
