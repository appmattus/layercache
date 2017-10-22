package com.appmattus.layercache

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.support.annotation.CheckResult
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.async

/**
 * Convert a cache to return LiveData objects
 */
class LiveDataCache<Key : Any, Value : Any>(private val cache: Cache<Key, Value>) {
    /**
     * Return the value associated with the key or null if not present
     */
    @CheckResult
    fun get(key: Key): LiveData<LiveDataResult<Value?>> {
        val liveData = MutableLiveData<LiveDataResult<Value?>>()
        liveData.postValue(LiveDataResult.Loading())

        cache.get(key).onCompletion {
            when (it) {
                is DeferredResult.Success -> LiveDataResult.Success<Value?>(it.value)
                is DeferredResult.Failure -> LiveDataResult.Failure<Value?>(it.exception)
                is DeferredResult.Cancelled -> LiveDataResult.Failure<Value?>(it.exception)
            }.let {
                liveData.postValue(it)
            }
        }


        return liveData
    }

    /**
     * Save the value against the key
     */
    fun set(key: Key, value: Value): LiveData<Unit> {
        val liveData = MutableLiveData<Unit>()

        async(CommonPool) {
            liveData.postValue(cache.set(key, value).await())
        }

        return liveData
    }

    /**
     * Remove the data associated with the key
     */
    fun evict(key: Key): LiveData<Unit> {
        val liveData = MutableLiveData<Unit>()

        async(CommonPool) {
            liveData.postValue(cache.evict(key).await())
        }

        return liveData
    }
}
