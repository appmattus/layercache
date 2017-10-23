package com.appmattus.layercache.encryption

/**
 * Represents an encryptor that can encrypt and decrypt text
 */
interface Encryption {
    /**
     * Encrypt the provided text
     */
    fun encrypt(value: String): String

    /**
     * Decrypt the provided text (inverse of encrypt)
     */
    fun decrypt(mappedValue: String): String
}
