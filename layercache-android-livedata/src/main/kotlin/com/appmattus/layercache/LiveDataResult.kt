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
