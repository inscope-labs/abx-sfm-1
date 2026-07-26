package com.inscopelabs.sfm.core.exception

/**
 * Exception for policy violations.
 */
open class PolicyViolationException(
    message: String,
    open val policyName: String? = null,
    val ruleDescription: String? = null,
    cause: Throwable? = null
) : FileManagerException(
    buildString {
        append("Policy violation")
        if (policyName != null) append(" [$policyName]")
        append(": ")
        append(message)
        if (ruleDescription != null) append(" (Rule: $ruleDescription)")
    },
    cause
) {
    override val errorCode: String = "POLICY_VIOLATION"
}

/**
 * Exception for blocked operations due to policy.
 */
class PolicyBlockedException(
    operation: String,
    reason: String,
    policyName: String? = null,
    cause: Throwable? = null
) : PolicyViolationException(
    "Operation '$operation' blocked: $reason",
    policyName,
    "Blocked operation: $operation",
    cause
) {
    override val errorCode: String = "POLICY_BLOCKED"
}

/**
 * Exception for user approval denial.
 */
class ApprovalDeniedException(
    operation: String,
    reason: String? = null,
    cause: Throwable? = null
) : PolicyViolationException(
    "User denied approval for: $operation" + if (reason != null) " ($reason)" else "",
    policyName = "UserApproval",
    ruleDescription = "User must approve dangerous operations",
    cause
) {
    override val errorCode: String = "POLICY_APPROVAL_DENIED"
}

/**
 * Exception for capability not granted scenarios.
 */
class CapabilityNotGrantedException(
    requiredCapability: String,
    resource: String? = null,
    cause: Throwable? = null
) : PolicyViolationException(
    buildString {
        append("Required capability not granted: $requiredCapability")
        if (resource != null) append(" (resource: $resource)")
    },
    policyName = "CapabilityPolicy",
    ruleDescription = "Capability-based access control",
    cause
) {
    override val errorCode: String = "POLICY_CAPABILITY_MISSING"
}

/**
 * Exception for resource quota exceeded.
 */
class QuotaExceededException(
    quotaType: String,
    limit: Long,
    used: Long,
    cause: Throwable? = null
) : PolicyViolationException(
    "Quota exceeded: $quotaType (limit: $limit, used: $used)",
    policyName = "ResourceQuota",
    ruleDescription = "Resource usage limits",
    cause
) {
    override val errorCode: String = "POLICY_QUOTA_EXCEEDED"
}

/**
 * Exception for time-based restrictions.
 */
class TimeRestrictionException(
    restriction: String,
    currentTime: Long? = null,
    cause: Throwable? = null
) : PolicyViolationException(
    buildString {
        append("Time restriction active: $restriction")
        if (currentTime != null) append(" (current: $currentTime)")
    },
    policyName = "TimePolicy",
    ruleDescription = "Time-based access restrictions",
    cause
) {
    override val errorCode: String = "POLICY_TIME_RESTRICTED"
}
