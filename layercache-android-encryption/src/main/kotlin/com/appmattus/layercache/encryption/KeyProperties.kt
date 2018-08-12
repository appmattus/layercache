/*
 * Copyright 2018 Appmattus Limited
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

object KeyProperties {
    val BLOCK_MODE_CBC = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) KeyProperties.BLOCK_MODE_CBC else "CBC"
    val BLOCK_MODE_CTR = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) KeyProperties.BLOCK_MODE_CTR else "CTR"
    val BLOCK_MODE_ECB = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) KeyProperties.BLOCK_MODE_ECB else "ECB"
    val BLOCK_MODE_GCM = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) KeyProperties.BLOCK_MODE_GCM else "GCM"

    val ENCRYPTION_PADDING_NONE = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) KeyProperties.ENCRYPTION_PADDING_NONE else "NoPadding"
    val ENCRYPTION_PADDING_PKCS7 = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) KeyProperties.ENCRYPTION_PADDING_PKCS7 else "PKCS7Padding"
    val ENCRYPTION_PADDING_RSA_OAEP = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) KeyProperties.ENCRYPTION_PADDING_RSA_OAEP else "OAEPPadding"
    val ENCRYPTION_PADDING_RSA_PKCS1 = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) KeyProperties.ENCRYPTION_PADDING_RSA_PKCS1 else "PKCS1Padding"

    val KEY_ALGORITHM_HMAC_SHA256 = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) KeyProperties.KEY_ALGORITHM_HMAC_SHA256 else "HmacSHA256"
    val KEY_ALGORITHM_RSA = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) KeyProperties.KEY_ALGORITHM_RSA else "RSA"
}
