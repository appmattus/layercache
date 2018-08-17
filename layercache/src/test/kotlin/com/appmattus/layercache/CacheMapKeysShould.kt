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

import kotlinx.coroutines.experimental.CancellationException
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.runBlocking
import org.hamcrest.core.IsEqual
import org.hamcrest.core.StringStartsWith
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException
import org.mockito.Answers
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.anyString
import org.mockito.MockitoAnnotations
import java.util.concurrent.TimeUnit

class CacheMapKeysShould {

    @get:Rule
    var thrown: ExpectedException = ExpectedException.none()

    @Mock
    private lateinit var cache: AbstractCache<String, Any>

    private lateinit var mappedKeysCache: Cache<Int, Any>
    private lateinit var mappedKeysCacheWithError: Cache<Int, Any>
    private lateinit var mappedKeysCacheWithNull: Cache<Int, Any>

    @Before
    fun before() {
        MockitoAnnotations.initMocks(this)
        val fInv: (Int) -> String = { int: Int -> int.toString() }
        Mockito.`when`(cache.keyTransform(fInv)).thenCallRealMethod()
        mappedKeysCache = cache.keyTransform(fInv)

        val errorFInv: (Int) -> String = { _: Int -> throw TestException() }
        Mockito.`when`(cache.keyTransform(errorFInv)).thenCallRealMethod()
        mappedKeysCacheWithError = cache.keyTransform(errorFInv)

        val nullFInv: (Int) -> String = { _: Int -> TestUtils.uninitialized() }
        Mockito.`when`(cache.keyTransform(nullFInv)).thenCallRealMethod()
        mappedKeysCacheWithNull = cache.keyTransform(nullFInv)

    }

    @Test
    fun `contain cache in composed parents`() {
        val localCache = mappedKeysCache
        if (localCache !is ComposedCache<Int, Any>) {
            Assert.fail()
            return
        }

        Assert.assertThat(localCache.parents, IsEqual.equalTo(listOf<Cache<*, *>>(cache)))
    }

    // get
    @Test
    fun `map string value in get to int`() {
        runBlocking {
            // given we have a string
            Mockito.`when`(cache.get("1")).then { async(CommonPool) { "value" } }

            // when we get the value
            val result = mappedKeysCache.get(1).await()

            Assert.assertEquals("value", result)
        }
    }

    @Test
    fun `throw exception when transform returns null during get`() {
        runBlocking {
            // expect exception
            thrown.expect(IllegalArgumentException::class.java)
            thrown.expectMessage(StringStartsWith("Required value was null"))

            // when the mapping function returns null
            mappedKeysCacheWithNull.get(1).await()
        }
    }

    @Test(expected = TestException::class)
    fun `throw exception when transform throws during get`() {
        runBlocking {
            // given we have a string
            Mockito.`when`(cache.get("1")).then { async(CommonPool) { "value" } }

            // when we get the value from a map with exception throwing functions
            mappedKeysCacheWithError.get(1).await()

            // then an exception is thrown
        }
    }

    @Test(expected = TestException::class)
    fun `throw exception when get throws`() {
        runBlocking {
            // given we have a string
            Mockito.`when`(cache.get("1")).then { async(CommonPool) { throw TestException() } }

            // when we get the value from a map
            mappedKeysCache.get(1).await()

            // then an exception is thrown
        }
    }

    @Test(expected = CancellationException::class)
    fun `throw exception when cancelled during get`() {
        runBlocking {
            // given we have a long running job
            Mockito.`when`(cache.get("1")).then { async(CommonPool) { delay(250, TimeUnit.MILLISECONDS) } }

            // when we canel the job
            val job = mappedKeysCache.get(1)
            delay(50, TimeUnit.MILLISECONDS)
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
            Mockito.`when`(cache.set(anyString(), anyString())).then(Answers.RETURNS_MOCKS)

            // when we set the value
            mappedKeysCache.set(1, "1").await()

            // then it is converted to a string
            Mockito.verify(cache).set("1", "1")
        }
    }

    @Test
    fun `throw exception when transform returns null during set`() {
        runBlocking {
            // expect exception
            thrown.expect(IllegalArgumentException::class.java)
            thrown.expectMessage(StringStartsWith("Required value was null"))

            // when the mapping function returns null
            mappedKeysCacheWithNull.set(1, "value").await()
        }
    }

