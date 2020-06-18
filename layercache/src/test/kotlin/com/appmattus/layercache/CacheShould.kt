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
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

class CacheShould {

    lateinit var cache: Cache<String, String>

    @Before
    fun before() {

        cache = object : Cache<String, String> {
            override suspend fun get(key: String): String? {
                delay(500)
                return "value"
            }

            override suspend fun set(key: String, value: String) {
                delay(500)
            }

            override suspend fun evict(key: String) {
                delay(500)
            }

            override suspend fun evictAll() {
                delay(500)
            }
        }
    }

    @Test(expected = CancellationException::class)
    fun `throw exception on get when job cancelled`() {
        runBlocking {
            // given we call get
            val job = async { cache.get("key") }

            // when we cancel the job
            job.cancel()

            // then the job is cancelled and exception returned
            job.await()
        }
    }


    @Test(expected = CancellationException::class)
    fun `throw exception on set when job cancelled`() {
        runBlocking {
            // given we call set
            val job = async { cache.set("key", "value") }

            // when we cancel the job
            job.cancel()

            // then the job is cancelled and exception returned
            job.await()
        }
    }

    @Test(expected = CancellationException::class)
    fun `throw exception on evict when job cancelled`() {
        runBlocking {
            // given we call evict
            val job = async { cache.evict("key") }

            // when we cancel the job
            job.cancel()

            // then the job is cancelled and exception returned
            job.await()
        }
    }

    @Test(expected = CancellationException::class)
    fun `throw exception on evictAll when job cancelled`() {
        runBlocking {
            // given we call evictAll
            val job = async { cache.evictAll() }

            // when we cancel the job
            job.cancel()

            // then the job is cancelled and exception returned
            job.await()
        }
    }


    @Test
    fun `execute onSuccess when job completes as expected`() {
        runBlocking {
            // given we call get
            val job = async { cache.get("key") }

            var called = false
            job.onSuccess { called = true }
            job.onCompletion { fail() }

            // when we wait for the job
            job.await()

            delay(100)

            // then onSuccess is executed
            assertEquals(true, called)
        }
    }

    @Test
    fun `onFailure`() {
        runBlocking {
            cache = object : Cache<String, String> {
                override suspend fun get(key: String): String? {
                    throw Exception("Forced failure")
                }

                override suspend fun set(key: String, value: String) {
                    delay(500)
                }

                override suspend fun evict(key: String) {
                    delay(500)
                }

                override suspend fun evictAll() {
                    delay(500)
                }
            }


            // given we call get
            val job = async { cache.get("key") }

            job.onSuccess { println("Result $it") }
            job.onCancel { println("Exception $it") }

            // when we cancel the job
            job.cancel()

            // then the job is cancelled and exception returned
            job.join()

            yield()
        }
    }

    @Test
    fun `onFailure cancelled`() {
        runBlocking {
            cache = object : Cache<String, String> {
                override suspend fun get(key: String): String? {
                    delay(500)
                    return "value"
                }

                override suspend fun set(key: String, value: String) {
                    delay(500)
                }

                override suspend fun evict(key: String) {
                    delay(500)
                }

                override suspend fun evictAll() {
                    delay(500)
                }
            }


            // given we call evict
            val job = async { cache.get("key") }

            job.onSuccess { result -> println("Result $result") }
            job.onCancel { exception -> println("Cancelled exception $exception") }

            // when we cancel the job
            job.cancel()

            // then the job is cancelled and exception returned
            job.join()

            yield()
        }
    }


    @Test
    fun `onCompletion cancelled`() {
        runBlocking {
            cache = object : Cache<String, String> {
                override suspend fun get(key: String): String? {
                    delay(500)
                    return "value"
                }

                override suspend fun set(key: String, value: String) {
                    delay(500)
                }

                override suspend fun evict(key: String) {
                    delay(500)
                }

                override suspend fun evictAll() {
                    delay(500)
                }
            }


            // given we call evict
            val job = async { cache.get("key") }

            job.onCompletion {
                when (it) {
                    is DeferredResult.Success -> println("Result ${it.value}")
                    is DeferredResult.Cancelled -> println("Cancelled exception ${it.exception}")
                }
            }

            job.onSuccess { value -> println("Result $value") }
            job.onCancel { exception -> println("Cancelled exception $exception") }

            // when we cancel the job
            job.cancel()

            // then the job is cancelled and exception returned
            job.join()

            yield()
        }
    }

}