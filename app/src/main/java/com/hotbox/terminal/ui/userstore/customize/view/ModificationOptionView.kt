package com.hotbox.terminal.ui.userstore.customize.view

import android.content.Context
import android.view.View
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.hotbox.terminal.R
import com.hotbox.terminal.api.menu.model.ModificationItem
import com.hotbox.terminal.api.userstore.model.OptionsItemRequest
import com.hotbox.terminal.base.ConstraintLayoutWithLifecycle
import com.hotbox.terminal.base.extension.subscribeAndObserveOnMainThread
import com.hotbox.terminal.base.extension.throttleClicks
import com.hotbox.terminal.base.extension.toDollar
import com.hotbox.terminal.databinding.ViewModifiersItemBinding
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class ModificationOptionView(context: Context) : ConstraintLayoutWithLifecycle(context) {

    private val modificationStateSubject: PublishSubject<ModificationItem> = PublishSubject.create()
    val modificationActionState: Observable<ModificationItem> = modificationStateSubject.hide()

    private val selectedOptionStateSubject: PublishSubject<OptionsItemRequest> = PublishSubject.create()
    val selectedOptionActionState: Observable<OptionsItemRequest> = selectedOptionStateSubject.hide()
    private var binding: ViewModifiersItemBinding? = null
    private var modifiersTotal : Double = 0.00
    private lateinit var selectedOptionAdapter: SelectedOptionAdapter

    init {
        initUi()
    }

    private fun initUi() {
        val view = View.inflate(context, R.layout.view_modifiers_item, this)
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        binding = ViewModifiersItemBinding.bind(view)
    }

    fun bind(modificationItem: ModificationItem) {
        modifiersTotal = 0.00
        binding?.apply {
            clModifiers.isVisible = !modificationItem.selectedOptionsItem.isNullOrEmpty()
            modificationTextView.text = modificationItem.modificationText
            modificationItem.selectedOptionsItem?.forEach {
                modifiersTotal = it.optionPrice?.let { it1 -> modifiersTotal.plus(it1) }!!
            }
            if (modifiersTotal == 0.00) optionPriceTextView.visibility = View.GONE
            else optionPriceTextView.visibility = View.VISIBLE
            optionPriceTextView.text = "+".plus(modifiersTotal.div(100).toDollar())

            clModifiers.throttleClicks().subscribeAndObserveOnMainThread {
                modificationStateSubject.onNext(modificationItem)
            }.autoDispose()
            selectedOptionAdapter = SelectedOptionAdapter(context).apply {
                selectedOptionActionState.subscribeAndObserveOnMainThread {
                    selectedOptionStateSubject.onNext(it)
                }

            }
            rvSelectedOption.layoutManager = LinearLayoutManager(context, RecyclerView.HORIZONTAL, false)
            selectedOptionAdapter.listOfOrderSubItem = modificationItem.selectedOptionsItem
            rvSelectedOption.apply {
                adapter = selectedOptionAdapter
            }
        }
    }
}