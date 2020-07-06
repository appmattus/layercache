package com.appmattus.layercache

import android.os.Build
import androidx.annotation.RequiresApi

/**
 * The encryption mode to use
 */
enum class EncryptionMode {
    /**
     * AES GCM encryption, requires Android KitKat API 19
     */
    @RequiresApi(Build.VERSION_CODES.KITKAT)
    AES_GCM_NoPadding,

    /**
     * AES CBC PKCS7Padding with HmacSHA26 encryption
     */
    AES_CBC_PKCS7Padding_with_HMAC
}
