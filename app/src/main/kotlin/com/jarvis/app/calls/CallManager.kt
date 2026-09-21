package com.jarvis.app.calls

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.core.content.ContextCompat

/**
 * Manages phone calls and dialer interactions for the JARVIS application.
 */
class CallManager(private val context: Context) {

    /**
     * Checks if the CALL_PHONE runtime permission is granted.
     */
    fun isCallPermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CALL_PHONE
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Directly initiates an outgoing phone call to the specified phone number.
     * Requires Manifest.permission.CALL_PHONE.
     *
     * @param phoneNumber The phone number to dial.
     * @return True if the call intent was successfully launched, false if permission was missing or an error occurred.
     */
    fun makeCall(phoneNumber: String): Boolean {
        if (!isCallPermissionGranted()) {
            return false
        }

        return try {
            val callIntent = Intent(Intent.ACTION_CALL).apply {
                data = Uri.parse("tel:${Uri.encode(phoneNumber)}")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(callIntent)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Opens the system dialer with the phone number pre-filled.
     * Does not require CALL_PHONE permission.
     *
     * @param phoneNumber The phone number to prefill in the dialer.
     */
    fun openDialer(phoneNumber: String) {
        try {
            val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:${Uri.encode(phoneNumber)}")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(dialIntent)
        } catch (e: Exception) {
            // Dialer activity not available
        }
    }
}
