package com.inscopelabs.sfm.data

import android.content.Context
import android.net.Uri
import com.inscopelabs.sfm.core.model.FileItem
import com.inscopelabs.sfm.core.exception.FileManagerException
import com.inscopelabs.sfm.core.model.FileOperationError
import com.inscopelabs.sfm.core.model.FileOperationResult
import com.inscopelabs.sfm.core.model.FileType
import com.inscopelabs.sfm.core.model.SortOption
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.UUID

class VaultRepository(private val context: Context) {

    private val db = AppDatabase.getDatabase(context)
    private val dao = db.vaultDao()

    // Preferences / Vault Lock settings stored in SharedPreferences
    private val prefs = context.getSharedPreferences("secure_vault_prefs", Context.MODE_PRIVATE)

    fun getVaultPin(): String {
        return prefs.getString("vault_pin", "1234") ?: "1234"
    }

    fun setVaultPin(newPin: String) {
        prefs.edit().putString("vault_pin", newPin).apply()
    }

    fun verifyPin(enteredPin: String): Boolean {
        return enteredPin == getVaultPin()
    }

    fun getLastUnlockTime(): Long {
        return prefs.getLong("last_unlock_time", System.currentTimeMillis())
    }

    fun recordUnlockSuccess() {
        prefs.edit().putLong("last_unlock_time", System.currentTimeMillis()).apply()
        resetFailedAttempts()
    }

    fun getFailedAttempts(): Int {
        return prefs.getInt("failed_attempts", 0)
    }

    fun incrementFailedAttempts(): Int {
        val attempts = getFailedAttempts() + 1
        prefs.edit().putInt("failed_attempts", attempts).apply()
        return attempts
    }

    fun resetFailedAttempts() {
        prefs.edit().putInt("failed_attempts", 0).apply()
    }

    val allVaultEntities: Flow<List<VaultFileEntity>> = dao.getAllFiles()

