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

/**
 * A cache exception that can hold multiple causes. The first exception is available in cause and subsequent exceptions
 * in suppressed
 */
class CacheException(message: String, innerExceptions: List<Throwable>) : Exception(message) {
    init {
        require(innerExceptions.isNotEmpty(), { "You must provide at least one Exception" })

        initCause(innerExceptions.first())

        val hasAddSuppressed: Boolean = try {
            Throwable::class.java.getDeclaredMethod("addSuppressed", Throwable::class.java) != null
        } catch (e: NoSuchMethodException) {
            false
        } catch (e: SecurityException) {
            false
        }

        if (hasAddSuppressed) {
            innerExceptions.subList(1, innerExceptions.size).forEach { addSuppressed(it) }
        }
    }
}
