package com.hotbox.terminal.ui.userstore.view

import android.content.Context
import android.view.View
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.hotbox.terminal.R
import com.hotbox.terminal.api.order.model.OptionsItem
import com.hotbox.terminal.base.ConstraintLayoutWithLifecycle
import com.hotbox.terminal.databinding.ViewCartSubItemBinding

class SubItemView(context: Context) : ConstraintLayoutWithLifecycle(context) {

    private var binding: ViewCartSubItemBinding? = null

    init {
        initUi()
    }

    private fun initUi() {
        val view = View.inflate(context, R.layout.view_cart_sub_item, this)
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        binding = ViewCartSubItemBinding.bind(view)
    }

    fun bind(subProduct: OptionsItem, position: Int) {
        binding?.apply {
            itemNumberTextView.text = position.toString()
            itemNameTextView.text = subProduct.optionName
            if (subProduct.optionImage != null) {
                subProductImageView.isVisible = true
                Glide.with(context).load(subProduct.optionImage).into(subProductImageView)
            }

        }
    }
}