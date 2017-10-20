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
