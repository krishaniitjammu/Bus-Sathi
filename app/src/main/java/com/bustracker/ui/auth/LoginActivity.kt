package com.karroh.bussathi.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope

import com.karroh.bussathi.data.repository.AuthRepository
import com.karroh.bussathi.databinding.ActivityLoginBinding
import com.karroh.bussathi.ui.main.MainActivity
import com.karroh.bussathi.R
import kotlinx.coroutines.launch

/**
 * Login screen for bus drivers
 */
class LoginActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityLoginBinding
    private val authRepository = AuthRepository()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Check if user is already logged in
        if (authRepository.isUserLoggedIn()) {
            navigateToMain()
            return
        }

        // Prefill email if returned from registration
        val prefillEmail = intent?.getStringExtra("email")
        if (!prefillEmail.isNullOrEmpty()) {
            binding.etEmail.setText(prefillEmail)
        }
        
        setupListeners()
    }
    
    private fun setupListeners() {
        binding.btnLogin.setOnClickListener {
            it.startAnimation(android.view.animation.AnimationUtils.loadAnimation(this, R.anim.button_click))
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()
            
            if (validateInput(email, password)) {
                performLogin(email, password)
            }
        }

        binding.tvCreateAccount.setOnClickListener {
            val intent = android.content.Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }
    }
    
    private fun validateInput(email: String, password: String): Boolean {
        var isValid = true
        
        if (email.isEmpty()) {
            binding.tilEmail.error = getString(R.string.email_required)
            isValid = false
        } else {
            binding.tilEmail.error = null
        }
        
        if (password.isEmpty()) {
            binding.tilPassword.error = getString(R.string.password_required)
            isValid = false
        } else {
            binding.tilPassword.error = null
        }
        
        return isValid
    }
    
    private fun performLogin(email: String, password: String) {
        showLoading(true)
        
        lifecycleScope.launch {
            val result = authRepository.signIn(email, password)
            
            result.onSuccess {
                showLoading(false)
                navigateToMain()
            }.onFailure { exception ->
                showLoading(false)
                showError(exception.message ?: getString(R.string.login_error))
            }
        }
    }
    
    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnLogin.isEnabled = !isLoading
        binding.etEmail.isEnabled = !isLoading
        binding.etPassword.isEnabled = !isLoading
        
        if (isLoading) {
            binding.btnLogin.text = getString(R.string.logging_in)
        } else {
            binding.btnLogin.text = getString(R.string.login_button)
        }
    }
    
    private fun showError(message: String) {
        binding.tvError.text = message
        binding.tvError.visibility = View.VISIBLE
        
        // Add shake animation to login button
        val shakeAnimation = android.view.animation.AnimationUtils.loadAnimation(this, R.anim.shake)
        binding.btnLogin.startAnimation(shakeAnimation)
        
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
    
    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}
