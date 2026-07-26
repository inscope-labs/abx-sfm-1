package com.inscopelabs.sfm.mcp.handlers

import android.content.Context
import android.net.Uri
import com.inscopelabs.sfm.core.model.FileOperationResult
import com.inscopelabs.sfm.core.model.FileOperationError
import com.inscopelabs.sfm.file.navigation.PathSanitizer
import com.inscopelabs.sfm.file.operations.CopyOperation
import com.inscopelabs.sfm.file.operations.CreateFileOperation
import com.inscopelabs.sfm.file.operations.CreateFolderOperation
import com.inscopelabs.sfm.file.operations.DeleteOperation
import com.inscopelabs.sfm.file.operations.MoveOperation
import com.inscopelabs.sfm.file.operations.RenameOperation
import com.inscopelabs.sfm.mcp.server.MCPHandler
import com.inscopelabs.sfm.mcp.server.MCPToolInvocation
import com.inscopelabs.sfm.security.audit.AuditLogger
import com.inscopelabs.sfm.security.audit.OperationResult
import com.inscopelabs.sfm.security.permissions.Capability
import com.inscopelabs.sfm.security.policy.PolicyDecision
import com.inscopelabs.sfm.security.policy.PolicyEngine
import com.inscopelabs.sfm.security.policy.PolicyOperation
import com.inscopelabs.sfm.security.policy.PolicyRequest
import com.inscopelabs.sfm.security.session.Session
import com.inscopelabs.sfm.security.session.SessionManager
import com.inscopelabs.sfm.storage.SAFManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Helper to validate session and policy across MCP handlers.
 */
private suspend fun validateAndEvaluate(
    invocation: MCPToolInvocation,
    path: String,
    operation: PolicyOperation,
    requiredCapabilities: Set<Capability>,
    sessionManager: SessionManager,
    policyEngine: PolicyEngine,
    auditLogger: AuditLogger
): Pair<Session?, Map<String, Any?>?> {
    val sessionResult = sessionManager.validateSession(invocation.sessionId)
    if (sessionResult !is SessionManager.SessionValidationResult.Valid) {
        val reason = when (sessionResult) {
            is SessionManager.SessionValidationResult.Invalid -> sessionResult.reason
            is SessionManager.SessionValidationResult.Expired -> sessionResult.reason
            else -> "Invalid session"
        }
        auditLogger.logFileOperation(
            sessionId = invocation.sessionId,
            operation = operation.name,
            resource = path,
            result = OperationResult.DENIED
        )
        val errMap: Map<String, Any?> = mapOf("success" to false, "error" to reason)
        return null to errMap
    }

    val session = sessionResult.session
    val policyRequest = PolicyRequest(
        session = session,
        operation = operation,
        resource = path,
        requiredCapabilities = requiredCapabilities
    )

    val decision = policyEngine.evaluate(policyRequest)
    if (decision is PolicyDecision.Deny) {
        auditLogger.logFileOperation(
            sessionId = invocation.sessionId,
            operation = operation.name,
            resource = path,
            result = OperationResult.DENIED
        )
        val errMap: Map<String, Any?> = mapOf("success" to false, "error" to (decision.reason ?: "Access denied"))
        return null to errMap
    }

    return session to null
}

/**
 * Handles filesystem read operations via MCP.
 */
