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

package com.appmattus.layercache.samples.data.database

import android.content.Context
import com.appmattus.layercache.Cache
import com.appmattus.layercache.samples.data.database.mapper.toDatabaseEntity
import com.appmattus.layercache.samples.data.database.mapper.toDomainEntity
import com.appmattus.layercache.samples.domain.PersonalDetails
import com.squareup.sqldelight.android.AndroidSqliteDriver

class SqldelightDataSource(context: Context) : Cache<Unit, PersonalDetails> {

    private val database = AppDatabase(AndroidSqliteDriver(AppDatabase.Schema, context, "cache.db"))

    override suspend fun get(key: Unit): PersonalDetails? =
        database.personalDetailsQueries.personalDetails().executeAsOneOrNull()?.toDomainEntity()

    override suspend fun set(key: Unit, value: PersonalDetails) {
        val entity = value.toDatabaseEntity()

        database.personalDetailsQueries.transaction {
            database.personalDetailsQueries.deletePersonalDetails()
            database.personalDetailsQueries.insertPersonalDetails(
                name = entity.name,
                tagline = entity.tagline,
                location = entity.location,
                avatarUrl = entity.avatarUrl
            )
        }
    }

    override suspend fun evict(key: Unit) = database.personalDetailsQueries.deletePersonalDetails()

    override suspend fun evictAll() = database.personalDetailsQueries.deletePersonalDetails()
}
