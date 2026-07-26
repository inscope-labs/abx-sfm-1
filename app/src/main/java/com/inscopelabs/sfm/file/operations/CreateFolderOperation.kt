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
 * Handles directory creation operations.
 */
class CreateFolderOperation(private val context: Context) {

    /**
     * Creates a new directory.
     */
    suspend fun createDirectory(
        parentUri: Uri,
        name: String
    ): FileOperationResult<FileItem> = withContext(Dispatchers.IO) {
        try {
            val sanitizedName = sanitizeFileName(name)

            if (sanitizedName.isBlank()) {
                return@withContext FileOperationResult.Failure(
                    FileOperationError.INVALID_NAME,
                    "Directory name cannot be empty"
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
            val existing = parentDir.findFile(sanitizedName)
            if (existing != null) {
                if (existing.isDirectory) {
                    return@withContext FileOperationResult.Failure(
                        FileOperationError.FILE_ALREADY_EXISTS,
                        "Directory already exists: $sanitizedName"
                    )
                } else {
                    return@withContext FileOperationResult.Failure(
                        FileOperationError.FILE_ALREADY_EXISTS,
                        "A file with this name already exists: $sanitizedName"
                    )
                }
            }

            val newDir = parentDir.createDirectory(sanitizedName)
                ?: return@withContext FileOperationResult.Failure(
                    FileOperationError.UNKNOWN_ERROR,
                    "Failed to create directory"
                )

            val result = newDir.toFileItem()
                ?: return@withContext FileOperationResult.Failure(
                    FileOperationError.UNKNOWN_ERROR,
                    "Directory created but failed to read it"
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
                "Create directory failed: ${e.message}",
                e
            )
        }
    }

    /**
     * Creates multiple directories in a path.
     */
    suspend fun createDirectoryPath(
        parentUri: Uri,
        path: String
    ): FileOperationResult<FileItem> = withContext(Dispatchers.IO) {
        try {
            val parts = path.split("/", "\\").filter { it.isNotBlank() }
            var currentUri = parentUri

            for (part in parts) {
                val sanitizedName = sanitizeFileName(part)
                val result = createDirectory(currentUri, sanitizedName)

                when (result) {
                    is FileOperationResult.Success -> {
                        currentUri = result.data.uri
                    }
                    is FileOperationResult.Failure -> {
                        if (result.error == FileOperationError.FILE_ALREADY_EXISTS) {
                            // Directory exists, continue
                            val existingDir = DocumentFile.fromTreeUri(context, currentUri)?.findFile(sanitizedName)
                            if (existingDir != null && existingDir.isDirectory) {
                                currentUri = existingDir.uri
                            } else {
                                return@withContext result
                            }
                        } else {
                            return@withContext result
                        }
                    }
                }
            }

            // Return the last created/found directory
            val lastDir = DocumentFile.fromTreeUri(context, currentUri)?.findFile(sanitizeFileName(parts.last()))
            val result = lastDir?.toFileItem()
                ?: return@withContext FileOperationResult.Failure(
                    FileOperationError.UNKNOWN_ERROR,
                    "Failed to create path"
                )

            FileOperationResult.Success(result)
        } catch (e: Exception) {
            FileOperationResult.Failure(
                FileOperationError.UNKNOWN_ERROR,
                "Create path failed: ${e.message}",
                e
            )
        }
    }
}
