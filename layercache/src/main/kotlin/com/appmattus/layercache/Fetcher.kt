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

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async

/**
 * Fetcher is a special kind of cache that is just used to retrieve data. It is not possible to cache any values
 * and so implements no-op for set and evict. An example would be a network fetcher.
 */
interface Fetcher<Key : Any, Value : Any> : Cache<Key, Value> {
    /**
     * No-op as Cache is a Fetcher
     */
    @Deprecated("set does nothing on a Fetcher")
    override suspend fun set(key: Key, value: Value) = Unit

    /**
     * No-op as Cache is a Fetcher
     */
    @Deprecated("evict does nothing on a Fetcher")
    override suspend fun evict(key: Key) = Unit

    /**
     * No-op as Cache is a Fetcher
     */
    @Deprecated("evictAll does nothing on a Fetcher")
    override fun evictAll(): Deferred<Unit> = GlobalScope.async {}

    @Deprecated("Use valueTransform(transform) on a Fetcher", ReplaceWith("valueTransform(transform)"))
    override fun <MappedValue : Any> valueTransform(
        transform: (Value) -> MappedValue,
        inverseTransform: (MappedValue) -> Value
    ): Fetcher<Key, MappedValue> {
        return valueTransform(transform)
    }

    override fun <MappedKey : Any> keyTransform(transform: (MappedKey) -> Key): Fetcher<MappedKey, Value> {
        @Suppress("EmptyClassBlock")
        return object : Fetcher<MappedKey, Value>, MapKeysCache<Key, Value, MappedKey>(this@Fetcher, transform) {}
    }

    override fun <MappedKey : Any> keyTransform(transform: OneWayTransform<MappedKey, Key>): Fetcher<MappedKey, Value> =
        keyTransform(transform::transform)

    override fun reuseInflight(): Fetcher<Key, Value> {
        @Suppress("EmptyClassBlock")
        return object : Fetcher<Key, Value>, ReuseInflightCache<Key, Value>(this) {}
    }
}
