/*
 * Copyright 2021 Appmattus Limited
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
import android.os.Build
import com.google.crypto.tink.Aead
import com.google.crypto.tink.DeterministicAead
import com.google.crypto.tink.KeysetHandle
import com.google.crypto.tink.aead.AesGcmKeyManager
import com.google.crypto.tink.daead.AesSivKeyManager
import com.google.crypto.tink.daead.DeterministicAeadConfig
import com.google.crypto.tink.hybrid.HybridConfig
import com.google.crypto.tink.integration.android.AndroidKeysetManager
import com.google.crypto.tink.integration.android.AndroidKeystoreKmsClient
import com.google.crypto.tink.signature.SignatureConfig
import com.google.crypto.tink.subtle.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.GeneralSecurityException
import java.security.SecureRandom

fun Cache<String, String>.encrypt(
    context: Context,
    fileName: String = "${context.packageName}_preferences",
    keystoreAlias: String = Encryption.DEFAULT_MASTER_KEY_ALIAS
) = Encryption(context, fileName, keystoreAlias).encrypt(this)

fun <Key : Any> Cache<Key, String>.encryptValues(
    context: Context,
    fileName: String = "${context.packageName}_preferences",
    keystoreAlias: String = Encryption.DEFAULT_MASTER_KEY_ALIAS
) = Encryption(context, fileName, keystoreAlias).encryptValues(this)

private class Encryption(context: Context, private val fileName: String, keystoreAlias: String) {

    private val deterministicAead: DeterministicAead by lazy {
        val daeadKeysetHandle: KeysetHandle = AndroidKeysetManager.Builder()
            .withKeyTemplate(AesSivKeyManager.aes256SivTemplate())
            .withSharedPref(context, KEY_KEYSET_ALIAS, fileName)
            .withMasterKeyUri(AndroidKeystoreKmsClient.PREFIX + keystoreAlias)
            .build().keysetHandle

        daeadKeysetHandle.getPrimitive(DeterministicAead::class.java)
    }

    private val aead: Aead by lazy {
        val aeadKeysetHandle: KeysetHandle = AndroidKeysetManager.Builder()
            .withKeyTemplate(AesGcmKeyManager.aes256GcmTemplate())
            .withSharedPref(context, VALUE_KEYSET_ALIAS, fileName)
            .withMasterKeyUri(AndroidKeystoreKmsClient.PREFIX + keystoreAlias)
            .build().keysetHandle

        aeadKeysetHandle.getPrimitive(Aead::class.java)
    }

    init {
        DeterministicAeadConfig.register()
        HybridConfig.register()
        SignatureConfig.register()
    }

    private fun encryptKey(key: String): EncryptedKey {
        return try {
            val encryptedKeyBytes: ByteArray = deterministicAead.encryptDeterministically(
                key.toByteArray(Charsets.UTF_8),
                fileName.toByteArray()
            )
            EncryptedKey(encryptedKeyBytes)
        } catch (ex: GeneralSecurityException) {
            throw SecurityException("Could not encrypt key. " + ex.message, ex)
        }
    }

    private fun encryptValue(value: String, associatedData: ByteArray): String {
        val data = associatedData.takeIf { Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP }
        return Base64.encode(aead.encrypt(value.toByteArray(Charsets.UTF_8), data))
    }

    private fun decryptValue(encryptedValue: String, associatedData: ByteArray): String {
        val data = associatedData.takeIf { Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP }
        return aead.decrypt(Base64.decode(encryptedValue, Base64.DEFAULT), data).toString(Charsets.UTF_8)
    }

    fun encrypt(cache: Cache<String, String>): Cache<String, String> = object : Cache<String, String> {

        override suspend fun get(key: String): String? = withContext(Dispatchers.IO) {
            try {
                val encryptedKey = encryptKey(key)
                val encryptedValue = cache.get(encryptedKey.base64)

                encryptedValue?.let { decryptValue(encryptedValue, encryptedKey.rawBytes) }
            } catch (expected: Exception) {
                expected.printStackTrace()
                null
            }
        }

        override suspend fun set(key: String, value: String) = withContext(Dispatchers.IO) {
            try {
                val encryptedKey = encryptKey(key)

                cache.set(encryptedKey.base64, encryptValue(value, encryptedKey.rawBytes))
            } catch (expected: Exception) {
                expected.printStackTrace()
            }
        }

        override suspend fun evict(key: String) = withContext(Dispatchers.IO) {
            try {
                cache.evict(encryptKey(key).base64)
            } catch (expected: Exception) {
                expected.printStackTrace()
            }
        }

        override suspend fun evictAll() = withContext(Dispatchers.IO) {
            cache.evictAll()
        }
    }

    fun <Key : Any> encryptValues(cache: Cache<Key, String>): Cache<Key, String> = object : Cache<Key, String> {

        override suspend fun get(key: Key): String? = withContext(Dispatchers.IO) {
            try {
                cache.get(key)?.let {
                    val (encryptedValue, salt) = it.split(":")

                    decryptValue(encryptedValue, Base64.decode(salt))
                }
            } catch (expected: Exception) {
                expected.printStackTrace()
                null
            }
        }

        override suspend fun set(key: Key, value: String) = withContext(Dispatchers.IO) {
            try {
                val secureRandom = SecureRandom()

                @Suppress("MagicNumber")
                val salt = ByteArray(16)
                secureRandom.nextBytes(salt)

                cache.set(key, encryptValue(value, salt) + ":" + Base64.encode(salt))
            } catch (expected: Exception) {
                expected.printStackTrace()
            }
        }

        override suspend fun evict(key: Key) = withContext(Dispatchers.IO) {
            cache.evict(key)
        }

        override suspend fun evictAll() = withContext(Dispatchers.IO) {
            cache.evictAll()
        }
    }

    class EncryptedKey(val rawBytes: ByteArray) {
        val base64: String
            get() = Base64.encode(rawBytes)
    }

    companion object {
        const val DEFAULT_MASTER_KEY_ALIAS = "_com_appmattus_layercache_master_key_"

        private const val KEY_KEYSET_ALIAS = "__com_appmattus_layercache_android_encryption_key_keyset__"
        private const val VALUE_KEYSET_ALIAS = "__com_appmattus_layercache_android_encryption_value_keyset__"
    }
}
