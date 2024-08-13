package com.hotbox.terminal.ui.main

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.AsyncTask
import android.os.Bundle
import android.os.Handler
import android.provider.Settings
import android.text.SpannableStringBuilder
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.core.view.isVisible
import com.hotbox.terminal.BuildConfig
import com.hotbox.terminal.R
import com.hotbox.terminal.api.authentication.LoggedInUserCache
import com.hotbox.terminal.api.authentication.model.AvailableToPrintRequest
import com.hotbox.terminal.api.order.model.OrderDetailsResponse
import com.hotbox.terminal.api.order.model.OrdersInfo
import com.hotbox.terminal.api.store.model.StoreResponse
import com.hotbox.terminal.api.store.model.UpdatePrintStatusRequest
import com.hotbox.terminal.application.HotBoxApplication
import com.hotbox.terminal.base.BaseActivity
import com.hotbox.terminal.base.RxBus
import com.hotbox.terminal.base.RxEvent
import com.hotbox.terminal.base.ViewModelFactory
import com.hotbox.terminal.base.extension.*
import com.hotbox.terminal.databinding.ActivityMainBinding
import com.hotbox.terminal.helper.*
import com.hotbox.terminal.helper.formatTo
import com.hotbox.terminal.helper.toDate
import com.hotbox.terminal.ui.login.LoginActivity
import com.hotbox.terminal.ui.main.store.viewmodel.StoreState
import com.hotbox.terminal.ui.main.store.viewmodel.StoreViewModel
import com.hotbox.terminal.ui.main.view.SideNavigationAdapter
import com.hotbox.terminal.ui.userstore.UserStoreWelcomeActivity
import com.hotbox.terminal.utils.Constants
import com.hotbox.terminal.utils.Constants.ORDER_STATUS_COMPLETED
import com.hotbox.terminal.utils.Constants.ORDER_STATUS_RECEIVE
import com.hotbox.terminal.utils.Constants.isDebugMode
import com.jakewharton.rxbinding3.widget.textChanges
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.properties.Delegates

class MainActivity : BaseActivity() {

    private lateinit var androidId: String
    private var bohPrintAddress: String? = null
    private lateinit var bohPrinterHelper: BohPrinterHelper

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<StoreViewModel>
    private lateinit var storeViewModel: StoreViewModel
    var versionName: String = BuildConfig.VERSION_NAME
    private var lastSelectedMenuId by Delegates.notNull<Int>()
    private val DISCONNECT_TIMEOUT: Long = 14400000 // 240 min = 240 * 60 * 1000 ms
    private val SHOW_TOAST_TIMER: Long = 12600000   // 210 min = 240 * 60 * 1000 ms

    companion object {
        fun getIntent(context: Context): Intent {
            return Intent(context, MainActivity::class.java)
        }
    }

    private lateinit var binding: ActivityMainBinding
    private lateinit var sideNavigationAdapter: SideNavigationAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        HotBoxApplication.component.inject(this)
        storeViewModel = getViewModelFromFactory(viewModelFactory)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        window.decorView.apply {
            systemUiVisibility =
                (View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
        }
        initUI()
        listenToViewModel()
        bohPrinterInitialize()
        storeViewModel.loadOrderData(loggedInUserCache.getLoggedInUserRandomNumber())
        storeViewModel.getPrintQueue(serialNumber = if (isDebugMode()) Constants.DEVICE_ID else androidId)
    }

    private fun bohPrinterInitialize() {
        Timber.tag("Printer").i("Call Connect BOH Printer")
        bohPrinterHelper = BohPrinterHelper.getInstance(this@MainActivity)
        bohPrinterHelper.printerInitialize(this@MainActivity)

    }

