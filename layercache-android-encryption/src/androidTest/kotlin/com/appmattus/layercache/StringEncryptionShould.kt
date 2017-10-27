package com.appmattus.layercache

import android.annotation.SuppressLint
import android.os.Build
import android.preference.PreferenceManager
import android.support.annotation.RequiresApi
import android.support.test.InstrumentationRegistry
import com.appmattus.layercache.encryption.EncryptionFactory
import junitparams.JUnitParamsRunner
import junitparams.Parameters
import junitparams.naming.TestCaseName
import kotlinx.coroutines.experimental.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException
import org.junit.runner.RunWith
import java.security.KeyStore

@RunWith(JUnitParamsRunner::class)
class StringEncryptionShould {

    @get:Rule
    var thrown = ExpectedException.none()

    @Before
    fun setup() {
        // AndroidKeyStore only exists on API 18 and higher
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            val appContext = InstrumentationRegistry.getContext().applicationContext

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
                mappedCache.set("key", "value").await()

                // then the value is encrypted in the original cache and decrypted in the wrapped cache
                assertNotEquals("value", cache.get("key").await())
                assertEquals("value", mappedCache.get("key").await())
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


                assertNull(diskCache.get("key").await())
                assertNull(encryptedDiskCache.get("key").await())


                val valueFromNetwork = chained.get("key").await()
                assertEquals("value", valueFromNetwork)


                val valueFromDiskCache = encryptedDiskCache.get("key").await()
                assertEquals("value", valueFromDiskCache)


                val valueFromRawCacheEncrypted = diskCache.get("key").await()
                assertNotEquals(valueFromRawCacheEncrypted, valueFromDiskCache)
            }
        }
    }

    fun encryptor(): Array<Array<Any>> {
        val appContext = InstrumentationRegistry.getContext().applicationContext

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
            thrown.expect(IllegalStateException::class.java)

            val appContext = InstrumentationRegistry.getContext().applicationContext
            StringEncryption(appContext, EncryptionFactory.Mode.AES_CBC_PKCS7Padding_with_HMAC, "testCbc")
        }
    }

    @Test
    @SuppressLint("NewApi")
    fun throw_exception_when_sdk_too_low_for_gcm() {
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.JELLY_BEAN_MR2) {
            thrown.expect(IllegalStateException::class.java)

            val appContext = InstrumentationRegistry.getContext().applicationContext
            StringEncryption(appContext, EncryptionFactory.Mode.AES_GCM_NoPadding, "testGcm")
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
            val appContext = InstrumentationRegistry.getContext().applicationContext
            val newEncryptor = when (encryptor.toString()) {
                EncryptionFactory.Mode.AES_GCM_NoPadding.toString() -> StringEncryption(appContext, EncryptionFactory.Mode.AES_GCM_NoPadding, "testGcm")
                EncryptionFactory.Mode.AES_CBC_PKCS7Padding_with_HMAC.toString() -> StringEncryption(appContext, EncryptionFactory.Mode.AES_CBC_PKCS7Padding_with_HMAC, "testCbc")
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
            thrown.expect(Exception::class.java)


            val appContext = InstrumentationRegistry.getContext().applicationContext

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

            val newEncryptor = when (encryptor.toString()) {
                EncryptionFactory.Mode.AES_GCM_NoPadding.toString() -> StringEncryption(appContext, EncryptionFactory.Mode.AES_GCM_NoPadding, "testGcm")
                EncryptionFactory.Mode.AES_CBC_PKCS7Padding_with_HMAC.toString() -> StringEncryption(appContext, EncryptionFactory.Mode.AES_CBC_PKCS7Padding_with_HMAC, "testCbc")
                else -> throw IllegalStateException("Unimplemented")
            }
            newEncryptor.transform(encryptedData)

            // then the data is unable to be decrypted, and an exception is thrown
        }
    }
}
