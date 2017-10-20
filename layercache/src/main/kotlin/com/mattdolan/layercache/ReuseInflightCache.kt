package com.mattdolan.layercache

import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.async

@Suppress("UnnecessaryAbstractClass", "ExceptionRaisedInUnexpectedLocation") // incorrectly reported
internal abstract class ReuseInflightCache<Key : Any, Value : Any>(private val cache: Cache<Key, Value>) :
        ComposedCache<Key, Value>() {
    final override val parents: List<Cache<*, *>>
        get() = listOf(cache)

    init {
        if (cache is ReuseInflightCache) {
            throw IllegalStateException("Do not directly chain reuseInflight")
        }
    }

    val map = mutableMapOf<Key, Deferred<Value?>>()

    final override fun get(key: Key): Deferred<Value?> {
        return map.get(key) ?: cache.get(key).apply {
            map.set(key, this)

            async(CommonPool) {
                // free up map when job is completed regardless of success or failure
                join()
                map.remove(key)
            }
        }
    }
}
