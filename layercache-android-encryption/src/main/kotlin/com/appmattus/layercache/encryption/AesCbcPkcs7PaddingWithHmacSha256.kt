package com.appmattus.layercache.encryption

import android.content.Context
import android.os.Build
import android.support.annotation.RequiresApi
import java.security.SecureRandom
import java.security.spec.AlgorithmParameterSpec
import javax.crypto.spec.IvParameterSpec

@Suppress("ExceptionRaisedInUnexpectedLocation")
@RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
internal class AesCbcPkcs7PaddingWithHmacSha256(context: Context, keystoreAlias: String) :
        EncryptionBase(context, keystoreAlias) {
    init {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2) {
            throw IllegalStateException("CBC requires API 18 or higher")
        }
    }

    override val integrityCheck = IntegrityCheck.HMAC_SHA256

    override val blockMode = BlockMode.CBC

    override val encryptionPadding = EncryptionPadding.PKCS7

    override fun providesIv() = true

    override fun generateIv(): AlgorithmParameterSpec {
        val secureRandom = SecureRandom()
        @Suppress("MagicNumber")
        val randomBytes = ByteArray(16)
        secureRandom.nextBytes(randomBytes)

        return IvParameterSpec(randomBytes)
    }

    override fun generateSpec(injectionVector: ByteArray): AlgorithmParameterSpec = IvParameterSpec(injectionVector)
}
