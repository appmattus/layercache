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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.core.IsEqual
import org.hamcrest.core.StringStartsWith
import org.junit.Assert
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.mockito.Answers
import org.mockito.ArgumentMatchers.anyString

class CacheMapKeysShould {

    private val cache = mock<AbstractCache<String, Any>>()

    private lateinit var mappedKeysCache: Cache<Int, Any>
    private lateinit var mappedKeysCacheWithError: Cache<Int, Any>
    private lateinit var mappedKeysCacheWithNull: Cache<Int, Any>

    @Before
    fun before() {
        val fInv: (Int) -> String = { int: Int -> int.toString() }
        whenever(cache.keyTransform(fInv)).thenCallRealMethod()
        mappedKeysCache = cache.keyTransform(fInv)

        val errorFInv: (Int) -> String = { _: Int -> throw TestException() }
        whenever(cache.keyTransform(errorFInv)).thenCallRealMethod()
        mappedKeysCacheWithError = cache.keyTransform(errorFInv)

        val nullFInv: (Int) -> String = { _: Int -> TestUtils.uninitialized() }
        whenever(cache.keyTransform(nullFInv)).thenCallRealMethod()
        mappedKeysCacheWithNull = cache.keyTransform(nullFInv)
    }

    @Test
    fun `contain cache in composed parents`() {
        val localCache = mappedKeysCache
        if (localCache !is ComposedCache<Int, Any>) {
            Assert.fail()
            return
        }

        assertThat(localCache.parents, IsEqual.equalTo(listOf<Cache<*, *>>(cache)))
    }

    // get
    @Test
    fun `map string value in get to int`() {
        runBlocking {
            // given we have a string
            whenever(cache.get("1")).then { "value" }

            // when we get the value
            val result = mappedKeysCache.get(1)

            Assert.assertEquals("value", result)
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
            whenever(cache.get("1")).then { "value" }

            // when we get the value from a map with exception throwing functions
            mappedKeysCacheWithError.get(1)

            // then an exception is thrown
        }
    }

    @Test(expected = TestException::class)
    fun `throw exception when get throws`() {
        runBlocking {
            // given we have a string
            whenever(cache.get("1")).then { throw TestException() }

            // when we get the value from a map
            mappedKeysCache.get(1)

            // then an exception is thrown
        }
    }

    @Test(expected = CancellationException::class)
    fun `throw exception when cancelled during get`() {
        runBlocking {
            // given we have a long running job
            whenever(cache.get("1")).then { runBlocking { delay(250) } }

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
            whenever(cache.set(anyString(), anyString())).then(Answers.RETURNS_MOCKS)

            // when we set the value
            mappedKeysCache.set(1, "1")

            // then it is converted to a string
            verify(cache).set("1", "1")
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
            whenever(cache.set(anyString(), anyString())).then { runBlocking { } }

            // when we get the value from a map with exception throwing functions
            mappedKeysCacheWithError.set(1, "1")

            // then an exception is thrown
        }
    }

    @Test(expected = TestException::class)
    fun `throw exception when set throws`() {
        runBlocking {
            // given we have a string
            whenever(cache.set(anyString(), anyString())).then { throw TestException() }

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
            whenever(cache.set(anyString(), anyString())).then { runBlocking { delay(250) } }

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
            whenever(cache.evict("1")).then { Unit }

            // when we get the value
            mappedKeysCache.evict(1)

            // then we return the value
            // Assert.assertEquals("value", result)
            verify(cache).evict("1")
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
            whenever(cache.evict("1")).then { Unit }

            // when we get the value
            mappedKeysCacheWithError.evict(1)

            // then we throw an exception
        }
    }

    @Test(expected = TestException::class)
    fun `throw exception when evict throws`() {
        runBlocking {
            // given value available in first cache only
            whenever(cache.evict("1")).then { throw TestException() }

            // when we get the value
            mappedKeysCache.evict(1)

            // then we throw an exception
        }
    }

    @Test(expected = CancellationException::class)
    fun `throw exception when cancelled during evict`() {
        runBlocking {
            // given we have a long running job
            whenever(cache.evict("1")).then { runBlocking { delay(250) } }

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
            whenever(cache.evictAll()).then { Unit }

            // when we evict the value
            mappedKeysCache.evictAll()

            // then we evictAll value
            verify(cache).evictAll()
        }
    }

    @Test
    fun `no exception when transform returns null during evictAll`() {
        runBlocking {
            // given evictAll is implemented
            whenever(cache.evictAll()).then { Unit }

            // when the mapping function returns null and we evictAll
            mappedKeysCacheWithNull.evictAll()

            // then no exception is thrown
        }
    }

    @Test
    fun `no exception when transform throws during evictAll`() {
        runBlocking {
            // given evictAll is implemented
            whenever(cache.evictAll()).then { Unit }

            // when the mapping function throws an exception and we evictAll
            mappedKeysCacheWithError.evictAll()

            // then no exception is thrown
        }
    }

    @Test(expected = TestException::class)
    fun `throw exception when evictAll throws`() {
        runBlocking {
            // given value available in first cache only
            whenever(cache.evictAll()).then { throw TestException() }

            // when we evictAll values
            mappedKeysCache.evictAll()

            // then we throw an exception
        }
    }

    @Test(expected = CancellationException::class)
    fun `throw exception when cancelled during evictAll`() {
        runBlocking {
            // given we have a long running job
            whenever(cache.evictAll()).then { runBlocking { delay(250) } }

            // when we cancel the job
            val job = async { mappedKeysCache.evictAll() }
            delay(50)
            job.cancel()

            // then an exception is thrown
            job.await()
        }
    }
}
