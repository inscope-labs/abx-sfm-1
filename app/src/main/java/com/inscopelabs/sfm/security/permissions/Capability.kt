package com.inscopelabs.sfm.security.permissions

/**
 * Fine-grained capability permissions for filesystem access.
 * Each capability is independently grantable.
 */
enum class Capability(
    val displayName: String,
    val description: String,
    val isDangerous: Boolean = false
) {
    // Filesystem read capabilities
    FILESYSTEM_READ(
        displayName = "Read Files",
        description = "Read files and their contents",
        isDangerous = false
    ),

    FILESYSTEM_WRITE(
        displayName = "Write Files",
        description = "Create and modify files",
        isDangerous = true
    ),

    FILESYSTEM_DELETE(
        displayName = "Delete Files",
        description = "Delete files and directories",
        isDangerous = true
    ),

    FILESYSTEM_RENAME(
        displayName = "Rename Files",
        description = "Rename files and directories",
        isDangerous = true
    ),

    FILESYSTEM_CREATE(
        displayName = "Create Files/Directories",
        description = "Create new files and directories",
        isDangerous = true
    ),

    FILESYSTEM_SEARCH(
        displayName = "Search Files",
        description = "Search for files by name and content",
        isDangerous = false
    ),

    FILESYSTEM_ARCHIVE(
        displayName = "Archive Operations",
        description = "Create and extract archives (zip, tar, etc.)",
        isDangerous = true
    ),

    FILESYSTEM_METADATA(
        displayName = "View Metadata",
        description = "View file metadata (size, dates, permissions)",
        isDangerous = false
    ),

    FILESYSTEM_SHARE(
        displayName = "Share Files",
        description = "Share files with other apps",
        isDangerous = true
    ),

    FILESYSTEM_COPY(
        displayName = "Copy Files",
        description = "Copy files to new locations",
        isDangerous = false
    ),

    FILESYSTEM_MOVE(
        displayName = "Move Files",
        description = "Move files to new locations",
        isDangerous = true
    ),

    // Advanced capabilities
    FILESYSTEM_ENCRYPT(
        displayName = "Encrypt Files",
        description = "Encrypt files with user keys",
        isDangerous = true
    ),

    FILESYSTEM_DECRYPT(
        displayName = "Decrypt Files",
        description = "Decrypt encrypted files",
        isDangerous = true
    ),

    FILESYSTEM_HASH(
        displayName = "Calculate Hashes",
        description = "Calculate file checksums",
        isDangerous = false
    ),

    // Session capabilities
    SESSION_MANAGE(
        displayName = "Manage Sessions",
        description = "Create and revoke sessions",
        isDangerous = true
    ),

    // Plugin capabilities
    PLUGIN_INSTALL(
        displayName = "Install Plugins",
        description = "Install JavaScript plugins",
        isDangerous = true
    ),

    PLUGIN_UNINSTALL(
        displayName = "Uninstall Plugins",
        description = "Uninstall JavaScript plugins",
        isDangerous = true
    ),

    // Admin capabilities
    ADMIN_CONFIG(
        displayName = "Configure Settings",
        description = "Modify application configuration",
        isDangerous = true
    ),

    ADMIN_AUDIT(
        displayName = "View Audit Logs",
        description = "View security audit logs",
        isDangerous = false
    );

    /**
     * Checks if this capability requires user approval.
     */
    fun requiresApproval(): Boolean = isDangerous

    /**
     * Gets the category of this capability.
     */
    fun getCategory(): CapabilityCategory {
        return when {
            name.startsWith("FILESYSTEM_") -> CapabilityCategory.FILESYSTEM
            name.startsWith("SESSION_") -> CapabilityCategory.SESSION
            name.startsWith("PLUGIN_") -> CapabilityCategory.PLUGIN
            name.startsWith("ADMIN_") -> CapabilityCategory.ADMIN
            else -> CapabilityCategory.OTHER
        }
    }

    companion object {
        /**
         * Gets capabilities by category.
         */
        fun getByCategory(category: CapabilityCategory): List<Capability> {
            return entries.filter { it.getCategory() == category }
        }

        /**
         * Gets all dangerous capabilities.
         */
        fun getDangerous(): List<Capability> {
            return entries.filter { it.isDangerous }
        }

        /**
         * Checks if a set of capabilities includes all required ones.
         */
        fun hasAll(required: Set<Capability>, granted: Set<Capability>): Boolean {
            return required.all { it in granted }
        }

        /**
         * Checks if a set of capabilities includes any of the required ones.
         */
        fun hasAny(required: Set<Capability>, granted: Set<Capability>): Boolean {
            return required.any { it in granted }
        }
    }
}

enum class CapabilityCategory {
    FILESYSTEM,
    SESSION,
    PLUGIN,
    ADMIN,
    OTHER
}
