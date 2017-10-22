package com.appmattus.layercache

/**
 * Sealed class representing the status of a cache request, one of Success, Failure or Loading
 */
sealed class LiveDataResult<Value> {
    /**
     * Success, contains the value returned by the cache execution
     */
    class Success<Value>(val value: Value) : LiveDataResult<Value>()

    /**
     * Failure, contains the exception thrown by the cache execution
     */
    class Failure<Value>(val exception: Throwable) : LiveDataResult<Value>()

    /**
     * Loading, when a cache execution is in progress and is not yet Success or Failure
     */
    class Loading<Value> : LiveDataResult<Value>()
}