    @SuppressLint("HardwareIds")
    private fun initUI() {
        sideNavigationAdapter = SideNavigationAdapter(this)

        binding.viewpager.apply {
            offscreenPageLimit = 1
            adapter = sideNavigationAdapter
            isUserInputEnabled = false
        }
        setLastSelectedMenu(R.id.orderMenuView)
        binding.orderMenuView.isMenuSelected = true

        binding.orderMenuView.throttleClicks().subscribeAndObserveOnMainThread {
            binding.viewpager.currentItem = 0
            binding.searchEditText.text?.clear()
            manageSelection(R.id.orderMenuView)
            hideKeyboard()
        }.autoDispose()

        binding.deliveriesMenuView.throttleClicks().subscribeAndObserveOnMainThread {
            binding.viewpager.currentItem = 1
            manageSelection(R.id.deliveriesMenuView)
            binding.searchEditText.text?.clear()
            RxBus.publish(RxEvent.ClearLoyaltyScreen)
            hideKeyboard()
        }.autoDispose()

        binding.orderMenuMenuView.throttleClicks().subscribeAndObserveOnMainThread {
            binding.viewpager.currentItem = 2
            manageSelection(R.id.orderMenuMenuView)
            RxBus.publish(RxEvent.ClearLoyaltyScreen)
            hideKeyboard()
        }.autoDispose()

        binding.timeManagementMenuView.throttleClicks().subscribeAndObserveOnMainThread {
            binding.viewpager.currentItem = 3
            manageSelection(R.id.timeManagementMenuView)
            RxBus.publish(RxEvent.ClearLoyaltyScreen)
        }.autoDispose()

        binding.storeMenuView.throttleClicks().subscribeAndObserveOnMainThread {
            binding.viewpager.currentItem = 3
            manageSelection(R.id.storeMenuView)
            RxBus.publish(RxEvent.ClearLoyaltyScreen)
            hideKeyboard()
        }.autoDispose()

        binding.LoyaltyMenuView.throttleClicks().subscribeAndObserveOnMainThread {
            binding.viewpager.currentItem = 4
            manageSelection(R.id.LoyaltyMenuView)
            hideKeyboard()
        }.autoDispose()

        binding.giftCardMenuView.throttleClicks().subscribeAndObserveOnMainThread {
            binding.viewpager.currentItem = 5
            manageSelection(R.id.giftCardMenuView)
            RxBus.publish(RxEvent.ClearLoyaltyScreen)
            hideKeyboard()
        }.autoDispose()

        binding.settingMenuView.throttleClicks().subscribeAndObserveOnMainThread {
            binding.viewpager.currentItem = 6
            manageSelection(R.id.settingMenuView)
            RxBus.publish(RxEvent.ClearLoyaltyScreen)
            hideKeyboard()
        }.autoDispose()

        binding.logOutMenuView.throttleClicks().subscribeAndObserveOnMainThread {
            hideKeyboard()
            openLogOutDialog()
        }.autoDispose()
        binding.locationAppCompatTextView.text = loggedInUserCache.getLocationInfo()?.location?.locationName ?: ""
        binding.versionNameAppCompatTextView.text = resources.getString(R.string.title_version, versionName)
        androidId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
        binding.androidIdAppCompatTextView.text =
            resources.getString(R.string.title_device_id, androidId).plus(" (${loggedInUserCache.getLoggedInUserRandomNumber()})")
        binding.liveTimeTextClock.format12Hour = "hh:mm a"
        binding.loginCrewNameTextView.text = loggedInUserCache.getLoggedInUserFullName()
        binding.loggedInUserJobPostTextView.text = loggedInUserCache.getLoggedInUserRole()
        binding.openStoreTimeLinearLayout.isSelected = true
        binding.tvOpenAndClose.isSelected = true
        RxBus.listen(RxEvent.EventOrderCountListen::class.java).subscribeAndObserveOnMainThread {
            binding.orderMenuView.notificationCount = it.count
        }.autoDispose()

        RxBus.listen(RxEvent.EventDeliveryCountListen::class.java).subscribeAndObserveOnMainThread {
            binding.deliveriesMenuView.notificationCount = it.count
        }.autoDispose()
        RxBus.listen(RxEvent.HideShowEditTextMainActivity::class.java).subscribeAndObserveOnMainThread {
            binding.edtSearch.isVisible = it.isShow
        }.autoDispose()
        binding.searchEditText.textChanges().skipInitialValue().doOnNext {

        }.debounce(300, TimeUnit.MILLISECONDS, Schedulers.io()).subscribeOnIoAndObserveOnMainThread({
            if (it.isNotEmpty()) {
                RxBus.publish(RxEvent.SearchOrderFilter(it.toString()))
            } else {
                RxBus.publish(RxEvent.SearchOrderFilter(""))
            }
        }, {
            Timber.e(it)
        }).autoDispose()
        binding.newOrderMaterialButton.throttleClicks().subscribeAndObserveOnMainThread {
            if (!binding.edtSearch.isVisible) {
                RxBus.publish(RxEvent.CloseOrderDetailsScreen)
                startActivityWithDefaultAnimation(UserStoreWelcomeActivity.getIntent(this, false))
            } else {
                startActivityWithDefaultAnimation(UserStoreWelcomeActivity.getIntent(this, false))
            }
        }.autoDispose()
    }

