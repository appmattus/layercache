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

import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class LruCacheWrapperIntegrationShould {

    private lateinit var integratedCache: Cache<String, String>

    @Before
    fun before() {
        integratedCache = Cache.createLruCache(10)
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
            integratedCache.set("key", "value").await()

            // when we retrieve a value
            val result = integratedCache.get("key")

            // then it is returned
            Assert.assertEquals("value", result)
        }
    }

    @Test
    fun return_null_when_the_key_has_been_evicted() {
        runBlocking {
            // given we have a cache with a value
            integratedCache.set("key", "value").await()

            // when we evict the value
            integratedCache.evict("key").await()

            // then the value is null
            assertNull(integratedCache.get("key"))
        }
    }

    @Test
    fun remove_first_value_when_all_unused() {
        runBlocking {
            // given we create a cache of size 1
            val cache = LruCacheWrapper<String, String>(1)

            // when we set 2 values
            cache.set("key1", "value1").await()
            cache.set("key2", "value2").await()

            // then only the second value is available
            assertNull(cache.get("key1"))
            assertEquals("value2", cache.get("key2"))
        }
    }

    @Test
    fun remove_oldest_value_when_one_is_used() {
        runBlocking {
            // given we create and populate a cache of size 2
            val cache = LruCacheWrapper<String, String>(2)
            cache.set("key1", "value1").await()
            cache.set("key2", "value2").await()

            // when we get the 1st and add a 3rd value
            cache.get("key1")
            cache.set("key3", "value3").await()

            // then the 2nd is removed leaving the 1st and 3rd values
            assertNull(cache.get("key2"))
            assertEquals("value1", cache.get("key1"))
            assertEquals("value3", cache.get("key3"))
        }
    }
}
