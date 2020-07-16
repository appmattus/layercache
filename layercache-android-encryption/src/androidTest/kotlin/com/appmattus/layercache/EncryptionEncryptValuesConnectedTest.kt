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

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.security.KeyStore

@RunWith(AndroidJUnit4::class)
class EncryptionEncryptValuesConnectedTest {

    private val appContext = ApplicationProvider.getApplicationContext<Context>()
    private val cache = MapCache()
    private val encrypted = cache.encryptValues(appContext)

    @Before
    fun clearKeys() {
        val keyStore = KeyStore.getInstance("AndroidKeyStore")
        keyStore.load(null)
        keyStore.aliases().toList().forEach { keyStore.deleteEntry(it) }

        val sharedPreferences = appContext.getSharedPreferences("${appContext.packageName}_preferences", Context.MODE_PRIVATE)
        sharedPreferences.edit().clear().commit()
    }

    @Test
    fun encrypts_and_decrypts_with_keys_in_memory() {
        runBlocking {
            // when we set a value
            encrypted.set("key", "hello")

            // then retrieving returns the original
            assertEquals("hello", encrypted.get("key"))
        }
    }

    @Test
    fun encrypts_and_decrypts_with_keys_reloaded() {
        runBlocking {
            // when we set a value
            encrypted.set("key", "hello")

            // then original value returned
            assertEquals("hello", cache.encryptValues(appContext).get("key"))
        }
    }

    @Test
    fun fails_decryption_when_key_and_value_keys_corrupted() {
        runBlocking {
            // when we set a value
            encrypted.set("key", "hello")

            // and corrupt the key and value keys in shared preferences
            val sharedPreferences = appContext.getSharedPreferences("${appContext.packageName}_preferences", Context.MODE_PRIVATE)
            sharedPreferences.all.forEach {
                sharedPreferences.edit().putString(it.key, it.value.toString().reversed()).apply()
            }

            // then no value is returned
            assertNull(cache.encryptValues(appContext).get("key"))
        }
    }

    @Test
    fun fails_decryption_when_all_keys_removed() {
        runBlocking {
            // when we set a value
            encrypted.set("key", "hello")

            // and clear all stored keys
            clearKeys()

            // then no value is returned
            assertNull(cache.encryptValues(appContext).get("key"))
        }
    }

    @Test
    fun value_is_encrypted_in_backing_map() {
        runBlocking {
            // when we set a value
            encrypted.set("key", "hello")

            // then the value is encrypted in the backing map
            assertNotEquals("hello", cache.get("key"))
        }
    }

    @Test
    fun non_existent_key_returns_null() {
        runBlocking {
            // when we get a non-existent value
            val result = encrypted.get("key")

            // then the result is null
            assertNull(result)
        }
    }
}