class ReadFileHandler(
    private val context: Context,
    private val sessionManager: SessionManager,
    private val policyEngine: PolicyEngine,
    private val auditLogger: AuditLogger
) : MCPHandler {

    override suspend fun handle(invocation: MCPToolInvocation): Map<String, Any?> {
        val args = invocation.arguments
        val rawPath = args["path"] as? String
            ?: return error("Missing required parameter: path")

        val path = try {
            PathSanitizer.sanitizePath(rawPath)
        } catch (e: Exception) {
            return error("Path traversal attempt detected: ${e.message}")
        }

        val evalResult = validateAndEvaluate(
            invocation = invocation,
            path = path,
            operation = PolicyOperation.READ,
            requiredCapabilities = setOf(Capability.FILESYSTEM_READ),
            sessionManager = sessionManager,
            policyEngine = policyEngine,
            auditLogger = auditLogger
        )
        if (evalResult.first == null) {
            return evalResult.second ?: error("Authentication or authorization failed")
        }
        val session = evalResult.first!!

        return readFile(session.sessionId, path)
    }

    private suspend fun readFile(sessionId: String, path: String): Map<String, Any?> {
        return withContext(Dispatchers.IO) {
            try {
                val uri = Uri.parse(path)
                val safManager = SAFManager(context)
                val docFile = safManager.resolveUri(uri)
                val inputStream = if (docFile != null) {
                    context.contentResolver.openInputStream(docFile.uri)
                } else {
                    context.contentResolver.openInputStream(uri)
                }

                inputStream?.use { input ->
                    val bytes = input.readBytes()
                    val content = String(bytes, Charsets.UTF_8)
                    auditLogger.logFileOperation(
                        sessionId = sessionId,
                        operation = PolicyOperation.READ.name,
                        resource = path,
                        result = OperationResult.SUCCESS
                    )
                    mapOf(
                        "success" to true,
                        "content" to content,
                        "size" to bytes.size,
                        "path" to path
                    )
                } ?: run {
                    auditLogger.logFileOperation(
                        sessionId = sessionId,
                        operation = PolicyOperation.READ.name,
                        resource = path,
                        result = OperationResult.FAILURE
                    )
                    error("Failed to open file")
                }
            } catch (e: Exception) {
                auditLogger.logFileOperation(
                    sessionId = sessionId,
                    operation = PolicyOperation.READ.name,
                    resource = path,
                    result = OperationResult.FAILURE
                )
                error("Read failed: ${e.message}")
            }
        }
    }

    private fun error(message: String): Map<String, Any?> = mapOf("success" to false, "error" to message)
}

/**
 * Handles filesystem write operations.
 */
class WriteFileHandler(
    private val context: Context,
    private val sessionManager: SessionManager,
    private val policyEngine: PolicyEngine,
    private val auditLogger: AuditLogger
) : MCPHandler {

    override suspend fun handle(invocation: MCPToolInvocation): Map<String, Any?> {
        val args = invocation.arguments
        val rawPath = args["path"] as? String
            ?: return error("Missing required parameter: path")
        val content = args["content"] as? String
            ?: return error("Missing required parameter: content")

        val path = try {
            PathSanitizer.sanitizePath(rawPath)
        } catch (e: Exception) {
            return error("Path traversal attempt detected: ${e.message}")
        }

        val evalResult = validateAndEvaluate(
            invocation = invocation,
            path = path,
            operation = PolicyOperation.WRITE,
            requiredCapabilities = setOf(Capability.FILESYSTEM_WRITE),
            sessionManager = sessionManager,
            policyEngine = policyEngine,
            auditLogger = auditLogger
        )
        if (evalResult.first == null) {
            return evalResult.second ?: error("Authentication or authorization failed")
        }
        val session = evalResult.first!!

        return writeFile(session.sessionId, path, content)
    }

    private suspend fun writeFile(sessionId: String, path: String, content: String): Map<String, Any?> {
        return withContext(Dispatchers.IO) {
            try {
                val uri = Uri.parse(path)
                val safManager = SAFManager(context)
                val docFile = safManager.resolveUri(uri)
                val targetUri = docFile?.uri ?: uri

                context.contentResolver.openOutputStream(targetUri, "wt")?.use { output ->
                    val bytes = content.toByteArray(Charsets.UTF_8)
                    output.write(bytes)
                    auditLogger.logFileOperation(
                        sessionId = sessionId,
                        operation = PolicyOperation.WRITE.name,
                        resource = path,
                        result = OperationResult.SUCCESS
                    )
                    mapOf(
                        "success" to true,
                        "bytesWritten" to bytes.size,
                        "path" to path
                    )
                } ?: run {
                    auditLogger.logFileOperation(
                        sessionId = sessionId,
                        operation = PolicyOperation.WRITE.name,
                        resource = path,
                        result = OperationResult.FAILURE
                    )
                    error("Failed to open file for writing")
                }
            } catch (e: Exception) {
                auditLogger.logFileOperation(
                    sessionId = sessionId,
                    operation = PolicyOperation.WRITE.name,
                    resource = path,
                    result = OperationResult.FAILURE
                )
                error("Write failed: ${e.message}")
            }
        }
    }

    private fun error(message: String): Map<String, Any?> = mapOf("success" to false, "error" to message)
}

/**
 * Handles filesystem delete operations.
 */
