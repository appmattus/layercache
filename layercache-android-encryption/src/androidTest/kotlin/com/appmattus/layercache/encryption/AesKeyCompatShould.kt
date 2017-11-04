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

package com.appmattus.layercache.encryption

import android.annotation.SuppressLint
import android.os.Build
import android.preference.PreferenceManager
import android.support.test.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException
import java.nio.charset.Charset
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec

class AesKeyCompatShould {

    @get:Rule
    var thrown = ExpectedException.none()

    private lateinit var aesKey: AesKeyCompat

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

            aesKey = AesKeyCompat(appContext, BlockMode.CBC, EncryptionPadding.PKCS7, false, IntegrityCheck.HMAC_SHA256)
        }
    }

    @Test
    @SuppressLint("NewApi")
    fun throw_exception_when_sdk_too_low() {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            thrown.expect(IllegalStateException::class.java)

            // when we create a new AesKeyCompat
            val appContext = InstrumentationRegistry.getContext().applicationContext
            AesKeyCompat(appContext, BlockMode.CBC, EncryptionPadding.PKCS7, false, IntegrityCheck.HMAC_SHA256)

            // then an exception is thrown
        }
    }

    @Test
    fun decrypt_text_using_second_requested_key() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            // given we retrieve a key
            val secretKey = aesKey.retrieveConfidentialityKey("testAes")

            // when we request the same key again
            val anotherSecretKey = aesKey.retrieveConfidentialityKey("testAes")

            // then they have the same private key - only possible to validate this by ensuring we can decrypt
            val encrypted1 = encrypt(secretKey, "hello world")
            assertEquals("hello world", decrypt(anotherSecretKey, encrypted1))
        }
    }

    @Test
    fun return_the_same_key_when_requested_twice() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2 && Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP_MR1) {

            // given we retrieve a key
            val secretKey = aesKey.retrieveConfidentialityKey("testAes")

            // when we request the same key again
            val anotherSecretKey = aesKey.retrieveConfidentialityKey("testAes")

            // then they have the same private key - only possible to validate this by ensuring we can decrypt
            assertEquals(secretKey.encoded.encodeBase64(), anotherSecretKey.encoded.encodeBase64())
        }
    }

    @Test
    fun use_rsa() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2 && Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP_MR1) {
            // given we have a key store
            val keyStore = KeyStore.getInstance("AndroidKeyStore")
            keyStore.load(null)

            // when we retrieve a key
            aesKey.retrieveConfidentialityKey("testAes")

            // then it contains an rsa key
            assertTrue(keyStore.containsAlias("testAes:rsa"))
        }
    }

    @Test
    fun not_use_rsa() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // given we have a key store
            val keyStore = KeyStore.getInstance("AndroidKeyStore")
            keyStore.load(null)

            // when we retrieve a key
            aesKey.retrieveConfidentialityKey("testAes")

            // then it does not contain an rsa key
            assertFalse(keyStore.containsAlias("testAes:rsa"))
        }
    }

    private fun encrypt(secretKey: SecretKey, value: String): String {
        val cipher = Cipher.getInstance("AES/CBC/PKCS7Padding")

        cipher.init(Cipher.ENCRYPT_MODE, secretKey)

        val cipherText = cipher.doFinal(value.toByteArray())

        return cipherText.encodeBase64() + ":" + cipher.iv.encodeBase64()
    }

    private fun decrypt(secretKey: SecretKey, mappedValue: String): String {
        val parts = mappedValue.split(":")
        val injectionVector = parts[1].decodeBase64()
        val cipherText = parts[0].decodeBase64()

        if (parts.size != 2) {
            throw IllegalArgumentException("Cannot parse iv:ciphertext")
        }

        val cipher = Cipher.getInstance("AES/CBC/PKCS7Padding")

        val spec = IvParameterSpec(injectionVector)

        cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)

        return cipher.doFinal(cipherText).toString(Charset.forName("UTF8"))
    }
}
