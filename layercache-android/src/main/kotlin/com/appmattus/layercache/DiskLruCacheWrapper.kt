/**
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

import com.jakewharton.disklrucache.DiskLruCache
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.async

/**
 * Wrapper around DiskLruCache (https://github.com/jakeWharton/DiskLruCache/)
 */
internal class DiskLruCacheWrapper(private val cache: DiskLruCache) : Cache<String, String> {

    override fun evict(key: String): Deferred<Unit> {
        return async<Unit>(CommonPool) {
            cache.remove(key)
        }
    }

    override fun get(key: String): Deferred<String?> {
        return async(CommonPool) {
            cache.get(key)?.getString(0)
        }
    }

    override fun set(key: String, value: String): Deferred<Unit> {
        return async(CommonPool) {
            val editor = cache.edit(key)
            editor.set(0, value)
            editor.commit()
        }
    }

    override fun evictAll(): Deferred<Unit> {
        return async<Unit>(CommonPool) {
            // Although setting maxSize to zero will cause the cache to be emptied this happens in a separate thread,
            // by calling flush immediately we ensure this happens in the same call
            cache.maxSize.let {
                cache.maxSize = 0
                cache.flush()
                cache.maxSize = it
            }
        }
    }
}

@Suppress("unused", "USELESS_CAST")
fun Cache.Companion.fromDiskLruCache(cache: DiskLruCache) = DiskLruCacheWrapper(cache) as Cache<String, String>
