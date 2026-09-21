package com.jarvis.app.auth

import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manages biometric and device credential authentication for the device owner.
 * Caches authentication state with a configurable timeout.
 */
class OwnerAuthManager {

    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    private var lastAuthTime: Long = 0L
    private val AUTH_TIMEOUT = 5 * 60 * 1000L // 5 minutes

    /**
     * Launches the biometric prompt to authenticate the user.
     *
     * @param activity FragmentActivity required to host BiometricPrompt.
     * @param onResult Callback receiving true if authentication succeeds, false otherwise.
     */
    fun authenticate(activity: FragmentActivity, onResult: (Boolean) -> Unit) {
        val executor = ContextCompat.getMainExecutor(activity)

        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                _isAuthenticated.value = true
                lastAuthTime = System.currentTimeMillis()
                onResult(true)
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                onResult(false)
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                // Biometric not recognized, user can try again
            }
        }

        val biometricPrompt = BiometricPrompt(activity, executor, callback)

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("JARVIS Owner Verification")
            .setSubtitle("Authenticate to authorize sensitive operations")
            .setAllowedAuthenticators(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)
            .build()

        biometricPrompt.authenticate(promptInfo)
    }

    /**
     * Checks if the user is currently authenticated and within the valid timeout window.
     */
    fun isAuthValid(): Boolean {
        val now = System.currentTimeMillis()
        if (_isAuthenticated.value && (now - lastAuthTime < AUTH_TIMEOUT)) {
            return true
        }
        if (_isAuthenticated.value) {
            _isAuthenticated.value = false
        }
        return false
    }

    /**
     * Revokes the current authenticated session.
     */
    fun deauthenticate() {
        _isAuthenticated.value = false
        lastAuthTime = 0L
    }
}
