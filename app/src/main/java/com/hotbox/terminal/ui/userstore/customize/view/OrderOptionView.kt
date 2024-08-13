package com.hotbox.terminal.ui.userstore.customize.view

import android.content.Context
import android.view.View
import androidx.core.view.isVisible
import com.hotbox.terminal.R
import com.hotbox.terminal.api.order.model.OptionsItem
import com.hotbox.terminal.base.ConstraintLayoutWithLifecycle
import com.hotbox.terminal.base.extension.subscribeAndObserveOnMainThread
import com.hotbox.terminal.base.extension.throttleClicks
import com.hotbox.terminal.base.extension.toDollar
import com.hotbox.terminal.databinding.ViewOptionItemBinding
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class OrderOptionView(context: Context) : ConstraintLayoutWithLifecycle(context) {

    private val optionStateSubject: PublishSubject<OptionsItem> = PublishSubject.create()
    val optionActionState: Observable<OptionsItem> = optionStateSubject.hide()

    private var binding: ViewOptionItemBinding? = null

    init {
        initUi()
    }

    private fun initUi() {
        val view = View.inflate(context, R.layout.view_option_item, this)
        layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        binding = ViewOptionItemBinding.bind(view)
    }

    fun bind(optionsItem: OptionsItem) {
        binding?.apply {
            optionNameTextView.text = optionsItem.optionName
            if (optionsItem.optionPrice != 0.0) {
                optionPriceTextView.isVisible = true
                optionPriceTextView.text = optionsItem.optionPrice?.div(100).toDollar()
            } else {
                optionPriceTextView.isVisible = false
            }
            llOption.isSelected = optionsItem.isCheck == true
            checkImageView.isVisible = optionsItem.isCheck == true
            if (optionsItem.isCheck == true) {
                optionNameTextView.setTextColor(resources.getColor(R.color.white))
                optionPriceTextView.setTextColor(resources.getColor(R.color.white))
            } else {
                optionNameTextView.setTextColor(resources.getColor(R.color.green_light))
                optionPriceTextView.setTextColor(resources.getColor(R.color.orange))
            }
            llOption.throttleClicks().subscribeAndObserveOnMainThread {
                optionStateSubject.onNext(optionsItem)
            }.autoDispose()
        }
    }
}