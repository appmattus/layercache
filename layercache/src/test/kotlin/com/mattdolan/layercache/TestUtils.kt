package com.mattdolan.layercache

import java.util.concurrent.TimeUnit

class TestUtils {
    companion object {
        fun blockingTask(duration: Long, unit: TimeUnit = TimeUnit.MILLISECONDS) {
            val start = System.nanoTime()
            while (TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start) < unit.toMillis(duration)) {
            }
        }

        @Suppress("UNCHECKED_CAST")
        fun <T> uninitialized(): T = null as T
    }
}