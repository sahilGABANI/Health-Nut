package com.hotbox.terminal.ui.userstore.customize.view

import android.content.Context
import android.view.View
import androidx.core.view.isVisible
import com.hotbox.terminal.R
import com.hotbox.terminal.api.order.model.OptionsItem
import com.hotbox.terminal.api.userstore.model.OptionsItemRequest
import com.hotbox.terminal.base.ConstraintLayoutWithLifecycle
import com.hotbox.terminal.base.extension.subscribeAndObserveOnMainThread
import com.hotbox.terminal.base.extension.throttleClicks
import com.hotbox.terminal.base.extension.toDollar
import com.hotbox.terminal.databinding.ViewOptionItemBinding
import com.hotbox.terminal.databinding.ViewSelectedOptionItemBinding
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class SelectedOptionView(context: Context) : ConstraintLayoutWithLifecycle(context) {

    private val selectedOptionStateSubject: PublishSubject<OptionsItemRequest> = PublishSubject.create()
    val selectedOptionActionState: Observable<OptionsItemRequest> = selectedOptionStateSubject.hide()

    private var binding: ViewSelectedOptionItemBinding? = null

    init {
        initUi()
    }

    private fun initUi() {
        val view = View.inflate(context, R.layout.view_selected_option_item, this)
        layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        binding = ViewSelectedOptionItemBinding.bind(view)
    }

    fun bind(optionsItem: OptionsItemRequest) {
        binding?.apply {
            optionNameTextView.text = optionsItem.optionName

            llOption.throttleClicks().subscribeAndObserveOnMainThread {
                selectedOptionStateSubject.onNext(optionsItem)
            }.autoDispose()
        }
    }
}