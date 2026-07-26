package com.inscopelabs.sfm.security.permissions

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

/**
 * Manages Android runtime permissions and SAF permissions.
 */
class PermissionManager(private val context: Context) {

    /**
     * Checks if storage permissions are granted.
     */
    fun hasStoragePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+ uses scoped storage and SAF
            true // SAF permissions are checked per-operation
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10 uses scoped storage
            true
        } else {
            // Legacy storage permission
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Checks if a specific Android permission is granted.
     */
    fun hasPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Checks if all specified permissions are granted.
     */
    fun hasAllPermissions(permissions: List<String>): Boolean {
        return permissions.all { hasPermission(it) }
    }

    /**
     * Checks if any of the specified permissions is granted.
     */
    fun hasAnyPermission(permissions: List<String>): Boolean {
        return permissions.any { hasPermission(it) }
    }

    /**
     * Gets list of permissions that need to be requested.
     */
    fun getMissingPermissions(permissions: List<String>): List<String> {
        return permissions.filter { !hasPermission(it) }
    }

    /**
     * Checks if the app should show permission rationale.
     */
    fun shouldShowRationale(permission: String): Boolean {
        return context.packageName.let { packageName ->
            context.packageManager.checkPermission(
                permission,
                packageName
            ) == PackageManager.PERMISSION_DENIED
        }
    }

    /**
     * Validates that required filesystem capabilities are available.
     */
    fun validateFilesystemAccess(
        uri: android.net.Uri,
        requireWrite: Boolean = false
    ): AccessValidation {
        return try {
            val targetStr = uri.toString()
            val permissions = context.contentResolver.persistedUriPermissions
                .filter {
                    val persistedStr = it.uri.toString()
                    val boundedPersisted = if (persistedStr.endsWith("/") || persistedStr.endsWith("%2F")) persistedStr else "$persistedStr/"
                    it.uri.authority == uri.authority && (targetStr == persistedStr || targetStr.startsWith(boundedPersisted))
                }

            val hasRead = permissions.any { it.isReadPermission }
            val hasWrite = permissions.any { it.isWritePermission }

            when {
                !hasRead && !hasWrite -> AccessValidation.NO_ACCESS
                requireWrite && !hasWrite -> AccessValidation.READ_ONLY
                else -> AccessValidation.FULL_ACCESS
            }
        } catch (e: Exception) {
            AccessValidation.ERROR
        }
    }

    /**
     * Checks if app has MANAGE_EXTERNAL_STORAGE permission (Android 11+).
     */
    fun hasManageStoragePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            android.os.Environment.isExternalStorageManager()
        } else {
            false
        }
    }

    /**
     * Checks if notification permission is granted (Android 13+).
     */
    fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    enum class AccessValidation {
        FULL_ACCESS,
        READ_ONLY,
        NO_ACCESS,
        ERROR
    }
}
