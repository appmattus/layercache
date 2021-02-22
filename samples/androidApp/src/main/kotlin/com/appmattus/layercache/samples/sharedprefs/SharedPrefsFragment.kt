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

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.appmattus.layercache.samples.R
import com.appmattus.layercache.samples.databinding.RecyclerViewFragmentBinding
import com.appmattus.layercache.samples.ui.component.ButtonItem
import com.appmattus.layercache.samples.ui.component.SingleLineTextHeaderItem
import com.appmattus.layercache.samples.ui.component.TwoLineTextItem
import com.appmattus.layercache.samples.ui.viewBinding
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import com.xwray.groupie.Section
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SharedPrefsFragment : Fragment(R.layout.recycler_view_fragment) {

    private val binding by viewBinding<RecyclerViewFragmentBinding>()

    private val viewModel by viewModels<SharedPrefsViewModel>()

    private val actionSection = Section().apply {
        add(ButtonItem("Load data") {
            viewModel.loadPersonalDetails()
        })
        add(ButtonItem("Clear") {
            viewModel.clear()
        })
    }
    private val personalDetailsSection = Section()
    private val preferencesSection = Section()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, false)
            adapter = GroupAdapter<GroupieViewHolder>().apply {
                add(SingleLineTextHeaderItem("Encrypted shared preferences"))
                add(actionSection)
                add(SingleLineTextHeaderItem("Response data"))
                add(personalDetailsSection)
                add(SingleLineTextHeaderItem("Preferences content"))
                add(preferencesSection)
            }
        }

        lifecycleScope.launch {
            viewModel.container.stateFlow.collect(::render)
        }
    }

    private fun render(state: SharedPrefsState) {
        state.personalDetails?.let {
            listOf(
                TwoLineTextItem(
                    primaryText = it.name,
                    secondaryText = it.tagline
                ),
                TwoLineTextItem(
                    primaryText = "Retrieved from",
                    secondaryText = state.loadedFrom
                )
            ).let(personalDetailsSection::update)
        } ?: personalDetailsSection.clear()

        state.preferences.map {
            TwoLineTextItem(
                primaryText = it.key,
                secondaryText = it.value.toString()
            )
        }.let(preferencesSection::update)
    }
}
