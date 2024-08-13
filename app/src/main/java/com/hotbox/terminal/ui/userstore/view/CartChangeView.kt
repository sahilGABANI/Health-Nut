package com.hotbox.terminal.ui.userstore.view

import android.content.Context
import android.view.View
import com.hotbox.terminal.R
import com.hotbox.terminal.api.userstore.model.CartItem
import com.hotbox.terminal.base.ConstraintLayoutWithLifecycle

class CartChangeView(context: Context) : ConstraintLayoutWithLifecycle(context) {

    init {
        initUi()
    }

    private fun initUi() {
        val view = View.inflate(context, R.layout.view_cart_item, this)
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
    }

    fun bind(cartInfo: CartItem) {

    }
}