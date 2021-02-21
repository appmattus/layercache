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
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.appmattus.layercache.samples.ui.component.SingleLineTextHeaderItem
import com.appmattus.layercache.samples.ui.component.SingleLineTextItem
import com.appmattus.layercache.samples.databinding.RecyclerViewFragmentBinding
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SamplesFragment : Fragment() {

    private lateinit var binding: RecyclerViewFragmentBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        RecyclerViewFragmentBinding.inflate(inflater, container, false).also { binding = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, false)
            adapter = GroupAdapter<GroupieViewHolder>().apply {
                add(SingleLineTextHeaderItem("Samples"))
                add(SingleLineTextItem("battery") {
                    findNavController().navigate(R.id.action_samplesFragment_to_batteryFragment)
                })

                add(SingleLineTextItem("connectivity") {
                    findNavController().navigate(R.id.action_samplesFragment_to_connectivityFragment)
                })

                add(SingleLineTextItem("package-info") {
                    findNavController().navigate(R.id.action_samplesFragment_to_packageInfoFragment)
                })
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Fix memory leak with RecyclerView
        binding.recyclerView.adapter = null
    }
}
