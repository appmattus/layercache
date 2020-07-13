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

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.core.IsEqual
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class CacheComposeShould {

    private val firstCache = TestCache("firstCache")
    private val secondCache = TestCache("secondCache")
    private val composedCache: Cache<String, String> = firstCache.compose(secondCache)

    @Test
    fun `throw exception when second cache is null`() {
        val throwable = assertThrows(IllegalArgumentException::class.java) {
            object : AbstractCache<String, String>() {
                override suspend fun get(key: String): String? = null
                override suspend fun set(key: String, value: String) = Unit
                override suspend fun evict(key: String) = Unit
                override suspend fun evictAll() = Unit
            }.compose(TestUtils.uninitialized())
        }

        assertTrue(throwable.message!!.startsWith("Parameter specified as non-null is null"))
    }

    @Test
    fun `throw exception when referencing self`() {
        val throwable = assertThrows(IllegalArgumentException::class.java) {
            firstCache.compose(firstCache)
        }

        assertEquals("Cache creates a circular reference", throwable.message)
    }

    @Test
    fun `throw exception when circular reference 1`() {
        val throwable = assertThrows(IllegalArgumentException::class.java) {
            val c = firstCache.compose(secondCache)
            firstCache.compose(c)
        }

        assertEquals("Cache creates a circular reference", throwable.message)
    }

    @Test
    fun `throw exception when circular reference 2`() {
        val throwable = assertThrows(IllegalArgumentException::class.java) {
            val c = firstCache.compose(secondCache)
            c.compose(firstCache)
        }

        assertEquals("Cache creates a circular reference", throwable.message)
    }

    @Test
    fun `throw exception when circular reference 3`() {
        val throwable = assertThrows(IllegalArgumentException::class.java) {
            val c = firstCache.compose(secondCache)
            secondCache.compose(c)
        }

        assertEquals("Cache creates a circular reference", throwable.message)
    }

    @Test
    fun `throw exception when circular reference 4`() {
        val throwable = assertThrows(IllegalArgumentException::class.java) {
            val c = firstCache.compose(secondCache)
            c.compose(secondCache)
        }

        assertEquals("Cache creates a circular reference", throwable.message)
    }

    @Test
    fun `throw exception when circular reference big chain 1`() {
        val throwable = assertThrows(IllegalArgumentException::class.java) {
            val c = firstCache.compose(secondCache).reuseInflight().keyTransform<String> { it }.valueTransform({ it }, { it }).reuseInflight()
            c.compose(secondCache)
        }

        assertEquals("Cache creates a circular reference", throwable.message)
    }

    @Test
    fun `throw exception when circular reference big chain 2`() {
        val throwable = assertThrows(IllegalArgumentException::class.java) {
            val c = firstCache.compose(secondCache).reuseInflight().keyTransform<String> { it }.valueTransform({ it }, { it }).reuseInflight()
            c.compose(firstCache)
        }

        assertEquals("Cache creates a circular reference", throwable.message)
    }

    @Test
    fun `contain both caches in composed parents`() {
        val cache = composedCache
        if (cache !is ComposedCache<String, String>) {
            fail()
            return
        }

        assertThat(cache.parents, IsEqual.equalTo(listOf<Cache<*, *>>(firstCache, secondCache)))
    }

    @Test
    @Suppress("ThrowsCount")
    fun `throw exception when parents not overridden?`() {
        // given we have a basic composed cache
        val cache = object : ComposedCache<String, String>() {
            override suspend fun get(key: String): String? = throw Exception("Unimplemented")
            override suspend fun set(key: String, value: String) = throw Exception("Unimplemented")
            override suspend fun evict(key: String) = throw Exception("Unimplemented")
            override suspend fun evictAll() = throw Exception("Unimplemented")
        }

        // when we get parents that has not been overridden
        val throwable = assertThrows(IllegalStateException::class.java) {
            cache.parents
        }

        // expect an exception
        assertEquals("Not overridden", throwable.message)
    }
}
