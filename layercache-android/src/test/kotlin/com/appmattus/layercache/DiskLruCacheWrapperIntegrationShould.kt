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

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jakewharton.disklrucache.DiskLruCache
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.io.File

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class DiskLruCacheWrapperIntegrationShould {

    private lateinit var diskCache: DiskLruCache
    private lateinit var integratedCache: Cache<String, String>

    @Before
    fun before() {
        diskCache = DiskLruCache.open(File(ApplicationProvider.getApplicationContext<Context>().cacheDir, "disk"), 0, 1, 200)
        integratedCache = Cache.fromDiskLruCache(diskCache)
    }

    @After
    fun after() {
        diskCache.delete()
    }

    @Test
    fun return_null_when_the_cache_is_empty() {
        runBlocking {
            // given we have an empty cache, integratedCache

            // when we retrieve a value
            val result = integratedCache.get("key")

            // then it is null
            assertNull(result)
        }
    }

    @Test
    fun return_value_when_cache_has_value() {
        runBlocking {
            // given we have a cache with a value
            integratedCache.set("key", "value")

            // when we retrieve a value
            val result = integratedCache.get("key")

            // then it is returned
            assertEquals("value", result)
        }
    }

    @Test
    fun return_null_when_the_key_has_been_evicted() {
        runBlocking {
            // given we have a cache with a value
            integratedCache.set("key", "value")

            // when we evict the value
            integratedCache.evict("key")

            // then the value is null
            assertNull(integratedCache.get("key"))
        }
    }

    @Test
    fun remove_first_value_when_all_unused() {
        runBlocking {
            val singleEntryDiskCache = DiskLruCache.open(File(ApplicationProvider.getApplicationContext<Context>().cacheDir, "disk2"), 0, 1, 6)

            try {
                // given we create a cache of size 1
                val cache = DiskLruCacheWrapper(singleEntryDiskCache)

                // when we set 2 values, and force flush the cache
                cache.set("key1", "value1")
                cache.set("key2", "value2")
                singleEntryDiskCache.flush()

                // then only the second value is available
                assertNull(cache.get("key1"))
                assertEquals("value2", cache.get("key2"))
            } finally {
                singleEntryDiskCache.delete()
            }
        }
    }

    @Test
    fun remove_oldest_value_when_one_is_used() {
        runBlocking {
            val doubleEntryDiskCache = DiskLruCache.open(File(ApplicationProvider.getApplicationContext<Context>().cacheDir, "disk2"), 0, 1, 12)

            try {
                // given we create and populate a cache of size 2
                val cache = DiskLruCacheWrapper(doubleEntryDiskCache)
                cache.set("key1", "value1")
                cache.set("key2", "value2")

                // when we get the 1st and add a 3rd value, and force flush the cache
                cache.get("key1")
                cache.set("key3", "value3")
                doubleEntryDiskCache.flush()

                // then the 2nd is removed leaving the 1st and 3rd values
                assertNull(cache.get("key2"))
                assertEquals("value1", cache.get("key1"))
                assertEquals("value3", cache.get("key3"))
            } finally {
                doubleEntryDiskCache.delete()
            }
        }
    }
}
