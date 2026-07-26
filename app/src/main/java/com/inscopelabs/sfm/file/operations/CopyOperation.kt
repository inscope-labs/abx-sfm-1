package com.inscopelabs.sfm.file.operations

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.inscopelabs.sfm.core.exception.FileManagerException
import com.inscopelabs.sfm.core.exception.FileNotFoundException
import com.inscopelabs.sfm.core.exception.InvalidFileOperationException
import com.inscopelabs.sfm.core.model.FileItem
import com.inscopelabs.sfm.core.model.FileOperationError
import com.inscopelabs.sfm.core.model.FileOperationResult
import com.inscopelabs.sfm.storage.DocumentFileExtensions.toFileItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Handles file copy operations with progress tracking.
 */
class CopyOperation(private val context: Context) {

    /**
     * Copies a single file to destination.
     */
    suspend fun copyFile(
        sourceUri: Uri,
        destinationUri: Uri,
        overwrite: Boolean = false,
        onProgress: (Long, Long) -> Unit = { _, _ -> }
    ): FileOperationResult<FileItem> = withContext(Dispatchers.IO) {
        try {
            val sourceFile = DocumentFile.fromSingleUri(context, sourceUri)
                ?: return@withContext FileOperationResult.Failure(
                    FileOperationError.NOT_FOUND,
                    "Source file not found"
                )

            val destDir = DocumentFile.fromTreeUri(context, destinationUri)
                ?: return@withContext FileOperationResult.Failure(
                    FileOperationError.NOT_FOUND,
                    "Destination directory not found"
                )

            if (!destDir.isDirectory) {
                return@withContext FileOperationResult.Failure(
                    FileOperationError.INVALID_PATH,
                    "Destination is not a directory"
                )
            }

            val fileName = sourceFile.name
                ?: return@withContext FileOperationResult.Failure(
                    FileOperationError.INVALID_NAME,
                    "Source file has no name"
                )

            // Check for existing file
            val existing = destDir.findFile(fileName)
            if (existing != null) {
                if (!overwrite) {
                    return@withContext FileOperationResult.Failure(
                        FileOperationError.FILE_ALREADY_EXISTS,
                        "File already exists: $fileName"
                    )
                }
                existing.delete()
            }

            // Perform copy
            val mimeType = sourceFile.type ?: "application/octet-stream"
            val newFile = destDir.createFile(mimeType, fileName)
                ?: return@withContext FileOperationResult.Failure(
                    FileOperationError.UNKNOWN_ERROR,
                    "Failed to create destination file"
                )

            val sourceSize = sourceFile.length()
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                context.contentResolver.openOutputStream(newFile.uri)?.use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    var bytesCopied = 0L
                    var bytesRead: Int

                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        bytesCopied += bytesRead
                        onProgress(bytesCopied, sourceSize)
                    }
                } ?: return@withContext FileOperationResult.Failure(
                    FileOperationError.UNKNOWN_ERROR,
                    "Failed to open output stream"
                )
            } ?: return@withContext FileOperationResult.Failure(
                FileOperationError.UNKNOWN_ERROR,
                "Failed to open input stream"
            )

            val result = newFile.toFileItem()
                ?: return@withContext FileOperationResult.Failure(
                    FileOperationError.UNKNOWN_ERROR,
                    "Failed to read copied file"
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
                "Copy failed: ${e.message}",
                e
            )
        }
    }

    /**
     * Copies multiple files to destination.
     */
    suspend fun copyFiles(
        sourceUris: List<Uri>,
        destinationUri: Uri,
        overwrite: Boolean = false,
        onProgress: (Int, Int, String) -> Unit = { _, _, _ -> }
    ): FileOperationResult<List<FileItem>> = withContext(Dispatchers.IO) {
        val results = mutableListOf<FileItem>()
        val destDir = DocumentFile.fromTreeUri(context, destinationUri)
            ?: return@withContext FileOperationResult.Failure(
                FileOperationError.NOT_FOUND,
                "Destination directory not found"
            )

        sourceUris.forEachIndexed { index, sourceUri ->
            val fileName = DocumentFile.fromSingleUri(context, sourceUri)?.name ?: "file_$index"
            onProgress(index, sourceUris.size, fileName)

            when (val result = copyFile(sourceUri, destinationUri, overwrite)) {
                is FileOperationResult.Success -> results.add(result.data)
                is FileOperationResult.Failure -> {
                    return@withContext FileOperationResult.Failure(
                        result.error,
                        "Failed to copy $fileName: ${result.message}",
                        result.cause
                    )
                }
            }
        }

        FileOperationResult.Success(results)
    }

    companion object {
        private const val DEFAULT_BUFFER_SIZE = 8192
    }
}
