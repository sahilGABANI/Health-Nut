package com.hotbox.terminal.ui.userstore.view

import android.annotation.SuppressLint
import android.content.Context
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.view.View
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.hotbox.terminal.R
import com.hotbox.terminal.api.authentication.LoggedInUserCache
import com.hotbox.terminal.api.userstore.model.CartItem
import com.hotbox.terminal.api.userstore.model.CartItemClickStates
import com.hotbox.terminal.application.HotBoxApplication
import com.hotbox.terminal.base.ConstraintLayoutWithLifecycle
import com.hotbox.terminal.base.extension.showToast
import com.hotbox.terminal.base.extension.subscribeAndObserveOnMainThread
import com.hotbox.terminal.base.extension.throttleClicks
import com.hotbox.terminal.base.extension.toDollar
import com.hotbox.terminal.databinding.ViewCartItemBinding
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import javax.inject.Inject

class CartItemView(context: Context) : ConstraintLayoutWithLifecycle(context) {

    private var binding: ViewCartItemBinding? = null
    private lateinit var subItemAdapter: SubItemAdapter
    private var productQuantity = 1
    private var productPrize: Double = 0.00

    private val userStoreCartStateSubject: PublishSubject<CartItemClickStates> = PublishSubject.create()
    val userStoreCartActionState: Observable<CartItemClickStates> = userStoreCartStateSubject.hide()

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache

    init {
        initUi()
    }

    private fun initUi() {
        val view = View.inflate(context, R.layout.view_cart_item, this)
        HotBoxApplication.component.inject(this)
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        binding = ViewCartItemBinding.bind(view)
    }

