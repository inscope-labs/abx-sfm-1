package com.inscopelabs.sfm.storage

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import com.inscopelabs.sfm.core.exception.FileManagerException
import com.inscopelabs.sfm.core.exception.FileNotFoundException
import com.inscopelabs.sfm.core.exception.InvalidFileOperationException
import com.inscopelabs.sfm.core.model.FileItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Helper for scoped storage operations on Android 10+.
 */
class ScopedStorageHelper(private val context: Context) {

    private val contentResolver: ContentResolver = context.contentResolver

    /**
     * Gets app-specific external files directory.
     */
    fun getAppExternalFilesDir(): FileItem? {
        val dir = context.getExternalFilesDir(null)
            ?: return null

        return FileItem(
            uri = Uri.fromFile(dir),
            name = dir.name,
            path = dir.absolutePath,
            mimeType = null,
            size = dir.walkTopDown().filter { it.isFile }.map { it.length() }.sum(),
            lastModified = dir.lastModified(),
            isDirectory = true,
            isReadable = dir.canRead(),
            isWritable = dir.canWrite()
        )
    }

    /**
     * Gets standard external storage directories.
     */
    fun getExternalStorageDirectories(): List<FileItem> {
        val directories = mutableListOf<FileItem>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+ - Use MediaStore queries
            directories.addAll(queryExternalStorageDirectories())
        } else {
            // Legacy storage access
            Environment.getExternalStorageDirectory()?.let { extDir ->
                directories.add(
                    FileItem(
                        uri = Uri.fromFile(extDir),
                        name = "Internal Storage",
                        path = extDir.absolutePath,
                        mimeType = null,
                        size = 0L,
                        lastModified = extDir.lastModified(),
                        isDirectory = true,
                        isReadable = extDir.canRead(),
                        isWritable = extDir.canWrite()
                    )
                )
            }
        }

