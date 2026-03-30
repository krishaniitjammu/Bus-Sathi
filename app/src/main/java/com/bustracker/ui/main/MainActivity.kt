package com.karroh.bussathi.ui.main

import android.Manifest
import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import com.karroh.bussathi.R
import com.karroh.bussathi.data.model.Trip
import com.karroh.bussathi.data.model.TripStatus
import com.karroh.bussathi.data.repository.AuthRepository
import com.karroh.bussathi.data.repository.LocationRepository
import com.karroh.bussathi.data.repository.TripRepository
import com.karroh.bussathi.databinding.ActivityMainBinding
import com.karroh.bussathi.service.LocationTrackingService
import com.karroh.bussathi.ui.auth.LoginActivity
import com.karroh.bussathi.util.DailyReminderWorker // <-- ADDED THIS IMPORT!
import com.karroh.bussathi.util.PermissionHelper
import com.karroh.bussathi.util.TripStateManager
import kotlinx.coroutines.launch
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * Main screen for trip management, updates, and security checks.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    // Repositories
    private val authRepository = AuthRepository()
    private lateinit var locationRepository: LocationRepository
    private val tripRepository = TripRepository()
    private lateinit var tripStateManager: TripStateManager

    // Service & Trip State
    private var trackingService: LocationTrackingService? = null
    private var isServiceBound = false
    private var isTripActive = false
    private var currentTripId: String? = null
    private var serviceStatsJob: kotlinx.coroutines.Job? = null

    // Update & Security
    private lateinit var appUpdateManager: AppUpdateManager
    private val UPDATE_REQUEST_CODE = 123
    private var devModeDialog: AlertDialog? = null

    // GPS Monitoring
    private var gpsStatusReceiver: android.content.BroadcastReceiver? = null
    private var gpsWarningDialog: AlertDialog? = null

    // Notification Permission Launcher
    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Toast.makeText(this, "Notifications enabled", Toast.LENGTH_SHORT).show()
            scheduleDailyReminder()
        }
    }

    // Service Connection for Location Tracking
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as LocationTrackingService.LocalBinder
            trackingService = binder.getService()
            isServiceBound = true

            val (pointCount, distance) = trackingService?.getStatistics() ?: Pair(0, 0.0)
            updateStatistics(pointCount, distance)

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 1. Initialize Managers
        locationRepository = LocationRepository.getInstance(applicationContext)
        tripStateManager = TripStateManager.getInstance(applicationContext)
        appUpdateManager = AppUpdateManagerFactory.create(this)

        // 2. Initial Setup
        setupUI()
        setupListeners()
        checkPermissions()
        restoreTripState()

        // 3. Check for Updates immediately on launch
        checkForUpdates()

        setupGpsReceiver()
    }

    override fun onResume() {
        super.onResume()

        // 1. Security Check: Strict Aggressive Check
        checkDeveloperOptions()

        // ADD THIS: Start listening for GPS changes
        val filter = IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION)
        gpsStatusReceiver?.let { registerReceiver(it, filter) }

        // ADD THIS: Check immediately in case they turned it off while app was in background
        checkGpsStatusDuringTrip()

        // 2. Update Check: If an update is in progress (downloaded in background), resume the screen
        appUpdateManager.appUpdateInfo.addOnSuccessListener { appUpdateInfo ->
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
                appUpdateManager.startUpdateFlowForResult(
                    appUpdateInfo,
                    AppUpdateType.IMMEDIATE,
                    this,
                    UPDATE_REQUEST_CODE
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceStatsJob?.cancel()
        if (isServiceBound) {
            unbindService(serviceConnection)
        }
    }

    // ==========================================
    // SECTION: Security & Updates
    // ==========================================

    private fun checkDeveloperOptions() {
        if (isDevOptionsEnabled()) {
            showDevModeBlockingDialog()
        } else {
            devModeDialog?.dismiss()
        }
    }

    private fun isDevOptionsEnabled(): Boolean {
        val devOptions = try { Settings.Global.getInt(contentResolver, Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0) } catch (e: Exception) { 0 }
        val adbOptions = try { Settings.Global.getInt(contentResolver, Settings.Global.ADB_ENABLED, 0) } catch (e: Exception) { 0 }
        val mockLocation = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            try { if(Settings.Secure.getString(contentResolver, Settings.Secure.ALLOW_MOCK_LOCATION) != "0") 1 else 0 } catch (e: Exception) { 0 }
        } else { 0 }

        return devOptions == 1 || adbOptions == 1 || mockLocation == 1
    }

    private fun showDevModeBlockingDialog() {
        if (devModeDialog?.isShowing == true) return

        devModeDialog = AlertDialog.Builder(this)
            .setTitle("Security Restriction")
            .setMessage("Developer Options are enabled on this device.\n\nTo prevent location spoofing and ensure data integrity, this app cannot run while Developer Mode is active.\n\nPlease disable Developer Options in Settings.")
            .setCancelable(false)
            .setPositiveButton("Go to Settings") { _, _ ->
                try {
                    startActivity(Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS))
                } catch (e: Exception) {
                    startActivity(Intent(Settings.ACTION_SETTINGS))
                }
            }
            .setNegativeButton("Exit App") { _, _ ->
                finishAffinity()
            }
            .create()

        devModeDialog?.show()
    }

    private fun checkForUpdates() {
        appUpdateManager.appUpdateInfo.addOnSuccessListener { appUpdateInfo ->
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)
            ) {
                appUpdateManager.startUpdateFlowForResult(
                    appUpdateInfo,
                    AppUpdateType.IMMEDIATE,
                    this,
                    UPDATE_REQUEST_CODE
                )
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == UPDATE_REQUEST_CODE) {
            if (resultCode != Activity.RESULT_OK) {
                Toast.makeText(this, "Update required to proceed", Toast.LENGTH_SHORT).show()
                checkForUpdates()
            }
        }
    }

    // ==========================================
    // SECTION: UI & Trip Logic
    // ==========================================

    private fun setupUI() {
        val user = authRepository.getCurrentUser()
        val driverName = user?.displayName ?: user?.email?.substringBefore("@") ?: "Driver"

        binding.tvWelcome.text = getString(R.string.welcome_message, driverName)

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
                // Ignore
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

    // ==========================================
    // SECTION: Permissions & Notifications
    // ==========================================

    private fun checkPermissions() {
        // 1. Check Location
        if (!PermissionHelper.hasLocationPermission(this)) {
            if (PermissionHelper.shouldShowLocationRationale(this)) {
                showPermissionRationale()
            } else {
                PermissionHelper.requestLocationPermission(this)
            }
        }

        // 2. Check Notifications and Handle Daily Reminder
        checkNotificationPermissionFlow()
    }

    private fun checkNotificationPermissionFlow() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED -> {
                    // Already granted. Ensure reminder is scheduled.
                    scheduleDailyReminder()
                }
                shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                    // User previously denied. Show the Soft Gate Pop-up!
                    showNotificationSoftGate()
                }
                else -> {
                    // First time asking ever. Ask directly without your custom pop-up.
                    requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        } else {
            // Devices below Android 13 do not need explicit runtime permission for notifications
            scheduleDailyReminder()
        }
    }

    private fun showNotificationSoftGate() {
        AlertDialog.Builder(this)
            .setTitle("Don't miss a trip!")
            .setMessage("Please allow notifications so we can remind you to start your daily trips. Without this, you might forget your schedule.")
            .setPositiveButton("Allow") { _, _ ->
                // Fire the system permission prompt
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                // User clicked cancel. Let them use the app normally!
                dialog.dismiss()
            }
            .show()
    }

    private fun scheduleDailyReminder() {
        // 1. Lock the timezone to Indian Standard Time (IST)
        val istTimeZone = java.util.TimeZone.getTimeZone("Asia/Kolkata")

        // 2. Get the exact time right now in IST
        val currentDate = java.util.Calendar.getInstance(istTimeZone)

        // 3. Set the target time to exactly 8:00 AM IST today
        val dueDate = java.util.Calendar.getInstance(istTimeZone)
        dueDate.set(java.util.Calendar.HOUR_OF_DAY, 8) // 8 for 8:00 AM
        dueDate.set(java.util.Calendar.MINUTE, 0)
        dueDate.set(java.util.Calendar.SECOND, 0)
        dueDate.set(java.util.Calendar.MILLISECOND, 0)

        // 4. If 8:00 AM IST has already passed today, push it to 8:00 AM tomorrow
        if (dueDate.before(currentDate)) {
            dueDate.add(java.util.Calendar.HOUR_OF_DAY, 24)
        }

        // 5. Calculate the exact milliseconds to wait until 8:00 AM IST hits
        val timeDiff = dueDate.timeInMillis - currentDate.timeInMillis

        // 6. Build the WorkRequest to wait until 8:00 AM, then repeat every 24 hours
        val dailyWorkRequest = PeriodicWorkRequestBuilder<DailyReminderWorker>(
            24L,
            TimeUnit.HOURS
        )
            .setInitialDelay(timeDiff, TimeUnit.MILLISECONDS)
            .build()

        // 7. Hand it over to Android WorkManager
        WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
            "DailyTripReminder",
            ExistingPeriodicWorkPolicy.UPDATE,
            dailyWorkRequest
        )
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

    // ==========================================
    // SECTION: Trip State Management
    // ==========================================

    private fun restoreTripState() {
        val tripState = tripStateManager.getTripState()

        if (tripState.isActive && tripState.tripId != null) {
            isTripActive = true
            currentTripId = tripState.tripId

            val serviceIntent = Intent(this, LocationTrackingService::class.java)
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(serviceIntent)
                } else {
                    startService(serviceIntent)
                }
                bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE)
                updateTripUI()
                Toast.makeText(this, "Reconnected to active trip", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
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

        lifecycleScope.launch {
            val user = authRepository.getCurrentUser()
            val uid = user?.uid ?: ""
            val driverResult = if (uid.isNotBlank()) authRepository.getDriver(uid) else Result.failure(Exception("No user"))
            val vehicleNumber = driverResult.getOrNull()?.vehicleNumber ?: ""
            val safeVehicle = if (vehicleNumber.isNotBlank()) {
                vehicleNumber.replace("\\s+".toRegex(), "_").uppercase(Locale.getDefault())
            } else {
                uid.take(6).uppercase(Locale.getDefault())
            }

            val startTime = System.currentTimeMillis()
            val startIdTime = com.karroh.bussathi.util.DateUtils.formatForId(startTime)
            currentTripId = "${safeVehicle}_${startIdTime}"

            tripStateManager.saveTripState(currentTripId!!, startTime)

            val serviceIntent = Intent(this@MainActivity, LocationTrackingService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }

            bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE)
            isTripActive = true
            updateTripUI()
            Toast.makeText(this@MainActivity, getString(R.string.trip_started), Toast.LENGTH_SHORT).show()
        }
    }

    private fun endTrip() {
        if (!isTripActive || currentTripId == null) return

        serviceStatsJob?.cancel()
        serviceStatsJob = null

        if (isServiceBound) {
            unbindService(serviceConnection)
            isServiceBound = false
        }
        trackingService = null

        val serviceIntent = Intent(this, LocationTrackingService::class.java)
        stopService(serviceIntent)

        val user = authRepository.getCurrentUser()
        val routePoints = locationRepository.getRecordedPoints().toList()
        val totalDistance = locationRepository.getTotalDistance()

        locationRepository.clearPoints()

        // ==========================================
        // SILENT DISCARD LOGIC
        // ==========================================
        // Check if distance is 0 OR points are less than 50
        if (totalDistance <= 0.0 || routePoints.size < 50) {

            // Reset the trip state and UI locally
            tripStateManager.clearTripState()
            isTripActive = false
            currentTripId = null
            updateTripUI()
            updateStatistics(0, 0.0)

            // Show the normal "Trip ended" toast so the driver doesn't suspect anything
            Toast.makeText(this, getString(R.string.trip_ended), Toast.LENGTH_SHORT).show()

            // RETURN completely stops the function here.
            // It will NEVER reach the Firebase upload code below.
            return
        }

        val driverName = user?.displayName ?: user?.email?.substringBefore("@") ?: ""
        val startEpoch = tripStateManager.getTripStartTime()
        val endEpoch = System.currentTimeMillis()

        val trip = Trip(
            id = currentTripId!!,
            driverId = user?.uid ?: "",
            driverEmail = user?.email ?: "",
            driverName = driverName,
            startTime = startEpoch,
            startTimeString = com.karroh.bussathi.util.DateUtils.formatForDisplay(startEpoch),
            endTime = endEpoch,
            endTimeString = com.karroh.bussathi.util.DateUtils.formatForDisplay(endEpoch),
            routePoints = routePoints,
            totalDistance = totalDistance,
            status = TripStatus.COMPLETED
        )

        tripStateManager.clearTripState()
        isTripActive = false
        currentTripId = null
        updateTripUI()
        updateStatistics(0, 0.0)

        lifecycleScope.launch {
            Toast.makeText(this@MainActivity, getString(R.string.trip_ended), Toast.LENGTH_SHORT).show()
            tripRepository.uploadTrip(trip).onSuccess {
                Toast.makeText(this@MainActivity, getString(R.string.upload_success), Toast.LENGTH_LONG).show()
            }.onFailure { exception ->
                Toast.makeText(
                    this@MainActivity,
                    getString(R.string.upload_error) + ": ${exception.message}",
                    Toast.LENGTH_LONG
                ).show()
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

            // ==========================================
            // LOTTIE ANIMATION: START DRIVING
            // ==========================================
            binding.lottieBusAnimation.playAnimation()

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

            // ==========================================
            // LOTTIE ANIMATION: STOP AND RESET
            // ==========================================
            binding.lottieBusAnimation.pauseAnimation()
            binding.lottieBusAnimation.progress = 0f
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

    // ==========================================
    // SECTION: Live GPS Monitoring
    // ==========================================

    private fun setupGpsReceiver() {
        gpsStatusReceiver = object : android.content.BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (LocationManager.PROVIDERS_CHANGED_ACTION == intent.action) {
                    checkGpsStatusDuringTrip()
                }
            }
        }
    }

    private fun checkGpsStatusDuringTrip() {
        // We only care if the GPS turns off WHILE a trip is active
        if (!isTripActive) return

        if (!isGPSEnabled()) {
            showGpsWarningDialog()
            // Optional: You could also pause your Lottie animation here!
            binding.lottieBusAnimation.pauseAnimation()
        } else {
            // If they turned it back on, hide the warning and resume!
            gpsWarningDialog?.dismiss()
            if (isTripActive) {
                binding.lottieBusAnimation.playAnimation()
            }
        }
    }

    private fun showGpsWarningDialog() {
        // Don't show it twice if it's already on the screen
        if (gpsWarningDialog?.isShowing == true) return

        gpsWarningDialog = AlertDialog.Builder(this)
            .setTitle("Trip Paused! GPS is OFF")
            .setMessage("Your trip is currently active, but your phone's Location (GPS) has been turned off.\n\nWe cannot record your points until you turn it back on. Please enable GPS to resume your trip.")
            .setCancelable(false) // They cannot tap outside to dismiss it!
            .setPositiveButton("Turn On GPS") { _, _ ->
                startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            }
            .create()

        gpsWarningDialog?.show()
    }
}