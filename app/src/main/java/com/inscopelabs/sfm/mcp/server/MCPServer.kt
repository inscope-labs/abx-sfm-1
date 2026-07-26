package com.inscopelabs.sfm.mcp.server

import android.content.Context
import com.inscopelabs.sfm.mcp.handlers.*
import com.inscopelabs.sfm.mcp.relay.RelayClient
import com.inscopelabs.sfm.mcp.security.MCPSecurityGuard
import com.inscopelabs.sfm.security.audit.AuditLogger
import com.inscopelabs.sfm.security.audit.SessionEventType
import com.inscopelabs.sfm.security.audit.SecurityWarningType
import com.inscopelabs.sfm.security.policy.PolicyEngine
import com.inscopelabs.sfm.security.session.SessionManager
import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentHashMap

/**
 * MCP Server lifecycle management.
 */
class MCPServer(
    private val context: Context,
    private val config: MCPServerConfig,
    private val sessionManager: SessionManager,
    private val policyEngine: PolicyEngine,
    private val auditLogger: AuditLogger,
    private val securityGuard: MCPSecurityGuard
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var isRunning = false
    private var relayClient: RelayClient? = null

    private val handlers = ConcurrentHashMap<String, MCPHandler>()
    private val activeRequests = ConcurrentHashMap<String, Job>()

    /**
     * Starts the MCP server.
     */
    suspend fun start(): ServerResult {
        if (isRunning) {
            return ServerResult.AlreadyRunning
        }

        try {
            // Register handlers
            registerHandlers()

            // Connect to relay if configured
            if (config.useRelay && config.relayConfig != null) {
                relayClient = RelayClient(context, config.relayConfig)
                relayClient?.connect()
            }

            isRunning = true
            auditLogger.logSessionEvent(
                sessionId = "server",
                event = SessionEventType.CREATED,
                userId = "system"
            )

            return ServerResult.Started
        } catch (e: Exception) {
            auditLogger.logSecurityWarning(
                SecurityWarningType.SUSPICIOUS_ACTIVITY,
                mapOf("error" to (e.message ?: "Unknown error"))
            )
            return ServerResult.Error(e.message ?: "Failed to start")
        }
    }

    /**
     * Stops the MCP server.
     */
    suspend fun stop(): ServerResult {
        if (!isRunning) {
            return ServerResult.NotRunning
        }

        try {
            // Cancel active requests
            activeRequests.values.forEach { it.cancel() }
            activeRequests.clear()

            // Disconnect relay
            relayClient?.disconnect()

            scope.cancel()
            isRunning = false

            auditLogger.logSessionEvent(
                sessionId = "server",
                event = SessionEventType.EXPIRED,
                userId = "system"
            )

            return ServerResult.Stopped
        } catch (e: Exception) {
            return ServerResult.Error(e.message ?: "Failed to stop")
        }
    }

    /**
     * Processes an MCP tool invocation.
     */
    suspend fun processRequest(invocation: MCPToolInvocation): MCPResponse {
        val requestId = invocation.requestId

        // Validate request security
        val validationResult = securityGuard.validateRequest(invocation)
        if (!validationResult.isValid()) {
            val errorMsg = (validationResult as? MCPSecurityGuard.ValidationResult.Invalid)?.error ?: "Validation failed"
            return MCPResponse.Error(
                requestId = requestId,
                error = errorMsg
            )
        }

        // Get handler
        val handler = handlers[invocation.toolName]
            ?: return MCPResponse.Error(
                requestId = requestId,
                error = "Unknown tool: ${invocation.toolName}"
            )

        // Track request and execute exactly once
        val deferred = scope.async {
            try {
                handler.handle(invocation)
            } finally {
                activeRequests.remove(requestId)
            }
        }

        activeRequests[requestId] = deferred

        return try {
            val result = deferred.await()
            MCPResponse.Success(requestId = requestId, result = result)
        } catch (e: CancellationException) {
            MCPResponse.Error(requestId = requestId, error = "Request cancelled")
        } catch (e: Exception) {
            auditLogger.logSecurityWarning(
                SecurityWarningType.SUSPICIOUS_ACTIVITY,
                mapOf(
                    "tool" to invocation.toolName,
                    "error" to (e.message ?: "Unknown error")
                )
            )
            MCPResponse.Error(requestId = requestId, error = e.message ?: "Handler error")
        }
    }

    /**
     * Cancels a request.
     */
    fun cancelRequest(requestId: String): Boolean {
        val job = activeRequests[requestId]
        return if (job != null) {
            job.cancel()
            activeRequests.remove(requestId)
            true
        } else {
            false
        }
    }

    /**
     * Gets server status.
     */
    fun getStatus(): ServerStatus {
        return ServerStatus(
            isRunning = isRunning,
            activeRequests = activeRequests.size,
            connectedToRelay = relayClient?.isConnected() == true,
            uptime = if (isRunning) System.currentTimeMillis() else 0
        )
    }

    private fun registerHandlers() {
        // File operations
        handlers["filesystem.read"] = ReadFileHandler(context, sessionManager, policyEngine, auditLogger)
        handlers["filesystem.write"] = WriteFileHandler(context, sessionManager, policyEngine, auditLogger)
        handlers["filesystem.delete"] = DeleteFileHandler(context, sessionManager, policyEngine, auditLogger)
        handlers["filesystem.list"] = ListDirectoryHandler(context, sessionManager, policyEngine, auditLogger)
        handlers["filesystem.search"] = SearchHandler(context, sessionManager, policyEngine, auditLogger)
        handlers["filesystem.rename"] = RenameFileHandler(context, sessionManager, policyEngine, auditLogger)
        handlers["filesystem.copy"] = CopyFileHandler(context, sessionManager, policyEngine, auditLogger)
        handlers["filesystem.move"] = MoveFileHandler(context, sessionManager, policyEngine, auditLogger)
        handlers["filesystem.create"] = CreateFileHandler(context, sessionManager, policyEngine, auditLogger)
        handlers["filesystem.metadata"] = MetadataHandler(context, sessionManager, policyEngine, auditLogger)
        handlers["filesystem.archive"] = ArchiveHandler(context, sessionManager, policyEngine, auditLogger)
        handlers["filesystem.share"] = ShareHandler(context, sessionManager, policyEngine, auditLogger)
    }

    sealed class ServerResult {
        object Started : ServerResult()
        object AlreadyRunning : ServerResult()
        object Stopped : ServerResult()
        object NotRunning : ServerResult()
        data class Error(val message: String) : ServerResult()
    }

    data class ServerStatus(
        val isRunning: Boolean,
        val activeRequests: Int,
        val connectedToRelay: Boolean,
        val uptime: Long
    )
}

/**
 * Represents an MCP tool invocation request.
 */
data class MCPToolInvocation(
    val requestId: String,
    val sessionId: String,
    val toolName: String,
    val arguments: Map<String, Any?>,
    val timestamp: Long,
    val capabilityToken: String? = null
)

/**
 * MCP response wrapper.
 */
sealed class MCPResponse {
    data class Success(
        val requestId: String,
        val result: Map<String, Any?>
    ) : MCPResponse()

    data class Error(
        val requestId: String,
        val error: String,
        val code: String? = null
    ) : MCPResponse()
}

/**
 * MCP handler interface.
 */
interface MCPHandler {
    suspend fun handle(invocation: MCPToolInvocation): Map<String, Any?>
}
