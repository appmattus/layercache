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
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.ArgumentMatchers.anyMap
import org.mockito.ArgumentMatchers.anyString
import java.util.concurrent.TimeUnit

class CacheBatchSetShould {

    private val requestTimeInMills = 250L

    private val cache = mock<AbstractCache<String, String>> {
        onBlocking { batchSet(any()) }.thenCallRealMethod()
    }

    @Test
    fun `throw exception when values map is null`() {
        // when values map is null
        val throwable = assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                object : AbstractCache<String, String>() {
                    override suspend fun get(key: String): String? = null
                    override suspend fun set(key: String, value: String) = Unit
                    override suspend fun evict(key: String) = Unit
                    override suspend fun evictAll() = Unit
                }.batchSet(TestUtils.uninitialized())
            }
        }

        // expect exception
        assertTrue(throwable.message!!.startsWith("Required value was null"))
    }

    @Test
    fun `throw exception when key in entry in values map is null`() {
        runBlocking {

            // when key in values map is null
            val throwable = assertThrows(IllegalArgumentException::class.java) {
                runBlocking {
                    cache.batchSet(mapOf(Pair("key1", "value1"), Pair(TestUtils.uninitialized(), "value2"), Pair("key3", "value3")))
                }
            }

            // expect exception
            assertTrue(throwable.message!!.startsWith("null element found in"))
        }
    }

    @Test
    fun `not throw exception when value in entry in values map is null`() {
        runBlocking {
            // given we have a cache
            whenever(cache.set(anyString(), any())).then { Unit }

            // when key in values map is null
            cache.batchSet(mapOf(Pair("key1", "value1"), Pair("key2", TestUtils.uninitialized()), Pair("key3", "value3")))

            // then no exception is thrown
        }
    }

    @Test(expected = CancellationException::class)
    fun `throw exception when job cancelled`() {
        runBlocking {
            // given we set values into a cache
            whenever(cache.set(anyString(), anyString())).then {
                runBlocking {
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
            whenever(cache.set(anyString(), anyString())).then {
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
            whenever(cache.set(anyString(), anyString())).then { runBlocking { } }
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
            whenever(cache.set(anyString(), anyString())).then {
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
