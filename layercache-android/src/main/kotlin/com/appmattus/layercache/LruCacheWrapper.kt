package com.appmattus.layercache

import android.annotation.TargetApi
import android.os.Build
import android.util.LruCache
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.async

/**
 * Wrapper around Android's built in LruCache
 */
@TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
class LruCacheWrapper<Key : Any, Value : Any>(val cache: LruCache<Key, Value>) : Cache<Key, Value> {
    constructor(maxSize: Int) : this(LruCache(maxSize))

    override fun evict(key: Key): Deferred<Unit> {
        return async<Unit>(CommonPool) {
            cache.remove(key)
        }
    }

    override fun get(key: Key): Deferred<Value?> {
        return async(CommonPool) {
            cache.get(key)
        }
    }

    override fun set(key: Key, value: Value): Deferred<Unit> {
        return async<Unit>(CommonPool) {
            cache.put(key, value)
        }
    }

    override fun evictAll(): Deferred<Unit> {
        return async<Unit>(CommonPool) {
            cache.evictAll()
        }
    }
}
