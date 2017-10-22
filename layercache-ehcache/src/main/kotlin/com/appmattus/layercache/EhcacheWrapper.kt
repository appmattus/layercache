/**
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
