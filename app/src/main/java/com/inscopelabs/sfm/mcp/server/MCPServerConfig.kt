package com.inscopelabs.sfm.mcp.server

/**
 * Configuration for the MCP server.
 */
data class MCPServerConfig(
    val serverName: String = "SecureFilesMCP",
    val serverVersion: String = "0.0.1",
    val port: Int = DEFAULT_PORT,
    val useRelay: Boolean = false,
    val relayConfig: RelayConfig? = null,
    val timeoutMs: Long = DEFAULT_TIMEOUT_MS,
    val maxRequestSize: Int = DEFAULT_MAX_REQUEST_SIZE,
    val enableTls: Boolean = true,
    val requireMutualTls: Boolean = false,
    val allowedOrigins: List<String> = emptyList(),
    val rateLimitPerMinute: Int = DEFAULT_RATE_LIMIT,
    val enableAuditLogging: Boolean = true,
    val requestValidation: RequestValidation = RequestValidation()
)

/**
 * Configuration for relay connection.
 */
data class RelayConfig(
    val relayUrl: String,
    val useTls: Boolean = true,
    val certificatePinning: Boolean = true,
    val pinnedCertificates: List<String> = emptyList(),
    val reconnectIntervalMs: Long = DEFAULT_RECONNECT_INTERVAL,
    val maxReconnectAttempts: Int = DEFAULT_MAX_RECONNECT,
    val heartbeatIntervalMs: Long = DEFAULT_HEARTBEAT_INTERVAL,
    val authToken: String? = null
)

/**
 * Request validation configuration.
 */
data class RequestValidation(
    val validateTimestamp: Boolean = true,
    val maxTimestampAgeSeconds: Long = 300,
    val validateNonce: Boolean = true,
    val allowReplay: Boolean = false,
    val requireCapabilityToken: Boolean = true,
    val validateSession: Boolean = true
)

/**
 * Available MCP tools.
 */
enum class MCPTool(
    val toolName: String,
    val description: String,
    val parameters: List<ToolParameter>
) {
    READ_FILE(
        toolName = "filesystem.read",
        description = "Read file contents",
        parameters = listOf(
            ToolParameter("path", "string", required = true, description = "File path")
        )
    ),

    WRITE_FILE(
        toolName = "filesystem.write",
        description = "Write content to a file",
        parameters = listOf(
            ToolParameter("path", "string", required = true, description = "File path"),
            ToolParameter("content", "string", required = true, description = "File content"),
            ToolParameter("encoding", "string", required = false, description = "Content encoding")
        )
    ),

    DELETE_FILE(
        toolName = "filesystem.delete",
        description = "Delete a file or directory",
        parameters = listOf(
            ToolParameter("path", "string", required = true, description = "File path"),
            ToolParameter("recursive", "boolean", required = false, description = "Delete recursively")
        )
    ),

    LIST_DIRECTORY(
        toolName = "filesystem.list",
        description = "List directory contents",
        parameters = listOf(
            ToolParameter("path", "string", required = true, description = "Directory path"),
            ToolParameter("sort", "string", required = false, description = "Sort field"),
            ToolParameter("order", "string", required = false, description = "Sort order")
        )
    ),

    SEARCH(
        toolName = "filesystem.search",
        description = "Search for files",
        parameters = listOf(
            ToolParameter("query", "string", required = true, description = "Search query"),
            ToolParameter("path", "string", required = false, description = "Root path"),
            ToolParameter("recursive", "boolean", required = false, description = "Search subdirectories")
        )
    ),

    RENAME(
        toolName = "filesystem.rename",
        description = "Rename a file or directory",
        parameters = listOf(
            ToolParameter("path", "string", required = true, description = "Current path"),
            ToolParameter("newName", "string", required = true, description = "New name")
        )
    ),

    COPY(
        toolName = "filesystem.copy",
        description = "Copy a file",
        parameters = listOf(
            ToolParameter("source", "string", required = true, description = "Source path"),
            ToolParameter("destination", "string", required = true, description = "Destination path")
        )
    ),

    MOVE(
        toolName = "filesystem.move",
        description = "Move a file",
        parameters = listOf(
            ToolParameter("source", "string", required = true, description = "Source path"),
            ToolParameter("destination", "string", required = true, description = "Destination path")
        )
    ),

    CREATE_FILE(
        toolName = "filesystem.create",
        description = "Create a new file",
        parameters = listOf(
            ToolParameter("path", "string", required = true, description = "File path"),
            ToolParameter("content", "string", required = false, description = "Initial content")
        )
    ),

    METADATA(
        toolName = "filesystem.metadata",
        description = "Get file metadata",
        parameters = listOf(
            ToolParameter("path", "string", required = true, description = "File path")
        )
    ),

    ARCHIVE(
        toolName = "filesystem.archive",
        description = "Create or extract archive",
        parameters = listOf(
            ToolParameter("operation", "string", required = true, description = "create or extract"),
            ToolParameter("source", "string", required = true, description = "Source path"),
            ToolParameter("destination", "string", required = false, description = "Destination path")
        )
    ),

    SHARE(
        toolName = "filesystem.share",
        description = "Share a file",
        parameters = listOf(
            ToolParameter("path", "string", required = true, description = "File path"),
            ToolParameter("method", "string", required = false, description = "Share method")
        )
    );

    companion object {
        fun fromName(name: String): MCPTool? {
            return entries.find { it.toolName == name }
        }
    }
}

data class ToolParameter(
    val name: String,
    val type: String,
    val required: Boolean = false,
    val description: String = "",
    val default: Any? = null
)

/**
 * MCP protocol messages.
 */
object MCPMessageTypes {
    const val INITIALIZE = "initialize"
    const val TOOL_CALL = "tool_call"
    const val TOOL_RESPONSE = "tool_response"
    const val ERROR = "error"
    const val PING = "ping"
    const val PONG = "pong"
    const val CLOSE = "close"
    const val NOTIFICATION = "notification"
}

private const val DEFAULT_PORT = 8080
private const val DEFAULT_TIMEOUT_MS = 30000L
private const val DEFAULT_MAX_REQUEST_SIZE = 10 * 1024 * 1024
private const val DEFAULT_RATE_LIMIT = 100
private const val DEFAULT_RECONNECT_INTERVAL = 5000L
private const val DEFAULT_MAX_RECONNECT = 5
private const val DEFAULT_HEARTBEAT_INTERVAL = 30000L
