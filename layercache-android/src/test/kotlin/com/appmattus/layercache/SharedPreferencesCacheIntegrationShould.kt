@file:Suppress("IllegalIdentifier")

package com.appmattus.layercache

import kotlinx.coroutines.experimental.runBlocking
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class SharedPreferencesCacheIntegrationShould {

    private lateinit var stringCache: Cache<String, String>
    private lateinit var intCache: Cache<String, Int>

    @Before
    fun before() {
        stringCache = SharedPreferencesCache(RuntimeEnvironment.application, "test").withString()
        intCache = SharedPreferencesCache(RuntimeEnvironment.application, "test").withInt()
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
            stringCache.set("key", "value").await()

            // when we retrieve a value
            val result = stringCache.get("key").await()

            // then it is returned
            Assert.assertEquals("value", result)
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun return_value_when_cache_has_value_3() {
        runBlocking {
            val cache = SharedPreferencesCache(RuntimeEnvironment.application, "test").withString()

            // given we have a cache with a value
            cache.set("key", TestUtils.uninitialized()).await()

            // then exception is thrown
        }
    }

    @Test
    fun return_value_when_cache_has_value_4() {
        runBlocking {
            val cache = SharedPreferencesCache(RuntimeEnvironment.application, "test").withInt()


            // given we have a cache with a value
            cache.set("key", 5).await()

            // when we retrieve a value
            val result = cache.get("key").await()

            // then it is returned
            Assert.assertEquals(5, result)
        }
    }


    @Test
    fun return_null_when_the_cache_is_empty() {
        runBlocking {
            // given we have an empty cache, integratedCache

            // when we retrieve a value
            val result = stringCache.get("key").await()

            // then it is null
            Assert.assertNull(result)
        }
    }

    @Test
    fun return_value_when_cache_has_value() {
        runBlocking {
            // given we have a cache with a value
            stringCache.set("key", "value").await()

            // when we retrieve a value
            val result = stringCache.get("key").await()

            // then it is returned
            Assert.assertEquals("value", result)
        }
    }

    @Test
    fun return_null_when_the_key_has_been_evicted() {
        runBlocking {
            // given we have a cache with a value
            stringCache.set("key", "value").await()

            // when we evict the value
            stringCache.evict("key").await()

            // then the value is null
            Assert.assertNull(stringCache.get("key").await())
        }
    }
}