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
import kotlinx.coroutines.yield
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.core.Is.`is`
import org.hamcrest.core.Is.isA
import org.hamcrest.core.StringStartsWith
import org.junit.Assert
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.ArgumentMatchers.anyString
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class CacheComposeSetShould {

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
        val throwable = assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                // when key is null
                composedCache.set(TestUtils.uninitialized(), "value")
            }
        }

        // expect exception
        assertThat(throwable.message, StringStartsWith("Required value was null"))
    }

    @Test
    fun `throw exception when value is null`() {
        runBlocking {
            whenever(firstCache.set(anyString(), any())).then { Unit }
            whenever(secondCache.set(anyString(), any())).then { Unit }

            val throwable = assertThrows(IllegalArgumentException::class.java) {
                runBlocking {
                    // when value is null
                    composedCache.set("key", TestUtils.uninitialized())
                }
            }

            // expect exception
            assertThat(throwable.message, StringStartsWith("Required value was null"))
        }
    }

    @Test
    fun `execute internal requests on set in parallel`() {
        runBlocking {
            val jobTimeInMillis = 250L

            // given we have two caches with a long running job to set a value
            whenever(firstCache.set(anyString(), anyString())).then {
                async(Executors.newSingleThreadExecutor().asCoroutineDispatcher()) {
                    TestUtils.blockingTask(jobTimeInMillis)
                }
            }
            whenever(secondCache.set(anyString(), anyString())).then {
                async(Executors.newSingleThreadExecutor().asCoroutineDispatcher()) {
                    TestUtils.blockingTask(jobTimeInMillis)
                }
            }

            // when we set the value and start the timer
            val start = System.nanoTime()
            composedCache.set("key", "value")

            // then set is called in parallel
            val elapsedTimeInMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start)
            Assert.assertTrue(elapsedTimeInMillis < (jobTimeInMillis * 2))
        }
    }

    @Test
    fun `execute set for each cache`() {
        runBlocking {
            // given we have two caches
            whenever(firstCache.set(anyString(), anyString())).then { Unit }
            whenever(secondCache.set(anyString(), anyString())).then { Unit }

            // when we set the value
            composedCache.set("key", "value")

            // then set is called on both caches
            verify(firstCache).set("key", "value")
            verify(secondCache).set("key", "value")
            verifyNoMoreInteractions(firstCache)
            verifyNoMoreInteractions(secondCache)
        }
    }

    @Test
    fun `throw internal exception on set when the first cache throws`() {
        runBlocking {
            executions.expect(1)

            // given the first cache throws an exception
            whenever(firstCache.set(anyString(), anyString())).then {
                throw TestException()
            }
            whenever(secondCache.set(anyString(), anyString())).then {
                runBlocking {
                    delay(50)
                    executions.execute()
                }
            }

            val throwable = assertThrows(CacheException::class.java) {
                runBlocking {
                    // when we set the value
                    val job = async { composedCache.set("key", "value") }
                    yield()

                    // then set on the second cache still completes and an exception is thrown
                    job.await()
                }
            }

            // expect exception and successful execution of secondCache
            assertThat(throwable.message, `is`("set failed for firstCache"))
            assertThat(throwable.cause as? TestException, isA(TestException::class.java))
        }
    }

    @Test
    fun `throw internal exception on set when the second cache throws`() {
        runBlocking {
            executions.expect(1)

            // given the second cache throws an exception
            whenever(firstCache.set(anyString(), anyString())).then {
                runBlocking {
                    delay(50)
                    executions.execute()
                }
            }
            whenever(secondCache.set(anyString(), anyString())).then {
                throw TestException()
            }

            val throwable = assertThrows(CacheException::class.java) {
                runBlocking {
                    // when we set the value
                    val job = async { composedCache.set("key", "value") }
                    yield()

                    // then an exception is thrown
                    job.await()
                }
            }

            // expect exception and successful execution of firstCache
            assertThat(throwable.message, `is`("set failed for secondCache"))
            assertThat(throwable.cause as? TestException, isA(TestException::class.java))
        }
    }

    @Test
    fun `throw internal exception on set when both caches throws`() {
        runBlocking {
            // given both caches throw an exception
            whenever(firstCache.set(anyString(), anyString())).then {
                throw TestException()
            }
            whenever(secondCache.set(anyString(), anyString())).then {
                throw TestException()
            }

            val throwable = assertThrows(CacheException::class.java) {
                runBlocking {
                    // when we set the value
                    val job = async { composedCache.set("key", "value") }
                    yield()

                    // then an exception is thrown
                    job.await()
                }
            }

            // expect exception
            assertThat(throwable.message, `is`("set failed for firstCache, set failed for secondCache"))
            assertThat(throwable.cause as? TestException, isA(TestException::class.java))
        }
    }

    @Test
    fun `throw exception when job cancelled on set and first cache is executing`() {
        runBlocking {
            executions.expect(1)

            // given the first cache throws an exception
            whenever(firstCache.set(anyString(), anyString())).then {
                runBlocking {
                    delay(250)
                }
            }
            whenever(secondCache.set(anyString(), anyString())).then {
                executions.execute()
            }

            val throwable = assertThrows(CancellationException::class.java) {
                runBlocking {
                    // when we set the value
                    val job = async { composedCache.set("key", "value") }
                    delay(50)
                    job.cancel()
                    yield()

                    // then set on the second cache still completes and an exception is thrown
                    job.await()
                }
            }

            // expect exception and successful execution of secondCache
            assertThat(throwable.message, `is`("DeferredCoroutine was cancelled"))
        }
    }

    @Test
    fun `throw exception when job cancelled on set and second cache is executing`() {
        runBlocking {
            executions.expect(1)

            // given the first cache throws an exception
            whenever(firstCache.set(anyString(), anyString())).then {
                executions.execute()
            }
            whenever(secondCache.set(anyString(), anyString())).then {
                runBlocking {
                    delay(250)
                }
            }

            val throwable = assertThrows(CancellationException::class.java) {
                runBlocking {
                    // when we set the value
                    val job = async { composedCache.set("key", "value") }
                    delay(50)
                    job.cancel()
                    yield()

                    // then set on the first cache still completes and an exception is thrown
                    job.await()
                }
            }

            // expect exception and successful execution of firstCache
            assertThat(throwable.message, `is`("DeferredCoroutine was cancelled"))
        }
    }

    @Test
    fun `throw exception when job cancelled on set and both caches executing`() {
        runBlocking {
            executions.expect(0)

            // given the first cache throws an exception
            whenever(firstCache.set(anyString(), anyString())).then {
                runBlocking {
                    delay(50)
                    executions.execute()
                }
            }
            whenever(secondCache.set(anyString(), anyString())).then {
                runBlocking {
                    delay(50)
                    executions.execute()
                }
            }

            val throwable = assertThrows(CancellationException::class.java) {
                runBlocking {
                    // when we set the value
                    val job = async { composedCache.set("key", "value") }
                    delay(25)
                    job.cancel()
                    yield()

                    // then an exception is thrown
                    job.await()
                }
            }

            // expect exception and no execution of caches
            assertThat(throwable.message, `is`("DeferredCoroutine was cancelled"))
        }
    }
}
