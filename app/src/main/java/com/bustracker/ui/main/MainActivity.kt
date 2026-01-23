package com.bustracker.ui.main

import android.content.*
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bustracker.R
import com.bustracker.data.model.Trip
import com.bustracker.data.model.TripStatus
import com.bustracker.data.repository.AuthRepository
import com.bustracker.data.repository.LocationRepository
import com.bustracker.data.repository.TripRepository
import com.bustracker.databinding.ActivityMainBinding
import com.bustracker.service.LocationTrackingService
import com.bustracker.ui.auth.LoginActivity
import com.bustracker.util.PermissionHelper
import com.bustracker.util.TripStateManager
import kotlinx.coroutines.launch
import java.util.*

/**
 * Main screen for trip management
 */
class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    private val authRepository = AuthRepository()
    private lateinit var locationRepository: LocationRepository
    private val tripRepository = TripRepository()
    private lateinit var tripStateManager: TripStateManager
    
    private var trackingService: LocationTrackingService? = null
    private var isServiceBound = false
    private var isTripActive = false
    private var currentTripId: String? = null
    
    private var serviceStatsJob: kotlinx.coroutines.Job? = null
    
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as LocationTrackingService.LocalBinder
            trackingService = binder.getService()
            isServiceBound = true
            
            // Fetch current statistics immediately
            val (pointCount, distance) = trackingService?.getStatistics() ?: Pair(0, 0.0)
            updateStatistics(pointCount, distance)
            
            // Start collecting live updates
            serviceStatsJob = lifecycleScope.launch {
                trackingService?.serviceStats?.collect { (points, dist) ->
                    updateStatistics(points, dist)
                }
            }
        }
        
        override fun onServiceDisconnected(name: ComponentName?) {
            trackingService = null
            isServiceBound = false
            serviceStatsJob?.cancel()
        }
    }
    
    // BroadcastReceiver removed as we use Flow now
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        locationRepository = LocationRepository.getInstance(applicationContext)
        tripStateManager = TripStateManager.getInstance(applicationContext)
        
        setupUI()
        setupListeners()
        checkPermissions()
        
        // Restore trip state if exists
        restoreTripState()
    }
    
    override fun onStart() {
        super.onStart()
        // Receiver removed - using Flow updates via service connection
    }
    
    override fun onStop() {
        super.onStop()
        // Receiver removed
    }
    
    override fun onDestroy() {
        super.onDestroy()
        serviceStatsJob?.cancel()
        if (isServiceBound) {
            unbindService(serviceConnection)
        }
    }
    
    private fun setupUI() {
        val user = authRepository.getCurrentUser()
        val driverName = user?.displayName ?: user?.email?.substringBefore("@") ?: "Driver"

        // Set a default welcome and then try to enrich with vehicle number from drivers/{uid}
        binding.tvWelcome.text = getString(R.string.welcome_message, driverName)

        // Attempt to fetch driver profile to display vehicle/bus number if available
        lifecycleScope.launch {
            try {
                val uid = user?.uid
                if (!uid.isNullOrBlank()) {
                    val driverResult = authRepository.getDriver(uid)
                    driverResult.onSuccess { driver ->
                        val vehicle = driver.vehicleNumber
                        if (!vehicle.isNullOrBlank()) {
                            binding.tvWelcome.text = getString(
                                R.string.welcome_message_with_bus,
                                driverName,
                                vehicle
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                // Ignore — keep default welcome
            }
        }

        updateStatistics(0, 0.0)
    }
    
    private fun setupListeners() {
        binding.btnStartTrip.setOnClickListener {
            it.startAnimation(android.view.animation.AnimationUtils.loadAnimation(this, R.anim.button_click))
            if (isGPSEnabled()) {
                startTrip()
            } else {
                showGPSDisabledDialog()
            }
        }
        
        binding.btnEndTrip.setOnClickListener {
            it.startAnimation(android.view.animation.AnimationUtils.loadAnimation(this, R.anim.button_click))
            endTrip()
        }
        
        binding.btnLogout.setOnClickListener {
            it.startAnimation(android.view.animation.AnimationUtils.loadAnimation(this, R.anim.button_click))
            showLogoutConfirmation()
        }
        
        binding.btnSignOut.setOnClickListener {
            showLogoutConfirmation()
        }
    }
    
    private fun checkPermissions() {
        if (!PermissionHelper.hasLocationPermission(this)) {
            if (PermissionHelper.shouldShowLocationRationale(this)) {
                showPermissionRationale()
            } else {
                PermissionHelper.requestLocationPermission(this)
            }
        }
        
        if (!PermissionHelper.hasNotificationPermission(this)) {
            PermissionHelper.requestNotificationPermission(this)
        }
    }
    
    private fun showPermissionRationale() {
        AlertDialog.Builder(this)
            .setTitle("Location Permission Required")
            .setMessage(getString(R.string.permission_rationale))
            .setPositiveButton("OK") { _, _ ->
                PermissionHelper.requestLocationPermission(this)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        when (requestCode) {
            PermissionHelper.LOCATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Location permission granted", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, getString(R.string.permission_denied), Toast.LENGTH_LONG).show()
                }
            }
        }
    }
    
    private fun isGPSEnabled(): Boolean {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
    }
    
    private fun showGPSDisabledDialog() {
        AlertDialog.Builder(this)
            .setTitle("GPS Disabled")
            .setMessage(getString(R.string.gps_disabled))
            .setPositiveButton("Enable") { _, _ ->
                startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    /**
     * Restore trip state if a trip was active when app was closed
     */
    private fun restoreTripState() {
        val tripState = tripStateManager.getTripState()
        
        if (tripState.isActive && tripState.tripId != null) {
            // Restore trip variables
            isTripActive = true
            currentTripId = tripState.tripId
            
            // Properly start and bind to the service
            val serviceIntent = Intent(this, LocationTrackingService::class.java)
            try {
                // Start the service first as a foreground service to ensure proper lifecycle
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(serviceIntent)
                } else {
                    startService(serviceIntent)
                }
                
                // Then bind to it
                bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE)
                updateTripUI()
                Toast.makeText(this, "Reconnected to active trip", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                // Service not running, clear state
                tripStateManager.clearTripState()
                locationRepository.clearPoints()
                isTripActive = false
                currentTripId = null
                updateTripUI()
            }
        }
    }
    
    private fun startTrip() {
        if (!PermissionHelper.hasLocationPermission(this)) {
            Toast.makeText(this, getString(R.string.permission_denied), Toast.LENGTH_SHORT).show()
            return
        }

        // Generate a human-friendly trip ID using bus/vehicle number + start timestamp
        lifecycleScope.launch {
            val user = authRepository.getCurrentUser()
            val uid = user?.uid ?: ""
            val driverResult = if (uid.isNotBlank()) authRepository.getDriver(uid) else Result.failure(Exception("No user"))
            val vehicleNumber = driverResult.getOrNull()?.vehicleNumber ?: ""
            val safeVehicle = if (vehicleNumber.isNotBlank()) {
                vehicleNumber.replace("\\s+".toRegex(), "_").uppercase(Locale.getDefault())
            } else {
                // Fallback to short user id
                uid.take(6).uppercase(Locale.getDefault())
            }

            val startTime = System.currentTimeMillis()
            val startIdTime = com.bustracker.util.DateUtils.formatForId(startTime)
            currentTripId = "${safeVehicle}_${startIdTime}"

            tripStateManager.saveTripState(currentTripId!!, startTime)

            // Start location tracking service
            val serviceIntent = Intent(this@MainActivity, LocationTrackingService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }

            // Bind to service
            bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE)

            // Update UI
            isTripActive = true
            updateTripUI()

            Toast.makeText(this@MainActivity, getString(R.string.trip_started), Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun endTrip() {
        if (!isTripActive || currentTripId == null) return
        
        // Cancel the stats collection job first to prevent any UI updates
        serviceStatsJob?.cancel()
        serviceStatsJob = null
        
        // Stop service FIRST to prevent new points from being recorded
        if (isServiceBound) {
            unbindService(serviceConnection)
            isServiceBound = false
        }
        trackingService = null
        
        val serviceIntent = Intent(this, LocationTrackingService::class.java)
        stopService(serviceIntent)
        
        // Get trip data AFTER stopping service (make a copy of the points)
        val user = authRepository.getCurrentUser()
        val routePoints = locationRepository.getRecordedPoints().toList()
        val totalDistance = locationRepository.getTotalDistance()
        
        // Clear points IMMEDIATELY after getting the data to prevent any accumulation
        // if the service somehow restarts
        locationRepository.clearPoints()
        
        val driverName = user?.displayName ?: user?.email?.substringBefore("@") ?: ""

        val startEpoch = tripStateManager.getTripStartTime()
        val endEpoch = System.currentTimeMillis()
        val trip = Trip(
            id = currentTripId!!,
            driverId = user?.uid ?: "",
            driverEmail = user?.email ?: "",
            driverName = driverName,
            startTime = startEpoch,
            startTimeString = com.bustracker.util.DateUtils.formatForDisplay(startEpoch),
            endTime = endEpoch,
            endTimeString = com.bustracker.util.DateUtils.formatForDisplay(endEpoch),
            routePoints = routePoints,
            totalDistance = totalDistance,
            status = TripStatus.COMPLETED
        )
        
        // Clear trip state and UI BEFORE upload
        tripStateManager.clearTripState()
        isTripActive = false
        currentTripId = null
        updateTripUI()
        updateStatistics(0, 0.0)
        
        // Upload to Firebase
        lifecycleScope.launch {
            Toast.makeText(this@MainActivity, getString(R.string.trip_ended), Toast.LENGTH_SHORT).show()
            
            val result = tripRepository.uploadTrip(trip)
            result.onSuccess {
                Toast.makeText(this@MainActivity, getString(R.string.upload_success), Toast.LENGTH_LONG).show()
                // Points already cleared above
            }.onFailure { exception ->
                Toast.makeText(
                    this@MainActivity,
                    getString(R.string.upload_error) + ": ${exception.message}",
                    Toast.LENGTH_LONG
                ).show()
                // Points already cleared above
            }
        }
    }
    
    private fun updateTripUI() {
        if (isTripActive) {
            binding.tvTripStatus.text = getString(R.string.trip_status_active)
            binding.btnStartTrip.isEnabled = false
            binding.btnStartTrip.backgroundTintList = android.content.res.ColorStateList.valueOf(
                resources.getColor(R.color.button_disabled, null)
            )
            binding.btnEndTrip.isEnabled = true
            binding.btnEndTrip.backgroundTintList = android.content.res.ColorStateList.valueOf(
                resources.getColor(R.color.button_red, null)
            )
        } else {
            binding.tvTripStatus.text = getString(R.string.trip_status_idle)
            binding.btnStartTrip.isEnabled = true
            binding.btnStartTrip.backgroundTintList = android.content.res.ColorStateList.valueOf(
                resources.getColor(R.color.button_green, null)
            )
            binding.btnEndTrip.isEnabled = false
            binding.btnEndTrip.backgroundTintList = android.content.res.ColorStateList.valueOf(
                resources.getColor(R.color.button_disabled, null)
            )
        }
    }
    
    private fun updateStatistics(pointCount: Int, distance: Double) {
        binding.tvPointsRecorded.text = getString(R.string.points_recorded, pointCount)
        binding.tvDistance.text = getString(R.string.distance_traveled, distance)
    }
    
    private fun showLogoutConfirmation() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.logout))
            .setMessage(getString(R.string.logout_confirmation))
            .setPositiveButton(getString(R.string.yes)) { _, _ ->
                if (isTripActive) {
                    Toast.makeText(this, "Please end the current trip first", Toast.LENGTH_SHORT).show()
                } else {
                    authRepository.signOut()
                    val intent = Intent(this, LoginActivity::class.java)
                    startActivity(intent)
                    finish()
                }
            }
            .setNegativeButton(getString(R.string.no), null)
            .show()
    }
}
