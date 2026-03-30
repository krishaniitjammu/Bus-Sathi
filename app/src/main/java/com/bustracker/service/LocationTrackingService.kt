package com.karroh.bussathi.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

import com.karroh.bussathi.data.repository.LocationRepository
import com.karroh.bussathi.ui.main.MainActivity
import com.karroh.bussathi.R
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

    // GPS Background Monitoring
    private var gpsReceiver: android.content.BroadcastReceiver? = null
    
    companion object {
        const val CHANNEL_ID = "LocationTrackingChannel"
        const val NOTIFICATION_ID = 1
        const val ACTION_LOCATION_UPDATE = "com.karroh.bussathi.LOCATION_UPDATE"
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

        registerGpsReceiver()
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

        gpsReceiver?.let { unregisterReceiver(it) }
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

    private fun registerGpsReceiver() {
        gpsReceiver = object : android.content.BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (android.location.LocationManager.PROVIDERS_CHANGED_ACTION == intent.action) {
                    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as android.location.LocationManager
                    val isGpsEnabled = locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER)

                    if (!isGpsEnabled) {
                        sendGpsWarningNotification()
                    }
                }
            }
        }
        val filter = android.content.IntentFilter(android.location.LocationManager.PROVIDERS_CHANGED_ACTION)
        registerReceiver(gpsReceiver, filter)
    }

    private fun sendGpsWarningNotification() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager

        // 1. Create a High-Priority Channel (Required for Android 8+)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel(
                "gps_alert_channel",
                "GPS Alerts",
                android.app.NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts when GPS is turned off during a trip"
            }
            notificationManager.createNotificationChannel(channel)
        }

        // 2. Create an Intent that opens Location Settings when the notification is tapped
        val settingsIntent = Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val pendingIntent = android.app.PendingIntent.getActivity(
            this, 0, settingsIntent, android.app.PendingIntent.FLAG_IMMUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT
        )

        // 3. Build and show the notification
        val notification = androidx.core.app.NotificationCompat.Builder(this, "gps_alert_channel")
            .setContentTitle("⚠️ Trip Paused: GPS is OFF")
            .setContentText("Tap here to turn on Location and resume recording points.")
            .setSmallIcon(android.R.drawable.ic_dialog_alert) // You can change this to R.drawable.your_app_icon later
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
            .setCategory(androidx.core.app.NotificationCompat.CATEGORY_ERROR)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        // 999 is just a unique ID for this specific alert
        notificationManager.notify(999, notification)
    }
}
