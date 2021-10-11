/*
 * Copyright 2021 Appmattus Limited
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

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.robolectric.Robolectric
import org.robolectric.annotation.Config
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class LiveDataCacheShould {

    companion object {
        val TIMEOUT_IN_MILLIS = TimeUnit.SECONDS.toMillis(10)
    }

    private val cache = mock<Cache<String, String>> {
        onBlocking { get("key") } doReturn "value"
    }

    private val liveDataCache = cache.toLiveData()

    @Test
    fun emitsLoading() {
        val latch = CountDownLatch(1)

        liveDataCache["key"].observeForever {
            if (it is LiveDataResult.Loading) latch.countDown()
        }

        flushMainThread(latch)

        assertTrue(latch.count == 0L)
    }

    @Test
    fun emitsSuccess() {
        val latch = CountDownLatch(1)

        liveDataCache["key"].observeForever {
            if (it is LiveDataResult.Success && it.value == "value") latch.countDown()
        }

        flushMainThread(latch)

        assertTrue(latch.count == 0L)
    }

    @Test
    fun emitsFailure() {
        val latch = CountDownLatch(1)

        cache.stub {
            onBlocking { get("key") }.then { throw IllegalStateException() }
        }

        liveDataCache["key"].observeForever {
            if (it is LiveDataResult.Failure && it.exception is IllegalStateException) latch.countDown()
        }

        flushMainThread(latch)

        assertTrue(latch.count == 0L)
    }

    /**
     * Force LiveData processing on main thread
     */
    private fun flushMainThread(latch: CountDownLatch) {
        val start = System.currentTimeMillis()
        while (latch.count != 0L && (System.currentTimeMillis() - start) < TIMEOUT_IN_MILLIS) {
            Robolectric.flushForegroundThreadScheduler()
        }
    }
}
