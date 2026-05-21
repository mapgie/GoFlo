package com.mapgie.goflo.data.security

import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object PinManager {
    private const val ITERATIONS = 100_000
    private const val KEY_LENGTH_BITS = 256
    private const val ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val SALT_BYTES = 16

    fun generateSalt(): String {
        val salt = ByteArray(SALT_BYTES)
        SecureRandom().nextBytes(salt)
        return Base64.encodeToString(salt, Base64.NO_WRAP)
    }

    fun hashPin(pin: String, saltBase64: String): String {
        val salt = Base64.decode(saltBase64, Base64.NO_WRAP)
        val spec = PBEKeySpec(pin.toCharArray(), salt, ITERATIONS, KEY_LENGTH_BITS)
        return try {
            val hash = SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).encoded
            Base64.encodeToString(hash, Base64.NO_WRAP)
        } finally {
            spec.clearPassword()
        }
    }

    // Constant-time comparison to prevent timing attacks
    fun verifyPin(pin: String, storedHash: String, storedSalt: String): Boolean {
        val candidateHash = hashPin(pin, storedSalt)
        return MessageDigest.isEqual(
            Base64.decode(candidateHash, Base64.NO_WRAP),
            Base64.decode(storedHash, Base64.NO_WRAP)
        )
    }
}
