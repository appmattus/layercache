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

@file:Suppress("IllegalIdentifier")

package com.appmattus.layercache

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.mock.BehaviorDelegate
import retrofit2.mock.MockRetrofit
import retrofit2.mock.NetworkBehavior

class RetrofitWrapperShould {

    interface RetrofitService {
        @GET("name")
        fun listName(@Path("name") name: String): Call<String>
    }

    class MockRetrofitService(private val serviceDelegate: BehaviorDelegate<RetrofitService>) : RetrofitService {
        override fun listName(name: String): Call<String> {
            return serviceDelegate.returningResponse("$name-a").listName(name)
        }
    }

    private lateinit var service: RetrofitService

    @Before
    fun before() {
        val retrofit = Retrofit.Builder().baseUrl("http://example.com").build()
        val networkBehavior = NetworkBehavior.create().apply {
            setFailurePercent(0)
        }

        val mockRetrofit = MockRetrofit.Builder(retrofit).networkBehavior(networkBehavior).build()

        service = MockRetrofitService(mockRetrofit.create(RetrofitService::class.java))
    }

    @Test
    fun `return value from service`() {
        runBlocking {
            val cache = Cache.fromRetrofit { key: String ->
                service.listName(key)
            }

            assertEquals("key-a", cache.get("key"))

            assertEquals("key2-a", cache.get("key2"))
        }
    }

    @Test
    fun `chain in memory cache with retrofit`() {
        runBlocking {
            val memoryCache = MapCache()
            val networkCache = Cache.fromRetrofit { key: String ->
                service.listName(key)
            }

            assertEquals(null, memoryCache.get("key"))

            val cache = memoryCache.compose(networkCache)

            assertEquals("key-a", cache.get("key"))
            assertEquals("key-a", memoryCache.get("key"))
        }
    }
}
