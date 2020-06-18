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

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.core.IsEqual
import org.hamcrest.core.StringStartsWith
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException

class CacheComposeShould {

    @get:Rule
    var thrown: ExpectedException = ExpectedException.none()

    private val firstCache = mock<AbstractCache<String, String>>()
    private val secondCache = mock<AbstractCache<String, String>>()

    private lateinit var composedCache: Cache<String, String>

    @Before
    fun before() {
        whenever(firstCache.compose(any())).thenCallRealMethod()
        whenever(secondCache.compose(any())).thenCallRealMethod()
        composedCache = firstCache.compose(secondCache)
    }

    @Test
    fun `throw exception when second cache is null`() {
        thrown.expect(IllegalArgumentException::class.java)
        thrown.expectMessage(StringStartsWith("Parameter specified as non-null is null"))

        object : AbstractCache<String, String>() {
            override suspend fun get(key: String): String? = null
            override suspend fun set(key: String, value: String) = Unit
            override suspend fun evict(key: String) = Unit
            override suspend fun evictAll() = Unit
        }.compose(TestUtils.uninitialized())
    }

    @Test
    fun `throw exception when referencing self`() {
        thrown.expect(IllegalArgumentException::class.java)
        thrown.expectMessage("Cache creates a circular reference")

        firstCache.compose(firstCache)
    }

    @Test
    fun `throw exception when circular reference 1`() {
        thrown.expect(IllegalArgumentException::class.java)
        thrown.expectMessage("Cache creates a circular reference")

        val c = firstCache.compose(secondCache)
        firstCache.compose(c)
    }

    @Test
    fun `throw exception when circular reference 2`() {
        thrown.expect(IllegalArgumentException::class.java)
        thrown.expectMessage("Cache creates a circular reference")

        val c = firstCache.compose(secondCache)
        c.compose(firstCache)
    }

    @Test
    fun `throw exception when circular reference 3`() {
        thrown.expect(IllegalArgumentException::class.java)
        thrown.expectMessage("Cache creates a circular reference")

        val c = firstCache.compose(secondCache)
        secondCache.compose(c)
    }

    @Test
    fun `throw exception when circular reference 4`() {
        thrown.expect(IllegalArgumentException::class.java)
        thrown.expectMessage("Cache creates a circular reference")

        val c = firstCache.compose(secondCache)
        c.compose(secondCache)
    }

    @Test
    fun `throw exception when circular reference big chain 1`() {
        thrown.expect(IllegalArgumentException::class.java)
        thrown.expectMessage("Cache creates a circular reference")

        val c = firstCache.compose(secondCache).reuseInflight().keyTransform<String> { it }.valueTransform({ it }, { it }).reuseInflight()
        c.compose(secondCache)
    }

    @Test
    fun `throw exception when circular reference big chain 2`() {
        thrown.expect(IllegalArgumentException::class.java)
        thrown.expectMessage("Cache creates a circular reference")

        val c = firstCache.compose(secondCache).reuseInflight().keyTransform<String> { it }.valueTransform({ it }, { it }).reuseInflight()
        c.compose(firstCache)
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

        // expect an exception
        thrown.expect(IllegalStateException::class.java)
        thrown.expectMessage("Not overridden")

        // when we get parents that has not been overridden
        cache.parents
    }
}
