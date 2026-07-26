package com.inscopelabs.sfm.plugin.api

import android.net.Uri
import com.inscopelabs.sfm.core.model.FileItem
import com.inscopelabs.sfm.security.permissions.Capability

/**
 * Secure API exposed to JavaScript plugins.
 * All operations go through the capability layer.
 */
class PluginAPI(
    private val context: android.content.Context,
    private val sessionId: String,
    private val grantedCapabilities: Set<Capability>,
    private val capabilityChecker: (Capability) -> Boolean
) {

    /**
     * Reads a file (requires FILESYSTEM_READ capability).
     */
    fun readFile(path: String): String? {
        if (!capabilityChecker(Capability.FILESYSTEM_READ)) {
            throw SecurityException("Capability denied: filesystem.read")
        }
        // Implementation delegates to FileManager
        return null
    }

    /**
     * Writes to a file (requires FILESYSTEM_WRITE capability).
     */
    fun writeFile(path: String, content: String): Boolean {
        if (!capabilityChecker(Capability.FILESYSTEM_WRITE)) {
            throw SecurityException("Capability denied: filesystem.write")
        }
        return false
    }

    /**
     * Lists directory (requires FILESYSTEM_READ capability).
     */
    fun listDirectory(path: String): List<FileItem>? {
        if (!capabilityChecker(Capability.FILESYSTEM_READ)) {
            throw SecurityException("Capability denied: filesystem.read")
        }
        return null
    }

    /**
     * Searches files (requires FILESYSTEM_SEARCH capability).
     */
    fun search(query: String, options: SearchOptions? = null): List<FileItem>? {
        if (!capabilityChecker(Capability.FILESYSTEM_SEARCH)) {
            throw SecurityException("Capability denied: filesystem.search")
        }
        return null
    }

    /**
     * Renames file (requires FILESYSTEM_RENAME capability).
     */
    fun rename(source: String, newName: String): Boolean {
        if (!capabilityChecker(Capability.FILESYSTEM_RENAME)) {
            throw SecurityException("Capability denied: filesystem.rename")
        }
        return false
    }

    /**
     * Deletes file (requires FILESYSTEM_DELETE capability).
     */
    fun delete(path: String, recursive: Boolean = false): Boolean {
        if (!capabilityChecker(Capability.FILESYSTEM_DELETE)) {
            throw SecurityException("Capability denied: filesystem.delete")
        }
        return false
    }

    /**
     * Copies file (requires FILESYSTEM_COPY capability).
     */
    fun copy(source: String, destination: String): Boolean {
        if (!capabilityChecker(Capability.FILESYSTEM_COPY)) {
            throw SecurityException("Capability denied: filesystem.copy")
        }
        return false
    }

    /**
     * Moves file (requires FILESYSTEM_MOVE capability).
     */
    fun move(source: String, destination: String): Boolean {
        if (!capabilityChecker(Capability.FILESYSTEM_MOVE)) {
            throw SecurityException("Capability denied: filesystem.move")
        }
        return false
    }

    /**
     * Gets file metadata (requires FILESYSTEM_METADATA capability).
     */
    fun getMetadata(path: String): FileItem? {
        if (!capabilityChecker(Capability.FILESYSTEM_METADATA)) {
            throw SecurityException("Capability denied: filesystem.metadata")
        }
        return null
    }

    /**
     * Creates archive (requires FILESYSTEM_ARCHIVE capability).
     */
    fun createArchive(sources: List<String>, destination: String): Boolean {
        if (!capabilityChecker(Capability.FILESYSTEM_ARCHIVE)) {
            throw SecurityException("Capability denied: filesystem.archive")
        }
        return false
    }

    /**
     * Extracts archive (requires FILESYSTEM_ARCHIVE capability).
     */
    fun extractArchive(archive: String, destination: String): Boolean {
        if (!capabilityChecker(Capability.FILESYSTEM_ARCHIVE)) {
            throw SecurityException("Capability denied: filesystem.archive")
        }
        return false
    }

    /**
     * Gets session info.
     */
    fun getSessionInfo(): SessionInfo {
        return SessionInfo(
            sessionId = sessionId,
            grantedCapabilities = grantedCapabilities.map { it.name }
        )
    }

    /**
     * Logs a message through audit system.
     */
    fun log(message: String) {
        // Log through audit system
    }

    data class SearchOptions(
        val recursive: Boolean = true,
        val extensions: List<String>? = null,
        val maxResults: Int = 100
    )

    data class SessionInfo(
        val sessionId: String,
        val grantedCapabilities: List<String>
    )
}
