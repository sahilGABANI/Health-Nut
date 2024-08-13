package com.hotbox.terminal.ui.userstore.cookies.view

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.hotbox.terminal.R
import com.hotbox.terminal.api.authentication.LoggedInUserCache
import com.hotbox.terminal.api.menu.model.ProductsItem
import com.hotbox.terminal.application.HotBoxApplication
import com.hotbox.terminal.base.ConstraintLayoutWithLifecycle
import com.hotbox.terminal.base.extension.subscribeAndObserveOnMainThread
import com.hotbox.terminal.base.extension.throttleClicks
import com.hotbox.terminal.base.extension.toDollar
import com.hotbox.terminal.databinding.ViewUserStoreItemBinding
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import timber.log.Timber
import javax.inject.Inject

class UserStoreProductView(context: Context) : ConstraintLayoutWithLifecycle(context) {
    private val userStoreProductStateSubject: PublishSubject<ProductsItem> = PublishSubject.create()
    val userStoreProductActionState: Observable<ProductsItem> = userStoreProductStateSubject.hide()

    private lateinit var binding: ViewUserStoreItemBinding
    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache

    init {
        inflateUi()
    }

    private fun inflateUi() {
        val view = View.inflate(context, R.layout.view_user_store_item, this)
        HotBoxApplication.component.inject(this)
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        binding = ViewUserStoreItemBinding.bind(view)
    }

    @SuppressLint("SetTextI18n")
    fun bind(productInfo: ProductsItem) {
        binding.apply {
            if(loggedInUserCache.getIsEmployeeMeal() == true) {
                binding.clCookies.setBackgroundColor(ContextCompat.getColor(context, R.color.green_light_50));
            }
            if (productInfo.isRedeemProduct == true) {
                if (productInfo.productLoyaltyTier?.tierValue != 0 && productInfo.productLoyaltyTier?.tierValue != null ) {
                    orderItemPrize.text = productInfo.productLoyaltyTier?.tierValue.toString().plus(" ").plus(resources.getString(R.string.leaves))
                }
            } else {
                orderItemPrize.text = productInfo.productBasePrice?.div(100).toDollar()
            }
            productNameTextView.text = productInfo.productName
            Glide.with(context).load(productInfo.productImage).placeholder(R.drawable.ic_launcher_logo).error(R.drawable.ic_launcher_logo)
                .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC).into(productImageView)
            if (productInfo.productCals != null && productInfo.productCals != 0 ){
                productSizeAndWeightTextView.text = productInfo.productCals.toString().plus(" cal")
            } else {
                productSizeAndWeightTextView.text = "0".plus(" cal")
            }
            if (productInfo.productTags != null) {
                if (productInfo.productTags.active == true) {
                    popularBackgroundCardView.isVisible = true
                    popularTextView.text = productInfo.productTags.tagName.toString()
                }
            } else {
                popularBackgroundCardView.isVisible = false
            }

            productDetailsLinearLayout.throttleClicks().subscribeAndObserveOnMainThread {
                userStoreProductStateSubject.onNext(productInfo)
            }.autoDispose()
        }
    }
}