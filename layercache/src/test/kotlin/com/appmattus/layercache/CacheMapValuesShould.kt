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

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Answers
import org.mockito.ArgumentMatchers.anyString

class CacheMapValuesShould {

    private val cache = mock<AbstractCache<Any, String>> {
        @Suppress("RemoveExplicitTypeArguments")
        on { valueTransform(any<(String) -> Int>(), any<(Int) -> String>()) }.thenCallRealMethod()
    }

    private val mappedValuesCache: Cache<Any, Int> = cache.valueTransform({ str: String -> str.toInt() }, { int: Int -> int.toString() })

    @Suppress("RedundantLambdaArrow")
    private val mappedValuesCacheWithError: Cache<Any, Int> =
        cache.valueTransform({ _: String -> throw TestException() }, { _: Int -> throw TestException() })

    // get
    @Test
    fun `map string value in get to int`() {
        runBlocking {
            // given we have a string
            whenever(cache.get("key")).then { "1" }

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
            whenever(cache.get("key")).then { "1" }

            // when we get the value from a map with exception throwing functions
            mappedValuesCacheWithError.get("key")

            // then an exception is thrown
        }
    }

    @Test(expected = TestException::class)
    fun `throw exception when mapping in get`() {
        runBlocking {
            // given we have a string
            whenever(cache.get("key")).then { throw TestException() }

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
            whenever(cache.set(anyString(), anyString())).then(Answers.RETURNS_MOCKS)

            // when we set the value
            mappedValuesCache.set("key", 1)

            // then it is converted to a string
            verify(cache).set("key", "1")
        }
    }

    @Test(expected = TestException::class)
    fun `throw exception when mapping in function set`() {
        runBlocking {
            // given we have a string
            whenever(cache.set(anyString(), anyString())).then(Answers.RETURNS_MOCKS)

            // when we get the value from a map with exception throwing functions
            mappedValuesCacheWithError.set("key", 1)

            // then an exception is thrown
        }
    }

    @Test(expected = TestException::class)
    fun `throw exception when mapping in set`() {
        runBlocking {
            // given we have a string
            whenever(cache.set(anyString(), anyString())).then { throw TestException() }

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
            whenever(cache.evict("key")).then { Unit }

            // when we get the value
            mappedValuesCache.evict("key")

            // then we return the value
            // Assert.assertEquals("value", result)
            verify(cache).evict("key")
        }
    }

    @Test(expected = TestException::class)
    fun `propagate exception on evict`() {
        runBlocking {
            // given value available in first cache only
            whenever(cache.evict("key")).then { throw TestException() }

            // when we get the value
            mappedValuesCache.evict("key")

            // then we throw an exception
        }
    }

    // evictAll
    @Test
    fun `call evictAll from cache`() {
        runBlocking {
            // given evictAll is implemented
            whenever(cache.evictAll()).then { Unit }

            // when we evictAll values
            mappedValuesCache.evictAll()

            // then evictAll is called
            verify(cache).evictAll()
        }
    }

    @Test(expected = TestException::class)
    fun `propagate exception on evictAll`() {
        runBlocking {
            // given evictAll throws an exception
            whenever(cache.evictAll()).then { throw TestException() }

            // when we evictAll values
            mappedValuesCache.evictAll()

            // then we throw an exception
        }
    }
}
