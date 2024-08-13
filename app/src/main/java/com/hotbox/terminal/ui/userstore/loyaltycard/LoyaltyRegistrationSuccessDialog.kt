package com.hotbox.terminal.ui.userstore.loyaltycard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import com.hotbox.terminal.R
import com.hotbox.terminal.base.BaseDialogFragment
import com.hotbox.terminal.base.RxBus
import com.hotbox.terminal.base.RxEvent
import com.hotbox.terminal.base.extension.startActivityWithDefaultAnimation
import com.hotbox.terminal.base.extension.subscribeAndObserveOnMainThread
import com.hotbox.terminal.base.extension.throttleClicks
import com.hotbox.terminal.databinding.LoyaltyRegistrationSuccessDialogBinding
import com.hotbox.terminal.ui.userstore.UserStoreActivity

class LoyaltyRegistrationSuccessDialog : BaseDialogFragment() {


    private var _binding: LoyaltyRegistrationSuccessDialogBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.MyDialog)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = LoyaltyRegistrationSuccessDialogBinding.inflate(inflater, container, false)
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
        binding.backToCheckoutButton.throttleClicks().subscribeAndObserveOnMainThread {
            requireActivity().startActivityWithDefaultAnimation(UserStoreActivity.getIntent(requireContext(),false))
            requireActivity().finish()
            dismiss()
        }.autoDispose()

    }

}