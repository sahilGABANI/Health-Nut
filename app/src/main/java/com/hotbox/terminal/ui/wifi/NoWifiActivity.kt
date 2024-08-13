package com.hotbox.terminal.ui.wifi

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import com.github.pwittchen.reactivenetwork.library.rx2.ReactiveNetwork
import com.hotbox.terminal.application.HotBoxApplication
import com.hotbox.terminal.base.ActivityManager
import com.hotbox.terminal.base.BaseActivity
import com.hotbox.terminal.base.extension.startActivityWithDefaultAnimation
import com.hotbox.terminal.base.extension.subscribeAndObserveOnMainThread
import com.hotbox.terminal.base.extension.throttleClicks
import com.hotbox.terminal.databinding.ActivityNoWifiBinding
import com.hotbox.terminal.ui.splash.SplashActivity
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers

class NoWifiActivity : BaseActivity() {

    private lateinit var binding: ActivityNoWifiBinding

    companion object {
        fun getIntent(context: Context): Intent {
            return Intent(context, NoWifiActivity::class.java)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        HotBoxApplication.component.inject(this)
        binding = ActivityNoWifiBinding.inflate(layoutInflater)
        setContentView(binding.root)
        window.decorView.apply {
            systemUiVisibility = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_FULLSCREEN
        }
        initUI()
    }

    private fun initUI() {
        ReactiveNetwork
            .observeInternetConnectivity()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { isConnectedToInternet: Boolean ->
                if (isConnectedToInternet) {
                    val activity = ActivityManager.getInstance().foregroundActivity
                    if (!(activity is SelectWifiActivity || activity is SplashActivity)) {
//                        startNewActivityWithDefaultAnimation(SplashActivity.getIntent(this))
                    }
                }
            }.autoDispose()
        binding.checkConnectionButton.throttleClicks().subscribeAndObserveOnMainThread {
            startActivityWithDefaultAnimation(AvailableWifiActivity.getIntent(this))
        }.autoDispose()
    }
}