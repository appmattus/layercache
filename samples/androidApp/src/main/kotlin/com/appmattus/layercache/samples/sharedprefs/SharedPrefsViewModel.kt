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

package com.appmattus.layercache.samples.sharedprefs

import android.content.Context
import androidx.lifecycle.ViewModel
import com.appmattus.layercache.Cache
import com.appmattus.layercache.asStringCache
import com.appmattus.layercache.encrypt
import com.appmattus.layercache.get
import com.appmattus.layercache.samples.data.LastRetrievedWrapper
import com.appmattus.layercache.samples.data.network.KtorDataSource
import com.appmattus.layercache.samples.domain.PersonalDetails
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.json.Json.Default.decodeFromString
import kotlinx.serialization.json.Json.Default.encodeToString
import org.orbitmvi.orbit.Container
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.syntax.simple.intent
import org.orbitmvi.orbit.syntax.simple.reduce
import org.orbitmvi.orbit.viewmodel.container
import javax.inject.Inject

@HiltViewModel
class SharedPrefsViewModel @Inject constructor(
    @ApplicationContext context: Context
) : ViewModel(), ContainerHost<SharedPrefsState, Unit> {

    // Stores the name of the cache data was returned from
    private val lastRetrievedWrapper = LastRetrievedWrapper()

    // Network fetcher, wrapped so we can detect when get returns a value
    private val ktorDataSource: Cache<Unit, PersonalDetails> = with(lastRetrievedWrapper) { KtorDataSource().wrap("Ktor network call") }

    // Encrypted shared preferences cache
    private val sharedPreferences = context.getSharedPreferences("encrypted", Context.MODE_PRIVATE)
    private val encryptedSharedPreferencesDataSource: Cache<Unit, PersonalDetails> = with(lastRetrievedWrapper) {
        sharedPreferences
            // Access as a Cache<String, String>
            .asStringCache()
            // Wrap the cache so we can detect when get returns a value
            .wrap("Shared preferences")
            // Encrypt all keys and values stored in this cache
            .encrypt(context)
            // We are only storing one value so using a default key and mapping externally to Unit, i.e. cache becomes Cache<Unit, String>
            .keyTransform<Unit> {
                "personalDetails"
            }
            // Transform string values to PersonalDetails, i.e. cache becomes Cache<Unit, PersonalDetails>
            .valueTransform(transform = {
                decodeFromString(PersonalDetails.serializer(), it)
            }, inverseTransform = {
                encodeToString(PersonalDetails.serializer(), it)
            })
    }

    // Combine shared preferences and ktor caches, i.e. first retrieve value from shared preferences and if not available retrieve from network
    private val repository = encryptedSharedPreferencesDataSource.compose(ktorDataSource)

    override val container: Container<SharedPrefsState, Unit> = container(SharedPrefsState()) {
        loadPreferencesContent()
    }

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

        loadPreferencesContent()
    }

    // Update the state with the current contents of shared preferences so we can demonstrate that the data is stored encrypted
    private fun loadPreferencesContent() = intent {
        val content = sharedPreferences.all

        reduce {
            state.copy(preferences = content)
        }
    }

    // Clear all data from shared preferences and the state object
    fun clear() = intent {
        reduce {
            state.copy(personalDetails = null, loadedFrom = "")
        }

        sharedPreferences.edit().clear().apply()
        loadPreferencesContent()
    }
}
