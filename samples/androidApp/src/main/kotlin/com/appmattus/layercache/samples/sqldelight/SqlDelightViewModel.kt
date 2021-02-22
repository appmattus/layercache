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

package com.appmattus.layercache.samples.sqldelight

import android.content.Context
import androidx.lifecycle.ViewModel
import com.appmattus.layercache.Cache
import com.appmattus.layercache.get
import com.appmattus.layercache.samples.data.LastRetrievedWrapper
import com.appmattus.layercache.samples.data.database.SqlDelightDataSource
import com.appmattus.layercache.samples.data.network.KtorDataSource
import com.appmattus.layercache.samples.domain.PersonalDetails
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import org.orbitmvi.orbit.Container
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.syntax.simple.intent
import org.orbitmvi.orbit.syntax.simple.reduce
import org.orbitmvi.orbit.viewmodel.container
import javax.inject.Inject

@HiltViewModel
class SqlDelightViewModel @Inject constructor(
    @ApplicationContext context: Context
) : ViewModel(), ContainerHost<SqlDelightState, Unit> {

    // Stores the name of the cache data was returned from
    private val lastRetrievedWrapper = LastRetrievedWrapper()

    // Network fetcher, wrapped so we can detect when get returns a value
    private val ktorDataSource: Cache<Unit, PersonalDetails> = with(lastRetrievedWrapper) { KtorDataSource().wrap("Ktor network call") }

    // Database cache, wrapped so we can detect when get returns a value
    private val sqlDelightDataSource = with(lastRetrievedWrapper) { SqlDelightDataSource(context).wrap("SqlDelight database") }

    // Combine sql delight and ktor caches, i.e. first retrieve value from sql delight and if not available retrieve from network
    private val repository = sqlDelightDataSource.compose(ktorDataSource)

    override val container: Container<SqlDelightState, Unit> = container(SqlDelightState())

    // Update state with personal details retrieved from the repository
    fun loadPersonalDetails() = intent {
        lastRetrievedWrapper.reset()

        reduce {
            state.copy(personalDetails = null, loadedFrom = "")
        }

        val personalDetails = repository.get()

        reduce {
            state.copy(personalDetails = personalDetails, loadedFrom = lastRetrievedWrapper.lastRetrieved ?: "")
        }
    }

    // Clear all data from database and the state object
    fun clear() = intent {
        reduce {
            state.copy(personalDetails = null, loadedFrom = "")
        }

        sqlDelightDataSource.evictAll()
    }
}
