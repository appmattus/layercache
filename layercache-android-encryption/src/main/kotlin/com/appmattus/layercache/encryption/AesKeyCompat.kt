package com.appmattus.layercache.encryption

import android.content.Context
import android.os.Build
import android.preference.PreferenceManager
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.support.annotation.RequiresApi
import java.math.BigInteger
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.SecureRandom
import java.util.Calendar
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec
import javax.security.auth.x500.X500Principal

@Suppress("ExceptionRaisedInUnexpectedLocation")
@RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
internal class AesKeyCompat(private val context: Context, private val blockMode: BlockMode,
                            private val encryptionPadding: EncryptionPadding, private val providesIv: Boolean,
                            private val integrityCheck: IntegrityCheck) {

    private companion object {
        val RSA_MODE = "RSA/ECB/PKCS1Padding"
        val ANDROID_KEYSTORE_PROVIDER = "AndroidKeyStore"
        val ANDROID_OPEN_SSL_PROVIDER = "AndroidOpenSSL"
        val AES_ALGORITHM = "AES"
    }

    private var mImpl: AESKey
    private val keyStore by lazy { KeyStore.getInstance(ANDROID_KEYSTORE_PROVIDER).apply { load(null) } }

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            mImpl = AESKeyApi23()
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            mImpl = AesKeyBase()
        } else {
            throw IllegalStateException("Requires API 18 or higher")
        }
    }

    internal fun retrieveConfidentialityKey(keystoreAlias: String): SecretKey =
            mImpl.retrieveConfidentialityKey(keystoreAlias)

    internal fun retrieveIntegrityKey(keystoreAlias: String): SecretKey = mImpl.retrieveIntegrityKey(keystoreAlias)

    private interface AESKey {
        @Suppress("UndocumentedPublicFunction") // incorrectly reported
        fun retrieveConfidentialityKey(keystoreAlias: String): SecretKey

        @Suppress("UndocumentedPublicFunction") // incorrectly reported
        fun retrieveIntegrityKey(keystoreAlias: String): SecretKey
    }

    @Suppress("TooManyFunctions")
    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    private inner class AesKeyBase : AESKey {

        private fun retrieveRSAKeys(keystoreAlias: String): KeyPair {
            val rsaKeyAlias = "$keystoreAlias:rsa"
            return loadRSAKeys(rsaKeyAlias) ?: generateRSAKeys(rsaKeyAlias)
        }

        private fun loadRSAKeys(keystoreAlias: String): KeyPair? {
            return (keyStore.getEntry(keystoreAlias, null) as? KeyStore.PrivateKeyEntry)?.let {
                KeyPair(it.certificate.publicKey, it.privateKey)
            }
        }

        private fun generateRSAKeys(keystoreAlias: String): KeyPair {
            val start = Calendar.getInstance()
            @Suppress("MagicNumber")
            val end = Calendar.getInstance().apply { add(Calendar.YEAR, 50) }

            @Suppress("DEPRECATION")
            val specBuilder = android.security.KeyPairGeneratorSpec.Builder(context)
                    .setAlias(keystoreAlias)
                    .setEndDate(end.getTime())
                    .setStartDate(start.getTime())
                    .setSerialNumber(BigInteger.ONE)
                    .setSubject(X500Principal("CN=${keystoreAlias}"))

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                @Suppress("MagicNumber")
                specBuilder.setKeySize(2048).setKeyType(KeyProperties.KEY_ALGORITHM_RSA)
            }

            return KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_RSA, ANDROID_KEYSTORE_PROVIDER).apply {
                initialize(specBuilder.build())
            }.generateKeyPair()
        }

        @Throws(Exception::class)
        private fun rsaEncrypt(keystoreAlias: String, secret: ByteArray): ByteArray {
            val secretKey = retrieveRSAKeys(keystoreAlias)

            // Encrypt the text
            val cipher = Cipher.getInstance(RSA_MODE, ANDROID_OPEN_SSL_PROVIDER)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey.public)

            return cipher.doFinal(secret)
        }

        private fun rsaDecrypt(keystoreAlias: String, encrypted: ByteArray): ByteArray {
            val secretKey = retrieveRSAKeys(keystoreAlias)

            val cipher = Cipher.getInstance(RSA_MODE, ANDROID_OPEN_SSL_PROVIDER)
            cipher.init(Cipher.DECRYPT_MODE, secretKey.private)

            return cipher.doFinal(encrypted)
        }

        private fun generateConfidentialityKey(alias: String): SecretKey {
            @Suppress("MagicNumber")
            return KeyGenerator.getInstance(AES_ALGORITHM).apply {
                init(256, SecureRandom())
            }.generateKey().apply {
                val encryptedKey = rsaEncrypt(alias, this.encoded).encodeBase64()
                PreferenceManager.getDefaultSharedPreferences(context).edit().
                        putString("${alias}:confidentiality", encryptedKey).apply()
            }
        }

        private fun loadConfidentialityKey(alias: String): SecretKey? {
            return keyStore.getEntry("${alias}:rsa", null)?.let {
                PreferenceManager.getDefaultSharedPreferences(context).
                        getString("${alias}:confidentiality", null)?.let {
                    SecretKeySpec(rsaDecrypt(alias, it.decodeBase64()), AES_ALGORITHM)
                }
            }
        }

        override fun retrieveConfidentialityKey(keystoreAlias: String): SecretKey {
            return loadConfidentialityKey(keystoreAlias) ?: generateConfidentialityKey(keystoreAlias)
        }

        override fun retrieveIntegrityKey(keystoreAlias: String): SecretKey {
            return loadIntegrityKey(keystoreAlias) ?: generateIntegrityKey(keystoreAlias)
        }

        private fun loadIntegrityKey(alias: String): SecretKey? {
            return keyStore.getEntry("${alias}:rsa", null)?.let {
                PreferenceManager.getDefaultSharedPreferences(context).
                        getString("$alias:integrity", null)?.let {
                    SecretKeySpec(rsaDecrypt(alias, it.decodeBase64()), AES_ALGORITHM)
                }
            }
        }

        private fun generateIntegrityKey(alias: String): SecretKey {
            return KeyGenerator.getInstance(integrityCheck.algorithm).generateKey().apply {
                val encryptedKey = rsaEncrypt(alias, this.encoded).encodeBase64()
                PreferenceManager.getDefaultSharedPreferences(context).edit().
                        putString("$alias:integrity", encryptedKey).apply()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private inner class AESKeyApi23 : AESKey {
        override fun retrieveConfidentialityKey(keystoreAlias: String): SecretKey {
            val confidentialityKeyAlias = "$keystoreAlias:confidentiality"

            return loadSecretKey(confidentialityKeyAlias) ?: generateConfidentialityKey(confidentialityKeyAlias)
        }

        private fun generateConfidentialityKey(alias: String): SecretKey {
            val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE_PROVIDER)

            val builder = KeyGenParameterSpec.Builder(alias,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(blockMode.mode)
                    .setEncryptionPaddings(encryptionPadding.padding)
                    .setRandomizedEncryptionRequired(!providesIv)

            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.N) {
                builder.setInvalidatedByBiometricEnrollment(false)
            }

            keyGenerator.init(builder.build())

            return keyGenerator.generateKey()
        }

        private fun loadSecretKey(keystoreAlias: String): SecretKey? {
            val entry = keyStore.getEntry(keystoreAlias, null)
            if (entry is KeyStore.SecretKeyEntry) {
                return entry.secretKey
            }

            return null
        }

        override fun retrieveIntegrityKey(keystoreAlias: String): SecretKey {
            val integrityKeyAlias = "$keystoreAlias:integrity"

            return loadSecretKey(integrityKeyAlias) ?: generateIntegrityKey(integrityKeyAlias)
        }

        private fun generateIntegrityKey(alias: String): SecretKey {
            val keyGen = KeyGenerator.getInstance(integrityCheck.algorithm, ANDROID_KEYSTORE_PROVIDER)

            keyGen.init(KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_SIGN).build())

            return keyGen.generateKey()
        }
    }
}
