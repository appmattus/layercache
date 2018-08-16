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

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi

/**
 * Factory to generate an encryptor
 */
@RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
class EncryptionFactory {
    /**
     * The encryption mode to use
     */
    enum class Mode {
        /**
         * AES GCM encryption
         */
        @RequiresApi(Build.VERSION_CODES.KITKAT)
        AES_GCM_NoPadding,

        /**
         * AES CBC PKCS7Padding with HmacSHA26 encryption
         */
        AES_CBC_PKCS7Padding_with_HMAC
    }

    companion object {
        /**
         * Generate an encryptor for the required encryption mode using the given alias for storage of keys.
         */
        @SuppressLint("NewApi")
        fun <T : Context> get(context: T, mode: Mode, keystoreAlias: String): Encryption {
            PRNGFixes.apply()

            return when (mode) {
                Mode.AES_GCM_NoPadding -> AesGcmNoPadding(context.applicationContext, keystoreAlias)
                Mode.AES_CBC_PKCS7Padding_with_HMAC -> AesCbcPkcs7PaddingWithHmacSha256(context.applicationContext,
                        keystoreAlias)
            }
        }
    }
}
