package com.inscopelabs.sfm.security.session

import com.inscopelabs.sfm.security.permissions.Capability
import java.security.SecureRandom
import java.util.concurrent.TimeUnit

/**
 * Manages MCP session lifecycle, creation, validation, and expiration.
 */
class SessionManager {

    private val sessions = mutableMapOf<String, Session>()
    private val sessionLock = Any()
    private val secureRandom = SecureRandom()

    /**
     * Creates a new session with specified capabilities.
     */
    fun createSession(
        userId: String,
        capabilities: Set<Capability>,
        ttlMinutes: Long = DEFAULT_SESSION_TTL_MINUTES
    ): Session {
        val sessionId = generateSessionId()
        val now = System.currentTimeMillis()

        val session = Session(
            sessionId = sessionId,
            userId = userId,
            capabilities = capabilities,
            createdAt = now,
            lastAccessedAt = now,
            expiresAt = now + TimeUnit.MINUTES.toMillis(ttlMinutes),
            isActive = true
        )

        synchronized(sessionLock) {
            sessions[sessionId] = session
        }

        return session
    }

    /**
     * Validates a session and returns it if valid.
     */
    fun validateSession(sessionId: String): SessionValidationResult {
        synchronized(sessionLock) {
            val session = sessions[sessionId]
                ?: return SessionValidationResult.Invalid("Session not found")

            if (!session.isActive) {
                return SessionValidationResult.Invalid("Session is inactive")
            }

            if (System.currentTimeMillis() > session.expiresAt) {
                session.isActive = false
                return SessionValidationResult.Expired("Session has expired")
            }

            // Update last accessed time
            session.lastAccessedAt = System.currentTimeMillis()

            return SessionValidationResult.Valid(session)
        }
    }

    /**
     * Extends a session's TTL.
     */
    fun extendSession(sessionId: String, additionalMinutes: Long): Boolean {
        synchronized(sessionLock) {
            val session = sessions[sessionId]
                ?: return false

            if (!session.isActive) return false

            session.expiresAt = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(additionalMinutes)
            session.lastAccessedAt = System.currentTimeMillis()

            return true
        }
    }

    /**
     * Revokes a session immediately.
     */
    fun revokeSession(sessionId: String): Boolean {
        synchronized(sessionLock) {
            val session = sessions[sessionId]
                ?: return false

            session.isActive = false
            session.revokedAt = System.currentTimeMillis()

            return true
        }
    }

    /**
     * Revokes all sessions for a user.
     */
    fun revokeAllUserSessions(userId: String): Int {
        var count = 0
        synchronized(sessionLock) {
            sessions.values.filter { it.userId == userId }.forEach { session ->
                session.isActive = false
                session.revokedAt = System.currentTimeMillis()
                count++
            }
        }
        return count
    }

    /**
     * Gets session info without validation.
     */
    fun getSessionInfo(sessionId: String): Session? {
        synchronized(sessionLock) {
            return sessions[sessionId]
        }
    }

    /**
     * Checks if a session has a specific capability.
     */
    fun hasCapability(sessionId: String, capability: Capability): Boolean {
        val result = validateSession(sessionId)
        return when (result) {
            is SessionValidationResult.Valid -> capability in result.session.capabilities
            else -> false
        }
    }

    /**
     * Gets all active sessions.
     */
    fun getActiveSessions(): List<Session> {
        synchronized(sessionLock) {
            return sessions.values.filter { it.isActive }.toList()
        }
    }

    /**
     * Cleans up expired sessions.
     */
    fun cleanupExpiredSessions(): Int {
        var count = 0
        val now = System.currentTimeMillis()

        synchronized(sessionLock) {
            val iterator = sessions.iterator()
            while (iterator.hasNext()) {
                val entry = iterator.next()
                val session = entry.value
                if (session.expiresAt < now) {
                    session.isActive = false
                    count++
                }
            }
        }

        return count
    }

    /**
     * Gets all sessions for a user.
     */
    fun getUserSessions(userId: String): List<Session> {
        synchronized(sessionLock) {
            return sessions.values.filter { it.userId == userId }.toList()
        }
    }

    private fun generateSessionId(): String {
        val bytes = ByteArray(SESSION_ID_LENGTH)
        secureRandom.nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }

    sealed class SessionValidationResult {
        data class Valid(val session: Session) : SessionValidationResult()
        data class Invalid(val reason: String) : SessionValidationResult()
        data class Expired(val reason: String) : SessionValidationResult()
    }

    companion object {
        private const val SESSION_ID_LENGTH = 32
        private const val DEFAULT_SESSION_TTL_MINUTES = 60L
    }
}

/**
 * Represents an active MCP session.
 */
data class Session(
    val sessionId: String,
    val userId: String,
    val capabilities: Set<Capability>,
    val createdAt: Long,
    var lastAccessedAt: Long,
    var expiresAt: Long,
    var isActive: Boolean,
    var revokedAt: Long? = null,
    val metadata: MutableMap<String, String> = mutableMapOf()
) {
    fun isExpired(): Boolean = System.currentTimeMillis() > expiresAt

    fun remainingTtlMillis(): Long = maxOf(0, expiresAt - System.currentTimeMillis())

    fun isCapabilityGranted(capability: Capability): Boolean = capability in capabilities
}
