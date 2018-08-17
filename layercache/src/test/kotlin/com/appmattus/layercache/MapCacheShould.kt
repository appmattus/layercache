/*
 * Copyright 2018 Appmattus Limited
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

import kotlinx.coroutines.experimental.runBlocking
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class MapCacheShould {

    private lateinit var mapCache: MapCache

    @Before
    fun before() {
        mapCache = MapCache()
    }

    @Test
    fun `return null when the cache is empty`() {
        runBlocking {
            // given we have an empty cache

            // when we retrieve a value
            val result = mapCache.get("key").await()

            // then it is null
            Assert.assertNull(result)
        }
    }

    @Test
    fun `return value when cache has value`() {
        runBlocking {
            // given we have a cache with a value
            mapCache.set("key", "value").await()

            // when we retrieve a value
            val result = mapCache.get("key").await()

            // then it is returned
            Assert.assertEquals("value", result)
        }
    }

    @Test
    fun `return null when the key has been evicted`() {
        runBlocking {
            // given we have a cache with a value
            mapCache.set("key", "value").await()

            // when we evict the value
            mapCache.evict("key").await()

            // then the value is null
            Assert.assertNull(mapCache.get("key").await())
        }
    }

    @Test
    fun `return null when all keys has been evicted`() {
        runBlocking {
            // given we have a cache with a value
            mapCache.set("key", "value").await()

            // when we evict the value
            mapCache.evictAll().await()

            // then the value is null
            Assert.assertNull(mapCache.get("key").await())
        }
    }
}