    suspend fun initializeDefaultVaultFilesIfEmpty() = withContext(Dispatchers.IO) {
        if (dao.getFileCount() == 0) {
            val initialFiles = listOf(
                VaultFileEntity(
                    id = UUID.randomUUID().toString(),
                    uriString = "content://com.inscopelabs.sfm.securefiles/vault/financial_report.pdf",
                    name = "Confidential_Q3_Financials.pdf",
                    path = "/SecureVault/Documents/Confidential_Q3_Financials.pdf",
                    mimeType = "application/pdf",
                    size = 2_450_000L,
                    lastModified = System.currentTimeMillis() - 86400000 * 2,
                    isDirectory = false,
                    isReadable = true,
                    isWritable = true,
                    isFavorite = true,
                    isEncrypted = true,
                    notes = "Encrypted Q3 audit & investor breakdown",
                    textContent = "CONFIDENTIAL AUDIT REPORT - Q3 2026\n\n1. Revenue: $14.2M\n2. Operating Expenses: $6.1M\n3. Net Margin: 57%\n\nAll data is AES-256 encrypted."
                ),
                VaultFileEntity(
                    id = UUID.randomUUID().toString(),
                    uriString = "content://com.inscopelabs.sfm.securefiles/vault/passport_scan.png",
                    name = "Passport_Scan_Biometric.png",
                    path = "/SecureVault/Identity/Passport_Scan_Biometric.png",
                    mimeType = "image/png",
                    size = 4_120_000L,
                    lastModified = System.currentTimeMillis() - 86400000 * 5,
                    isDirectory = false,
                    isReadable = true,
                    isWritable = true,
                    isFavorite = true,
                    isEncrypted = true,
                    notes = "High-res biometric passport copy"
                ),
                VaultFileEntity(
                    id = UUID.randomUUID().toString(),
                    uriString = "content://com.inscopelabs.sfm.securefiles/vault/crypto_backup.zip",
                    name = "HardwareWallet_Backup_Seed.zip",
                    path = "/SecureVault/Archives/HardwareWallet_Backup_Seed.zip",
                    mimeType = "application/zip",
                    size = 850_000L,
                    lastModified = System.currentTimeMillis() - 86400000 * 1,
                    isDirectory = false,
                    isReadable = true,
                    isWritable = true,
                    isFavorite = false,
                    isEncrypted = true,
                    notes = "Encrypted hardware wallet recovery phrase archive"
                ),
                VaultFileEntity(
                    id = UUID.randomUUID().toString(),
                    uriString = "content://com.inscopelabs.sfm.securefiles/vault/server_keys.json",
                    name = "Production_API_Keys.json",
                    path = "/SecureVault/Code/Production_API_Keys.json",
                    mimeType = "application/json",
                    size = 14_200L,
                    lastModified = System.currentTimeMillis() - 3600000 * 4,
                    isDirectory = false,
                    isReadable = true,
                    isWritable = true,
                    isFavorite = true,
                    isEncrypted = true,
                    notes = "Production cloud API & database credentials",
                    textContent = "{\n  \"environment\": \"production\",\n  \"database_cluster\": \"us-west1-spanner\",\n  \"api_endpoint\": \"https://api.vault.internal/v2\",\n  \"status\": \"ENCRYPTED_AES_GCM\"\n}"
                ),
                VaultFileEntity(
                    id = UUID.randomUUID().toString(),
                    uriString = "content://com.inscopelabs.sfm.securefiles/vault/secure_deploy.sh",
                    name = "deploy_security_cluster.sh",
                    path = "/SecureVault/Scripts/deploy_security_cluster.sh",
                    mimeType = "application/x-sh",
                    size = 5_800L,
                    lastModified = System.currentTimeMillis() - 86400000 * 3,
                    isDirectory = false,
                    isReadable = true,
                    isWritable = true,
                    isFavorite = false,
                    isEncrypted = true,
                    notes = "Zero-trust deployment shell script",
                    textContent = "#!/bin/bash\n# Zero-trust Secure Deployment Pipeline\n echo '[VAULT] Verifying RSA 4096 Signatures...'\n echo '[VAULT] Decrypting Key Store...'\n echo '[VAULT] Cluster Deployment Successful!'"
                ),
                VaultFileEntity(
                    id = UUID.randomUUID().toString(),
                    uriString = "content://com.inscopelabs.sfm.securefiles/vault/family_photo.jpg",
                    name = "Family_Vacation_2026.jpg",
                    path = "/SecureVault/Photos/Family_Vacation_2026.jpg",
                    mimeType = "image/jpeg",
                    size = 3_800_000L,
                    lastModified = System.currentTimeMillis() - 86400000 * 10,
                    isDirectory = false,
                    isReadable = true,
                    isWritable = true,
                    isFavorite = false,
                    isEncrypted = true,
                    notes = "Private family photo album"
                ),
                VaultFileEntity(
                    id = UUID.randomUUID().toString(),
                    uriString = "content://com.inscopelabs.sfm.securefiles/vault/voice_memo.mp3",
                    name = "VoiceMemo_Meeting_Notes.mp3",
                    path = "/SecureVault/Audio/VoiceMemo_Meeting_Notes.mp3",
                    mimeType = "audio/mpeg",
                    size = 12_400_000L,
                    lastModified = System.currentTimeMillis() - 86400000 * 7,
                    isDirectory = false,
                    isReadable = true,
                    isWritable = true,
                    isFavorite = false,
                    isEncrypted = true,
                    notes = "Encrypted audio log"
                ),
                VaultFileEntity(
                    id = UUID.randomUUID().toString(),
                    uriString = "content://com.inscopelabs.sfm.securefiles/vault/vault_notes.txt",
                    name = "Personal_Vault_Master_Notes.txt",
                    path = "/SecureVault/Notes/Personal_Vault_Master_Notes.txt",
                    mimeType = "text/plain",
                    size = 1_200L,
                    lastModified = System.currentTimeMillis() - 1800000,
                    isDirectory = false,
                    isReadable = true,
                    isWritable = true,
                    isFavorite = true,
                    isEncrypted = true,
                    notes = "Quick secure notes",
                    textContent = "1. Security Vault PIN set to default '1234'.\n2. Backup seed saved in HardwareWallet_Backup_Seed.zip.\n3. All files stored in isolated internal sandbox with AES encryption."
                )
            )
            dao.insertFiles(initialFiles)
        }
    }

