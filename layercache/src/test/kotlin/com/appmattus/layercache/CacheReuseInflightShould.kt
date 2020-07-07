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
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

class CacheReuseInflightShould {

    private val cache = mock<AbstractCache<Any, Any>>()

    private lateinit var reuseInflightCache: Cache<Any, Any>

    @Before
    fun before() {
        whenever(cache.reuseInflight()).thenCallRealMethod()
        reuseInflightCache = cache.reuseInflight()
    }

    // get
    @Test
    fun `single call to get returns the value`() {
        runBlocking {
            // given value available in first cache only
            whenever(cache.get("key")).then { "value" }

            // when we get the value
            val result = reuseInflightCache.get("key")

            // then we return the value
            verify(cache).get("key")
            assertEquals("value", result)
        }
    }

    @Test
    fun `execute get only once whilst a call is in flight`() {
        val count = AtomicInteger(0)

        runBlocking {
            // given value available in first cache only
            whenever(cache.get("key")).then {
                runBlocking {
                    delay(100)
                }

                count.getAndIncrement()
            }

            // when we get the same key 5 times
            val jobs = arrayListOf<Deferred<Any?>>()
            for (i in 1..5) {
                jobs.add(async { reuseInflightCache.get("key") })
            }
            jobs.forEach { it.await() }

            // then get is only called once
            assertEquals(1, count.get())
        }
    }

    @Test
    fun `execute get twice if a call is made once the first one has finished`() {
        val count = AtomicInteger(0)

        runBlocking {
            // given value available in first cache only
            whenever(cache.get("key")).then {
                runBlocking {
                    delay(100)

                    count.incrementAndGet()
                }
            }

            launch { reuseInflightCache.get("key") }

            // we yield here as the map that stores the reuse may not have been cleared yet
            delay(200)

            // when we get the same key 5 times
            val jobs = arrayListOf<Deferred<Any?>>()
            for (i in 1..5) {
                jobs.add(async { reuseInflightCache.get("key") })
            }
            jobs.forEach { it.await() }

            // then get is only called once
            assertEquals(2, count.get())
        }
    }

    @Test(expected = TestException::class)
    fun `propogate exception on get`() {
        runBlocking {
            // given value available in first cache only
            whenever(cache.get("key")).then { throw TestException() }

            // when we get the value
            reuseInflightCache.get("key")

            // then we throw an exception
        }
    }

    // set
    @Test
    fun `call set from cache`() {
        runBlocking {
            // given value available in first cache only
            whenever(cache.set("key", "value")).then { "value" }

            // when we get the value
            reuseInflightCache.set("key", "value")

            // then we return the value
            // Assert.assertEquals("value", result)
            verify(cache).set("key", "value")
        }
    }

    @Test(expected = TestException::class)
    fun `propagate exception on set`() {
        runBlocking {
            // given value available in first cache only
            whenever(cache.set("key", "value")).then { throw TestException() }

            // when we get the value
            reuseInflightCache.set("key", "value")

            // then we throw an exception
        }
    }

    // evict
    @Test
    fun `call evict from cache`() {
        runBlocking {
            // given value available in first cache only
            whenever(cache.evict("key")).then { Unit }

            // when we get the value
            reuseInflightCache.evict("key")

            // then we return the value
            // Assert.assertEquals("value", result)
            verify(cache).evict("key")
        }
    }

    @Test(expected = TestException::class)
    fun `propagate exception on evict`() {
        runBlocking {
            // given value available in first cache only
            whenever(cache.evict("key")).then { throw TestException() }

            // when we get the value
            reuseInflightCache.evict("key")

            // then we throw an exception
        }
    }

    // evictAll
    @Test
    fun `call evictAll from cache`() {
        runBlocking {
            // given evictAll is implemented
            whenever(cache.evictAll()).then { Unit }

            // when we evictAll values
            reuseInflightCache.evictAll()

            // then evictAll is called
            verify(cache).evictAll()
        }
    }

    @Test(expected = TestException::class)
    fun `propagate exception on evictAll`() {
        runBlocking {
            // given evictAll throws an exception
            whenever(cache.evictAll()).then { throw TestException() }

            // when we evictAll values
            reuseInflightCache.evictAll()

            // then we throw an exception
        }
    }

    @Test
    fun `throw exception when directly chained`() {
        runBlocking {
            // when reuseInflight called again
            val throwable = assertThrows(IllegalStateException::class.java) {
                reuseInflightCache.reuseInflight()
            }

            // expect exception
            assertEquals(throwable.message, "Do not directly chain reuseInflight")
        }
    }
}
