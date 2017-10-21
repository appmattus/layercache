package com.appmattus.layercache

import retrofit2.Response

class RetrofitException(val response: Response<*>) : Exception(response.message())
