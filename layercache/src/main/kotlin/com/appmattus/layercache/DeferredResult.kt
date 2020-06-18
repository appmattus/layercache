package com.appmattus.layercache

/**
 * Sealed class representing the result of a deferred, one of Success, Failure or Cancelled
 */
@Suppress("unused")
sealed class DeferredResult<Value> {
    /**
     * Success, contains the value returned by the deferred execution
     * @property value Result of the Deferred
     */
    class Success<Value>(val value: Value?) : DeferredResult<Value>()

    /**
     * Cancelled, contains the exception thrown by cancelling the deferred execution
     * @property exception Cancellation exception
     */
    class Cancelled<Value>(val exception: Throwable?) : DeferredResult<Value>()
}
