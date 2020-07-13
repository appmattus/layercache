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

import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.core.Is.`is`
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.TimeUnit

class CacheComposeEvictAllShould {

    @get:Rule
    var executions = ExecutionExpectation()

    private val firstCache = TestCache("firstCache")
    private val secondCache = TestCache("secondCache")

    private val composedCache: Cache<String, String> = firstCache.compose(secondCache)

    @Test
    fun `execute internal requests on evictAll in parallel`() {
        runBlocking {
            val jobTimeInMillis = 250L

            // given we have two caches with a long running job to evict a value
            firstCache.evictAllFn = {
                delay(jobTimeInMillis)
            }
            secondCache.evictAllFn = {
                delay(jobTimeInMillis)
            }

            // when we evictAll values and start the timer
            val start = System.nanoTime()
            composedCache.evictAll()

            // then evictAll is called in parallel
            val elapsedTimeInMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start)
            assertTrue(elapsedTimeInMillis < (jobTimeInMillis * 2))
        }
    }

    @Test
    fun `execute evictAll for each cache`() {
        runBlocking {
            // given we have two caches
            val firstCache = mock<AbstractCache<String, String>> {
                onBlocking { evictAll() } doReturn Unit
            }
            val secondCache = mock<AbstractCache<String, String>> {
                onBlocking { evictAll() } doReturn Unit
            }
            whenever(firstCache.compose(secondCache)).thenCallRealMethod()
            val composedCache = firstCache.compose(secondCache)
            verify(firstCache).compose(secondCache)

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
            // given the first cache throws an exception
            firstCache.evictAllFn = {
                throw TestException()
            }
            secondCache.evictAllFn = {
                delay(50)
            }

            assertThrows(TestException::class.java) {
                runBlocking {
                    // when we evictAll values
                    val job = async { composedCache.evictAll() }

                    // then evictAll on the second cache still completes and an exception is thrown
                    job.await()
                }
            }

            // expect exception
        }
    }

    @Test
    fun `throw internal exception on evictAll when the second cache throws`() {
        runBlocking {
            // given the second cache throws an exception
            firstCache.evictAllFn = {
                delay(50)
            }
            secondCache.evictAllFn = {
                throw TestException()
            }

            assertThrows(TestException::class.java) {
                runBlocking {
                    // when we evictAll values
                    val job = async { composedCache.evictAll() }

                    // then an exception is thrown
                    job.await()
                }
            }

            // expect exception
        }
    }

    @Test
    fun `throw internal exception on evictAll when both caches throws`() {
        runBlocking {
            // given both caches throw an exception
            firstCache.evictAllFn = {
                throw TestException()
            }
            secondCache.evictAllFn = {
                throw TestException()
            }

            assertThrows(TestException::class.java) {
                runBlocking {
                    // when we evictAll values
                    val job = async { composedCache.evictAll() }

                    // then an exception is thrown
                    job.await()
                }
            }

            // expect exception
        }
    }

    @Test
    fun `throw exception when job cancelled on evictAll and first cache is executing`() {
        runBlocking {
            executions.expect(1)

            // given the first cache throws an exception
            firstCache.evictAllFn = {
                delay(250)
            }
            secondCache.evictAllFn = {
                executions.execute()
            }

            val throwable = assertThrows(CancellationException::class.java) {
                runBlocking {
                    // when we evictAll values
                    val job = async { composedCache.evictAll() }
                    delay(50)
                    job.cancel()

                    // then evictAll on the second cache still completes and an exception is thrown
                    job.await()
                }
            }

            // expect exception and successful execution of secondCache
            assertThat(throwable.message, `is`("DeferredCoroutine was cancelled"))
        }
    }

    @Test
    fun `throw exception when job cancelled on evictAll and second cache is executing`() {
        runBlocking {
            executions.expect(1)

            // given the first cache throws an exception
            firstCache.evictAllFn = {
                executions.execute()
            }
            secondCache.evictAllFn = {
                delay(250)
            }

            val throwable = assertThrows(CancellationException::class.java) {
                runBlocking {
                    // when we evictAll values
                    val job = async { composedCache.evictAll() }
                    delay(50)
                    job.cancel()

                    // then evictAll on the first cache still completes and an exception is thrown
                    job.await()
                }
            }

            // expect exception and successful execution of firstCache
            assertThat(throwable.message, `is`("DeferredCoroutine was cancelled"))
        }
    }

    @Test
    fun `throw exception when job cancelled on evictAll and both caches executing`() {
        runBlocking {
            executions.expect(0)

            // given the first cache throws an exception
            firstCache.evictAllFn = {
                delay(50)
                executions.execute()
            }
            secondCache.evictAllFn = {
                delay(50)
                executions.execute()
            }

            val throwable = assertThrows(CancellationException::class.java) {
                runBlocking {
                    // when we evictAll values
                    val job = async { composedCache.evictAll() }
                    delay(25)
                    job.cancel()

                    // then an exception is thrown
                    job.await()
                }
            }

            // expect exception and no execution of caches
            assertThat(throwable.message, `is`("DeferredCoroutine was cancelled"))
        }
    }
}
