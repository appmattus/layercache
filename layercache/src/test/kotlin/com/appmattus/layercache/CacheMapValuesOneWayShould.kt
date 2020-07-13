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
import com.nhaarman.mockitokotlin2.atLeastOnce
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.verifyNoInteractions

class CacheMapValuesOneWayShould {

    private val cache = mock<AbstractCache<Any, String>> {
        on { valueTransform(any<(String) -> Int>()) }.thenCallRealMethod()
    }

    private val function = mock<(String) -> Int>()

    private val functionInverse = mock<(Int) -> String>()

    private val mappedValuesCache: Fetcher<Any, Int> = cache.valueTransform(function).also {
        verify(cache, atLeastOnce()).valueTransform(any<(String) -> Any>())
    }

    // get
    @Test
    fun `only invoke function and not inverse function`() {
        runBlocking {
            // given we have a cache that returns a string
            whenever(cache.get("key")).then { "1" }
            whenever(function.invoke(anyString())).then { it.getArgument<String>(0).toInt() }

            // when we get the value
            mappedValuesCache.get("key")

            // then the main function is invoked but the inverse is not
            verify(function).invoke("1")
            verifyNoInteractions(functionInverse)
        }
    }

    @Test
    fun `map string value in get to int`() {
        runBlocking {
            // given we have a cache that returns a string
            whenever(cache.get("key")).then { "1" }
            whenever(function.invoke(anyString())).then { it.getArgument<String>(0).toInt() }

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
            // given we have a string and transform throws an exception
            whenever(cache.get("key")).then { "1" }
            whenever(function.invoke(anyString())).then { throw TestException() }

            // when we get the value from a map with exception throwing functions
            mappedValuesCache.get("key")

            // then an exception is thrown
        }
    }

    @Test(expected = TestException::class)
    fun `throw exception when mapping in get`() {
        runBlocking {
            // given we throw an exception on get
            whenever(cache.get("key")).then { throw TestException() }

            // when we get the value from a map
            mappedValuesCache.get("key")

            // then an exception is thrown
        }
    }

    // set
    @Test
    fun `not interact with parent set`() {
        runBlocking {
            // when we set the value
            @Suppress("DEPRECATION_ERROR")
            mappedValuesCache.set("1", 1)

            // then the parent cache is not called
            verifyNoMoreInteractions(cache)
        }
    }

    @Test
    fun `not interact with transform during set`() {
        runBlocking {
            // when we set the value
            @Suppress("DEPRECATION_ERROR")
            mappedValuesCache.set("1", 1)

            // then the parent cache is not called
            verifyNoInteractions(function)
        }
    }

    // evict
    @Test
    fun `not interact with parent evict`() {
        runBlocking {
            // when we set the value
            @Suppress("DEPRECATION_ERROR")
            mappedValuesCache.evict("1")

            // then the parent cache is not called
            verifyNoMoreInteractions(cache)
        }
    }

    @Test
    fun `not interact with transform during evict`() {
        runBlocking {
            // when we set the value
            @Suppress("DEPRECATION_ERROR")
            mappedValuesCache.evict("1")

            // then the parent cache is not called
            verifyNoInteractions(function)
        }
    }

    // evictAll
    @Test
    fun `not interact with parent evictAll`() {
        runBlocking {
            // when we set the value
            @Suppress("DEPRECATION_ERROR")
            mappedValuesCache.evictAll()

            // then the parent cache is not called
            verifyNoMoreInteractions(cache)
        }
    }

    @Test
    fun `not interact with transform during evictAll`() {
        runBlocking {
            // when we set the value
            @Suppress("DEPRECATION_ERROR")
            mappedValuesCache.evictAll()

            // then the parent cache is not called
            verifyNoInteractions(function)
        }
    }
}
