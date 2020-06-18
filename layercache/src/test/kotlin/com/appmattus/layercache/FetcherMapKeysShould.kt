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

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.atLeastOnce
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.core.IsEqual
import org.hamcrest.core.StringStartsWith
import org.junit.Assert
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.Mockito.verifyNoInteractions

class FetcherMapKeysShould {

    private val cache = mock<AbstractFetcher<String, Any>> {
        on { keyTransform(any<(Int) -> String>()) }.thenCallRealMethod()
    }
    private val function = mock<(Int) -> String>()

    private lateinit var mappedKeysCache: Fetcher<Int, Any>

    @Before
    fun before() {
        mappedKeysCache = cache.keyTransform(function)

        verify(cache, atLeastOnce()).keyTransform(any<(Int) -> String>())
    }

    @Test
    fun `contain cache in composed parents`() {
        val localCache = mappedKeysCache
        if (localCache !is ComposedCache<*, *>) {
            Assert.fail()
            return
        }

        assertThat(localCache.parents, IsEqual.equalTo(listOf<Cache<*, *>>(cache)))
    }

    // get
    @Test
    fun `map string value in get to int`() {
        runBlocking {
            // given we have a string
            transformConvertsIntToString()
            whenever(cache.get("1")).then { "value" }

            // when we get the value
            val result = mappedKeysCache.get(1)

            Assert.assertEquals("value", result)
        }
    }

    @Test
    fun `throw exception when transform returns null during get`() {
        runBlocking {
            // given transform returns null
            whenever(function.invoke(anyInt())).then { TestUtils.uninitialized() }

            // when the mapping function returns null
            val throwable = assertThrows(IllegalArgumentException::class.java) {
                runBlocking {
                    mappedKeysCache.get(1)
                }
            }

            // expect exception
            assertThat(throwable.message, StringStartsWith("Required value was null"))
        }
    }

    @Test(expected = TestException::class)
    fun `throw exception when transform throws during get`() {
        runBlocking {
            // given we have a string
            whenever(function.invoke(anyInt())).then { throw TestException() }

            // whenever(cache.get("1")).then { async(CommonPool) { "value" } }

            // when we get the value from a map with exception throwing functions
            mappedKeysCache.get(1)

            // then an exception is thrown
        }
    }

    @Test(expected = TestException::class)
    fun `throw exception when get throws`() {
        runBlocking {
            // given we have a string
            transformConvertsIntToString()
            whenever(cache.get("1")).then { throw TestException() }

            // when we get the value from a map
            mappedKeysCache.get(1)

            // then an exception is thrown
        }
    }

    @Test(expected = CancellationException::class)
    fun `throw exception when cancelled during get`() {
        runBlocking {
            // given we have a long running job
            transformConvertsIntToString()
            whenever(cache.get("1")).then { runBlocking { delay(250) } }

            // when we cancel the job
            val job = async { mappedKeysCache.get(1) }
            delay(50)
            job.cancel()

            // then an exception is thrown
            job.await()
        }
    }

    // set
    @Test
    fun `not interact with parent set`() {
        runBlocking {
            // when we set the value
            @Suppress("DEPRECATION")
            mappedKeysCache.set(1, "1")

            // then the parent cache is not called
            verifyNoMoreInteractions(cache)
        }
    }

    @Test
    fun `not interact with transform during set`() {
        runBlocking {
            // when we set the value
            @Suppress("DEPRECATION")
            mappedKeysCache.set(1, "1")

            // then the parent cache is not called
            verifyNoInteractions(function)
        }
    }

    // evict
    @Test
    fun `not interact with parent evict`() {
        runBlocking {
            // when we set the value
            @Suppress("DEPRECATION")
            mappedKeysCache.evict(1)

            // then the parent cache is not called
            verifyNoMoreInteractions(cache)
        }
    }

    @Test
    fun `not interact with transform during evict`() {
        runBlocking {
            // when we set the value
            @Suppress("DEPRECATION")
            mappedKeysCache.evict(1)

            // then the parent cache is not called
            verifyNoInteractions(function)
        }
    }

    // evictAll
    @Test
    fun `not interact with parent evictAll`() {
        runBlocking {
            // when we evictAll values
            @Suppress("DEPRECATION")
            mappedKeysCache.evictAll()

            // then the parent cache is not called
            verifyNoMoreInteractions(cache)
        }
    }

    @Test
    fun `not interact with transform during evictAll`() {
        runBlocking {
            // when we evictAll values
            @Suppress("DEPRECATION")
            mappedKeysCache.evictAll()

            // then the parent cache is not called
            verifyNoInteractions(function)
        }
    }

    private fun transformConvertsIntToString() {
        whenever(function.invoke(anyInt())).then { it.getArgument<Int>(0).toString() }
    }
}
