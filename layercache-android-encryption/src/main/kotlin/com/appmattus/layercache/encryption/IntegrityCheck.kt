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

import android.security.keystore.KeyProperties

@Suppress("UseDataClass", "MagicNumber") // incorrectly reported
internal enum class IntegrityCheck(val required: Boolean, val algorithm: String, val bits: Int) {
    NONE(false, "", 0),
    HMAC_SHA256(true, KeyProperties.KEY_ALGORITHM_HMAC_SHA256, 256);
}
