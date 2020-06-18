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

import android.util.LruCache
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class LiveDataCacheShould {

    companion object {
        val TIMEOUT_IN_MILLIS = TimeUnit.SECONDS.toMillis(10)
    }

    private val lruCache = mock<LruCache<String, String>>()

    @Test
    fun receive_value() {
        runBlocking {
            val latch = CountDownLatch(1)

            // given value available in first cache only
            whenever(lruCache.get("key")).then { "value" }

            val liveDataCache = Cache.fromLruCache(lruCache).toLiveData()

            val liveData = liveDataCache.get("key")

            liveData.observeForever { liveDataResult ->
                when (liveDataResult) {
                    is LiveDataResult.Success -> {
                        latch.countDown()
                    }
                }
            }

            // Force LiveData processing on main thread
            val start = System.currentTimeMillis()
            while (latch.count != 0L && (System.currentTimeMillis() - start) < TIMEOUT_IN_MILLIS) {
                Robolectric.flushForegroundThreadScheduler()
            }

            assertTrue(latch.count == 0L)
        }
    }
}
