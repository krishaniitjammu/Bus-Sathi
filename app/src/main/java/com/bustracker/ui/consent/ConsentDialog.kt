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
     * Show the popup. Consent is saved before [onAccepted] is called.
     * Declining offers to review again or exit the app.
     */
    fun show(activity: AppCompatActivity, onAccepted: () -> Unit): AlertDialog {
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
            onAccepted()
        }

        // Consent is mandatory: declining is not saved, so the popup shows again next launch
        binding.btnConsentDecline.setOnClickListener {
            AlertDialog.Builder(activity)
                .setTitle(R.string.consent_required_title)
                .setMessage(R.string.consent_required_message)
                .setCancelable(false)
                .setPositiveButton(R.string.consent_required_review, null)
                .setNegativeButton(R.string.consent_required_exit) { _, _ ->
                    dialog.dismiss()
                    activity.finishAffinity()
                }
                .show()
        }

        dialog.show()
        return dialog
    }
}
