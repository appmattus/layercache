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
import retrofit2.Call

internal class RetrofitWrapper<Key : Any, Value : Any>(val retrofitCall: (Key) -> Call<Value>) : Fetcher<Key, Value> {

    override fun get(key: Key): Deferred<Value?> {
        return async(CommonPool) {
            val response = retrofitCall(key).execute()

            if (!response.isSuccessful) {
                throw RetrofitException(response)
            }

            response.body()
        }
    }
}

@Suppress("unused", "USELESS_CAST")
fun <Key : Any, Value : Any> Cache.Companion.fromRetrofit(retrofitCall: (Key) -> Call<Value>) = RetrofitWrapper(retrofitCall) as Fetcher<Key, Value>
