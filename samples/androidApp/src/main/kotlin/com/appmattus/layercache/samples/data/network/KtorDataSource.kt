/*
 * Copyright 2021 Appmattus Limited
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

package com.appmattus.layercache.samples.data.network

import com.appmattus.layercache.Fetcher
import com.appmattus.layercache.samples.domain.PersonalDetails
import io.ktor.client.HttpClient
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.features.json.serializer.KotlinxSerializer
import io.ktor.client.request.get
import io.ktor.http.ContentType
import io.ktor.http.Url
import io.ktor.http.contentType

class KtorDataSource(
    private val client: HttpClient = HttpClient {
        install(JsonFeature) {
            serializer = KotlinxSerializer()
        }
    }
) : Fetcher<Unit, PersonalDetails> {

    override suspend fun get(key: Unit): PersonalDetails {
        return client.get<PersonalDetailsNetworkEntity>(Url("https://mattdolan.com/cv/api/personal-details.json")) {
            contentType(ContentType.Application.Json)
        }.toDomainEntity()
    }
}
