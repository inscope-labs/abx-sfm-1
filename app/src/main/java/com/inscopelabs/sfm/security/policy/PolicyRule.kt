package com.inscopelabs.sfm.security.policy

import com.inscopelabs.sfm.security.permissions.Capability
import java.util.regex.Pattern

/**
 * Defines a policy rule for access control.
 */
data class PolicyRule(
    val id: String,
    val name: String,
    val description: String,
    val effect: PolicyEffect,
    val policyName: String,
    val conditions: List<PolicyCondition>,
    val priority: Int = 0,
    val isEnabled: Boolean = true
) {
    /**
     * Checks if this rule matches the given request.
     */
    fun matches(request: PolicyRequest): Boolean {
        if (!isEnabled) return false

        return conditions.all { condition ->
            condition.evaluate(request)
        }
    }
}

/**
 * Condition for policy rule matching.
 */
sealed class PolicyCondition {

    /**
     * Matches based on operation type.
     */
    data class OperationCondition(
        val operations: Set<PolicyOperation>
    ) : PolicyCondition() {
        override fun evaluate(request: PolicyRequest): Boolean {
            return request.operation in operations
        }
    }

    /**
     * Matches based on required capabilities.
     */
    data class CapabilityCondition(
        val capabilities: Set<Capability>,
        val matchAll: Boolean = true
    ) : PolicyCondition() {
        override fun evaluate(request: PolicyRequest): Boolean {
            return if (matchAll) {
                capabilities.all { it in request.requiredCapabilities }
            } else {
                capabilities.any { it in request.requiredCapabilities }
            }
        }
    }

    /**
     * Matches based on resource path pattern.
     */
    data class ResourceCondition(
        val pattern: String,
        val useRegex: Boolean = false
    ) : PolicyCondition() {
        private val regexPattern = if (useRegex) {
            Pattern.compile(pattern)
        } else {
            null
        }

        override fun evaluate(request: PolicyRequest): Boolean {
            return if (useRegex) {
                regexPattern?.matcher(request.resource)?.matches() ?: false
            } else {
                request.resource.startsWith(pattern) ||
                        request.resource == pattern
            }
        }
    }

    /**
     * Matches based on session attributes.
     */
    data class SessionCondition(
        val userId: String? = null,
        val minSessionAge: Long? = null,
        val requiresCapability: Capability? = null
    ) : PolicyCondition() {
        override fun evaluate(request: PolicyRequest): Boolean {
            if (userId != null && request.session.userId != userId) {
                return false
            }

            if (minSessionAge != null) {
                val sessionAge = System.currentTimeMillis() - request.session.createdAt
                if (sessionAge < minSessionAge) {
                    return false
                }
            }

            if (requiresCapability != null) {
                if (requiresCapability !in request.session.capabilities) {
                    return false
                }
            }

            return true
        }
    }

    /**
     * Matches based on time of day.
     */
    data class TimeCondition(
        val startHour: Int,
        val endHour: Int,
        val daysOfWeek: Set<Int> = setOf(1, 2, 3, 4, 5, 6, 7) // Mon-Sun
    ) : PolicyCondition() {
        override fun evaluate(request: PolicyRequest): Boolean {
            val calendar = java.util.Calendar.getInstance()
            val currentHour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
            val currentDay = calendar.get(java.util.Calendar.DAY_OF_WEEK)

            if (currentDay !in daysOfWeek) {
                return false
            }

            return if (startHour <= endHour) {
                currentHour in startHour..endHour
            } else {
                // Handles overnight ranges
                currentHour >= startHour || currentHour <= endHour
            }
        }
    }

    /**
     * Matches based on request context values.
     */
    data class ContextCondition(
        val key: String,
        val value: Any,
        val operator: ContextOperator = ContextOperator.EQUALS
    ) : PolicyCondition() {
        override fun evaluate(request: PolicyRequest): Boolean {
            val contextValue = request.context[key]
                ?: return false

            return when (operator) {
                ContextOperator.EQUALS -> contextValue == value
                ContextOperator.NOT_EQUALS -> contextValue != value
                ContextOperator.CONTAINS -> contextValue.toString().contains(value.toString())
                ContextOperator.MATCHES -> {
                    if (value is String) {
                        contextValue.toString().matches(Regex(value))
                    } else {
                        false
                    }
                }
                ContextOperator.GREATER_THAN -> {
                    (contextValue as? Number)?.toLong()?.let { it > (value as Number).toLong() } ?: false
                }
                ContextOperator.LESS_THAN -> {
                    (contextValue as? Number)?.toLong()?.let { it < (value as Number).toLong() } ?: false
                }
            }
        }
    }

    /**
     * Combines multiple conditions with AND.
     */
    data class AndCondition(
        val conditions: List<PolicyCondition>
    ) : PolicyCondition() {
        override fun evaluate(request: PolicyRequest): Boolean {
            return conditions.all { it.evaluate(request) }
        }
    }

    /**
     * Combines multiple conditions with OR.
     */
    data class OrCondition(
        val conditions: List<PolicyCondition>
    ) : PolicyCondition() {
        override fun evaluate(request: PolicyRequest): Boolean {
            return conditions.any { it.evaluate(request) }
        }
    }

    /**
     * Negates a condition.
     */
    data class NotCondition(
        val condition: PolicyCondition
    ) : PolicyCondition() {
        override fun evaluate(request: PolicyRequest): Boolean {
            return !condition.evaluate(request)
        }
    }

    abstract fun evaluate(request: PolicyRequest): Boolean
}

enum class ContextOperator {
    EQUALS,
    NOT_EQUALS,
    CONTAINS,
    MATCHES,
    GREATER_THAN,
    LESS_THAN
}

/**
 * Builder for policy rules.
 */
class PolicyRuleBuilder {
    private var id: String = ""
    private var name: String = ""
    private var description: String = ""
    private var effect: PolicyEffect = PolicyEffect.DENY
    private var policyName: String = ""
    private var conditions = mutableListOf<PolicyCondition>()
    private var priority: Int = 0
    private var enabled: Boolean = true

    fun id(id: String) = apply { this.id = id }
    fun name(name: String) = apply { this.name = name }
    fun description(desc: String) = apply { this.description = desc }
    fun effect(effect: PolicyEffect) = apply { this.effect = effect }
    fun policyName(name: String) = apply { this.policyName = name }
    fun priority(priority: Int) = apply { this.priority = priority }
    fun enabled(enabled: Boolean) = apply { this.enabled = enabled }
    fun condition(condition: PolicyCondition) = apply { conditions.add(condition) }

    fun build(): PolicyRule {
        require(id.isNotBlank()) { "Rule ID is required" }
        require(name.isNotBlank()) { "Rule name is required" }
        require(conditions.isNotEmpty()) { "At least one condition is required" }

        return PolicyRule(
            id = id,
            name = name,
            description = description,
            effect = effect,
            policyName = policyName,
            conditions = conditions.toList(),
            priority = priority,
            isEnabled = enabled
        )
    }
}
