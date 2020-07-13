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

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

class CacheReuseInflightShould {

    @get:Rule
    var executions = ExecutionExpectation()

    private val cache = TestCache<Any, Any>()

    private lateinit var reuseInflightCache: Cache<Any, Any>

    @Before
    fun before() {
        reuseInflightCache = cache.reuseInflight()
    }

    // get
    @Test
    fun `single call to get returns the value`() {
        runBlocking {
            // given value available in first cache only
            cache.getFn = { key -> if (key == "key") "value" else null }

            // when we get the value
            val result = reuseInflightCache.get("key")

            // then we return the value
            assertEquals("value", result)
        }
    }

    @Test
    fun `execute get only once whilst a call is in flight`() {
        val count = AtomicInteger(0)

        runBlocking {
            // given value available in first cache only
            cache.getFn = { key ->
                if (key == "key") {
                    delay(100)
                    count.getAndIncrement()
                }
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
            cache.getFn = { key ->
                if (key == "key") {
                    delay(100)
                    count.getAndIncrement()
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
            cache.getFn = { key -> if (key == "key") throw TestException() }

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
            executions.expect(1)
            cache.setFn = { key, value -> if (key == "key" && value == "value") executions.execute() }

            // when we get the value
            reuseInflightCache.set("key", "value")

            // then we return the value
        }
    }

    @Test(expected = TestException::class)
    fun `propagate exception on set`() {
        runBlocking {
            // given value available in first cache only
            cache.setFn = { key, value -> if (key == "key" && value == "value") throw TestException() }

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
            executions.expect(1)
            cache.evictFn = { key -> if (key == "key") executions.execute() }

            // when we get the value
            reuseInflightCache.evict("key")

            // then we return the value
        }
    }

    @Test(expected = TestException::class)
    fun `propagate exception on evict`() {
        runBlocking {
            // given value available in first cache only
            cache.evictFn = { key -> if (key == "key") throw TestException() }

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
            executions.expect(1)
            cache.evictAllFn = { executions.execute() }

            // when we evictAll values
            reuseInflightCache.evictAll()

            // then evictAll is called
        }
    }

    @Test(expected = TestException::class)
    fun `propagate exception on evictAll`() {
        runBlocking {
            // given evictAll throws an exception
            cache.evictAllFn = { throw TestException() }

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
