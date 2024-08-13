package com.hotbox.terminal.ui.main.store.view

import android.content.Context
import android.view.View
import com.hotbox.terminal.R
import com.hotbox.terminal.api.store.model.StoreShiftTime
import com.hotbox.terminal.base.ConstraintLayoutWithLifecycle
import com.hotbox.terminal.databinding.ViewOrderingHoursBinding

class OrderingHoursView(context: Context) : ConstraintLayoutWithLifecycle(context) {

    private var binding: ViewOrderingHoursBinding? = null

    init {
        initUi()
    }

    private fun initUi() {
        val view = View.inflate(context, R.layout.view_ordering_hours, this)
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        binding = ViewOrderingHoursBinding.bind(view)

    }

    fun bind(storeShiftTime: StoreShiftTime) {
        binding?.apply {
            timeLimitAppCompatTextView.text = storeShiftTime.time
            dayAppCompatTextView.text = storeShiftTime.dayOfWeek
        }
    }

    override fun onDestroy() {
        binding = null
        super.onDestroy()
    }
}