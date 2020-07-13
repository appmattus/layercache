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

open class TestCache<Key : Any, Value : Any>(private val cacheName: String = "") : Cache<Key, Value> {
    var getFn: suspend (key: Key) -> Value? = { null }
    var setFn: suspend (key: Key, value: Value) -> Unit = { _, _ -> }
    var evictFn: suspend (key: Key) -> Unit = { }
    var evictAllFn: suspend () -> Unit = { }

    override suspend fun get(key: Key): Value? = getFn(key)
    override suspend fun set(key: Key, value: Value) = setFn(key, value)
    override suspend fun evict(key: Key) = evictFn(key)
    override suspend fun evictAll() = evictAllFn()

    override fun toString() = cacheName
}
