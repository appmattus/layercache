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
import android.support.annotation.RequiresApi
import java.security.spec.AlgorithmParameterSpec

@Suppress("UnnecessaryAbstractClass") // incorrectly reported
@RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
internal abstract class EncryptionDefaultIv(context: Context, keystoreAlias: String) :
        EncryptionBase(context, keystoreAlias) {
    override fun providesIv() = false

    override fun generateIv(): AlgorithmParameterSpec = throw IllegalStateException("Should not be called")
}
