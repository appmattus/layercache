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
import java.nio.charset.Charset
import java.security.GeneralSecurityException
import java.security.spec.AlgorithmParameterSpec
import javax.crypto.Cipher
import javax.crypto.Mac
import kotlin.experimental.xor

@RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
internal abstract class EncryptionBase(private val context: Context, private val keystoreAlias: String) : Encryption {

    protected abstract fun generateSpec(injectionVector: ByteArray): AlgorithmParameterSpec
    protected abstract fun generateIv(): AlgorithmParameterSpec
    protected abstract fun providesIv(): Boolean

    protected abstract val blockMode: BlockMode
    protected abstract val encryptionPadding: EncryptionPadding

    protected abstract val integrityCheck: IntegrityCheck

    private val aesKey: AesKeyCompat by lazy {
        // lazy initialization as values defined in sub classes
        AesKeyCompat(context, blockMode, encryptionPadding, providesIv(), integrityCheck)
    }

    private fun getTransformation(): String {
        return "AES/$blockMode/$encryptionPadding"
    }

    final override fun encrypt(value: String): String {
        val secretKey = aesKey.retrieveConfidentialityKey(keystoreAlias)

        val cipher = Cipher.getInstance(getTransformation())

        if (providesIv()) {
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, generateIv())
        } else {
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        }

        val cipherText = cipher.doFinal(value.toByteArray())

        if (integrityCheck.required) {
            val mac = Mac.getInstance(integrityCheck.algorithm)
            mac.init(aesKey.retrieveIntegrityKey(keystoreAlias))

            val bytes = mac.doFinal(cipher.iv + cipherText)

            return cipherText.encodeBase64() + ":" + cipher.iv.encodeBase64() + ":" + bytes.encodeBase64()
        }

        return cipherText.encodeBase64() + ":" + cipher.iv.encodeBase64()
    }

    final override fun decrypt(mappedValue: String): String {
        val parts = mappedValue.split(":")
        val injectionVector = parts[1].decodeBase64()
        val cipherText = parts[0].decodeBase64()

        if (integrityCheck.required) {
            @Suppress("MagicNumber")
            if (parts.size != 3) {
                throw IllegalArgumentException("Cannot parse iv:ciphertext:mac")
            }
        } else {
            @Suppress("MagicNumber")
            if (parts.size != 2) {
                throw IllegalArgumentException("Cannot parse iv:ciphertext")
            }
        }

        if (integrityCheck.required) {
            val calculatedMac = parts[2].decodeBase64()

            @Suppress("MagicNumber")
            if (integrityCheck.bits / 8 != calculatedMac.size) {
                throw GeneralSecurityException("MAC wrong size")
            }

            val mac = Mac.getInstance(integrityCheck.algorithm)
            mac.init(aesKey.retrieveIntegrityKey(keystoreAlias))

            val computedMac = mac.doFinal(injectionVector + cipherText)

            if (!constantTimeEquals(computedMac, calculatedMac)) {
                throw GeneralSecurityException("MAC stored in civ does not match computed MAC.")
            }
        }

        val secretKey = aesKey.retrieveConfidentialityKey(keystoreAlias)

        val cipher = Cipher.getInstance(getTransformation())

        val spec = generateSpec(injectionVector)

        cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)

        return cipher.doFinal(cipherText).toString(Charset.forName("UTF8"))
    }

    private fun constantTimeEquals(a: ByteArray, b: ByteArray): Boolean {
        if (a.size != b.size) {
            return false
        }
        var result = 0
        for (i in a.indices) {
            result = result or (a[i] xor b[i]).toInt()
        }
        return result == 0
    }
}
