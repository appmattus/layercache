package com.appmattus.layercache.samples.data

import com.appmattus.layercache.Cache

/**
 * Class used to wrap a cache just so we can figure out from composing two caches where data originated from
 */
class LastRetrievedWrapper {
    // Stores the name of the cache data was returned from
    var lastRetrieved: String? = null

    fun reset() {
        lastRetrieved = null
    }

    // A simple Cache wrapper to update lastRetrieved when the cache returns a value from its get function
    fun <K : Any, V : Any> Cache<K, V>.wrap(name: String): Cache<K, V> {
        val delegate = this
        return object : Cache<K, V> {
            override suspend fun get(key: K): V? {
                println(delegate::class)

                return delegate.get(key)?.also {
                    println(it)
                    println(lastRetrieved)
                    if (lastRetrieved == null) {
                        println(name)
                        lastRetrieved = name
                    }
                }
            }

            override suspend fun set(key: K, value: V) = delegate.set(key, value)
            override suspend fun evict(key: K) = delegate.evict(key)
            override suspend fun evictAll() = delegate.evictAll()
        }
    }
}
