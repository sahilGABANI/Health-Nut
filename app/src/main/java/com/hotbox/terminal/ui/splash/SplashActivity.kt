package com.hotbox.terminal.ui.splash

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings.Secure
import android.view.View
import androidx.core.view.isVisible
import com.github.pwittchen.reactivenetwork.library.rx2.ReactiveNetwork
import com.hotbox.terminal.R
import com.hotbox.terminal.api.authentication.LoggedInUserCache
import com.hotbox.terminal.application.HotBoxApplication
import com.hotbox.terminal.base.BaseActivity
import com.hotbox.terminal.base.ViewModelFactory
import com.hotbox.terminal.base.extension.*
import com.hotbox.terminal.databinding.ActivitySplashBinding
import com.hotbox.terminal.ui.login.LoginActivity
import com.hotbox.terminal.ui.main.MainActivity
import com.hotbox.terminal.ui.splash.viewmodel.LocationViewModel
import com.hotbox.terminal.ui.splash.viewmodel.LocationViewState
import com.hotbox.terminal.ui.wifi.NoWifiActivity
import com.hotbox.terminal.utils.Constants.DEVICE_ID
import com.hotbox.terminal.utils.Constants.isDebugMode
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import javax.inject.Inject


@SuppressLint("CustomSplashScreen")
class SplashActivity : BaseActivity() {

    private lateinit var binding: ActivitySplashBinding

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<LocationViewModel>
    private lateinit var locationViewModel: LocationViewModel
    private lateinit var androidId: String

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache

    companion object {
        fun getIntent(context: Context): Intent {
            return Intent(context, SplashActivity::class.java)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        HotBoxApplication.component.inject(this)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        locationViewModel = getViewModelFromFactory(viewModelFactory)
        setContentView(binding.root)
        window?.decorView?.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)

        listenToViewModel()
        initUI()
    }

    @SuppressLint("HardwareIds")
    private fun initUI() {
        if (!loggedInUserCache.isUserLoggedIn()) {
            androidId = Secure.getString(contentResolver, Secure.ANDROID_ID).toString()
            binding.androidIdTextView.text = androidId
            binding.startButton.throttleClicks().subscribeAndObserveOnMainThread {
                locationViewModel.clickOnStartButton()
//                Bugsnag.notify(RuntimeException("Test error"))
            }.autoDispose()
            ReactiveNetwork.observeInternetConnectivity().subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
                .subscribe { isConnectedToInternet: Boolean ->
                    if (!isConnectedToInternet) {
                        startActivityWithDefaultAnimation(NoWifiActivity.getIntent(this))
                    }
                }.autoDispose()
            binding.androidIdTextView.throttleClicks().subscribeAndObserveOnMainThread {
                val clip = ClipData.newPlainText("Copied Text", androidId)
                (getSystemService(CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(clip)
                showToast("Android id is copied")
            }.autoDispose()
        } else {
            finish()
            startActivity(MainActivity.getIntent(this@SplashActivity))
        }

    }

    override fun onResume() {
        if (isDebugMode()) {
            androidId = DEVICE_ID
        }
        locationViewModel.loadLocation(androidId)
        super.onResume()
    }

    override fun onPause() {
        locationViewModel.clear()
        super.onPause()
    }

    private fun listenToViewModel() {
        locationViewModel.locationState.subscribeAndObserveOnMainThread {
            when (it) {
                is LocationViewState.ErrorMessage -> {
                    binding.locationTextView.text = getString(R.string.no_location_set)
                }
                is LocationViewState.LoadingState -> {
                    progressVisibility(it.isLoading)
                }
                is LocationViewState.SuccessMessage -> {
                    showToast(it.successMessage)
                }
                is LocationViewState.LocationsData -> {
                    setLocationInformation(it.locationResponse?.locationName)
                }
                is LocationViewState.OpenLoginScreen -> {
                    startActivityWithDefaultAnimation(
                        LoginActivity.getIntent(
                            this, it.locationResponse.location?.id
                        )
                    )
                    finish()
                }
                is LocationViewState.StartButtonState -> {
                    startButtonVisibility(it.isVisible)
                }
            }
        }.autoDispose()
    }

    private fun setLocationInformation(locationResponse: String?) {
        binding.locationTextView.text = locationResponse
    }

    private fun progressVisibility(isVisible: Boolean) {
        binding.progressBar.isVisible = isVisible
    }

    private fun startButtonVisibility(isVisible: Boolean) {
        binding.startButton.isVisible = isVisible

    }
}