package com.inscopelabs.sfm.mcp.security

import com.inscopelabs.sfm.core.exception.ReplayAttackException
import com.inscopelabs.sfm.core.exception.TokenValidationException
import com.inscopelabs.sfm.mcp.server.MCPToolInvocation
import com.inscopelabs.sfm.mcp.server.RequestValidation
import java.util.concurrent.ConcurrentHashMap

import com.inscopelabs.sfm.security.session.SessionManager

/**
 * Validates MCP requests for security.
 */
class MCPSecurityGuard(
    private val sessionManager: SessionManager = SessionManager(),
    private val validation: RequestValidation = RequestValidation()
) {
    private val usedNonces = ConcurrentHashMap<String, Long>()
    private val requestTimestamps = ConcurrentHashMap<String, Long>()

    /**
     * Validates an MCP tool invocation.
     */
    fun validateRequest(invocation: MCPToolInvocation): ValidationResult {
        val errors = mutableListOf<String>()

        // Validate timestamp
        if (validation.validateTimestamp) {
            val timestampValidation = validateTimestamp(invocation.timestamp)
            if (!timestampValidation.isValid()) {
                errors.add((timestampValidation as ValidationResult.Invalid).error)
            }
        }

        // Validate nonce (replay protection)
        if (validation.validateNonce && !validation.allowReplay) {
            val nonceValidation = validateNonce(invocation.requestId)
            if (!nonceValidation.isValid()) {
                errors.add((nonceValidation as ValidationResult.Invalid).error)
            }
        }

        // Validate capability token
        if (validation.requireCapabilityToken) {
            val tokenValidation = validateCapabilityToken(invocation.capabilityToken)
            if (!tokenValidation.isValid()) {
                errors.add((tokenValidation as ValidationResult.Invalid).error)
            }
        }

        // Validate session
        if (validation.validateSession) {
            val sessionValidation = validateSession(invocation.sessionId)
            if (!sessionValidation.isValid()) {
                errors.add((sessionValidation as ValidationResult.Invalid).error)
            }
        }

        return if (errors.isEmpty()) {
            ValidationResult.Valid
        } else {
            ValidationResult.Invalid(errors.joinToString("; "))
        }
    }

    /**
     * Validates request timestamp is recent.
     */
    private fun validateTimestamp(timestamp: Long): ValidationResult {
        val now = System.currentTimeMillis()
        val age = now - timestamp

        if (age < 0) {
            return ValidationResult.Invalid("Timestamp is in the future")
        }

        if (age > validation.maxTimestampAgeSeconds * 1000) {
            return ValidationResult.Invalid("Timestamp is too old")
        }

        return ValidationResult.Valid
    }

    /**
     * Validates nonce hasn't been used (replay protection).
     */
    private fun validateNonce(requestId: String): ValidationResult {
        val now = System.currentTimeMillis()

        // Check if nonce was recently used
        usedNonces[requestId]?.let { lastUsed ->
            if (now - lastUsed < NONCE_TTL_MS) {
                return ValidationResult.Invalid("Nonce has been used (replay attack)")
            }
        }

        // Record this nonce
        usedNonces[requestId] = now

        // Clean up old nonces
        cleanupOldNonces(now)

        return ValidationResult.Valid
    }

    /**
     * Validates capability token.
     * Capability tokens are removed from trust decisions; authentication and permissions rely on active Sessions.
     */
    private fun validateCapabilityToken(token: String?): ValidationResult {
        if (token == null && validation.requireCapabilityToken) {
            return ValidationResult.Invalid("Capability token is required")
        }
        return ValidationResult.Valid
    }

    /**
     * Validates session ID against SessionManager.
     */
    private fun validateSession(sessionId: String): ValidationResult {
        if (sessionId.isBlank()) {
            return ValidationResult.Invalid("Session ID is required")
        }

        return when (val result = sessionManager.validateSession(sessionId)) {
            is SessionManager.SessionValidationResult.Valid -> ValidationResult.Valid
            is SessionManager.SessionValidationResult.Invalid -> ValidationResult.Invalid(result.reason)
            is SessionManager.SessionValidationResult.Expired -> ValidationResult.Invalid(result.reason)
        }
    }

    /**
     * Cleans up expired nonces.
     */
    private fun cleanupOldNonces(now: Long) {
        val iterator = usedNonces.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (now - entry.value > NONCE_TTL_MS * 2) {
                iterator.remove()
            }
        }
    }

    sealed class ValidationResult {
        object Valid : ValidationResult()
        data class Invalid(val error: String) : ValidationResult()

        fun isValid(): Boolean = this is Valid
    }

    companion object {
        private const val NONCE_TTL_MS = 5 * 60 * 1000L // 5 minutes
        private const val MIN_TOKEN_LENGTH = 16
        private const val MIN_SESSION_ID_LENGTH = 8
    }
}
