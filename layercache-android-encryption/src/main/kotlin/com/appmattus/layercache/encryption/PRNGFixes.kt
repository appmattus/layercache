/*
 * This software is provided 'as-is', without any express or implied
 * warranty.  In no event will Google be held liable for any damages
 * arising from the use of this software.
 *
 * Permission is granted to anyone to use this software for any purpose,
 * including commercial applications, and to alter it and redistribute it
 * freely, as long as the origin is not misrepresented.
 */
package com.appmattus.layercache.encryption

import android.annotation.SuppressLint
import android.os.Build
import android.os.Process
import android.util.Log
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.io.UnsupportedEncodingException
import java.security.NoSuchAlgorithmException
import java.security.Provider
import java.security.SecureRandom
import java.security.SecureRandomSpi
import java.security.Security

/**
 * Fixes for the output of the default PRNG having low entropy.
 *
 * The fixes need to be applied via [.apply] before any use of Java
 * Cryptography Architecture primitives. A good place to invoke them is in the
 * application's `onCreate`.
 */
internal object PRNGFixes {
    private val BUILD_FINGERPRINT_AND_DEVICE_SERIAL = buildFingerprintAndDeviceSerial

    /**
     * Applies all fixes.
     *
     * @throws SecurityException if a fix is needed but could not be applied.
     */
    fun apply() {
        applyOpenSSLFix()
        installLinuxPRNGSecureRandom()
    }

