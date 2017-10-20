package com.appmattus.layercache

import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.async

/**
 * Wrapper around EhCache (http://www.ehcache.org/)
 */
class EhcacheWrapper<Key : Any, Value : Any>(private val cache: org.ehcache.Cache<Key, Value>) :
        Cache<Key, Value> {
    override fun evict(key: Key): Deferred<Unit> {
        return async(CommonPool) {
            cache.remove(key)
        }
    }

    override fun get(key: Key): Deferred<Value?> {
        return async(CommonPool) {
            cache.get(key)
        }
    }

    override fun set(key: Key, value: Value): Deferred<Unit> {
        return async(CommonPool) {
            cache.put(key, value)
        }
    }

    override fun evictAll(): Deferred<Unit> {
        return async(CommonPool) {
            cache.clear()
        }
    }
}
