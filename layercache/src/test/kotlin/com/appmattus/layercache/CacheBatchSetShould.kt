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

import kotlinx.coroutines.experimental.CancellationException
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.runBlocking
import org.hamcrest.core.StringStartsWith
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.anyMap
import org.mockito.Mockito.anyString
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoMoreInteractions
import org.mockito.MockitoAnnotations
import java.util.concurrent.TimeUnit

class CacheBatchSetShould {

    private val requestTimeInMills = 250L

    @get:Rule
    var thrown = ExpectedException.none()

    @Mock
    private lateinit var cache: AbstractCache<String, String>

    @Before
    fun before() {
        MockitoAnnotations.initMocks(this)
        Mockito.`when`(cache.batchSet(MockitoKotlin.any())).thenCallRealMethod()
    }

    @Test
    fun `throw exception when values map is null`() {
        runBlocking {
            // expect exception
            thrown.expect(IllegalArgumentException::class.java)
            thrown.expectMessage(StringStartsWith("Parameter specified as non-null is null"))

            // when values map is null
            cache.batchSet(TestUtils.uninitialized<Map<String, String>>()).await()
        }
    }

    @Test
    fun `throw exception when key in entry in values map is null`() {
        runBlocking {
            // expect exception
            thrown.expect(IllegalArgumentException::class.java)
            thrown.expectMessage(StringStartsWith("null element found in"))

            // when key in values map is null
            cache.batchSet(mapOf(Pair("key1", "value1"), Pair(TestUtils.uninitialized<String>(), "value2"), Pair("key3", "value3"))).await()
        }
    }

    @Test
    fun `not throw exception when value in entry in values map is null`() {
        runBlocking {
            // given we have a cache
            Mockito.`when`(cache.set(anyString(), MockitoKotlin.any())).then { async(CommonPool) {} }

            // when key in values map is null
            cache.batchSet(mapOf(Pair("key1", "value1"), Pair("key2", TestUtils.uninitialized<String>()), Pair("key3", "value3"))).await()

            // then no exception is thrown
        }
    }

    @Test(expected = CancellationException::class)
    fun `throw exception when job cancelled`() {
        runBlocking {
            // given we set values into a cache
            Mockito.`when`(cache.set(anyString(), anyString())).then {
                async(CommonPool) {
                    delay(requestTimeInMills, TimeUnit.MILLISECONDS)
                }
            }
            val job = cache.batchSet(mapOf(Pair("key1", "value1"), Pair("key2", "value2"), Pair("key3", "value3")))

            // when we cancel the job
            assertTrue(job.cancel())

            // then a CancellationException is thrown
            job.await()
        }
    }

    @Test
    fun `execute internal requests in parallel`() {
        runBlocking {
            // given we start a timer and set the values for 3 keys
            Mockito.`when`(cache.set(anyString(), anyString())).then {
                async(CommonPool) {
                    delay(requestTimeInMills, TimeUnit.MILLISECONDS)
                }
            }
            val start = System.nanoTime()
            val job = cache.batchSet(mapOf(Pair("key1", "value1"), Pair("key2", "value2"), Pair("key3", "value3")))

            // when we wait for the job to complete
            job.await()

            // then the job completes in less that the time to execute all 3 requests in sequence
            val executionTimeInMills = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start)
            assertTrue(executionTimeInMills > requestTimeInMills)
            assertTrue(executionTimeInMills < (requestTimeInMills * 3))
        }
    }

    @Test
    fun `execute set for each key`() {
        runBlocking {
            // given we set the values for 3 keys
            Mockito.`when`(cache.set(anyString(), anyString())).then { async(CommonPool) { } }
            val job = cache.batchSet(mapOf(Pair("key1", "value1"), Pair("key2", "value2"), Pair("key3", "value3")))

            // when we wait for the job to complete
            job.await()

            // then the job completes and calls set for each pair
            verify(cache).batchSet(anyMap())
            verify(cache).set("key1", "value1")
            verify(cache).set("key2", "value2")
            verify(cache).set("key3", "value3")
            verifyNoMoreInteractions(cache)
        }
    }

    @Test(expected = TestException::class)
    fun `throw internal exception`() {
        runBlocking {
            // given we request 3 keys where the second key throws an exception
            Mockito.`when`(cache.set(anyString(), anyString())).then {
                async(CommonPool) {
                    val key = it.getArgument<String>(0)
                    if (key == "key2") {
                        throw TestException()
                    }
                    key.replace("key", "value")
                }
            }
            val job = cache.batchSet(mapOf(Pair("key1", "value1"), Pair("key2", "value2"), Pair("key3", "value3")))

            // when we wait for the job to complete
            job.await()

            // then the internal exception is thrown
        }
    }
}
