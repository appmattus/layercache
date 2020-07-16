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

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.core.IsEqual
import org.hamcrest.core.StringStartsWith
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test

class CacheMapKeysShould {

    @get:Rule
    var executions = ExecutionExpectation()

    private val cache = TestCache<String, Any>()

    private val mappedKeysCache: Cache<Int, Any> = cache.keyTransform { int: Int -> int.toString() }
    private val mappedKeysCacheWithError: Cache<Int, Any> = cache.keyTransform { throw TestException() }
    private val mappedKeysCacheWithNull: Cache<Int, Any> = cache.keyTransform { TestUtils.uninitialized() }

    @Test
    fun `contain cache in composed parents`() {
        val localCache = mappedKeysCache
        if (localCache !is ComposedCache<Int, Any>) {
            fail()
            return
        }

        assertThat(localCache.parents, IsEqual.equalTo(listOf<Cache<*, *>>(cache)))
    }

    // get
    @Test
    fun `map string value in get to int`() {
        runBlocking {
            // given we have a string
            cache.getFn = { if (it == "1") "value" else null }

            // when we get the value
            val result = mappedKeysCache.get(1)

            assertEquals("value", result)
        }
    }

    @Test
    fun `throw exception when transform returns null during get`() {
        // when the mapping function returns null
        val throwable = assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                mappedKeysCacheWithNull.get(1)
            }
        }

        // expect exception
        assertThat(throwable.message, StringStartsWith("Required value was null"))
    }

    @Test(expected = TestException::class)
    fun `throw exception when transform throws during get`() {
        runBlocking {
            // given we have a string
            cache.getFn = { if (it == "1") "value" else null }

            // when we get the value from a map with exception throwing functions
            mappedKeysCacheWithError.get(1)

            // then an exception is thrown
        }
    }

    @Test(expected = TestException::class)
    fun `throw exception when get throws`() {
        runBlocking {
            // given we have a string
            cache.getFn = { if (it == "1") throw TestException() else null }

            // when we get the value from a map
            mappedKeysCache.get(1)

            // then an exception is thrown
        }
    }

    @Test(expected = CancellationException::class)
    fun `throw exception when cancelled during get`() {
        runBlocking {
            // given we have a long running job
            cache.getFn = {
                delay(250)
                null
            }

            // when we cancel the job
            val job = async { mappedKeysCache.get(1) }
            delay(50)
            job.cancel()

            // then an exception is thrown
            job.await()
        }
    }

    // set
    @Test
    fun `map int value in set to string`() {
        runBlocking {
            // given we have a string
            executions.expect(1)
            cache.setFn = { key, _ -> if (key == "1") executions.execute() }

            // when we set the value
            mappedKeysCache.set(1, "1")

            // then it is converted to a string
        }
    }

    @Test
    fun `throw exception when transform returns null during set`() {
        // when the mapping function returns null
        val throwable = assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                mappedKeysCacheWithNull.set(1, "value")
            }
        }

        // expect exception
        assertThat(throwable.message, StringStartsWith("Required value was null"))
    }

    @Test(expected = TestException::class)
    fun `throw exception when transform throws during set`() {
        runBlocking {
            // given we have a string

            // when we get the value from a map with exception throwing functions
            mappedKeysCacheWithError.set(1, "1")

            // then an exception is thrown
        }
    }

    @Test(expected = TestException::class)
    fun `throw exception when set throws`() {
        runBlocking {
            // given we have a string
            cache.setFn = { _, _ -> throw TestException() }

            // when we set the value from a map
            mappedKeysCache.set(1, "1")

            // then an exception is thrown
        }
    }

    // throw exception when cancelled during set
    @Test(expected = CancellationException::class)
    fun `throw exception when cancelled during set`() {
        runBlocking {
            // given we have a long running job
            cache.setFn = { _, _ -> delay(250) }

            // when we cancel the job
            val job = async { mappedKeysCache.set(1, "1") }
            delay(50)
            job.cancel()

            // then an exception is thrown
            job.await()
        }
    }

    // evict
    @Test
    fun `call evict from cache`() {
        runBlocking {
            // given value available in first cache only
            executions.expect(1)
            cache.evictFn = { key -> if (key == "1") executions.execute() }

            // when we get the value
            mappedKeysCache.evict(1)

            // then we return the value
        }
    }

    @Test
    fun `throw exception when transform returns null during evict`() {
        // when the mapping function returns null
        val throwable = assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                mappedKeysCacheWithNull.evict(1)
            }
        }

        // expect exception
        assertThat(throwable.message, StringStartsWith("Required value was null"))
    }

    @Test(expected = TestException::class)
    fun `throw exception when transform throws during evict`() {
        runBlocking {
            // given value available in first cache only

            // when we get the value
            mappedKeysCacheWithError.evict(1)

            // then we throw an exception
        }
    }

    @Test(expected = TestException::class)
    fun `throw exception when evict throws`() {
        runBlocking {
            // given value available in first cache only
            cache.evictFn = { key -> if (key == "1") throw TestException() }

            // when we get the value
            mappedKeysCache.evict(1)

            // then we throw an exception
        }
    }

    @Test(expected = CancellationException::class)
    fun `throw exception when cancelled during evict`() {
        runBlocking {
            // given we have a long running job
            cache.evictFn = { key -> if (key == "1") delay(250) }

            // when we cancel the job
            val job = async { mappedKeysCache.evict(1) }
            delay(50)
            job.cancel()

            // then an exception is thrown
            job.await()
        }
    }

    // evictAll
    @Test
    fun `call evictAll from cache`() {
        runBlocking {
            // given value available in first cache only
            executions.expect(1)
            cache.evictAllFn = { executions.execute() }

            // when we evict the value
            mappedKeysCache.evictAll()

            // then we evictAll value
        }
    }

    @Test
    fun `no exception when transform returns null during evictAll`() {
        runBlocking {
            // given evictAll is implemented

            // when the mapping function returns null and we evictAll
            mappedKeysCacheWithNull.evictAll()

            // then no exception is thrown
        }
    }

    @Test
    fun `no exception when transform throws during evictAll`() {
        runBlocking {
            // given evictAll is implemented

            // when the mapping function throws an exception and we evictAll
            mappedKeysCacheWithError.evictAll()

            // then no exception is thrown
        }
    }

    @Test(expected = TestException::class)
    fun `throw exception when evictAll throws`() {
        runBlocking {
            // given value available in first cache only
            cache.evictAllFn = { throw TestException() }

            // when we evictAll values
            mappedKeysCache.evictAll()

            // then we throw an exception
        }
    }

    @Test(expected = CancellationException::class)
    fun `throw exception when cancelled during evictAll`() {
        runBlocking {
            // given we have a long running job
            cache.evictAllFn = { delay(250) }

            // when we cancel the job
            val job = async { mappedKeysCache.evictAll() }
            delay(50)
            job.cancel()

            // then an exception is thrown
            job.await()
        }
    }
}
