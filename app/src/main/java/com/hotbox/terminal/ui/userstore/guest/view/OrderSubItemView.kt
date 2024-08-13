package com.hotbox.terminal.ui.userstore.guest.view

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.hotbox.terminal.R
import com.hotbox.terminal.api.order.model.OptionsItem
import com.hotbox.terminal.api.userstore.model.SubOrderItemData
import com.hotbox.terminal.base.ConstraintLayoutWithLifecycle
import com.hotbox.terminal.base.extension.showToast
import com.hotbox.terminal.base.extension.subscribeAndObserveOnMainThread
import com.hotbox.terminal.base.extension.throttleClicks
import com.hotbox.terminal.databinding.OrderSubitemLayoutBinding
import com.hotbox.terminal.utils.doOnCollapse
import com.hotbox.terminal.utils.doOnExpand
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class OrderSubItemView(context: Context) : ConstraintLayoutWithLifecycle(context) {

    private var binding: OrderSubitemLayoutBinding? = null
    private lateinit var subProductAdapter: SubProductAdapter

    private val subProductStateSubject: PublishSubject<SubOrderItemData> = PublishSubject.create()
    val subProductActionState: Observable<SubOrderItemData> = subProductStateSubject.hide()

    init {
        initUi()
    }

    private fun initUi() {
        val view = View.inflate(context, R.layout.order_subitem_layout, this)
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        binding = OrderSubitemLayoutBinding.bind(view)
    }

    @SuppressLint("SetTextI18n")
    fun bind(subOrderItemData: SubOrderItemData, position: Int) {
        binding?.apply {
            subProductNameAppCompatTextView.text = subOrderItemData.modifiers?.modificationText
            subProductNumberAppCompatTextView.text = (position + 1).toString()
            subOrderItemData.optionImage?.let {
                subProductNameAppCompatTextView.isSelected = true
                subProductNumberAppCompatTextView.isSelected = true
                productImageView.isVisible = true
                Glide.with(context).load(it)
                    .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC).into(productImageView)
            }

            dropDownMaterialCardView.throttleClicks().subscribeAndObserveOnMainThread {
                if (expandable.isExpanded) {
                    downArrowImageView.isSelected = false
                    expandable.collapse()
                } else {
                    downArrowImageView.isSelected = true
                    expandable.expand()
                }
            }.autoDispose()
            header.throttleClicks().subscribeAndObserveOnMainThread {
                if (expandable.isExpanded) {
                    downArrowImageView.isSelected = false
                    expandable.collapse()
                } else {
                    downArrowImageView.isSelected = true
                    expandable.expand()
                }
            }.autoDispose()
            if (subOrderItemData.isLastItem) lastView.isVisible = true

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

                    Glide.with(context).load(subOrderItemData.optionImage).placeholder(R.drawable.ic_launcher_logo).error(R.drawable.ic_launcher_logo)
                        .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC).into(productImageView)
                    if (subOrderItemData.modifiers?.selectMax == 1) {
                        val listofOption = subProductAdapter.listOfSubProduct
                        listofOption?.filter { it.isCheck == true }?.forEach {
                            it.isCheck = false
                            if(subOrderItemData.optionsItem.contains(item)) subOrderItemData.optionsItem.remove(item)
                        }
                        listofOption?.find { it.id == item.id }?.apply {
                            isCheck = true
                            subOrderItemData.optionsItem.add(item)
                        }
                        subProductAdapter.listOfSubProduct = listofOption
                    } else {
                        val listOfOption = subProductAdapter.listOfSubProduct
                        if (item.isCheck == true) {
                            listOfOption?.find { it.id == item.id }?.apply {
                                isCheck = false
                                if(subOrderItemData.optionsItem.contains(item)) subOrderItemData.optionsItem.remove(item)
                            }
                        } else {
                            if (subOrderItemData.optionsItem.isEmpty()) {
                                if (subOrderItemData.optionsItem.size < subOrderItemData.modifiers?.selectMax!!) {
                                    listOfOption?.find { it.id == item.id }?.apply {
                                        isCheck = !isCheck!!
                                        if (isCheck == true) {
                                            subOrderItemData.optionsItem.add(item)
                                        } else {
                                            if(subOrderItemData.optionsItem.contains(item)) subOrderItemData.optionsItem.remove(item)
                                        }
                                    }
                                } else {
                                    context.showToast("you have select maximum option")
                                }
                            } else {
                                if (subOrderItemData.optionsItem.size < subOrderItemData.modifiers?.selectMax!!) {
                                    listOfOption?.find { it.id == item.id }?.apply {
                                        isCheck = !isCheck!!
                                        if (isCheck == true) {
                                            subOrderItemData.optionsItem.add(item)
                                        } else {
                                            if(subOrderItemData.optionsItem.contains(item)) subOrderItemData.optionsItem.remove(item)
                                        }
                                    }
                                } else {
                                    context.showToast("you have select maximum option")
                                }
                            }

                        }
                        subProductAdapter.listOfSubProduct = listOfOption
                    }

                    subProductStateSubject.onNext(subOrderItemData)
                }

            }
            rvSubItem.layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
            rvSubItem.adapter = subProductAdapter
            subOrderItemData.subProductList?.forEach {
                if (it.optionRecommendation == 1) {
                    it.isCheck = true
                    subOrderItemData.optionsItem.add(it)
                }
            }
            subProductAdapter.listOfSubProduct = subOrderItemData.subProductList

            expandable.expand()
        }
    }
}