    @Test(expected = TestException::class)
    fun `throw exception when transform throws during set`() {
        runBlocking {
            // given we have a string
            Mockito.`when`(cache.set(anyString(), anyString())).then { async(CommonPool) { } }

            // when we get the value from a map with exception throwing functions
            mappedKeysCacheWithError.set(1, "1").await()

            // then an exception is thrown
        }
    }

    @Test(expected = TestException::class)
    fun `throw exception when set throws`() {
        runBlocking {
            // given we have a string
            Mockito.`when`(cache.set(anyString(), anyString())).then { async(CommonPool) { throw TestException() } }

            // when we set the value from a map
            mappedKeysCache.set(1, "1").await()

            // then an exception is thrown
        }
    }

    // throw exception when cancelled during set
    @Test(expected = CancellationException::class)
    fun `throw exception when cancelled during set`() {
        runBlocking {
            // given we have a long running job
            Mockito.`when`(cache.set(anyString(), anyString())).then { async(CommonPool) { delay(250, TimeUnit.MILLISECONDS) } }

            // when we canel the job
            val job = mappedKeysCache.set(1, "1")
            delay(50, TimeUnit.MILLISECONDS)
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
            Mockito.`when`(cache.evict("1")).then { async(CommonPool) {} }

            // when we get the value
            mappedKeysCache.evict(1).await()

            // then we return the value
            //Assert.assertEquals("value", result)
            Mockito.verify(cache).evict("1")
        }
    }

    @Test
    fun `throw exception when transform returns null during evict`() {
        runBlocking {
            // expect exception
            thrown.expect(IllegalArgumentException::class.java)
            thrown.expectMessage(StringStartsWith("Required value was null"))

            // when the mapping function returns null
            mappedKeysCacheWithNull.evict(1).await()
        }
    }

    @Test(expected = TestException::class)
    fun `throw exception when transform throws during evict`() {
        runBlocking {
            // given value available in first cache only
            Mockito.`when`(cache.evict("1")).then { async(CommonPool) { } }

            // when we get the value
            mappedKeysCacheWithError.evict(1).await()

            // then we throw an exception
        }
    }

    @Test(expected = TestException::class)
    fun `throw exception when evict throws`() {
        runBlocking {
            // given value available in first cache only
            Mockito.`when`(cache.evict("1")).then { async(CommonPool) { throw TestException() } }

            // when we get the value
            mappedKeysCache.evict(1).await()

            // then we throw an exception
        }
    }

    @Test(expected = CancellationException::class)
    fun `throw exception when cancelled during evict`() {
        runBlocking {
            // given we have a long running job
            Mockito.`when`(cache.evict("1")).then { async(CommonPool) { delay(250, TimeUnit.MILLISECONDS) } }

            // when we canel the job
            val job = mappedKeysCache.evict(1)
            delay(50, TimeUnit.MILLISECONDS)
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
            Mockito.`when`(cache.evictAll()).then { async(CommonPool) {} }

            // when we evict the value
            mappedKeysCache.evictAll().await()

            // then we evictAll value
            Mockito.verify(cache).evictAll()
        }
    }

    @Test
    fun `no exception when transform returns null during evictAll`() {
        runBlocking {
            // given evictAll is implemented
            Mockito.`when`(cache.evictAll()).then { async(CommonPool) { } }

            // when the mapping function returns null and we evictAll
            mappedKeysCacheWithNull.evictAll().await()

            // then no exception is thrown
        }
    }

    @Test
    fun `no exception when transform throws during evictAll`() {
        runBlocking {
            // given evictAll is implemented
            Mockito.`when`(cache.evictAll()).then { async(CommonPool) { } }

            // when the mapping function throws an exception and we evictAll
            mappedKeysCacheWithError.evictAll().await()

            // then no exception is thrown
        }
    }

    @Test(expected = TestException::class)
    fun `throw exception when evictAll throws`() {
        runBlocking {
            // given value available in first cache only
            Mockito.`when`(cache.evictAll()).then { async(CommonPool) { throw TestException() } }

            // when we evictAll values
            mappedKeysCache.evictAll().await()

            // then we throw an exception
        }
    }

    @Test(expected = CancellationException::class)
    fun `throw exception when cancelled during evictAll`() {
        runBlocking {
            // given we have a long running job
            Mockito.`when`(cache.evictAll()).then { async(CommonPool) { delay(250, TimeUnit.MILLISECONDS) } }

            // when we canel the job
            val job = mappedKeysCache.evictAll()
            delay(50, TimeUnit.MILLISECONDS)
            job.cancel()

            // then an exception is thrown
            job.await()
        }
    }
}