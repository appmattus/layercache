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

open class TestCache(private val cacheName: String = "") : Cache<String, String> {
    var getFn: suspend (key: String) -> String? = { null }
    var setFn: suspend (key: String, value: String) -> Unit = { _, _ -> }
    var evictFn: suspend () -> Unit = { }
    var evictAllFn: suspend () -> Unit = { }

    override suspend fun get(key: String): String? = getFn(key)
    override suspend fun set(key: String, value: String) = setFn(key, value)
    override suspend fun evict(key: String) = evictFn()
    override suspend fun evictAll() = evictAllFn()

    override fun toString() = cacheName
}
