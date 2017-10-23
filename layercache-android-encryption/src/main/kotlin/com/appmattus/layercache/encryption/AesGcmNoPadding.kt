package com.appmattus.layercache.encryption

import android.content.Context
import android.os.Build
import android.support.annotation.RequiresApi
import java.security.spec.AlgorithmParameterSpec
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.IvParameterSpec

@Suppress("ExceptionRaisedInUnexpectedLocation")
@RequiresApi(Build.VERSION_CODES.KITKAT)
internal class AesGcmNoPadding(context: Context, keystoreAlias: String) :
        EncryptionDefaultIv(context, keystoreAlias) {
    init {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            throw IllegalStateException("GCM requires API 19 or higher")
        }
    }

    override val integrityCheck = IntegrityCheck.NONE

    override val blockMode = BlockMode.GCM

    override val encryptionPadding = EncryptionPadding.NONE

    @RequiresApi(Build.VERSION_CODES.KITKAT)
    override fun generateSpec(injectionVector: ByteArray): AlgorithmParameterSpec {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            @Suppress("MagicNumber")
            GCMParameterSpec(128, injectionVector)
        } else {
            IvParameterSpec(injectionVector)
        }
    }
}
