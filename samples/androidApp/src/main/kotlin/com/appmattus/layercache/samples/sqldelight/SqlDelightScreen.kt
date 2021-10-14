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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.appmattus.layercache.samples.ui.component.TwoLineText

@Composable
fun SqlDelightScreen(viewModel: SqlDelightViewModel) {

    val state = viewModel.container.stateFlow.collectAsState().value

    Column(modifier = Modifier.fillMaxHeight()) {

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "SqlDelight",
            style = MaterialTheme.typography.h6,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        Row(modifier = Modifier.padding(horizontal = 16.dp)) {
            Button(onClick = { viewModel.loadPersonalDetails() }, modifier = Modifier.padding(vertical = 8.dp)) {
                Text("Load data")
            }
            Spacer(modifier = Modifier.width(16.dp))
            Button(onClick = { viewModel.clear() }, modifier = Modifier.padding(vertical = 8.dp)) {
                Text("Clear")
            }
        }

        Text(
            text = "Response data",
            style = MaterialTheme.typography.h6,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        state.personalDetails?.let {
            TwoLineText(
                title = it.name,
                subtitle = it.tagline,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            TwoLineText(
                title = "Retrieved from",
                subtitle = state.loadedFrom,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}
