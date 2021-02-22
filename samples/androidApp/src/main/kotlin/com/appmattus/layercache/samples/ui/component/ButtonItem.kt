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

package com.appmattus.layercache.samples.ui.component

import android.view.View
import com.appmattus.layercache.samples.R
import com.appmattus.layercache.samples.databinding.ButtonItemBinding
import com.xwray.groupie.Item
import com.xwray.groupie.viewbinding.BindableItem

data class ButtonItem(
    val primaryText: CharSequence,
    val clickListener: () -> Unit = emptyListener
) : BindableItem<ButtonItemBinding>() {

    override fun isSameAs(other: Item<*>): Boolean = primaryText == (other as? ButtonItem)?.primaryText

    override fun hasSameContentAs(other: Item<*>): Boolean {
        return primaryText == (other as? ButtonItem)?.primaryText
    }

    override fun initializeViewBinding(view: View) = ButtonItemBinding.bind(view)

    override fun getLayout() = R.layout.button_item

    override fun bind(viewBinding: ButtonItemBinding, position: Int) {
        viewBinding.primaryText.text = primaryText

        viewBinding.primaryText.setOnClickListener {
            clickListener()
        }

        viewBinding.primaryText.isEnabled = clickListener != emptyListener
    }

    companion object {
        private val emptyListener: () -> Unit = {}
    }
}
