package com.hotbox.terminal.ui.main.orderdetail.view

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.hotbox.terminal.R
import com.hotbox.terminal.api.order.model.CartItem
import com.hotbox.terminal.api.order.model.OrderDetailItem
import com.hotbox.terminal.base.ConstraintLayoutWithLifecycle
import com.hotbox.terminal.base.extension.subscribeAndObserveOnMainThread
import com.hotbox.terminal.base.extension.throttleClicks
import com.hotbox.terminal.base.extension.toDollar
import com.hotbox.terminal.databinding.OrderItemLayoutBinding

class OrderDetailsView(context: Context) : ConstraintLayoutWithLifecycle(context) {

    private lateinit var binding: OrderItemLayoutBinding

    init {
        inflateUi()
    }

    private fun inflateUi() {
        val view = View.inflate(context, R.layout.order_item_layout, this)
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        binding = OrderItemLayoutBinding.bind(view)
    }

    @SuppressLint("SetTextI18n", "CutPasteId")
    fun bind(orderDetailsInfo: CartItem) {
        binding.apply {
            Glide.with(context).load(orderDetailsInfo.menu?.product?.productImage).placeholder(R.drawable.ic_launcher_logo)
                .error(R.drawable.ic_launcher_logo).diskCacheStrategy(DiskCacheStrategy.AUTOMATIC).into(productImageView)
            productQuantityTextview.text = "X ${orderDetailsInfo.menuItemQuantity.toString()}"
            if (orderDetailsInfo.menuItemRedemption == 1) {
                productPrizeTextView.text = orderDetailsInfo.menuItemPrice?.div(100).toDollar().plus(" (Redeem)")
            } else if (orderDetailsInfo.menuItemComp == 1) {
                productPrizeTextView.text = orderDetailsInfo.menuItemPrice?.div(100).toDollar().plus(" (Comp)")
            } else {
                productPrizeTextView.text = orderDetailsInfo.menuItemPrice?.div(100).toDollar()
            }
            productNameTextView.text = orderDetailsInfo.menu?.product?.productName
            if (orderDetailsInfo.menu?.product?.productCals != null && orderDetailsInfo.menu.product.productCals != 0) {
                productDescriptionTextView.text = orderDetailsInfo.menu.product.productCals.toString().plus(" cal")
            } else {
                productDescriptionTextView.text = "0".plus(" cal")
            }
            cardAndBowLinearLayout.removeAllViews()
            orderDetailsInfo.menuItemModifiers?.forEach { item ->
                item.modificationText?.let {
                    cardAndBowLinearLayout.isVisible = true
                    item.modificationText.let {
                        cardAndBowLinearLayout.isVisible = true
                        val v: View = View.inflate(context, R.layout.modification_text_view, null)
                        v.findViewById<AppCompatTextView>(R.id.productTextview).text = "$it"
                        v.findViewById<AppCompatTextView>(R.id.productTextDescription).isVisible = false
                        v.findViewById<AppCompatTextView>(R.id.productPriceTextView).isVisible = false
                        cardAndBowLinearLayout.addView(v)
                    }
                    item.options?.forEach {
                        if (it.optionPrice != 0.0 && it.optionPrice != null) {
                            val v: View = View.inflate(context, R.layout.modification_text_view, null)
                            v.findViewById<AppCompatTextView>(R.id.productTextview).isVisible = false
                            v.findViewById<AppCompatTextView>(R.id.productPriceTextView).isVisible = true
                            v.findViewById<AppCompatTextView>(R.id.productTextDescription).text =
                                "- ${it.optionName}"
                            v.findViewById<AppCompatTextView>(R.id.productPriceTextView).text = "(${it.optionPrice.div(100).toDollar()})"
                            cardAndBowLinearLayout.addView(v)
                        } else {
                            val v: View = View.inflate(context, R.layout.modification_text_view, null)
                            v.findViewById<AppCompatTextView>(R.id.productTextview).isVisible = false
                            v.findViewById<AppCompatTextView>(R.id.productPriceTextView).isVisible = false
                            v.findViewById<AppCompatTextView>(R.id.productTextDescription).text = "- ${it.optionName}"
                            cardAndBowLinearLayout.addView(v)
                        }
                    }
                    cardAndBowLinearLayout.visibility = View.VISIBLE


                }
            }
            if (orderDetailsInfo.menuItemInstructions != " ") {
                orderDetailsInfo.menuItemInstructions?.trim()?.let {
                    textSpecialLinear.isVisible = true
                    orderSpecialInstructionsAppCompatTextView.text = orderDetailsInfo.menuItemInstructions.trim()
                }
            }
        }
    }
}
