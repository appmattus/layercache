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

package com.appmattus.layercache

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async

/**
 * Wrapper around EhCache (http://www.ehcache.org/)
 * @property cache  Ehcache cache
 */
internal class EhcacheWrapper<Key : Any, Value : Any>(private val cache: org.ehcache.Cache<Key, Value>) : Cache<Key, Value> {

    override fun evict(key: Key): Deferred<Unit> {
        return GlobalScope.async {
            cache.remove(key)
        }
    }

    override suspend fun get(key: Key): Value? {
        return cache.get(key)
    }

    override fun set(key: Key, value: Value): Deferred<Unit> {
        return GlobalScope.async {
            cache.put(key, value)
        }
    }

    override fun evictAll(): Deferred<Unit> {
        return GlobalScope.async {
            cache.clear()
        }
    }
}

/**
 * Wrapper around EhCache (http://www.ehcache.org/)
 * @property cache  Ehcache cache
 * @return Cache
 */
@Suppress("unused", "USELESS_CAST")
fun <Key : Any, Value : Any> Cache.Companion.fromEhcache(cache: org.ehcache.Cache<Key, Value>) = EhcacheWrapper(cache) as Cache<Key, Value>
