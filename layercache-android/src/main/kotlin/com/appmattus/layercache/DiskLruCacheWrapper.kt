package com.appmattus.layercache

import com.jakewharton.disklrucache.DiskLruCache
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.async

/**
 * Wrapper around DiskLruCache (https://github.com/jakeWharton/DiskLruCache/)
 */
class DiskLruCacheWrapper(val cache: DiskLruCache) : Cache<String, String> {

    override fun evict(key: String): Deferred<Unit> {
        return async<Unit>(CommonPool) {
            cache.remove(key)
        }
    }

    override fun get(key: String): Deferred<String?> {
        return async(CommonPool) {
            cache.get(key)?.getString(0)
        }
    }

    override fun set(key: String, value: String): Deferred<Unit> {
        return async(CommonPool) {
            val editor = cache.edit(key)
            editor.set(0, value)
            editor.commit()
        }
    }

    override fun evictAll(): Deferred<Unit> {
        return async<Unit>(CommonPool) {
            // Although setting maxSize to zero will cause the cache to be emptied this happens in a separate thread,
            // by calling flush immediately we ensure this happens in the same call
            cache.maxSize.let {
                cache.maxSize = 0
                cache.flush()
                cache.maxSize = it
            }
        }
    }
}
