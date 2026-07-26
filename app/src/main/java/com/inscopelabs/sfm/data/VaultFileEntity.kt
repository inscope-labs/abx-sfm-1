package com.inscopelabs.sfm.data

import android.net.Uri
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.inscopelabs.sfm.core.model.FileItem

@Entity(tableName = "vault_files")
data class VaultFileEntity(
    @PrimaryKey
    val id: String,
    val uriString: String,
    val name: String,
    val path: String,
    val mimeType: String?,
    val size: Long,
    val lastModified: Long,
    val isDirectory: Boolean,
    val isReadable: Boolean,
    val isWritable: Boolean,
    val isFavorite: Boolean = false,
    val isEncrypted: Boolean = true,
    val notes: String? = null,
    val textContent: String? = null
) {
    fun toFileItem(): FileItem {
        return FileItem(
            uri = Uri.parse(uriString),
            name = name,
            path = path,
            mimeType = mimeType,
            size = size,
            lastModified = lastModified,
            isDirectory = isDirectory,
            isReadable = isReadable,
            isWritable = isWritable
        )
    }
}
