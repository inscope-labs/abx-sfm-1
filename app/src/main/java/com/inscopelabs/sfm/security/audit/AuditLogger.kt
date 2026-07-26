package com.inscopelabs.sfm.security.audit

import com.inscopelabs.sfm.data.AuditDao
import com.inscopelabs.sfm.data.AuditEntryEntity
import com.inscopelabs.sfm.security.encryption.EncryptionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Logs all security-relevant operations for audit and compliance.
 */
class AuditLogger(
    private val auditDao: AuditDao? = null,
    private val encryptionManager: EncryptionManager? = null
) {

    private val logQueue = ConcurrentLinkedQueue<AuditEntry>()
    private val isEnabled = AtomicBoolean(true)
    private val signedEntries = AtomicBoolean(true)
    private val ioScope = CoroutineScope(Dispatchers.IO)

    @Volatile
    private var lastSignature: String? = null

    init {
        // Hydrate last signature from persisted storage if available
        if (auditDao != null) {
            try {
                runBlocking {
                    val lastEntry = auditDao.getLastEntry()
                    lastSignature = lastEntry?.signature
                }
            } catch (e: Exception) {
                // Fallback to null
            }
        }
    }

    /**
     * Logs an audit entry.
     */
    fun log(entry: AuditEntry) {
        if (!isEnabled.get()) return

        val prevSig = lastSignature ?: GENESIS_SIGNATURE
        val entryWithPrev = entry.copy(previousSignature = prevSig)

        val signedEntry = if (signedEntries.get()) {
            val sig = calculateSignature(entryWithPrev)
            entryWithPrev.copy(signature = sig)
        } else {
            entryWithPrev
        }

        lastSignature = signedEntry.signature
        logQueue.offer(signedEntry)

        // Trigger async persistence
        persistEntry(signedEntry)
    }

    /**
     * Logs a file operation.
     */
    fun logFileOperation(
        sessionId: String?,
        operation: String,
        resource: String,
        result: OperationResult,
        userApprovalState: UserApprovalState? = null,
        elapsedMs: Long = 0
    ) {
        log(
            AuditEntry(
                timestamp = System.currentTimeMillis(),
                sessionId = sessionId,
                eventType = AuditEventType.FILE_OPERATION,
                operation = operation,
                resource = resource,
                result = result,
                userApprovalState = userApprovalState,
                elapsedMs = elapsedMs
            )
        )
    }

    /**
     * Logs an authentication event.
     */
    fun logAuthentication(
        sessionId: String?,
        userId: String?,
        success: Boolean,
        method: String,
        failureReason: String? = null
    ) {
        log(
            AuditEntry(
                timestamp = System.currentTimeMillis(),
                sessionId = sessionId,
                eventType = AuditEventType.AUTHENTICATION,
                operation = if (success) "auth_success" else "auth_failure",
                resource = userId ?: "unknown",
                result = if (success) OperationResult.ALLOWED else OperationResult.DENIED,
                details = failureReason?.let { mapOf("reason" to it) },
                method = method
            )
        )
    }

    /**
     * Logs an authorization event.
     */
    fun logAuthorization(
        sessionId: String,
        requiredCapability: String,
        granted: Boolean,
        resource: String
    ) {
        log(
            AuditEntry(
                timestamp = System.currentTimeMillis(),
                sessionId = sessionId,
                eventType = AuditEventType.AUTHORIZATION,
                operation = "capability_check",
                resource = resource,
                result = if (granted) OperationResult.ALLOWED else OperationResult.DENIED,
                details = mapOf(
                    "required_capability" to requiredCapability,
                    "granted" to granted.toString()
                )
            )
        )
    }

    /**
     * Logs a policy violation.
     */
    fun logPolicyViolation(
        sessionId: String?,
        policyName: String,
        ruleDescription: String,
        resource: String
    ) {
        log(
            AuditEntry(
                timestamp = System.currentTimeMillis(),
                sessionId = sessionId,
                eventType = AuditEventType.POLICY_VIOLATION,
                operation = "policy_check",
                resource = resource,
                result = OperationResult.DENIED,
                details = mapOf(
                    "policy_name" to policyName,
                    "rule" to ruleDescription
                )
            )
        )
    }

    /**
     * Logs a security warning.
     */
    fun logSecurityWarning(
        warningType: SecurityWarningType,
        details: Map<String, String>
    ) {
        log(
            AuditEntry(
                timestamp = System.currentTimeMillis(),
                sessionId = null,
                eventType = AuditEventType.SECURITY_WARNING,
                operation = warningType.name.lowercase(),
                resource = "system",
                result = OperationResult.WARNING,
                details = details
            )
        )
    }

    /**
     * Logs a session event.
     */
    fun logSessionEvent(
        sessionId: String,
        event: SessionEventType,
        userId: String? = null
    ) {
        log(
            AuditEntry(
                timestamp = System.currentTimeMillis(),
                sessionId = sessionId,
                eventType = AuditEventType.SESSION,
                operation = event.name.lowercase(),
                resource = userId ?: "unknown",
                result = OperationResult.SUCCESS
            )
        )
    }

    /**
     * Gets recent audit entries.
     */
    fun getRecentEntries(count: Int = 100): List<AuditEntry> {
        if (auditDao != null) {
            try {
                val dbEntries = runBlocking { auditDao.getRecentEntries(count) }
                if (dbEntries.isNotEmpty()) {
                    return dbEntries.reversed().map { it.toAuditEntry() }
                }
            } catch (e: Exception) {
                // Fallback to queue
            }
        }
        return logQueue.toList().takeLast(count)
    }

    /**
     * Gets entries for a specific session.
     */
    fun getSessionEntries(sessionId: String): List<AuditEntry> {
        if (auditDao != null) {
            try {
                val dbEntries = runBlocking { auditDao.getSessionEntries(sessionId) }
                if (dbEntries.isNotEmpty()) {
                    return dbEntries.map { it.toAuditEntry() }
                }
            } catch (e: Exception) {
                // Fallback to queue
            }
        }
        return logQueue.filter { it.sessionId == sessionId }
    }

    /**
     * Queries entries by criteria.
     */
    fun queryEntries(criteria: AuditQuery): List<AuditEntry> {
        val allEntries = if (auditDao != null) {
            try {
                runBlocking { auditDao.getAllEntries() }.map { it.toAuditEntry() }
            } catch (e: Exception) {
                logQueue.toList()
            }
        } else {
            logQueue.toList()
        }

        return allEntries.filter { entry ->
            var matches = true

            if (criteria.startTime != null && entry.timestamp < criteria.startTime) {
                matches = false
            }
            if (criteria.endTime != null && entry.timestamp > criteria.endTime) {
                matches = false
            }
            if (criteria.sessionId != null && entry.sessionId != criteria.sessionId) {
                matches = false
            }
            if (criteria.eventType != null && entry.eventType != criteria.eventType) {
                matches = false
            }
            if (criteria.result != null && entry.result != criteria.result) {
                matches = false
            }
            if (criteria.resource != null && !entry.resource.contains(criteria.resource)) {
                matches = false
            }

            matches
        }
    }

    /**
     * Verifies audit log integrity.
     */
    fun verifyIntegrity(): IntegrityResult {
        val entries = if (auditDao != null) {
            try {
                runBlocking { auditDao.getAllEntries() }.map { it.toAuditEntry() }
            } catch (e: Exception) {
                logQueue.toList()
            }
        } else {
            logQueue.toList()
        }

        var expectedPreviousSig = GENESIS_SIGNATURE

        for (entry in entries) {
            if (signedEntries.get() && entry.signature == null) {
                return IntegrityResult.INVALID("Missing signature at entry ${entry.timestamp}")
            }

            if (entry.previousSignature != expectedPreviousSig) {
                return IntegrityResult.INVALID("Chain broken at entry ${entry.timestamp}: expected $expectedPreviousSig, got ${entry.previousSignature}")
            }

            if (signedEntries.get()) {
                val expectedSig = calculateSignature(entry)
                if (expectedSig != entry.signature) {
                    return IntegrityResult.INVALID("Signature mismatch at entry ${entry.timestamp}")
                }
            }

            expectedPreviousSig = entry.signature ?: GENESIS_SIGNATURE
        }

        return IntegrityResult.VALID
    }

    /**
     * Enables or disables audit logging.
     */
    fun setEnabled(enabled: Boolean) {
        isEnabled.set(enabled)
    }

    /**
     * Enables or disables signature signing.
     */
    fun setSignedEntries(signed: Boolean) {
        signedEntries.set(signed)
    }

    private fun calculateSignature(entry: AuditEntry): String {
        val data = "${entry.timestamp}|${entry.sessionId}|${entry.eventType}|${entry.operation}|${entry.resource}|${entry.result}|${entry.previousSignature ?: GENESIS_SIGNATURE}"
        val dataBytes = data.toByteArray(Charsets.UTF_8)
        return encryptionManager?.calculateHmac(dataBytes) ?: calculateHmacFallback(dataBytes)
    }

    private fun calculateHmacFallback(data: ByteArray): String {
        val mac = javax.crypto.Mac.getInstance("HmacSHA256")
        val secretKey = javax.crypto.spec.SecretKeySpec("AUDIT_LOG_DEFAULT_HMAC_KEY".toByteArray(Charsets.UTF_8), "HmacSHA256")
        mac.init(secretKey)
        return mac.doFinal(data).joinToString("") { "%02x".format(it) }
    }

    private fun persistEntry(entry: AuditEntry) {
        if (auditDao == null) return
        ioScope.launch {
            try {
                val entity = entry.toEntity()
                auditDao.insert(entity)
                // Retention policy enforcement: keep last MAX_RETENTION_ENTRIES
                auditDao.pruneOldEntries(MAX_RETENTION_ENTRIES)
            } catch (e: Exception) {
                // Exception handled
            }
        }
    }

    private fun AuditEntry.toEntity(): AuditEntryEntity {
        return AuditEntryEntity(
            timestamp = timestamp,
            sessionId = sessionId,
            eventType = eventType.name,
            operation = operation,
            resource = resource,
            result = result.name,
            userApprovalState = userApprovalState?.name,
            elapsedMs = elapsedMs,
            detailsJson = details?.toString(),
            method = method,
            signature = signature,
            previousSignature = previousSignature
        )
    }

    private fun AuditEntryEntity.toAuditEntry(): AuditEntry {
        return AuditEntry(
            timestamp = timestamp,
            sessionId = sessionId,
            eventType = try { AuditEventType.valueOf(eventType) } catch (e: Exception) { AuditEventType.FILE_OPERATION },
            operation = operation,
            resource = resource,
            result = try { OperationResult.valueOf(result) } catch (e: Exception) { OperationResult.SUCCESS },
            userApprovalState = userApprovalState?.let { try { UserApprovalState.valueOf(it) } catch (e: Exception) { null } },
            elapsedMs = elapsedMs,
            details = detailsJson?.let { mapOf("details" to it) },
            method = method,
            signature = signature,
            previousSignature = previousSignature
        )
    }

    data class AuditQuery(
        val startTime: Long? = null,
        val endTime: Long? = null,
        val sessionId: String? = null,
        val eventType: AuditEventType? = null,
        val result: OperationResult? = null,
        val resource: String? = null
    )

    sealed class IntegrityResult {
        object VALID : IntegrityResult()
        data class INVALID(val reason: String) : IntegrityResult()
    }

    companion object {
        const val GENESIS_SIGNATURE = "0000000000000000000000000000000000000000000000000000000000000000"
        const val MAX_RETENTION_ENTRIES = 10000
    }
}

