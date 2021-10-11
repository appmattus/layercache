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

import kotlinx.coroutines.runBlocking
import org.ehcache.config.builders.CacheConfigurationBuilder
import org.ehcache.config.builders.CacheManagerBuilder
import org.ehcache.config.builders.ResourcePoolsBuilder
import org.ehcache.core.Ehcache
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class EhcacheWrapperShould {

    private val ehcache = mock<Ehcache<String, String>>()

    private lateinit var wrappedCache: Cache<String, String>

    private lateinit var integratedCache: Cache<String, String>

    @Before
    fun before() {
        wrappedCache = EhcacheWrapper(ehcache)

        val cacheManager = CacheManagerBuilder.newCacheManagerBuilder().build(true)
        integratedCache = Cache.fromEhcache(
            cacheManager.createCache(
                "myCache",
                CacheConfigurationBuilder.newCacheConfigurationBuilder(String::class.java, String::class.java, ResourcePoolsBuilder.heap(10))
            )
        )
    }

    // get
    @Test
    fun `get returns value from cache`() {
        runBlocking {
            // given value available in first cache only
            whenever(ehcache.get("key")).thenReturn("value")

            // when we get the value
            val result = wrappedCache.get("key")

            // then we return the value
            assertEquals("value", result)
        }
    }

    @Test(expected = TestException::class)
    fun `get throws`() {
        runBlocking {
            // given value available in first cache only
            whenever(ehcache.get("key")).then { throw TestException() }

            // when we get the value
            wrappedCache.get("key")

            // then we throw an exception
        }
    }

    // set
    @Test
    fun `set returns value from cache`() {
        runBlocking {
            // given

            // when we set the value
            wrappedCache.set("key", "value")

            // then put is called
            verify(ehcache).put("key", "value")
        }
    }

    @Test(expected = TestException::class)
    fun `set throws`() {
        runBlocking {
            // given value available in first cache only
            whenever(ehcache.put("key", "value")).then { throw TestException() }

            // when we set the value
            wrappedCache.set("key", "value")

            // then we throw an exception
        }
    }

    // evict
    @Test
    fun `evict returns value from cache`() {
        runBlocking {
            // given

            // when we evict the value
            wrappedCache.evict("key")

            // then remove is called
            verify(ehcache).remove("key")
        }
    }

    @Test(expected = TestException::class)
    fun `evict throws`() {
        runBlocking {
            // given value available in first cache only
            whenever(ehcache.remove("key")).then { throw TestException() }

            // when we evict the value
            wrappedCache.evict("key")

            // then we throw an exception
        }
    }

    // evictAll
    @Test
    fun `evictAll returns value from cache`() {
        runBlocking {
            // given

            // when we evictAll values
            wrappedCache.evictAll()

            // then clear is called
            verify(ehcache).clear()
        }
    }

    @Test(expected = TestException::class)
    fun `evictAll throws`() {
        runBlocking {
            // given value available in first cache only
            whenever(ehcache.clear()).then { throw TestException() }

            // when we evictAll values
            wrappedCache.evictAll()

            // then we throw an exception
        }
    }

    // misc
    @Test
    fun `return null when the cache is empty`() {
        runBlocking {
            // given we have an empty cache
            // integratedCache

            // when we retrieve a value
            val result = integratedCache.get("key")

            // then it is null
            assertNull(result)
        }
    }

    @Test
    fun `return value when cache has value`() {
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
    fun `return null when the key has been evicted`() {
        runBlocking {
            // given we have a cache with a value
            integratedCache.set("key", "value")

            // when we evict the value
            integratedCache.evict("key")

            // then the value is null
            assertNull(integratedCache.get("key"))
        }
    }
}
