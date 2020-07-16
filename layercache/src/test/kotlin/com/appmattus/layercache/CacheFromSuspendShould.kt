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

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.mock.BehaviorDelegate
import retrofit2.mock.MockRetrofit
import retrofit2.mock.NetworkBehavior

class CacheFromSuspendShould {

    interface RetrofitService {
        @GET("name")
        suspend fun listName(@Path("name") name: String): String
    }

    class MockRetrofitService(private val serviceDelegate: BehaviorDelegate<RetrofitService>) : RetrofitService {
        override suspend fun listName(name: String): String {
            return serviceDelegate.returningResponse("$name-a").listName(name)
        }
    }

    private val mockRetrofit = MockRetrofit.Builder(Retrofit.Builder().baseUrl("http://example.com").build())
        .networkBehavior(NetworkBehavior.create().apply { setFailurePercent(0) })
        .build()

    private val service: RetrofitService = MockRetrofitService(mockRetrofit.create(RetrofitService::class.java))

    @Test
    fun `chain in memory cache with retrofit`() {
        val memoryCache = MapCache()
        val networkCache = cache { key: String -> service.listName(key) }.reuseInflight()

        assertEquals(null, runBlocking { memoryCache.get("key") })

        val cache = memoryCache + networkCache

        assertEquals("key-a", runBlocking { cache.get("key") })
        assertEquals("key-a", runBlocking { memoryCache.get("key") })
    }
}
