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

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

/**
 * Executes completion handler with a DeferredResult when the Deferred completes, regardless of status
 */
fun <Value> Deferred<Value>.onCompletion(completion: (result: DeferredResult<Value>) -> Unit): Deferred<Value> {
    GlobalScope.async {
        join()

        @Suppress("RemoveExplicitTypeArguments")
        when {
            isCancelled -> DeferredResult.Cancelled<Value>(getCompletionExceptionOrNull())
            else -> DeferredResult.Success<Value>(getCompleted())
        }.let {
            GlobalScope.launch {
                completion(it)
            }
        }
    }

    return this
}

/**
 * Executes success handler only when Deferred completes successfully returning its data
 */
fun <Value> Deferred<Value>.onSuccess(success: (value: Value?) -> Unit): Deferred<Value> {
    GlobalScope.async {
        join()

        if (!isCancelled) {
            GlobalScope.launch {
                success(getCompleted())
            }
        }
    }

    return this
}

/**
 * Executes cancelled handler only when Deferred is cancelled by calling job.cancel()
 */
fun <Value> Deferred<Value>.onCancel(cancelled: (exception: Throwable?) -> Unit): Deferred<Value> {
    GlobalScope.async {
        join()

        if (isCancelled) {
            GlobalScope.launch {
                cancelled(getCompletionExceptionOrNull())
            }
        }
    }

    return this
}
