package com.inscopelabs.sfm.file.search

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import androidx.documentfile.provider.DocumentFile
import com.inscopelabs.sfm.core.model.FileItem
import com.inscopelabs.sfm.core.model.FileOperationError
import com.inscopelabs.sfm.core.model.FileOperationResult
import com.inscopelabs.sfm.file.navigation.PathSanitizer
import com.inscopelabs.sfm.storage.DocumentFileExtensions.toFileItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Searches files with filters and respects authorization boundaries.
 */
class FileSearchEngine(private val context: Context) {

    private var currentSearchJob: Job? = null

    /**
     * Searches for files matching a query.
     */
    suspend fun search(
        query: String,
        rootUri: Uri? = null,
        options: SearchOptions = SearchOptions()
    ): FileOperationResult<List<FileItem>> = withContext(Dispatchers.IO) {
        try {
            val sanitizedQuery = PathSanitizer.sanitizeSearchQuery(query)

            if (sanitizedQuery.isBlank() && !options.searchByExtension) {
                return@withContext FileOperationResult.Success(emptyList())
            }

            val results = mutableListOf<FileItem>()

            if (rootUri != null) {
                // Search within specific directory
                results.addAll(searchInDirectory(rootUri, sanitizedQuery, options))
            } else {
                // Search using MediaStore
                results.addAll(searchMediaStore(sanitizedQuery, options))
            }

            // Apply post-search filters
            val filteredResults = applyFilters(results, options)

            FileOperationResult.Success(filteredResults.take(options.maxResults))
        } catch (e: Exception) {
            FileOperationResult.Failure(
                FileOperationError.UNKNOWN_ERROR,
                "Search failed: ${e.message}",
                e
            )
        }
    }

    /**
     * Searches within a specific directory.
     */
    private suspend fun searchInDirectory(
        rootUri: Uri,
        query: String,
        options: SearchOptions
    ): List<FileItem> = coroutineScope {
        val results = mutableListOf<FileItem>()
        val directory = DocumentFile.fromTreeUri(context, rootUri) ?: return@coroutineScope results

        searchRecursively(directory, query, options, results, 0)
        results
    }

    private fun searchRecursively(
        directory: DocumentFile,
        query: String,
        options: SearchOptions,
        results: MutableList<FileItem>,
        depth: Int
    ) {
        if (depth > options.maxDepth) return

        directory.listFiles().forEach { file ->
            val name = file.name ?: return@forEach

            val matches = when {
                // Match by name
                query.isNotBlank() && name.contains(query, ignoreCase = true) -> true
                // Match by extension
                options.searchByExtension && options.extensions.any { ext ->
                    name.endsWith(".$ext", ignoreCase = true)
                } -> true
                else -> false
            }

            if (matches && file.toFileItem() != null) {
                val item = file.toFileItem()!!
                if (passesFilters(item, options)) {
                    results.add(item)
                }
            }

            // Search subdirectories
            if (file.isDirectory && options.searchSubdirectories) {
                searchRecursively(file, query, options, results, depth + 1)
            }
        }
    }

