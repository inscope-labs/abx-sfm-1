package com.inscopelabs.sfm.file.operations

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.inscopelabs.sfm.core.exception.InvalidFileNameException
import com.inscopelabs.sfm.core.exception.InvalidFileOperationException
import com.inscopelabs.sfm.core.model.FileItem
import com.inscopelabs.sfm.core.model.FileOperationError
import com.inscopelabs.sfm.core.model.FileOperationResult
import com.inscopelabs.sfm.storage.DocumentFileExtensions.sanitizeFileName
import com.inscopelabs.sfm.storage.DocumentFileExtensions.toFileItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Handles file and directory rename operations.
 */
class RenameOperation(private val context: Context) {

    companion object {
        private val INVALID_CHARS = charArrayOf('/', '\\', ':', '*', '?', '"', '<', '>', '|', '\u0000')
        private val RESERVED_NAMES = setOf(
            "CON", "PRN", "AUX", "NUL",
            "COM1", "COM2", "COM3", "COM4", "COM5", "COM6", "COM7", "COM8", "COM9",
            "LPT1", "LPT2", "LPT3", "LPT4", "LPT5", "LPT6", "LPT7", "LPT8", "LPT9"
        )
    }

    /**
     * Renames a file or directory.
     */
    suspend fun rename(uri: Uri, newName: String): FileOperationResult<FileItem> =
        withContext(Dispatchers.IO) {
            try {
                // Validate new name
                validateNewName(newName)

                val sanitizedName = sanitizeFileName(newName)

                if (sanitizedName.isBlank()) {
                    return@withContext FileOperationResult.Failure(
                        FileOperationError.INVALID_NAME,
                        "Name cannot be empty or contain only invalid characters"
                    )
                }

                val file = DocumentFile.fromSingleUri(context, uri)
                    ?: return@withContext FileOperationResult.Failure(
                        FileOperationError.NOT_FOUND,
                        "File not found"
                    )

                if (!file.canWrite()) {
                    return@withContext FileOperationResult.Failure(
                        FileOperationError.FILE_IS_READ_ONLY,
                        "File is read-only"
                    )
                }

                // Check if new name already exists in parent
                val parentUri = getParentUri(uri)
                if (parentUri != null) {
                    val parentDir = DocumentFile.fromTreeUri(context, parentUri)
                    val existing = parentDir?.findFile(sanitizedName)
                    if (existing != null && existing.uri != uri) {
                        return@withContext FileOperationResult.Failure(
                            FileOperationError.FILE_ALREADY_EXISTS,
                            "A file with this name already exists"
                        )
                    }
                }

                val success = file.renameTo(sanitizedName)
                if (success) {
                    val renamedFile = file.toFileItem()
                        ?: return@withContext FileOperationResult.Failure(
                            FileOperationError.UNKNOWN_ERROR,
                            "Rename succeeded but file not found"
                        )
                    FileOperationResult.Success(renamedFile)
                } else {
                    FileOperationResult.Failure(
                        FileOperationError.UNKNOWN_ERROR,
                        "Rename operation failed"
                    )
                }
            } catch (e: SecurityException) {
                FileOperationResult.Failure(
                    FileOperationError.PERMISSION_DENIED,
                    "Permission denied: ${e.message}",
                    e
                )
            } catch (e: InvalidFileNameException) {
                FileOperationResult.Failure(
                    FileOperationError.INVALID_NAME,
                    e.message ?: "Invalid file name",
                    e
                )
            } catch (e: Exception) {
                FileOperationResult.Failure(
                    FileOperationError.UNKNOWN_ERROR,
                    "Rename failed: ${e.message}",
                    e
                )
            }
        }

    /**
     * Validates a new file name for safety.
     */
    fun validateNewName(name: String) {
        if (name.isBlank()) {
            throw InvalidFileNameException("Name cannot be blank")
        }

        // Check for invalid characters
        for (char in INVALID_CHARS) {
            if (name.contains(char)) {
                throw InvalidFileNameException(
                    "Name cannot contain: ${char}",
                    InvalidFileNameException(name)
                )
            }
        }

        // Check for path traversal
        if (name.contains("..")) {
            throw InvalidFileNameException("Name cannot contain '..'")
        }

        // Check for leading/trailing spaces or dots
        if (name != name.trim() || name.endsWith('.')) {
            throw InvalidFileNameException("Name cannot have leading/trailing spaces or dots")
        }

        // Check for reserved names (Windows compatibility)
        val upperName = name.uppercase().substringBeforeLast('.')
        if (RESERVED_NAMES.contains(upperName)) {
            throw InvalidFileNameException("Name '$name' is reserved")
        }

        // Check length
        if (name.length > 255) {
            throw InvalidFileNameException("Name is too long (max 255 characters)")
        }
    }

    private fun getParentUri(uri: Uri): Uri? {
        val uriString = uri.toString()
        val lastSlash = uriString.lastIndexOf('/')
        return if (lastSlash > 0) {
            Uri.parse(uriString.substring(0, lastSlash))
        } else {
            null
        }
    }
}
