package com.hotbox.terminal.ui.userstore

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.view.isVisible
import com.hotbox.terminal.api.authentication.LoggedInUserCache
import com.hotbox.terminal.api.checkout.model.QRScanResponse
import com.hotbox.terminal.api.store.model.AssignedEmployeeInfo
import com.hotbox.terminal.application.HotBoxApplication
import com.hotbox.terminal.base.BaseActivity
import com.hotbox.terminal.base.ViewModelFactory
import com.hotbox.terminal.base.extension.*
import com.hotbox.terminal.databinding.ActivitySelectEmployeeBinding
import com.hotbox.terminal.ui.login.LoginActivity
import com.hotbox.terminal.ui.userstore.viewmodel.UserStoreState
import com.hotbox.terminal.ui.userstore.viewmodel.UserStoreViewModel
import timber.log.Timber
import javax.inject.Inject

class SelectEmployeeActivity : BaseActivity() {

    companion object {
        fun getIntent(context: Context, ): Intent {
            return Intent(context, SelectEmployeeActivity::class.java)
        }
    }

//    var DISCONNECT_TIMEOUT: Long = 300000 // 5 min = 5 * 60 * 1000 ms
//    var SHOW_TOAST_TIMER: Long = 270000

    var DISCONNECT_TIMEOUT: Long = 14400000 // 240 min = 240 * 60 * 1000 ms
    var SHOW_TOAST_TIMER: Long = 12600000 // 210 min = 5 * 60 * 1000 ms
    private lateinit var binding: ActivitySelectEmployeeBinding
    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<UserStoreViewModel>
    private lateinit var userStoreViewModel: UserStoreViewModel
    private var employeeList : ArrayList<AssignedEmployeeInfo> = arrayListOf()
    private var employeeNameList = listOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        HotBoxApplication.component.inject(this)
        userStoreViewModel = getViewModelFromFactory(viewModelFactory)
        binding = ActivitySelectEmployeeBinding.inflate(layoutInflater)
        window.decorView.apply {
            systemUiVisibility = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_FULLSCREEN
        }
        setContentView(binding.root)
        listenToViewModel()
        initUI()
    }

    private fun initUI() {
        if (loggedInUserCache.isUserLoggedIn()) {
             DISCONNECT_TIMEOUT = 14400000  // 240 min = 240 * 60 * 1000 ms
             SHOW_TOAST_TIMER = 12600000   // 210 min = 5 * 60 * 1000 ms
            onUserInteraction()
        }
        binding.confirmMaterialButton.isEnabled = false
        binding.autoCompleteStatus.throttleClicks().subscribeAndObserveOnMainThread {
            binding.autoCompleteStatus.isSelected = true
            binding.autoCompleteStatus.showDropDown()
        }.autoDispose()
        binding.confirmMaterialButton.throttleClicks().subscribeAndObserveOnMainThread {
            loggedInUserCache.setIsEmployeeMeal(true)
            startActivityWithDefaultAnimation(UserStoreActivity.getIntent(this,false,))
            finish()
        }.autoDispose()
        binding.closeButtonMaterialCardView.throttleClicks().subscribeAndObserveOnMainThread {
            finish()
        }.autoDispose()
        binding.autoCompleteStatus.onItemClickListener = AdapterView.OnItemClickListener { parent, _, position, _ ->
            binding.autoCompleteStatus.isSelected = false
            loggedInUserCache.setemployeeUserIdEmployeeMeal(employeeList[position].user?.id ?: "")
            loggedInUserCache.setLoyaltyQrResponse(QRScanResponse(phone = employeeList[position].user?.userPhone , email = employeeList[position].user?.userEmail , fullName = employeeList[position].user?.firstName.plus(" ").plus(employeeList[position].user?.lastName) , id = employeeList[position].user?.id))
            binding.confirmMaterialButton.isEnabled = true
        }
        binding.tvBack.throttleClicks().subscribeAndObserveOnMainThread {
            onBackPressed()
        }.autoDispose()
    }

    private fun listenToViewModel() {
        userStoreViewModel.userStoreState.subscribeAndObserveOnMainThread {
            when (it) {
                is UserStoreState.ErrorMessage -> {
                    showToast(it.errorMessage)
                }
                is UserStoreState.LoadingState -> {

                }
                is UserStoreState.SuccessMessage -> {
                    showToast(it.successMessage)
                }
                is UserStoreState.StoreResponses -> {
                    it.storeResponse.employee?.let {
                        employeeList.addAll(it.filter { it.active == true })
                    }
                    employeeList.forEach { employee ->
                        employeeNameList = employeeNameList + employee.getSafeFullName()
                    }
                    val arrayAdapter = ArrayAdapter(this@SelectEmployeeActivity, android.R.layout.simple_spinner_dropdown_item, employeeNameList)
                    binding.autoCompleteStatus.setAdapter(arrayAdapter)
                }
                else -> {

                }
            }
        }.autoDispose()
    }

    override fun onResume() {
        super.onResume()
        userStoreViewModel.loadCurrentStoreResponse()
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
                this@SelectEmployeeActivity, loggedInUserCache.getLocationInfo()?.location?.id ?: null
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