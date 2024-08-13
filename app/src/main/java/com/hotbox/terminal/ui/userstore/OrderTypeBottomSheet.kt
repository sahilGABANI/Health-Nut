package com.hotbox.terminal.ui.userstore

import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.hotbox.terminal.R
import com.hotbox.terminal.api.authentication.LoggedInUserCache
import com.hotbox.terminal.api.userstore.model.CompReasonType
import com.hotbox.terminal.application.HotBoxApplication
import com.hotbox.terminal.base.BaseBottomSheetDialogFragment
import com.hotbox.terminal.base.extension.subscribeAndObserveOnMainThread
import com.hotbox.terminal.base.extension.throttleClicks
import com.hotbox.terminal.databinding.CompReasonBottomSheetBinding
import com.hotbox.terminal.databinding.FragmentOrderTypeBottomSheetBinding
import com.hotbox.terminal.ui.userstore.loyaltycard.LoyaltyCardFragment
import com.hotbox.terminal.utils.Constants
import com.hotbox.terminal.utils.Constants.ORDER_TYPE_ID_DINE_IN
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import javax.inject.Inject

class OrderTypeBottomSheet : BaseBottomSheetDialogFragment() {

    private var _binding: FragmentOrderTypeBottomSheetBinding? = null
    private val binding get() = _binding!!


    private val orderTypeSubject: PublishSubject<Int> = PublishSubject.create()
    val orderTypeClick: Observable<Int> = orderTypeSubject.hide()

    companion object {

        fun newInstance() = OrderTypeBottomSheet()
    }

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        HotBoxApplication.component.inject(this)
        setStyle(STYLE_NORMAL, R.style.BSDialogThemeRegular)

    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentOrderTypeBottomSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val bottomSheet = (view.parent as View)
        bottomSheet.backgroundTintMode = PorterDuff.Mode.CLEAR
        bottomSheet.setBackgroundColor(Color.TRANSPARENT)

        dialog?.apply {
            val behavior: BottomSheetBehavior<*> = BottomSheetBehavior.from(bottomSheet)
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
            behavior.peekHeight = 0
            behavior.addBottomSheetCallback(mBottomSheetBehaviorCallback)
        }

        listenToViewEvent()
    }

    private fun listenToViewEvent() {
        manageSelectionVisibility("")
        binding.clDineIn.throttleClicks().subscribeAndObserveOnMainThread {
            manageSelectionVisibility("DINE IN")

            orderTypeSubject.onNext(ORDER_TYPE_ID_DINE_IN)
        }.autoDispose()
        binding.clFoodToGo.throttleClicks().subscribeAndObserveOnMainThread {

            manageSelectionVisibility("FOOD TO GO")
            orderTypeSubject.onNext(Constants.ORDER_TYPE_ID_TO_GO)
        }.autoDispose()
    }

    private fun manageSelectionVisibility(isSelected :String? = null) {
        when (isSelected) {
            "DINE IN" -> {
                binding.clDineIn.isSelected = true
                binding.dineInCheckImageView.isVisible = true
                binding.clFoodToGo.isSelected = false
                binding.foodToGoCheckImageView.isVisible = false
            }
            "FOOD TO GO" -> {
                binding.clDineIn.isSelected = false
                binding.dineInCheckImageView.isVisible = false
                binding.clFoodToGo.isSelected = true
                binding.foodToGoCheckImageView.isVisible = true
            }

            else -> {
                binding.clDineIn.isSelected = false
                binding.dineInCheckImageView.isVisible = false
                binding.clFoodToGo.isSelected = false
                binding.foodToGoCheckImageView.isVisible = false
            }
        }
    }

    private val mBottomSheetBehaviorCallback: BottomSheetBehavior.BottomSheetCallback =
        object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if (newState == BottomSheetBehavior.STATE_COLLAPSED || newState == BottomSheetBehavior.STATE_HIDDEN) {
                    dismiss()
                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {}
        }
}