        return directories
    }

    /**
     * Queries files using MediaStore.
     */
    suspend fun queryMediaStore(
        collection: Uri,
        selection: String? = null,
        selectionArgs: Array<String>? = null,
        sortOrder: String? = null
    ): List<FileItem> = withContext(Dispatchers.IO) {
        val items = mutableListOf<FileItem>()

        contentResolver.query(
            collection,
            PROJECTION,
            selection,
            selectionArgs,
            sortOrder
        )?.use { cursor ->
            val nameColumn = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
            val sizeColumn = cursor.getColumnIndex(MediaStore.MediaColumns.SIZE)
            val dateColumn = cursor.getColumnIndex(MediaStore.MediaColumns.DATE_MODIFIED)
            val mimeColumn = cursor.getColumnIndex(MediaStore.MediaColumns.MIME_TYPE)
            val uriColumn = cursor.getColumnIndex(MediaStore.MediaColumns._ID)

            while (cursor.moveToNext()) {
                val uriId = cursor.getLong(uriColumn)
                val uri = Uri.withAppendedPath(collection, uriId.toString())

                items.add(
                    FileItem(
                        uri = uri,
                        name = cursor.getString(nameColumn) ?: "Unknown",
                        path = uri.toString(),
                        mimeType = cursor.getString(mimeColumn),
                        size = cursor.getLong(sizeColumn),
                        lastModified = cursor.getLong(dateColumn) * 1000,
                        isDirectory = false,
                        isReadable = true,
                        isWritable = true
                    )
                )
            }
        }

        items
    }

    /**
     * Searches files by name using MediaStore.
     */
    suspend fun searchFiles(query: String, limit: Int = 100): List<FileItem> = withContext(Dispatchers.IO) {
        val items = mutableListOf<FileItem>()

        val selection = "${MediaStore.MediaColumns.DISPLAY_NAME} LIKE ?"
        val selectionArgs = arrayOf("%$query%")

        contentResolver.query(
            MediaStore.Files.getContentUri("external"),
            PROJECTION,
            selection,
            selectionArgs,
            "${MediaStore.MediaColumns.DATE_MODIFIED} DESC"
        )?.use { cursor ->
            val nameColumn = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
            val sizeColumn = cursor.getColumnIndex(MediaStore.MediaColumns.SIZE)
            val dateColumn = cursor.getColumnIndex(MediaStore.MediaColumns.DATE_MODIFIED)
            val mimeColumn = cursor.getColumnIndex(MediaStore.MediaColumns.MIME_TYPE)
            val uriColumn = cursor.getColumnIndex(MediaStore.MediaColumns._ID)

            while (cursor.moveToNext() && items.size < limit) {
                val uriId = cursor.getLong(uriColumn)
                val uri = Uri.withAppendedPath(MediaStore.Files.getContentUri("external"), uriId.toString())
                val mimeType = if (mimeColumn >= 0) cursor.getString(mimeColumn) else null
                val isDirectory = mimeType == DocumentsContract.Document.MIME_TYPE_DIR

                items.add(
                    FileItem(
                        uri = uri,
                        name = cursor.getString(nameColumn) ?: "Unknown",
                        path = uri.toString(),
                        mimeType = if (isDirectory) null else mimeType,
                        size = if (isDirectory) 0 else cursor.getLong(sizeColumn),
                        lastModified = cursor.getLong(dateColumn) * 1000,
                        isDirectory = isDirectory,
                        isReadable = true,
                        isWritable = true
                    )
                )
            }
        }

        items
    }

    /**
     * Gets file info from MediaStore.
     */
    suspend fun getMediaStoreFile(uri: Uri): FileItem = withContext(Dispatchers.IO) {
        contentResolver.query(
            uri,
            PROJECTION,
            null,
            null,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameColumn = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
                val sizeColumn = cursor.getColumnIndex(MediaStore.MediaColumns.SIZE)
                val dateColumn = cursor.getColumnIndex(MediaStore.MediaColumns.DATE_MODIFIED)
                val mimeColumn = cursor.getColumnIndex(MediaStore.MediaColumns.MIME_TYPE)

                val mimeType = if (mimeColumn >= 0) cursor.getString(mimeColumn) else null
                val isDirectory = mimeType == DocumentsContract.Document.MIME_TYPE_DIR

                return@withContext FileItem(
                    uri = uri,
                    name = cursor.getString(nameColumn) ?: "Unknown",
                    path = uri.toString(),
                    mimeType = if (isDirectory) null else mimeType,
                    size = if (isDirectory) 0 else cursor.getLong(sizeColumn),
                    lastModified = cursor.getLong(dateColumn) * 1000,
                    isDirectory = isDirectory,
                    isReadable = true,
                    isWritable = true
                )
            }
        }

        throw FileNotFoundException("File not found in MediaStore: $uri")
    }

    /**
     * Deletes a file using MediaStore.
     */
    suspend fun deleteMediaStoreFile(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            DocumentsContract.deleteDocument(contentResolver, uri)
        } catch (e: Exception) {
            contentResolver.delete(uri, null, null) > 0
        }
    }

    private fun queryExternalStorageDirectories(): List<FileItem> {
        val directories = mutableListOf<FileItem>()

        // Query for storage volumes using MediaStore
        val collection = MediaStore.Files.getContentUri("external")

        contentResolver.query(
            collection,
            arrayOf(
                MediaStore.Files.FileColumns.DATA,
                MediaStore.Files.FileColumns.DISPLAY_NAME,
                MediaStore.Files.FileColumns.SIZE,
                MediaStore.Files.FileColumns.DATE_MODIFIED,
                MediaStore.Files.FileColumns.MIME_TYPE
            ),
            "${MediaStore.Files.FileColumns.DATA} LIKE ?",
            arrayOf("%/Android/data/${context.packageName}/%"),
            null
        )?.use { cursor ->
            // Process results for app-specific directories
        }

        return directories
    }

    companion object {
        private val PROJECTION = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.DATE_MODIFIED,
            MediaStore.MediaColumns.MIME_TYPE
        )
    }
}
