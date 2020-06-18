/*
 * Copyright 2017 Appmattus Limited
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.appmattus.layercache

import androidx.annotation.CheckResult
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async

/**
 * Convert a cache to return LiveData objects
 * @property cache  Cache to convert
 */
class LiveDataCache<Key : Any, Value : Any>(private val cache: Cache<Key, Value>) {
    /**
     * Return the value associated with the key or null if not present
     */
    @CheckResult
    suspend fun get(key: Key): LiveData<LiveDataResult<Value?>> {
        val liveData = MutableLiveData<LiveDataResult<Value?>>()
        liveData.postValue(LiveDataResult.Loading())

        GlobalScope.async { cache.get(key) }.onCompletion {
            when (it) {
                is DeferredResult.Success -> LiveDataResult.Success<Value?>(it.value)
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

        GlobalScope.async {
            liveData.postValue(cache.set(key, value).await())
        }

        return liveData
    }

    /**
     * Remove the data associated with the key
     */
    fun evict(key: Key): LiveData<Unit> {
        val liveData = MutableLiveData<Unit>()

        GlobalScope.async {
            liveData.postValue(cache.evict(key).await())
        }

        return liveData
    }
}

/**
 * Convert a cache to return LiveData objects
 */
@Suppress("unused")
fun <Key : Any, Value : Any> Cache<Key, Value>.toLiveData() = LiveDataCache(this)