    private fun listenToViewModel() {
        storeViewModel.storeState.subscribeOnIoAndObserveOnMainThread({
            when (it) {
                is StoreState.ErrorMessage -> {
                    showToast(it.errorMessage)
                }
                is StoreState.LoadingState -> {
                }
                is StoreState.SuccessMessage -> {
                    showToast(it.successMessage)
                }
                is StoreState.StoreResponses -> {
                    setStoreOpenAndClose(it.storeResponse)
                    loggedInUserCache.setStoreResponse(it.storeResponse)
                    loggedInUserCache.setlocationPhone(it.storeResponse.locationLocationPhone ?: "")
                }
                is StoreState.OrderInfoSate -> {
                    autoComplete(it.orderInfo)
                }
                is StoreState.EmployeesInfo -> {
                    it.employeesInfo.roles?.find { item -> item.location?.id == 1 && item.role?.id == 11 }?.user?.id.let { item ->
                        if (item != null && item != "") {
                            loggedInUserCache.setAutoReceiverId(item)
                        }
                    }
                }
                is StoreState.UnavailableToPrintInfo -> {

                }
                is StoreState.GetPrintQueue -> {
                    it.getPrintQueueInfo?.firstOrNull()?.let { item ->
                        if (item.orderId != null && !bohPrinterHelper.isPrinterConnected()) {
                             storeViewModel.loadOrderDetailsItem(item.orderId)
                        }
                    }
                }

                is StoreState.OrderDetailItemResponse -> {
                    if((!it.orderDetails.guest.isNullOrEmpty() || it.orderDetails.user != null) && !bohPrinterHelper.isPrinterConnected()) {
                        storeViewModel.updatePrintStatus(
                            UpdatePrintStatusRequest(
                                orderId = it.orderDetails.id,
                                serialNumber = if (isDebugMode()) Constants.DEVICE_ID else androidId,
                            )
                        )
                        it.orderDetails.id?.let { it1 -> storeViewModel.updateOrderStatusDetails(ORDER_STATUS_RECEIVE, it1) }
                        val t: Thread = object : Thread() {
                            override fun run() {
                                autoReceive(it.orderDetails)
                            }
                        }
                        t.start()
                    }
                }
                else -> {

                }
            }
        }) {
            showToast(it.toString())
        }.autoDispose()
    }

