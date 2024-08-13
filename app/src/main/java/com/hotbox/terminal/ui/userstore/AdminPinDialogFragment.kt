package com.hotbox.terminal.ui.userstore

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.inputmethod.EditorInfo
import android.widget.TextView.OnEditorActionListener
import androidx.core.view.isVisible
import com.hotbox.terminal.R
import com.hotbox.terminal.api.authentication.LoggedInUserCache
import com.hotbox.terminal.api.authentication.model.LoginCrewRequest
import com.hotbox.terminal.application.HotBoxApplication
import com.hotbox.terminal.base.BaseDialogFragment
import com.hotbox.terminal.base.ViewModelFactory
import com.hotbox.terminal.base.extension.*
import com.hotbox.terminal.databinding.FragmentAdminPinDialogBinding
import com.hotbox.terminal.ui.login.viewmodel.LoginViewModel
import com.hotbox.terminal.ui.login.viewmodel.LoginViewState
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import javax.inject.Inject

class AdminPinDialogFragment : BaseDialogFragment() {

    private var _binding: FragmentAdminPinDialogBinding? = null
    private val binding get() = _binding!!


    private val adminPinSuccessSubject: PublishSubject<String> = PublishSubject.create()
    val adminPinSuccess: Observable<String> = adminPinSuccessSubject.hide()

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache


    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<LoginViewModel>
    private lateinit var loginViewModel: LoginViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        HotBoxApplication.component.inject(this)
        loginViewModel = getViewModelFromFactory(viewModelFactory)
        setStyle(STYLE_NORMAL, R.style.MyDialog)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAdminPinDialogBinding.inflate(inflater, container, false)
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
        listenToViewModel()
        listenToViewEvent()
    }

    private fun listenToViewEvent() {
        binding.checkPinButton.throttleClicks().subscribeAndObserveOnMainThread {
            loginViewModel.checkAdminPin(LoginCrewRequest(binding.passwordEditText.text.toString(), loggedInUserCache.getLocationInfo()?.location?.id ?:0))
        }.autoDispose()
        binding.closeButtonMaterialCardView.throttleClicks().subscribeAndObserveOnMainThread {
            dismiss()
        }.autoDispose()

        binding.passwordEditText.setOnEditorActionListener(OnEditorActionListener { v, actionId, event ->
            requireActivity().hideKeyboard(v)
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                loginViewModel.checkAdminPin(LoginCrewRequest(binding.passwordEditText.text.toString(), loggedInUserCache.getLocationInfo()?.location?.id ?:0))
                true
            } else false
        })

        binding.llAdminPinBackground.setOnTouchListener { v, event ->
            requireActivity().hideKeyboard(v)
        }
    }



    private fun listenToViewModel() {
        loginViewModel.loginState.subscribeAndObserveOnMainThread {
            when (it) {
                is LoginViewState.ErrorMessage -> {
                    updateErrorText()
                }
                is LoginViewState.LoadingState -> {

                }
                is LoginViewState.SuccessMessage -> {
                    showToast(it.successMessage)
                }
                is LoginViewState.LoginSuccess -> {

                }
                is LoginViewState.CheckAdminPin -> {
                    if (it.loginResponse.isAdminPin == true) {
                        it.loginResponse.userId?.let { it1 -> adminPinSuccessSubject.onNext(it1) }
                        this.dismiss()
                    } else {
                        updateErrorText()
                    }
                }
                else -> {

                }
            }
        }.autoDispose()
    }


    private fun updateErrorText() {
        binding.errorTextView.isVisible = true
        binding.errorTextView.setText(R.string.error_admin_pin)
    }

}


