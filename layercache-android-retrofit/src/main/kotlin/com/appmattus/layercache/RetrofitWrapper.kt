package com.appmattus.layercache

import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.async
import retrofit2.Call

class RetrofitWrapper<Key : Any, Value : Any>(val retrofitCall: (Key) -> Call<Value>) : Fetcher<Key, Value> {

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
