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

import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.JSON

/**
 * Two-way transform to serialise and deserialise data class objects to String
 */
internal class JSONSerializer<Value : Any>(private val serializer: KSerializer<Value>) : TwoWayTransform<String, Value> {
    override fun transform(value: String): Value {
        return JSON.parse(serializer, value)
    }

    override fun inverseTransform(mappedValue: Value): String {
        return JSON.stringify(serializer, mappedValue)
    }
}

@Suppress("unused", "USELESS_CAST")
fun <Key : Any, Value : Any> Cache<Key, String>.jsonSerializer(serializer: KSerializer<Value>) = this.valueTransform(JSONSerializer(serializer))