    private fun autoReceive(orderInfo: OrderDetailsResponse) {
        val textData = SpannableStringBuilder("")
        if (orderInfo.guest?.isEmpty() == true) {
            if (orderInfo.user?.fullName()?.isNotEmpty() == true) {
                textData.append("\n${orderInfo.user.fullName()}")
            }
        } else {
            if (orderInfo.guest?.firstOrNull()?.fullName()?.isNotEmpty() == true) {
                textData.append("\n${orderInfo.guest.firstOrNull()?.fullName()}")
            }
        }
        textData.append("\n\n")
        if (!orderInfo.orderPromisedTime?.trim().isNullOrEmpty()) {
            textData.append("\n")
            textData.append(
                spaceBetweenProductAndPrice(
                    orderInfo.orderPromisedTime?.toDate("yyyy-MM-dd HH:mm a")?.formatTo("MMMM dd, yyyy").toString(),
                    orderInfo.orderPromisedTime?.toDate("yyyy-MM-dd hh:mm a")?.formatTo("hh:mm:ss a").toString()
                )
            )
        }
        textData.append("\n------------------------------------------\n")
        if (orderInfo.id != 0 && orderInfo.id != null) {
            textData.append(spaceBetweenProductAndPrice(Constants.RECEIPT, "#${orderInfo.id}"))
        }
        if (orderInfo.orderMode?.modeName?.isNotEmpty() == true && orderInfo.orderType?.subcategory?.isNotEmpty() == true) {
            textData.append("\n")
            textData.append("${orderInfo.orderMode.modeName} - ${orderInfo.orderType.subcategory}")
        }
        textData.append("\n------------------------------------------\n")
        if (orderInfo.cartGroup?.cart?.isNotEmpty() == true) {
            orderInfo.cartGroup.cart.forEach {
                if (it.menuItemQuantity != null && !it.menu?.product?.productName.isNullOrEmpty()) {
                    textData.append("\n  ${it.menuItemQuantity} x ${it.menu?.product?.productName} \n")
                }
                if (!it.menuItemModifiers.isNullOrEmpty()) {
                    it.menuItemModifiers.forEach { item ->
                        item.options?.forEach { item1 ->
                            if (!item1.optionName.isNullOrEmpty()) {
                                textData.append("\t${item1.optionName}\n")
                            }

                        }
                    }

                }
                it.menuItemInstructions?.trim()?.let { it ->
                    if(it.isNotEmpty()){
                        textData.append("Note :$it")
                    }
                }
            }
        }
        Timber.tag("OkHttpClient").i(textData.toString())
        bohPrintAddress = loggedInUserCache.getLocationInfo()?.bohPrintAddress
        if (!bohPrinterHelper.isPrinterConnected() && bohPrintAddress != null) {
            try {
                val isConnected = bohPrinterHelper.printerConnect(bohPrintAddress)
                Timber.tag("AutoReceive").e("Printer connection response ========= $isConnected")
            } catch (e: java.lang.Exception) {
                Timber.tag("AutoReceive").e(e)
            }
        }
        if (bohPrintAddress != null && bohPrinterHelper.isPrinterConnected()) {
            try {
                bohPrinterHelper.runPrintBOHReceiptSequence(orderInfo, bohPrintAddress)
            } catch (e: java.lang.Exception) {
                Timber.tag("runPrintBOHReceiptSequence").e(e)
            }
        } else {
            Timber.tag("AutoReceive").e("----------------- Printer not connected -----------------")
        }
    }

    private fun autoComplete(orderInfo: List<OrdersInfo>) {
        val timeZone = loggedInUserCache.getStoreResponse()?.locationLocationTimezone ?: "America/Los_Angeles"
        orderInfo.forEach {
            it.status = it.status?.sortedByDescending { item -> item.id }
            if (it.status?.firstOrNull()?.orderStatus?.toLowerCase() == "received" && it.orderType?.isDelivery == false && it.orderType.subcategory != "Delivery") {
                val timezoneHelper = TimezoneHelper(timeZone)
                val timestamp = it.status?.firstOrNull()?.timestamp ?: ""
                val timeDifference = timezoneHelper.getTimeDifference(timestamp)
                if (timeDifference >= 20) {
                    it.id?.let { it1 -> storeViewModel.updateOrderStatusDetails(ORDER_STATUS_COMPLETED, it1) }
                }
            }
        }
    }

    private fun spaceBetweenProductAndPrice(product: String, price: String): String {
        val l = "${product}${price}".length;
        if (l < 42) {
            val s = 42 - l;

            var space: String = ""
            for (i in 1..s) {
                space = "$space ";
            }
            return product.plus(space).plus(price)
        } else {
            val s = 42 - price.length;

            var space: String = ""
            for (i in 1..s) {
                space = "$space ";
            }
            return product.plus("\n$space").plus(price)
        }
    }

    private fun manageSelection(selectedMenuViewId: Int) {
        removeSelectionForLastSelection()
        setLastSelectedMenu(selectedMenuViewId)
        when (selectedMenuViewId) {
            R.id.orderMenuView -> {
                binding.orderMenuView.isMenuSelected = true
            }
            R.id.deliveriesMenuView -> {
                binding.deliveriesMenuView.isMenuSelected = true
            }
            R.id.orderMenuMenuView -> {
                binding.orderMenuMenuView.isMenuSelected = true
            }
            R.id.timeManagementMenuView -> {
                binding.timeManagementMenuView.isMenuSelected = true
            }
            R.id.storeMenuView -> {
                binding.storeMenuView.isMenuSelected = true
            }
            R.id.LoyaltyMenuView -> {
                binding.LoyaltyMenuView.isMenuSelected = true
            }
            R.id.settingMenuView -> {
                binding.settingMenuView.isMenuSelected = true
            }
            R.id.giftCardMenuView -> {
                binding.giftCardMenuView.isMenuSelected = true
            }
        }
    }

