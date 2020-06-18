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

import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.hamcrest.core.Is.isA
import org.hamcrest.core.StringStartsWith
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException
import org.mockito.Mockito.anyString
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class CacheComposeEvictShould {

    @get:Rule
    var thrown: ExpectedException = ExpectedException.none()

    @get:Rule
    var executions = ExecutionExpectation()

    private val firstCache = mock<AbstractCache<String, String>> {
        onGeneric { toString() } doReturn "firstCache"
    }
    private val secondCache = mock<AbstractCache<String, String>> {
        onGeneric { toString() } doReturn "secondCache"
    }

    private lateinit var composedCache: Cache<String, String>

    @Before
    fun before() {
        whenever(firstCache.compose(secondCache)).thenCallRealMethod()
        composedCache = firstCache.compose(secondCache)
        verify(firstCache).compose(secondCache)
    }

    @Test
    fun `throw exception when key is null`() {
        runBlocking {
            // expect exception
            thrown.expect(IllegalArgumentException::class.java)
            thrown.expectMessage(StringStartsWith("Required value was null"))

            // when key is null
            composedCache.evict(TestUtils.uninitialized())
        }
    }

    @Test
    fun `execute internal requests on evict in parallel`() {
        runBlocking {
            val jobTimeInMillis = 250L

            // given we have two caches with a long running job to evict a value
            whenever(firstCache.evict(anyString())).then {
                async(Executors.newSingleThreadExecutor().asCoroutineDispatcher()) {
                    TestUtils.blockingTask(jobTimeInMillis)
                }
            }
            whenever(secondCache.evict(anyString())).then {
                async(Executors.newSingleThreadExecutor().asCoroutineDispatcher()) {
                    TestUtils.blockingTask(jobTimeInMillis)
                }
            }

            // when we evict the value and start the timer
            val start = System.nanoTime()
            composedCache.evict("key")

            // then evict is called in parallel
            val elapsedTimeInMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start)
            Assert.assertTrue(elapsedTimeInMillis < (jobTimeInMillis * 2))
        }
    }

    @Test
    fun `execute evict for each cache`() {
        runBlocking {
            // given we have two caches
            whenever(firstCache.evict(anyString())).then { Unit }
            whenever(secondCache.evict(anyString())).then { Unit }

            // when we evict the value
            composedCache.evict("key")

            // then evict is called on both caches
            verify(firstCache).evict("key")
            verify(secondCache).evict("key")
            verifyNoMoreInteractions(firstCache)
            verifyNoMoreInteractions(secondCache)
        }
    }

    @Test
    fun `throw internal exception on evict when the first cache throws`() {
        runBlocking {
            // expect exception and successful execution of secondCache
            thrown.expect(CacheException::class.java)
            thrown.expectMessage("evict failed for firstCache")
            thrown.expectCause(isA(TestException::class.java))

            executions.expect(1)

            // given the first cache throws an exception
            whenever(firstCache.evict(anyString())).then {
                throw TestException()
            }
            whenever(secondCache.evict(anyString())).then {
                runBlocking {
                    delay(50)
                    executions.execute()
                }
            }

            // when we evict the value
            val job = async { composedCache.evict("key") }

            // then evict on the second cache still completes and an exception is thrown
            job.await()
        }
    }

    @Test
    fun `throw internal exception on evict when the second cache throws`() {
        runBlocking {
            // expect exception and successful execution of firstCache
            thrown.expect(CacheException::class.java)
            thrown.expectMessage("evict failed for secondCache")
            thrown.expectCause(isA(TestException::class.java))
            executions.expect(1)

            // given the second cache throws an exception
            whenever(firstCache.evict(anyString())).then {
                runBlocking {
                    delay(50)
                    executions.execute()
                }
            }
            whenever(secondCache.evict(anyString())).then {
                throw TestException()
            }

            // when we evict the value
            val job = async { composedCache.evict("key") }

            // then an exception is thrown
            job.await()
        }
    }

    @Test
    fun `throw internal exception on evict when both caches throws`() {
        runBlocking {
            // expect exception
            thrown.expect(CacheException::class.java)
            thrown.expectMessage("evict failed for firstCache, evict failed for secondCache")
            thrown.expectCause(isA(TestException::class.java))

            // given both caches throw an exception
            whenever(firstCache.evict(anyString())).then {
                throw TestException()
            }
            whenever(secondCache.evict(anyString())).then {
                throw TestException()
            }

            // when we evict the value
            val job = async { composedCache.evict("key") }

            // then an exception is thrown
            job.await()
        }
    }

    @Test
    fun `throw exception when job cancelled on evict and first cache is executing`() {
        runBlocking {
            // expect exception and successful execution of secondCache
            thrown.expect(CancellationException::class.java)
            thrown.expectMessage("DeferredCoroutine was cancelled")
            executions.expect(1)

            // given the first cache throws an exception
            whenever(firstCache.evict(anyString())).then {
                runBlocking {
                    delay(250)
                }
            }
            whenever(secondCache.evict(anyString())).then {
                executions.execute()
            }

            // when we evict the value
            val job = async { composedCache.evict("key") }
            delay(50)
            job.cancel()

            // then evict on the second cache still completes and an exception is thrown
            job.await()
        }
    }

    @Test
    fun `throw exception when job cancelled on evict and second cache is executing`() {
        runBlocking {
            // expect exception and successful execution of firstCache
            thrown.expect(CancellationException::class.java)
            thrown.expectMessage("DeferredCoroutine was cancelled")
            executions.expect(1)

            // given the first cache throws an exception
            whenever(firstCache.evict(anyString())).then {
                executions.execute()
            }
            whenever(secondCache.evict(anyString())).then {
                runBlocking {
                    delay(250)
                }
            }

            // when we evict the value
            val job = async { composedCache.evict("key") }
            delay(50)
            job.cancel()

            // then evict on the first cache still completes and an exception is thrown
            job.await()
        }
    }

    @Test
    fun `throw exception when job cancelled on evict and both caches executing`() {
        runBlocking {
            // expect exception and no execution of caches
            thrown.expect(CancellationException::class.java)
            thrown.expectMessage("DeferredCoroutine was cancelled")
            executions.expect(0)

            // given the first cache throws an exception
            whenever(firstCache.evict(anyString())).then {
                runBlocking {
                    delay(50)
                    executions.execute()
                }
            }
            whenever(secondCache.evict(anyString())).then {
                runBlocking {
                    delay(50)
                    executions.execute()
                }
            }

            // when we evict the value
            val job = async { composedCache.evict("key") }
            delay(25)
            job.cancel()

            // then an exception is thrown
            job.await()
        }
    }
}
