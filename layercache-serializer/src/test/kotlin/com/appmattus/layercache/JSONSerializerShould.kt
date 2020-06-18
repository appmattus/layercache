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

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonDecodingException
import kotlinx.serialization.serializer
import org.hamcrest.core.StringContains
import org.hamcrest.core.StringStartsWith
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException

class JSONSerializerShould {

    @get:Rule
    var thrown = ExpectedException.none()

    @Serializable
    internal data class ValueClass(val value: Int)

    @Test
    fun `allow mapping data class into json through a cache`() {
        runBlocking {
            // given we create a simple cache and valueTransform with a serializer
            val initialCache = object : Cache<String, String> {
                var lastValue: String? = null

                override suspend fun get(key: String): String? = lastValue
                override fun set(key: String, value: String): Deferred<Unit> = GlobalScope.async { lastValue = value }
                override fun evict(key: String): Deferred<Unit> = TODO("not implemented")
                override fun evictAll(): Deferred<Unit> = TODO("not implemented")
            }
            val cache: Cache<String, ValueClass> = initialCache.jsonSerializer()

            // when we set a data class into it
            cache.set("a", ValueClass(4)).await()

            // then the the original cache contains json and the data object can retrieved from the wrapper
            assertEquals("{\"value\":4}", initialCache.lastValue)
            assertEquals(4, cache.get("a")?.value)
        }
    }

    @Test
    fun `serialize data class into json`() {
        val serializer = JSONSerializer(ValueClass::class.serializer())
        assertEquals("{\"value\":5}", serializer.inverseTransform(ValueClass(5)))
    }

    @Test
    fun `deserialize json into data class`() {
        val serializer = JSONSerializer(ValueClass::class.serializer())
        assertEquals(ValueClass(5), serializer.transform("{\"value\":5}"))
    }

    @Test
    fun `serialize and deserialize between data class and json`() {
        val serializer = JSONSerializer(ValueClass::class.serializer())
        assertEquals(ValueClass(6), serializer.transform(serializer.inverseTransform(ValueClass(6))))
    }

    @Test
    fun `throw exception when parameter to transform is null`() {
        thrown.expect(IllegalArgumentException::class.java)
        thrown.expectMessage(StringStartsWith("Parameter specified as non-null is null"))

        JSONSerializer(ValueClass::class.serializer()).transform(TestUtils.uninitialized())
    }

    @Test
    fun `throw exception when parameter to inverseTransform is null`() {
        thrown.expect(IllegalArgumentException::class.java)
        thrown.expectMessage(StringStartsWith("Parameter specified as non-null is null"))

        JSONSerializer(ValueClass::class.serializer()).inverseTransform(TestUtils.uninitialized())
    }

    @Test
    fun `throw exception when parameter to transform is not json`() {
        thrown.expect(JsonDecodingException::class.java)
        thrown.expectMessage(StringStartsWith("Unexpected JSON token"))

        JSONSerializer(ValueClass::class.serializer()).transform("junk")
    }

    @Test
    fun `throw exception when parameter to transform contains field that is not expected`() {
        thrown.expect(JsonDecodingException::class.java)
        thrown.expectMessage(StringContains("Encountered an unknown key 'result'"))

        JSONSerializer(ValueClass::class.serializer()).transform("{\"result\":5}")
    }
}
