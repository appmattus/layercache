package com.mattdolan.layercache

import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.runBlocking
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations

class CacheMapValuesOneWayShould {

    @Mock
    private lateinit var cache: AbstractCache<Any, String>

    @Mock
    private lateinit var function: (String) -> Int

    @Mock
    private lateinit var functionInverse: (Int) -> String

    private lateinit var mappedValuesCache: Fetcher<Any, Int>


    @Suppress("DEPRECATION")
    @Before
    fun before() {
        MockitoAnnotations.initMocks(this)

        Mockito.`when`(cache.mapValues(MockitoKotlin.any(function::class.java))).thenCallRealMethod()

        mappedValuesCache = cache.mapValues(function)

        Mockito.verify(cache, Mockito.atLeastOnce()).mapValues(MockitoKotlin.any<(String) -> Any>())
    }

    // get
    @Test
    fun `only invoke function and not inverse function`() {
        runBlocking {
            // given we have a cache that returns a string
            Mockito.`when`(cache.get("key")).then { async(CommonPool) { "1" } }
            Mockito.`when`(function.invoke(Mockito.anyString())).then { it.getArgument<String>(0).toInt() }

            // when we get the value
            mappedValuesCache.get("key").await()

            // then the main function is invoked but the inverse is not
            Mockito.verify(function).invoke("1")
            Mockito.verifyZeroInteractions(functionInverse)
        }
    }


    @Test
    fun `map string value in get to int`() {
        runBlocking {
            // given we have a cache that returns a string
            Mockito.`when`(cache.get("key")).then { async(CommonPool) { "1" } }
            Mockito.`when`(function.invoke(Mockito.anyString())).then { it.getArgument<String>(0).toInt() }

            // when we get the value
            val result = mappedValuesCache.get("key").await()

            // then it is converted to an integer
            Assert.assertEquals(1, result)
            Assert.assertTrue(result is Int)
        }
    }

    @Test(expected = TestException::class)
    fun `throw exception when mapping in function`() {
        runBlocking {
            // given we have a string and transform throws an exception
            Mockito.`when`(cache.get("key")).then { async(CommonPool) { "1" } }
            Mockito.`when`(function.invoke(Mockito.anyString())).then { throw TestException() }

            // when we get the value from a map with exception throwing functions
            mappedValuesCache.get("key").await()

            // then an exception is thrown
        }
    }

    @Test(expected = TestException::class)
    fun `throw exception when mapping in get`() {
        runBlocking {
            // given we throw an exception on get
            Mockito.`when`(cache.get("key")).then { async(CommonPool) { throw TestException() } }

            // when we get the value from a map
            mappedValuesCache.get("key").await()

            // then an exception is thrown
        }
    }

    // set
    @Test
    fun `not interact with parent set`() {
        runBlocking {
            // when we set the value
            @Suppress("DEPRECATION")
            mappedValuesCache.set("1", 1).await()

            // then the parent cache is not called
            Mockito.verifyNoMoreInteractions(cache)
        }
    }

    @Test
    fun `not interact with transform during set`() {
        runBlocking {
            // when we set the value
            @Suppress("DEPRECATION")
            mappedValuesCache.set("1", 1).await()

            // then the parent cache is not called
            Mockito.verifyZeroInteractions(function)
        }
    }

    // evict
    @Test
    fun `not interact with parent evict`() {
        runBlocking {
            // when we set the value
            @Suppress("DEPRECATION")
            mappedValuesCache.evict("1").await()

            // then the parent cache is not called
            Mockito.verifyNoMoreInteractions(cache)
        }
    }

    @Test
    fun `not interact with transform during evict`() {
        runBlocking {
            // when we set the value
            @Suppress("DEPRECATION")
            mappedValuesCache.evict("1").await()

            // then the parent cache is not called
            Mockito.verifyZeroInteractions(function)
        }
    }
}