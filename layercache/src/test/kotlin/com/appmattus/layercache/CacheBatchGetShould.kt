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
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyList
import org.mockito.Mockito
import org.mockito.Mockito.anyString
import java.util.concurrent.TimeUnit

class CacheBatchGetShould {

    private val requestTimeInMills = 250L

    private val cache = mock<AbstractCache<String, String>>()

    @Before
    fun before() {
        whenever(runBlocking { cache.batchGet(anyList()) }).thenCallRealMethod()
        whenever(runBlocking { cache.batchGet(TestUtils.uninitialized()) }).thenCallRealMethod()
    }

    @Test
    fun `throw exception when keys list is null`() {
        runBlocking {
            // when key is null
            val throwable = assertThrows(IllegalArgumentException::class.java) {
                runBlocking {
                    cache.batchGet(TestUtils.uninitialized())
                }
            }

            // expect exception
            assertTrue(throwable.message!!.startsWith("Required value was null"))
        }
    }

    @Test
    fun `throw exception when key in list is null`() {
        runBlocking {
            // when key in list is null
            val throwable = assertThrows(IllegalArgumentException::class.java) {
                runBlocking {
                    cache.batchGet(listOf("key1", TestUtils.uninitialized(), "key3"))
                }
            }

            // expect exception
            assertTrue(throwable.message!!.startsWith("null element found in"))
        }
    }

    @Test(expected = CancellationException::class)
    fun `throw exception when job cancelled`() {
        runBlocking {
            // given we request the values for 3 keys
            whenever(cache.get(anyString())).then {
                runBlocking {
                    delay(requestTimeInMills)
                    "value"
                }
            }
            val job = async { cache.batchGet(listOf("key1", "key2", "key3")) }

            // when we cancel the job
            job.cancel()

            // then a CancellationException is thrown
            job.await()
        }
    }

    @Test
    fun `execute internal requests in parallel`() {
        try {
            runBlocking {
                // given we start a timer and request the values for 3 keys
                whenever(cache.get(anyString())).then {
                    runBlocking {
                        delay(requestTimeInMills); "value"
                    }
                }

                val start = System.nanoTime()
                val job = async { cache.batchGet(listOf("key1", "key2", "key3")) }

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
            whenever(cache.get(anyString())).then {
                runBlocking {
                    val key = it.getArgument<String>(0)
                    if (key == "key2") {
                        delay(requestTimeInMills)
                    }
                    key.replace("key", "value")
                }
            }
            val job = async { cache.batchGet(listOf("key1", "key2", "key3")) }

            // when we wait for the job to complete
            val result = job.await()

            // then the job completes with the values in the same sequence as the keys
            assertEquals(listOf("value1", "value2", "value3"), result)
            verify(cache, Mockito.times(3)).get(anyString())
        }
    }

    @Test(expected = TestException::class)
    fun `throw internal exception`() {
        runBlocking {
            // given we request 3 keys where the second key throws an exception
            whenever(cache.get(anyString())).then {
                val key = it.getArgument<String>(0)
                if (key == "key2") {
                    throw TestException()
                }
                key.replace("key", "value")
            }
            val job = async { cache.batchGet(listOf("key1", "key2", "key3")) }

            // when we wait for the job to complete
            job.await()

            // then the internal exception is thrown
        }
    }
}
