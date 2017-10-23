package com.appmattus.layercache.encryption

import android.security.keystore.KeyProperties

@Suppress("UseDataClass") // incorrectly reported
internal enum class BlockMode(val mode: String) {
    CBC(KeyProperties.BLOCK_MODE_CBC),
    CTR(KeyProperties.BLOCK_MODE_CTR),
    ECB(KeyProperties.BLOCK_MODE_ECB),
    GCM(KeyProperties.BLOCK_MODE_GCM);

    override fun toString() = mode
}
