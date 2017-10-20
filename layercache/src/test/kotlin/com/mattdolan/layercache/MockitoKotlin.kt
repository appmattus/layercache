package com.mattdolan.layercache

import org.mockito.Mockito

class MockitoKotlin {
    companion object {
        fun <T> any(type: Class<T>): T {
            Mockito.any<T>(type)
            return TestUtils.uninitialized()
        }

        fun <T> any(): T {
            Mockito.any<T>()
            return TestUtils.uninitialized()
        }
    }
}