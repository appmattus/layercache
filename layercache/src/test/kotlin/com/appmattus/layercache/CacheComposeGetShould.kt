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

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.hamcrest.core.StringStartsWith
import org.junit.Assert
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.anyString
import org.mockito.MockitoAnnotations

class CacheComposeGetShould {

    @get:Rule
    var thrown: ExpectedException = ExpectedException.none()

    @get:Rule
    var executions = ExecutionExpectation()

    @Mock
    private lateinit var firstCache: AbstractCache<String, String>

    @Mock
    private lateinit var secondCache: AbstractCache<String, String>

    private lateinit var composedCache: Cache<String, String>

    @Before
    fun before() {
        MockitoAnnotations.initMocks(this)
        Mockito.`when`(firstCache.compose(secondCache)).thenCallRealMethod()
        composedCache = firstCache.compose(secondCache)
        Mockito.verify(firstCache).compose(secondCache)
    }

    @Test
    fun `throw exception when key is null`() {
        runBlocking {
            // expect exception
            thrown.expect(IllegalArgumentException::class.java)
            thrown.expectMessage(StringStartsWith("Required value was null"))

            // when key is null
            composedCache.get(TestUtils.uninitialized())
        }
    }

    @Test
    fun `return value from first cache when available in first cache`() {
        runBlocking {
            // given value available in first cache only
            Mockito.`when`(firstCache.get("key")).then { "value" }

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
            Mockito.`when`(firstCache.get("key")).then { "value" }

            // when we get the value
            composedCache.get("key")

            // then we do not call the second cache
            Mockito.verifyNoInteractions(secondCache)
        }
    }

    @Test
    fun `return value from second cache when only available in second cache and set on first cache`() {
        runBlocking {
            // given value available in second cache only
            Mockito.`when`(firstCache.get("key")).then { null }
            Mockito.`when`(secondCache.get("key")).then { "value" }
            Mockito.`when`(firstCache.set(anyString(), anyString())).then { GlobalScope.async {} }

            // when we get the value
            val result = composedCache.get("key")

            // then we return the value and set it in the first cache
            Assert.assertEquals("value", result)
            Mockito.verify(firstCache).set("key", "value")
        }
    }

    @Test(expected = TestException::class)
    fun `throw internal exception on get when the first cache throws`() {
        runBlocking {
            // given the first cache throws an exception on get
            Mockito.`when`(firstCache.get(anyString())).then { throw TestException() }

            // when we get the value
            composedCache.get("key")

            // then an exception is thrown
        }
    }


    @Test(expected = TestException::class)
    fun `throw internal exception on get when first cache empty and second cache throws`() {
        runBlocking {
            // given the second cache throws an exception on get
            Mockito.`when`(firstCache.get(Mockito.anyString())).then { null }
            Mockito.`when`(secondCache.get(Mockito.anyString())).then { throw TestException() }

            // when we get the value
            composedCache.get("key")

            // then an exception is thrown
        }
    }

    @Test(expected = TestException::class)
    fun `throw internal exception on get when first cache empty, second cache returns and set on first cache throws`() {
        runBlocking {
            // given value available in second cache only
            Mockito.`when`(firstCache.get("key")).then { null }
            Mockito.`when`(secondCache.get("key")).then { "value" }
            Mockito.`when`(firstCache.set(Mockito.anyString(), Mockito.anyString())).then { GlobalScope.async { throw TestException() } }

            // when we get the value
            composedCache.get("key")

            // then an exception is thrown
        }
    }

    @Test
    @Ignore("No longer true in latest coroutine library")
    fun `throw exception when job cancelled on get and first cache is executing get`() {
        runBlocking {
            // expect exception
            thrown.expect(CancellationException::class.java)
            thrown.expectMessage("Job was cancelled")

            // given the first cache throws an exception
            Mockito.`when`(firstCache.get(anyString())).then { runBlocking { delay(250) } }
            Mockito.`when`(secondCache.get(anyString())).then { executions.execute() }

            // when we get the value
            val job = async { composedCache.get("key") }
            delay(50)
            job.cancel()
            yield()

            // then get on the second cache is not called and an exception is thrown
            job.await()
        }
    }

    @Test
    @Ignore("No longer true in latest coroutine library")
    fun `throw exception when job cancelled on get and second cache is executing get`() {
        runBlocking {
            // expect exception
            thrown.expect(CancellationException::class.java)
            thrown.expectMessage("Job was cancelled")

            // given the first cache throws an exception
            Mockito.`when`(firstCache.get(anyString())).then { null }
            Mockito.`when`(secondCache.get(anyString())).then { runBlocking { delay(250) } }

            // when we get the value
            val job = async { composedCache.get("key") }
            delay(50)
            job.cancel()
            yield()

            // then get on the second cache is not called and an exception is thrown
            job.await()
        }
    }

    @Test
    @Ignore("No longer true in latest coroutine library")
    fun `throw exception when job cancelled on get and first cache is executing set after get`() {
        runBlocking {
            // expect exception
            thrown.expect(CancellationException::class.java)
            thrown.expectMessage("Job was cancelled")

            // given the first cache throws an exception
            Mockito.`when`(firstCache.get(anyString())).then { null }
            Mockito.`when`(secondCache.get(anyString())).then { "value" }
            Mockito.`when`(firstCache.set(anyString(), anyString())).then { GlobalScope.async { delay(250) } }

            // when we get the value
            val job = async { composedCache.get("key") }
            delay(50)
            job.cancel()
            yield()

            // then get on the second cache is not called and an exception is thrown
            job.await()
        }
    }
}