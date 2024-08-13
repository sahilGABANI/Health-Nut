package com.hotbox.terminal.ui.main.order.view

import android.content.Context
import android.view.View
import com.hotbox.terminal.R
import com.hotbox.terminal.api.order.model.SectionInfo
import com.hotbox.terminal.base.ConstraintLayoutWithLifecycle
import com.hotbox.terminal.databinding.HeaderOrderItemBinding

class OrderHeaderItemView(context: Context) : ConstraintLayoutWithLifecycle(context) {

    private var binding: HeaderOrderItemBinding? = null

    init {
        inflateUi()
    }

    private fun inflateUi() {
        val view = View.inflate(context, R.layout.header_order_item, this)
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        binding = HeaderOrderItemBinding.bind(view)
    }

    fun bind(sectionInfo: SectionInfo) {
        binding?.apply {
            orderIdTextView.text = sectionInfo.orderId
            customerNameTextView.text = sectionInfo.guest
            productPrizeTextView.text = sectionInfo.total
            orderTypeTextView.text = sectionInfo.orderType
            statusTextView.text = sectionInfo.status
            promisedTimeAppCompatTextView.text = sectionInfo.promiseTime
            orderDateAndTimeTextView.text = sectionInfo.orderPlace
        }
    }

    override fun onDestroy() {
        binding = null
        super.onDestroy()
    }
}