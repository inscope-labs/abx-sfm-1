package com.inscopelabs.sfm.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.inscopelabs.sfm.data.VaultFileEntity
import com.inscopelabs.sfm.data.VaultRepository
import com.inscopelabs.sfm.core.model.FileItem
import com.inscopelabs.sfm.core.model.FileOperationResult
import com.inscopelabs.sfm.core.model.FileType
import com.inscopelabs.sfm.core.model.SortOption
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class VaultViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = VaultRepository(application)

    val isVaultUnlocked = MutableStateFlow(false)
    val enteredPin = MutableStateFlow("")
    val pinErrorMessage = MutableStateFlow<String?>(null)

    val searchQuery = MutableStateFlow("")
    val selectedCategoryFilter = MutableStateFlow("ALL")
    val currentSortOption = MutableStateFlow(SortOption.NAME_ASC)

    private val _rawVaultFiles = repository.allVaultEntities
    val allEntities: StateFlow<List<VaultFileEntity>> = _rawVaultFiles.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val filteredFiles: StateFlow<List<VaultFileEntity>> = combine(
        _rawVaultFiles,
        searchQuery,
        selectedCategoryFilter,
        currentSortOption
    ) { files, query, category, sort ->
        files.filter { entity ->
            val matchesQuery = query.isBlank() || entity.name.contains(query, ignoreCase = true) || (entity.notes?.contains(query, ignoreCase = true) == true)
            val fileItem = entity.toFileItem()
            val matchesCategory = when (category) {
                "ALL" -> true
                "DOCUMENTS" -> fileItem.fileType in listOf(
                    FileType.PDF, FileType.DOC, FileType.DOCX, FileType.XLS,
                    FileType.XLSX, FileType.PPT, FileType.PPTX, FileType.TXT, FileType.RTF
                )
                "IMAGES" -> fileItem.fileType in listOf(
                    FileType.IMAGE, FileType.JPEG, FileType.PNG, FileType.GIF, FileType.WEBP, FileType.SVG
                )
                "AUDIO" -> fileItem.fileType in listOf(
                    FileType.AUDIO, FileType.MP3, FileType.WAV, FileType.FLAC, FileType.OGG
                )
                "VIDEO" -> fileItem.fileType in listOf(
                    FileType.VIDEO, FileType.MP4, FileType.MKV, FileType.AVI, FileType.WEBM
                )
                "ARCHIVES" -> fileItem.fileType in listOf(
                    FileType.ARCHIVE, FileType.ZIP, FileType.TAR, FileType.GZ, FileType.RAR
                )
                "CODE" -> fileItem.fileType in listOf(
                    FileType.CODE, FileType.JSON, FileType.XML, FileType.HTML, FileType.CSS,
                    FileType.JAVASCRIPT, FileType.KOTLIN, FileType.JAVA, FileType.PYTHON
                )
                "EXECUTABLES" -> fileItem.fileType in listOf(
                    FileType.APK, FileType.SHELL
                )
                else -> true
            }
            matchesQuery && matchesCategory
        }.sortedWith { a, b ->
            sort.compare(a.toFileItem(), b.toFileItem())
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // UI Dialog & Modal states
    val selectedFileForDetails = MutableStateFlow<VaultFileEntity?>(null)
    val selectedFileForPreview = MutableStateFlow<VaultFileEntity?>(null)
    val fileToRename = MutableStateFlow<VaultFileEntity?>(null)

    val showCreateFolderDialog = MutableStateFlow(false)
    val showImportNoteDialog = MutableStateFlow(false)
    val showChangePinDialog = MutableStateFlow(false)

    val userMessage = MutableStateFlow<String?>(null)

    init {
        viewModelScope.launch {
            repository.initializeDefaultVaultFilesIfEmpty()
        }
    }

    // Security & PIN methods
    fun appendPinDigit(digit: String) {
        if (enteredPin.value.length < 4) {
            enteredPin.value += digit
            pinErrorMessage.value = null
            if (enteredPin.value.length == 4) {
                verifyEnteredPin()
            }
        }
    }

    fun removeLastPinDigit() {
        if (enteredPin.value.isNotEmpty()) {
            enteredPin.value = enteredPin.value.dropLast(1)
            pinErrorMessage.value = null
        }
    }

    fun clearPin() {
        enteredPin.value = ""
        pinErrorMessage.value = null
    }

    fun verifyEnteredPin() {
        if (repository.verifyPin(enteredPin.value)) {
            isVaultUnlocked.value = true
            enteredPin.value = ""
            pinErrorMessage.value = null
            repository.recordUnlockSuccess()
            userMessage.value = "Vault Unlocked"
        } else {
            val attempts = repository.incrementFailedAttempts()
            enteredPin.value = ""
            pinErrorMessage.value = "Incorrect PIN. (Attempt $attempts) Default PIN is 1234"
        }
    }

    fun quickBiometricUnlock() {
        isVaultUnlocked.value = true
        enteredPin.value = ""
        pinErrorMessage.value = null
        repository.recordUnlockSuccess()
        userMessage.value = "Biometric Authentication Confirmed"
    }

    fun lockVault() {
        isVaultUnlocked.value = false
        enteredPin.value = ""
        userMessage.value = "Vault Locked"
    }

    fun updatePin(oldPin: String, newPin: String): Boolean {
        if (!repository.verifyPin(oldPin)) {
            userMessage.value = "Old PIN is incorrect"
            return false
        }
        if (newPin.length != 4 || !newPin.all { it.isDigit() }) {
            userMessage.value = "New PIN must be 4 digits"
            return false
        }
        repository.setVaultPin(newPin)
        userMessage.value = "Vault PIN updated successfully"
        return true
    }

    // Vault File Operations
    fun importFileFromUri(name: String, uri: Uri, mimeType: String?, size: Long) {
        viewModelScope.launch {
            val result = repository.addVaultFile(
                name = name,
                uri = uri,
                mimeType = mimeType,
                size = size
            )
            result.onSuccess { item ->
                userMessage.value = "Imported '${item.name}' (${item.formattedSize})"
            }.onFailure { error, message ->
                userMessage.value = "Error: $message (${error.toUserMessage()})"
            }
        }
    }

    fun createSecureNote(title: String, content: String) {
        viewModelScope.launch {
            val fileName = if (title.endsWith(".txt")) title else "$title.txt"
            val result = repository.addVaultFile(
                name = fileName,
                uri = Uri.parse("content://com.inscopelabs.sfm.securefiles/notes/$fileName"),
                mimeType = "text/plain",
                size = content.toByteArray().size.toLong(),
                notes = "Encrypted Note created in Vault",
                textContent = content
            )
            result.onSuccess { item ->
                userMessage.value = "Note '${item.name}' created"
            }.onFailure { error, msg ->
                userMessage.value = "Failed: $msg"
            }
        }
    }

    fun createFolder(folderName: String) {
        viewModelScope.launch {
            val result = repository.createFolder(folderName)
            result.onSuccess { item ->
                userMessage.value = "Folder '${item.name}' created"
            }.onFailure { _, msg ->
                userMessage.value = "Error: $msg"
            }
        }
    }

    fun renameFile(id: String, newName: String) {
        viewModelScope.launch {
            val result = repository.renameVaultFile(id, newName)
            result.onSuccess { item ->
                userMessage.value = "Renamed to '${item.name}'"
                fileToRename.value = null
            }.onFailure { _, msg ->
                userMessage.value = "Error renaming: $msg"
            }
        }
    }

    fun toggleFavorite(id: String) {
        viewModelScope.launch {
            val result = repository.toggleFavorite(id)
            result.onSuccess { isFav ->
                userMessage.value = if (isFav) "Added to Favorites" else "Removed from Favorites"
            }
        }
    }

    fun deleteFile(id: String) {
        viewModelScope.launch {
            val result = repository.deleteVaultFile(id)
            result.onSuccess {
                userMessage.value = "File permanently removed from vault"
                if (selectedFileForDetails.value?.id == id) selectedFileForDetails.value = null
                if (selectedFileForPreview.value?.id == id) selectedFileForPreview.value = null
            }.onFailure { error, msg ->
                userMessage.value = "Could not delete: $msg (${error.toUserMessage()})"
            }
        }
    }

    fun clearUserMessage() {
        userMessage.value = null
    }

    fun getFormattedLastUnlockTime(): String {
        val time = repository.getLastUnlockTime()
        val diffMinutes = (System.currentTimeMillis() - time) / (1000 * 60)
        return if (diffMinutes < 1) "Just now" else "$diffMinutes minutes ago"
    }

    fun getFailedAttempts(): Int {
        return repository.getFailedAttempts()
    }
}
