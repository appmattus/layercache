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

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Answers
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.anyString
import org.mockito.MockitoAnnotations

class CacheMapValuesShould {

    @Mock
    private lateinit var cache: AbstractCache<Any, String>

    private lateinit var mappedValuesCache: Cache<Any, Int>
    private lateinit var mappedValuesCacheWithError: Cache<Any, Int>

    @Before
    fun before() {
        MockitoAnnotations.initMocks(this)
        val f: (String) -> Int = { str: String -> str.toInt() }
        val fInv: (Int) -> String = { int: Int -> int.toString() }
        Mockito.`when`(cache.valueTransform(MockitoKotlin.any(f::class.java), MockitoKotlin.any(fInv::class.java))).thenCallRealMethod()
        mappedValuesCache = cache.valueTransform(f, fInv)

        val errorF: (String) -> Int = { _: String -> throw TestException() }
        val errorFInv: (Int) -> String = { _: Int -> throw TestException() }
        Mockito.`when`(cache.valueTransform(MockitoKotlin.any(errorF::class.java), MockitoKotlin.any(errorFInv::class.java)))
            .thenCallRealMethod()
        mappedValuesCacheWithError = cache.valueTransform(errorF, errorFInv)
    }

    // get
    @Test
    fun `map string value in get to int`() {
        runBlocking {
            // given we have a string
            Mockito.`when`(cache.get("key")).then { "1" }

            // when we get the value
            val result = mappedValuesCache.get("key")

            // then it is converted to an integer
            assertEquals(1, result)
            assertTrue(result is Int)
        }
    }

    @Test(expected = TestException::class)
    fun `throw exception when mapping in function`() {
        runBlocking {
            // given we have a string
            Mockito.`when`(cache.get("key")).then { "1" }

            // when we get the value from a map with exception throwing functions
            mappedValuesCacheWithError.get("key")

            // then an exception is thrown
        }
    }

    @Test(expected = TestException::class)
    fun `throw exception when mapping in get`() {
        runBlocking {
            // given we have a string
            Mockito.`when`(cache.get("key")).then { throw TestException() }

            // when we get the value from a map
            mappedValuesCache.get("key")

            // then an exception is thrown
        }
    }

    // set
    @Test
    fun `map int value in set to string`() {
        runBlocking {
            // given we have a string
            Mockito.`when`(cache.set(anyString(), anyString())).then(Answers.RETURNS_MOCKS)

            // when we set the value
            mappedValuesCache.set("key", 1)

            // then it is converted to a string
            Mockito.verify(cache).set("key", "1")
        }
    }

    @Test(expected = TestException::class)
    fun `throw exception when mapping in function set`() {
        runBlocking {
            // given we have a string
            Mockito.`when`(cache.set(anyString(), anyString())).then(Answers.RETURNS_MOCKS)

            // when we get the value from a map with exception throwing functions
            mappedValuesCacheWithError.set("key", 1)

            // then an exception is thrown
        }
    }

    @Test(expected = TestException::class)
    fun `throw exception when mapping in set`() {
        runBlocking {
            // given we have a string
            Mockito.`when`(cache.set(anyString(), anyString())).then { throw TestException() }

            // when we get the value from a map
            mappedValuesCache.set("key", 1)

            // then an exception is thrown
        }
    }

    // evict
    @Test
    fun `call evict from cache`() {
        runBlocking {
            // given value available in first cache only
            Mockito.`when`(cache.evict("key")).then { GlobalScope.async {} }

            // when we get the value
            mappedValuesCache.evict("key").await()

            // then we return the value
            //Assert.assertEquals("value", result)
            Mockito.verify(cache).evict("key")
        }
    }

    @Test(expected = TestException::class)
    fun `propagate exception on evict`() {
        runBlocking {
            // given value available in first cache only
            Mockito.`when`(cache.evict("key")).then { GlobalScope.async { throw TestException() } }

            // when we get the value
            mappedValuesCache.evict("key").await()

            // then we throw an exception
        }
    }

    // evictAll
    @Test
    fun `call evictAll from cache`() {
        runBlocking {
            // given evictAll is implemented
            Mockito.`when`(cache.evictAll()).then { GlobalScope.async {} }

            // when we evictAll values
            mappedValuesCache.evictAll().await()

            // then evictAll is called
            Mockito.verify(cache).evictAll()
        }
    }

    @Test(expected = TestException::class)
    fun `propagate exception on evictAll`() {
        runBlocking {
            // given evictAll throws an exception
            Mockito.`when`(cache.evictAll()).then { GlobalScope.async { throw TestException() } }

            // when we evictAll values
            mappedValuesCache.evictAll().await()

            // then we throw an exception
        }
    }
}
