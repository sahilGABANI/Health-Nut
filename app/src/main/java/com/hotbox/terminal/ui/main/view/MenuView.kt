package com.hotbox.terminal.ui.main.view

import android.content.Context
import android.content.res.TypedArray
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import androidx.core.view.isVisible
import com.hotbox.terminal.R
import com.hotbox.terminal.base.extension.leftDrawable
import com.hotbox.terminal.databinding.MenuViewBinding
import timber.log.Timber

class MenuView(context: Context, attributeSet: AttributeSet) : LinearLayout(context, attributeSet) {

    var menuName: String? = null
    var menuIcon: Drawable? = null
    var isMenuSelected: Boolean = false
    set(value) {
        manageSelection(value)
    }
    var notificationCount: Int = 0
    set(value) {
        updateCount(value)
    }
    private val binding = MenuViewBinding.inflate(LayoutInflater.from(context), this, true)

    init {
        val attributeArray: TypedArray = context.theme.obtainStyledAttributes(
            attributeSet,
            R.styleable.MenuView, 0, 0
        )
        try {
            menuName = attributeArray.getString(R.styleable.MenuView_name)
            menuIcon = attributeArray.getDrawable(R.styleable.MenuView_icon)
            isMenuSelected =
                attributeArray.getBoolean(R.styleable.MenuView_isMenuSelected, false)
            notificationCount =
                attributeArray.getInt(R.styleable.MenuView_notificationCount, 0)
        } finally {
            attributeArray.recycle()
        }
        binding.menuTextView.text = menuName ?: ""
        binding.menuTextView.leftDrawable(menuIcon)
        updateCount(notificationCount)
        manageSelection(isMenuSelected)
    }

    private fun manageSelection(isSelected: Boolean) {
        binding.selectionView.isVisible = isSelected
        binding.menuTextView.isSelected = isSelected
        binding.notificationCountTextView.isSelected = isSelected
        binding.menuConstraintLayout.isSelected = isSelected
    }

    private fun updateCount(count: Int) {
        if (count != 0) {
            binding.notificationCountTextView.isVisible = true
            binding.notificationCountTextView.text = count.toString()
                        Timber.d("order count: $count")
        } else {
            binding.notificationCountTextView.isVisible = false
            Timber.d("order count: $count")
        }
    }
}