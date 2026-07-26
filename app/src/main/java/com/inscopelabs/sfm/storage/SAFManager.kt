package com.inscopelabs.sfm.storage

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.inscopelabs.sfm.core.exception.FileNotFoundException
import com.inscopelabs.sfm.core.exception.FilePermissionException
import com.inscopelabs.sfm.core.exception.InvalidFileOperationException
import com.inscopelabs.sfm.core.model.FileItem
import com.inscopelabs.sfm.core.model.SortOption
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Storage Access Framework Manager.
 * Provides secure abstraction over SAF for file operations.
 */
class SAFManager(private val context: Context) {

    private val authorizedRoots: MutableMap<String, DocumentFile> = mutableMapOf()

    /**
     * Opens a directory picker and returns the selected root URI.
     */
    suspend fun pickDirectory(): Uri? = withContext(Dispatchers.Main) {
        // This would typically be called from an Activity/Fragment
        // Return null if cancelled, or the selected URI
        null
    }

    /**
     * Adds an authorized root directory.
     */
    fun addAuthorizedRoot(rootUri: Uri, persistPermission: Boolean = true) {
        if (persistPermission) {
            val takeFlags = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            try {
                context.contentResolver.takePersistableUriPermission(rootUri, takeFlags)
            } catch (e: SecurityException) {
                throw FilePermissionException(rootUri.toString(), e)
            }
        }

        val documentFile = DocumentFile.fromTreeUri(context, rootUri)
            ?: throw InvalidFileOperationException("Invalid tree URI: $rootUri")

        val rootKey = rootUri.toString()
        authorizedRoots[rootKey] = documentFile
    }

    /**
     * Removes an authorized root directory.
     */
    fun removeAuthorizedRoot(rootUri: Uri) {
        val rootKey = rootUri.toString()
        if (authorizedRoots.remove(rootKey) != null) {
            try {
                val takeFlags = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                context.contentResolver.releasePersistableUriPermission(rootUri, takeFlags)
            } catch (e: SecurityException) {
                // Permission already released or never held
            }
        }
    }

    /**
     * Gets all authorized roots.
     */
    fun getAuthorizedRoots(): List<DocumentFile> = authorizedRoots.values.toList()

    /**
     * Checks if a URI is within an authorized root.
     */
    fun isWithinAuthorizedRoot(uri: Uri): Boolean {
        val uriString = uri.toString()
        return authorizedRoots.keys.any { rootKey ->
            val boundedRoot = if (rootKey.endsWith("/") || rootKey.endsWith("%2F")) rootKey else "$rootKey/"
            uriString == rootKey || uriString.startsWith(boundedRoot)
        }
    }

    /**
     * Gets the root for a given URI.
     */
    fun getRootForUri(uri: Uri): DocumentFile? {
        val uriString = uri.toString()
        for ((rootKey, rootDoc) in authorizedRoots) {
            val boundedRoot = if (rootKey.endsWith("/") || rootKey.endsWith("%2F")) rootKey else "$rootKey/"
            if (uriString == rootKey || uriString.startsWith(boundedRoot)) {
                return rootDoc
            }
        }
        return null
    }

