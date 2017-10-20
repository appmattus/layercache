package com.mattdolan.layercache

import kotlinx.coroutines.experimental.Deferred
import org.hamcrest.core.IsEqual
import org.hamcrest.core.StringStartsWith
import org.junit.Assert.assertThat
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations


class CacheComposeShould {

    @get:Rule
    var thrown = ExpectedException.none()

    @Mock
    private lateinit var firstCache: AbstractCache<String, String>

    @Mock
    private lateinit var secondCache: AbstractCache<String, String>

    private lateinit var composedCache: Cache<String, String>

    @Before
    fun before() {
        MockitoAnnotations.initMocks(this)
        Mockito.`when`(firstCache.compose(MockitoKotlin.any())).thenCallRealMethod()
        Mockito.`when`(secondCache.compose(MockitoKotlin.any())).thenCallRealMethod()
        composedCache = firstCache.compose(secondCache)
    }

    @Test
    fun `throw exception when second cache is null`() {
        thrown.expect(IllegalArgumentException::class.java)
        thrown.expectMessage(StringStartsWith("Parameter specified as non-null is null"))

        val nullCache: Cache<String, String> = TestUtils.uninitialized()
        firstCache.compose(nullCache)
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

        val c = firstCache.compose(secondCache).reuseInflight().mapKeys<String> { it }.mapValues({ it }, { it }).reuseInflight()
        c.compose(secondCache)
    }

    @Test
    fun `throw exception when circular reference big chain 2`() {
        thrown.expect(IllegalArgumentException::class.java)
        thrown.expectMessage("Cache creates a circular reference")

        val c = firstCache.compose(secondCache).reuseInflight().mapKeys<String> { it }.mapValues({ it }, { it }).reuseInflight()
        c.compose(firstCache)
    }

    @Test
    fun `contain both caches in composed parents`() {
        val cache = composedCache
        if (!(cache is ComposedCache<String, String>)) {
            fail()
            return
        }

        assertThat(cache.parents, IsEqual.equalTo(listOf<Cache<*, *>>(firstCache, secondCache)))
    }

    @Test
    fun `throw exception when parents not overidden?`() {
        // given we have a basic composed cache
        val cache = object : ComposedCache<String, String>() {
            override fun get(key: String): Deferred<String?> = throw Exception("Unimplemented")
            override fun set(key: String, value: String): Deferred<Unit> = throw Exception("Unimplemented")
            override fun evict(key: String): Deferred<Unit> = throw Exception("Unimplemented")
            override fun evictAll(): Deferred<Unit> = throw Exception("Unimplemented")
        }

        // expect an exception
        thrown.expect(IllegalStateException::class.java)
        thrown.expectMessage("Not overidden")

        // when we get parents that has not been overidden
        cache.parents
    }
}