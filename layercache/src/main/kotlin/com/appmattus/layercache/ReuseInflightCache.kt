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

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

@Suppress("UnnecessaryAbstractClass", "ExceptionRaisedInUnexpectedLocation") // incorrectly reported
internal abstract class ReuseInflightCache<Key : Any, Value : Any>(private val cache: Cache<Key, Value>) : ComposedCache<Key, Value>() {

    final override val parents: List<Cache<*, *>>
        get() = listOf(cache)

    init {
        if (cache is ReuseInflightCache) {
            throw IllegalStateException("Do not directly chain reuseInflight")
        }
    }

    val map = mutableMapOf<Key, Deferred<Value?>>()

    final override suspend fun get(key: Key): Value? = coroutineScope {
        (map[key] ?: async(Dispatchers.IO) { cache.get(key) }.apply {
            map[key] = this
        }).await().also {
            @Suppress("DeferredResultUnused")
            map.remove(key)
        }
    }
}
