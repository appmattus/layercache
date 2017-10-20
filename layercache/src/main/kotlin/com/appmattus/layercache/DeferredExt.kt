/**
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

import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.async

/**
 * Sealed class representing the result of a deferred, one of Success, Failure or Cancelled
 */
@Suppress("unused")
sealed class DeferredResult<Value> {
    /**
     * Success, contains the value returned by the deferred execution
     */
    class Success<Value>(val value: Value?) : DeferredResult<Value>()

    /**
     * Failure, contains the exception thrown in the deferred execution
     */
    class Failure<Value>(val exception: Throwable) : DeferredResult<Value>()

    /**
     * Cancelled, contains the exception thrown by cancelling the deferred execution
     */
    class Cancelled<Value>(val exception: Throwable) : DeferredResult<Value>()
}

/**
 * Executes completion handler with a DeferredResult when the Deferred completes, regardless of status
 */
fun <Value> Deferred<Value>.onCompletion(completion: (result: DeferredResult<Value>) -> Unit): Deferred<Value> {
    async(CommonPool) {
        join()

        @Suppress("RemoveExplicitTypeArguments")
        when {
            isCancelled -> DeferredResult.Cancelled<Value>(getCompletionException())
            isCompletedExceptionally -> DeferredResult.Failure<Value>(getCompletionException())
            else -> DeferredResult.Success<Value>(getCompleted())
        }.let {
            completion(it)
        }
    }

    return this
}

/**
 * Executes success handler only when Deferred completes successfully returning its data
 */
fun <Value> Deferred<Value>.onSuccess(success: (value: Value?) -> Unit): Deferred<Value> {
    async(CommonPool) {
        join()

        if (!isCancelled && !isCompletedExceptionally) {
            success(getCompleted())
        }
    }

    return this
}

/**
 * Executes failure handler only when Deferred throws an exception
 */
fun <Value> Deferred<Value>.onFailure(failure: (exception: Throwable) -> Unit): Deferred<Value> {
    async(CommonPool) {
        join()

        if (!isCancelled && isCompletedExceptionally) {
            failure(getCompletionException())
        }
    }

    return this
}

/**
 * Executes cancelled handler only when Deferred is cancelled by calling job.cancel()
 */
fun <Value> Deferred<Value>.onCancel(cancelled: (exception: Throwable) -> Unit): Deferred<Value> {
    async(CommonPool) {
        join()

        if (isCancelled) {
            cancelled(getCompletionException())
        }
    }

    return this
}
