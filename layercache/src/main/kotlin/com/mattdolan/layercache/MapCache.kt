package com.mattdolan.layercache

import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.async

/**
 * Simple cache that stores values associated with keys in a map with no expiration or cleanup logic. Use at your own
 * risk.
 */
class MapCache : Cache<String, String> {
    private val map = mutableMapOf<String, String?>()

    override fun get(key: String): Deferred<String?> {
        return async(CommonPool) {
            map.get(key)
        }
    }

    override fun set(key: String, value: String): Deferred<Unit> {
        return async<Unit>(CommonPool) {
            map.put(key, value)
        }
    }

    override fun evict(key: String): Deferred<Unit> {
        return async<Unit>(CommonPool) {
            map.remove(key)
        }
    }

    override fun evictAll(): Deferred<Unit> {
        return async<Unit>(CommonPool) {
            map.clear()
        }
    }
}