    private fun setStoreOpenAndClose(storeResponse: StoreResponse) {
        val timeZone = storeResponse.locationLocationTimezone ?: "America/Los_Angeles"
        binding.liveTimeTextClock.timeZone = loggedInUserCache.getStoreResponse()?.locationLocationTimezone ?: "America/Los_Angeles"
        val c = Calendar.getInstance()
        val dayOfWeek = c[Calendar.DAY_OF_WEEK]
        val currentTime = getCurrentsStoreTime(timeZone).formatToStoreTime("HH:mm:ss")
        when (dayOfWeek - 1) {
            0 -> {
                val isSundayClosed = storeResponse.isSundayClosed == 1
                binding.openStoreTimeLinearLayout.isSelected = false
                binding.tvOpenAndClose.isSelected = false
                binding.tvOpenAndClose.text = resources.getText(if (isSundayClosed) R.string.close else R.string.open)
                if (!isSundayClosed) {
                    val openTime = storeResponse.sundayOpenTime?.toDate("HH:mm:ss")?.formatTo("HH:mm:ss")
                    val closeTime = storeResponse.sundayCloseTime?.toDate("HH:mm:ss")?.formatTo("HH:mm:ss")
                    if (openTime != null && closeTime != null) {
                        val isOpen = openTime < currentTime && closeTime > currentTime
                        binding.openStoreTimeLinearLayout.isSelected = isOpen
                        binding.tvOpenAndClose.isSelected = isOpen
                        binding.tvOpenAndClose.text = resources.getText(if (isOpen) R.string.open else R.string.close)
                    }
                }
            }
            1 -> {
                val isMondayClosed = storeResponse.isMondayClosed == 1
                binding.openStoreTimeLinearLayout.isSelected = false
                binding.tvOpenAndClose.isSelected = false
                binding.tvOpenAndClose.text = resources.getText(if (isMondayClosed) R.string.close else R.string.open)

                if (!isMondayClosed) {
                    val openTime = storeResponse.mondayOpenTime?.toDate("HH:mm:ss")?.formatTo("HH:mm:ss")
                    val closeTime = storeResponse.mondayCloseTime?.toDate("HH:mm:ss")?.formatTo("HH:mm:ss")

                    if (openTime != null && closeTime != null) {
                        val isOpen = openTime < currentTime && closeTime > currentTime

                        binding.openStoreTimeLinearLayout.isSelected = isOpen
                        binding.tvOpenAndClose.isSelected = isOpen
                        binding.tvOpenAndClose.text = resources.getText(if (isOpen) R.string.open else R.string.close)
                    }
                }
            }
            2 -> {
                val isTuesdayClosed = storeResponse.isTuesdayClosed == 1
                binding.openStoreTimeLinearLayout.isSelected = false
                binding.tvOpenAndClose.isSelected = false
                binding.tvOpenAndClose.text = resources.getText(if (isTuesdayClosed) R.string.close else R.string.open)

                if (!isTuesdayClosed) {
                    val openTime = storeResponse.tuesdayOpenTime?.toDate("HH:mm:ss")?.formatTo("HH:mm:ss")
                    val closeTime = storeResponse.tuesdayCloseTime?.toDate("HH:mm:ss")?.formatTo("HH:mm:ss")

                    if (openTime != null && closeTime != null) {
                        val isOpen = openTime < currentTime && closeTime > currentTime

                        binding.openStoreTimeLinearLayout.isSelected = isOpen
                        binding.tvOpenAndClose.isSelected = isOpen
                        binding.tvOpenAndClose.text = resources.getText(if (isOpen) R.string.open else R.string.close)
                    }
                }
            }
            3 -> {
                val isWednesdayClosed = storeResponse.isWednesdayClosed == 1
                binding.openStoreTimeLinearLayout.isSelected = false
                binding.tvOpenAndClose.isSelected = false
                binding.tvOpenAndClose.text = resources.getText(if (isWednesdayClosed) R.string.close else R.string.open)

                if (!isWednesdayClosed) {
                    val openTime = storeResponse.wednesdayOpenTime?.toDate("HH:mm:ss")?.formatTo("HH:mm:ss")
                    val closeTime = storeResponse.wednesdayCloseTime?.toDate("HH:mm:ss")?.formatTo("HH:mm:ss")

                    if (openTime != null && closeTime != null) {
                        val isOpen = openTime < currentTime && closeTime > currentTime

                        binding.openStoreTimeLinearLayout.isSelected = isOpen
                        binding.tvOpenAndClose.isSelected = isOpen
                        binding.tvOpenAndClose.text = resources.getText(if (isOpen) R.string.open else R.string.close)
                    }
                }
            }
            4 -> {
                val isThursdayClosed = storeResponse.isThursdayClosed == 1
                binding.openStoreTimeLinearLayout.isSelected = false
                binding.tvOpenAndClose.isSelected = false
                binding.tvOpenAndClose.text = resources.getText(if (isThursdayClosed) R.string.close else R.string.open)

                if (!isThursdayClosed) {
                    val openTime = storeResponse.thursdayOpenTime?.toDate("HH:mm:ss")?.formatTo("HH:mm:ss")
                    val closeTime = storeResponse.thursdayCloseTime?.toDate("HH:mm:ss")?.formatTo("HH:mm:ss")

                    if (openTime != null && closeTime != null) {
                        val isOpen = openTime < currentTime && closeTime > currentTime

                        binding.openStoreTimeLinearLayout.isSelected = isOpen
                        binding.tvOpenAndClose.isSelected = isOpen
                        binding.tvOpenAndClose.text = resources.getText(if (isOpen) R.string.open else R.string.close)
                    }
                }
            }
            5 -> {
                val isFridayClosed = storeResponse.isFridayClosed == 1
                binding.openStoreTimeLinearLayout.isSelected = false
                binding.tvOpenAndClose.isSelected = false
                binding.tvOpenAndClose.text = resources.getText(if (isFridayClosed) R.string.close else R.string.open)

                if (!isFridayClosed) {
                    val openTime = storeResponse.fridayOpenTime?.toDate("HH:mm:ss")?.formatTo("HH:mm:ss")
                    val closeTime = storeResponse.fridayCloseTime?.toDate("HH:mm:ss")?.formatTo("HH:mm:ss")

                    if (openTime != null && closeTime != null) {
                        val isOpen = openTime < currentTime && closeTime > currentTime

                        binding.openStoreTimeLinearLayout.isSelected = isOpen
                        binding.tvOpenAndClose.isSelected = isOpen
                        binding.tvOpenAndClose.text = resources.getText(if (isOpen) R.string.open else R.string.close)
                    }
                }
            }
            6 -> {
                val isSaturdayClosed = storeResponse.isSaturdayClosed == 1
                binding.openStoreTimeLinearLayout.isSelected = false
                binding.tvOpenAndClose.isSelected = false
                binding.tvOpenAndClose.text = resources.getText(if (isSaturdayClosed) R.string.close else R.string.open)

                if (!isSaturdayClosed) {
                    val openTime = storeResponse.saturdayOpenTime?.toDate("HH:mm:ss")?.formatTo("HH:mm:ss")
                    val closeTime = storeResponse.saturdayCloseTime?.toDate("HH:mm:ss")?.formatTo("HH:mm:ss")

                    if (openTime != null && closeTime != null) {
                        val isOpen = openTime < currentTime && closeTime > currentTime

                        binding.openStoreTimeLinearLayout.isSelected = isOpen
                        binding.tvOpenAndClose.isSelected = isOpen
                        binding.tvOpenAndClose.text = resources.getText(if (isOpen) R.string.open else R.string.close)
                    }
                }
            }
        }
    }

