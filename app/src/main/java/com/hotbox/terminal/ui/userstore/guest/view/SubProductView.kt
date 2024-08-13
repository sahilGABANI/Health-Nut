package com.hotbox.terminal.ui.userstore.guest.view

import android.content.Context
import android.view.View
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.hotbox.terminal.R
import com.hotbox.terminal.api.order.model.OptionsItem
import com.hotbox.terminal.base.ConstraintLayoutWithLifecycle
import com.hotbox.terminal.base.extension.subscribeAndObserveOnMainThread
import com.hotbox.terminal.base.extension.throttleClicks
import com.hotbox.terminal.base.extension.toDollar
import com.hotbox.terminal.databinding.LayoutSubProductSelectedBinding
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class SubProductView(context: Context) : ConstraintLayoutWithLifecycle(context) {

    private val subProductStateSubject: PublishSubject<OptionsItem> = PublishSubject.create()
    val subProductActionState: Observable<OptionsItem> = subProductStateSubject.hide()

    private var binding: LayoutSubProductSelectedBinding? = null

    init {
        initUi()
    }

    private fun initUi() {
        val view = View.inflate(context, R.layout.layout_sub_product_selected, this)
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        binding = LayoutSubProductSelectedBinding.bind(view)
    }

    fun bind(subProductInfo: OptionsItem) {
        binding?.apply {
            itemNameTextView.text = subProductInfo.optionName
            itemSelectedRadioButton.isChecked = subProductInfo.isCheck == true
            if(subProductInfo.optionPrice != 0.0 && subProductInfo.optionPrice != null) {
                tvItemPrice.visibility = View.VISIBLE
                tvItemPrice.text = "(${subProductInfo.optionPrice.div(100).toDollar()})"
            }
            if (subProductInfo.optionImage != null) {
                subProductImageView.isVisible =  true
                Glide.with(context).load(subProductInfo.optionImage)
                    .placeholder(R.drawable.ic_launcher_logo)
                    .error(R.drawable.ic_launcher_logo)
                    .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                    .into(subProductImageView)
            }

            itemSelectedRadioButton.throttleClicks().subscribeAndObserveOnMainThread {
                subProductStateSubject.onNext(subProductInfo)
            }.autoDispose()
        }

    }
}