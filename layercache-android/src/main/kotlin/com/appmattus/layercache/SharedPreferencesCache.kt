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

import android.content.SharedPreferences

/**
 * Any-based value shared preference cache
 */
fun SharedPreferences.asCache(): Cache<String, Any> {
    return BaseCache(
        this,
        { sharedPreferences, key -> sharedPreferences.all[key] },
        { editor, key, value ->
            when (value) {
                is String -> editor.putString(key, value)
                is Set<*> -> @Suppress("UNCHECKED_CAST") editor.putStringSet(key, value as Set<String>)
                is Int -> editor.putInt(key, value)
                is Boolean -> editor.putBoolean(key, value)
                is Long -> editor.putLong(key, value)
                is Float -> editor.putFloat(key, value)
                else -> error("Only primitive types can be stored in SharedPreferences")
            }
        }
    )
}

/**
 * String-based value shared preference cache
 */
fun SharedPreferences.asStringCache(): Cache<String, String> = BaseCache(
    this,
    { sharedPreferences, key -> sharedPreferences.getString(key, null) },
    { editor, key, value -> editor.putString(key, value) }
)

/**
 * Set<String>-based value shared preference cache
 */
fun SharedPreferences.asStringSetCache(): Cache<String, Set<String>> = BaseCache(
    this,
    { sharedPreferences, key -> sharedPreferences.getStringSet(key, null) },
    { editor, key, value -> editor.putStringSet(key, value) }
)

/**
 * Int-based value shared preference cache
 */
fun SharedPreferences.asIntCache(): Cache<String, Int> = BaseCache(
    this,
    { sharedPreferences: SharedPreferences, key: String -> sharedPreferences.getInt(key, 0) },
    { editor, key, value -> editor.putInt(key, value) }
)

/**
 * Float-based value shared preference cache
 */
fun SharedPreferences.asFloatCache(): Cache<String, Float> = BaseCache(
    this,
    { sharedPreferences, key -> sharedPreferences.getFloat(key, 0f) },
    { editor, key, value -> editor.putFloat(key, value) }
)

/**
 * Boolean-based value shared preference cache
 */
fun SharedPreferences.asBooleanCache(): Cache<String, Boolean> = BaseCache(
    this,
    { sharedPreferences, key -> sharedPreferences.getBoolean(key, false) },
    { editor, key, value -> editor.putBoolean(key, value) }
)

/**
 * Long-based value shared preference cache
 */
fun SharedPreferences.asLongCache(): Cache<String, Long> = BaseCache(
    this,
    { sharedPreferences, key -> sharedPreferences.getLong(key, 0) },
    { editor, key, value -> editor.putLong(key, value) }
)

/**
 * Simple cache that stores values associated with keys in a shared preferences file with no expiration or cleanup
 * logic. Use at your own risk.
 */
private class BaseCache<T : Any>(
    private val sharedPreferences: SharedPreferences,
    val getFun: (sharedPreferences: SharedPreferences, key: String) -> T?,
    val setFun: (editor: SharedPreferences.Editor, key: String, value: T) -> Unit
) : Cache<String, T> {

    override suspend fun get(key: String): T? {
        return if (sharedPreferences.contains(key)) {
            getFun(sharedPreferences, key)
        } else {
            null
        }
    }

    override suspend fun set(key: String, value: T) {
        val editor = sharedPreferences.edit()
        setFun(editor, key, value)
        editor.apply()
    }

    override suspend fun evict(key: String) {
        val editor = sharedPreferences.edit()
        editor.remove(key)
        editor.apply()
    }

    override suspend fun evictAll() {
        val editor = sharedPreferences.edit()
        editor.clear()
        editor.apply()
    }
}
