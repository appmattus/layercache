package com.appmattus.layercache.encryption

import android.security.keystore.KeyProperties

@Suppress("UseDataClass") // incorrectly reported
internal enum class EncryptionPadding(val padding: String) {
    NONE(KeyProperties.ENCRYPTION_PADDING_NONE),
    PKCS7(KeyProperties.ENCRYPTION_PADDING_PKCS7),
    RSA_OAEP(KeyProperties.ENCRYPTION_PADDING_RSA_OAEP),
    RSA_PKCS1(KeyProperties.ENCRYPTION_PADDING_RSA_PKCS1);

    override fun toString() = padding
}