    @SuppressLint("CutPasteId", "SetTextI18n")
    fun bind(cartItem: CartItem) {
        binding?.apply {
            productNameTextView.text = cartItem.menu?.product?.productName
            productPrize = 0.00
            productPrize = productPrize.plus(cartItem.menuItemPrice!!)
            cartItem.menuItemQuantity?.let { productQuantity = it }
            cartItem.menuItemModifiers?.forEach {
                it.options?.forEach {
                    if (it.optionPrice != 0.00 && it.optionPrice != null) {
                        productPrize = productPrize.plus(it.optionPrice)
                    }
                }
            }
            val productQuantity = cartItem.menuItemQuantity
            tvProductQuantity.text = productQuantity.toString()
            tvProductQuantityLast.text = productQuantity.toString()
            if (cartItem.menuItemComp == 1 && cartItem.menuItemRedemption == 1) {
                tvProductPrize.text = "$0.00"
                tvProductPrizeLast.text = "$0.00"
            } else {
                tvProductPrize.text = cartItem.menuItemPrice?.div(100).toDollar()
                tvProductPrizeLast.text = cartItem.menuItemPrice?.div(100).toDollar()
            }
            Glide.with(context).load(cartItem.menu?.product?.productImage).placeholder(R.drawable.ic_launcher_logo).error(R.drawable.ic_launcher_logo)
                .into(productImageView)
            subItemAdapter = SubItemAdapter(context)
            cartItem.menuItemInstructions?.let {
                specialTextRelativeLayout.isVisible = true
                orderSpecialInstructionsAppCompatTextView.text = it
            }
            if (cartItem.menuItemModifiers != null) {
                llEdit.isVisible = true
                tvModifiers.removeAllViews()
                cartItem.menuItemModifiers.forEach { item ->
                    item.modificationText?.let {
                        tvModifiers.isVisible = true
                        val v: View = View.inflate(context, R.layout.modification_text_view, null)
                        v.findViewById<AppCompatTextView>(R.id.productTextview).text = "$it"
                        v.findViewById<AppCompatTextView>(R.id.productTextDescription).isVisible = false
                        tvModifiers.addView(v)
                    }
                    item.options?.forEach {
                        if (it.optionPrice != 0.0 && it.optionPrice != null) {
                            val v: View = View.inflate(context, R.layout.modification_text_view, null)
                            v.findViewById<AppCompatTextView>(R.id.productTextview).isVisible = false
                            v.findViewById<AppCompatTextView>(R.id.productPriceTextView).isVisible = true
                            v.findViewById<AppCompatTextView>(R.id.productTextDescription).text =
                                "- ${it.optionName}"
                            v.findViewById<AppCompatTextView>(R.id.productPriceTextView).text = "(${it.optionPrice.div(100).toDollar()})"
                            tvModifiers.addView(v)
                        } else {
                            val v: View = View.inflate(context, R.layout.modification_text_view, null)
                            v.findViewById<AppCompatTextView>(R.id.productTextview).isVisible = false
                            v.findViewById<AppCompatTextView>(R.id.productPriceTextView).isVisible = false
                            v.findViewById<AppCompatTextView>(R.id.productTextDescription).text = "- ${it.optionName}"
                            tvModifiers.addView(v)
                        }
                    }
                    tvModifiers.visibility = View.VISIBLE
                }
                tvModifiers.visibility = View.VISIBLE
            } else {
                llEdit.isVisible = false
                tvModifiers.visibility = View.GONE
            }
            if (!cartItem.menuItemCompReason.isNullOrEmpty()) {
                compReasonRelativeLayout.isVisible = true
                compReasonAppCompatTextView.text = cartItem.menuItemCompReason
            }
            if (cartItem.menuItemComp == 1) {
                llAddition.visibility = View.GONE
                llSubtraction.visibility = View.GONE
                tvBack.isVisible = true
                tvComp.isVisible = true
                redeemButton.isVisible = false
                tvRedeemProduct.isVisible = false
                ivRemoveRedeemProduct.isVisible = false
                tvProductPrize.text = "$0.00"
            } else {
                tvBack.isVisible = false
                tvComp.isVisible = false
                if (cartItem.menu?.product?.productLoyaltyTier != null && !loggedInUserCache.getLoyaltyQrResponse()?.fullName.isNullOrEmpty() && cartItem.menuItemRedemption != 1) {
//                    redeemButton.isVisible = true
                    tvProductQuantity.isVisible = true
                    tvMultiply.isVisible = true
                    tvRedeemProduct.isVisible = false
                    ivRemoveRedeemProduct.isVisible = false
                    rlOption.isVisible = true
                    llAddition.isVisible = true
                    llSubtraction.isVisible = true
                    redeemButton.text =
                        resources.getString(R.string.redeemed).plus(" ").plus("${cartItem.menu.product.productLoyaltyTier.tierValue} Leaves")
                }
            }
            if (cartItem.menuItemRedemption == 1) {
                tvProductPrize.text = "$0.00"
                redeemButton.isVisible = true
                llAddition.visibility = View.GONE
                llSubtraction.visibility = View.GONE
                tvProductQuantity.isVisible = false
                tvMultiply.isVisible = true
                tvRedeemProduct.isVisible = false
                ivRemoveRedeemProduct.isVisible = true
                redeemButton.text = "Redeemed ${cartItem.menu?.product?.productLoyaltyTier?.tierValue} Leaves"
            } else {
                redeemButton.isVisible = false
            }
            if (cartItem.isChanging == false) {
                llAddition.visibility = View.GONE
                llSubtraction.visibility = View.GONE
                llDelete.visibility = View.GONE
                llEdit.visibility = View.GONE
                ivRemoveRedeemProduct.visibility = View.GONE
                tvProductPrize.visibility = View.GONE
                tvBack.visibility = View.GONE
                tvMultiply.visibility = View.GONE
                tvProductQuantity.visibility = View.GONE
                tvProductPrizeLast.visibility = View.VISIBLE
                tvProductQuantityLast.visibility = View.VISIBLE
                tvMultiplyLast.visibility = View.VISIBLE
                productCalTextView.visibility = View.VISIBLE
            } else {
                tvProductQuantity.visibility = View.VISIBLE
                tvProductQuantityLast.visibility = View.GONE
                tvProductPrizeLast.visibility = View.GONE
                tvMultiplyLast.visibility = View.GONE
                tvMultiply.visibility = View.VISIBLE
                productCalTextView.visibility = View.GONE
                llDelete.visibility = View.VISIBLE
                llEdit.isVisible = cartItem.menuItemModifiers != null
                tvProductPrize.visibility = View.VISIBLE
                if (cartItem.menuItemRedemption == 1) {
                    redeemButton.visibility = View.VISIBLE
                    ivRemoveRedeemProduct.visibility = View.VISIBLE
                }
                if (cartItem.menuItemComp == 1) {
                    tvBack.isVisible = true
                    tvComp.isVisible = true
                }
            }
            if (!cartItem.compReason?.displayType.isNullOrEmpty()) {
                selectReason.text = cartItem.compReason?.displayType.toString()
            }
            if (loggedInUserCache.isUserLoggedIn() && cartItem.menuItemComp == 0 && cartItem.isVisibleComp == true && cartItem.menuItemRedemption == 0) {
                tvSelectReasonForCompProduct.isVisible = true
                confirmMaterialButton.isVisible = true
            } else {
                tvSelectReasonForCompProduct.isVisible = false
                confirmMaterialButton.isVisible = false
            }
            if (loggedInUserCache.getIsEmployeeMeal() == true) {
                llAddition.isVisible = false
                llSubtraction.isVisible = false
            }
            productCalTextView.text = cartItem.menu?.product?.productCals.toString().plus(" cal")
            llAddition.throttleClicks().subscribeAndObserveOnMainThread {
                userStoreCartStateSubject.onNext(CartItemClickStates.CartItemAdditionClick(cartItem))
            }.autoDispose()
            llSubtraction.throttleClicks().subscribeAndObserveOnMainThread {
                userStoreCartStateSubject.onNext(CartItemClickStates.CartItemSubscriptionClick(cartItem))
            }.autoDispose()
            llDelete.throttleClicks().subscribeAndObserveOnMainThread {
                userStoreCartStateSubject.onNext(CartItemClickStates.CartItemDeleteClick(cartItem))
            }.autoDispose()
            llEdit.throttleClicks().subscribeAndObserveOnMainThread {
                userStoreCartStateSubject.onNext(CartItemClickStates.CartItemEditClick(cartItem))
            }.autoDispose()
            clCartItem.throttleClicks().subscribeAndObserveOnMainThread {
                if (cartItem.isChanging == true) {
                    userStoreCartStateSubject.onNext(CartItemClickStates.CartItemEditClick(cartItem))
                }
            }.autoDispose()
            tvSelectReasonForCompProduct.throttleClicks().subscribeAndObserveOnMainThread {
                userStoreCartStateSubject.onNext(CartItemClickStates.CartItemCompProductReasonClick(cartItem))
            }.autoDispose()
            confirmMaterialButton.throttleClicks().subscribeAndObserveOnMainThread {
                userStoreCartStateSubject.onNext(CartItemClickStates.CartItemConfirmButtonClick(cartItem))
            }.autoDispose()
            redeemButton.throttleClicks().subscribeAndObserveOnMainThread {
                if ((cartItem.menu?.product?.productLoyaltyTier?.tierValue ?: 0) <= (loggedInUserCache.getLoyaltyQrResponse()?.points ?: 0)) {
                    userStoreCartStateSubject.onNext(CartItemClickStates.RedeemProductClick(cartItem))
                } else {
                    context.showToast("You don't have enough leaves to redeem")
                }
            }.autoDispose()
            ivRemoveRedeemProduct.throttleClicks().subscribeAndObserveOnMainThread {
                ivRemoveRedeemProduct.isVisible = false
                userStoreCartStateSubject.onNext(CartItemClickStates.RedeemProductClick(cartItem))
            }.autoDispose()
        }
    }
}