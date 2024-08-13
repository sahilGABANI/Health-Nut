package com.hotbox.terminal.ui.userstore.checkout

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.fragment.app.DialogFragment
import com.epson.epos2.Epos2Exception
import com.epson.epos2.printer.Printer
import com.hotbox.terminal.R
import com.hotbox.terminal.api.authentication.LoggedInUserCache
import com.hotbox.terminal.api.userstore.model.CompReasonType
import com.hotbox.terminal.application.HotBoxApplication
import com.hotbox.terminal.base.BaseDialogFragment
import com.hotbox.terminal.base.BaseFragment
import com.hotbox.terminal.base.extension.*
import com.hotbox.terminal.databinding.FragmentPhoneLoyaltyBinding
import com.hotbox.terminal.databinding.PrintReceiptDialogBinding
import com.hotbox.terminal.utils.UserInteractionInterceptor
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import javax.inject.Inject

class PhoneLoyaltyFragment : BaseDialogFragment() {
    private var _binding: FragmentPhoneLoyaltyBinding? = null
    private val binding get() = _binding!!

    private val checkWithPhoneClickSubject: PublishSubject<String> = PublishSubject.create()
    val checkWithPhoneClick: Observable<String> = checkWithPhoneClickSubject.hide()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.MyDialog)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPhoneLoyaltyBinding.inflate(inflater, container, false)
        dialog?.window?.requestFeature(Window.FEATURE_NO_TITLE)
        dialog?.window?.decorView?.systemUiVisibility =
            (View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        listenToViewEvent()
    }

    private fun listenToViewEvent() {

        binding.checkWithPhoneButton.throttleClicks().subscribeAndObserveOnMainThread {
            if (isValidate()) {
                checkWithPhoneClickSubject.onNext(binding.phoneEditText.text?.trim().toString())
                dismiss()
            }
        }.autoDispose()
        binding.closeButtonMaterialCardView.throttleClicks().subscribeAndObserveOnMainThread {
            dismiss()
        }.autoDispose()
    }

    private fun isValidate(): Boolean {
        return when {
            binding.phoneEditText.isNotValidPhoneLength() -> {
                showToast(getString(R.string.invalid_phone))
                false
            }
            else -> true
        }
    }
    override fun onStart() {
        super.onStart()
        UserInteractionInterceptor.wrapWindowCallback(requireActivity().window, activity)
    }
}