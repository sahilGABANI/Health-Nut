package com.hotbox.terminal.ui.userstore.loyaltycard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import com.hotbox.terminal.R
import com.hotbox.terminal.api.authentication.LoggedInUserCache
import com.hotbox.terminal.api.checkout.model.CreateUserRequest
import com.hotbox.terminal.api.checkout.model.QRScanResponse
import com.hotbox.terminal.api.userstore.model.UserDetails
import com.hotbox.terminal.application.HotBoxApplication
import com.hotbox.terminal.base.BaseDialogFragment
import com.hotbox.terminal.base.RxBus
import com.hotbox.terminal.base.RxEvent
import com.hotbox.terminal.base.ViewModelFactory
import com.hotbox.terminal.base.extension.*
import com.hotbox.terminal.databinding.JoinLoyaltyProgramDialogBinding
import com.hotbox.terminal.ui.userstore.checkout.viewmodel.CheckOutState
import com.hotbox.terminal.ui.userstore.checkout.viewmodel.CheckOutViewModel
import com.hotbox.terminal.utils.UserInteractionInterceptor
import javax.inject.Inject
import kotlin.random.Random

class JoinLoyaltyProgramDialog : BaseDialogFragment() {

    private var _binding: JoinLoyaltyProgramDialogBinding? = null
    private val binding get() = _binding!!

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<CheckOutViewModel>
    private lateinit var checkOutViewModel: CheckOutViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        HotBoxApplication.component.inject(this)
        checkOutViewModel = getViewModelFromFactory(viewModelFactory)
        setStyle(STYLE_NORMAL, R.style.MyDialog)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = JoinLoyaltyProgramDialogBinding.inflate(inflater, container, false)
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

    private fun listenToViewModel() {
        checkOutViewModel.checkOutState.subscribeAndObserveOnMainThread {
            when (it) {
                is CheckOutState.ErrorMessage -> {
                    showToast(it.errorMessage)
                }
                is CheckOutState.CreateUserInformation -> {
                    val loyaltyCardFragment = LoyaltyRegistrationSuccessDialog()
                    loyaltyCardFragment.show(parentFragmentManager, JoinLoyaltyProgramDialog::class.java.name)
                    dismiss()
                    loggedInUserCache.setLoyaltyQrResponse(
                        QRScanResponse(
                            it.createUserResponse.userPhone,
                            it.createUserResponse.fullName(),
                            it.createUserResponse.id,
                            it.createUserResponse.userEmail
                        )
                    )
                }
                else -> {

                }
            }

        }
    }

    private fun listenToViewEvent() {
        binding.registerMaterialButton.throttleClicks().subscribeAndObserveOnMainThread {
            if (isValidate()) {
                checkOutViewModel.createUser(
                    CreateUserRequest(
                        userEmail = binding.emailEditText.text.toString().trim(),
                        lastName = binding.surNameEditText.text.toString(),
                        userPhone = binding.phoneEditText.text.toString(),
                        firstName = binding.nameEditText.text.toString(),
                        userPassword = generateRandomPassword(8)
                    )
                )
            }
        }.autoDispose()
        binding.tvCancel.throttleClicks().subscribeAndObserveOnMainThread {
            dismiss()
        }.autoDispose()

    }
    fun generateRandomPassword(length: Int): String {
        val passwordLength = length.coerceAtLeast(1) // Ensure the length is at least 1
        val charPool = ('a'..'z') // Define the character pool as lowercase letters (a-z)
        val random = Random(System.currentTimeMillis()) // Create a random number generator

        val password = StringBuilder()
        repeat(passwordLength) {
            val randomChar = charPool.random(random) // Select a random character from the pool
            password.append(randomChar) // Append the character to the password
        }

        return password.toString()
    }

    private fun isValidate(): Boolean {
        return when {
            binding.nameEditText.isFieldBlank() -> {
                showToast(getString(R.string.invalid_name))
                false
            }
            binding.surNameEditText.isFieldBlank() -> {
                showToast(getString(R.string.invalid_surname))
                false
            }
            binding.phoneEditText.isFieldBlank() -> {
                showToast(getString(R.string.invalid_phone))
                false
            }
            binding.phoneEditText.isNotValidPhoneLength() -> {
                showToast(getString(R.string.invalid_phone))
                false
            }
            binding.emailEditText.isNotValidEmail() -> {
                showToast(getString(R.string.invalid_email))
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