package com.mattdolan.layercache

import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class CacheReuseInflightShould {

    @get:Rule
    var thrown = ExpectedException.none()

    @Mock
    private lateinit var cache: AbstractCache<Any, Any>

    private lateinit var reuseInflightCache: Cache<Any, Any>

    @Before
    fun before() {
        MockitoAnnotations.initMocks(this)
        Mockito.`when`(cache.reuseInflight()).thenCallRealMethod()
        reuseInflightCache = cache.reuseInflight()
    }

    // get
    @Test
    fun `single call to get returns the value`() {
        runBlocking {
            // given value available in first cache only
            Mockito.`when`(cache.get("key")).then { async(CommonPool) { "value" } }

            // when we get the value
            val result = reuseInflightCache.get("key").await()

            // then we return the value
            Mockito.verify(cache).get("key")
            assertEquals("value", result)
        }
    }

    @Test
    fun `execute get only once whilst a call is in flight`() {
        val count = AtomicInteger(0)

        runBlocking {
            // given value available in first cache only
            Mockito.`when`(cache.get("key")).then {
                async(CommonPool) {
                    delay(100, TimeUnit.MILLISECONDS)

                    count.getAndIncrement()
                }
            }

            // when we get the same key 5 times
            val jobs = arrayListOf<Deferred<Any?>>()
            for (i in 1..5) {
                jobs.add(reuseInflightCache.get("key"))
            }
            jobs.forEach { it.await() }

            // then get is only called once
            assertEquals(1, count.get())
        }
    }

    @Test
    fun `execute get twice if a call is made once the first one has finished`() {
        val count = AtomicInteger(0)

        runBlocking {
            // given value available in first cache only
            Mockito.`when`(cache.get("key")).then {
                async(CommonPool) {
                    delay(500, TimeUnit.MILLISECONDS)

                    count.incrementAndGet()
                }
            }

            reuseInflightCache.get("key").await()

            // we yield here as the map that stores the reuse may not have been cleared yet
            delay(100, TimeUnit.MILLISECONDS)

            // when we get the same key 5 times
            val jobs = arrayListOf<Deferred<Any?>>()
            for (i in 1..5) {
                jobs.add(reuseInflightCache.get("key"))
            }
            jobs.forEach { it.await() }

            // then get is only called once
            assertEquals(2, count.get())
        }
    }

    @Test(expected = TestException::class)
    fun `propogate exception on get`() {
        runBlocking {
            // given value available in first cache only
            Mockito.`when`(cache.get("key")).then { async(CommonPool) { throw TestException() } }

            // when we get the value
            reuseInflightCache.get("key").await()

            // then we throw an exception
        }
    }

    // set
    @Test
    fun `call set from cache`() {
        runBlocking {
            // given value available in first cache only
            Mockito.`when`(cache.set("key", "value")).then { async(CommonPool) { "value" } }

            // when we get the value
            reuseInflightCache.set("key", "value").await()

            // then we return the value
            //Assert.assertEquals("value", result)
            Mockito.verify(cache).set("key", "value")
        }
    }

    @Test(expected = TestException::class)
    fun `propagate exception on set`() {
        runBlocking {
            // given value available in first cache only
            Mockito.`when`(cache.set("key", "value")).then { async(CommonPool) { throw TestException() } }

            // when we get the value
            reuseInflightCache.set("key", "value").await()

            // then we throw an exception
        }
    }

    // evict
    @Test
    fun `call evict from cache`() {
        runBlocking {
            // given value available in first cache only
            Mockito.`when`(cache.evict("key")).then { async(CommonPool) {} }

            // when we get the value
            reuseInflightCache.evict("key").await()

            // then we return the value
            //Assert.assertEquals("value", result)
            Mockito.verify(cache).evict("key")
        }
    }

    @Test(expected = TestException::class)
    fun `propagate exception on evict`() {
        runBlocking {
            // given value available in first cache only
            Mockito.`when`(cache.evict("key")).then { async(CommonPool) { throw TestException() } }

            // when we get the value
            reuseInflightCache.evict("key").await()

            // then we throw an exception
        }
    }

    @Test
    fun `throw exception when directly chained`() {
        runBlocking {
            // expect exception
            thrown.expect(IllegalStateException::class.java)
            thrown.expectMessage("Do not directly chain reuseInflight")

            // when reuseInflight called again
            reuseInflightCache.reuseInflight()
        }
    }
}