class DeleteFileHandler(
    private val context: Context,
    private val sessionManager: SessionManager,
    private val policyEngine: PolicyEngine,
    private val auditLogger: AuditLogger
) : MCPHandler {

    private val deleteOperation = DeleteOperation(context)

    override suspend fun handle(invocation: MCPToolInvocation): Map<String, Any?> {
        val args = invocation.arguments
        val rawPath = args["path"] as? String
            ?: return error("Missing required parameter: path")

        val path = try {
            PathSanitizer.sanitizePath(rawPath)
        } catch (e: Exception) {
            return error("Path traversal attempt detected: ${e.message}")
        }

        val evalResult = validateAndEvaluate(
            invocation = invocation,
            path = path,
            operation = PolicyOperation.DELETE,
            requiredCapabilities = setOf(Capability.FILESYSTEM_DELETE),
            sessionManager = sessionManager,
            policyEngine = policyEngine,
            auditLogger = auditLogger
        )
        if (evalResult.first == null) {
            return evalResult.second ?: error("Authentication or authorization failed")
        }
        val session = evalResult.first!!

        val uri = Uri.parse(path)
        return when (val res = deleteOperation.deleteFile(uri)) {
            is FileOperationResult.Success -> {
                auditLogger.logFileOperation(
                    sessionId = session.sessionId,
                    operation = PolicyOperation.DELETE.name,
                    resource = path,
                    result = OperationResult.SUCCESS
                )
                mapOf("success" to true, "path" to path)
            }
            is FileOperationResult.Failure -> {
                auditLogger.logFileOperation(
                    sessionId = session.sessionId,
                    operation = PolicyOperation.DELETE.name,
                    resource = path,
                    result = OperationResult.FAILURE
                )
                error("Delete failed: ${res.message}")
            }
        }
    }

    private fun error(message: String): Map<String, Any?> = mapOf("success" to false, "error" to message)
}

/**
 * Handles directory listing operations.
 */
class ListDirectoryHandler(
    private val context: Context,
    private val sessionManager: SessionManager,
    private val policyEngine: PolicyEngine,
    private val auditLogger: AuditLogger
) : MCPHandler {

    override suspend fun handle(invocation: MCPToolInvocation): Map<String, Any?> {
        val args = invocation.arguments
        val rawPath = args["path"] as? String
            ?: return error("Missing required parameter: path")

        val path = try {
            PathSanitizer.sanitizePath(rawPath)
        } catch (e: Exception) {
            return error("Path traversal attempt detected: ${e.message}")
        }

        val evalResult = validateAndEvaluate(
            invocation = invocation,
            path = path,
            operation = PolicyOperation.READ,
            requiredCapabilities = setOf(Capability.FILESYSTEM_READ),
            sessionManager = sessionManager,
            policyEngine = policyEngine,
            auditLogger = auditLogger
        )
        if (evalResult.first == null) {
            return evalResult.second ?: error("Authentication or authorization failed")
        }
        val session = evalResult.first!!

        return listDirectory(session.sessionId, path)
    }

    private suspend fun listDirectory(sessionId: String, path: String): Map<String, Any?> {
        return withContext(Dispatchers.IO) {
            try {
                val uri = Uri.parse(path)
                val safManager = SAFManager(context)
                val items = safManager.listFiles(uri).map { fileItem ->
                    mapOf(
                        "name" to fileItem.name,
                        "path" to fileItem.uri.toString(),
                        "isDirectory" to fileItem.isDirectory,
                        "size" to fileItem.size
                    )
                }
                auditLogger.logFileOperation(
                    sessionId = sessionId,
                    operation = PolicyOperation.READ.name,
                    resource = path,
                    result = OperationResult.SUCCESS
                )
                mapOf(
                    "success" to true,
                    "path" to path,
                    "items" to items
                )
            } catch (e: Exception) {
                auditLogger.logFileOperation(
                    sessionId = sessionId,
                    operation = PolicyOperation.READ.name,
                    resource = path,
                    result = OperationResult.FAILURE
                )
                error("List directory failed: ${e.message}")
            }
        }
    }

    private fun error(message: String): Map<String, Any?> = mapOf("success" to false, "error" to message)
}

/**
 * Handles file search operations.
 */
