package com.bustracker.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bustracker.R
import com.bustracker.data.repository.AuthRepository
import com.bustracker.databinding.ActivityRegisterBinding
import com.bustracker.ui.main.MainActivity
import kotlinx.coroutines.launch

/**
 * Registration screen for new drivers
 */
class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private val authRepository = AuthRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupListeners()
    }

    private fun setupListeners() {
        binding.btnCreateAccount.setOnClickListener {
            it.startAnimation(android.view.animation.AnimationUtils.loadAnimation(this, R.anim.button_click))
            val name = binding.etName.text.toString().trim()
            val email = binding.etEmailReg.text.toString().trim()
            val password = binding.etPasswordReg.text.toString().trim()
            val phone = binding.etPhone.text.toString().trim()
            val vehicle = binding.etVehicle.text.toString().trim()
            val license = binding.etLicense.text.toString().trim()

            if (validateInput(name, email, password, phone, vehicle, license)) {
                createAccount(name, email, password, phone, vehicle, license)
            }
        }
    }

    private fun validateInput(name: String, email: String, password: String, phone: String, vehicle: String, license: String): Boolean {
        var isValid = true

        if (name.isEmpty()) {
            binding.tilName.error = getString(R.string.name_required)
            isValid = false
        } else {
            binding.tilName.error = null
        }

        if (email.isEmpty()) {
            binding.tilEmailReg.error = getString(R.string.email_required)
            isValid = false
        } else {
            binding.tilEmailReg.error = null
        }

        if (password.isEmpty()) {
            binding.tilPasswordReg.error = getString(R.string.password_required)
            isValid = false
        } else {
            binding.tilPasswordReg.error = null
        }
        
        if (phone.isEmpty()) {
            binding.tilPhone.error = "Phone number is required"
            isValid = false
        } else {
            binding.tilPhone.error = null
        }
        
        if (vehicle.isEmpty()) {
            binding.tilVehicle.error = "Vehicle/Bus number is required"
            isValid = false
        } else {
            binding.tilVehicle.error = null
        }
        
        if (license.isEmpty()) {
            binding.tilLicense.error = "License number is required"
            isValid = false
        } else {
            binding.tilLicense.error = null
        }

        return isValid
    }

    private fun createAccount(
        name: String,
        email: String,
        password: String,
        phone: String,
        vehicle: String,
        license: String
    ) {
        showLoading(true)

        lifecycleScope.launch {
            val result = authRepository.createAccount(email, password, name, phone, vehicle, license)

            result.onSuccess {
                showLoading(false)
                // After account creation, sign out so the driver can sign in manually
                authRepository.signOut()

                Toast.makeText(
                    this@RegisterActivity,
                    getString(R.string.registration_success),
                    Toast.LENGTH_LONG
                ).show()

                // Return to LoginActivity and prefill email
                val intent = Intent(this@RegisterActivity, LoginActivity::class.java)
                intent.putExtra("email", email)
                startActivity(intent)
                finish()
            }.onFailure { exception ->
                showLoading(false)
                showError(exception.message ?: getString(R.string.registration_error))
            }
        }
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBarReg.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnCreateAccount.isEnabled = !isLoading
        binding.etName.isEnabled = !isLoading
        binding.etEmailReg.isEnabled = !isLoading
        binding.etPasswordReg.isEnabled = !isLoading
        binding.etPhone.isEnabled = !isLoading
        binding.etVehicle.isEnabled = !isLoading
        binding.etLicense.isEnabled = !isLoading

        if (isLoading) {
            binding.btnCreateAccount.text = getString(R.string.registering)
        } else {
            binding.btnCreateAccount.text = getString(R.string.create_account_button)
        }
    }

    private fun showError(message: String) {
        binding.tvRegError.text = message
        binding.tvRegError.visibility = View.VISIBLE
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}