package com.hotbox.terminal.ui.login

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.View
import androidx.core.view.isVisible
import com.hotbox.terminal.BuildConfig
import com.hotbox.terminal.R
import com.hotbox.terminal.api.authentication.LoggedInUserCache
import com.hotbox.terminal.api.authentication.model.AvailableToPrintRequest
import com.hotbox.terminal.api.authentication.model.LoginCrewRequest
import com.hotbox.terminal.databinding.ActivityLoginBinding
import com.hotbox.terminal.application.HotBoxApplication
import com.hotbox.terminal.base.BaseActivity
import com.hotbox.terminal.base.ViewModelFactory
import com.hotbox.terminal.base.extension.*
import com.hotbox.terminal.ui.login.viewmodel.LoginViewModel
import com.hotbox.terminal.ui.login.viewmodel.LoginViewState
import com.hotbox.terminal.ui.main.MainActivity
import com.hotbox.terminal.ui.userstore.UserStoreWelcomeActivity
import com.hotbox.terminal.utils.Constants
import com.hotbox.terminal.utils.Constants.AVAILABLE_TO_PRINT_STATUS
import com.hotbox.terminal.utils.Constants.DEVICE_ID
import com.hotbox.terminal.utils.Constants.isDebugMode
import javax.inject.Inject
import kotlin.random.Random

class LoginActivity : BaseActivity() {

    companion object {
        const val LOCATION_ID = "LOCATION_ID"
        fun getIntent(context: Context, locationId: Int? = null): Intent {
            val intent = Intent(context, LoginActivity::class.java)
            intent.putExtra(LOCATION_ID, locationId)
            return intent
        }
    }

    private lateinit var binding: ActivityLoginBinding

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<LoginViewModel>
    private lateinit var loginViewModel: LoginViewModel
    private lateinit var androidId: String

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        HotBoxApplication.component.inject(this)
        loginViewModel = getViewModelFromFactory(viewModelFactory)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        window?.decorView?.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
        initUI()
        listenToViewModel()

    }

    private fun listenToViewModel() {
        loginViewModel.loginState.subscribeAndObserveOnMainThread {
            when (it) {
                is LoginViewState.ErrorMessage -> {
                    updateErrorText(it.errorMessage)
                }
                is LoginViewState.LoadingState -> {
                    buttonVisibility(it.isLoading)
                }
                is LoginViewState.SuccessMessage -> {
                    showToast(it.successMessage)
                }
                is LoginViewState.LoginSuccess -> {
                    showToast("Crew login success")

                    val locationId = loggedInUserCache.getLocationInfo()?.location?.id ?: throw Exception("location not found")
                    val availableToPrintRequest = AvailableToPrintRequest(
                        serialNumber = if (isDebugMode()) DEVICE_ID else androidId,
                        locationId =  locationId,
                        status = AVAILABLE_TO_PRINT_STATUS
                    )
                    loginViewModel.availableToPrint(availableToPrintRequest)
                    loggedInUserCache.setLoggedInUserCartGroupId(0)
                    loggedInUserCache.setLoggedInUserRandomNumber(generateRandomNumber())
                    startActivityWithDefaultAnimation(MainActivity.getIntent(this))
                    finish()
                }
                else -> {}
            }
        }.autoDispose()
    }
    private fun generateRandomNumber(): Int {
         val random = Random(System.currentTimeMillis())
        return random.nextInt(15, 59)
    }
    private fun initUI() {
        androidId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID).toString()
        guestOrCrewSelection(false)
        if (!isDebugMode()) {
            if (loggedInUserCache.getLocationInfo()?.guestMode == false) {
                binding.guestSelectLinear.isVisible = loggedInUserCache.getLocationInfo()?.guestMode != false
                binding.viewLogInGuest.root.isVisible = loggedInUserCache.getLocationInfo()?.guestMode != false
                binding.viewLogInCrew.root.isVisible = loggedInUserCache.getLocationInfo()?.guestMode == false
                guestOrCrewSelection(true)
            }
        }
        binding.guestSelectLinear.throttleClicks().subscribeAndObserveOnMainThread {
            guestOrCrewSelection(false)
        }.autoDispose()

        binding.crewSelectLinear.throttleClicks().subscribeAndObserveOnMainThread {
            guestOrCrewSelection(true)
        }.autoDispose()
        binding.viewLogInCrew.forgotPasswordTextView.throttleClicks()
            .subscribeAndObserveOnMainThread {
                binding.viewLogInCrew.root.visibility = View.GONE
                binding.viewLogInGuest.root.visibility = View.GONE
                binding.viewForgotPassword.root.visibility = View.VISIBLE
            }.autoDispose()

        binding.viewForgotPassword.loginAgainButton.throttleClicks()
            .subscribeAndObserveOnMainThread {
                binding.crewImageView.isSelected = true
                binding.guestImageview.isSelected = false
                binding.guestTextview.isSelected = false
                binding.crewTextview.isSelected = true
                binding.guestSelectLinear.isSelected = false
                binding.crewSelectLinear.isSelected = true
                binding.viewLogInGuest.root.visibility = View.GONE
                binding.viewForgotPassword.root.visibility = View.GONE
                binding.viewLogInCrew.root.visibility = View.VISIBLE
            }.autoDispose()

        binding.viewLogInCrew.loginButton.throttleClicks().subscribeAndObserveOnMainThread {
            if (isValidate()) {
                val password = binding.viewLogInCrew.passwordEditText.text.toString()
                binding.viewLogInCrew.errorTextView.isVisible = false
                val locationId = intent.getIntExtra(LOCATION_ID, 0)
                loginViewModel.loginCrew(LoginCrewRequest(password,locationId))
            } else {
                binding.viewLogInCrew.errorTextView.isVisible = true
            }
        }.autoDispose()
        binding.viewLogInGuest.proceedToMenuButton.throttleClicks().subscribeAndObserveOnMainThread {
            startActivityWithDefaultAnimation(UserStoreWelcomeActivity.getIntent(this,true))
        }.autoDispose()
    }

    private fun isValidate(): Boolean {
        return when {
            binding.viewLogInCrew.passwordEditText.isFieldBlank() -> {
                updateErrorText(getText(R.string.blank_password).toString())
                false
            }
            else -> true
        }
    }

    private fun buttonVisibility(isLoading: Boolean) {
        binding.viewLogInCrew.loginButton.visibility = if (isLoading) View.GONE else View.VISIBLE
        binding.viewLogInCrew.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
    }

    private fun updateErrorText(errorMessage: String) {
        binding.viewLogInCrew.errorTextView.isVisible = true
        binding.viewLogInCrew.errorTextView.text = errorMessage
    }

    private fun guestOrCrewSelection(isCrew: Boolean) {
        binding.guestImageview.isSelected = !isCrew
        binding.crewImageView.isSelected = isCrew
        binding.guestTextview.isSelected = !isCrew
        binding.crewTextview.isSelected = isCrew
        binding.guestSelectLinear.isSelected = !isCrew
        binding.crewSelectLinear.isSelected = isCrew
        binding.viewLogInGuest.root.visibility = if (isCrew) View.GONE else View.VISIBLE
        binding.viewLogInCrew.root.visibility = if (isCrew) View.VISIBLE else View.GONE
        binding.viewForgotPassword.root.visibility = View.GONE
    }
}