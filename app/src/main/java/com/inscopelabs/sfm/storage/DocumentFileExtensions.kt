package com.inscopelabs.sfm.storage

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.inscopelabs.sfm.core.exception.FileNotFoundException
import com.inscopelabs.sfm.core.exception.InvalidFileOperationException
import com.inscopelabs.sfm.core.model.FileItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Extension functions for DocumentFile operations.
 */
object DocumentFileExtensions {

    /**
     * Converts DocumentFile to FileItem safely.
     */
    fun DocumentFile.toFileItem(): FileItem? {
        return try {
            val name = this.name ?: return null
            FileItem(
                uri = this.uri,
                name = name,
                path = this.uri.path ?: this.uri.toString(),
                mimeType = this.type,
                size = this.length(),
                lastModified = this.lastModified(),
                isDirectory = this.isDirectory,
                isReadable = this.canRead(),
                isWritable = this.canWrite()
            )
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Safely lists files in a DocumentFile directory.
     */
    suspend fun DocumentFile.safeListFiles(): List<DocumentFile> = withContext(Dispatchers.IO) {
        try {
            if (!this@safeListFiles.isDirectory) {
                return@withContext emptyList()
            }
            this@safeListFiles.listFiles().toList()
        } catch (e: SecurityException) {
            emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Finds a child file or directory by name.
     */
    suspend fun DocumentFile.findChild(name: String): DocumentFile? = withContext(Dispatchers.IO) {
        try {
            this@findChild.findFile(name)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Safely creates a file with conflict handling.
     */
    suspend fun DocumentFile.safeCreateFile(
        mimeType: String,
        name: String
    ): DocumentFile = withContext(Dispatchers.IO) {
        if (!this@safeCreateFile.isDirectory) {
            throw InvalidFileOperationException("Not a directory")
        }

        // Check for existing file
        var existing = this@safeCreateFile.findFile(name)
        if (existing != null) {
            return@withContext existing
        }

        // Create new file
        val newFile = this@safeCreateFile.createFile(mimeType, name)
            ?: throw InvalidFileOperationException("Failed to create file: $name")

        newFile
    }

    /**
     * Safely creates a directory with conflict handling.
     */
    suspend fun DocumentFile.safeCreateDirectory(name: String): DocumentFile = withContext(Dispatchers.IO) {
        if (!this@safeCreateDirectory.isDirectory) {
            throw InvalidFileOperationException("Not a directory")
        }

        // Check for existing directory
        var existing = this@safeCreateDirectory.findFile(name)
        if (existing != null && existing.isDirectory) {
            return@withContext existing
        }

        // Create new directory
        val newDir = this@safeCreateDirectory.createDirectory(name)
            ?: throw InvalidFileOperationException("Failed to create directory: $name")

        newDir
    }

    /**
     * Safely deletes a file or directory.
     */
    suspend fun DocumentFile.safeDelete(): Boolean = withContext(Dispatchers.IO) {
        try {
            this@safeDelete.delete()
        } catch (e: SecurityException) {
            false
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Safely renames a file or directory.
     */
    suspend fun DocumentFile.safeRename(newName: String): Uri? = withContext(Dispatchers.IO) {
        try {
            val success = this@safeRename.renameTo(newName)
            if (success) this@safeRename.uri else null
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Copies a file to a destination directory.
     */
    suspend fun DocumentFile.copyTo(
        context: Context,
        destination: DocumentFile,
        bufferSize: Int = 8192
    ): DocumentFile = withContext(Dispatchers.IO) {
        val sourceName = this@copyTo.name
            ?: throw InvalidFileOperationException("Source file has no name")

        val mimeType = this@copyTo.type ?: "application/octet-stream"

        // Create destination file
        val destFile = destination.createFile(mimeType, sourceName)
            ?: throw InvalidFileOperationException("Failed to create destination file")

        // Copy content
        context.contentResolver.openInputStream(this@copyTo.uri)?.use { input ->
            context.contentResolver.openOutputStream(destFile.uri)?.use { output ->
                input.copyTo(output, bufferSize)
            } ?: throw InvalidFileOperationException("Failed to open output stream")
        } ?: throw InvalidFileOperationException("Failed to open input stream")

        destFile
    }

    /**
     * Gets the total size of a directory recursively.
     */
    suspend fun DocumentFile.getTotalSize(): Long = withContext(Dispatchers.IO) {
        if (isFile) {
            return@withContext length()
        }

        var total = 0L
        try {
            listFiles().forEach { child ->
                total += child.getTotalSize()
            }
        } catch (e: Exception) {
            // Ignore errors when calculating size
        }
        total
    }

    /**
     * Counts files and directories recursively.
     */
    suspend fun DocumentFile.countContents(): Pair<Int, Int> = withContext(Dispatchers.IO) {
        var files = 0
        var directories = 0

        if (isFile) {
            return@withContext 1 to 0
        }

        try {
            listFiles().forEach { child ->
                if (child.isDirectory) {
                    directories++
                    val (f, d) = child.countContents()
                    files += f
                    directories += d
                } else {
                    files++
                }
            }
        } catch (e: Exception) {
            // Ignore errors
        }

        files to directories
    }

    /**
     * Validates that a path is safe (no path traversal).
     */
    fun validatePath(path: String): Boolean {
        val normalized = path.replace("\\", "/")

        // Check for path traversal attempts
        return !normalized.contains("..") &&
                !normalized.startsWith("/") &&
                !normalized.matches(Regex("^[a-zA-Z]:.*")) // Windows paths
    }

    /**
     * Sanitizes a file name to prevent security issues.
     */
    fun sanitizeFileName(name: String): String {
        // Remove null bytes
        var sanitized = name.replace("\u0000", "")

        // Remove path separators
        sanitized = sanitized.replace("/", "")
        sanitized = sanitized.replace("\\", "")

        // Remove common path traversal patterns
        sanitized = sanitized.replace("..", "")

        // Trim whitespace
        sanitized = sanitized.trim()

        // Limit length
        if (sanitized.length > 255) {
            val ext = sanitized.substringAfterLast('.', "")
            val nameWithoutExt = sanitized.substringBeforeLast('.')
            sanitized = nameWithoutExt.take(255 - ext.length - 1) + "." + ext
        }

        return sanitized
    }
}