    /**
     * Applies the fix for OpenSSL PRNG having low entropy. Does nothing if the
     * fix is not needed.
     *
     * @throws SecurityException if the fix is needed but could not be applied.
     */
    @SuppressLint("ObsoleteSdkInt")
    @Throws(SecurityException::class)
    private fun applyOpenSSLFix() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN || Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN_MR2) {
            // No need to apply the fix
            return
        }

        try {
            // Mix in the device- and invocation-specific seed.
            Class.forName("org.apache.harmony.xnet.provider.jsse.NativeCrypto")
                .getMethod("RAND_seed", ByteArray::class.java)
                .invoke(null, generateSeed())

            // Mix output of Linux PRNG into OpenSSL's PRNG
            val bytesRead = Class.forName(
                "org.apache.harmony.xnet.provider.jsse.NativeCrypto"
            )
                .getMethod("RAND_load_file", String::class.java, Long::class.javaPrimitiveType)
                .invoke(null, "/dev/urandom", 1024) as Int
            if (bytesRead != 1024) {
                throw IOException("Unexpected number of bytes read from Linux PRNG: $bytesRead")
            }
        } catch (e: Exception) {
            throw SecurityException("Failed to seed OpenSSL PRNG", e)
        }
    }

    /**
     * Installs a Linux PRNG-backed `SecureRandom` implementation as the
     * default. Does nothing if the implementation is already the default or if
     * there is not need to install the implementation.
     *
     * @throws SecurityException if the fix is needed but could not be applied.
     */
    @Throws(SecurityException::class)
    private fun installLinuxPRNGSecureRandom() {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN_MR2) {
            // No need to apply the fix
            return
        }

        // Install a Linux PRNG-based SecureRandom implementation as the
        // default, if not yet installed.
        val secureRandomProviders = Security.getProviders("SecureRandom.SHA1PRNG")
        if (secureRandomProviders == null
            || secureRandomProviders.isEmpty()
            || LinuxPRNGSecureRandomProvider::class.java != secureRandomProviders[0].javaClass
        ) {
            Security.insertProviderAt(LinuxPRNGSecureRandomProvider(), 1)
        }

        // Assert that new SecureRandom() and
        // SecureRandom.getInstance("SHA1PRNG") return a SecureRandom backed
        // by the Linux PRNG-based SecureRandom implementation.
        val rng1 = SecureRandom()
        if (LinuxPRNGSecureRandomProvider::class.java != rng1.provider.javaClass) {
            throw SecurityException("new SecureRandom() backed by wrong Provider: ${rng1.provider.javaClass}")
        }

        val rng2 = try {
            SecureRandom.getInstance("SHA1PRNG")
        } catch (e: NoSuchAlgorithmException) {
            throw SecurityException("SHA1PRNG not available", e)
        }

        if (LinuxPRNGSecureRandomProvider::class.java != rng2.provider.javaClass) {
            throw SecurityException("SecureRandom.getInstance(\"SHA1PRNG\") backed by wrong Provider: ${rng2.provider.javaClass}")
        }
    }

    /**
     * Generates a device- and invocation-specific seed to be mixed into the
     * Linux PRNG.
     */
    private fun generateSeed(): ByteArray {
        return try {
            val seedBuffer = ByteArrayOutputStream()
            DataOutputStream(seedBuffer).use {
                with(it) {
                    writeLong(System.currentTimeMillis())
                    writeLong(System.nanoTime())
                    writeInt(Process.myPid())
                    writeInt(Process.myUid())
                    write(BUILD_FINGERPRINT_AND_DEVICE_SERIAL)
                }
            }
            seedBuffer.toByteArray()
        } catch (e: IOException) {
            throw SecurityException("Failed to generate seed", e)
        }
    }

    /**
     * Gets the hardware serial number of this device.
     *
     * @return serial number or `null` if not available.
     */
    private val deviceSerialNumber: String?
        get() = try {
            // We're using the Reflection API because Build.SERIAL is only available
            // since API Level 9 (Gingerbread, Android 2.3).
            Build::class.java.getField("SERIAL")[null] as String
        } catch (ignored: Exception) {
            null
        }

    private val buildFingerprintAndDeviceSerial: ByteArray
        get() {
            val result = StringBuilder()

            Build.FINGERPRINT?.let { result.append(it) }
            deviceSerialNumber?.let { result.append(it) }

            return try {
                result.toString().toByteArray(Charsets.UTF_8)
            } catch (e: UnsupportedEncodingException) {
                throw RuntimeException("UTF-8 encoding not supported")
            }
        }

    /**
     * `Provider` of `SecureRandom` engines which pass through
     * all requests to the Linux PRNG.
     */
    private class LinuxPRNGSecureRandomProvider : Provider("LinuxPRNG", 1.0, "A Linux-specific random number provider that uses /dev/urandom") {
        init {
            // Although /dev/urandom is not a SHA-1 PRNG, some apps
            // explicitly request a SHA1PRNG SecureRandom and we thus need to
            // prevent them from getting the default implementation whose output
            // may have low entropy.
            put("SecureRandom.SHA1PRNG", LinuxPRNGSecureRandom::class.java.name)
            put("SecureRandom.SHA1PRNG ImplementedIn", "Software")
        }
    }

    /**
     * [SecureRandomSpi] which passes all requests to the Linux PRNG
     * (`/dev/urandom`).
     */
    class LinuxPRNGSecureRandom : SecureRandomSpi() {
        /**
         * Whether this engine instance has been seeded. This is needed because
         * each instance needs to seed itself if the client does not explicitly
         * seed it.
         */
        private var mSeeded = false
        override fun engineSetSeed(bytes: ByteArray) {
            try {
                var out: OutputStream?
                synchronized(sLock) { out = urandomOutputStream }
                out!!.write(bytes)
                out!!.flush()
            } catch (e: IOException) {
                // On a small fraction of devices /dev/urandom is not writable.
                // Log and ignore.
                Log.w(PRNGFixes::class.java.simpleName, "Failed to mix seed into $URANDOM_FILE")
            } finally {
                mSeeded = true
            }
        }

        override fun engineNextBytes(bytes: ByteArray) {
            if (!mSeeded) {
                // Mix in the device- and invocation-specific seed.
                engineSetSeed(generateSeed())
            }
            try {
                var `in`: DataInputStream?
                synchronized(sLock) { `in` = urandomInputStream }
                synchronized(`in`!!) { `in`!!.readFully(bytes) }
            } catch (e: IOException) {
                throw SecurityException("Failed to read from $URANDOM_FILE", e)
            }
        }

        override fun engineGenerateSeed(size: Int): ByteArray {
            val seed = ByteArray(size)
            engineNextBytes(seed)
            return seed
        }

        // NOTE: Consider inserting a BufferedInputStream between
        // DataInputStream and FileInputStream if you need higher
        // PRNG output performance and can live with future PRNG
        // output being pulled into this process prematurely.
        private val urandomInputStream: DataInputStream?
            get() {
                synchronized(sLock) {
                    if (sUrandomIn == null) {
                        // NOTE: Consider inserting a BufferedInputStream between
                        // DataInputStream and FileInputStream if you need higher
                        // PRNG output performance and can live with future PRNG
                        // output being pulled into this process prematurely.
                        sUrandomIn = try {
                            DataInputStream(FileInputStream(URANDOM_FILE))
                        } catch (e: IOException) {
                            throw SecurityException("Failed to open $URANDOM_FILE for reading", e)
                        }
                    }
                    return sUrandomIn
                }
            }

        @get:Throws(IOException::class)
        private val urandomOutputStream: OutputStream?
            get() {
                synchronized(sLock) {
                    if (sUrandomOut == null) {
                        sUrandomOut = FileOutputStream(URANDOM_FILE)
                    }
                    return sUrandomOut
                }
            }

        companion object {
            /*
             * IMPLEMENTATION NOTE: Requests to generate bytes and to mix in a seed
             * are passed through to the Linux PRNG (/dev/urandom). Instances of
             * this class seed themselves by mixing in the current time, PID, UID,
             * build fingerprint, and hardware serial number (where available) into
             * Linux PRNG.
             *
             * Concurrency: Read requests to the underlying Linux PRNG are
             * serialized (on sLock) to ensure that multiple threads do not get
             * duplicated PRNG output.
             */
            private val URANDOM_FILE = File("/dev/urandom")

            private val sLock = Any()

            /**
             * Input stream for reading from Linux PRNG or `null` if not yet
             * opened.
             *
             * @GuardedBy("sLock")
             */
            private var sUrandomIn: DataInputStream? = null

            /**
             * Output stream for writing to Linux PRNG or `null` if not yet
             * opened.
             *
             * @GuardedBy("sLock")
             */
            private var sUrandomOut: OutputStream? = null
        }
    }
}
