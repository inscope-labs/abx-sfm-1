package com.inscopelabs.sfm.file.operations

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.inscopelabs.sfm.core.model.FileItem
import com.inscopelabs.sfm.core.model.FileOperationError
import com.inscopelabs.sfm.core.model.FileOperationResult
import com.inscopelabs.sfm.storage.DocumentFileExtensions.sanitizeFileName
import com.inscopelabs.sfm.storage.DocumentFileExtensions.toFileItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Handles file creation operations.
 */
class CreateFileOperation(private val context: Context) {

    /**
     * Creates a new empty file.
     */
    suspend fun createFile(
        parentUri: Uri,
        name: String,
        mimeType: String = "application/octet-stream",
        content: ByteArray? = null
    ): FileOperationResult<FileItem> = withContext(Dispatchers.IO) {
        try {
            val sanitizedName = sanitizeFileName(name)

            if (sanitizedName.isBlank()) {
                return@withContext FileOperationResult.Failure(
                    FileOperationError.INVALID_NAME,
                    "File name cannot be empty"
                )
            }

            val parentDir = DocumentFile.fromTreeUri(context, parentUri)
                ?: return@withContext FileOperationResult.Failure(
                    FileOperationError.NOT_FOUND,
                    "Parent directory not found"
                )

            if (!parentDir.isDirectory) {
                return@withContext FileOperationResult.Failure(
                    FileOperationError.INVALID_PATH,
                    "Parent is not a directory"
                )
            }

            if (!parentDir.canWrite()) {
                return@withContext FileOperationResult.Failure(
                    FileOperationError.PERMISSION_DENIED,
                    "No write permission in parent directory"
                )
            }

            // Check if already exists
            var existing = parentDir.findFile(sanitizedName)
            if (existing != null) {
                return@withContext FileOperationResult.Failure(
                    FileOperationError.FILE_ALREADY_EXISTS,
                    "File already exists: $sanitizedName"
                )
            }

            val newFile = parentDir.createFile(mimeType, sanitizedName)
                ?: return@withContext FileOperationResult.Failure(
                    FileOperationError.UNKNOWN_ERROR,
                    "Failed to create file"
                )

            // Write content if provided
            if (content != null && content.isNotEmpty()) {
                context.contentResolver.openOutputStream(newFile.uri)?.use { output ->
                    output.write(content)
                } ?: return@withContext FileOperationResult.Failure(
                    FileOperationError.UNKNOWN_ERROR,
                    "Failed to write content to file"
                )
            }

            val result = newFile.toFileItem()
                ?: return@withContext FileOperationResult.Failure(
                    FileOperationError.UNKNOWN_ERROR,
                    "File created but failed to read it"
                )

            FileOperationResult.Success(result)
        } catch (e: SecurityException) {
            FileOperationResult.Failure(
                FileOperationError.PERMISSION_DENIED,
                "Permission denied: ${e.message}",
                e
            )
        } catch (e: Exception) {
            FileOperationResult.Failure(
                FileOperationError.UNKNOWN_ERROR,
                "Create file failed: ${e.message}",
                e
            )
        }
    }

    /**
     * Creates a temporary file for writing.
     */
    suspend fun createTempFile(
        name: String,
        extension: String,
        content: ByteArray? = null
    ): FileOperationResult<FileItem> = withContext(Dispatchers.IO) {
        try {
            val sanitizedName = sanitizeFileName(name)
            val fullName = if (extension.startsWith('.')) {
                "$sanitizedName$extension"
            } else {
                "$sanitizedName.$extension"
            }

            val cacheDir = context.cacheDir
            val tempFile = java.io.File(cacheDir, fullName)

            if (content != null) {
                tempFile.writeBytes(content)
            } else {
                tempFile.createNewFile()
            }

            val result = FileItem(
                uri = Uri.fromFile(tempFile),
                name = tempFile.name,
                path = tempFile.absolutePath,
                mimeType = getMimeType(extension),
                size = tempFile.length(),
                lastModified = tempFile.lastModified(),
                isDirectory = false,
                isReadable = tempFile.canRead(),
                isWritable = tempFile.canWrite()
            )

            FileOperationResult.Success(result)
        } catch (e: Exception) {
            FileOperationResult.Failure(
                FileOperationError.UNKNOWN_ERROR,
                "Create temp file failed: ${e.message}",
                e
            )
        }
    }

    private fun getMimeType(extension: String): String {
        val ext = extension.lowercase().removePrefix(".")
        return when (ext) {
            "txt" -> "text/plain"
            "json" -> "application/json"
            "xml" -> "application/xml"
            "html", "htm" -> "text/html"
            "css" -> "text/css"
            "js" -> "application/javascript"
            "png" -> "image/png"
            "jpg", "jpeg" -> "image/jpeg"
            "gif" -> "image/gif"
            "pdf" -> "application/pdf"
            "zip" -> "application/zip"
            else -> "application/octet-stream"
        }
    }
}
