package com.hotbox.terminal.ui.wifi

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.annotation.NonNull
import androidx.core.view.isVisible
import com.hotbox.terminal.R
import com.hotbox.terminal.application.HotBoxApplication
import com.hotbox.terminal.base.BaseActivity
import com.hotbox.terminal.base.extension.*
import com.hotbox.terminal.databinding.ActivitySelectWifiBinding
import com.hotbox.terminal.ui.splash.SplashActivity
import com.thanosfisherman.wifiutils.WifiUtils
import com.thanosfisherman.wifiutils.wifiConnect.ConnectionErrorCode
import com.thanosfisherman.wifiutils.wifiConnect.ConnectionSuccessListener

class SelectWifiActivity : BaseActivity() {

    private lateinit var binding: ActivitySelectWifiBinding

    private var wifiName: String? = null

    companion object {
        var WIFI_NAME = "WIFI_NAME"
        fun getIntent(context: Context, wifiName: String): Intent {
            val intent = Intent(context, SelectWifiActivity::class.java)
            intent.putExtra(WIFI_NAME, wifiName)
            return intent
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        HotBoxApplication.component.inject(this)
        binding = ActivitySelectWifiBinding.inflate(layoutInflater)
        setContentView(binding.root)
        window.decorView.apply {
            systemUiVisibility = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_FULLSCREEN
        }
        initUI()
    }

    private fun initUI() {

        intent?.let {
            wifiName = it.getStringExtra(WIFI_NAME)

            binding.wifiNameAppCompatTextView.text = wifiName
        }


        binding.backLinearLayout.onClick {
            onBackPressed()
        }
        binding.tvBack.throttleClicks().subscribeAndObserveOnMainThread {
            onBackPressed()
        }.autoDispose()



        binding.connectMaterialButton.onClick {
            if (isValidate()) {
                WifiUtils.withContext(applicationContext)
                    .connectWith(wifiName ?: "", binding.passwordEditText.text.toString())
                    .setTimeout(60000)
                    .onConnectionResult(object : ConnectionSuccessListener {
                        override fun success() {
                            Toast.makeText(applicationContext, "SUCCESS!", Toast.LENGTH_SHORT).show()
                            startNewActivityWithDefaultAnimation(SplashActivity.getIntent(this@SelectWifiActivity))
                        }

                        override fun failed(@NonNull errorCode: ConnectionErrorCode) {
                            runOnUiThread {
                                binding.errorTextView.isVisible = true
                                binding.connectMaterialButton.text = resources.getString(R.string.try_again)
                            }
                        }
                    }).start()

            } else {
                binding.errorTextView.isVisible = true
            }
        }
    }

    private fun isValidate(): Boolean {
        return when {
            binding.passwordEditText.isFieldBlank() -> {
                showToast(getString(R.string.blank_password))
                false
            }
            else -> true
        }
    }
}