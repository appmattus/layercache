/*
 * Copyright 2020 Appmattus Limited
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

package com.appmattus.layercache

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import com.appmattus.layercache.encryption.EncryptionFactory

/**
 * Two-way transform to encrypt and decrypt values stored in a cache
 */
@RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
internal class StringEncryption(
    context: Context,
    private val encryptionMode: EncryptionMode,
    keystoreAlias: String
) : TwoWayTransform<String, String> {

    private val encryption = EncryptionFactory.get(context, encryptionMode, keystoreAlias)

    /**
     * Decrypt the value or return null on error
     */
    override fun transform(value: String): String = encryption.decrypt(value)

    /**
     * Encrypt the value or return null on error
     */
    override fun inverseTransform(mappedValue: String): String = encryption.encrypt(mappedValue)

    /**
     * Output the encryption mode
     */
    override fun toString(): String {
        return encryptionMode.toString()
    }
}

/**
 * Two-way transform using the [encryptionMode] to encrypt and decrypt [String] values stored in the cache
 */
@RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
fun <Key : Any> Cache<Key, String>.encryptValues(context: Context, encryptionMode: EncryptionMode, keystoreAlias: String) =
    valueTransform(StringEncryption(context, encryptionMode, keystoreAlias))
