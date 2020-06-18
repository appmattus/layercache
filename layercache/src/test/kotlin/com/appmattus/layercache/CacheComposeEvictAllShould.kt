/*
 * Copyright 2018 Appmattus Limited
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
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class CacheComposeEvictAllShould {

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
    fun `execute internal requests on evictAll in parallel`() {
        runBlocking {
            val jobTimeInMillis = 250L

            // given we have two caches with a long running job to evict a value
            whenever(firstCache.evictAll()).then {
                async(Executors.newSingleThreadExecutor().asCoroutineDispatcher()) {
                    TestUtils.blockingTask(jobTimeInMillis)
                }
            }
            whenever(secondCache.evictAll()).then {
                async(Executors.newSingleThreadExecutor().asCoroutineDispatcher()) {
                    TestUtils.blockingTask(jobTimeInMillis)
                }
            }

            // when we evictAll values and start the timer
            val start = System.nanoTime()
            composedCache.evictAll()

            // then evictAll is called in parallel
            val elapsedTimeInMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start)
            Assert.assertTrue(elapsedTimeInMillis < (jobTimeInMillis * 2))
        }
    }

    @Test
    fun `execute evictAll for each cache`() {
        runBlocking {
            // given we have two caches
            whenever(firstCache.evictAll()).then { Unit }
            whenever(secondCache.evictAll()).then { Unit }

            // when we evictAll values
            composedCache.evictAll()

            // then evictAll is called on both caches
            verify(firstCache).evictAll()
            verify(secondCache).evictAll()
            verifyNoMoreInteractions(firstCache)
            verifyNoMoreInteractions(secondCache)
        }
    }

    @Test
    fun `throw internal exception on evictAll when the first cache throws`() {
        runBlocking {
            // expect exception and successful execution of secondCache
            thrown.expect(CacheException::class.java)
            thrown.expectMessage("evictAll failed for firstCache")
            thrown.expectCause(isA(TestException::class.java))

            executions.expect(1)

            // given the first cache throws an exception
            whenever(firstCache.evictAll()).then {
                throw TestException()
            }
            whenever(secondCache.evictAll()).then {
                runBlocking {
                    delay(50)
                    executions.execute()
                }
            }

            // when we evictAll values
            val job = async { composedCache.evictAll() }

            // then evictAll on the second cache still completes and an exception is thrown
            job.await()
        }
    }

    @Test
    fun `throw internal exception on evictAll when the second cache throws`() {
        runBlocking {
            // expect exception and successful execution of firstCache
            thrown.expect(CacheException::class.java)
            thrown.expectMessage("evictAll failed for secondCache")
            thrown.expectCause(isA(TestException::class.java))
            executions.expect(1)

            // given the second cache throws an exception
            whenever(firstCache.evictAll()).then {
                runBlocking {
                    delay(50)
                    executions.execute()
                }
            }
            whenever(secondCache.evictAll()).then {
                throw TestException()
            }

            // when we evictAll values
            val job = async { composedCache.evictAll() }

            // then an exception is thrown
            job.await()
        }
    }

    @Test
    fun `throw internal exception on evictAll when both caches throws`() {
        runBlocking {
            // expect exception
            thrown.expect(CacheException::class.java)
            thrown.expectMessage("evictAll failed for firstCache, evictAll failed for secondCache")
            thrown.expectCause(isA(TestException::class.java))

            // given both caches throw an exception
            whenever(firstCache.evictAll()).then {
                throw TestException()
            }
            whenever(secondCache.evictAll()).then {
                throw TestException()
            }

            // when we evictAll values
            val job = async { composedCache.evictAll() }

            // then an exception is thrown
            job.await()
        }
    }

    @Test
    fun `throw exception when job cancelled on evictAll and first cache is executing`() {
        runBlocking {
            // expect exception and successful execution of secondCache
            thrown.expect(CancellationException::class.java)
            thrown.expectMessage("DeferredCoroutine was cancelled")
            executions.expect(1)

            // given the first cache throws an exception
            whenever(firstCache.evictAll()).then {
                runBlocking {
                    delay(250)
                }
            }
            whenever(secondCache.evictAll()).then {
                executions.execute()
            }

            // when we evictAll values
            val job = async { composedCache.evictAll() }
            delay(50)
            job.cancel()

            // then evictAll on the second cache still completes and an exception is thrown
            job.await()
        }
    }

    @Test
    fun `throw exception when job cancelled on evictAll and second cache is executing`() {
        runBlocking {
            // expect exception and successful execution of firstCache
            thrown.expect(CancellationException::class.java)
            thrown.expectMessage("DeferredCoroutine was cancelled")
            executions.expect(1)

            // given the first cache throws an exception
            whenever(firstCache.evictAll()).then {
                executions.execute()
            }
            whenever(secondCache.evictAll()).then {
                runBlocking {
                    delay(250)
                }
            }

            // when we evictAll values
            val job = async { composedCache.evictAll() }
            delay(50)
            job.cancel()

            // then evictAll on the first cache still completes and an exception is thrown
            job.await()
        }
    }

    @Test
    fun `throw exception when job cancelled on evictAll and both caches executing`() {
        runBlocking {
            // expect exception and no execution of caches
            thrown.expect(CancellationException::class.java)
            thrown.expectMessage("DeferredCoroutine was cancelled")
            executions.expect(0)

            // given the first cache throws an exception
            whenever(firstCache.evictAll()).then {
                runBlocking {
                    delay(50)
                    executions.execute()
                }
            }
            whenever(secondCache.evictAll()).then {
                runBlocking {
                    delay(50)
                    executions.execute()
                }
            }

            // when we evictAll values
            val job = async { composedCache.evictAll() }
            delay(25)
            job.cancel()

            // then an exception is thrown
            job.await()
        }
    }
}
