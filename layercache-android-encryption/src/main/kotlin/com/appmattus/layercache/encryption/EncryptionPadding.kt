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

import com.appmattus.layercache.encryption.KeyProperties.ENCRYPTION_PADDING_NONE
import com.appmattus.layercache.encryption.KeyProperties.ENCRYPTION_PADDING_PKCS7
import com.appmattus.layercache.encryption.KeyProperties.ENCRYPTION_PADDING_RSA_OAEP
import com.appmattus.layercache.encryption.KeyProperties.ENCRYPTION_PADDING_RSA_PKCS1

internal enum class EncryptionPadding(val padding: String) {
    NONE(ENCRYPTION_PADDING_NONE),
    PKCS7(ENCRYPTION_PADDING_PKCS7),
    RSA_OAEP(ENCRYPTION_PADDING_RSA_OAEP),
    RSA_PKCS1(ENCRYPTION_PADDING_RSA_PKCS1);

    override fun toString() = padding
}
