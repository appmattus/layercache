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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.hamcrest.core.StringStartsWith
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException
import org.mockito.Mockito
import org.mockito.Mockito.anyMap
import org.mockito.Mockito.anyString
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoMoreInteractions
import java.util.concurrent.TimeUnit

class CacheBatchSetShould {

    private val requestTimeInMills = 250L

    @get:Rule
    var thrown: ExpectedException = ExpectedException.none()

    private val cache = mock<AbstractCache<String, String>>()

    @Before
    fun before() {
        runBlocking {
            Mockito.`when`(cache.batchSet(MockitoKotlin.any())).thenCallRealMethod()
        }
    }

    @Test
    fun `throw exception when values map is null`() {
        runBlocking {
            // expect exception
            thrown.expect(IllegalArgumentException::class.java)
            thrown.expectMessage(StringStartsWith("Required value was null"))

            // when values map is null
            cache.batchSet(TestUtils.uninitialized<Map<String, String>>())
        }
    }

    @Test
    fun `throw exception when key in entry in values map is null`() {
        runBlocking {
            // expect exception
            thrown.expect(IllegalArgumentException::class.java)
            thrown.expectMessage(StringStartsWith("null element found in"))

            // when key in values map is null
            cache.batchSet(mapOf(Pair("key1", "value1"), Pair(TestUtils.uninitialized<String>(), "value2"), Pair("key3", "value3")))
        }
    }

    @Test
    fun `not throw exception when value in entry in values map is null`() {
        runBlocking {
            // given we have a cache
            Mockito.`when`(cache.set(anyString(), MockitoKotlin.any())).then { GlobalScope.async {} }

            // when key in values map is null
            cache.batchSet(mapOf(Pair("key1", "value1"), Pair("key2", TestUtils.uninitialized<String>()), Pair("key3", "value3")))

            // then no exception is thrown
        }
    }

    @Test(expected = CancellationException::class)
    fun `throw exception when job cancelled`() {
        runBlocking {
            // given we set values into a cache
            Mockito.`when`(cache.set(anyString(), anyString())).then {
                GlobalScope.async {
                    delay(requestTimeInMills)
                }
            }
            val job = async { cache.batchSet(mapOf(Pair("key1", "value1"), Pair("key2", "value2"), Pair("key3", "value3"))) }

            // when we cancel the job
            job.cancel()

            // then a CancellationException is thrown
            job.await()
        }
    }

    @Test
    fun `execute internal requests in parallel`() {
        runBlocking {
            // given we start a timer and set the values for 3 keys
            Mockito.`when`(cache.set(anyString(), anyString())).then {
                runBlocking {
                    delay(requestTimeInMills)
                }
            }
            val start = System.nanoTime()
            val job = async { cache.batchSet(mapOf(Pair("key1", "value1"), Pair("key2", "value2"), Pair("key3", "value3"))) }

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
            Mockito.`when`(cache.set(anyString(), anyString())).then { GlobalScope.async { } }
            val job = async { cache.batchSet(mapOf(Pair("key1", "value1"), Pair("key2", "value2"), Pair("key3", "value3"))) }

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
                val key = it.getArgument<String>(0)
                if (key == "key2") {
                    throw TestException()
                }
                key.replace("key", "value")
            }
            val job = async { cache.batchSet(mapOf(Pair("key1", "value1"), Pair("key2", "value2"), Pair("key3", "value3"))) }

            // when we wait for the job to complete
            job.await()

            // then the internal exception is thrown
        }
    }
}
