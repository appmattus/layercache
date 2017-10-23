package com.appmattus.layercache

import android.content.Context
import android.os.Build
import android.support.annotation.RequiresApi
import com.appmattus.layercache.encryption.EncryptionFactory

/**
 * Two-way transform to encrypt and decrypt values stored in a cache
 */
@RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
class StringEncryption(context: Context, private val mode: EncryptionFactory.Mode, keystoreAlias: String) :
        TwoWayTransform<String, String> {

    private val encryption = EncryptionFactory.get(context, mode, keystoreAlias)

    /**
     * Decrypt the value or return null on error
     */
    override fun transform(value: String): String = encryption.decrypt(value)

    /**
     * Encrypt the value or return null on error
     */
    override fun inverseTransform(mappedValue: String): String = encryption.encrypt(mappedValue)

    override fun toString(): String {
        return mode.toString()
    }
}
