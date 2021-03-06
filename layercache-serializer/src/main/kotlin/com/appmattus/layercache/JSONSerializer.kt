/*
 * Copyright 2021 Appmattus Limited
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

import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import kotlin.reflect.typeOf

/**
 * Two-way transform to serialise and deserialise data class objects to String
 */
internal class JSONSerializer<Value : Any>(private val serializer: KSerializer<Value>) : TwoWayTransform<String, Value> {
    @Suppress("EXPERIMENTAL_API_USAGE")
    private val json = Json {}

    override fun transform(value: String): Value {
        return json.decodeFromString(serializer, value)
    }

    override fun inverseTransform(mappedValue: Value): String {
        return json.encodeToString(serializer, mappedValue)
    }
}

/**
 * Two-way transform to serialise and deserialise data class objects to String
 * @property serializer The Kotlin class serializer to use
 */
@Suppress("unused")
public fun <Key : Any, Value : Any> Cache<Key, String>.jsonSerializer(serializer: KSerializer<Value>): Cache<Key, Value> =
    this.valueTransform(JSONSerializer(serializer))

/**
 * Two-way transform to serialise and deserialise data class objects to String
 */
@Suppress("unused", "EXPERIMENTAL_API_USAGE_ERROR", "UNCHECKED_CAST")
public inline fun <Key : Any, reified Value : Any> Cache<Key, String>.jsonSerializer(): Cache<Key, Value> =
    jsonSerializer(serializer(typeOf<Value>()) as KSerializer<Value>)