class SearchHandler(
    private val context: Context,
    private val sessionManager: SessionManager,
    private val policyEngine: PolicyEngine,
    private val auditLogger: AuditLogger
) : MCPHandler {

    override suspend fun handle(invocation: MCPToolInvocation): Map<String, Any?> {
        val args = invocation.arguments
        val rawQuery = args["query"] as? String
            ?: return error("Missing required parameter: query")

        val query = try {
            PathSanitizer.sanitizeSearchQuery(rawQuery)
        } catch (e: Exception) {
            return error("Invalid search query: ${e.message}")
        }

        val evalResult = validateAndEvaluate(
            invocation = invocation,
            path = query,
            operation = PolicyOperation.SEARCH,
            requiredCapabilities = setOf(Capability.FILESYSTEM_READ),
            sessionManager = sessionManager,
            policyEngine = policyEngine,
            auditLogger = auditLogger
        )
        if (evalResult.first == null) {
            return evalResult.second ?: error("Authentication or authorization failed")
        }
        val session = evalResult.first!!

        auditLogger.logFileOperation(
            sessionId = session.sessionId,
            operation = PolicyOperation.SEARCH.name,
            resource = query,
            result = OperationResult.SUCCESS
        )

        return mapOf(
            "success" to true,
            "query" to query,
            "results" to emptyList<Map<String, Any?>>()
        )
    }

    private fun error(message: String): Map<String, Any?> = mapOf("success" to false, "error" to message)
}

/**
 * Handles file rename operations.
 */
class RenameFileHandler(
    private val context: Context,
    private val sessionManager: SessionManager,
    private val policyEngine: PolicyEngine,
    private val auditLogger: AuditLogger
) : MCPHandler {

    private val renameOperation = RenameOperation(context)

    override suspend fun handle(invocation: MCPToolInvocation): Map<String, Any?> {
        val args = invocation.arguments
        val rawPath = args["path"] as? String
            ?: return error("Missing required parameter: path")
        val rawNewName = args["newName"] as? String
            ?: return error("Missing required parameter: newName")

        val path = try {
            PathSanitizer.sanitizePath(rawPath)
        } catch (e: Exception) {
            return error("Path traversal attempt detected: ${e.message}")
        }

        val newName = try {
            PathSanitizer.sanitizeFileName(rawNewName)
        } catch (e: Exception) {
            return error("Invalid file name: ${e.message}")
        }

        val evalResult = validateAndEvaluate(
            invocation = invocation,
            path = path,
            operation = PolicyOperation.RENAME,
            requiredCapabilities = setOf(Capability.FILESYSTEM_WRITE),
            sessionManager = sessionManager,
            policyEngine = policyEngine,
            auditLogger = auditLogger
        )
        if (evalResult.first == null) {
            return evalResult.second ?: error("Authentication or authorization failed")
        }
        val session = evalResult.first!!

        val uri = Uri.parse(path)
        return when (val res = renameOperation.rename(uri, newName)) {
            is FileOperationResult.Success -> {
                auditLogger.logFileOperation(
                    sessionId = session.sessionId,
                    operation = PolicyOperation.RENAME.name,
                    resource = path,
                    result = OperationResult.SUCCESS
                )
                mapOf("success" to true, "newName" to newName, "item" to res.data.name)
            }
            is FileOperationResult.Failure -> {
                auditLogger.logFileOperation(
                    sessionId = session.sessionId,
                    operation = PolicyOperation.RENAME.name,
                    resource = path,
                    result = OperationResult.FAILURE
                )
                error("Rename failed: ${res.message}")
            }
        }
    }

    private fun error(message: String): Map<String, Any?> = mapOf("success" to false, "error" to message)
}

/**
 * Handles file copy operations.
 */
