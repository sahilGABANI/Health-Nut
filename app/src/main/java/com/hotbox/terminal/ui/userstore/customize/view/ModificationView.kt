package com.hotbox.terminal.ui.userstore.customize.view

import android.content.Context
import android.view.View
import androidx.core.view.isVisible
import com.hotbox.terminal.R
import com.hotbox.terminal.api.menu.model.ModificationItem
import com.hotbox.terminal.api.order.model.OptionsItem
import com.hotbox.terminal.base.ConstraintLayoutWithLifecycle
import com.hotbox.terminal.base.extension.subscribeAndObserveOnMainThread
import com.hotbox.terminal.base.extension.throttleClicks
import com.hotbox.terminal.databinding.ViewModificationItemBinding
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class ModificationView(context: Context) : ConstraintLayoutWithLifecycle(context) {

    private val modificationStateSubject: PublishSubject<ModificationItem> = PublishSubject.create()
    val modificationActionState: Observable<ModificationItem> = modificationStateSubject.hide()
    private var binding: ViewModificationItemBinding? = null

    init {
        initUi()
    }

    private fun initUi() {
        val view = View.inflate(context, R.layout.view_modification_item, this)
        layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        binding = ViewModificationItemBinding.bind(view)
    }

    fun bind(modificationItem: ModificationItem) {
        binding?.apply {
            viewGreensOption.isVisible = modificationItem.isSelected == true
            tvGreensOption.text = modificationItem.modificationText

            viewVertical.isVisible = modificationItem.isLast == false
            tvGreensOption.isSelected = modificationItem.isRequired == true

            tvGreensOption.throttleClicks().subscribeAndObserveOnMainThread {
                modificationStateSubject.onNext(modificationItem)
            }.autoDispose()
        }
    }
}