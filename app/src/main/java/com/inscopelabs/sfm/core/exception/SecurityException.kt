package com.inscopelabs.sfm.core.exception

/**
 * Base exception for security-related violations.
 */
open class SecurityException(
    message: String,
    cause: Throwable? = null
) : FileManagerException(message, cause) {
    override val errorCode: String = "SEC_UNKNOWN"
}

/**
 * Exception for path traversal attacks.
 */
class PathTraversalException(
    attemptedPath: String,
    cause: Throwable? = null
) : SecurityException("Path traversal attempt detected: $attemptedPath", cause) {
    override val errorCode: String = "SEC_PATH_TRAVERSAL"
}

/**
 * Exception for unauthorized access attempts.
 */
class UnauthorizedAccessException(
    resource: String,
    requiredCapability: String,
    cause: Throwable? = null
) : SecurityException(
    "Unauthorized access to $resource, required capability: $requiredCapability",
    cause
) {
    override val errorCode: String = "SEC_UNAUTHORIZED"
}

/**
 * Exception for session-related security issues.
 */
class SessionSecurityException(
    message: String,
    sessionId: String? = null,
    cause: Throwable? = null
) : SecurityException(
    if (sessionId != null) "Session security error [$sessionId]: $message"
    else "Session security error: $message",
    cause
) {
    override val errorCode: String = "SEC_SESSION_ERROR"
}

/**
 * Exception for capability violations.
 */
class CapabilityViolationException(
    required: String,
    provided: String,
    cause: Throwable? = null
) : SecurityException(
    "Capability violation: required '$required', provided '$provided'",
    cause
) {
    override val errorCode: String = "SEC_CAPABILITY_VIOLATION"
}

/**
 * Exception for token validation failures.
 */
class TokenValidationException(
    reason: String,
    cause: Throwable? = null
) : SecurityException("Token validation failed: $reason", cause) {
    override val errorCode: String = "SEC_TOKEN_INVALID"
}

/**
 * Exception for encryption/decryption failures.
 */
class EncryptionException(
    message: String,
    cause: Throwable? = null
) : SecurityException("Encryption error: $message", cause) {
    override val errorCode: String = "SEC_ENCRYPTION_ERROR"
}

/**
 * Exception for certificate pinning failures.
 */
class CertificatePinningException(
    message: String,
    cause: Throwable? = null
) : SecurityException("Certificate pinning failed: $message", cause) {
    override val errorCode: String = "SEC_CERTIFICATE_PINNING"
}

/**
 * Exception for replay attack detection.
 */
class ReplayAttackException(
    requestId: String,
    timestamp: Long,
    cause: Throwable? = null
) : SecurityException(
    "Replay attack detected: requestId=$requestId, timestamp=$timestamp",
    cause
) {
    override val errorCode: String = "SEC_REPLAY_ATTACK"
}

/**
 * Exception for root/jailbreak detection.
 */
class RootDetectionException(
    detectionType: String,
    cause: Throwable? = null
) : SecurityException(
    "Root/jailbreak detected: $detectionType",
    cause
) {
    override val errorCode: String = "SEC_ROOT_DETECTED"
}

/**
 * Exception for debugger detection.
 */
class DebuggerDetectionException(
    cause: Throwable? = null
) : SecurityException("Debugger attachment detected", cause) {
    override val errorCode: String = "SEC_DEBUGGER_DETECTED"
}
