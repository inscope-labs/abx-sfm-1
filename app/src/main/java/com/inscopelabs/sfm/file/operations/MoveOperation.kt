package com.inscopelabs.sfm.file.operations

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.inscopelabs.sfm.core.exception.FileNotFoundException
import com.inscopelabs.sfm.core.exception.InvalidFileOperationException
import com.inscopelabs.sfm.core.model.FileItem
import com.inscopelabs.sfm.core.model.FileOperationError
import com.inscopelabs.sfm.core.model.FileOperationResult
import com.inscopelabs.sfm.storage.DocumentFileExtensions.toFileItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Handles file move operations.
 */
class MoveOperation(private val context: Context) {

    private val copyOperation = CopyOperation(context)

    /**
     * Moves a file to destination.
     * Uses copy+delete for SAF, as SAF doesn't support direct rename across directories.
     */
    suspend fun moveFile(
        sourceUri: Uri,
        destinationUri: Uri,
        overwrite: Boolean = false
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

            val fileName = sourceFile.name
                ?: return@withContext FileOperationResult.Failure(
                    FileOperationError.INVALID_NAME,
                    "Source file has no name"
                )

            // Check if same directory (can use rename)
            val sourceParent = getParentUri(sourceUri)
            if (sourceParent == destinationUri) {
                return@withContext renameInPlace(sourceFile, fileName)
            }

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

            // Copy to destination
            val copyResult = copyOperation.copyFile(sourceUri, destinationUri, overwrite)
            if (copyResult is FileOperationResult.Failure) {
                return@withContext copyResult
            }

            // Delete source if copy succeeded
            if (sourceFile.delete()) {
                val movedFile = destDir.findFile(fileName)
                val result = movedFile?.toFileItem()
                    ?: return@withContext FileOperationResult.Failure(
                        FileOperationError.UNKNOWN_ERROR,
                        "Move completed but file not found"
                    )
                FileOperationResult.Success(result)
            } else {
                FileOperationResult.Failure(
                    FileOperationError.UNKNOWN_ERROR,
                    "Copy succeeded but source deletion failed"
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
                "Move failed: ${e.message}",
                e
            )
        }
    }

    /**
     * Renames file in place (same directory).
     */
    private suspend fun renameInPlace(
        file: DocumentFile,
        newName: String
    ): FileOperationResult<FileItem> = withContext(Dispatchers.IO) {
        val success = file.renameTo(newName)
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
                "Rename failed"
            )
        }
    }

    /**
     * Moves multiple files.
     */
    suspend fun moveFiles(
        sourceUris: List<Uri>,
        destinationUri: Uri,
        overwrite: Boolean = false
    ): FileOperationResult<List<FileItem>> = withContext(Dispatchers.IO) {
        val results = mutableListOf<FileItem>()

        sourceUris.forEach { sourceUri ->
            when (val result = moveFile(sourceUri, destinationUri, overwrite)) {
                is FileOperationResult.Success -> results.add(result.data)
                is FileOperationResult.Failure -> {
                    return@withContext FileOperationResult.Failure(
                        result.error,
                        "Failed to move some files: ${result.message}",
                        result.cause
                    )
                }
            }
        }

        FileOperationResult.Success(results)
    }

    private fun getParentUri(uri: Uri): Uri? {
        val path = uri.path ?: return null
        val lastSlash = path.lastIndexOf('/')
        return if (lastSlash > 0) {
            Uri.parse(uri.toString().substring(0, uri.toString().lastIndexOf('/')))
        } else {
            null
        }
    }
}
