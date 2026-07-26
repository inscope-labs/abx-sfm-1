package com.inscopelabs.sfm.file.navigation

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.inscopelabs.sfm.core.exception.FileNotFoundException
import com.inscopelabs.sfm.core.exception.InvalidFileOperationException
import com.inscopelabs.sfm.core.model.FileItem
import com.inscopelabs.sfm.core.model.FileOperationError
import com.inscopelabs.sfm.core.model.FileOperationResult
import com.inscopelabs.sfm.core.model.SortOption
import com.inscopelabs.sfm.storage.DocumentFileExtensions.toFileItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Handles directory navigation operations.
 */
class DirectoryNavigator(private val context: Context) {

    private val navigationHistory = mutableListOf<Uri>()
    private var currentIndex = -1

    /**
     * Lists contents of a directory.
     */
    suspend fun listDirectory(
        uri: Uri,
        sortOption: SortOption = SortOption.NAME_ASC
    ): FileOperationResult<List<FileItem>> = withContext(Dispatchers.IO) {
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

            val files = directory.listFiles()
                .mapNotNull { it.toFileItem() }
                .sortedWith { a, b -> sortOption.compare(a, b) }

            FileOperationResult.Success(files)
        } catch (e: SecurityException) {
            FileOperationResult.Failure(
                FileOperationError.PERMISSION_DENIED,
                "Permission denied: ${e.message}",
                e
            )
        } catch (e: Exception) {
            FileOperationResult.Failure(
                FileOperationError.UNKNOWN_ERROR,
                "Failed to list directory: ${e.message}",
                e
            )
        }
    }

    /**
     * Navigates to a child directory.
     */
    suspend fun navigateTo(uri: Uri, childName: String): FileOperationResult<FileItem> =
        withContext(Dispatchers.IO) {
            try {
                val directory = DocumentFile.fromTreeUri(context, uri)
                    ?: return@withContext FileOperationResult.Failure(
                        FileOperationError.NOT_FOUND,
                        "Directory not found"
                    )

                val child = directory.findFile(childName)
                    ?: return@withContext FileOperationResult.Failure(
                        FileOperationError.NOT_FOUND,
                        "Child not found: $childName"
                    )

                val childItem = child.toFileItem()
                    ?: return@withContext FileOperationResult.Failure(
                        FileOperationError.UNKNOWN_ERROR,
                        "Failed to read child"
                    )

                if (!childItem.isDirectory) {
                    return@withContext FileOperationResult.Failure(
                        FileOperationError.INVALID_PATH,
                        "Not a directory"
                    )
                }

                // Add to history
                addToHistory(uri)

                FileOperationResult.Success(childItem)
            } catch (e: Exception) {
                FileOperationResult.Failure(
                    FileOperationError.UNKNOWN_ERROR,
                    "Navigation failed: ${e.message}",
                    e
                )
            }
        }

    /**
     * Navigates to parent directory.
     */
    suspend fun navigateToParent(uri: Uri): FileOperationResult<FileItem?> =
        withContext(Dispatchers.IO) {
            try {
                val directory = DocumentFile.fromTreeUri(context, uri)
                    ?: return@withContext FileOperationResult.Failure(
                        FileOperationError.NOT_FOUND,
                        "Directory not found"
                    )

                // Store current in history
                addToHistory(uri)

                // Get parent from URI
                val parentUri = getParentUri(uri)
                    ?: return@withContext FileOperationResult.Success(null)

                val parentDir = DocumentFile.fromTreeUri(context, parentUri)
                val parentItem = parentDir?.toFileItem()
                    ?: return@withContext FileOperationResult.Success(null)

                FileOperationResult.Success(parentItem)
            } catch (e: Exception) {
                FileOperationResult.Failure(
                    FileOperationError.UNKNOWN_ERROR,
                    "Navigation failed: ${e.message}",
                    e
                )
            }
        }

    /**
     * Gets breadcrumb path from root to directory.
     */
    suspend fun getBreadcrumbPath(uri: Uri): List<FileItem> = withContext(Dispatchers.IO) {
        val breadcrumbs = mutableListOf<FileItem>()
        var currentUri: Uri? = uri

        while (currentUri != null) {
            val doc = DocumentFile.fromTreeUri(context, currentUri)
            val item = doc?.toFileItem()
            if (item != null) {
                breadcrumbs.add(0, item)
            }
            currentUri = getParentUri(currentUri)
        }

        breadcrumbs
    }

    /**
     * Goes back in navigation history.
     */
    fun goBack(): Uri? {
        if (currentIndex > 0) {
            currentIndex--
            return navigationHistory[currentIndex]
        }
        return null
    }

    /**
     * Goes forward in navigation history.
     */
    fun goForward(): Uri? {
        if (currentIndex < navigationHistory.size - 1) {
            currentIndex++
            return navigationHistory[currentIndex]
        }
        return null
    }

    /**
     * Checks if back navigation is available.
     */
    fun canGoBack(): Boolean = currentIndex > 0

    /**
     * Checks if forward navigation is available.
     */
    fun canGoForward(): Boolean = currentIndex < navigationHistory.size - 1

    /**
     * Clears navigation history.
     */
    fun clearHistory() {
        navigationHistory.clear()
        currentIndex = -1
    }

    /**
     * Gets directory statistics.
     */
    suspend fun getDirectoryStats(uri: Uri): FileOperationResult<DirectoryStats> =
        withContext(Dispatchers.IO) {
            try {
                val directory = DocumentFile.fromTreeUri(context, uri)
                    ?: return@withContext FileOperationResult.Failure(
                        FileOperationError.NOT_FOUND,
                        "Directory not found"
                    )

                var fileCount = 0
                var directoryCount = 0
                var totalSize = 0L

                countContents(directory, 0).let { (files, dirs, size) ->
                    fileCount = files
                    directoryCount = dirs
                    totalSize = size
                }

                FileOperationResult.Success(
                    DirectoryStats(
                        fileCount = fileCount,
                        directoryCount = directoryCount,
                        totalSize = totalSize
                    )
                )
            } catch (e: Exception) {
                FileOperationResult.Failure(
                    FileOperationError.UNKNOWN_ERROR,
                    "Failed to get stats: ${e.message}",
                    e
                )
            }
        }

    private fun countContents(
        directory: DocumentFile,
        depth: Int
    ): Triple<Int, Int, Long> {
        var files = 0
        var directories = 0
        var size = 0L

        if (depth > MAX_DEPTH) return Triple(0, 0, 0)

        directory.listFiles().forEach { file ->
            if (file.isDirectory) {
                directories++
                val (f, d, s) = countContents(file, depth + 1)
                files += f
                directories += d
                size += s
            } else {
                files++
                size += file.length()
            }
        }

        return Triple(files, directories, size)
    }

    private fun addToHistory(uri: Uri) {
        // Remove any forward history
        while (navigationHistory.size > currentIndex + 1) {
            navigationHistory.removeAt(navigationHistory.size - 1)
        }
        navigationHistory.add(uri)
        currentIndex = navigationHistory.size - 1
    }

    private fun getParentUri(uri: Uri): Uri? {
        val uriString = uri.toString()
        val encodedPath = uri.encodedPath ?: return null
        val lastSlash = encodedPath.lastIndexOf('/')
        return if (lastSlash > 0) {
            val newPath = encodedPath.substring(0, lastSlash)
            Uri.parse(uri.toString().replace(encodedPath, newPath))
        } else {
            null
        }
    }

    data class DirectoryStats(
        val fileCount: Int,
        val directoryCount: Int,
        val totalSize: Long
    )

    companion object {
        private const val MAX_DEPTH = 10
    }
}
