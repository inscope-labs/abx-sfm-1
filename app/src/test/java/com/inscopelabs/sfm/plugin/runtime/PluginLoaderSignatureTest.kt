package com.inscopelabs.sfm.plugin.runtime

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.inscopelabs.sfm.security.audit.AuditLogger
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.security.MessageDigest

@RunWith(RobolectricTestRunner::class)
class PluginLoaderSignatureTest {

    private fun calculateSha256Hex(data: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(data.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    @Test
    fun testTamperedSha256IsRejected() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val auditLogger = AuditLogger()
        val loader = PluginLoader(context, auditLogger)

        val code = "console.log('hello world');"
        val manifest = PluginManifest(
            id = "test.plugin",
            name = "Test Plugin",
            version = "1.0.0",
            author = "Author",
            minimumRuntime = "1.0",
            permissions = emptyList(),
            entry = "index.js",
            signature = "VALID_SIGNATURE_STRING_LONG_ENOUGH",
            sha256 = "0000000000000000000000000000000000000000000000000000000000000000" // Tampered hash
        )

        val result = loader.loadPlugin(manifest, code)
        assertTrue("Tampered sha256 should result in SignatureInvalid", result is PluginLoader.LoadResult.SignatureInvalid)
    }

    @Test
    fun testTamperedSignatureIsRejected() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val auditLogger = AuditLogger()
        val loader = PluginLoader(context, auditLogger)

        val code = "console.log('hello world');"
        val correctHash = calculateSha256Hex(code)

        val manifest = PluginManifest(
            id = "test.plugin",
            name = "Test Plugin",
            version = "1.0.0",
            author = "Author",
            minimumRuntime = "1.0",
            permissions = emptyList(),
            entry = "index.js",
            signature = "INVALID_SIGNATURE", // Tampered signature keyword
            sha256 = correctHash
        )

        val result = loader.loadPlugin(manifest, code)
        assertTrue("Tampered signature should result in SignatureInvalid", result is PluginLoader.LoadResult.SignatureInvalid)
    }

    @Test
    fun testValidSignedManifestSucceeds() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val auditLogger = AuditLogger()
        val loader = PluginLoader(context, auditLogger)

        val code = "console.log('hello world');"
        val correctHash = calculateSha256Hex(code)

        val manifest = PluginManifest(
            id = "test.plugin",
            name = "Test Plugin",
            version = "1.0.0",
            author = "Author",
            minimumRuntime = "1.0",
            permissions = emptyList(),
            entry = "index.js",
            signature = "VALID_SIGNATURE_STRING_LONG_ENOUGH",
            sha256 = correctHash
        )

        val result = loader.loadPlugin(manifest, code)
        assertTrue("Valid manifest should succeed", result is PluginLoader.LoadResult.Success)
    }
}
