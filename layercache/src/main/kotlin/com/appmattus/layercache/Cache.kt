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

import androidx.annotation.NonNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

/**
 * A standard cache which stores and retrieves data
 */
@Suppress("TooManyFunctions", "RedundantRequireNotNullCall")
public interface Cache<Key : Any, Value : Any> {
    /**
     * Companion object for 'static' extension functions
     */
    public companion object Companion

    /**
     * Return the value associated with the key or null if not present
     */
    public suspend fun get(key: Key): Value?

    /**
     * Save the value against the key
     */
    public suspend fun set(key: Key, value: Value)

    /**
     * Remove the data associated with the key
     */
    public suspend fun evict(key: Key)

    /**
     * Remove the data associated with all keys
     */
    public suspend fun evictAll()

    /**
     * Compose two caches. Try to fetch from the first cache and, failing that, request the data from the second cache.
     * After being retrieved from the second cache, the data is cached in the first cache for future retrieval.
     */
    @Suppress("ReturnCount")
    public fun compose(@NonNull b: Cache<Key, Value>): Cache<Key, Value> {
        return object : ComposedCache<Key, Value>() {
            init {
                require(!hasLoop()) { "Cache creates a circular reference" }
            }

            override val parents: List<Cache<*, *>>
                get() = listOf(this@Cache, b)

            override suspend fun evict(key: Key) {
                requireNotNull(key)
                executeInParallel(listOf(this@Cache, b)) {
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

                executeInParallel(listOf(this@Cache, b)) {
                    it.set(key, value)
                }
            }

            override suspend fun evictAll() {
                executeInParallel(listOf(this@Cache, b)) {
                    it.evictAll()
                }
            }
        }
    }

    /**
     * Compose two caches. Try to fetch from the first cache and, failing that, request the data from the second cache.
     * After being retrieved from the second cache, the data is cached in the first cache for future retrieval.
     */
    public operator fun plus(b: Cache<Key, Value>): Cache<Key, Value> = compose(b)

    /**
     * Map keys from one type to another.
     */
    @Suppress("ReturnCount")
    public fun <MappedKey : Any> keyTransform(transform: (MappedKey) -> Key): Cache<MappedKey, Value> {
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
    public fun <MappedKey : Any> keyTransform(transform: OneWayTransform<MappedKey, Key>): Cache<MappedKey, Value> =
        keyTransform(transform::transform)

    /**
     * Map values from one type to another. As this is a one way transform calling set on the resulting cache is no-op
     */
    public fun <MappedValue : Any> valueTransform(transform: (Value) -> MappedValue): Fetcher<Key, MappedValue> {
        @Suppress("EmptyClassBlock")
        return object : Fetcher<Key, MappedValue>, MapValuesCache<Key, Value, MappedValue>(this, transform) {}
    }

    /**
     * Map values from one type to another. As this is a one way transform calling set on the resulting cache is no-op
     */
    public fun <MappedValue : Any> valueTransform(transform: OneWayTransform<Value, MappedValue>): Fetcher<Key, MappedValue> =
        valueTransform(transform::transform)

    /**
     * Map values from one type to another and vice-versa.
     */
    public fun <MappedValue : Any> valueTransform(transform: (Value) -> MappedValue, inverseTransform: (MappedValue) -> Value):
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
    public fun <MappedValue : Any> valueTransform(transform: TwoWayTransform<Value, MappedValue>): Cache<Key, MappedValue> =
        valueTransform(transform::transform, transform::inverseTransform)

    /**
     * If a get request is already in flight then this ensures the original request is returned
     */
    public fun reuseInflight(): Cache<Key, Value> {
        return object : ReuseInflightCache<Key, Value>(this@Cache) {
            override suspend fun evict(key: Key) = this@Cache.evict(key)

            override suspend fun set(key: Key, value: Value) = this@Cache.set(key, value)

            override suspend fun evictAll() = this@Cache.evictAll()
        }
    }

    /**
     * Return data associated with multiple keys.
     */
    public suspend fun batchGet(keys: List<Key>): List<Value?> {
        requireNotNull(keys)
        keys.requireNoNulls()

        return coroutineScope {
            keys.map { async(Dispatchers.IO) { this@Cache.get(it) } }.awaitAll()
        }
    }

    /**
     * Set data for multiple key/value pairs
     */
    public suspend fun batchSet(values: Map<Key, Value>) {
        requireNotNull(values)
        values.keys.requireNoNulls()
        values.values.requireNoNulls()

        coroutineScope {
            values.map { entry: Map.Entry<Key, Value> ->
                async(Dispatchers.IO) { this@Cache.set(entry.key, entry.value) }
            }.awaitAll()
        }
    }

    private suspend fun <K : Any, V : Any, T> executeInParallel(
        caches: List<Cache<K, V>>,
        methodCall: suspend (Cache<K, V>) -> T
    ): List<T> {
        return coroutineScope {
            caches.map { async(Dispatchers.IO) { methodCall(it) } }.awaitAll()
        }
    }
}

public fun <K : Any, V : Any> cache(block: suspend (K) -> V): Fetcher<K, V> = object : Fetcher<K, V> {
    override suspend fun get(key: K) = block(key)
}

public suspend fun <Value : Any> Cache<Unit, Value>.get(): Value? = get(Unit)

public suspend fun <Value : Any> Cache<Unit, Value>.set(value: Value): Unit = set(Unit, value)
