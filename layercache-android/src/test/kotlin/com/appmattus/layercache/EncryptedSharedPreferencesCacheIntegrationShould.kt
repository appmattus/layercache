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

@file:Suppress("IllegalIdentifier")

package com.appmattus.layercache

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.appmattus.layercache.keystore.RobolectricKeyStore
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE, sdk = [22, 28])
class EncryptedSharedPreferencesCacheIntegrationShould {

    private val context = ApplicationProvider.getApplicationContext<Context>()

    private val masterKey = MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()

    private val sharedPreferences = EncryptedSharedPreferences.create(
        context,
        "test",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    private lateinit var stringCache: Cache<String, String>
    private lateinit var intCache: Cache<String, Int>

    @Before
    fun before() {
        stringCache = sharedPreferences.asStringCache()
        intCache = sharedPreferences.asIntCache()

        runBlocking {
            stringCache.evictAll()
            intCache.evictAll()
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
            assertEquals("value", result)
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun return_value_when_cache_has_value_3() {
        runBlocking {
            // given we have a cache with a value
            stringCache.set("key", TestUtils.uninitialized())

            // then exception is thrown
        }
    }

    @Test
    fun return_value_when_cache_has_value_4() {
        runBlocking {
            // given we have a cache with a value
            intCache.set("key", 5)

            // when we retrieve a value
            val result = intCache.get("key")

            // then it is returned
            assertEquals(5, result)
        }
    }

    @Test
    fun return_null_when_the_cache_is_empty() {
        runBlocking {
            // given we have an empty cache, integratedCache

            // when we retrieve a value
            val result = stringCache.get("key")

            // then it is null
            assertNull(result)
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
            assertEquals("value", result)
        }
    }

    @Test
    fun return_null_when_the_key_has_been_evicted() {
        runBlocking {
            // given we have a cache with a value
            stringCache.set("key", "value")

            // when we evict the value
            stringCache.evict("key")

            // then the value is null
            assertNull(stringCache.get("key"))
        }
    }

    companion object {
        @JvmStatic
        @BeforeClass
        fun beforeClass() {
            RobolectricKeyStore.setup
        }
    }
}
