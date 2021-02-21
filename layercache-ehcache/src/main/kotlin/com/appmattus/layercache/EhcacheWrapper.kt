/*
 * Copyright 2021 Appmattus Limited
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

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Wrapper around EhCache (http://www.ehcache.org/)
 * @property cache Ehcache cache
 */
internal class EhcacheWrapper<Key : Any, Value : Any>(private val cache: org.ehcache.Cache<Key, Value>) : Cache<Key, Value> {

    override suspend fun evict(key: Key) = withContext(Dispatchers.IO) { cache.remove(key) }

    override suspend fun get(key: Key): Value? = withContext(Dispatchers.IO) { cache.get(key) }

    override suspend fun set(key: Key, value: Value) = withContext(Dispatchers.IO) { cache.put(key, value) }

    override suspend fun evictAll() = withContext(Dispatchers.IO) { cache.clear() }
}

/**
 * Wrapper around EhCache (http://www.ehcache.org/)
 * @property cache Ehcache cache
 * @return Cache
 */
@Suppress("unused", "USELESS_CAST")
public fun <Key : Any, Value : Any> Cache.Companion.fromEhcache(cache: org.ehcache.Cache<Key, Value>): Cache<Key, Value> = EhcacheWrapper(cache)
