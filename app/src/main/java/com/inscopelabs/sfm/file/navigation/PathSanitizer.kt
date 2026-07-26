package com.inscopelabs.sfm.file.navigation

import android.net.Uri
import com.inscopelabs.sfm.core.exception.PathTraversalException
import java.net.URLDecoder
import java.net.URLEncoder

/**
 * Sanitizes and validates file paths to prevent path traversal attacks.
 */
object PathSanitizer {

    /**
     * Sanitizes a file path.
     * Returns the sanitized path or throws an exception if dangerous.
     */
    fun sanitizePath(path: String): String {
        if (path.isBlank()) {
            throw PathTraversalException("Empty path")
        }

        // Normalize separators
        var normalized = path.replace("\\", "/")

        // Check for null bytes
        if (normalized.contains("\u0000")) {
            throw PathTraversalException("Path contains null bytes")
        }

        // Check for path traversal attempts
        if (normalized.contains("..")) {
            throw PathTraversalException("Path traversal attempt detected: $path")
        }

        // Remove leading/trailing slashes
        normalized = normalized.trim('/')

        // Check for absolute paths that might escape sandbox
        if (normalized.startsWith("/")) {
            throw PathTraversalException("Absolute paths not allowed: $path")
        }

        // Check for Windows drive letters
        if (normalized.matches(Regex("^[a-zA-Z]:"))) {
            throw PathTraversalException("Drive letters not allowed: $path")
        }

        return normalized
    }

    /**
     * Sanitizes a file name.
     */
    fun sanitizeFileName(name: String): String {
        if (name.isBlank()) {
            throw PathTraversalException("Empty file name")
        }

        // Remove null bytes
        var sanitized = name.replace("\u0000", "")

        // Remove path separators
        sanitized = sanitized.replace("/", "")
        sanitized = sanitized.replace("\\", "")

        // Remove path traversal patterns
        sanitized = sanitized.replace("..", "")

        // Trim whitespace
        sanitized = sanitized.trim()

        if (sanitized.isBlank()) {
            throw PathTraversalException("File name contains only invalid characters")
        }

        // Limit length
        if (sanitized.length > 255) {
            val ext = sanitized.substringAfterLast('.', "")
            val nameWithoutExt = sanitized.substringBeforeLast('.')
            sanitized = nameWithoutExt.take(255 - ext.length - 1) + "." + ext
        }

        return sanitized
    }

    /**
     * Validates that a path is within allowed boundaries.
     */
    fun isPathSafe(path: String, allowedRoots: List<String>): Boolean {
        val sanitized = try {
            sanitizePath(path)
        } catch (e: PathTraversalException) {
            return false
        }

        return allowedRoots.any { root ->
            val sanitizedRoot = try { sanitizePath(root) } catch (e: Exception) { root.trim('/') }
            sanitized == sanitizedRoot || sanitized.startsWith("$sanitizedRoot/")
        }
    }

    /**
     * URL-encodes a path component.
     */
    fun encodePathComponent(component: String): String {
        return URLEncoder.encode(component, "UTF-8")
            .replace("+", "%20")
    }

    /**
     * URL-decodes a path component.
     */
    fun decodePathComponent(component: String): String {
        return URLDecoder.decode(component, "UTF-8")
    }

    /**
     * Validates a search query to prevent injection.
     */
    fun sanitizeSearchQuery(query: String): String {
        if (query.isBlank()) return ""

        // Remove special regex characters
        var sanitized = query
            .replace("\\", "")
            .replace("[", "")
            .replace("]", "")
            .replace("{", "")
            .replace("}", "")
            .replace("(", "")
            .replace(")", "")
            .replace("|", "")
            .replace("^", "")
            .replace("$", "")
            .replace("*", "")
            .replace("+", "")
            .replace("?", "")
            .replace("\u0000", "")

        // Trim and limit length
        sanitized = sanitized.trim()
        if (sanitized.length > MAX_QUERY_LENGTH) {
            sanitized = sanitized.take(MAX_QUERY_LENGTH)
        }

        return sanitized
    }

    /**
     * Validates that a URI is within an authorized scope.
     */
    fun isUriInScope(uri: Uri, authorizedRoots: List<Uri>): Boolean {
        val uriString = uri.toString()
        return authorizedRoots.any { root ->
            uriString.startsWith(root.toString())
        }
    }

    /**
     * Builds a safe path from components.
     */
    fun buildPath(vararg components: String): String {
        return components
            .filter { it.isNotBlank() }
            .map { sanitizeFileName(it) }
            .joinToString("/")
    }

    /**
     * Validates a file extension.
     */
    fun isExtensionAllowed(extension: String, allowedExtensions: Set<String>): Boolean {
        val ext = extension.lowercase().removePrefix(".")
        return allowedExtensions.isEmpty() || allowedExtensions.contains(ext)
    }

    /**
     * Gets file extension from name.
     */
    fun getExtension(fileName: String): String {
        val lastDot = fileName.lastIndexOf('.')
        return if (lastDot > 0) {
            fileName.substring(lastDot + 1).lowercase()
        } else {
            ""
        }
    }

    /**
     * Removes extension from file name.
     */
    fun removeExtension(fileName: String): String {
        val lastDot = fileName.lastIndexOf('.')
        return if (lastDot > 0) {
            fileName.substring(0, lastDot)
        } else {
            fileName
        }
    }

    private const val MAX_QUERY_LENGTH = 100
}
