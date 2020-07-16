/*
 * Copyright 2020 Appmattus Limited
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
import androidx.lifecycle.liveData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Convert a cache to return LiveData objects
 * @property cache Cache to convert
 */
class LiveDataCache<Key : Any, Value : Any>(private val cache: Cache<Key, Value>) {
    /**
     * Return the value associated with the key or null if not present
     */
    @CheckResult
    operator fun get(key: Key) = liveData {
        emit(LiveDataResult.Loading)
        try {
            emit(LiveDataResult.Success(withContext(Dispatchers.IO) { cache.get(key) }))
        } catch (expected: Exception) {
            emit(LiveDataResult.Failure(expected))
        }
    }

    /**
     * Save the value against the key
     */
    @CheckResult
    operator fun set(key: Key, value: Value) = liveData {
        emit(withContext(Dispatchers.IO) { cache.set(key, value) })
    }

    /**
     * Remove the data associated with the key
     */
    @CheckResult
    fun evict(key: Key) = liveData {
        emit(withContext(Dispatchers.IO) { cache.evict(key) })
    }
}

/**
 * Convert a cache to return LiveData objects
 */
@Suppress("unused")
@CheckResult
fun <Key : Any, Value : Any> Cache<Key, Value>.toLiveData() = LiveDataCache(this)
