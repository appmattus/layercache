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

import com.appmattus.layercache.encryption.KeyProperties.BLOCK_MODE_CBC
import com.appmattus.layercache.encryption.KeyProperties.BLOCK_MODE_CTR
import com.appmattus.layercache.encryption.KeyProperties.BLOCK_MODE_ECB
import com.appmattus.layercache.encryption.KeyProperties.BLOCK_MODE_GCM

internal enum class BlockMode(val mode: String) {
    CBC(BLOCK_MODE_CBC),
    CTR(BLOCK_MODE_CTR),
    ECB(BLOCK_MODE_ECB),
    GCM(BLOCK_MODE_GCM);

    override fun toString() = mode
}
