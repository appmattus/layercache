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

@file:Suppress("IllegalIdentifier")

package com.appmattus.layercache

import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class SharedPreferencesCacheIntegrationShould {

    private lateinit var stringCache: Cache<String, String>
    private lateinit var intCache: Cache<String, Int>

    @Before
    fun before() {
        stringCache = SharedPreferencesCache(ApplicationProvider.getApplicationContext(), "test").withString()
        intCache = SharedPreferencesCache(ApplicationProvider.getApplicationContext(), "test").withInt()
    }

    @After
    fun after() {
        runBlocking {
            stringCache.evictAll().await()
            intCache.evictAll().await()
        }
    }

    @Test
    fun return_value_when_cache_has_value_2() {
        runBlocking {
            // given we have a cache with a value
            stringCache.set("key", "value")

            // when we retrieve a value
            val result = stringCache.get("key")

            // then it is returned
            Assert.assertEquals("value", result)
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun return_value_when_cache_has_value_3() {
        runBlocking {
            val cache = SharedPreferencesCache(ApplicationProvider.getApplicationContext(), "test").withString()

            // given we have a cache with a value
            cache.set("key", TestUtils.uninitialized())

            // then exception is thrown
        }
    }

    @Test
    fun return_value_when_cache_has_value_4() {
        runBlocking {
            val cache = SharedPreferencesCache(ApplicationProvider.getApplicationContext(), "test").withInt()


            // given we have a cache with a value
            cache.set("key", 5)

            // when we retrieve a value
            val result = cache.get("key")

            // then it is returned
            Assert.assertEquals(5, result)
        }
    }


    @Test
    fun return_null_when_the_cache_is_empty() {
        runBlocking {
            // given we have an empty cache, integratedCache

            // when we retrieve a value
            val result = stringCache.get("key")

            // then it is null
            Assert.assertNull(result)
        }
    }

    @Test
    fun return_value_when_cache_has_value() {
        runBlocking {
            // given we have a cache with a value
            stringCache.set("key", "value")

            // when we retrieve a value
            val result = stringCache.get("key")

            // then it is returned
            Assert.assertEquals("value", result)
        }
    }

    @Test
    fun return_null_when_the_key_has_been_evicted() {
        runBlocking {
            // given we have a cache with a value
            stringCache.set("key", "value")

            // when we evict the value
            stringCache.evict("key").await()

            // then the value is null
            Assert.assertNull(stringCache.get("key"))
        }
    }
}
