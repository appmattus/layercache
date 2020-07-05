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

import android.annotation.SuppressLint
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.preference.PreferenceManager
import androidx.test.platform.app.InstrumentationRegistry
import com.appmattus.layercache.encryption.EncryptionFactory
import junitparams.JUnitParamsRunner
import junitparams.Parameters
import junitparams.naming.TestCaseName
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.security.KeyStore

@RunWith(JUnitParamsRunner::class)
class StringEncryptionShould {

    @Before
    fun setup() {
        // AndroidKeyStore only exists on API 18 and higher
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            val appContext = InstrumentationRegistry.getInstrumentation().context.applicationContext

            // cleanup old keys
            val keyStore = KeyStore.getInstance("AndroidKeyStore")
            keyStore.load(null)
            keyStore.aliases().toList().forEach { keyStore.deleteEntry(it) }

            // cleanup old preferences
            val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(appContext)
            sharedPrefs.all.forEach { entry: Map.Entry<String, Any?> ->
                sharedPrefs.edit().remove(entry.key).apply()
            }
        }
    }

    @Test
    @Parameters(method = "encryptor")
    @TestCaseName("{method}_with_{params}")
    fun encrypt_string(encryptor: StringEncryption) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            assertNotEquals("hello world", encryptor.inverseTransform("hello world"))
        }
    }

    @Test
    @Parameters(method = "encryptor")
    @TestCaseName("{method}_with_{params}")
    fun decrypt_back_to_original_string(encryptor: StringEncryption) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            assertEquals("hello world", encryptor.transform(encryptor.inverseTransform("hello world")))
        }
    }

    @Test
    @Parameters(method = "encryptor")
    @TestCaseName("{method}_with_{params}")
    fun encrypt_differently_each_time(encryptor: StringEncryption) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            val conversion1 = encryptor.inverseTransform("hello world")
            val conversion2 = encryptor.inverseTransform("hello world")
            val conversion3 = encryptor.inverseTransform("hello world")

            assertTrue(conversion1 != conversion2)
            assertTrue(conversion1 != conversion3)
            assertTrue(conversion2 != conversion3)
        }
    }

    @Test
    @Parameters(method = "encryptor")
    @TestCaseName("{method}_with_{params}")
    fun map_encrypted_values(encryptor: StringEncryption) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            runBlocking {
                // given we have a cache where we map the values using encryption
                val cache = Cache.createLruCache<String, String>(10)
                val mappedCache = cache.valueTransform(encryptor::transform, encryptor::inverseTransform)

                // when we set a value and retrieve it
                mappedCache.set("key", "value")

                // then the value is encrypted in the original cache and decrypted in the wrapped cache
                assertNotEquals("value", cache.get("key"))
                assertEquals("value", mappedCache.get("key"))
            }
        }
    }

    @Test
    @Parameters(method = "encryptor")
    @TestCaseName("{method}_with_{params}")
    fun encrypt_values_when_value_retrieved_from_composed_cache(encryptor: StringEncryption) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            runBlocking {

                val networkCache = Cache.createLruCache<String, String>(10)
                networkCache.set("key", "value")

                val diskCache = Cache.createLruCache<String, String>(10)
                val encryptedDiskCache = diskCache.valueTransform(encryptor::transform, encryptor::inverseTransform)

                val chained = encryptedDiskCache.compose(networkCache)

                assertNull(diskCache.get("key"))
                assertNull(encryptedDiskCache.get("key"))

                val valueFromNetwork = chained.get("key")
                assertEquals("value", valueFromNetwork)

                val valueFromDiskCache = encryptedDiskCache.get("key")
                assertEquals("value", valueFromDiskCache)

                val valueFromRawCacheEncrypted = diskCache.get("key")
                assertNotEquals(valueFromRawCacheEncrypted, valueFromDiskCache)
            }
        }
    }

    fun encryptor(): Array<Array<Any>> {
        val appContext = InstrumentationRegistry.getInstrumentation().context.applicationContext

        val params = mutableListOf<Array<Any>>()

        // API 18
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            params.add(arrayOf(StringEncryption(appContext, EncryptionFactory.Mode.AES_CBC_PKCS7Padding_with_HMAC, "testCbc")))
        }

        // API 19
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            params.add(arrayOf(StringEncryption(appContext, EncryptionFactory.Mode.AES_GCM_NoPadding, "testGcm")))
        }

        return params.toTypedArray()
    }

    @Test
    @SuppressLint("NewApi")
    fun throw_exception_when_sdk_too_low_for_cbc() {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            assertThrows(IllegalStateException::class.java) {
                val appContext = InstrumentationRegistry.getInstrumentation().context.applicationContext
                StringEncryption(appContext, EncryptionFactory.Mode.AES_CBC_PKCS7Padding_with_HMAC, "testCbc")
            }
        }
    }

    @Test
    @SuppressLint("NewApi")
    fun throw_exception_when_sdk_too_low_for_gcm() {
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.JELLY_BEAN_MR2) {
            assertThrows(IllegalStateException::class.java) {
                val appContext = InstrumentationRegistry.getInstrumentation().context.applicationContext
                StringEncryption(appContext, EncryptionFactory.Mode.AES_GCM_NoPadding, "testGcm")
            }
        }
    }

    @Test
    @Parameters(method = "encryptor")
    @TestCaseName("{method}_with_{params}")
    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    fun decrypt_using_new_encryptor_using_same_key(encryptor: StringEncryption) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            // given we encrypt some data
            val encryptedData = encryptor.inverseTransform("hello world")

            // when we decrypt using a new encryptor using the same alias (as the keys will be the same)
            val appContext = InstrumentationRegistry.getInstrumentation().context.applicationContext
            val newEncryptor = when (encryptor.toString()) {
                EncryptionFactory.Mode.AES_GCM_NoPadding.toString() -> StringEncryption(
                    appContext,
                    EncryptionFactory.Mode.AES_GCM_NoPadding,
                    "testGcm"
                )
                EncryptionFactory.Mode.AES_CBC_PKCS7Padding_with_HMAC.toString() -> StringEncryption(
                    appContext,
                    EncryptionFactory.Mode.AES_CBC_PKCS7Padding_with_HMAC,
                    "testCbc"
                )
                else -> throw IllegalStateException("Unimplemented")
            }
            val decryptedData = newEncryptor.transform(encryptedData)

            // then the data is correctly decrypted
            assertEquals("hello world", decryptedData)
        }
    }

    @Test
    @Parameters(method = "encryptor")
    @TestCaseName("{method}_with_{params}")
    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    fun decrypt_using_new_encryptor_using_new_key(encryptor: StringEncryption) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            val appContext = InstrumentationRegistry.getInstrumentation().context.applicationContext

            // given we encrypt some data
            val encryptedData = encryptor.inverseTransform("hello world")

            // cleanup old keys
            val keyStore = KeyStore.getInstance("AndroidKeyStore")
            keyStore.load(null)
            keyStore.aliases().toList().forEach { keyStore.deleteEntry(it) }

            // cleanup old preferences
            val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(appContext)
            sharedPrefs.all.forEach { entry: Map.Entry<String, Any?> ->
                sharedPrefs.edit().remove(entry.key).apply()
            }

            // when we decrypt using a new encryptor using the same alias but different keys
            assertThrows(Exception::class.java) {

                val newEncryptor = when (encryptor.toString()) {
                    EncryptionFactory.Mode.AES_GCM_NoPadding.toString() -> StringEncryption(
                        appContext,
                        EncryptionFactory.Mode.AES_GCM_NoPadding,
                        "testGcm"
                    )
                    EncryptionFactory.Mode.AES_CBC_PKCS7Padding_with_HMAC.toString() -> StringEncryption(
                        appContext,
                        EncryptionFactory.Mode.AES_CBC_PKCS7Padding_with_HMAC,
                        "testCbc"
                    )
                    else -> throw IllegalStateException("Unimplemented")
                }
                newEncryptor.transform(encryptedData)
            }

            // then the data is unable to be decrypted, and an exception is thrown
        }
    }
}
