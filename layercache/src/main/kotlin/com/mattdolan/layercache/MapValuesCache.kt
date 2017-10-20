package com.mattdolan.layercache

import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.async

@Suppress("UnnecessaryAbstractClass") // incorrectly reported
internal abstract class MapValuesCache<Key : Any, Value : Any, MappedValue : Any>(
        private val cache: Cache<Key, Value>, private val transform: (Value) -> MappedValue) :
        ComposedCache<Key, MappedValue>() {
    final override val parents: List<Cache<*, *>>
        get() = listOf(cache)

    final override fun get(key: Key): Deferred<MappedValue?> {
        return async(CommonPool) {
            cache.get(key).await()?.run(transform)
        }
    }
}
