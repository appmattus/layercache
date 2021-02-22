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

package com.appmattus.layercache.samples.packageinfo

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import org.orbitmvi.orbit.Container
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.syntax.simple.intent
import org.orbitmvi.orbit.syntax.simple.reduce
import org.orbitmvi.orbit.viewmodel.container

@HiltViewModel
class PackageInfoViewModel @Inject constructor(
    //private val packageInfo: PackageInfo,
    savedStateHandle: SavedStateHandle
) : ViewModel(), ContainerHost<PackageInfoState, Unit> {

    override val container: Container<PackageInfoState, Unit> = container(PackageInfoState(), savedStateHandle) {
        if (it.appName.isEmpty()) loadPackageInfo()
    }

    private fun loadPackageInfo() = intent {
        /*val appName = packageInfo.appName ?: "n/a"
        val packageName = packageInfo.packageName ?: "n/a"
        val version = packageInfo.version ?: "n/a"
        val buildNumber = packageInfo.buildNumber ?: "n/a"

        reduce {
            state.copy(
                appName = appName,
                packageName = packageName,
                version = version,
                buildNumber = buildNumber,
            )
        }*/
    }
}
