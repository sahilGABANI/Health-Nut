package com.hotbox.terminal.ui.userstore

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.widget.Toast
import androidx.core.view.isVisible
import com.hotbox.terminal.api.authentication.LoggedInUserCache
import com.hotbox.terminal.api.checkout.model.QRScanResponse
import com.hotbox.terminal.application.HotBoxApplication
import com.hotbox.terminal.base.BaseActivity
import com.hotbox.terminal.base.extension.startActivityWithDefaultAnimation
import com.hotbox.terminal.base.extension.startNewActivityWithDefaultAnimation
import com.hotbox.terminal.base.extension.subscribeAndObserveOnMainThread
import com.hotbox.terminal.base.extension.throttleClicks
import com.hotbox.terminal.databinding.ActivityUserStoreWelcomeBinding
import com.hotbox.terminal.ui.login.LoginActivity
import com.hotbox.terminal.ui.userstore.loyaltycard.LoyaltyCardFragment
import com.hotbox.terminal.utils.Constants.ORDER_TYPE_DINE_IN
import com.hotbox.terminal.utils.Constants.ORDER_TYPE_EMPLOYEE_MEAL
import com.hotbox.terminal.utils.Constants.ORDER_TYPE_ID_DINE_IN
import com.hotbox.terminal.utils.Constants.ORDER_TYPE_ID_EMPLOYEE_MEAL
import com.hotbox.terminal.utils.Constants.ORDER_TYPE_ID_TO_GO
import com.hotbox.terminal.utils.Constants.ORDER_TYPE_TO_GO
import io.reactivex.Observable
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class UserStoreWelcomeActivity : BaseActivity() {
    private var isGuest: Boolean = false
    companion object {
        const val IS_GUEST = "IS_GUEST"
        fun getIntent(context: Context, isGuest: Boolean): Intent {
            val intent = Intent(context, UserStoreWelcomeActivity::class.java)
            intent.putExtra(IS_GUEST, isGuest)
            return intent
        }
    }

//    var DISCONNECT_TIMEOUT: Long = 300000 // 5 min = 5 * 60 * 1000 ms
//    var SHOW_TOAST_TIMER: Long = 270000

    var DISCONNECT_TIMEOUT: Long = 14400000 // 240 min = 240 * 60 * 1000 ms
    var SHOW_TOAST_TIMER: Long = 12600000 // 210 min = 5 * 60 * 1000 ms

    private lateinit var binding: ActivityUserStoreWelcomeBinding
    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserStoreWelcomeBinding.inflate(layoutInflater)
        HotBoxApplication.component.inject(this)
        setContentView(binding.root)
        window.decorView.apply {
            systemUiVisibility = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_FULLSCREEN
        }
        initUI()
    }

    private fun initUI() {
        intent.apply {
            isGuest = getBooleanExtra(UserStoreActivity.IS_GUEST,false)
        }

        if (loggedInUserCache.isUserLoggedIn()) {
            DISCONNECT_TIMEOUT = 14400000 // 240 min = 240 * 60 * 1000 ms
            SHOW_TOAST_TIMER = 12600000 // 210 min = 5 * 60 * 1000 ms
            onUserInteraction()
        }
        if (!isGuest) binding.clEmployeeMeal.visibility = View.VISIBLE
        binding.clDineIn.throttleClicks().subscribeAndObserveOnMainThread {
            loggedInUserCache.setIsEmployeeMeal(false)
            manageSelectionVisibility(ORDER_TYPE_DINE_IN)
            loggedInUserCache.setorderTypeId(ORDER_TYPE_ID_DINE_IN)
            loggedInUserCache.setLoggedInUserCartGroupId(0)
            loggedInUserCache.setemployeeUserIdEmployeeMeal(null)
            val loyaltyCardFragment =  LoyaltyCardFragment().apply {
                qrScan.subscribeAndObserveOnMainThread {
                    this.dismiss()
                    startActivity(UserStoreActivity.getIntent(requireContext(),false))
                    this@UserStoreWelcomeActivity.finish()
                }.autoDispose()
            }
            loyaltyCardFragment.show(supportFragmentManager, LoyaltyCardFragment::class.java.name)
        }.autoDispose()
        binding.clFoodToGo.throttleClicks().subscribeAndObserveOnMainThread {
            loggedInUserCache.setIsEmployeeMeal(false)
            loggedInUserCache.setorderTypeId(ORDER_TYPE_ID_TO_GO)
            loggedInUserCache.setLoggedInUserCartGroupId(0)
            loggedInUserCache.setemployeeUserIdEmployeeMeal(null)
            manageSelectionVisibility(ORDER_TYPE_TO_GO)
            val loyaltyCardFragment =  LoyaltyCardFragment().apply {
                qrScan.subscribeAndObserveOnMainThread {
                    this.dismiss()
                    startActivity(UserStoreActivity.getIntent(requireContext(),false))
                    this@UserStoreWelcomeActivity.finish()
                }.autoDispose()
            }
            loyaltyCardFragment.show(supportFragmentManager, LoyaltyCardFragment::class.java.name)
        }.autoDispose()
        binding.closeButtonMaterialCardView.throttleClicks().subscribeAndObserveOnMainThread {
           finish()
        }.autoDispose()
        binding.clEmployeeMeal.throttleClicks().subscribeAndObserveOnMainThread {
            loggedInUserCache.setLoyaltyQrResponse(QRScanResponse(null,null,null,null,null,null))
            manageSelectionVisibility(ORDER_TYPE_EMPLOYEE_MEAL)
            loggedInUserCache.setorderTypeId(ORDER_TYPE_ID_EMPLOYEE_MEAL)
            loggedInUserCache.setLoggedInUserCartGroupId(0)
            Observable.timer(500, TimeUnit.MILLISECONDS).subscribeAndObserveOnMainThread {
                startActivityWithDefaultAnimation(SelectEmployeeActivity.getIntent(this))
                finish()
            }.autoDispose()
        }.autoDispose()

    }


    private fun manageSelectionVisibility(isSelected :String? = null) {
        when (isSelected) {
            ORDER_TYPE_DINE_IN -> {
                binding.clDineIn.isSelected = true
                binding.dineInCheckImageView.isVisible = true
                binding.clFoodToGo.isSelected = false
                binding.foodToGoCheckImageView.isVisible = false
                binding.clEmployeeMeal.isSelected = false
                binding.employeeMealCheckImageView.isVisible = false
            }
            ORDER_TYPE_TO_GO -> {
                binding.clDineIn.isSelected = false
                binding.dineInCheckImageView.isVisible = false
                binding.clFoodToGo.isSelected = true
                binding.foodToGoCheckImageView.isVisible = true
                binding.clEmployeeMeal.isSelected = false
                binding.employeeMealCheckImageView.isVisible = false
            }
            ORDER_TYPE_EMPLOYEE_MEAL -> {
                binding.clDineIn.isSelected = false
                binding.dineInCheckImageView.isVisible = false
                binding.clFoodToGo.isSelected = false
                binding.foodToGoCheckImageView.isVisible = false
                binding.clEmployeeMeal.isSelected = true
                binding.employeeMealCheckImageView.isVisible = true
            }
            else -> {
                binding.clDineIn.isSelected = false
                binding.dineInCheckImageView.isVisible = false
                binding.clFoodToGo.isSelected = false
                binding.foodToGoCheckImageView.isVisible = false
                binding.clEmployeeMeal.isSelected = false
                binding.employeeMealCheckImageView.isVisible = false
            }
        }
    }

    override fun onResume() {
        super.onResume()
        manageSelectionVisibility()
    }


    override fun onUserInteraction() {
        Timber.tag("SessionTimeOutActivity").d("onUserInteraction")
        binding.toolbarLinearLayout.alpha = 1F
        binding.semiTransparentImageView.alpha = 1F
        binding.logOutTextView.isVisible = false
        resetDisconnectTimer()
    }

    private val disconnectHandler = Handler {
        Timber.tag("SessionTimeOutActivity").d("disconnectHandler")
        false
    }
    private val showToastHandler = Handler {
        Timber.tag("SessionTimeOutActivity").d("showToastHandler")
        false
    }

    private val disconnectCallback = Runnable { // Perform any required operation on disconnect
        Timber.tag("SessionTimeOutActivity").d("disconnectCallback")
        loggedInUserCache.clearLoggedInUserLocalPrefs()
        Toast.makeText(applicationContext, "Guest time out", Toast.LENGTH_LONG).show()
        startNewActivityWithDefaultAnimation(
            LoginActivity.getIntent(
                this@UserStoreWelcomeActivity, loggedInUserCache.getLocationInfo()?.location?.id ?: null
            )
        )
        finish()
    }
    private val showToastCallback = Runnable { // Perform any required operation on disconnect
        Timber.tag("SessionTimeOutActivity").d("showToastCallback")
        if (loggedInUserCache.isUserLoggedIn()) {
            binding.toolbarLinearLayout.alpha = 0.2F
            binding.semiTransparentImageView.alpha = 0.2F
            binding.logOutTextView.isVisible = true

        }

    }

    private fun resetDisconnectTimer() {
        Timber.tag("SessionTimeOutActivity").d("resetDisconnectTimer")
        disconnectHandler.removeCallbacks(disconnectCallback)
        showToastHandler.removeCallbacks(showToastCallback)
        disconnectHandler.postDelayed(disconnectCallback, DISCONNECT_TIMEOUT)
        showToastHandler.postDelayed(showToastCallback, SHOW_TOAST_TIMER)
    }

    fun stopDisconnectTimer() {
        disconnectHandler.removeCallbacks(disconnectCallback)
        showToastHandler.removeCallbacks(showToastCallback)
    }


    override fun onPause() {
        super.onPause()
        Timber.tag("UserStoreActivity").d("stopDisconnectTimer")
        stopDisconnectTimer()
    }
}