package com.inscopelabs.sfm.security.policy

import android.net.Uri
import com.inscopelabs.sfm.core.exception.CapabilityNotGrantedException
import com.inscopelabs.sfm.core.exception.PolicyBlockedException
import com.inscopelabs.sfm.file.navigation.PathSanitizer
import com.inscopelabs.sfm.security.audit.AuditLogger
import com.inscopelabs.sfm.security.audit.OperationResult
import com.inscopelabs.sfm.security.audit.UserApprovalState
import com.inscopelabs.sfm.security.permissions.Capability
import com.inscopelabs.sfm.security.session.Session

/**
 * Central policy engine that evaluates all operations.
 * Request flow: Authentication → Authorization → Policy Evaluation → User Approval → Filesystem
 */
class PolicyEngine(
    private val auditLogger: AuditLogger
) {

    private val policyRules = mutableListOf<PolicyRule>()
    private val defaultDenyPolicy = true

    /**
     * Evaluates if an operation should be allowed.
     */
    fun evaluate(request: PolicyRequest): PolicyDecision {
        val startTime = System.currentTimeMillis()

        try {
            // 1. Check session validity
            if (!request.session.isActive || request.session.isExpired()) {
                return deny(
                    request,
                    "Session is inactive or expired",
                    startTime
                )
            }

            // 2. Check capability
            val capabilityCheck = checkCapability(request)
            if (capabilityCheck is PolicyDecision.Deny) {
                return capabilityCheck
            }

            // 3. Evaluate policy rules
            val ruleDecision = evaluateRules(request)
            if (ruleDecision is PolicyDecision.Deny) {
                auditLogger.logPolicyViolation(
                    request.session.sessionId,
                    ruleDecision.policyName ?: "unknown",
                    ruleDecision.reason ?: "policy violation",
                    request.resource
                )
                return ruleDecision
            }

            // 4. Check for dangerous operation approval
            if (requiresApproval(request)) {
                val approvalState = checkApproval(request)
                if (approvalState != ApprovalState.APPROVED) {
                    return deny(
                        request,
                        "User approval required: $approvalState",
                        startTime
                    )
                }
            }

            // 5. Check resource scope
            if (!isResourceInScope(request)) {
                return deny(
                    request,
                    "Resource is outside authorized scope",
                    startTime
                )
            }

            // All checks passed
            auditLogger.logFileOperation(
                request.session.sessionId,
                request.operation.name,
                request.resource,
                OperationResult.ALLOWED,
                userApprovalState = UserApprovalState.APPROVED_ONCE,
                elapsedMs = System.currentTimeMillis() - startTime
            )

            return PolicyDecision.Allow

        } catch (e: Exception) {
            return deny(request, "Policy evaluation error: ${e.message}", startTime)
        }
    }

    /**
     * Checks if session has required capability.
     */
    private fun checkCapability(request: PolicyRequest): PolicyDecision {
        val required = request.requiredCapabilities
        val granted = request.session.capabilities

        for (capability in required) {
            if (capability !in granted) {
                auditLogger.logAuthorization(
                    request.session.sessionId,
                    capability.name,
                    false,
                    request.resource
                )
                return PolicyDecision.Deny(
                    reason = "Missing capability: ${capability.displayName}",
                    error = CapabilityNotGrantedException(capability.name, request.resource)
                )
            }
        }

        return PolicyDecision.Allow
    }

    /**
     * Evaluates policy rules against the request.
     */
    private fun evaluateRules(request: PolicyRequest): PolicyDecision {
        for (rule in policyRules) {
            if (rule.matches(request)) {
                return when (rule.effect) {
                    PolicyEffect.ALLOW -> PolicyDecision.Allow
                    PolicyEffect.DENY -> PolicyDecision.Deny(
                        reason = "Policy rule: ${rule.name}",
                        policyName = rule.policyName
                    )
                }
            }
        }

        // Default deny if no rules match and defaultDenyPolicy is enabled
        return if (defaultDenyPolicy) {
            PolicyDecision.Deny(reason = "no matching allow rule")
        } else {
            PolicyDecision.Allow
        }
    }

    /**
     * Checks if operation requires user approval.
     */
    private fun requiresApproval(request: PolicyRequest): Boolean {
        return request.requiredCapabilities.any { it.requiresApproval() }
    }

    /**
     * Checks if operation has been approved.
     */
    private fun checkApproval(request: PolicyRequest): ApprovalState {
        // In production, this would check against stored approvals
        // For now, use session-level approval
        return if (request.session.capabilities.contains(Capability.ADMIN_CONFIG)) {
            ApprovalState.APPROVED // Admin bypass
        } else {
            ApprovalState.NEEDS_APPROVAL
        }
    }

    /**
     * Checks if resource is within authorized scope.
     */
    private fun isResourceInScope(request: PolicyRequest): Boolean {
        val allowedRoots = (request.context["authorizedRoots"] as? List<String>)
            ?: request.session.metadata["authorizedRoots"]?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() }
            ?: emptyList()

        if (allowedRoots.isNotEmpty()) {
            return PathSanitizer.isPathSafe(request.resource, allowedRoots)
        }

        return try {
            PathSanitizer.sanitizePath(request.resource)
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun deny(request: PolicyRequest, reason: String, startTime: Long): PolicyDecision.Deny {
        auditLogger.logFileOperation(
            request.session.sessionId,
            request.operation.name,
            request.resource,
            OperationResult.DENIED,
            elapsedMs = System.currentTimeMillis() - startTime
        )

        return PolicyDecision.Deny(reason = reason)
    }

    /**
     * Adds a policy rule.
     */
    fun addRule(rule: PolicyRule) {
        policyRules.add(rule)
    }

    /**
     * Removes a policy rule.
     */
    fun removeRule(rule: PolicyRule) {
        policyRules.remove(rule)
    }

    /**
     * Clears all policy rules.
     */
    fun clearRules() {
        policyRules.clear()
    }

    /**
     * Gets all policy rules.
     */
    fun getRules(): List<PolicyRule> = policyRules.toList()
}

/**
 * Represents a policy evaluation request.
 */
data class PolicyRequest(
    val session: Session,
    val operation: PolicyOperation,
    val resource: String,
    val resourceUri: Uri? = null,
    val requiredCapabilities: Set<Capability>,
    val context: Map<String, Any> = emptyMap()
)

/**
 * Operations that require policy evaluation.
 */
enum class PolicyOperation {
    READ,
    WRITE,
    DELETE,
    RENAME,
    CREATE,
    SEARCH,
    ARCHIVE,
    SHARE,
    COPY,
    MOVE,
    ENCRYPT,
    DECRYPT,
    MANAGE_SESSION,
    INSTALL_PLUGIN,
    CONFIGURE
}

/**
 * Policy evaluation decision.
 */
sealed class PolicyDecision {
    object Allow : PolicyDecision()

    data class Deny(
        val reason: String? = null,
        val policyName: String? = null,
        val error: Exception? = null
    ) : PolicyDecision()
}

/**
 * User approval states.
 */
enum class ApprovalState {
    APPROVED,
    DENIED,
    NEEDS_APPROVAL,
    TIMEOUT
}

/**
 * Policy rule effect.
 */
enum class PolicyEffect {
    ALLOW,
    DENY
}
