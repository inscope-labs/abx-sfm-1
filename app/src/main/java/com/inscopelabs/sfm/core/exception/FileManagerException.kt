package com.inscopelabs.sfm.core.exception

/**
 * Base exception for all file manager operations.
 */
open class FileManagerException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause) {
    open val errorCode: String = "FM_UNKNOWN"

    override fun toString(): String = "[$errorCode] $message"
}

/**
 * Exception for file not found operations.
 */
class FileNotFoundException(
    path: String,
    cause: Throwable? = null
) : FileManagerException("File not found: $path", cause) {
    override val errorCode: String = "FM_NOT_FOUND"
}

/**
 * Exception for permission denied operations.
 */
class FilePermissionException(
    path: String,
    cause: Throwable? = null
) : FileManagerException("Permission denied: $path", cause) {
    override val errorCode: String = "FM_PERMISSION_DENIED"
}

/**
 * Exception for invalid file operations.
 */
class InvalidFileOperationException(
    message: String,
    cause: Throwable? = null
) : FileManagerException(message, cause) {
    override val errorCode: String = "FM_INVALID_OPERATION"
}

/**
 * Exception for storage full conditions.
 */
class StorageFullException(
    required: Long,
    available: Long,
    cause: Throwable? = null
) : FileManagerException(
    "Not enough storage: required $required bytes, available $available bytes",
    cause
) {
    override val errorCode: String = "FM_STORAGE_FULL"
}

/**
 * Exception for file already exists conditions.
 */
class FileAlreadyExistsException(
    path: String,
    cause: Throwable? = null
) : FileManagerException("File already exists: $path", cause) {
    override val errorCode: String = "FM_FILE_EXISTS"
}

/**
 * Exception for invalid file names.
 */
class InvalidFileNameException(
    name: String,
    cause: Throwable? = null
) : FileManagerException("Invalid file name: $name", cause) {
    override val errorCode: String = "FM_INVALID_NAME"
}

/**
 * Exception for operation cancellation.
 */
class OperationCancelledException(
    cause: Throwable? = null
) : FileManagerException("Operation was cancelled", cause) {
    override val errorCode: String = "FM_CANCELLED"
}

/**
 * Exception for archive-related errors.
 */
class ArchiveException(
    message: String,
    cause: Throwable? = null
) : FileManagerException(message, cause) {
    override val errorCode: String = "FM_ARCHIVE_ERROR"
}

/**
 * Exception for zip bomb detection.
 */
class ZipBombException(
    details: String,
    cause: Throwable? = null
) : FileManagerException("Potential zip bomb detected: $details", cause) {
    override val errorCode: String = "FM_ZIP_BOMB"
}