class CopyFileHandler(
    private val context: Context,
    private val sessionManager: SessionManager,
    private val policyEngine: PolicyEngine,
    private val auditLogger: AuditLogger
) : MCPHandler {

    private val copyOperation = CopyOperation(context)

    override suspend fun handle(invocation: MCPToolInvocation): Map<String, Any?> {
        val args = invocation.arguments
        val rawSource = (args["sourcePath"] ?: args["path"]) as? String
            ?: return error("Missing required parameter: sourcePath")
        val rawDest = (args["destinationPath"] ?: args["destPath"]) as? String
            ?: return error("Missing required parameter: destinationPath")

        val sourcePath = try { PathSanitizer.sanitizePath(rawSource) } catch (e: Exception) { return error("Invalid source path: ${e.message}") }
        val destPath = try { PathSanitizer.sanitizePath(rawDest) } catch (e: Exception) { return error("Invalid destination path: ${e.message}") }

        val evalResult = validateAndEvaluate(
            invocation = invocation,
            path = sourcePath,
            operation = PolicyOperation.COPY,
            requiredCapabilities = setOf(Capability.FILESYSTEM_READ, Capability.FILESYSTEM_WRITE),
            sessionManager = sessionManager,
            policyEngine = policyEngine,
            auditLogger = auditLogger
        )
        if (evalResult.first == null) {
            return evalResult.second ?: error("Authentication or authorization failed")
        }
        val session = evalResult.first!!

        val sourceUri = Uri.parse(sourcePath)
        val destUri = Uri.parse(destPath)
        return when (val res = copyOperation.copyFile(sourceUri, destUri, overwrite = false)) {
            is FileOperationResult.Success -> {
                auditLogger.logFileOperation(
                    sessionId = session.sessionId,
                    operation = PolicyOperation.COPY.name,
                    resource = sourcePath,
                    result = OperationResult.SUCCESS
                )
                mapOf("success" to true, "sourcePath" to sourcePath, "destinationPath" to destPath)
            }
            is FileOperationResult.Failure -> {
                auditLogger.logFileOperation(
                    sessionId = session.sessionId,
                    operation = PolicyOperation.COPY.name,
                    resource = sourcePath,
                    result = OperationResult.FAILURE
                )
                error("Copy failed: ${res.message}")
            }
        }
    }

    private fun error(message: String): Map<String, Any?> = mapOf("success" to false, "error" to message)
}

/**
 * Handles file move operations.
 */
class MoveFileHandler(
    private val context: Context,
    private val sessionManager: SessionManager,
    private val policyEngine: PolicyEngine,
    private val auditLogger: AuditLogger
) : MCPHandler {

    private val moveOperation = MoveOperation(context)

    override suspend fun handle(invocation: MCPToolInvocation): Map<String, Any?> {
        val args = invocation.arguments
        val rawSource = (args["sourcePath"] ?: args["path"]) as? String
            ?: return error("Missing required parameter: sourcePath")
        val rawDest = (args["destinationPath"] ?: args["destPath"]) as? String
            ?: return error("Missing required parameter: destinationPath")

        val sourcePath = try { PathSanitizer.sanitizePath(rawSource) } catch (e: Exception) { return error("Invalid source path: ${e.message}") }
        val destPath = try { PathSanitizer.sanitizePath(rawDest) } catch (e: Exception) { return error("Invalid destination path: ${e.message}") }

        val evalResult = validateAndEvaluate(
            invocation = invocation,
            path = sourcePath,
            operation = PolicyOperation.MOVE,
            requiredCapabilities = setOf(Capability.FILESYSTEM_WRITE),
            sessionManager = sessionManager,
            policyEngine = policyEngine,
            auditLogger = auditLogger
        )
        if (evalResult.first == null) {
            return evalResult.second ?: error("Authentication or authorization failed")
        }
        val session = evalResult.first!!

        val sourceUri = Uri.parse(sourcePath)
        val destUri = Uri.parse(destPath)
        return when (val res = moveOperation.moveFile(sourceUri, destUri, overwrite = false)) {
            is FileOperationResult.Success -> {
                auditLogger.logFileOperation(
                    sessionId = session.sessionId,
                    operation = PolicyOperation.MOVE.name,
                    resource = sourcePath,
                    result = OperationResult.SUCCESS
                )
                mapOf("success" to true, "sourcePath" to sourcePath, "destinationPath" to destPath)
            }
            is FileOperationResult.Failure -> {
                auditLogger.logFileOperation(
                    sessionId = session.sessionId,
                    operation = PolicyOperation.MOVE.name,
                    resource = sourcePath,
                    result = OperationResult.FAILURE
                )
                error("Move failed: ${res.message}")
            }
        }
    }

    private fun error(message: String): Map<String, Any?> = mapOf("success" to false, "error" to message)
}

/**
 * Handles file creation operations.
 */