    private fun setLastSelectedMenu(selectedMenuViewId: Int) {
        lastSelectedMenuId = selectedMenuViewId
    }

    private fun openLogOutDialog() {
        val locationId = loggedInUserCache.getLocationInfo()?.location?.id ?: throw Exception("location not found")
        val alertDialog =
            AlertDialog.Builder(this).setTitle(resources.getText(R.string.logout)).setMessage(resources.getText(R.string.are_you_sure_log_out))
                .setNegativeButton(resources.getText(R.string.label_cancel)) { dialog, _ ->
                    dialog.dismiss()
                }.setPositiveButton(resources.getText(R.string.label_ok)) { _, _ ->
                    runOnUiThread {
                        val availableToPrintRequest = AvailableToPrintRequest(
                            serialNumber = if (isDebugMode()) Constants.DEVICE_ID else androidId,
                            locationId = locationId,
                            status = Constants.UNAVAILABLE_TO_PRINT_STATUS
                        )
                        storeViewModel.unavailableToPrint(availableToPrintRequest)
                        loggedInUserCache.clearLoggedInUserLocalPrefs()
                        finish()
                    }
                }.show()
        alertDialog.window?.setLayout(800, WindowManager.LayoutParams.WRAP_CONTENT)
    }

    private fun removeSelectionForLastSelection() {
        when (lastSelectedMenuId) {
            R.id.orderMenuView -> {
                binding.orderMenuView.isMenuSelected = false
            }
            R.id.deliveriesMenuView -> {
                binding.deliveriesMenuView.isMenuSelected = false
            }
            R.id.orderMenuMenuView -> {
                binding.orderMenuMenuView.isMenuSelected = false
            }
            R.id.timeManagementMenuView -> {
                binding.timeManagementMenuView.isMenuSelected = false
            }
            R.id.storeMenuView -> {
                binding.storeMenuView.isMenuSelected = false
            }
            R.id.LoyaltyMenuView -> {
                binding.LoyaltyMenuView.isMenuSelected = false
            }
            R.id.settingMenuView -> {
                binding.settingMenuView.isMenuSelected = false
            }
            R.id.giftCardMenuView -> {
                binding.giftCardMenuView.isMenuSelected = false
            }
        }
    }

