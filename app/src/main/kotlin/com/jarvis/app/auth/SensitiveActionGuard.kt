package com.jarvis.app.auth

/**
 * Guards high-risk actions requiring explicit biometric authentication or confirmation.
 */
class SensitiveActionGuard(private val authManager: OwnerAuthManager) {

    private val sensitiveIntents = setOf(
        "send_message",
        "make_call",
        "purchase",
        "change_password",
        "change_security",
        "delete_data",
        "share_private"
    )

    /**
     * Checks if the specified action intent is considered sensitive.
     */
    fun isSensitiveAction(intent: String): Boolean {
        return sensitiveIntents.contains(intent.lowercase().trim())
    }

    /**
     * Evaluates whether an action can proceed based on sensitivity and owner authentication status.
     *
     * @param intent The identifier or name of the action/intent.
     * @param requireAuth Whether biometric authentication should be enforced for sensitive actions.
     * @return AuthCheckResult indicating if the action is ALLOWED, requires AUTH_REQUIRED, or CONFIRMATION_NEEDED.
     */
    fun canProceed(intent: String, requireAuth: Boolean = true): AuthCheckResult {
        if (!isSensitiveAction(intent)) {
            return AuthCheckResult.ALLOWED
        }

        if (requireAuth && !authManager.isAuthValid()) {
            return AuthCheckResult.AUTH_REQUIRED
        }

        return AuthCheckResult.CONFIRMATION_NEEDED
    }

    enum class AuthCheckResult {
        ALLOWED,
        AUTH_REQUIRED,
        CONFIRMATION_NEEDED
    }
}
