package com.karroh.bussathi.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.karroh.bussathi.R
import com.karroh.bussathi.data.repository.AuthRepository
import com.karroh.bussathi.databinding.ActivityRegisterBinding
import com.karroh.bussathi.ui.main.MainActivity
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

            // 1. Get Text Inputs
            val name = binding.etName.text.toString().trim()
            val email = binding.etEmailReg.text.toString().trim()
            val password = binding.etPasswordReg.text.toString().trim()
            val phone = binding.etPhone.text.toString().trim()
            val vehicle = binding.etVehicle.text.toString().trim()
            val license = binding.etLicense.text.toString().trim()

            // 2. Get Spinner Selections
            // These IDs must match your activity_signup.xml (spinnerCity, spinnerCapacity)
            val region = binding.spinnerCity.selectedItem.toString()
            val capacity = binding.spinnerCapacity.selectedItem.toString()

            // 3. Validate and Create
            if (validateInput(name, email, password, phone, vehicle, license, region, capacity)) {
                createAccount(name, email, password, phone, vehicle, license, region, capacity)
            }
        }
    }

    // Updated validation to include Region and Capacity
    private fun validateInput(
        name: String,
        email: String,
        password: String,
        phone: String,
        vehicle: String,
        license: String,
        region: String,
        capacity: String
    ): Boolean {
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

        // Validate Spinners (Ensure user didn't leave it on "Select City")
        if (region == "Select City") {
            showError("Please select a valid City/Region")
            isValid = false
        }

        if (capacity == "Select Vehicle Capacity") {
            showError("Please select your vehicle capacity")
            isValid = false
        }

        return isValid
    }

    private fun createAccount(
        name: String,
        email: String,
        password: String,
        phone: String,
        vehicle: String,
        license: String,
        region: String,
        capacity: String
    ) {
        showLoading(true)

        lifecycleScope.launch {
            // IMPORTANT: You must update your AuthRepository to accept these new arguments!
            val result = authRepository.createAccount(
                email,
                password,
                name,
                phone,
                vehicle,
                license,
                region,    // Passing Region to Repository
                capacity   // Passing Capacity to Repository
            )

            result.onSuccess {
                showLoading(false)
                authRepository.signOut()

                Toast.makeText(
                    this@RegisterActivity,
                    getString(R.string.registration_success),
                    Toast.LENGTH_LONG
                ).show()

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
        // Also disable spinners while loading
        binding.spinnerCity.isEnabled = !isLoading
        binding.spinnerCapacity.isEnabled = !isLoading

        if (isLoading) {
            binding.btnCreateAccount.text = getString(R.string.registering)
        } else {
            binding.btnCreateAccount.text = getString(R.string.create_account_button)
        }
    }

    private fun showError(message: String) {
        binding.tvRegError.text = message
        binding.tvRegError.visibility = View.VISIBLE
        // Optional: Hide after 3 seconds or keep visible
    }
}