    override fun onResume() {
        super.onResume()
        storeViewModel.loadCurrentStoreResponse()
        storeViewModel.getEmployee()
        resetDisconnectTimer()
    }

    override fun onBackPressed() {
        openLogOutDialog()
    }


    override fun onUserInteraction() {
        binding.toolbarRelativeLayout.alpha = 1F
        binding.bottomLayout.alpha = 1F
        binding.toolbarDivider.alpha = 1F
        binding.logOutTextView.isVisible = false
        Timber.tag("MainActivity").d("onUserInteraction")
        resetDisconnectTimer()
    }

    private val disconnectHandler = Handler {
        Timber.tag("MainActivity").d("disconnectHandler")
        false
    }

    private val showToastHandler = Handler {
        Timber.tag("SessionTimeOutActivity").d("showToastHandler")
        false
    }

    private val disconnectCallback = Runnable { // Perform any required operation on disconnect
        Timber.tag("MainActivity").d("disconnectCallback")
        loggedInUserCache.clearLoggedInUserLocalPrefs()
        Toast.makeText(applicationContext, "Crew time out", Toast.LENGTH_LONG).show()
        startNewActivityWithDefaultAnimation(
            LoginActivity.getIntent(
                this@MainActivity, loggedInUserCache.getLocationInfo()?.location?.id
            )
        )
        finish()
    }

    private val showToastCallback = Runnable { // Perform any required operation on disconnect
        Timber.tag("MainActivity").d("showToastCallback")
        if (loggedInUserCache.isUserLoggedIn()) {
            binding.toolbarRelativeLayout.alpha = 0.2F
            binding.bottomLayout.alpha = 0.2F
            binding.toolbarDivider.alpha = 0.2F
            binding.logOutTextView.isVisible = true
        }
    }

    private fun resetDisconnectTimer() {
        Timber.tag("MainActivity").d("resetDisconnectTimer")
        disconnectHandler.removeCallbacks(disconnectCallback)
        showToastHandler.removeCallbacks(showToastCallback)
        disconnectHandler.postDelayed(disconnectCallback, DISCONNECT_TIMEOUT)
        showToastHandler.postDelayed(showToastCallback, SHOW_TOAST_TIMER)
    }

    private fun stopDisconnectTimer() {
        disconnectHandler.removeCallbacks(disconnectCallback)
        showToastHandler.removeCallbacks(showToastCallback)
    }

    override fun onPause() {
        super.onPause()
        Timber.tag("MainActivity").d("stopDisconnectTimer")
        stopDisconnectTimer()
    }

    override fun onDestroy() {
        storeViewModel.closeObserver()
        super.onDestroy()
    }
}