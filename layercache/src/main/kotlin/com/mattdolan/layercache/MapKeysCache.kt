package com.mattdolan.layercache

import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.async

@Suppress("UnnecessaryAbstractClass") // incorrectly reported
internal abstract class MapKeysCache<Key : Any, Value : Any, MappedKey : Any>(
        private val cache: Cache<Key, Value>, private val transform: (MappedKey) -> Key) :
        ComposedCache<MappedKey, Value>() {
    final override val parents: List<Cache<*, *>>
        get() = listOf(cache)

    final override fun get(key: MappedKey): Deferred<Value?> {
        return async(CommonPool) {
            val mappedKey = requireNotNull(transform(key)) { "Required value was null. Key '$key' mapped to null" }
            cache.get(mappedKey).await()
        }
    }
}
