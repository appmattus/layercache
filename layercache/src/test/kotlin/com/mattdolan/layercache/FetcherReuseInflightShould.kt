package com.mattdolan.layercache

import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.runBlocking
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class FetcherReuseInflightShould {

    @get:Rule
    var thrown = ExpectedException.none()

    @Mock
    private lateinit var cache: AbstractFetcherCache<Any, Any>

    private lateinit var reuseInflightCache: Fetcher<Any, Any>

    @Before
    fun before() {
        MockitoAnnotations.initMocks(this)
        Mockito.`when`(cache.reuseInflight()).thenCallRealMethod()
        reuseInflightCache = cache.reuseInflight()

        Mockito.verify(cache).reuseInflight()
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
            Assert.assertEquals("value", result)
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
            Assert.assertEquals(1, count.get())
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
            Assert.assertEquals(2, count.get())
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
    fun `not interact with parent set`() {
        runBlocking {
            // when we set the value
            @Suppress("DEPRECATION")
            reuseInflightCache.set("1", 1).await()

            // then the parent cache is not called
            Mockito.verifyNoMoreInteractions(cache)
        }
    }

    // evict
    @Test
    fun `not interact with parent evict`() {
        runBlocking {
            // when we set the value
            @Suppress("DEPRECATION")
            reuseInflightCache.evict("1").await()

            // then the parent cache is not called
            Mockito.verifyNoMoreInteractions(cache)
        }
    }
}