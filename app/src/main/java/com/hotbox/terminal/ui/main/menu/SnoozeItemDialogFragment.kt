package com.hotbox.terminal.ui.main.menu

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import com.hotbox.terminal.R
import com.hotbox.terminal.api.menu.model.ProductsItem
import com.hotbox.terminal.base.BaseDialogFragment
import com.hotbox.terminal.base.extension.subscribeAndObserveOnMainThread
import com.hotbox.terminal.base.extension.throttleClicks
import com.hotbox.terminal.base.extension.toDollar
import com.hotbox.terminal.databinding.FragmentSnoozeItemDialogBinding
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class SnoozeItemDialogFragment(private val productsItem: ProductsItem) : BaseDialogFragment() {

    private var _binding: FragmentSnoozeItemDialogBinding? = null
    private val binding get() = _binding!!
    private val productActiveSubject: PublishSubject<Boolean> = PublishSubject.create()
    val productActive: Observable<Boolean> = productActiveSubject.hide()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.MyDialog)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSnoozeItemDialogBinding.inflate(inflater, container, false)
        dialog?.window?.requestFeature(Window.FEATURE_NO_TITLE)
        dialog?.window?.decorView?.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        listenToViewEvent()
    }

    private fun listenToViewEvent() {
        binding.productNameTextView.text = productsItem.productName
        binding.productPrize.text = productsItem.productBasePrice?.div(100).toDollar()
        binding.confirmButton.throttleClicks().subscribeAndObserveOnMainThread {
            productActiveSubject.onNext(true)
        }.autoDispose()
        binding.closeButtonMaterialCardView.throttleClicks().subscribeAndObserveOnMainThread {
            dismiss()
        }.autoDispose()
        binding.turnOffUntilItTurnItOnRadioButton.throttleClicks().subscribeAndObserveOnMainThread {
            binding.snoozedOneHourRadioButton.isChecked = false
            binding.snoozedFourHourRadioButton.isChecked = false
            binding.snoozeUntilTomorrowRadioButton.isChecked = false
            binding.turnOffUntilItTurnItOnLinearLayout.isSelected = true
            binding.snoozedOneHourLinearLayout.isSelected = false
            binding.snoozedFourHourLinearLayout.isSelected = false
            binding.snoozeUntilTomorrowLinearLayout.isSelected = false
        }.autoDispose()

        binding.snoozedOneHourRadioButton.throttleClicks().subscribeAndObserveOnMainThread {
            binding.turnOffUntilItTurnItOnRadioButton.isChecked = false
            binding.snoozedOneHourRadioButton.isChecked = true
            binding.snoozedFourHourRadioButton.isChecked = false
            binding.snoozeUntilTomorrowRadioButton.isChecked = false
            binding.turnOffUntilItTurnItOnLinearLayout.isSelected = false
            binding.snoozedOneHourLinearLayout.isSelected = true
            binding.snoozedFourHourLinearLayout.isSelected = false
            binding.snoozeUntilTomorrowLinearLayout.isSelected = false
        }.autoDispose()
        binding.snoozedFourHourRadioButton.throttleClicks().subscribeAndObserveOnMainThread {
            binding.turnOffUntilItTurnItOnRadioButton.isChecked = false
            binding.snoozedOneHourRadioButton.isChecked = false
            binding.snoozedFourHourRadioButton.isChecked = true
            binding.snoozeUntilTomorrowRadioButton.isChecked = false
            binding.turnOffUntilItTurnItOnLinearLayout.isSelected = false
            binding.snoozedOneHourLinearLayout.isSelected = false
            binding.snoozedFourHourLinearLayout.isSelected = true
            binding.snoozeUntilTomorrowLinearLayout.isSelected = false
        }.autoDispose()
        binding.snoozeUntilTomorrowRadioButton.throttleClicks().subscribeAndObserveOnMainThread {
            binding.turnOffUntilItTurnItOnRadioButton.isChecked = false
            binding.snoozedOneHourRadioButton.isChecked = false
            binding.snoozedFourHourRadioButton.isChecked = false
            binding.snoozeUntilTomorrowRadioButton.isChecked = true
            binding.turnOffUntilItTurnItOnLinearLayout.isSelected = false
            binding.snoozedOneHourLinearLayout.isSelected = false
            binding.snoozedFourHourLinearLayout.isSelected = false
            binding.snoozeUntilTomorrowLinearLayout.isSelected = true
        }.autoDispose()
    }
}