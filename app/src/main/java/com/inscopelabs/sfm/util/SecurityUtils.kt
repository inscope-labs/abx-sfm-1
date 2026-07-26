package com.inscopelabs.sfm.util

import java.security.MessageDigest

/**
 * Calculates file checksums.
 */
class ChecksumCalculator {

    /**
     * Calculates SHA-256 hash of data.
     */
    fun sha256(data: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(data).joinToString("") { "%02x".format(it) }
    }

    /**
     * Calculates MD5 hash of data.
     */
    fun md5(data: ByteArray): String {
        val digest = MessageDigest.getInstance("MD5")
        return digest.digest(data).joinToString("") { "%02x".format(it) }
    }

    /**
     * Calculates SHA-1 hash of data.
     */
    fun sha1(data: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-1")
        return digest.digest(data).joinToString("") { "%02x".format(it) }
    }
}

/**
 * Securely wipes data from memory.
 */
object SecureWiper {

    /**
     * Overwrites a byte array with zeros.
     */
    fun wipe(data: ByteArray) {
        data.fill(0)
    }

    /**
     * Overwrites a string's backing char array.
     */
    fun wipeString(string: StringBuilder) {
        for (i in string.indices) {
            string.setCharAt(i, '\u0000')
        }
        string.clear()
    }
}

/**
 * Android Keystore helper utilities.
 */
object AndroidKeystoreHelper {

    private const val ANDROID_KEYSTORE = "AndroidKeyStore"

    /**
     * Checks if a key exists in the keystore.
     */
    fun hasKey(keyAlias: String): Boolean {
        return try {
            val keyStore = java.security.KeyStore.getInstance(ANDROID_KEYSTORE)
            keyStore.load(null)
            keyStore.containsAlias(keyAlias)
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Deletes a key from the keystore.
     */
    fun deleteKey(keyAlias: String): Boolean {
        return try {
            val keyStore = java.security.KeyStore.getInstance(ANDROID_KEYSTORE)
            keyStore.load(null)
            keyStore.deleteEntry(keyAlias)
            true
        } catch (e: Exception) {
            false
        }
    }
}

/**
 * Detects debugging and root access.
 */
object DebugDetector {

    /**
     * Checks if device is rooted.
     */
    fun isRooted(): Boolean {
        return checkRootBinaries() || checkSuCommand()
    }

    private fun checkRootBinaries(): Boolean {
        val paths = listOf(
            "/system/app/Superuser.apk",
            "/sbin/su",
            "/system/bin/su",
            "/system/xbin/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/system/sd/xbin/su",
            "/system/bin/failsafe/su",
            "/data/local/su",
            "/su/bin/su"
        )
        return paths.any { java.io.File(it).exists() }
    }

    private fun checkSuCommand(): Boolean {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("/system/xbin/which", "su"))
            val reader = java.io.BufferedReader(java.io.InputStreamReader(process.inputStream))
            reader.readLine() != null
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Checks if debugger is attached.
     */
    fun isDebuggerAttached(): Boolean {
        return android.os.Debug.isDebuggerConnected()
    }

    /**
     * Checks for suspicious apps (Magisk, etc.).
     */
    fun hasMagisk(): Boolean {
        val paths = listOf(
            "/sbin/.magisk",
            "/data/adb/magisk",
            "/data/adb/magisk.img"
        )
        return paths.any { java.io.File(it).exists() }
    }
}
