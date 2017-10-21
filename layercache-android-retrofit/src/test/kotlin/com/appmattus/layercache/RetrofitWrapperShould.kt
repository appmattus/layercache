package com.appmattus.layercache

import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Path

class RetrofitWrapperShould {
    interface GitHubService {
        @GET("users/{user}/repos")
        fun listRepos(@Path("user") user: String): Call<List<String>>
    }
    
    fun testIt() {
        val retrofit: Retrofit = TestUtils.uninitialized()

        val service = retrofit.create(GitHubService::class.java)

        val x = RetrofitWrapper({ key: String ->
            service.listRepos(key)
        })

        val y = RetrofitWrapper { key: String ->
            service.listRepos(key)
        }
    }
}