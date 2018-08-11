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

import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.DefaultDispatcher
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.launch
import kotlin.coroutines.experimental.CoroutineContext

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
     * Failure, contains the exception thrown in the deferred execution
     * @property exception Thrown exception
     */
    class Failure<Value>(val exception: Throwable) : DeferredResult<Value>()

    /**
     * Cancelled, contains the exception thrown by cancelling the deferred execution
     * @property exception Cancellation exception
     */
    class Cancelled<Value>(val exception: Throwable) : DeferredResult<Value>()
}

/**
 * Executes completion handler with a DeferredResult when the Deferred completes, regardless of status
 */
fun <Value> Deferred<Value>.onCompletion(context: CoroutineContext = DefaultDispatcher, completion: (result: DeferredResult<Value>) -> Unit): Deferred<Value> {
    async(CommonPool) {
        join()

        @Suppress("RemoveExplicitTypeArguments")
        when {
            isCancelled -> DeferredResult.Cancelled<Value>(getCancellationException())
            isCompletedExceptionally -> DeferredResult.Failure<Value>(getCancellationException())
            else -> DeferredResult.Success<Value>(getCompleted())
        }.let {
            launch(context) {
                completion(it)
            }
        }
    }

    return this
}

/**
 * Executes success handler only when Deferred completes successfully returning its data
 */
fun <Value> Deferred<Value>.onSuccess(context: CoroutineContext = DefaultDispatcher, success: (value: Value?) -> Unit): Deferred<Value> {
    async(CommonPool) {
        join()

        if (!isCancelled && !isCompletedExceptionally) {
            launch(context) {
                success(getCompleted())
            }
        }
    }

    return this
}

/**
 * Executes failure handler only when Deferred throws an exception
 */
fun <Value> Deferred<Value>.onFailure(context: CoroutineContext = DefaultDispatcher, failure: (exception: Throwable) -> Unit): Deferred<Value> {
    async(CommonPool) {
        join()

        if (!isCancelled && isCompletedExceptionally) {
            launch(context) {
                failure(getCancellationException())
            }
        }
    }

    return this
}

/**
 * Executes cancelled handler only when Deferred is cancelled by calling job.cancel()
 */
fun <Value> Deferred<Value>.onCancel(context: CoroutineContext = DefaultDispatcher, cancelled: (exception: Throwable) -> Unit): Deferred<Value> {
    async(CommonPool) {
        join()

        if (isCancelled) {
            launch(context) {
                cancelled(getCancellationException())
            }
        }
    }

    return this
}
