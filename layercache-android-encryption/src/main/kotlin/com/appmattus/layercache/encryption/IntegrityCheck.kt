package com.appmattus.layercache.encryption

import android.security.keystore.KeyProperties

@Suppress("UseDataClass", "MagicNumber") // incorrectly reported
internal enum class IntegrityCheck(val required: Boolean, val algorithm: String, val bits: Int) {
    NONE(false, "", 0),
    HMAC_SHA256(true, KeyProperties.KEY_ALGORITHM_HMAC_SHA256, 256);
}
