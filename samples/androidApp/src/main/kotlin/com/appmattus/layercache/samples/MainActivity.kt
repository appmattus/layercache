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

package com.appmattus.layercache.samples

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.appmattus.layercache.samples.sharedprefs.SharedPrefsScreen
import com.appmattus.layercache.samples.sharedprefs.SharedPrefsViewModel
import com.appmattus.layercache.samples.sqldelight.SqlDelightScreen
import com.appmattus.layercache.samples.sqldelight.SqlDelightViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val navController = rememberNavController()
            NavHost(navController, startDestination = "main") {
                composable("main") {
                    MainScreen(navController = navController)
                }
                composable("sharedPrefs") {
                    val viewModel by viewModels<SharedPrefsViewModel>()
                    SharedPrefsScreen(viewModel = viewModel)
                }
                composable("sqlDelight") {
                    val viewModel by viewModels<SqlDelightViewModel>()
                    SqlDelightScreen(viewModel = viewModel)
                }
            }
        }
    }
}
