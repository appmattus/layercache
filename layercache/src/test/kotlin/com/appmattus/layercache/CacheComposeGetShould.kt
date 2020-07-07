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

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.core.StringStartsWith
import org.junit.Assert
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.verifyNoInteractions

class CacheComposeGetShould {

    @get:Rule
    var executions = ExecutionExpectation()

    private val firstCache = mock<AbstractCache<String, String>>()
    private val secondCache = mock<AbstractCache<String, String>>()

    private lateinit var composedCache: Cache<String, String>

    @Before
    fun before() {
        whenever(firstCache.compose(secondCache)).thenCallRealMethod()
        composedCache = firstCache.compose(secondCache)
        verify(firstCache).compose(secondCache)
    }

    @Test
    fun `throw exception when key is null`() {
        // when key is null
        val throwable = assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                composedCache.get(TestUtils.uninitialized())
            }
        }

        // expect exception
        assertThat(throwable.message, StringStartsWith("Required value was null"))
    }

    @Test
    fun `return value from first cache when available in first cache`() {
        runBlocking {
            // given value available in first cache only
            whenever(firstCache.get("key")).then { "value" }

            // when we get the value
            val result = composedCache.get("key")

            // then we return the value
            Assert.assertEquals("value", result)
        }
    }

    @Test
    fun `not call get on second cache when value available in first cache`() {
        runBlocking {
            // given value available in first cache only
            whenever(firstCache.get("key")).then { "value" }

            // when we get the value
            composedCache.get("key")

            // then we do not call the second cache
            verifyNoInteractions(secondCache)
        }
    }

    @Test
    fun `return value from second cache when only available in second cache and set on first cache`() {
        runBlocking {
            // given value available in second cache only
            whenever(firstCache.get("key")).then { null }
            whenever(secondCache.get("key")).then { "value" }
            whenever(firstCache.set(anyString(), anyString())).then { Unit }

            // when we get the value
            val result = composedCache.get("key")

            // then we return the value and set it in the first cache
            Assert.assertEquals("value", result)
            verify(firstCache).set("key", "value")
        }
    }

    @Test(expected = TestException::class)
    fun `throw internal exception on get when the first cache throws`() {
        runBlocking {
            // given the first cache throws an exception on get
            whenever(firstCache.get(anyString())).then { throw TestException() }

            // when we get the value
            composedCache.get("key")

            // then an exception is thrown
        }
    }

    @Test(expected = TestException::class)
    fun `throw internal exception on get when first cache empty and second cache throws`() {
        runBlocking {
            // given the second cache throws an exception on get
            whenever(firstCache.get(anyString())).then { null }
            whenever(secondCache.get(anyString())).then { throw TestException() }

            // when we get the value
            composedCache.get("key")

            // then an exception is thrown
        }
    }

    @Test(expected = TestException::class)
    fun `throw internal exception on get when first cache empty, second cache returns and set on first cache throws`() {
        runBlocking {
            // given value available in second cache only
            whenever(firstCache.get("key")).then { null }
            whenever(secondCache.get("key")).then { "value" }
            whenever(firstCache.set(anyString(), anyString())).then { throw TestException() }

            // when we get the value
            composedCache.get("key")

            // then an exception is thrown
        }
    }

    @Test
    fun `throw exception when job cancelled on get and first cache is executing get`() {
        runBlocking {
            // given the first cache throws an exception
            whenever(firstCache.get(anyString())).then { runBlocking { delay(250) } }
            whenever(secondCache.get(anyString())).then { executions.execute() }

            // when we get the value
            val job = async(Dispatchers.IO) { composedCache.get("key") }
            delay(50)
            job.cancel()
            yield()

            // then get on the second cache is not called and an exception is thrown
            val throwable = assertThrows(CancellationException::class.java) {
                runBlocking {
                    job.await()
                }
            }

            // expect exception
            assertThat(throwable.message, StringStartsWith("DeferredCoroutine was cancelled"))
        }
    }

    @Test
    fun `throw exception when job cancelled on get and second cache is executing get`() {
        runBlocking {
            // given the first cache throws an exception
            whenever(firstCache.get(anyString())).then { null }
            whenever(secondCache.get(anyString())).then { runBlocking { delay(250) } }

            // when we get the value
            val job = async(Dispatchers.IO) { composedCache.get("key") }
            delay(50)
            job.cancel()
            yield()

            // then get on the second cache is not called and an exception is thrown
            val throwable = assertThrows(CancellationException::class.java) {
                runBlocking {
                    job.await()
                }
            }

            // expect exception
            assertThat(throwable.message, StringStartsWith("DeferredCoroutine was cancelled"))
        }
    }

    @Test
    fun `throw exception when job cancelled on get and first cache is executing set after get`() {
        runBlocking {
            // given the first cache throws an exception
            whenever(firstCache.get(anyString())).then { null }
            whenever(secondCache.get(anyString())).then { "value" }
            whenever(firstCache.set(anyString(), anyString())).then { runBlocking { delay(250) } }

            // when we get the value
            val job = async(Dispatchers.IO) { composedCache.get("key") }
            delay(50)
            job.cancel()
            yield()

            // then get on the second cache is not called and an exception is thrown
            val throwable = assertThrows(CancellationException::class.java) {
                runBlocking {
                    job.await()
                }
            }

            // expect exception
            assertThat(throwable.message, StringStartsWith("DeferredCoroutine was cancelled"))
        }
    }
}
