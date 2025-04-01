package com.example.ht2000obd.base

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.example.ht2000obd.utils.LogUtils
import java.security.KeyStore
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Interface for encryption operations
 */
interface Encryptor {
    fun encrypt(data: String): String
    fun decrypt(data: String): String
}

/**
 * Interface for hashing operations
 */
interface Hasher {
    fun hash(data: String): String
    fun verify(data: String, hash: String): Boolean
}

/**
 * Base implementation of Android Keystore encryption
 */
class KeystoreEncryptor(context: Context) : Encryptor {
    private val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply {
        load(null)
    }

    private val cipher = Cipher.getInstance(TRANSFORMATION)

    init {
        createKey()
    }

    private fun createKey() {
        try {
            if (!keyStore.containsAlias(KEY_ALIAS)) {
                val keyGenerator = KeyGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_AES,
                    KEYSTORE_PROVIDER
                )

                val keyGenParameterSpec = KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .build()

                keyGenerator.init(keyGenParameterSpec)
                keyGenerator.generateKey()
            }
        } catch (e: Exception) {
            LogUtils.e("Security", "Error creating encryption key", e)
            throw SecurityException("Error creating encryption key", e)
        }
    }

    override fun encrypt(data: String): String {
        return try {
            val key = keyStore.getKey(KEY_ALIAS, null) as SecretKey
            cipher.init(Cipher.ENCRYPT_MODE, key)
            val iv = cipher.iv
            val encrypted = cipher.doFinal(data.toByteArray())
            
            // Combine IV and encrypted data
            val combined = ByteArray(iv.size + encrypted.size)
            System.arraycopy(iv, 0, combined, 0, iv.size)
            System.arraycopy(encrypted, 0, combined, iv.size, encrypted.size)
            
            Base64.encodeToString(combined, Base64.DEFAULT)
        } catch (e: Exception) {
            LogUtils.e("Security", "Error encrypting data", e)
            throw SecurityException("Error encrypting data", e)
        }
    }

    override fun decrypt(data: String): String {
        return try {
            val key = keyStore.getKey(KEY_ALIAS, null) as SecretKey
            val combined = Base64.decode(data, Base64.DEFAULT)
            
            // Extract IV and encrypted data
            val iv = ByteArray(GCM_IV_LENGTH)
            val encrypted = ByteArray(combined.size - GCM_IV_LENGTH)
            System.arraycopy(combined, 0, iv, 0, GCM_IV_LENGTH)
            System.arraycopy(combined, GCM_IV_LENGTH, encrypted, 0, encrypted.size)
            
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, iv))
            String(cipher.doFinal(encrypted))
        } catch (e: Exception) {
            LogUtils.e("Security", "Error decrypting data", e)
            throw SecurityException("Error decrypting data", e)
        }
    }

    companion object {
        private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        private const val KEY_ALIAS = "AppSecretKey"
        private const val TRANSFORMATION =
            "${KeyProperties.KEY_ALGORITHM_AES}/${KeyProperties.BLOCK_MODE_GCM}/${KeyProperties.ENCRYPTION_PADDING_NONE}"
        private const val GCM_IV_LENGTH = 12
    }
}

/**
 * Base implementation of SHA-256 hashing
 */
class SHA256Hasher : Hasher {
    private val messageDigest = MessageDigest.getInstance("SHA-256")

    override fun hash(data: String): String {
        return try {
            val hash = messageDigest.digest(data.toByteArray())
            bytesToHex(hash)
        } catch (e: Exception) {
            LogUtils.e("Security", "Error hashing data", e)
            throw SecurityException("Error hashing data", e)
        }
    }

    override fun verify(data: String, hash: String): Boolean {
        return try {
            hash(data) == hash
        } catch (e: Exception) {
            LogUtils.e("Security", "Error verifying hash", e)
            false
        }
    }

    private fun bytesToHex(bytes: ByteArray): String {
        val hexChars = CharArray(bytes.size * 2)
        for (i in bytes.indices) {
            val v = bytes[i].toInt() and 0xFF
            hexChars[i * 2] = HEX_ARRAY[v ushr 4]
            hexChars[i * 2 + 1] = HEX_ARRAY[v and 0x0F]
        }
        return String(hexChars)
    }

    companion object {
        private val HEX_ARRAY = "0123456789ABCDEF".toCharArray()
    }
}

/**
 * Security utilities
 */
object SecurityUtils {
    /**
     * Generate a random salt
     */
    fun generateSalt(length: Int = 16): String {
        val bytes = ByteArray(length)
        java.security.SecureRandom().nextBytes(bytes)
        return Base64.encodeToString(bytes, Base64.DEFAULT)
    }

    /**
     * Generate a secure random string
     */
    fun generateRandomString(length: Int = 32): String {
        val allowedChars = ('A'..'Z') + ('a'..'z') + ('0'..'9')
        return (1..length)
            .map { allowedChars.random() }
            .joinToString("")
    }

    /**
     * Sanitize user input
     */
    fun sanitizeInput(input: String): String {
        return input.replace(Regex("[^A-Za-z0-9]"), "")
    }

    /**
     * Validate password strength
     */
    fun isPasswordStrong(password: String): Boolean {
        val hasUpperCase = password.any { it.isUpperCase() }
        val hasLowerCase = password.any { it.isLowerCase() }
        val hasDigit = password.any { it.isDigit() }
        val hasSpecialChar = password.any { !it.isLetterOrDigit() }
        val isLongEnough = password.length >= 8

        return hasUpperCase && hasLowerCase && hasDigit && hasSpecialChar && isLongEnough
    }
}

/**
 * Security exceptions
 */
class SecurityException : Exception {
    constructor(message: String) : super(message)
    constructor(message: String, cause: Throwable) : super(message, cause)
}