package com.karroh.bussathi.ui.splash

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.karroh.bussathi.R
import com.karroh.bussathi.data.repository.AuthRepository
import com.karroh.bussathi.ui.auth.LoginActivity
import com.karroh.bussathi.ui.consent.ConsentDialog
import com.karroh.bussathi.ui.main.MainActivity
import com.karroh.bussathi.util.ConsentManager

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
        if (isFinishing || isDestroyed) return

        // First launch: ask for research data consent before moving on
        if (!ConsentManager.getInstance(applicationContext).hasAnsweredConsent()) {
            ConsentDialog.show(this) { navigateToNextScreen() }
            return
        }

        val intent = if (authRepository.isUserLoggedIn()) {
            Intent(this, MainActivity::class.java)
        } else {
            Intent(this, LoginActivity::class.java)
        }
        
        startActivity(intent)
        finish()
    }
}
