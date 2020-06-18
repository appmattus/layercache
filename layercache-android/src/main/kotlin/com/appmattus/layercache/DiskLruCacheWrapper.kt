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

import com.jakewharton.disklrucache.DiskLruCache
import java.io.File

/**
 * Wrapper around DiskLruCache (https://github.com/jakeWharton/DiskLruCache/)
 */
internal class DiskLruCacheWrapper(private val cache: DiskLruCache) : Cache<String, String> {

    override suspend fun evict(key: String) {
        cache.remove(key)
    }

    override suspend fun get(key: String): String? {
        return cache.get(key)?.getString(0)
    }

    override suspend fun set(key: String, value: String) {
        val editor = cache.edit(key)
        editor.set(0, value)
        editor.commit()
    }

    override suspend fun evictAll() {
        // Although setting maxSize to zero will cause the cache to be emptied this happens in a separate thread,
        // by calling flush immediately we ensure this happens in the same call
        cache.maxSize.let {
            cache.maxSize = 0
            cache.flush()
            cache.maxSize = it
        }
    }
}

/**
 * Create a Cache from a DiskLruCache
 * @property diskLruCache   A DiskLruCache
 */
@Suppress("unused", "USELESS_CAST")
fun Cache.Companion.fromDiskLruCache(diskLruCache: DiskLruCache) = DiskLruCacheWrapper(diskLruCache) as Cache<String, String>

/**
 * Create a Cache from a newly created DiskLruCache
 * @property directory  Directory to create cache in
 * @property maxSize    Maximum number of bytes
 */
@Suppress("unused", "USELESS_CAST")
fun Cache.Companion.createDiskLruCache(directory: File, maxSize: Long) = fromDiskLruCache(DiskLruCache.open(directory, 1, 1, maxSize))
