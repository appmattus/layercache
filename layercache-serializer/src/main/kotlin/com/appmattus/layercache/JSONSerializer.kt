package com.appmattus.layercache

import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.JSON

/**
 * Two-way transform to serialise and deserialise data class objects to String
 */
class JSONSerializer<Value : Any>(private val serializer: KSerializer<Value>) : TwoWayTransform<String, Value> {
    override fun transform(value: String): Value {
        return JSON.parse(serializer, value)
    }

    override fun inverseTransform(mappedValue: Value): String {
        return JSON.stringify(serializer, mappedValue)
    }
}