    suspend fun addVaultFile(
        name: String,
        uri: Uri,
        mimeType: String?,
        size: Long,
        notes: String? = null,
        textContent: String? = null
    ): FileOperationResult<FileItem> = withContext(Dispatchers.IO) {
        if (name.isBlank()) {
            return@withContext FileOperationResult.Failure(
                error = FileOperationError.INVALID_NAME,
                message = "File name cannot be empty"
            )
        }

        try {
            val id = UUID.randomUUID().toString()
            val entity = VaultFileEntity(
                id = id,
                uriString = uri.toString(),
                name = name,
                path = "/SecureVault/$name",
                mimeType = mimeType ?: "application/octet-stream",
                size = if (size <= 0) 1024L else size,
                lastModified = System.currentTimeMillis(),
                isDirectory = false,
                isReadable = true,
                isWritable = true,
                isFavorite = false,
                isEncrypted = true,
                notes = notes,
                textContent = textContent
            )
            dao.insertFile(entity)
            FileOperationResult.Success(entity.toFileItem())
        } catch (e: Exception) {
            FileOperationResult.Failure(
                error = FileOperationError.ENCRYPTION_FAILED,
                message = "Failed to import file into secure vault",
                cause = e
            )
        }
    }

    suspend fun createFolder(folderName: String): FileOperationResult<FileItem> = withContext(Dispatchers.IO) {
        if (folderName.isBlank()) {
            return@withContext FileOperationResult.Failure(
                error = FileOperationError.INVALID_NAME,
                message = "Folder name cannot be empty"
            )
        }

        try {
            val id = UUID.randomUUID().toString()
            val entity = VaultFileEntity(
                id = id,
                uriString = "content://com.inscopelabs.sfm.securefiles/vault/folder_$folderName",
                name = folderName,
                path = "/SecureVault/$folderName",
                mimeType = null,
                size = 0L,
                lastModified = System.currentTimeMillis(),
                isDirectory = true,
                isReadable = true,
                isWritable = true,
                isFavorite = false,
                isEncrypted = true
            )
            dao.insertFile(entity)
            FileOperationResult.Success(entity.toFileItem())
        } catch (e: Exception) {
            FileOperationResult.Failure(
                error = FileOperationError.UNKNOWN_ERROR,
                message = "Failed to create folder",
                cause = e
            )
        }
    }

    suspend fun renameVaultFile(id: String, newName: String): FileOperationResult<FileItem> = withContext(Dispatchers.IO) {
        if (newName.isBlank()) {
            return@withContext FileOperationResult.Failure(
                error = FileOperationError.INVALID_NAME,
                message = "New file name cannot be blank"
            )
        }

        try {
            val existing = dao.getFileById(id) ?: return@withContext FileOperationResult.Failure(
                error = FileOperationError.NOT_FOUND,
                message = "File not found in vault"
            )

            val updated = existing.copy(
                name = newName,
                path = existing.path.substringBeforeLast('/') + "/$newName",
                lastModified = System.currentTimeMillis()
            )
            dao.updateFile(updated)
            FileOperationResult.Success(updated.toFileItem())
        } catch (e: Exception) {
            FileOperationResult.Failure(
                error = FileOperationError.UNKNOWN_ERROR,
                message = "Failed to rename file",
                cause = e
            )
        }
    }

    suspend fun toggleFavorite(id: String): FileOperationResult<Boolean> = withContext(Dispatchers.IO) {
        try {
            val existing = dao.getFileById(id) ?: return@withContext FileOperationResult.Failure(
                error = FileOperationError.NOT_FOUND,
                message = "File not found"
            )
            val newStatus = !existing.isFavorite
            dao.updateFavorite(id, newStatus)
            FileOperationResult.Success(newStatus)
        } catch (e: Exception) {
            FileOperationResult.Failure(
                error = FileOperationError.UNKNOWN_ERROR,
                message = "Failed to update favorite status",
                cause = e
            )
        }
    }

    suspend fun deleteVaultFile(id: String): FileOperationResult<Unit> = withContext(Dispatchers.IO) {
        try {
            val existing = dao.getFileById(id) ?: return@withContext FileOperationResult.Failure(
                error = FileOperationError.NOT_FOUND,
                message = "File not found"
            )
            dao.deleteFileById(id)
            FileOperationResult.Success(Unit)
        } catch (e: Exception) {
            FileOperationResult.Failure(
                error = FileOperationError.PERMISSION_DENIED,
                message = "Could not delete vault file",
                cause = e
            )
        }
    }
}
