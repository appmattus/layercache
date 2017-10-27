/**
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

import android.support.annotation.NonNull
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.async

/**
 * A standard cache which stores and retrieves data
 */
@Suppress("TooManyFunctions")
interface Cache<Key : Any, Value : Any> {
    //Declaring an empty companion object here
    companion object Companion

    /**
     * Return the value associated with the key or null if not present
     */
    fun get(key: Key): Deferred<Value?>

    /**
     * Save the value against the key
     */
    fun set(key: Key, value: Value): Deferred<Unit>

    /**
     * Remove the data associated with the key
     */
    fun evict(key: Key): Deferred<Unit>

    /**
     * Remove the data associated with all keys
     */
    fun evictAll(): Deferred<Unit>

    /**
     * Compose two caches. Try to fetch from the first cache and, failing that, request the data from the second cache.
     * After being retrieved from the second cache, the data is cached in the first cache for future retrieval.
     */
    @Suppress("ReturnCount")
    fun compose(@NonNull b: Cache<Key, Value>): Cache<Key, Value> {
        return object : ComposedCache<Key, Value>() {
            init {
                require(!hasLoop(), { "Cache creates a circular reference" })
            }

            override val parents: List<Cache<*, *>>
                get() = listOf(this@Cache, b)

            override fun evict(key: Key): Deferred<Unit> {
                return async(CommonPool) {
                    executeInParallel(listOf(this@Cache, b), "evict", {
                        it.evict(key)
                    })
                    Unit
                }
            }

            override fun get(key: Key): Deferred<Value?> {
                return async(CommonPool) {
                    this@Cache.get(key).await() ?: let {
                        b.get(key).await()?.apply {
                            this@Cache.set(key, this).await()
                        }
                    }
                }
            }

            override fun set(key: Key, value: Value): Deferred<Unit> {
                return async(CommonPool) {
                    executeInParallel(listOf(this@Cache, b), "set", {
                        it.set(key, value)
                    })

                    Unit
                }
            }

            override fun evictAll(): Deferred<Unit> {
                return async(CommonPool) {
                    executeInParallel(listOf(this@Cache, b), "evictAll", {
                        it.evictAll()
                    })
                    Unit
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
            override fun evict(key: MappedKey): Deferred<Unit> {
                return async(CommonPool) {
                    val mappedKey = requireNotNull(transform(key)) {
                        "Required value was null. Key '$key' mapped to null"
                    }
                    this@Cache.evict(mappedKey).await()
                }
            }

            override fun set(key: MappedKey, value: Value): Deferred<Unit> {
                return async(CommonPool) {
                    val mappedKey = requireNotNull(transform(key)) {
                        "Required value was null. Key '$key' mapped to null"
                    }
                    this@Cache.set(mappedKey, value).await()
                }
            }

            override fun evictAll(): Deferred<Unit> = this@Cache.evictAll()
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
            override fun evict(key: Key) = this@Cache.evict(key)

            override fun set(key: Key, value: MappedValue): Deferred<Unit> {
                return async(CommonPool) {
                    this@Cache.set(key, inverseTransform(value)).await()
                }
            }

            override fun evictAll(): Deferred<Unit> = this@Cache.evictAll()
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
            override fun evict(key: Key) = this@Cache.evict(key)

            override fun set(key: Key, value: Value) = this@Cache.set(key, value)

            override fun evictAll(): Deferred<Unit> = this@Cache.evictAll()
        }
    }

    /**
     * Return data associated with multiple keys.
     */
    fun batchGet(keys: List<Key>): Deferred<List<Value?>> {
        keys.requireNoNulls()

        return async(CommonPool) {
            keys.map { this@Cache.get(it) }.map { it.await() }
        }
    }

    /**
     * Set data for multiple key/value pairs
     */
    fun batchSet(values: Map<Key, Value>): Deferred<Unit> {
        values.keys.requireNoNulls()

        return async(CommonPool) {
            values.map { entry: Map.Entry<Key, Value> ->
                this@Cache.set(entry.key, entry.value)
            }.forEach { it.await() }
            Unit
        }
    }

    private suspend fun <T> executeJobsInParallel(jobs: List<Deferred<T>>, lazyMessage: (index: Int) -> Any): List<T> {
        jobs.forEach { it.join() }

        val jobsWithExceptions = jobs.filter { it.isCompletedExceptionally }
        if (jobsWithExceptions.isNotEmpty()) {
            val errorMessage = jobsWithExceptions.map { "${lazyMessage(jobs.indexOf(it))}" }.joinToString()

            val exceptions = jobsWithExceptions.map { it.getCompletionException() }

            throw CacheException(errorMessage, exceptions)
        }

        return jobs.map { it.getCompleted() }
    }

    private suspend fun <K : Any, V : Any, T> executeInParallel(caches: List<Cache<K, V>>, message: String,
                                                                methodCall: (Cache<K, V>) -> Deferred<T>): List<T> {
        val jobs = caches.map { methodCall(it) }
        return executeJobsInParallel(jobs, { index -> "${message} failed for ${caches[index]}" })
    }
}
