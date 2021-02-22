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

package com.appmattus.layercache.samples.data.database.mapper

import com.appmattus.layercache.samples.data.database.PersonalDetails

internal fun PersonalDetails.toDomainEntity() = com.appmattus.layercache.samples.domain.PersonalDetails(
    name = name,
    tagline = tagline,
    location = location,
    avatarUrl = avatarUrl
)

internal fun com.appmattus.layercache.samples.domain.PersonalDetails.toDatabaseEntity() = PersonalDetails(
    name = name,
    tagline = tagline,
    location = location,
    avatarUrl = avatarUrl
)