    /**
     * Searches using MediaStore.
     */
    private suspend fun searchMediaStore(
        query: String,
        options: SearchOptions
    ): List<FileItem> = withContext(Dispatchers.IO) {
        val results = mutableListOf<FileItem>()

        val selection = if (query.isNotBlank()) {
            "${MediaStore.MediaColumns.DISPLAY_NAME} LIKE ?"
        } else if (options.extensions.isNotEmpty()) {
            val extConditions = options.extensions.joinToString(" OR ") {
                "${MediaStore.MediaColumns.DISPLAY_NAME} LIKE ?"
            }
            "($extConditions)"
        } else {
            null
        }

        val selectionArgs = if (query.isNotBlank()) {
            arrayOf("%$query%")
        } else if (options.extensions.isNotEmpty()) {
            options.extensions.map { "%.$it" }.toTypedArray()
        } else {
            null
        }

        context.contentResolver.query(
            MediaStore.Files.getContentUri("external"),
            PROJECTION,
            selection,
            selectionArgs,
            "${MediaStore.MediaColumns.DATE_MODIFIED} DESC"
        )?.use { cursor ->
            val nameColumn = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
            val uriColumn = cursor.getColumnIndex(MediaStore.MediaColumns._ID)
            val mimeColumn = cursor.getColumnIndex(MediaStore.MediaColumns.MIME_TYPE)
            val sizeColumn = cursor.getColumnIndex(MediaStore.MediaColumns.SIZE)
            val dateColumn = cursor.getColumnIndex(MediaStore.MediaColumns.DATE_MODIFIED)

            while (cursor.moveToNext() && results.size < options.maxResults) {
                val name = cursor.getString(nameColumn)
                val uriId = cursor.getLong(uriColumn)
                val uri = Uri.withAppendedPath(MediaStore.Files.getContentUri("external"), uriId.toString())

                val item = FileItem(
                    uri = uri,
                    name = name,
                    path = uri.toString(),
                    mimeType = cursor.getString(mimeColumn),
                    size = cursor.getLong(sizeColumn),
                    lastModified = cursor.getLong(dateColumn) * 1000,
                    isDirectory = false,
                    isReadable = true,
                    isWritable = true
                )

                if (passesFilters(item, options)) {
                    results.add(item)
                }
            }
        }

        results
    }

    private fun passesFilters(item: FileItem, options: SearchOptions): Boolean {
        // Filter by file type
        if (options.fileType != null) {
            if (options.fileType == FileType.DIRECTORY && !item.isDirectory) return false
            if (options.fileType != FileType.DIRECTORY && item.isDirectory) return false
        }

        // Filter by extensions
        if (options.extensions.isNotEmpty()) {
            val ext = PathSanitizer.getExtension(item.name)
            if (ext !in options.extensions.map { it.lowercase() }) {
                return false
            }
        }

        // Filter by minimum size
        if (options.minSize != null && item.size < options.minSize) {
            return false
        }

        // Filter by maximum size
        if (options.maxSize != null && item.size > options.maxSize) {
            return false
        }

        // Filter by date
        if (options.modifiedAfter != null && item.lastModified < options.modifiedAfter) {
            return false
        }

        if (options.modifiedBefore != null && item.lastModified > options.modifiedBefore) {
            return false
        }

        return true
    }

    private fun applyFilters(items: List<FileItem>, options: SearchOptions): List<FileItem> {
        var filtered = items

        // Sort if specified
        if (options.sortBy != null) {
            filtered = filtered.sortedWith { a, b ->
                when (options.sortBy) {
                    SortField.NAME -> a.name.compareTo(b.name, ignoreCase = true)
                    SortField.SIZE -> a.size.compareTo(b.size)
                    SortField.DATE -> a.lastModified.compareTo(b.lastModified)
                }
            }
            if (options.sortDescending) {
                filtered = filtered.reversed()
            }
        }

        return filtered
    }

    /**
     * Cancels the current search operation.
     */
    fun cancelSearch() {
        currentSearchJob?.cancel()
        currentSearchJob = null
    }

    data class SearchOptions(
        val searchSubdirectories: Boolean = true,
        val maxDepth: Int = 10,
        val maxResults: Int = 100,
        val searchByExtension: Boolean = false,
        val extensions: List<String> = emptyList(),
        val fileType: FileType? = null,
        val minSize: Long? = null,
        val maxSize: Long? = null,
        val modifiedAfter: Long? = null,
        val modifiedBefore: Long? = null,
        val sortBy: SortField? = null,
        val sortDescending: Boolean = false
    )

    enum class SortField {
        NAME, SIZE, DATE
    }

    enum class FileType {
        DIRECTORY, FILE, IMAGE, AUDIO, VIDEO, DOCUMENT, ARCHIVE
    }

    companion object {
        private val PROJECTION = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.MIME_TYPE,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.DATE_MODIFIED
        )
    }
}
