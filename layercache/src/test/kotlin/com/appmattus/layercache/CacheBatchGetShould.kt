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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.anyString
import org.mockito.MockitoAnnotations
import java.util.concurrent.TimeUnit

class CacheBatchGetShould {

    private val requestTimeInMills = 250L

    @get:Rule
    var thrown = ExpectedException.none()

    @Mock
    private lateinit var cache: AbstractCache<String, String>

    @Before
    fun before() {
        MockitoAnnotations.initMocks(this)
        Mockito.`when`(cache.batchGet(Mockito.anyList<String>())).thenCallRealMethod()
        Mockito.`when`(cache.batchGet(MockitoKotlin.any())).thenCallRealMethod()
    }

    @Test
    fun `throw exception when keys list is null`() {
        runBlocking {
            // expect exception
            thrown.expect(IllegalArgumentException::class.java)
            thrown.expectMessage(StringStartsWith("Parameter specified as non-null is null"))

            // when key is null
            cache.batchGet(TestUtils.uninitialized<List<String>>()).await()
        }
    }

    @Test
    fun `throw exception when key in list is null`() {
        runBlocking {
            // expect exception
            thrown.expect(IllegalArgumentException::class.java)
            thrown.expectMessage(StringStartsWith("null element found in"))

            // when key in list is null
            cache.batchGet(listOf("key1", TestUtils.uninitialized<String>(), "key3")).await()
        }
    }


    @Test(expected = CancellationException::class)
    fun `throw exception when job cancelled`() {
        runBlocking {
            // given we request the values for 3 keys
            Mockito.`when`(cache.get(anyString())).then {
                async(CommonPool) {
                    delay(requestTimeInMills, TimeUnit.MILLISECONDS)
                    "value"
                }
            }
            val job = cache.batchGet(listOf("key1", "key2", "key3"))

            // when we cancel the job
            assertTrue(job.cancel())

            // then a CancellationException is thrown
            job.await()
        }
    }

    @Test
    fun `execute internal requests in parallel`() {
        try {
            runBlocking {
                // given we start a timer and request the values for 3 keys
                Mockito.`when`(cache.get(anyString())).then {
                    async(CommonPool) {
                        delay(requestTimeInMills, TimeUnit.MILLISECONDS); "value"
                    }
                }

                val start = System.nanoTime()
                val job = cache.batchGet(listOf("key1", "key2", "key3"))

                // when we wait for the job to complete
                job.await()

                // then the job completes in less that the time to execute all 3 requests in sequence
                val executionTimeInMills = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start)
                assertTrue(executionTimeInMills > requestTimeInMills)
                assertTrue(executionTimeInMills < (requestTimeInMills * 3))
            }
        } catch (e: Throwable) {
            throw e
        }
    }

    @Test
    fun `return values in key sequence`() {
        runBlocking {
            // given we request the values for 3 keys where the second value takes longer to return
            Mockito.`when`(cache.get(anyString())).then {
                async(CommonPool) {
                    val key = it.getArgument<String>(0)
                    if (key == "key2") {
                        delay(requestTimeInMills, TimeUnit.MILLISECONDS)
                    }
                    key.replace("key", "value")
                }
            }
            val job = cache.batchGet(listOf("key1", "key2", "key3"))

            // when we wait for the job to complete
            val result = job.await()

            // then the job completes with the values in the same sequence as the keys
            assertEquals(listOf("value1", "value2", "value3"), result)
            Mockito.verify(cache, Mockito.times(3)).get(anyString())
        }
    }

    @Test(expected = TestException::class)
    fun `throw internal exception`() {
        runBlocking {
            // given we request 3 keys where the second key throws an exception
            Mockito.`when`(cache.get(anyString())).then {
                async(CommonPool) {
                    val key = it.getArgument<String>(0)
                    if (key == "key2") {
                        throw TestException()
                    }
                    key.replace("key", "value")
                }
            }
            val job = cache.batchGet(listOf("key1", "key2", "key3"))

            // when we wait for the job to complete
            job.await()

            // then the internal exception is thrown
        }
    }
}
