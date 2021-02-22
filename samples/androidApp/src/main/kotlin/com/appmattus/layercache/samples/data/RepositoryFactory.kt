package com.appmattus.layercache.samples.data

import android.content.Context
import com.appmattus.layercache.asStringCache
import com.appmattus.layercache.encrypt
import com.appmattus.layercache.encryptValues
import com.appmattus.layercache.get
import com.appmattus.layercache.samples.data.database.SqlDelightDataSource
import com.appmattus.layercache.samples.data.network.KtorDataSource
import com.appmattus.layercache.samples.domain.PersonalDetails
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class RepositoryFactory(context: Context) {

    val database = SqlDelightDataSource(context)

    private val encryptedSharedPreferences = context.getSharedPreferences("encrypted", Context.MODE_PRIVATE).asStringCache()//.encrypt(context)

    //val x = SharedPreferences()
    private val repository = SqlDelightDataSource(context)
        .valueTransform(transform = {
            Json.encodeToString(it)
        }, inverseTransform = {
            Json.decodeFromString(it)
        })
        .encryptValues(context)

    init {
        runBlocking {

            val pd = encryptedSharedPreferences.keyTransform<Unit> {
                "personalDetails"
            }.valueTransform(transform = {
                Json.decodeFromString<PersonalDetails>(it)
            }, inverseTransform = {
                Json.encodeToString(it)
            })

            val pd2 = pd.compose(KtorDataSource())

            val value2 = pd2.get()


            val value = repository.get()

            value2?.name + value
        }
    }
}
