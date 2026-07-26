package com.inscopelabs.sfm.core.model

import com.inscopelabs.sfm.core.exception.FileManagerException

/**
 * Result wrapper for file operations.
 * Provides detailed status and error information.
 */
sealed class FileOperationResult<out T> {
    data class Success<T>(val data: T) : FileOperationResult<T>()

    data class Failure(
        val error: FileOperationError,
        val message: String,
        val cause: Throwable? = null
    ) : FileOperationResult<Nothing>()

    val isSuccess: Boolean get() = this is Success
    val isFailure: Boolean get() = this is Failure

    fun getOrNull(): T? = (this as? Success)?.data

    fun getOrThrow(): T = when (this) {
        is Success -> data
        is Failure -> throw FileManagerException("$message: ${cause?.message}")
    }

    inline fun <R> map(transform: (T) -> R): FileOperationResult<R> = when (this) {
        is Success -> Success(transform(data))
        is Failure -> this
    }

    inline fun onSuccess(action: (T) -> Unit): FileOperationResult<T> {
        if (this is Success) action(data)
        return this
    }

    inline fun onFailure(action: (FileOperationError, String) -> Unit): FileOperationResult<T> {
        if (this is Failure) action(error, message)
        return this
    }
}

enum class FileOperationError {
    // General errors
    UNKNOWN_ERROR,
    NOT_FOUND,
    PERMISSION_DENIED,
    FILE_ALREADY_EXISTS,
    FILE_IS_READ_ONLY,
    DIRECTORY_NOT_EMPTY,
    INVALID_NAME,
    INVALID_PATH,
    PATH_TOO_LONG,
    STORAGE_FULL,
    OPERATION_CANCELLED,

    // Security errors
    SECURITY_VIOLATION,
    POLICY_VIOLATION,
    UNAUTHORIZED_ACCESS,
    SESSION_EXPIRED,
    CAPABILITY_NOT_GRANTED,

    // Path traversal
    PATH_TRAVERSAL_DETECTED,

    // Archive errors
    INVALID_ARCHIVE,
    ZIP_BOMB_DETECTED,
    CORRUPT_ARCHIVE,

    // Network errors
    NETWORK_ERROR,
    RELAY_DISCONNECTED,
    ENCRYPTION_FAILED,
    DECRYPTION_FAILED;

    fun toUserMessage(): String = when (this) {
        UNKNOWN_ERROR -> "An unknown error occurred"
        NOT_FOUND -> "File or directory not found"
        PERMISSION_DENIED -> "Permission denied"
        FILE_ALREADY_EXISTS -> "A file with this name already exists"
        FILE_IS_READ_ONLY -> "File is read-only"
        DIRECTORY_NOT_EMPTY -> "Directory is not empty"
        INVALID_NAME -> "Invalid file or folder name"
        INVALID_PATH -> "Invalid file path"
        PATH_TOO_LONG -> "File path is too long"
        STORAGE_FULL -> "Storage is full"
        OPERATION_CANCELLED -> "Operation was cancelled"
        SECURITY_VIOLATION -> "Security violation detected"
        POLICY_VIOLATION -> "Operation violates security policy"
        UNAUTHORIZED_ACCESS -> "Unauthorized access attempt"
        SESSION_EXPIRED -> "Session has expired"
        CAPABILITY_NOT_GRANTED -> "Required capability not granted"
        PATH_TRAVERSAL_DETECTED -> "Path traversal attempt detected"
        INVALID_ARCHIVE -> "Invalid or corrupt archive"
        ZIP_BOMB_DETECTED -> "Potential zip bomb detected"
        CORRUPT_ARCHIVE -> "Archive is corrupt"
        NETWORK_ERROR -> "Network error occurred"
        RELAY_DISCONNECTED -> "Relay connection lost"
        ENCRYPTION_FAILED -> "Encryption failed"
        DECRYPTION_FAILED -> "Decryption failed"
    }
}
