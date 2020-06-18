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

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import java.util.concurrent.atomic.AtomicInteger

class FetcherReuseInflightShould {

    @get:Rule
    var thrown: ExpectedException = ExpectedException.none()

    @Mock
    private lateinit var cache: AbstractFetcher<Any, Any>

    private lateinit var reuseInflightCache: Fetcher<Any, Any>

    @Before
    fun before() {
        MockitoAnnotations.initMocks(this)
        Mockito.`when`(cache.reuseInflight()).thenCallRealMethod()
        reuseInflightCache = cache.reuseInflight()

        Mockito.verify(cache).reuseInflight()
    }

    @Test
    fun `throw exception when directly chained`() {
        runBlocking {
            // expect exception
            thrown.expect(IllegalStateException::class.java)
            thrown.expectMessage("Do not directly chain reuseInflight")

            // when reuseInflight called again
            reuseInflightCache.reuseInflight()
        }
    }

    // get
    @Test
    fun `single call to get returns the value`() {
        runBlocking {
            // given value available in first cache only
            Mockito.`when`(cache.get("key")).then { "value" }

            // when we get the value
            val result = reuseInflightCache.get("key")

            // then we return the value
            Mockito.verify(cache).get("key")
            Assert.assertEquals("value", result)
        }
    }

    @Test
    fun `execute get only once whilst a call is in flight`() {
        val count = AtomicInteger(0)

        runBlocking {
            // given value available in first cache only
            Mockito.`when`(cache.get("key")).then {
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
            Assert.assertEquals(1, count.get())
        }
    }

    @Test
    fun `execute get twice if a call is made once the first one has finished`() {
        val count = AtomicInteger(0)

        runBlocking {
            // given value available in first cache only
            Mockito.`when`(cache.get("key")).then {
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
            Assert.assertEquals(2, count.get())
        }
    }

    @Test(expected = TestException::class)
    fun `propogate exception on get`() {
        runBlocking {
            // given value available in first cache only
            Mockito.`when`(cache.get("key")).then { throw TestException() }

            // when we get the value
            reuseInflightCache.get("key")

            // then we throw an exception
        }
    }

    // set
    @Test
    fun `not interact with parent set`() {
        runBlocking {
            // when we set the value
            @Suppress("DEPRECATION")
            reuseInflightCache.set("1", 1)

            // then the parent cache is not called
            Mockito.verifyNoMoreInteractions(cache)
        }
    }

    // evict
    @Test
    fun `not interact with parent evict`() {
        runBlocking {
            // when we evict the value
            @Suppress("DEPRECATION")
            reuseInflightCache.evict("1")

            // then the parent cache is not called
            Mockito.verifyNoMoreInteractions(cache)
        }
    }

    // evictAll
    @Test
    fun `not interact with parent evictAll`() {
        runBlocking {
            // when evictAll values
            @Suppress("DEPRECATION")
            reuseInflightCache.evictAll()

            // then the parent cache is not called
            Mockito.verifyNoMoreInteractions(cache)
        }
    }
}