/**
 * Represents an audit log entry.
 */
data class AuditEntry(
    val timestamp: Long,
    val sessionId: String?,
    val eventType: AuditEventType,
    val operation: String,
    val resource: String,
    val result: OperationResult,
    val userApprovalState: UserApprovalState? = null,
    val elapsedMs: Long = 0,
    val details: Map<String, String>? = null,
    val method: String? = null,
    val signature: String? = null,
    val previousSignature: String? = null
)

enum class AuditEventType {
    FILE_OPERATION,
    AUTHENTICATION,
    AUTHORIZATION,
    POLICY_VIOLATION,
    SECURITY_WARNING,
    SESSION,
    PLUGIN_EVENT,
    NETWORK_EVENT
}

enum class OperationResult {
    ALLOWED,
    DENIED,
    SUCCESS,
    FAILURE,
    WARNING,
    TIMEOUT
}

enum class UserApprovalState {
    NOT_REQUIRED,
    APPROVED_ONCE,
    APPROVED_SESSION,
    APPROVED_ALWAYS,
    DENIED
}

enum class SessionEventType {
    CREATED,
    VALIDATED,
    EXTENDED,
    EXPIRED,
    REVOKED,
    REJECTED
}

enum class SecurityWarningType {
    ROOT_DETECTED,
    DEBUGGER_ATTACHED,
    SUSPICIOUS_ACTIVITY,
    RATE_LIMIT_EXCEEDED,
    REPLAY_ATTACK_DETECTED,
    CERTIFICATE_INVALID
}
