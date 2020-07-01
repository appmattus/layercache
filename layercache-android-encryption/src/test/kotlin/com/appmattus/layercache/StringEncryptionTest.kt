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
import android.content.Context
import android.os.Build
import androidx.preference.PreferenceManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.appmattus.layercache.encryption.EncryptionFactory
import com.appmattus.layercache.keystore.RobolectricKeyStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.security.KeyStore

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE, sdk = [17, 18, 19, 28])
class StringEncryptionTest {

    private val appContext = ApplicationProvider.getApplicationContext<Context>()

    @Before
    fun setup() {
        // AndroidKeyStore only exists on API 18 and higher
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            // cleanup old keys
            val keyStore = KeyStore.getInstance("AndroidKeyStore")
            keyStore.load(null)
            keyStore.aliases().asSequence().forEach { keyStore.deleteEntry(it) }

            // cleanup old preferences
            val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(appContext)
            sharedPrefs.all.forEach { entry: Map.Entry<String, Any?> ->
                sharedPrefs.edit().remove(entry.key).apply()
            }
        }
    }

    /*// API 18
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
        params.add(arrayOf(StringEncryption(appContext, EncryptionFactory.Mode.AES_CBC_PKCS7Padding_with_HMAC, "testCbc")))
    }
    // API 19
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
        params.add(arrayOf(StringEncryption(appContext, EncryptionFactory.Mode.AES_GCM_NoPadding, "testGcm")))
    }*/

    @Test
    @Config(sdk = [18, 19, 28])
    fun encrypt_string_with_AES_CBC_PKCS7Padding_with_HMAC() {
        val encryptor = StringEncryption(appContext, EncryptionFactory.Mode.AES_CBC_PKCS7Padding_with_HMAC, "testCbc")

        assertNotEquals("hello world", encryptor.inverseTransform("hello world"))
    }

    @Test
    @Config(sdk = [19, 28])
    fun encrypt_string_with_AES_GCM_NoPadding() {
        val encryptor = StringEncryption(appContext, EncryptionFactory.Mode.AES_GCM_NoPadding, "testGcm")

        assertNotEquals("hello world", encryptor.inverseTransform("hello world"))
    }

    @Test
    @Config(sdk = [18, 19, 28])
    fun decrypt_back_to_original_string_with_AES_CBC_PKCS7Padding_with_HMAC() {
        val encryptor = StringEncryption(appContext, EncryptionFactory.Mode.AES_CBC_PKCS7Padding_with_HMAC, "testCbc")

        assertEquals("hello world", encryptor.transform(encryptor.inverseTransform("hello world")))
    }

    @Test
    @Config(sdk = [19, 28])
    fun decrypt_back_to_original_string_with_AES_GCM_NoPadding() {
        val encryptor = StringEncryption(appContext, EncryptionFactory.Mode.AES_GCM_NoPadding, "testGcm")

        assertEquals("hello world", encryptor.transform(encryptor.inverseTransform("hello world")))
    }

    /*@Test
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
    }*/

    fun encryptor(): Array<Array<Any>> {
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
    @Config(sdk = [17, 18])
    fun throw_exception_when_sdk_too_low_for_gcm() {
        assertThrows(IllegalStateException::class.java) {
            val appContext = ApplicationProvider.getApplicationContext<Context>()
            StringEncryption(appContext, EncryptionFactory.Mode.AES_GCM_NoPadding, "testGcm")
        }
    }

    /*@Test
    @Parameters(method = "encryptor")
    @TestCaseName("{method}_with_{params}")
    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    fun decrypt_using_new_encryptor_using_same_key(encryptor: StringEncryption) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            // given we encrypt some data
            val encryptedData = encryptor.inverseTransform("hello world")

            // when we decrypt using a new encryptor using the same alias (as the keys will be the same)
            val appContext = ApplicationProvider.getApplicationContext<Context>()
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
            val appContext = ApplicationProvider.getApplicationContext<Context>()

            // given we encrypt some data
            val encryptedData = encryptor.inverseTransform("hello world")

            // cleanup old keys
            val keyStore = KeyStore.getInstance("AndroidKeyStore")
            keyStore.load(null)
            keyStore.aliases().asSequence().forEach { keyStore.deleteEntry(it) }

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
    }*/

    companion object {
        @JvmStatic
        @BeforeClass
        fun beforeClass() {
            RobolectricKeyStore.setup
        }
    }
}
