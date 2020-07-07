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

import android.os.Build
import androidx.annotation.RequiresApi

/**
 * The encryption mode to use
 */
enum class EncryptionMode {
    /**
     * AES GCM encryption, requires Android KitKat API 19
     */
    @RequiresApi(Build.VERSION_CODES.KITKAT)
    AES_GCM_NoPadding,

    /**
     * AES CBC PKCS7Padding with HmacSHA26 encryption
     */
    AES_CBC_PKCS7Padding_with_HMAC
}
