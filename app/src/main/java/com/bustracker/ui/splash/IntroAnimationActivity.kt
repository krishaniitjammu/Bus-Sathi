package com.karroh.bussathi.ui.splash // Make sure this matches your actual package folder!

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.karroh.bussathi.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class IntroAnimationActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_intro_animation)

        // Wait for 2.5 seconds (2500 milliseconds) for the Lottie animation to finish
        lifecycleScope.launch {
            delay(2500)

            // Move to your existing Splash Screen!
            val intent = Intent(this@IntroAnimationActivity, SplashActivity::class.java)
            startActivity(intent)

            // Close this animation screen so the user can't press 'Back' to see it again
            finish()
        }
    }
}