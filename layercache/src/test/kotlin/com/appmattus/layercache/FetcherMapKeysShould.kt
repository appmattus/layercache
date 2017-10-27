/**
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
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import java.util.concurrent.TimeUnit

class FetcherMapKeysShould {

    @get:Rule
    var thrown = ExpectedException.none()

    @Mock
    private lateinit var cache: AbstractFetcherCache<String, Any>

    @Mock
    private lateinit var function: (Int) -> String

    private lateinit var mappedKeysCache: Fetcher<Int, Any>

    @Before
    fun before() {
        MockitoAnnotations.initMocks(this)

        Mockito.`when`(cache.keyTransform(MockitoKotlin.any(function::class.java))).thenCallRealMethod()

        mappedKeysCache = cache.keyTransform(function)

        Mockito.verify(cache, Mockito.atLeastOnce()).keyTransform(MockitoKotlin.any(function::class.java))
    }

    @Test
    fun `contain cache in composed parents`() {
        val localCache = mappedKeysCache
        if (!(localCache is ComposedCache<*, *>)) {
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
            transformConvertsIntToString()
            Mockito.`when`(cache.get("1")).then { async(CommonPool) { "value" } }

            // when we get the value
            val result = mappedKeysCache.get(1).await()

            Assert.assertEquals("value", result)
        }
    }

    @Test
    fun `throw exception when transform returns null during get`() {
        runBlocking {
            // given transform returns null
            Mockito.`when`(function.invoke(Mockito.anyInt())).then { TestUtils.uninitialized() }

            // expect exception
            thrown.expect(IllegalArgumentException::class.java)
            thrown.expectMessage(StringStartsWith("Required value was null"))

            // when the mapping function returns null
            mappedKeysCache.get(1).await()
        }
    }

    @Test(expected = TestException::class)
    fun `throw exception when transform throws during get`() {
        runBlocking {
            // given we have a string
            Mockito.`when`(function.invoke(Mockito.anyInt())).then { throw TestException() }

            //Mockito.`when`(cache.get("1")).then { async(CommonPool) { "value" } }

            // when we get the value from a map with exception throwing functions
            mappedKeysCache.get(1).await()

            // then an exception is thrown
        }
    }

    @Test(expected = TestException::class)
    fun `throw exception when get throws`() {
        runBlocking {
            // given we have a string
            transformConvertsIntToString()
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
            transformConvertsIntToString()
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
    fun `not interact with parent set`() {
        runBlocking {
            // when we set the value
            @Suppress("DEPRECATION")
            mappedKeysCache.set(1, "1").await()

            // then the parent cache is not called
            Mockito.verifyNoMoreInteractions(cache)
        }
    }

    @Test
    fun `not interact with transform during set`() {
        runBlocking {
            // when we set the value
            @Suppress("DEPRECATION")
            mappedKeysCache.set(1, "1").await()

            // then the parent cache is not called
            Mockito.verifyZeroInteractions(function)
        }
    }

    // evict
    @Test
    fun `not interact with parent evict`() {
        runBlocking {
            // when we set the value
            @Suppress("DEPRECATION")
            mappedKeysCache.evict(1).await()

            // then the parent cache is not called
            Mockito.verifyNoMoreInteractions(cache)
        }
    }

    @Test
    fun `not interact with transform during evict`() {
        runBlocking {
            // when we set the value
            @Suppress("DEPRECATION")
            mappedKeysCache.evict(1).await()

            // then the parent cache is not called
            Mockito.verifyZeroInteractions(function)
        }
    }

    private fun transformConvertsIntToString() {
        Mockito.`when`(function.invoke(Mockito.anyInt())).then { it.getArgument<Int>(0).toString() }
    }
}