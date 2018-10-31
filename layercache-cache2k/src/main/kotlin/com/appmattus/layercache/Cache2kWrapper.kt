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
import kotlinx.coroutines.runBlocking
import org.cache2k.integration.FunctionalCacheLoader

/**
 * Wrapper around Cache2k (https://cache2k.org/)
 * @property cache  Cache2k cache
 */
internal class Cache2kWrapper<Key : Any, Value : Any>(private val cache: org.cache2k.Cache<Key, Value>) :
        Cache<Key, Value> {
    override fun evict(key: Key): Deferred<Unit> {
        return GlobalScope.async {
            cache.remove(key)
        }
    }

    override fun get(key: Key): Deferred<Value?> {
        return GlobalScope.async {
            cache.get(key)
        }
    }

    override fun set(key: Key, value: Value): Deferred<Unit> {
        return GlobalScope.async {
            cache.put(key, value)
        }
    }

    override fun evictAll(): Deferred<Unit> {
        return GlobalScope.async {
            cache.clear()

            //FunctionalCacheLoader
        }
    }
}

/**
 * Wrapper around Cache2k (https://cache2k.org/)
 * @property cache  Cache2k cache
 * @return Cache
 */
@Suppress("unused", "USELESS_CAST")
fun <Key : Any, Value : Any> Cache.Companion.fromCache2k(cache: org.cache2k.Cache<Key, Value>) = Cache2kWrapper(cache) as Cache<Key, Value>

/**
 * Convert a Fetcher into a Cache2k loader. Note the Fetcher should not return null
 * @return Cache2k loader
 */
@Suppress("unused")
fun <Key : Any, Value : Any> Fetcher<Key, Value>.toCache2kLoader(): FunctionalCacheLoader<Key, Value> {
    return FunctionalCacheLoader { key ->
        // TODO What thread does a cache loader run on?
        runBlocking { get(key).await() }
    }
}
