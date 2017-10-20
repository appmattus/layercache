package com.mattdolan.layercache

import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.runBlocking
import org.junit.Assert
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class DeferredOnFailureShould {

    @Test
    fun `not call onFailure on success`() {
        runBlocking {
            // given we have a job that throws an exception
            val latch = CountDownLatch(1)
            val job = async(CommonPool) {
                "value"
            }

            // when we attach a listener and wait for the result
            job.onFailure { latch.countDown() }
            job.completeAndWait(250, TimeUnit.MILLISECONDS)

            // then onFailure is not called
            Assert.assertEquals(1, latch.count)
        }
    }

    @Test
    fun `not call onFailure on cancellation`() {
        runBlocking {
            // given we have a job that throws an exception
            val latch = CountDownLatch(1)
            val job = async(CommonPool) {
                delay(500, TimeUnit.MILLISECONDS)
            }

            // when we attach a listener, cancel the request and wait for the result
            job.onFailure { latch.countDown() }
            job.cancel()
            job.completeAndWait(250, TimeUnit.MILLISECONDS)

            // then onFailure is not called
            Assert.assertEquals(1, latch.count)
        }
    }

    @Test
    fun `call onFailure with exception on failure`() {
        runBlocking {
            // given we have a job that throws an exception
            val latch = CountDownLatch(1)
            val job = async(CommonPool) {
                throw TestException()
            }

            // when we attach a listener and wait for the result
            job.onFailure {
                if (it is TestException) {
                    latch.countDown()
                }
            }
            job.completeAndWait(250, TimeUnit.MILLISECONDS)

            // then onFailure is called supplying the exception
            Assert.assertEquals(0, latch.count)
        }
    }

    private suspend fun Deferred<*>.completeAndWait(time: Long, unit: TimeUnit = TimeUnit.MILLISECONDS) {
        join()
        // yield to allow parallel jobs time to complete
        delay(time, unit)
    }
}