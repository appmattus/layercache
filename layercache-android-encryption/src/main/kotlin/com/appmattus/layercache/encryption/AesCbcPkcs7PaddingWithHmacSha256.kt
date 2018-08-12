/*
 * Copyright 2017 Appmattus Limited
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.appmattus.layercache.encryption

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
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
