package com.hotbox.terminal.ui.main.timemanagement.view

import android.content.Context
import android.view.View
import com.hotbox.terminal.R
import com.hotbox.terminal.api.clockinout.model.TimeResponse
import com.hotbox.terminal.base.ConstraintLayoutWithLifecycle
import com.hotbox.terminal.databinding.ClockinOutDetailsItemBinding

class ClockInOutDetailItemView(context: Context) : ConstraintLayoutWithLifecycle(context) {

    private lateinit var binding: ClockinOutDetailsItemBinding

    init {
        inflateUi()
    }

    private fun inflateUi() {
        val view = View.inflate(context, R.layout.clockin_out_details_item, this)
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        binding = ClockinOutDetailsItemBinding.bind(view)
    }

    fun bind(clockInOutDetailsInfo: TimeResponse) {
        val date = clockInOutDetailsInfo.getActionFormattedTime("MM/dd/yyyy")
        val timeDate = clockInOutDetailsInfo.getActionFormattedTime()
        binding.actionTextView.text = clockInOutDetailsInfo.getClockType().displayType
        binding.dateTextView.text = date
        binding.timeTextView.text = timeDate
    }
}