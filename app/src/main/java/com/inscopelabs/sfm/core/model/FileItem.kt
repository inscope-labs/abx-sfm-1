package com.inscopelabs.sfm.core.model

import android.net.Uri

/**
 * Immutable representation of a file or directory.
 * All fields are read-only to ensure immutability.
 */
data class FileItem(
    val uri: Uri,
    val name: String,
    val path: String,
    val mimeType: String?,
    val size: Long,
    val lastModified: Long,
    val isDirectory: Boolean,
    val isReadable: Boolean,
    val isWritable: Boolean
) {
    val fileType: FileType
        get() = FileType.fromMimeType(mimeType, isDirectory)

    val extension: String
        get() = name.substringAfterLast('.', "")

    val formattedSize: String
        get() = formatFileSize(size)

    companion object {
        fun formatFileSize(size: Long): String {
            return when {
                size < 1024 -> "$size B"
                size < 1024 * 1024 -> "${size / 1024} KB"
                size < 1024 * 1024 * 1024 -> "${size / (1024 * 1024)} MB"
                else -> String.format("%.2f GB", size / (1024.0 * 1024.0 * 1024.0))
            }
        }
    }
}
