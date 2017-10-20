package com.mattdolan.layercache

import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class DeferredOnSuccessShould {

    @Test
    fun `not call onSuccess on failure`() {
        runBlocking {
            // given we have a job that throws an exception
            val latch = CountDownLatch(1)
            val job = async(CommonPool) {
                throw TestException()
            }

            // when we attach a listener and wait for the result
            job.onSuccess { latch.countDown() }
            job.completeAndWait(250, TimeUnit.MILLISECONDS)

            // then onSuccess is not called
            assertEquals(1, latch.count)
        }
    }

    @Test
    fun `not call onSuccess on cancellation`() {
        runBlocking {
            // given we have a job that throws an exception
            val latch = CountDownLatch(1)
            val job = async(CommonPool) {
                delay(500, TimeUnit.MILLISECONDS)
            }

            // when we attach a listener, cancel the request and wait for the result
            job.onSuccess { latch.countDown() }
            job.cancel()

            job.completeAndWait(250, TimeUnit.MILLISECONDS)

            // then onSuccess is not called
            assertEquals(1, latch.count)
        }
    }

    @Test
    fun `call onSuccess with value on success`() {
        runBlocking {
            // given we have a job that throws an exception
            val latch = CountDownLatch(1)
            val job = async(CommonPool) {
                "value"
            }

            // when we attach a listener and wait for the result
            job.onSuccess {
                if (it == "value") {
                    latch.countDown()
                }
            }

            job.completeAndWait(250, TimeUnit.MILLISECONDS)

            // then onSuccess is called supplying the value
            assertEquals(0, latch.count)
        }
    }

    private suspend fun Deferred<*>.completeAndWait(time: Long, unit: TimeUnit = TimeUnit.MILLISECONDS) {
        join()
        // yield to allow parallel jobs time to complete
        delay(time, unit)
    }
}