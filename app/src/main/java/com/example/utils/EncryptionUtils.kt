package com.example.utils

import android.util.Base64

object EncryptionUtils {
    private const val SECRET_SALT = "PulseAISecretSalt2026"

    fun encrypt(plainText: String?): String? {
        if (plainText.isNullOrEmpty()) return null
        return try {
            val bytes = plainText.toByteArray(Charsets.UTF_8)
            val saltBytes = SECRET_SALT.toByteArray(Charsets.UTF_8)
            val xorBytes = ByteArray(bytes.size)
            for (i in bytes.indices) {
                xorBytes[i] = (bytes[i].toInt() xor saltBytes[i % saltBytes.size].toInt()).toByte()
            }
            Base64.encodeToString(xorBytes, Base64.NO_WRAP)
        } catch (e: Exception) {
            plainText
        }
    }

    fun decrypt(encryptedText: String?): String? {
        if (encryptedText.isNullOrEmpty()) return null
        return try {
            val xorBytes = Base64.decode(encryptedText, Base64.NO_WRAP)
            val saltBytes = SECRET_SALT.toByteArray(Charsets.UTF_8)
            val bytes = ByteArray(xorBytes.size)
            for (i in xorBytes.indices) {
                bytes[i] = (xorBytes[i].toInt() xor saltBytes[i % saltBytes.size].toInt()).toByte()
            }
            String(bytes, Charsets.UTF_8)
        } catch (e: Exception) {
            encryptedText
        }
    }
}
