package com.hotbox.terminal.ui.main.menu.view

import android.content.Context
import android.view.View
import com.hotbox.terminal.R
import com.hotbox.terminal.api.menu.model.MenuSectionInfo
import com.hotbox.terminal.base.ConstraintLayoutWithLifecycle
import com.hotbox.terminal.databinding.HeaderMenuItemBinding

class MenuHeaderItemView(context: Context) : ConstraintLayoutWithLifecycle(context) {

    private var binding: HeaderMenuItemBinding? = null

    init {
        inflateUi()
    }

    private fun inflateUi() {
        val view = View.inflate(context, R.layout.header_menu_item, this)
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        binding = HeaderMenuItemBinding.bind(view)
    }

    fun bind(sectionInfo: MenuSectionInfo) {
        binding?.apply {
            productTextView.text = sectionInfo.productName
            descriptionTextView.text = sectionInfo.productDescription
            priceTextView.text = sectionInfo.productPrice
            stateTextView.text = sectionInfo.productState

        }
    }

    override fun onDestroy() {
        binding = null
        super.onDestroy()
    }
}