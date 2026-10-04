package com.karroh.bussathi.ui.consent

import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.karroh.bussathi.R
import com.karroh.bussathi.databinding.DialogConsentBinding
import com.karroh.bussathi.util.ConsentManager

/**
 * Privacy and consent popup asking the user to allow their data to be used for research
 */
object ConsentDialog {

    /**
     * Show the popup. The choice is saved before [onAnswered] is called.
     */
    fun show(activity: AppCompatActivity, onAnswered: (granted: Boolean) -> Unit): AlertDialog {
        val binding = DialogConsentBinding.inflate(activity.layoutInflater)
        val consentManager = ConsentManager.getInstance(activity.applicationContext)

        val dialog = AlertDialog.Builder(activity)
            .setView(binding.root)
            .setCancelable(false) // The user must pick Accept or Decline
            .create()

        binding.tvConsentDetailsToggle.setOnClickListener {
            val expand = binding.tvConsentDetails.visibility != View.VISIBLE
            binding.tvConsentDetails.visibility = if (expand) View.VISIBLE else View.GONE
            binding.tvConsentDetailsToggle.text = activity.getString(
                if (expand) R.string.consent_details_hide else R.string.consent_details_show
            )
        }

        binding.btnConsentAccept.setOnClickListener {
            consentManager.setResearchConsent(true)
            dialog.dismiss()
            onAnswered(true)
        }

        binding.btnConsentDecline.setOnClickListener {
            consentManager.setResearchConsent(false)
            dialog.dismiss()
            onAnswered(false)
        }

        dialog.show()
        return dialog
    }
}
