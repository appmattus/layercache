package com.appmattus.layercache.encryption

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.support.annotation.RequiresApi

/**
 * Factory to generate an encryptor
 */
@RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
class EncryptionFactory {
    /**
     * The encryption mode to use
     */
    enum class Mode {
        /**
         * AES GCM encryption
         */
        @RequiresApi(Build.VERSION_CODES.KITKAT)
        AES_GCM_NoPadding,

        /**
         * AES CBC PKCS7Padding with HmacSHA26 encryption
         */
        AES_CBC_PKCS7Padding_with_HMAC
    }

    companion object {
        /**
         * Generate an encryptor for the required encryption mode using the given alias for storage of keys.
         */
        @SuppressLint("NewApi")
        fun <T : Context> get(context: T, mode: Mode, keystoreAlias: String): Encryption {
            PRNGFixes.apply()

            return when (mode) {
                Mode.AES_GCM_NoPadding -> AesGcmNoPadding(context.applicationContext, keystoreAlias)
                Mode.AES_CBC_PKCS7Padding_with_HMAC -> AesCbcPkcs7PaddingWithHmacSha256(context.applicationContext,
                        keystoreAlias)
            }
        }
    }
}
