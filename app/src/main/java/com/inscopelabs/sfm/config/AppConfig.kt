package com.inscopelabs.sfm.config

/**
 * Application configuration.
 */
object AppConfig {
    const val APP_NAME = "Secure Files"
    const val VERSION = "0.0.1"
    const val DATABASE_NAME = "filemanager.db"
    const val PREFERENCES_NAME = "sfm_prefs"
}

/**
 * Security configuration.
 */
object SecurityConfig {
    const val ENABLE_AUDIT_LOGGING = true
    const val ENABLE_CERTIFICATE_PINNING = true
    const val REQUIRE_USER_APPROVAL_FOR_DANGEROUS = true
    const val SESSION_TIMEOUT_MINUTES = 60L
    const val MAX_LOGIN_ATTEMPTS = 5
    const val LOCKOUT_DURATION_MINUTES = 15L
    const val ENABLE_ROOT_DETECTION = true
    const val ALLOW_ROOTED_DEVICES = false // Set to false for stricter security
}

/**
 * Plugin configuration.
 */
object PluginConfig {
    const val ENABLE_PLUGINS = true
    const val ALLOW_PLUGIN_INSTALL = true
    const val REQUIRE_SIGNATURE_VERIFICATION = true
    const val MAX_PLUGIN_SIZE_KB = 1024
    const val PLUGIN_TIMEOUT_SECONDS = 30
    const val MAX_CONCURRENT_PLUGINS = 5
}

/**
 * Storage configuration.
 */
object StorageConfig {
    const val MAX_FILE_SIZE_MB = 100L
    const val MAX_ARCHIVE_SIZE_MB = 500L
    const val ENABLE_SCOPED_STORAGE = true
    const val ALLOW_EXTERNAL_STORAGE_ACCESS = false
    const val COMPRESSION_ENABLED = true
}

/**
 * MCP server configuration defaults.
 */
object MCPServerConfig {
    const val DEFAULT_PORT = 8080
    const val DEFAULT_TIMEOUT_MS = 30000L
    const val MAX_REQUEST_SIZE = 10 * 1024 * 1024 // 10MB
    const val RATE_LIMIT_PER_MINUTE = 100
    const val ENABLE_TLS = true
    const val ENABLE_MUTUAL_TLS = false
}
