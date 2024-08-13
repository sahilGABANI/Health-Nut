package com.hotbox.terminal.ui.userstore.guest.view

import android.content.Context
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.hotbox.terminal.R
import com.hotbox.terminal.api.menu.model.ModificationItem
import com.hotbox.terminal.api.userstore.model.ModifiersItem
import com.hotbox.terminal.api.userstore.model.OptionsItemRequest
import com.hotbox.terminal.base.ConstraintLayoutWithLifecycle
import com.hotbox.terminal.base.extension.subscribeAndObserveOnMainThread
import com.hotbox.terminal.base.extension.throttleClicks
import com.hotbox.terminal.databinding.OrderSubitemLayoutBinding
import com.hotbox.terminal.utils.doOnCollapse
import com.hotbox.terminal.utils.doOnExpand

class SubProductItemView(context: Context) : ConstraintLayoutWithLifecycle(context) {

    private var binding: OrderSubitemLayoutBinding? = null
    private lateinit var subProductAdapter: SubProductAdapter


    init {
        initUi()
    }

    private fun initUi() {
        val view = View.inflate(context, R.layout.order_subitem_layout, this)
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        binding = OrderSubitemLayoutBinding.bind(view)
    }

    fun bind(subOrderItemData: ModificationItem) {
        binding?.apply {
            subProductNumberAppCompatTextView.visibility = View.GONE
            subProductNameAppCompatTextView.text = subOrderItemData.modificationText.toString()
            dropDownMaterialCardView.throttleClicks().subscribeAndObserveOnMainThread {
                if (expandable.isExpanded) {
                    downArrowImageView.isSelected = false
                    expandable.collapse()
                } else {
                    downArrowImageView.isSelected = true
                    expandable.expand()
                }
            }.autoDispose()

            expandable.doOnCollapse {
                subProductNameAppCompatTextView.isSelected = true
                subProductNumberAppCompatTextView.isSelected = true
                downArrowImageView.isSelected = false
            }
            expandable.doOnExpand {
                downArrowImageView.isSelected = true
            }
            subProductAdapter = SubProductAdapter(context).apply {
                subProductActionState.subscribeAndObserveOnMainThread { item ->
                    val listofOption = subProductAdapter.listOfSubProduct
                    listofOption?.filter { it.isCheck == true }?.forEach {
                        it.isCheck = false
                    }
                    listofOption?.find { it.id == item.id }?.apply {
                        subOrderItemData.selectedOption = 1
                        isCheck = true
                    }
                    subProductAdapter.listOfSubProduct = listofOption
                }.autoDispose()
            }
            rvSubItem.layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
            rvSubItem.adapter = subProductAdapter
            subProductAdapter.listOfSubProduct = subOrderItemData.options
        }

    }
}