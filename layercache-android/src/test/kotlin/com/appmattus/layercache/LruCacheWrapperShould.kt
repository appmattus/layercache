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

import android.util.LruCache
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations

class LruCacheWrapperShould {

    @Mock
    private lateinit var lruCache: LruCache<String, String>

    private lateinit var wrappedCache: Cache<String, String>

    @Before
    fun before() {
        MockitoAnnotations.initMocks(this)
        wrappedCache = LruCacheWrapper(lruCache)
    }

    // get
    @Test
    fun get_returns_value_from_cache() {
        runBlocking {
            // given value available in first cache only
            Mockito.`when`(lruCache.get("key")).thenReturn("value")

            // when we get the value
            val result = wrappedCache.get("key").await()

            // then we return the value
            Assert.assertEquals("value", result)
        }
    }

    @Test(expected = TestException::class)
    fun get_throws() {
        runBlocking {
            // given value available in first cache only
            Mockito.`when`(lruCache.get("key")).then { throw TestException() }

            // when we get the value
            wrappedCache.get("key").await()

            // then we throw an exception
        }
    }

    // set
    @Test
    fun set_returns_value_from_cache() {
        runBlocking {
            // given

            // when we set the value
            wrappedCache.set("key", "value").await()

            // then put is called
            Mockito.verify(lruCache).put("key", "value")
        }
    }

    @Test(expected = TestException::class)
    fun set_throws() {
        runBlocking {
            // given value available in first cache only
            Mockito.`when`(lruCache.put("key", "value")).then { throw TestException() }

            // when we get the value
            wrappedCache.set("key", "value").await()

            // then we throw an exception
        }
    }

    // evict
    @Test
    fun evict_returns_value_from_cache() {
        runBlocking {
            // given

            // when we get the value
            wrappedCache.evict("key").await()

            // then we return the value
            //assertEquals("value", result)
            Mockito.verify(lruCache).remove("key")
        }
    }

    @Test(expected = TestException::class)
    fun evict_throws() {
        runBlocking {
            // given value available in first cache only
            Mockito.`when`(lruCache.remove("key")).then { throw TestException() }

            // when we get the value
            wrappedCache.evict("key").await()

            // then we throw an exception
        }
    }
}
