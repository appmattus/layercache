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

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.runBlocking
import org.cache2k.Cache2kBuilder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyString
import java.util.concurrent.TimeUnit

class Cache2kWrapperShould {

    private val cache2k = mock<org.cache2k.Cache<String, String>>()

    private lateinit var wrappedCache: Cache<String, String>

    private lateinit var integratedCache: Cache<String, String>

    private val loaderFetcher = mock<AbstractFetcher<String, String>>()

    private lateinit var integratedCacheWithLoader: Cache<String, String>

    @Before
    fun before() {
        runBlocking {
            wrappedCache = Cache2kWrapper(cache2k)

            val cache2k = object : Cache2kBuilder<String, String>() {}
                // expire/refresh after 5 minutes
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .build()
            integratedCache = Cache.fromCache2k(cache2k)

            whenever(loaderFetcher.get(anyString())).then { "hello" }

            val cache2kWithLoader = object : Cache2kBuilder<String, String>() {}
                .expireAfterWrite(5, TimeUnit.MINUTES) // expire/refresh after 5 minutes
                // exceptions
                .refreshAhead(true) // keep fresh when expiring
                .loader(loaderFetcher.toCache2kLoader()) // auto populating function
                .build()
            integratedCacheWithLoader = Cache.fromCache2k(cache2kWithLoader)
        }
    }

    // get
    @Test
    fun `get returns value from cache`() {
        runBlocking {
            // given value available in first cache only
            whenever(cache2k.get("key")).thenReturn("value")

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
            whenever(cache2k.get("key")).then { throw TestException() }

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
            verify(cache2k).put("key", "value")
        }
    }

    @Test(expected = TestException::class)
    fun `set throws`() {
        runBlocking {
            // given value available in first cache only
            whenever(cache2k.put("key", "value")).then { throw TestException() }

            // when we get the value
            wrappedCache.set("key", "value")

            // then we throw an exception
        }
    }

    // evict
    @Test
    fun `evict returns value from cache`() {
        runBlocking {
            // given

            // when we get the value
            wrappedCache.evict("key")

            // then we return the value
            // assertEquals("value", result)
            verify(cache2k).remove("key")
        }
    }

    @Test(expected = TestException::class)
    fun `evict throws`() {
        runBlocking {
            // given value available in first cache only
            whenever(cache2k.remove("key")).then { throw TestException() }

            // when we get the value
            wrappedCache.evict("key")

            // then we throw an exception
        }
    }

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

    @Test
    fun `return from loader when the cache is empty`() {
        runBlocking {
            // given we have an empty cache
            // integratedCacheWithLoader

            // when we retrieve a value
            val result = integratedCacheWithLoader.get("key")

            // then the value comes from the loader
            assertEquals("hello", result)
        }
    }

    @Test
    fun `return value and not from loader when cache has value`() {
        runBlocking {
            // given we have a cache with a value
            integratedCacheWithLoader.set("key", "value")

            // when we retrieve a value
            val result = integratedCacheWithLoader.get("key")

            // then it is returned
            assertEquals("value", result)
        }
    }

    @Test
    fun `return value from loader when the key has been evicted`() {
        runBlocking {
            // given we have a cache with a value
            integratedCacheWithLoader.set("key", "value")

            // when we evict the value
            integratedCacheWithLoader.evict("key")

            // then the value comes from the loader
            assertEquals("hello", integratedCacheWithLoader.get("key"))
        }
    }
}
