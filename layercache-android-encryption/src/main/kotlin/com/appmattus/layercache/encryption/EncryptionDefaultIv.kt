package com.appmattus.layercache.encryption

import android.content.Context
import java.security.spec.AlgorithmParameterSpec

@Suppress("UnnecessaryAbstractClass") // incorrectly reported
internal abstract class EncryptionDefaultIv(context: Context, keystoreAlias: String) :
        EncryptionBase(context, keystoreAlias) {
    override fun providesIv() = false

    override fun generateIv(): AlgorithmParameterSpec = throw IllegalStateException("Should not be called")
}
