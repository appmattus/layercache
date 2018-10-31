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

import android.annotation.TargetApi
import android.os.Build
import android.util.LruCache
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async

/**
 * Wrapper around Android's built in LruCache
 */
@TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
internal class LruCacheWrapper<Key : Any, Value : Any>(private val cache: LruCache<Key, Value>) : Cache<Key, Value> {
    constructor(maxSize: Int) : this(LruCache(maxSize))

    override fun evict(key: Key): Deferred<Unit> {
        return GlobalScope.async<Unit> {
            cache.remove(key)
        }
    }

    override fun get(key: Key): Deferred<Value?> {
        return GlobalScope.async {
            cache.get(key)
        }
    }

    override fun set(key: Key, value: Value): Deferred<Unit> {
        return GlobalScope.async<Unit> {
            cache.put(key, value)
        }
    }

    override fun evictAll(): Deferred<Unit> {
        return GlobalScope.async {
            cache.evictAll()
        }
    }
}

/**
 * Create a Cache from Android's built in LruCache
 * @property lruCache   An LruCache
 */
@Suppress("unused", "USELESS_CAST")
@TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
fun <Key : Any, Value : Any> Cache.Companion.fromLruCache(lruCache: LruCache<Key, Value>) = LruCacheWrapper(lruCache) as Cache<Key, Value>

/**
 * Create a Cache from a newly created LruCache
 * @property maxSize    Maximum number of entries
 */
@Suppress("unused")
fun <Key : Any, Value : Any> Cache.Companion.createLruCache(maxSize: Int) = Cache.fromLruCache(LruCache<Key, Value>(maxSize))
