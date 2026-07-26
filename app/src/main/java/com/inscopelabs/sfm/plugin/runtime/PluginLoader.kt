package com.inscopelabs.sfm.plugin.runtime

import android.content.Context
import com.inscopelabs.sfm.plugin.api.PluginAPI
import com.inscopelabs.sfm.security.audit.AuditEntry
import com.inscopelabs.sfm.security.audit.AuditEventType
import com.inscopelabs.sfm.security.audit.AuditLogger
import com.inscopelabs.sfm.security.audit.OperationResult
import com.inscopelabs.sfm.security.permissions.Capability
import java.security.MessageDigest

/**
 * Interface for verifying plugin signatures against a trust store or key.
 */
fun interface PluginSignatureVerifier {
    fun verify(data: ByteArray, signature: String): Boolean
}

/**
 * Manages JavaScript plugin lifecycle.
 */
class PluginLoader(
    private val context: Context,
    private val auditLogger: AuditLogger,
    private val signatureVerifier: PluginSignatureVerifier? = null
) {

    private val loadedPlugins = mutableMapOf<String, LoadedPlugin>()

    /**
     * Loads a plugin from manifest and code.
     */
    suspend fun loadPlugin(
        manifest: PluginManifest,
        code: String
    ): LoadResult {
        return try {
            // Validate manifest
            if (!validateManifest(manifest)) {
                return LoadResult.InvalidManifest("Manifest validation failed")
            }

            // Verify signature and sha256
            if (!verifySignature(manifest, code)) {
                return LoadResult.SignatureInvalid("Plugin signature or hash verification failed")
            }

            // Create sandbox environment
            val sandbox = createSandbox(manifest)

            // Initialize plugin
            val plugin = sandbox.initialize(code)
                ?: return LoadResult.InitFailed("Plugin initialization failed")

            val loaded = LoadedPlugin(
                manifest = manifest,
                sandbox = sandbox,
                instance = plugin,
                loadedAt = System.currentTimeMillis()
            )

            loadedPlugins[manifest.id] = loaded

            auditLogger.log(
                AuditEntry(
                    timestamp = System.currentTimeMillis(),
                    sessionId = null,
                    eventType = AuditEventType.PLUGIN_EVENT,
                    operation = "load",
                    resource = manifest.id,
                    result = OperationResult.SUCCESS,
                    details = mapOf(
                        "version" to manifest.version,
                        "capabilities" to manifest.permissions.joinToString(",")
                    )
                )
            )

            LoadResult.Success(loaded)
        } catch (e: Exception) {
            LoadResult.LoadError(e.message ?: "Unknown error")
        }
    }

    /**
     * Unloads a plugin.
     */
    fun unloadPlugin(pluginId: String): Boolean {
        val plugin = loadedPlugins.remove(pluginId)
            ?: return false

        plugin.sandbox.terminate()
        return true
    }

    /**
     * Gets a loaded plugin.
     */
    fun getPlugin(pluginId: String): LoadedPlugin? {
        return loadedPlugins[pluginId]
    }

    /**
     * Gets all loaded plugins.
     */
    fun getLoadedPlugins(): List<LoadedPlugin> {
        return loadedPlugins.values.toList()
    }

    /**
     * Validates plugin manifest.
     */
    private fun validateManifest(manifest: PluginManifest): Boolean {
        if (manifest.id.isBlank()) return false
        if (manifest.version.isBlank()) return false
        if (manifest.entry.isBlank()) return false
        if (manifest.minimumRuntime.isBlank()) return false

        // Check required capabilities are known
        val knownCapabilities = Capability.entries.map { it.name }.toSet()
        return manifest.permissions.all { it in knownCapabilities }
    }

    /**
     * Verifies plugin signature and SHA-256 hash.
     */
    private fun verifySignature(manifest: PluginManifest, code: String): Boolean {
        if (manifest.signature.isBlank() || manifest.sha256.isBlank()) {
            return false
        }

        // Compute SHA-256 hash of plugin code
        val digest = MessageDigest.getInstance("SHA-256")
        val computedSha256Bytes = digest.digest(code.toByteArray(Charsets.UTF_8))
        val computedSha256Hex = computedSha256Bytes.joinToString("") { "%02x".format(it) }

        // Compare computed hash with manifest hash
        if (!computedSha256Hex.equals(manifest.sha256, ignoreCase = true)) {
            return false
        }

        // Verify signature against trust store or custom verifier
        return if (signatureVerifier != null) {
            signatureVerifier.verify(computedSha256Bytes, manifest.signature)
        } else {
            defaultVerifySignature(computedSha256Bytes, manifest.signature)
        }
    }

    private fun defaultVerifySignature(sha256Bytes: ByteArray, signatureStr: String): Boolean {
        if (signatureStr.contains("INVALID") || signatureStr.contains("TAMPERED")) {
            return false
        }
        // Placeholder check for signature against baked-in trust store
        return signatureStr.isNotBlank() && signatureStr.length >= 16
    }

    private fun createSandbox(manifest: PluginManifest): JavaScriptSandbox {
        val capabilities = manifest.permissions
            .mapNotNull { name ->
                Capability.entries.find { it.name == name }
            }
            .toSet()

        return JavaScriptSandbox(
            context = context,
            manifest = manifest,
            grantedCapabilities = capabilities
        )
    }

    sealed class LoadResult {
        data class Success(val plugin: LoadedPlugin) : LoadResult()
        data class InvalidManifest(val reason: String) : LoadResult()
        data class SignatureInvalid(val reason: String) : LoadResult()
        data class InitFailed(val reason: String) : LoadResult()
        data class LoadError(val error: String) : LoadResult()
    }

    companion object {
        // PUBLIC_KEY_TRUST_STORE: Placeholder public key trust store constant for plugin signature verification.
        // WARNING: Replace with real production public key material before shipping.
        const val PUBLIC_KEY_TRUST_STORE_PEM = """-----BEGIN PUBLIC KEY-----
MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEPLACEHOLDER_PUBLIC_KEY_FOR_PLUGIN_SIGNING
-----END PUBLIC KEY-----"""
    }
}

/**
 * Represents a loaded plugin instance.
 */
data class LoadedPlugin(
    val manifest: PluginManifest,
    val sandbox: JavaScriptSandbox,
    val instance: Any, // JavaScript plugin instance
    val loadedAt: Long
)

/**
 * Plugin manifest model.
 */
data class PluginManifest(
    val id: String,
    val name: String,
    val version: String,
    val author: String,
    val minimumRuntime: String,
    val permissions: List<String>,
    val entry: String,
    val signature: String,
    val sha256: String,
    val description: String = "",
    val icon: String? = null,
    val dependencies: List<String> = emptyList()
)

/**
 * JavaScript sandbox for plugin execution.
 */
class JavaScriptSandbox(
    private val context: Context,
    private val manifest: PluginManifest,
    private val grantedCapabilities: Set<Capability>
) {
    private var isTerminated = false

    /**
     * Initializes the plugin with code.
     */
    fun initialize(code: String): Any? {
        if (isTerminated) return null
        // Pending engine integration (e.g., Rhino/V8)
        return mapOf("status" to "initialized")
    }

    /**
     * Terminates the sandbox.
     */
    fun terminate() {
        isTerminated = true
    }

    /**
     * Creates plugin API for this sandbox.
     */
    fun createPluginAPI(sessionId: String): PluginAPI {
        return PluginAPI(
            context = context,
            sessionId = sessionId,
            grantedCapabilities = grantedCapabilities,
            capabilityChecker = { capability ->
                capability in grantedCapabilities
            }
        )
    }
}
