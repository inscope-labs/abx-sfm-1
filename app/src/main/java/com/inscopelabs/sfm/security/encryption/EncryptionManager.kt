package com.inscopelabs.sfm.security.encryption

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Manages file encryption using AES-256-GCM with Android Keystore.
 */
class EncryptionManager(private val context: Context) {

    private val keyStore: KeyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply {
        load(null)
    }

    /**
     * Encrypts data using AES-256-GCM.
     */
    fun encrypt(data: ByteArray, keyAlias: String): EncryptedData {
        val secretKey = getOrCreateKey(keyAlias)

        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)

        val encryptedBytes = cipher.doFinal(data)
        val iv = cipher.iv

        return EncryptedData(
            ciphertext = encryptedBytes,
            iv = iv,
            keyAlias = keyAlias
        )
    }

    /**
     * Decrypts data using AES-256-GCM.
     */
    fun decrypt(encryptedData: EncryptedData): ByteArray {
        val secretKey = keyStore.getKey(encryptedData.keyAlias, null) as? SecretKey
            ?: throw EncryptionException("Key not found: ${encryptedData.keyAlias}")

        val cipher = Cipher.getInstance(TRANSFORMATION)
        val spec = GCMParameterSpec(TAG_LENGTH_BITS, encryptedData.iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)

        return cipher.doFinal(encryptedData.ciphertext)
    }

    /**
     * Encrypts data with a new key for temporary cache.
     */
    fun encryptForCache(data: ByteArray): EncryptedData {
        return encrypt(data, CACHE_KEY_ALIAS)
    }

    /**
     * Decrypts temporary cache data.
     */
    fun decryptFromCache(encryptedData: EncryptedData): ByteArray {
        if (encryptedData.keyAlias != CACHE_KEY_ALIAS) {
            throw EncryptionException("Invalid cache key")
        }
        return decrypt(encryptedData)
    }

    /**
     * Generates a new encryption key.
     */
    fun generateKey(alias: String, requireUserAuth: Boolean = false) {
        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            ANDROID_KEYSTORE
        )

        val builder = KeyGenParameterSpec.Builder(
            alias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(KEY_SIZE)
            .setRandomizedEncryptionRequired(true)

        if (requireUserAuth) {
            builder.setUserAuthenticationRequired(true)
                .setUserAuthenticationValidityDurationSeconds(AUTH_VALIDITY_SECONDS)
        }

        keyGenerator.init(builder.build())
        keyGenerator.generateKey()
    }

    /**
     * Deletes an encryption key.
     */
    fun deleteKey(alias: String): Boolean {
        return try {
            keyStore.deleteEntry(alias)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Checks if a key exists.
     */
    fun hasKey(alias: String): Boolean {
        return keyStore.containsAlias(alias)
    }

    /**
     * Rotates a key by generating a new one with the same alias.
     */
    fun rotateKey(alias: String) {
        deleteKey(alias)
        generateKey(alias)
    }

    private fun getOrCreateKey(alias: String): SecretKey {
        return try {
            keyStore.getKey(alias, null) as? SecretKey
                ?: throw EncryptionException("Key not found")
        } catch (e: Exception) {
            generateKey(alias)
            keyStore.getKey(alias, null) as SecretKey
        }
    }

    /**
     * Calculates HMAC-SHA256 signature using a dedicated Keystore key or fallback.
     */
    fun calculateHmac(data: ByteArray, alias: String = AUDIT_SIGNING_KEY_ALIAS): String {
        val secretKey = getOrCreateHmacKey(alias)
        val mac = javax.crypto.Mac.getInstance("HmacSHA256")
        mac.init(secretKey)
        val hmacBytes = mac.doFinal(data)
        return hmacBytes.joinToString("") { "%02x".format(it) }
    }

    private fun getOrCreateHmacKey(alias: String): SecretKey {
        return try {
            (keyStore.getKey(alias, null) as? SecretKey) ?: generateHmacKey(alias)
        } catch (e: Exception) {
            generateHmacKey(alias)
        }
    }

    private fun generateHmacKey(alias: String): SecretKey {
        return try {
            val keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_HMAC_SHA256,
                ANDROID_KEYSTORE
            )
            val builder = KeyGenParameterSpec.Builder(
                alias,
                KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY
            )
                .setKeySize(256)
            keyGenerator.init(builder.build())
            keyGenerator.generateKey()
        } catch (e: Exception) {
            // Software key fallback for test environments without AndroidKeyStore
            val keyGenerator = KeyGenerator.getInstance("HmacSHA256")
            keyGenerator.init(256)
            keyGenerator.generateKey()
        }
    }

    /**
     * Calculates SHA-256 hash of data.
     */
    fun calculateHash(data: ByteArray): String {
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(data)
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Calculates hash of a file.
     */
    suspend fun calculateFileHash(uri: android.net.Uri): String {
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        context.contentResolver.openInputStream(uri)?.use { input ->
            val buffer = ByteArray(BUFFER_SIZE)
            var bytesRead: Int
            while (input.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    data class EncryptedData(
        val ciphertext: ByteArray,
        val iv: ByteArray,
        val keyAlias: String
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as EncryptedData

            if (!ciphertext.contentEquals(other.ciphertext)) return false
            if (!iv.contentEquals(other.iv)) return false
            if (keyAlias != other.keyAlias) return false

            return true
        }

        override fun hashCode(): Int {
            var result = ciphertext.contentHashCode()
            result = 31 * result + iv.contentHashCode()
            result = 31 * result + keyAlias.hashCode()
            return result
        }

        fun toBase64(): String {
            return "${android.util.Base64.encodeToString(ciphertext, android.util.Base64.NO_WRAP)}|" +
                    "${android.util.Base64.encodeToString(iv, android.util.Base64.NO_WRAP)}|$keyAlias"
        }

        companion object {
            fun fromBase64(encoded: String): EncryptedData {
                val parts = encoded.split("|")
                require(parts.size == 3) { "Invalid encrypted data format" }
                return EncryptedData(
                    ciphertext = android.util.Base64.decode(parts[0], android.util.Base64.NO_WRAP),
                    iv = android.util.Base64.decode(parts[1], android.util.Base64.NO_WRAP),
                    keyAlias = parts[2]
                )
            }
        }
    }

    class EncryptionException(message: String, cause: Throwable? = null) :
        Exception(message, cause)

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val KEY_SIZE = 256
        private const val TAG_LENGTH_BITS = 128
        private const val BUFFER_SIZE = 8192
        private const val AUTH_VALIDITY_SECONDS = 30
        private const val CACHE_KEY_ALIAS = "filemanager_cache_key"
        const val AUDIT_SIGNING_KEY_ALIAS = "audit_logging_signing_key"
    }
}
