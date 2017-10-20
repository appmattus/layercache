package com.mattdolan.layercache

@Suppress("UnnecessaryAbstractClass") // incorrectly reported
internal abstract class ComposedCache<Key : Any, Value : Any> : Cache<Key, Value> {
    open val parents: List<Cache<*, *>>
        get() = throw IllegalStateException("Not overidden")

    /**
     * Iterates over parents to determine if there are any circular references
     */
    protected fun hasLoop(): Boolean {
        val baseCaches = mutableListOf<Cache<*, *>>()

        val cacheQueue = mutableListOf<Cache<*, *>>()
        cacheQueue.addAll(parents)

        while (cacheQueue.isNotEmpty()) {
            val cache = cacheQueue.removeAt(0)

            when (cache) {
                is ComposedCache -> cacheQueue.addAll(cache.parents)
                else -> baseCaches.add(cache)
            }
        }

        return baseCaches.size != baseCaches.distinct().size
    }
}
