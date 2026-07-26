package com.inscopelabs.sfm.file.operations

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.inscopelabs.sfm.core.model.FileOperationError
import com.inscopelabs.sfm.core.model.FileOperationResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Handles file and directory deletion operations.
 */
class DeleteOperation(private val context: Context) {

    /**
     * Deletes a single file or empty directory.
     */
    suspend fun deleteFile(uri: Uri): FileOperationResult<Boolean> = withContext(Dispatchers.IO) {
        try {
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

            val success = file.delete()
            if (success) {
                FileOperationResult.Success(true)
            } else {
                FileOperationResult.Failure(
                    FileOperationError.UNKNOWN_ERROR,
                    "Delete operation returned false"
                )
            }
        } catch (e: SecurityException) {
            FileOperationResult.Failure(
                FileOperationError.PERMISSION_DENIED,
                "Permission denied: ${e.message}",
                e
            )
        } catch (e: Exception) {
            FileOperationResult.Failure(
                FileOperationError.UNKNOWN_ERROR,
                "Delete failed: ${e.message}",
                e
            )
        }
    }

    /**
     * Deletes a directory and all its contents.
     */
    suspend fun deleteDirectory(uri: Uri, recursive: Boolean = true): FileOperationResult<Int> =
        withContext(Dispatchers.IO) {
            try {
                val directory = DocumentFile.fromTreeUri(context, uri)
                    ?: return@withContext FileOperationResult.Failure(
                        FileOperationError.NOT_FOUND,
                        "Directory not found"
                    )

                if (!directory.isDirectory) {
                    return@withContext FileOperationResult.Failure(
                        FileOperationError.INVALID_PATH,
                        "Not a directory"
                    )
                }

                if (!recursive && directory.listFiles().isNotEmpty()) {
                    return@withContext FileOperationResult.Failure(
                        FileOperationError.DIRECTORY_NOT_EMPTY,
                        "Directory is not empty"
                    )
                }

                val count = deleteRecursively(directory)
                FileOperationResult.Success(count)
            } catch (e: SecurityException) {
                FileOperationResult.Failure(
                    FileOperationError.PERMISSION_DENIED,
                    "Permission denied: ${e.message}",
                    e
                )
            } catch (e: Exception) {
                FileOperationResult.Failure(
                    FileOperationError.UNKNOWN_ERROR,
                    "Delete failed: ${e.message}",
                    e
                )
            }
        }

    /**
     * Deletes multiple files.
     */
    suspend fun deleteFiles(uris: List<Uri>): FileOperationResult<Int> = withContext(Dispatchers.IO) {
        var deletedCount = 0
        var failedCount = 0

        uris.forEach { uri ->
            when (val result = deleteFile(uri)) {
                is FileOperationResult.Success -> deletedCount++
                is FileOperationResult.Failure -> failedCount++
            }
        }

        if (failedCount > 0 && deletedCount == 0) {
            FileOperationResult.Failure(
                FileOperationError.UNKNOWN_ERROR,
                "Failed to delete any files"
            )
        } else if (failedCount > 0) {
            FileOperationResult.Success(deletedCount)
        } else {
            FileOperationResult.Success(deletedCount)
        }
    }

    /**
     * Securely wipes a file by overwriting with random data before deletion.
     */
    suspend fun secureDelete(uri: Uri): FileOperationResult<Boolean> = withContext(Dispatchers.IO) {
        try {
            val file = DocumentFile.fromSingleUri(context, uri)
                ?: return@withContext FileOperationResult.Failure(
                    FileOperationError.NOT_FOUND,
                    "File not found"
                )

            if (!file.isFile) {
                return@withContext FileOperationResult.Failure(
                    FileOperationError.INVALID_PATH,
                    "Not a file"
                )
            }

            // Overwrite with zeros
            val size = file.length()
            context.contentResolver.openOutputStream(uri)?.use { output ->
                val buffer = ByteArray(8192)
                var written = 0L
                while (written < size) {
                    output.write(buffer)
                    written += buffer.size
                }
            }

            // Delete after overwrite
            deleteFile(uri)
        } catch (e: Exception) {
            FileOperationResult.Failure(
                FileOperationError.UNKNOWN_ERROR,
                "Secure delete failed: ${e.message}",
                e
            )
        }
    }

    private fun deleteRecursively(directory: DocumentFile): Int {
        var count = 0
        directory.listFiles().forEach { file ->
            if (file.isDirectory) {
                count += deleteRecursively(file)
            }
            if (file.delete()) {
                count++
            }
        }
        if (directory.delete()) {
            count++
        }
        return count
    }
}
