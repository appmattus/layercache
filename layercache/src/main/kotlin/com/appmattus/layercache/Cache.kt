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

import androidx.annotation.NonNull
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll

/**
 * A standard cache which stores and retrieves data
 */
@Suppress("TooManyFunctions")
interface Cache<Key : Any, Value : Any> {
    /**
     * Companion object for 'static' extension functions
     */
    companion object Companion

    /**
     * Return the value associated with the key or null if not present
     */
    suspend fun get(key: Key): Value?

    /**
     * Save the value against the key
     */
    suspend fun set(key: Key, value: Value)

    /**
     * Remove the data associated with the key
     */
    suspend fun evict(key: Key)

    /**
     * Remove the data associated with all keys
     */
    suspend fun evictAll()

    /**
     * Compose two caches. Try to fetch from the first cache and, failing that, request the data from the second cache.
     * After being retrieved from the second cache, the data is cached in the first cache for future retrieval.
     */
    @Suppress("ReturnCount")
    fun compose(@NonNull b: Cache<Key, Value>): Cache<Key, Value> {
        return object : ComposedCache<Key, Value>() {
            init {
                require(!hasLoop()) { "Cache creates a circular reference" }
            }

            override val parents: List<Cache<*, *>>
                get() = listOf(this@Cache, b)

            override suspend fun evict(key: Key) {
                requireNotNull(key)
                executeInParallel(listOf(this@Cache, b), "evict") {
                    it.evict(key)
                }
            }

            override suspend fun get(key: Key): Value? {
                requireNotNull(key)
                return this@Cache.get(key) ?: let {
                    b.get(key)?.apply {
                        this@Cache.set(key, this)
                    }
                }
            }

            override suspend fun set(key: Key, value: Value) {
                requireNotNull(key)
                requireNotNull(value)

                executeInParallel(listOf(this@Cache, b), "set") {
                    it.set(key, value)
                }
            }

            override suspend fun evictAll() {
                executeInParallel(listOf(this@Cache, b), "evictAll") {
                    it.evictAll()
                }
            }
        }
    }

    /**
     * Compose two caches. Try to fetch from the first cache and, failing that, request the data from the second cache.
     * After being retrieved from the second cache, the data is cached in the first cache for future retrieval.
     */
    operator fun plus(b: Cache<Key, Value>) = compose(b)

    /**
     * Map keys from one type to another.
     */
    @Suppress("ReturnCount")
    fun <MappedKey : Any> keyTransform(transform: (MappedKey) -> Key): Cache<MappedKey, Value> {
        return object : MapKeysCache<Key, Value, MappedKey>(this@Cache, transform) {
            override suspend fun evict(key: MappedKey) {
                val mappedKey = requireNotNull(transform(key)) {
                    "Required value was null. Key '$key' mapped to null"
                }
                this@Cache.evict(mappedKey)
            }

            override suspend fun set(key: MappedKey, value: Value) {
                val mappedKey = requireNotNull(transform(key)) {
                    "Required value was null. Key '$key' mapped to null"
                }
                return this@Cache.set(mappedKey, value)
            }

            override suspend fun evictAll() = this@Cache.evictAll()
        }
    }

    /**
     * Map keys from one type to another.
     */
    fun <MappedKey : Any> keyTransform(transform: OneWayTransform<MappedKey, Key>): Cache<MappedKey, Value> =
        keyTransform(transform::transform)

    /**
     * Map values from one type to another. As this is a one way transform calling set on the resulting cache is no-op
     */
    fun <MappedValue : Any> valueTransform(transform: (Value) -> MappedValue): Fetcher<Key, MappedValue> {
        @Suppress("EmptyClassBlock")
        return object : Fetcher<Key, MappedValue>, MapValuesCache<Key, Value, MappedValue>(this, transform) {}
    }

    /**
     * Map values from one type to another. As this is a one way transform calling set on the resulting cache is no-op
     */
    fun <MappedValue : Any> valueTransform(transform: OneWayTransform<Value, MappedValue>): Fetcher<Key, MappedValue> =
        valueTransform(transform::transform)

    /**
     * Map values from one type to another and vice-versa.
     */
    fun <MappedValue : Any> valueTransform(transform: (Value) -> MappedValue, inverseTransform: (MappedValue) -> Value):
            Cache<Key, MappedValue> {
        return object : MapValuesCache<Key, Value, MappedValue>(this@Cache, transform) {
            override suspend fun evict(key: Key) = this@Cache.evict(key)

            override suspend fun set(key: Key, value: MappedValue) {
                return this@Cache.set(key, inverseTransform(value))
            }

            override suspend fun evictAll() = this@Cache.evictAll()
        }
    }

    /**
     * Map values from one type to another and vice-versa.
     */
    fun <MappedValue : Any> valueTransform(transform: TwoWayTransform<Value, MappedValue>): Cache<Key, MappedValue> =
        valueTransform(transform::transform, transform::inverseTransform)

    /**
     * If a get request is already in flight then this ensures the original request is returned
     */
    fun reuseInflight(): Cache<Key, Value> {
        return object : ReuseInflightCache<Key, Value>(this@Cache) {
            override suspend fun evict(key: Key) = this@Cache.evict(key)

            override suspend fun set(key: Key, value: Value) = this@Cache.set(key, value)

            override suspend fun evictAll() = this@Cache.evictAll()
        }
    }

    /**
     * Return data associated with multiple keys.
     */
    suspend fun batchGet(keys: List<Key>): List<Value?> {
        keys.requireNoNulls()

        return keys.map { GlobalScope.async { this@Cache.get(it) } }.awaitAll()
    }

    /**
     * Set data for multiple key/value pairs
     */
    suspend fun batchSet(values: Map<Key, Value>) {
        requireNotNull(values)
        values.keys.requireNoNulls()

        values.map { entry: Map.Entry<Key, Value> ->
            GlobalScope.async { this@Cache.set(entry.key, entry.value) }
        }.awaitAll()
    }

    private suspend fun <T> executeJobsInParallel(jobs: List<Deferred<T>>, lazyMessage: (index: Int) -> Any): List<T> {
        jobs.forEach { it.join() }

        val jobsWithExceptions = jobs.filter { it.isCancelled }
        if (jobsWithExceptions.isNotEmpty()) {
            val errorMessage = jobsWithExceptions.map { "${lazyMessage(jobs.indexOf(it))}" }.joinToString()

            val exceptions = jobsWithExceptions.map { it.getCompletionExceptionOrNull() }.filterNotNull()

            throw CacheException(errorMessage, exceptions)
        }

        return jobs.map { it.getCompleted() }
    }

    private suspend fun <K : Any, V : Any, T> executeInParallel(
        caches: List<Cache<K, V>>,
        message: String,
        methodCall: suspend (Cache<K, V>) -> T
    ): List<T> {
        val jobs = caches.map { GlobalScope.async { methodCall(it) } }
        return executeJobsInParallel(jobs) { index -> "$message failed for ${caches[index]}" }
    }
}