class CreateFileHandler(
    private val context: Context,
    private val sessionManager: SessionManager,
    private val policyEngine: PolicyEngine,
    private val auditLogger: AuditLogger
) : MCPHandler {

    private val createFileOperation = CreateFileOperation(context)
    private val createFolderOperation = CreateFolderOperation(context)

    override suspend fun handle(invocation: MCPToolInvocation): Map<String, Any?> {
        val args = invocation.arguments
        val rawParent = (args["parentPath"] ?: args["path"]) as? String
            ?: return error("Missing required parameter: parentPath")
        val rawName = args["name"] as? String
            ?: return error("Missing required parameter: name")
        val isFolder = (args["isFolder"] as? Boolean) ?: false
        val content = (args["content"] as? String) ?: ""

        val parentPath = try { PathSanitizer.sanitizePath(rawParent) } catch (e: Exception) { return error("Invalid parent path: ${e.message}") }
        val name = try { PathSanitizer.sanitizeFileName(rawName) } catch (e: Exception) { return error("Invalid name: ${e.message}") }

        val evalResult = validateAndEvaluate(
            invocation = invocation,
            path = parentPath,
            operation = PolicyOperation.CREATE,
            requiredCapabilities = setOf(Capability.FILESYSTEM_WRITE),
            sessionManager = sessionManager,
            policyEngine = policyEngine,
            auditLogger = auditLogger
        )
        if (evalResult.first == null) {
            return evalResult.second ?: error("Authentication or authorization failed")
        }
        val session = evalResult.first!!

        val parentUri = Uri.parse(parentPath)
        return if (isFolder) {
            when (val res = createFolderOperation.createDirectory(parentUri, name)) {
                is FileOperationResult.Success -> {
                    auditLogger.logFileOperation(
                        sessionId = session.sessionId,
                        operation = PolicyOperation.CREATE.name,
                        resource = parentPath,
                        result = OperationResult.SUCCESS
                    )
                    mapOf("success" to true, "name" to name, "isDirectory" to true)
                }
                is FileOperationResult.Failure -> {
                    auditLogger.logFileOperation(
                        sessionId = session.sessionId,
                        operation = PolicyOperation.CREATE.name,
                        resource = parentPath,
                        result = OperationResult.FAILURE
                    )
                    error("Folder creation failed: ${res.message}")
                }
            }
        } else {
            when (val res = createFileOperation.createFile(parentUri, name, "text/plain", content.toByteArray(Charsets.UTF_8))) {
                is FileOperationResult.Success -> {
                    auditLogger.logFileOperation(
                        sessionId = session.sessionId,
                        operation = PolicyOperation.CREATE.name,
                        resource = parentPath,
                        result = OperationResult.SUCCESS
                    )
                    mapOf("success" to true, "name" to name, "isDirectory" to false)
                }
                is FileOperationResult.Failure -> {
                    auditLogger.logFileOperation(
                        sessionId = session.sessionId,
                        operation = PolicyOperation.CREATE.name,
                        resource = parentPath,
                        result = OperationResult.FAILURE
                    )
                    error("File creation failed: ${res.message}")
                }
            }
        }
    }

    private fun error(message: String): Map<String, Any?> = mapOf("success" to false, "error" to message)
}

/**
 * Unsupported handlers (explicitly labeled unsupported responses).
 */
class MetadataHandler(
    ctx: Context,
    sessionManager: SessionManager,
    pe: PolicyEngine,
    al: AuditLogger
) : MCPHandler {
    override suspend fun handle(invocation: MCPToolInvocation) = mapOf(
        "success" to false,
        "code" to "UNSUPPORTED_OPERATION",
        "error" to "Unsupported operation: Metadata handler is not supported in Phase 1a"
    )
}

class ArchiveHandler(
    ctx: Context,
    sessionManager: SessionManager,
    pe: PolicyEngine,
    al: AuditLogger
) : MCPHandler {
    override suspend fun handle(invocation: MCPToolInvocation) = mapOf(
        "success" to false,
        "code" to "UNSUPPORTED_OPERATION",
        "error" to "Unsupported operation: Archive handler is not supported in Phase 1a"
    )
}

class ShareHandler(
    ctx: Context,
    sessionManager: SessionManager,
    pe: PolicyEngine,
    al: AuditLogger
) : MCPHandler {
    override suspend fun handle(invocation: MCPToolInvocation) = mapOf(
        "success" to false,
        "code" to "UNSUPPORTED_OPERATION",
        "error" to "Unsupported operation: Share handler is not supported in Phase 1a"
    )
}
