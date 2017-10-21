package com.appmattus.layercache

import com.jakewharton.disklrucache.DiskLruCache
import kotlinx.coroutines.experimental.runBlocking
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations

class DiskLruCacheWrapperShould {

    @Mock
    private lateinit var lruCache: DiskLruCache

    @Mock
    private lateinit var snapshot: DiskLruCache.Snapshot

    private lateinit var wrappedCache: Cache<String, String>

    @Before
    fun before() {
        MockitoAnnotations.initMocks(this)
        wrappedCache = DiskLruCacheWrapper(lruCache)
    }

    // get
    @Test
    fun get_returns_value_from_cache() {
        runBlocking {
            // given value available in first cache only
            Mockito.`when`(snapshot.getString(0)).thenReturn("value")
            Mockito.`when`(lruCache.get("key")).thenReturn(snapshot)

            // when we get the value
            val result = wrappedCache.get("key").await()

            // then we return the value
            Assert.assertEquals("value", result)
        }
    }

    @Test(expected = TestException::class)
    fun get_throws() {
        runBlocking {
            // given value available in first cache only
            Mockito.`when`(lruCache.get("key")).then { throw TestException() }

            // when we get the value
            wrappedCache.get("key").await()

            // then we throw an exception
        }
    }

    // set
    /*@Test
    fun set_returns_value_from_cache() {
        runBlocking {
            // given

            // when we set the value
            wrappedCache.set("key", "value").await()

            // then put is called
            Mockito.verify(lruCache).put("key", "value")
        }
    }

    @Test(expected = TestException::class)
    fun set_throws() {
        runBlocking {
            // given value available in first cache only
            Mockito.`when`(lruCache.put("key", "value")).then { throw TestException() }

            // when we get the value
            wrappedCache.set("key", "value").await()

            // then we throw an exception
        }
    }

    // evict
    @Test
    fun evict_returns_value_from_cache() {
        runBlocking {
            // given

            // when we get the value
            wrappedCache.evict("key").await()

            // then we return the value
            //assertEquals("value", result)
            Mockito.verify(lruCache).remove("key")
        }
    }

    @Test(expected = TestException::class)
    fun evict_throws() {
        runBlocking {
            // given value available in first cache only
            Mockito.`when`(lruCache.remove("key")).then { throw TestException() }

            // when we get the value
            wrappedCache.evict("key").await()

            // then we throw an exception
        }
    }*/
}