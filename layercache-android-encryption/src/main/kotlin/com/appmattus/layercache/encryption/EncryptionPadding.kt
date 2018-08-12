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

import android.os.Build
import android.security.keystore.KeyProperties

internal enum class EncryptionPadding(val padding: String) {
    NONE(if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) KeyProperties.ENCRYPTION_PADDING_NONE else "NoPadding"),
    PKCS7(if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) KeyProperties.ENCRYPTION_PADDING_PKCS7 else "PKCS7Padding"),
    RSA_OAEP(if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) KeyProperties.ENCRYPTION_PADDING_RSA_OAEP else "OAEPPadding"),
    RSA_PKCS1(if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) KeyProperties.ENCRYPTION_PADDING_RSA_PKCS1 else "PKCS1Padding");

    override fun toString() = padding
}