    /**
     * Resolves a URI to a DocumentFile.
     */
    fun resolveUri(uri: Uri): DocumentFile? {
        return try {
            DocumentFile.fromSingleUri(context, uri)
                ?: DocumentFile.fromTreeUri(context, uri)
                ?: getDocumentFileFromPath(uri)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Lists files in a directory with sorting.
     */
    suspend fun listFiles(
        directoryUri: Uri,
        sortOption: SortOption = SortOption.NAME_ASC
    ): List<FileItem> = withContext(Dispatchers.IO) {
        val directory = resolveUri(directoryUri)
            ?: throw FileNotFoundException("Directory not found: $directoryUri")

        if (!directory.isDirectory) {
            throw InvalidFileOperationException("Not a directory: $directoryUri")
        }

        val files = directory.listFiles()
            .mapNotNull { doc -> documentFileToFileItem(doc) }
            .sortedWith { a, b -> sortOption.compare(a, b) }

        files
    }

    /**
     * Gets file metadata.
     */
    suspend fun getFileMetadata(uri: Uri): FileItem = withContext(Dispatchers.IO) {
        val doc = resolveUri(uri)
            ?: throw FileNotFoundException("File not found: $uri")

        documentFileToFileItem(doc)
            ?: throw FileNotFoundException("Cannot read file: $uri")
    }

    /**
     * Checks if a file exists.
     */
    suspend fun exists(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        resolveUri(uri) != null
    }

    /**
     * Creates a file in the given directory.
     */
    suspend fun createFile(
        parentUri: Uri,
        name: String,
        mimeType: String
    ): FileItem = withContext(Dispatchers.IO) {
        val parent = resolveUri(parentUri) as? DocumentFile
            ?: throw FileNotFoundException("Parent directory not found: $parentUri")

        if (!parent.isDirectory) {
            throw InvalidFileOperationException("Not a directory: $parentUri")
        }

        val existing = parent.findFile(name)
        if (existing != null) {
            return@withContext documentFileToFileItem(existing)!!
        }

        val newFile = parent.createFile(mimeType, name)
            ?: throw InvalidFileOperationException("Failed to create file: $name")

        documentFileToFileItem(newFile)
            ?: throw InvalidFileOperationException("Failed to read created file")
    }

    /**
     * Creates a subdirectory.
     */
    suspend fun createDirectory(
        parentUri: Uri,
        name: String
    ): FileItem = withContext(Dispatchers.IO) {
        val parent = resolveUri(parentUri) as? DocumentFile
            ?: throw FileNotFoundException("Parent directory not found: $parentUri")

        if (!parent.isDirectory) {
            throw InvalidFileOperationException("Not a directory: $parentUri")
        }

        var newDir = parent.findFile(name)
        if (newDir == null) {
            newDir = parent.createDirectory(name)
                ?: throw InvalidFileOperationException("Failed to create directory: $name")
        }

        documentFileToFileItem(newDir)
            ?: throw InvalidFileOperationException("Failed to read created directory")
    }

    /**
     * Deletes a file or directory.
     */
    suspend fun delete(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        val doc = resolveUri(uri)
            ?: return@withContext false

        doc.delete()
    }

    /**
     * Renames a file or directory.
     */
    suspend fun rename(uri: Uri, newName: String): FileItem = withContext(Dispatchers.IO) {
        val doc = resolveUri(uri)
            ?: throw FileNotFoundException("File not found: $uri")

        val success = doc.renameTo(newName)
        if (!success) {
            throw InvalidFileOperationException("Failed to rename to: $newName")
        }

        // URI changes after rename
        val newUri = doc.uri
        documentFileToFileItem(doc) ?: throw InvalidFileOperationException("Failed to read renamed file")
    }

    /**
     * Gets the display path relative to the root.
     */
    fun getRelativePath(uri: Uri): String {
        val root = getRootForUri(uri) ?: return uri.path ?: uri.toString()
        val rootUri = root.uri.toString()
        val fileUri = uri.toString()

        if (!fileUri.startsWith(rootUri)) {
            return fileUri
        }

        val relativePath = fileUri.removePrefix(rootUri)
        return relativePath.removePrefix("/")
    }

    private fun documentFileToFileItem(doc: DocumentFile): FileItem? {
        return try {
            FileItem(
                uri = doc.uri,
                name = doc.name ?: return null,
                path = doc.uri.path ?: doc.uri.toString(),
                mimeType = doc.type,
                size = doc.length(),
                lastModified = doc.lastModified(),
                isDirectory = doc.isDirectory,
                isReadable = doc.canRead(),
                isWritable = doc.canWrite()
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun getDocumentFileFromPath(uri: Uri): DocumentFile? {
        return try {
            DocumentFile.fromTreeUri(context, uri)
        } catch (e: Exception) {
            null
        }
    }
}
