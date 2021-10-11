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

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.serializer
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.core.AnyOf
import org.hamcrest.core.StringContains
import org.hamcrest.core.StringStartsWith
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import kotlin.reflect.typeOf

class JSONSerializerShould {

    @Serializable
    internal data class ValueClass(val value: Int)

    @Test
    fun `allow mapping data class into json through a cache`() {
        runBlocking {
            // given we create a simple cache and valueTransform with a serializer
            val initialCache = object : Cache<String, String> {
                var lastValue: String? = null

                override suspend fun get(key: String): String? = lastValue
                override suspend fun set(key: String, value: String) {
                    lastValue = value
                }

                override suspend fun evict(key: String) = TODO("not implemented")
                override suspend fun evictAll() = TODO("not implemented")
            }
            val cache: Cache<String, ValueClass> = initialCache.jsonSerializer()

            // when we set a data class into it
            cache.set("a", ValueClass(4))

            // then the the original cache contains json and the data object can retrieved from the wrapper
            assertEquals("{\"value\":4}", initialCache.lastValue)
            assertEquals(4, cache.get("a")?.value)
        }
    }

    @Test
    fun `serialize data class into json`() {
        @Suppress("EXPERIMENTAL_API_USAGE_ERROR", "UNCHECKED_CAST")
        val serializer = JSONSerializer(serializer(typeOf<ValueClass>()) as KSerializer<ValueClass>)
        assertEquals("{\"value\":5}", serializer.inverseTransform(ValueClass(5)))
    }

    @Test
    fun `deserialize json into data class`() {
        @Suppress("EXPERIMENTAL_API_USAGE_ERROR", "UNCHECKED_CAST")
        val serializer = JSONSerializer(serializer(typeOf<ValueClass>()) as KSerializer<ValueClass>)
        assertEquals(ValueClass(5), serializer.transform("{\"value\":5}"))
    }

    @Test
    fun `serialize and deserialize between data class and json`() {
        @Suppress("EXPERIMENTAL_API_USAGE_ERROR", "UNCHECKED_CAST")
        val serializer = JSONSerializer(serializer(typeOf<ValueClass>()) as KSerializer<ValueClass>)
        assertEquals(ValueClass(6), serializer.transform(serializer.inverseTransform(ValueClass(6))))
    }

    @Test
    fun `throw exception when parameter to transform is null`() {
        val throwable = assertThrows(NullPointerException::class.java) {
            @Suppress("EXPERIMENTAL_API_USAGE_ERROR", "UNCHECKED_CAST")
            JSONSerializer(serializer(typeOf<ValueClass>()) as KSerializer<ValueClass>).transform(TestUtils.uninitialized())
        }

        assertThat(throwable.message, StringStartsWith("Parameter specified as non-null is null"))
    }

    @Test
    fun `throw exception when parameter to inverseTransform is null`() {
        val throwable = assertThrows(NullPointerException::class.java) {
            @Suppress("EXPERIMENTAL_API_USAGE_ERROR", "UNCHECKED_CAST")
            JSONSerializer(serializer(typeOf<ValueClass>()) as KSerializer<ValueClass>).inverseTransform(TestUtils.uninitialized())
        }

        assertThat(throwable.message, StringStartsWith("Parameter specified as non-null is null"))
    }

    @Test
    fun `throw exception when parameter to transform is not json`() {
        val throwable = assertThrows(Exception::class.java) {
            @Suppress("EXPERIMENTAL_API_USAGE_ERROR", "UNCHECKED_CAST")
            JSONSerializer(serializer(typeOf<ValueClass>()) as KSerializer<ValueClass>).transform("junk")
        }

        assertThat(
            throwable.message,
            AnyOf(listOf(StringStartsWith("Unexpected JSON token"), StringStartsWith("Expected start of the object")))
        )
    }

    @Test
    fun `throw exception when parameter to transform contains field that is not expected`() {
        val throwable = assertThrows(Exception::class.java) {
            @Suppress("EXPERIMENTAL_API_USAGE_ERROR", "UNCHECKED_CAST")
            JSONSerializer(serializer(typeOf<ValueClass>()) as KSerializer<ValueClass>).transform("{\"result\":5}")
        }

        assertThat(throwable.message, StringContains("Encountered an unknown key 'result'"))
    }
}
