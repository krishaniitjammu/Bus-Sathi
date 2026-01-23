package com.bustracker.ui.splash

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.bustracker.R
import com.bustracker.data.repository.AuthRepository
import com.bustracker.ui.auth.LoginActivity
import com.bustracker.ui.main.MainActivity

/**
 * Splash screen showing the app logo while checking authentication status
 */
class SplashActivity : AppCompatActivity() {
    
    private val authRepository = AuthRepository()
    private val splashDelay = 2000L // 2 seconds
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        
        // Navigate after delay
        Handler(Looper.getMainLooper()).postDelayed({
            navigateToNextScreen()
        }, splashDelay)
    }
    
    private fun navigateToNextScreen() {
        val intent = if (authRepository.isUserLoggedIn()) {
            Intent(this, MainActivity::class.java)
        } else {
            Intent(this, LoginActivity::class.java)
        }
        
        startActivity(intent)
        finish()
    }
}
