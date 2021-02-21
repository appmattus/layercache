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

package com.appmattus.layercache.samples.di

import android.content.Context
import com.appmattus.battery.Battery
import com.appmattus.connectivity.Connectivity
import com.appmattus.packageinfo.PackageInfo
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object SamplesModule {

    @Provides
    fun provideBattery(@ApplicationContext context: Context) = Battery(context)

    @Provides
    fun provideConnectivity(@ApplicationContext context: Context) = Connectivity(context)

    @Provides
    fun providePackageInfo(@ApplicationContext context: Context) = PackageInfo(context)
}
