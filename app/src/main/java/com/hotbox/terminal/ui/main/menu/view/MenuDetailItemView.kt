package com.hotbox.terminal.ui.main.menu.view

import android.annotation.SuppressLint
import android.content.Context
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.hotbox.terminal.R
import com.hotbox.terminal.api.authentication.LoggedInUserCache
import com.hotbox.terminal.api.menu.model.ProductsItem
import com.hotbox.terminal.application.HotBoxApplication
import com.hotbox.terminal.base.ConstraintLayoutWithLifecycle
import com.hotbox.terminal.base.extension.showToast
import com.hotbox.terminal.base.extension.subscribeAndObserveOnMainThread
import com.hotbox.terminal.base.extension.throttleClicks
import com.hotbox.terminal.base.extension.toDollar
import com.hotbox.terminal.databinding.MenuDetailItemBinding
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import javax.inject.Inject


class MenuDetailItemView(context: Context) : ConstraintLayoutWithLifecycle(context) {

    private var binding: MenuDetailItemBinding? = null

    private val menuStateSubject: PublishSubject<ProductsItem> = PublishSubject.create()
    val menuActionState: Observable<ProductsItem> = menuStateSubject.hide()

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache

    init {
        inflateUi()
    }

    private fun inflateUi() {
        val view = View.inflate(context, R.layout.menu_detail_item, this)
        HotBoxApplication.component.inject(this)
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        binding = MenuDetailItemBinding.bind(view)
    }

    @SuppressLint("ClickableViewAccessibility")
    fun bind(productsItem: ProductsItem) {
        binding?.apply {
            Glide.with(context).load(productsItem.productImage).placeholder(R.drawable.ic_launcher_logo).error(R.drawable.ic_launcher_logo)
                .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC).into(productImageView)
            productNameTextView.text = productsItem.productName
            if (!productsItem.productDescription.isNullOrEmpty()) {
                descriptionTextView.text = productsItem.productDescription
            } else {
                descriptionTextView.text = resources.getString(R.string._text)
            }
            if (productsItem.productBasePrice != null) {
                priceTextView.text = productsItem.productBasePrice?.div(100).toDollar()
            } else {
                priceTextView.text = ""
            }
            stateSwitchCompat.setOnCheckedChangeListener { compoundButton, b ->
                if (!loggedInUserCache.isAdmin()) {
                    stateSwitchCompat.isChecked = productsItem.menuActive == true
                }
            }
            if (productsItem.menuActive == true) {
                productImageView.alpha = 1F
                descriptionTextView.setTextColor(resources.getColor(R.color.color_666666))
                productNameTextView.setTextColor(resources.getColor(R.color.black))
                priceTextView.setTextColor(resources.getColor(R.color.green_light))
                menuStateTextView.setTextColor(resources.getColor(R.color.black))
                productNameTextView.isSelected = true
                menuStateTextView.text = resources.getText(R.string.available)
                stateSwitchCompat.isChecked = true
                productsItem.menuActive = true
            } else {
                productImageView.alpha = 0.5F
                descriptionTextView.setTextColor(resources.getColor(R.color.md_grey))
                productNameTextView.setTextColor(resources.getColor(R.color.md_grey))
                priceTextView.setTextColor(resources.getColor(R.color.md_grey))
                menuStateTextView.setTextColor(resources.getColor(R.color.md_grey))
                menuStateTextView.text = resources.getText(R.string.off_unavailable)
                stateSwitchCompat.isChecked = false
                productsItem.menuActive = false
            }
            stateSwitchCompat.throttleClicks().subscribeAndObserveOnMainThread {
                if (!loggedInUserCache.isAdmin()) {
                    context.showToast("This feature requires elevated permissions")
                    stateSwitchCompat.isChecked = productsItem.menuActive == true
                } else {
                    menuStateSubject.onNext(productsItem)
                    if (stateSwitchCompat.isChecked) {
                        menuStateTextView.text = resources.getText(R.string.available)
                        productsItem.menuActive = true
                    } else {
                        productsItem.menuActive = false
                    }
                }

            }.autoDispose()
        }
    }

    override fun onDestroy() {
        binding = null
        super.onDestroy()
    }
}