package com.hotbox.terminal.ui.userstore.view


import android.content.Context
import android.view.View
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.hotbox.terminal.R
import com.hotbox.terminal.api.menu.model.MenusItem
import com.hotbox.terminal.base.ConstraintLayoutWithLifecycle
import com.hotbox.terminal.base.extension.subscribeAndObserveOnMainThread
import com.hotbox.terminal.base.extension.throttleClicks
import com.hotbox.terminal.databinding.CategoryViewBinding
import com.hotbox.terminal.databinding.MenuViewBinding
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject


class CategoryItemView(context: Context) : ConstraintLayoutWithLifecycle(context) {

    private val userStoreCategoryStateSubject: PublishSubject<MenusItem> = PublishSubject.create()
    val userStoreCategoryActionState: Observable<MenusItem> = userStoreCategoryStateSubject.hide()

    private var binding: CategoryViewBinding? = null

    init {
        initUi()
    }

    private fun initUi() {
        val view = View.inflate(context, R.layout.category_view, this)
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        binding = CategoryViewBinding.bind(view)
    }

    fun bind(menusItem: MenusItem, position: Int) {
        binding?.apply {
            if(menusItem.isSelected) {
                menuTextView.isSelected = true
                selectionView.isVisible = true
            } else {
                menuTextView.isSelected = false
                selectionView.isVisible = false
            }

            menuTextView.text = menusItem.categoryName?.toUpperCase()
            menuTextView.typeface = ResourcesCompat.getFont(context, R.font.gotham_medium)
            if (menusItem.products?.size != 0) {
                val size =  menusItem.products?.size ?: 0
                val randomNumber = (0 until size).random()
                val productImage = menusItem.products?.get(randomNumber)?.productImage
                Glide.with(context).load(productImage).placeholder(R.drawable.ic_launcher_logo).error(R.drawable.ic_launcher_logo)
                    .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC).into(categoryImageView)
            }

            menuConstraintLayout.throttleClicks().subscribeAndObserveOnMainThread {
                userStoreCategoryStateSubject.onNext(menusItem)
            }.autoDispose()
        }
    }
}