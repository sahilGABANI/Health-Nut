package com.hotbox.terminal.ui.userstore

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.provider.Settings
import android.view.View
import android.view.ViewGroup.GONE
import android.view.WindowManager
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.hotbox.terminal.BuildConfig
import com.hotbox.terminal.R
import com.hotbox.terminal.api.authentication.LoggedInUserCache
import com.hotbox.terminal.api.menu.model.MenuListInfo
import com.hotbox.terminal.api.menu.model.ProductsItem
import com.hotbox.terminal.api.store.model.StoreResponse
import com.hotbox.terminal.api.userstore.model.*
import com.hotbox.terminal.application.HotBoxApplication
import com.hotbox.terminal.base.BaseActivity
import com.hotbox.terminal.base.RxBus
import com.hotbox.terminal.base.RxEvent
import com.hotbox.terminal.base.ViewModelFactory
import com.hotbox.terminal.base.extension.*
import com.hotbox.terminal.databinding.ActivityUserStoreBinding
import com.hotbox.terminal.helper.formatTo
import com.hotbox.terminal.helper.toDate
import com.hotbox.terminal.ui.login.LoginActivity
import com.hotbox.terminal.ui.userstore.cookies.CookiesFragment
import com.hotbox.terminal.ui.userstore.cookies.RedeemProductFragment
import com.hotbox.terminal.ui.userstore.editcart.EditCartActivity
import com.hotbox.terminal.ui.userstore.view.CartAdapter
import com.hotbox.terminal.ui.userstore.view.CategoryAdapter
import com.hotbox.terminal.ui.userstore.view.SideNavigationAdapterUserStore
import com.hotbox.terminal.ui.userstore.viewmodel.UserStoreState
import com.hotbox.terminal.ui.userstore.viewmodel.UserStoreViewModel
import com.hotbox.terminal.utils.Constants
import timber.log.Timber
import java.util.*
import javax.inject.Inject
import androidx.core.content.ContextCompat
import com.hotbox.terminal.api.menu.model.MenusItem
import com.hotbox.terminal.helper.formatToStoreTime
import com.hotbox.terminal.helper.getCurrentsStoreTime
import com.hotbox.terminal.ui.userstore.editcart.EditCartFragment
import com.hotbox.terminal.utils.Constants.ABANDONED
import com.hotbox.terminal.utils.Constants.COMP_ORDER_TEXT
import com.hotbox.terminal.utils.Constants.ORDER_TYPE_ID_DINE_IN
import com.hotbox.terminal.utils.Constants.ORDER_TYPE_ID_EMPLOYEE_MEAL
import com.hotbox.terminal.utils.Constants.ORDER_TYPE_ID_TO_GO
import kotlin.math.abs

class UserStoreActivity : BaseActivity() {

    private var orderSubTotal: Double = 0.0
    private var isGuest: Boolean = false
    private var orderTax: Double = 0.0
    private var orderSubTotalCount: Double = 0.00
    private var orderTotal: Double? = 0.0

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<UserStoreViewModel>
    private lateinit var userStoreViewModel: UserStoreViewModel
    private lateinit var androidId: String
    private var versionName: String = BuildConfig.VERSION_NAME

    private var disconnectTimeOut: Long = 14400000 // 240 min = 240 * 60 * 1000 ms
    private var showToastTime: Long = 12600000 // 210 min = 5 * 60 * 1000 ms
//    val disconnectTimeOut: Long = 120000 // 5 min = 5 * 60 * 1000 ms
//    val disconnectTimeOut = (1000 * 60).toLong()


    private lateinit var cartAdapter: CartAdapter
    private lateinit var categoryAdapter: CategoryAdapter
    private lateinit var deletedCartItemId: CartItem
    private var listOfProductDetails: ArrayList<CartItem>? = null
    private var listOfProduct: ArrayList<ProductsItem>? = null
    private var listOfMenu: ArrayList<MenusItem>? = null
    private var giftCardAmount: Double = 0.00
    private var giftCardId: String? = null
    private var promoCodeAmount: Double = 0.00
    private var userCredit: Double = 0.00
    private var employeeDiscount: Double = 0.00
    private var adjustmentDiscount: Double = 0.00
    private var isRedeemPointScreen: Boolean = false
    private var createOrder: Boolean = false

    companion object {
        const val IS_GUEST = "IS_GUEST"
        fun getIntent(context: Context, isGuest: Boolean): Intent {
            val intent = Intent(context, UserStoreActivity::class.java)
            intent.putExtra(IS_GUEST, isGuest)
            return intent
        }
    }

    private lateinit var binding: ActivityUserStoreBinding
    private lateinit var sideNavigationAdapterUserStore: SideNavigationAdapterUserStore
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserStoreBinding.inflate(layoutInflater)
        HotBoxApplication.component.inject(this)
        userStoreViewModel = getViewModelFromFactory(viewModelFactory)
        setContentView(binding.root)
        window.decorView.apply {
            systemUiVisibility = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_FULLSCREEN
        }
        userStoreViewModel.getMenuProductByLocation()
        listenToViewModel()
        initUI()
    }

    @SuppressLint("HardwareIds", "SetTextI18n")
    private fun initUI() {
        initAdapter()
        androidId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID).toString()
        intent.apply {
            isGuest = getBooleanExtra(IS_GUEST, false)
        }

        if (!loggedInUserCache.isUserLoggedIn()) {
            disconnectTimeOut = 300000 // 5 min = 5 * 60 * 1000 ms
            showToastTime = 270000
            onUserInteraction()
            binding.rlEmployeeDetails.visibility = View.GONE
            binding.rlGuest.visibility = View.VISIBLE
        } else {
            binding.rlEmployeeDetails.visibility = View.VISIBLE
            binding.rlGuest.visibility = View.GONE
            binding.employeeNameAppCompatTextView.text = loggedInUserCache.getLoggedInUserFullName()
            binding.loggedInUserRoleTextView.text = loggedInUserCache.getLoggedInUserRole()
        }
        when (loggedInUserCache.getorderTypeId()) {
            ORDER_TYPE_ID_DINE_IN -> {
                binding.orderTypeTextView.text = Constants.ORDER_TYPE_DINE_IN

            }
            ORDER_TYPE_ID_TO_GO -> {
                binding.orderTypeTextView.text = Constants.ORDER_TYPE_TO_GO
            }
            ORDER_TYPE_ID_EMPLOYEE_MEAL -> {
                binding.orderTypeTextView.text = Constants.ORDER_TYPE_EMPLOYEE_MEAL
                binding.orderTypeTextView.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, 0, 0)
            }
            else -> {

            }
        }
        if (loggedInUserCache.getIsEmployeeMeal() == true) {
            binding.cartView.rlPoint.isVisible = false
            binding.cartView.viewPointBelow.isVisible = false
            binding.tvLoyaltyName.isVisible = true
            binding.tvLoyaltyName.text = loggedInUserCache.getLoyaltyQrResponse()?.fullName
            binding.cartView.flCartView.setBackgroundColor(ContextCompat.getColor(this@UserStoreActivity, R.color.green_light_50))
            if (binding.userStoreViewPager.currentItem == 0) {
                binding.userStoreViewPager.setBackgroundColor(ContextCompat.getColor(this@UserStoreActivity, R.color.green_light_50))
                binding.llViewPager.setBackgroundColor(ContextCompat.getColor(this@UserStoreActivity, R.color.green_light_50))
            }
        } else {
            if (loggedInUserCache.getLoyaltyQrResponse()?.fullName != "" && loggedInUserCache.getLoyaltyQrResponse()?.fullName != null) {
                binding.tvLoyaltyName.isVisible = true
                binding.tvLoyaltyPoint.isVisible = true
                binding.tvLoyaltyName.text = loggedInUserCache.getLoyaltyQrResponse()?.fullName
                binding.tvLoyaltyPoint.text =
                    resources.getString(R.string.leaves).plus(":").plus(loggedInUserCache.getLoyaltyQrResponse()?.points.toString())
            }
            binding.verticalViewMenu.isVisible = true
            binding.verticalViewRightMenu.isVisible = false
            binding.backVerticalView.isVisible = true
            binding.backViewVertical.isVisible = false
        }

        binding.cartView.cartListAndPrizeLinearLayout.isVisible = false
        binding.cartView.emptyMessageAppCompatTextView.isVisible = true
        sideNavigationAdapterUserStore = SideNavigationAdapterUserStore(this)
        binding.userStoreViewPager.apply {
            offscreenPageLimit = 1
            adapter = sideNavigationAdapterUserStore
            isUserInputEnabled = false
        }
        binding.userStoreViewPager.currentItem = 0
        binding.backLinearLayout.throttleClicks().subscribeAndObserveOnMainThread {
            when (binding.userStoreViewPager.currentItem) {
                1 -> {
                    if (loggedInUserCache.getIsEmployeeMeal() == true) {
                        binding.userStoreViewPager.setBackgroundColor(ContextCompat.getColor(this@UserStoreActivity, R.color.green_light_50))
                        binding.llViewPager.setBackgroundColor(ContextCompat.getColor(this@UserStoreActivity, R.color.green_light_50))
                    }
                    binding.userStoreViewPager.currentItem = 0
                    binding.cartView.rlCredit.isVisible = false
                    binding.cartView.rlGiftCard.isVisible = false
                    binding.cartView.rlPromocode.isVisible = false
                    binding.cartView.rlEmployeeDiscount.isVisible = false
                    binding.cartView.proceedToPaymentMaterialButton.isVisible = false
                    binding.cartView.proceedToCheckoutMaterialButton.isVisible = true
                    binding.tvCartName.text = resources.getText(R.string.cart)
                    userCredit = 0.0
                    giftCardAmount = 0.0
                    promoCodeAmount = 0.0
                    setCartData(cartAdapter.listOfProductDetails)
                    orderTotalCount()
                    categorySelection()
                }
                2 -> {
                    checkOutFragment()
                }
                else -> {
                    openLogOutDialog()
                }
            }
        }.autoDispose()
        RxBus.listen(RxEvent.EventCartGroupIdListen::class.java).subscribeAndObserveOnMainThread {
            userStoreViewModel.getCartDetails(it.cartGroupId)
        }.autoDispose()
        binding.locationAppCompatTextView.text = loggedInUserCache.getLocationInfo()?.location?.locationName ?: throw Exception("location not found")
        binding.versionNameAppCompatTextView.text = resources.getString(R.string.title_version, versionName)
        val androidId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
        binding.androidIdAppCompatTextView.text = resources.getString(R.string.title_device_id, androidId)
        binding.liveTimeTextClock.format12Hour = "hh:mm a"
        binding.cartView.proceedToCheckoutMaterialButton.throttleClicks().subscribeAndObserveOnMainThread {
            hideKeyboard()
            removeCategorySelection()
            val list = cartAdapter.listOfProductDetails
            list?.filter { it.isChanging == true }?.forEach {
                it.isChanging = false
            }
            cartAdapter.listOfProductDetails = list
            binding.tvCartName.text = resources.getString(R.string.your_order)
            binding.userStoreViewPager.currentItem = 1
            binding.cartView.proceedToCheckoutMaterialButton.isVisible = false
            binding.cartView.proceedToPaymentMaterialButton.isVisible = true
            if (orderTotal == 0.00) {
                binding.cartView.proceedToPaymentMaterialButton.isVisible = false
                binding.cartView.createOrderMaterialButton.isVisible = true
            }
            RxBus.publish(RxEvent.EventTotalCheckOut(OrderPrice(orderTotal.toConvertDecimalFormat(), orderSubTotalCount)))
        }.autoDispose()
        binding.cartView.proceedToPaymentMaterialButton.throttleClicks().subscribeAndObserveOnMainThread {
            paymentButtonVisibility(true)
            val list = cartAdapter.listOfProductDetails
            list?.filter { it.isChanging == true }?.forEach {
                it.isChanging = false
            }
            cartAdapter.listOfProductDetails = list
            hideKeyboard()
            RxBus.publish(RxEvent.EventCheckValidation)
        }.autoDispose()
        binding.cartView.adjustmentsMaterialButton.throttleClicks().subscribeAndObserveOnMainThread {
            if (loggedInUserCache.getLoggedInUser()?.crewResponse?.isAdminPin == true) {
                startActivityWithDefaultAnimation(
                    CompAndAdjustmentActivity.getIntent(
                        this@UserStoreActivity, employeeDiscount, adjustmentDiscount, giftCardAmount, promoCodeAmount
                    )
                )
            } else {
                val adminPinDialogFragment = AdminPinDialogFragment().apply {
                    adminPinSuccess.subscribeAndObserveOnMainThread {
                        startActivityWithDefaultAnimation(
                            CompAndAdjustmentActivity.getIntent(
                                this@UserStoreActivity, employeeDiscount, adjustmentDiscount, giftCardAmount, promoCodeAmount
                            )
                        )
                    }.autoDispose()
                }
                adminPinDialogFragment.show(supportFragmentManager, AdminPinDialogFragment::class.java.name)
            }
        }.autoDispose()
        binding.cartView.createOrderMaterialButton.throttleClicks().subscribeAndObserveOnMainThread {
            hideKeyboard()
            val list = cartAdapter.listOfProductDetails
            list?.filter { it.isChanging == true }?.forEach {
                it.isChanging = false
            }
            cartAdapter.listOfProductDetails = list
            if (loggedInUserCache.getIsEmployeeMeal() == true) {
                userStoreViewModel.createOrder(
                    CreateOrderRequest(
//                            orderUserId = if (loggedInUserCache.getLoyaltyQrResponse()?.id != null) loggedInUserCache.getLoyaltyQrResponse()?.id else null,
                        userId = if (loggedInUserCache.getLoyaltyQrResponse()?.id != null && loggedInUserCache.getLoyaltyQrResponse()?.id != "") loggedInUserCache.getLoyaltyQrResponse()?.id else null,
                        Constants.MODE_ID,
                        orderTip = 0,
                        orderTypeId = loggedInUserCache.getorderTypeId(),
                        orderCartGroupId = loggedInUserCache.getLoggedInUserCartGroupId(),
                        orderLocationId = loggedInUserCache.getLocationInfo()?.location?.id,
//                        transactionId = "Transaction Comp--Cart ID: [".plus("${loggedInUserCache.getLoggedInUserCartGroupId()}").plus("]"),
                        deliveryAddress = null,
                        guestFirstName = loggedInUserCache.getLoyaltyQrResponse()?.fullName,
                        guestLastName = if (loggedInUserCache.getLoyaltyQrResponse()?.lastName?.isNotEmpty() == true) loggedInUserCache.getLoyaltyQrResponse()?.lastName else null,
                        guestPhone = (if (loggedInUserCache.getLoyaltyQrResponse()?.phone?.isNotEmpty() == true) loggedInUserCache.getLoyaltyQrResponse()?.phone.toString() else "N/A"),
                        guestEmail = if (loggedInUserCache.getLoyaltyQrResponse()?.email?.isNotEmpty() == true) loggedInUserCache.getLoyaltyQrResponse()?.email.toString() else null,
                        transactionIdOfProcessor = "Transaction Comp--Cart ID: [".plus("${loggedInUserCache.getLoggedInUserCartGroupId()}").plus("]"),
                        transactionChargeId = COMP_ORDER_TEXT,
                        transactionTerminal = COMP_ORDER_TEXT,
                        orderTotal = 0,
                        orderSubtotal = if(employeeDiscount.toInt() != 0) employeeDiscount.toInt() else null,
                        transactionTotalAmount = 0,
                        giftCardId = null,
                        orderGiftCardAmount = null,
                        orderAdjustmentAmount = if (adjustmentDiscount.toInt() == 0) null else adjustmentDiscount.toInt()
//                                orderPromisedTime = promisedTime
                    )
                )
            } else {
                if (loggedInUserCache.getLoyaltyQrResponse()?.id.isNullOrEmpty()) {
                    RxBus.publish(RxEvent.EventValidation)
                    createOrder = true
                } else {
                    userStoreViewModel.createOrder(
                        CreateOrderRequest(
                            userId = if (loggedInUserCache.getLoyaltyQrResponse()?.id != null && loggedInUserCache.getLoyaltyQrResponse()?.id != "") loggedInUserCache.getLoyaltyQrResponse()?.id else null,
                            Constants.MODE_ID,
                            orderTip = 0,
                            orderTypeId = loggedInUserCache.getorderTypeId(),
                            orderCartGroupId = loggedInUserCache.getLoggedInUserCartGroupId(),
                            orderLocationId = loggedInUserCache.getLocationInfo()?.location?.id,
//                            transactionId = "Transaction Comp--Cart ID: [".plus("${loggedInUserCache.getLoggedInUserCartGroupId()}").plus("]"),
                            deliveryAddress = null,
                            guestFirstName = loggedInUserCache.getLoyaltyQrResponse()?.fullName,
                            guestLastName = if (loggedInUserCache.getLoyaltyQrResponse()?.lastName?.isNotEmpty() == true) loggedInUserCache.getLoyaltyQrResponse()?.lastName else null,
                            guestPhone = if (loggedInUserCache.getLoyaltyQrResponse()?.phone?.isNotEmpty() == true) loggedInUserCache.getLoyaltyQrResponse()?.phone.toString() else "N/A",
                            guestEmail = if (loggedInUserCache.getLoyaltyQrResponse()?.email?.isNotEmpty() == true) loggedInUserCache.getLoyaltyQrResponse()?.email.toString() else null,
                            transactionIdOfProcessor = "Transaction Comp--Cart ID: [".plus("${loggedInUserCache.getLoggedInUserCartGroupId()}")
                                .plus("]"),
                            transactionChargeId = COMP_ORDER_TEXT,
                            transactionTerminal = COMP_ORDER_TEXT,
                            orderSubtotal =  if(employeeDiscount.toInt() != 0) employeeDiscount.toInt() else null,
                            orderTotal = 0,
                            transactionTotalAmount = 0,
                            giftCardId = giftCardId,
                            orderGiftCardAmount = if (giftCardAmount.toInt() == 0) null else giftCardAmount.toInt(),
                            orderAdjustmentAmount = if (adjustmentDiscount.toInt() == 0) null else adjustmentDiscount.toInt()
                        )
                    )
                    createOrder = true
                }
            }
        }.autoDispose()
        binding.orderTypeTextView.throttleClicks().subscribeAndObserveOnMainThread {
            if (loggedInUserCache.getIsEmployeeMeal() == false) {
                val orderTypeBottomSheet = OrderTypeBottomSheet.newInstance().apply {
                    orderTypeClick.subscribeAndObserveOnMainThread {
                        when (it) {
                            ORDER_TYPE_ID_DINE_IN -> {
                                this.dismiss()
                                binding.orderTypeTextView.text = Constants.ORDER_TYPE_DINE_IN
                                loggedInUserCache.setIsEmployeeMeal(false)
                                loggedInUserCache.setorderTypeId(ORDER_TYPE_ID_DINE_IN)
                                loggedInUserCache.setemployeeUserIdEmployeeMeal(null)
                            }
                            ORDER_TYPE_ID_TO_GO -> {
                                this.dismiss()
                                binding.orderTypeTextView.text = Constants.ORDER_TYPE_TO_GO
                                loggedInUserCache.setIsEmployeeMeal(false)
                                loggedInUserCache.setorderTypeId(ORDER_TYPE_ID_TO_GO)
                                loggedInUserCache.setemployeeUserIdEmployeeMeal(null)
                            }
                            else -> {
                                dismiss()
                            }
                        }
                    }.autoDispose()
                }
                orderTypeBottomSheet.show(supportFragmentManager, UserStoreActivity::class.java.name)
            }
        }.autoDispose()
        RxBus.listen(RxEvent.EventGoToBack::class.java).subscribeAndObserveOnMainThread {
            if (createOrder) {
                userStoreViewModel.createOrder(
                    CreateOrderRequest(
                        userId = if (loggedInUserCache.getLoyaltyQrResponse()?.id != null && loggedInUserCache.getLoyaltyQrResponse()?.id != "") loggedInUserCache.getLoyaltyQrResponse()?.id else null,
                        Constants.MODE_ID,
                        0,
                        orderTypeId = loggedInUserCache.getorderTypeId(),
                        orderCartGroupId = loggedInUserCache.getLoggedInUserCartGroupId(),
                        orderLocationId = loggedInUserCache.getLocationInfo()?.location?.id,
//                        transactionId = "Transaction Comp--Cart ID: [".plus("${loggedInUserCache.getLoggedInUserCartGroupId()}").plus("]"),
                        deliveryAddress = null,
                        guestFirstName = loggedInUserCache.getLoyaltyQrResponse()?.fullName,
                        guestLastName = if (loggedInUserCache.getLoyaltyQrResponse()?.lastName?.trim()
                                ?.isNotEmpty() == true
                        ) loggedInUserCache.getLoyaltyQrResponse()?.lastName else null,
                        guestEmail = if (loggedInUserCache.getLoyaltyQrResponse()?.email?.trim()
                                ?.isNotEmpty() == true
                        ) loggedInUserCache.getLoyaltyQrResponse()?.email else null,
                        guestPhone = if (loggedInUserCache.getLoyaltyQrResponse()?.phone?.trim()
                                ?.isNotEmpty() == true
                        ) loggedInUserCache.getLoyaltyQrResponse()?.phone else "N/A",
                        transactionIdOfProcessor = "Transaction Comp--Cart ID: [".plus("${loggedInUserCache.getLoggedInUserCartGroupId()}").plus("]"),
                        transactionChargeId = COMP_ORDER_TEXT,
                        transactionTerminal = COMP_ORDER_TEXT,
                        orderTotal = 0,
                        transactionTotalAmount = 0,
                        orderSubtotal = if(employeeDiscount.toInt() != 0) employeeDiscount.toInt() else null,
                        giftCardId = giftCardId,
                        orderGiftCardAmount = if (giftCardAmount.toInt() == 0) null else giftCardAmount.toInt(),
                        orderAdjustmentAmount = if (adjustmentDiscount.toInt() == 0) null else adjustmentDiscount.toInt()
//                                orderPromisedTime = promisedTime
                    )
                )
                createOrder = false
            }
        }.autoDispose()
        RxBus.listen(RxEvent.EventGoToPaymentScreen::class.java).subscribeAndObserveOnMainThread {
            if (it.data) {
                paymentButtonVisibility(false)
                removeCategorySelection()
                cartItemNotChangeNow()
                binding.userStoreViewPager.currentItem = 3
                binding.cartView.proceedToPaymentMaterialButton.isVisible = false
                binding.tvCartName.text = resources.getText(R.string.order_details)
                binding.cartView.customerDetails.isVisible = true
                binding.cartView.tvOrderHeading.isVisible = true
                binding.cartView.rlTax.isVisible = false
                binding.cartView.rlOrderPrice.isVisible = false
                binding.cartView.rlGiftCard.isVisible = false
                binding.cartView.rlPromocode.isVisible = false
                binding.cartView.rlEmployeeDiscount.isVisible = false
                binding.cartView.rlAdjustment.isVisible = false
                binding.cartView.rlTotal.isVisible = false
                binding.cartView.rlPoint.isVisible = false
                binding.cartView.viewPointBelow.isVisible = false
                binding.tvTotalPrizeNumber.isVisible = false
                binding.cartView.viewOrder.isVisible = true
                binding.cartView.adjustmentsMaterialButton.isVisible = false
                binding.cartView.customerNameAppCompatTextView.text =
                    loggedInUserCache.getLoyaltyQrResponse()?.fullName.plus(" ").plus(loggedInUserCache.getLoyaltyQrResponse()?.lastName)
                if (loggedInUserCache.getLoyaltyQrResponse()?.email != "") {
                    binding.cartView.customerEmailAppCompatTextView.text = loggedInUserCache.getLoyaltyQrResponse()?.email
                }
                if (loggedInUserCache.getLoyaltyQrResponse()?.phone != "") {
                    binding.cartView.customerPhoneNumberAppCompatTextView.text = loggedInUserCache.getLoyaltyQrResponse()?.phone
                }
                RxBus.publish(RxEvent.EventTotalPayment(OrderPrice(orderTotal.toConvertDecimalFormat(), employeeDiscount = employeeDiscount.toInt(),adjustmentDiscount = adjustmentDiscount.toInt())))
            }
        }.autoDispose()
        RxBus.listen(RxEvent.EventPaymentButtonEnabled::class.java).subscribeAndObserveOnMainThread {
            binding.cartView.proceedToPaymentMaterialButton.isEnabled = it.enable
        }.autoDispose()
        RxBus.listen(RxEvent.EventCreateOrderMaterialButton::class.java).subscribeAndObserveOnMainThread {
            binding.cartView.createOrderMaterialButton.isEnabled = it.enable
        }.autoDispose()
        RxBus.listen(RxEvent.AddGiftCart::class.java).subscribeAndObserveOnMainThread {
            binding.cartView.rlGiftCard.isVisible = true
            giftCardAmount = it.giftCardAmount
            giftCardId = it.giftCardId
            orderTotal = orderTotal?.minus(it.giftCardAmount.div(100))
            orderTotal = orderTotal?.let { it1 -> kotlin.math.abs(it1) }
            binding.cartView.tvCardAndBowCharge.text = "-${it.giftCardAmount.div(100).toDollar()}"
            binding.cartView.tvTotalPrizeNumber.text = orderTotal?.toConvertDecimalFormat().toDollar()
            binding.cartView.tvTotalPrizeNumber.text = orderTotal?.toConvertDecimalFormat().toDollar()
            RxBus.publish(RxEvent.PassTotal(orderTotal.toConvertDecimalFormat()))
            if (orderTotal?.toConvertDecimalFormat() == 0.00) {
                binding.cartView.proceedToPaymentMaterialButton.isVisible = false
                binding.cartView.adjustmentsMaterialButton.isVisible = false
                binding.cartView.createOrderMaterialButton.isVisible = true
                binding.cartView.createOrderMaterialButton.isEnabled = true
            } else {
                binding.cartView.proceedToPaymentMaterialButton.isVisible = true
                binding.cartView.createOrderMaterialButton.isVisible = false
            }
        }.autoDispose()
        RxBus.listen(RxEvent.AddPromoCode::class.java).subscribeAndObserveOnMainThread {
            binding.cartView.rlPromocode.isVisible = true
            promoCodeAmount = it.promocodeAmount
            orderTotal = orderTotal?.minus(it.promocodeAmount.div(100))
            orderTotal = orderTotal?.let { it1 -> kotlin.math.abs(it1) }
            binding.cartView.tvPromoCodeDiscount.text = "-${it.promocodeAmount.div(100).toDollar()}"
            binding.tvTotalPrizeNumber.text = orderTotal?.toConvertDecimalFormat().toDollar()
            RxBus.publish(RxEvent.PassTotal(orderTotal.toConvertDecimalFormat()))
            if (orderTotal?.toConvertDecimalFormat() == 0.00) {
                binding.cartView.proceedToPaymentMaterialButton.isVisible = false
                binding.cartView.createOrderMaterialButton.isVisible = true
                binding.cartView.createOrderMaterialButton.isEnabled = true
                binding.cartView.adjustmentsMaterialButton.isVisible = false
            } else {
                binding.cartView.proceedToPaymentMaterialButton.isVisible = true
                binding.cartView.createOrderMaterialButton.isVisible = false
            }
        }.autoDispose()
        RxBus.listen(RxEvent.RemoveGiftCart::class.java).subscribeAndObserveOnMainThread {
            binding.cartView.rlGiftCard.isVisible = false
            orderTotal = orderTotal?.plus(giftCardAmount.div(100))
            giftCardAmount = 0.00
            giftCardId = null
            binding.cartView.tvTotalPrizeNumber.text = orderTotal.toConvertDecimalFormat().toDollar()
            binding.tvTotalPrizeNumber.text = orderTotal.toConvertDecimalFormat().toDollar()
            RxBus.publish(RxEvent.PassTotal(orderTotal.toConvertDecimalFormat()))
            if (orderTotal.toConvertDecimalFormat() != 0.00) {
                binding.cartView.proceedToPaymentMaterialButton.isVisible = true
                binding.cartView.createOrderMaterialButton.isVisible = false
                if (loggedInUserCache.getIsEmployeeMeal() != true && loggedInUserCache.isUserLoggedIn()) {
                    binding.cartView.adjustmentsMaterialButton.isVisible = true
                }
            }
        }.autoDispose()
        RxBus.listen(RxEvent.RemovePromoCode::class.java).subscribeAndObserveOnMainThread {
            binding.cartView.rlPromocode.isVisible = false
            orderTotal = orderTotal?.plus(promoCodeAmount.div(100))
            promoCodeAmount = 0.00
            binding.cartView.tvTotalPrizeNumber.text = orderTotal.toConvertDecimalFormat().toDollar()
            RxBus.publish(RxEvent.PassTotal(orderTotal.toConvertDecimalFormat()))
            if (orderTotal.toConvertDecimalFormat() != 0.00) {
                binding.cartView.proceedToPaymentMaterialButton.isVisible = true
                binding.cartView.createOrderMaterialButton.isVisible = false
                if (loggedInUserCache.getIsEmployeeMeal() != true && loggedInUserCache.isUserLoggedIn()) {
                    binding.cartView.adjustmentsMaterialButton.isVisible = true
                }
            }
        }.autoDispose()
        RxBus.listen(RxEvent.RemoveCredit::class.java).subscribeAndObserveOnMainThread {
            binding.cartView.rlCredit.isVisible = false
            orderTotal = orderTotal?.plus(userCredit.div(100))
            userCredit = 0.00
            binding.cartView.tvTotalPrizeNumber.text = orderTotal.toConvertDecimalFormat().toDollar()
//            orderTotalCount()
            if (orderTotal.toConvertDecimalFormat() != 0.00) {
                binding.cartView.proceedToPaymentMaterialButton.isVisible = true
                if (loggedInUserCache.getIsEmployeeMeal() != true && loggedInUserCache.isUserLoggedIn()) {
                    binding.cartView.adjustmentsMaterialButton.isVisible = true
                }
            }
        }.autoDispose()
        RxBus.listen(RxEvent.EventGotoStartButton::class.java).subscribeAndObserveOnMainThread {
//            binding.userStoreViewPager.currentItem = 0
//            categorySelection()
//            binding.tvCartName.text = resources.getString(R.string.cart)
//            binding.cartView.customerDetails.isVisible = false
//            binding.cartView.tvOrderHeading.isVisible = false
//            binding.cartView.viewOrder.isVisible = false
//            binding.cartView.rlGiftCard.isVisible = false
//            binding.cartView.rlPromocode.isVisible = false
//            binding.cartView.rlCredit.isVisible = false
//            binding.cartView.cartListAndPrizeLinearLayout.isVisible = false
//            binding.cartView.emptyMessageAppCompatTextView.isVisible = true
//            promoCodeAmount = 0.0
//            giftCardAmount = 0.0
//            userCredit = 0.0
//            binding.llBack.isVisible = true
            onBackPressed()
        }.autoDispose()
        RxBus.listen(RxEvent.AddCredit::class.java).subscribeAndObserveOnMainThread {
            binding.cartView.rlCredit.isVisible = true
            userCredit = it.credit
            binding.cartView.tvCreditDiscount.text = "-${it.credit.div(100).toDollar()}"
            binding.cartView.tvTotalPrizeNumber.text = orderTotal?.minus(it.credit.div(100)).toDollar()
            orderTotal = orderTotal?.minus(it.credit.div(100))
        }.autoDispose()

        RxBus.listen(RxEvent.AddEmployeeDiscount::class.java).subscribeAndObserveOnMainThread {
            binding.cartView.rlEmployeeDiscount.isVisible = true
            employeeDiscount = it.discount
            binding.cartView.tvEmployeeDiscountPrize.text = "-${employeeDiscount.div(100).toDollar()}"
            binding.cartView.tvTotalPrizeNumber.text = orderTotal?.minus(employeeDiscount.div(100)).toDollar()
            orderTotal = orderTotal?.minus(employeeDiscount.div(100))
        }.autoDispose()
        RxBus.listen(RxEvent.AddAdjustmentDiscount::class.java).subscribeAndObserveOnMainThread {
            if (it.discount == 0.00) {
                binding.cartView.rlAdjustment.isVisible = false
                orderTotal = orderTotal?.plus(adjustmentDiscount.div(100))
                adjustmentDiscount = 0.00
                binding.cartView.tvTotalPrizeNumber.text = orderTotal?.plus(adjustmentDiscount.div(100)).toDollar()
                orderTotal = orderTotal?.minus(employeeDiscount.div(100))
            } else {
                binding.cartView.rlAdjustment.isVisible = true
                orderTotal = orderTotal?.plus(adjustmentDiscount.div(100))
                adjustmentDiscount = it.discount
                if (it.discount > 0 ) {
                    binding.cartView.tvAdjustmentDiscountPrize.text = adjustmentDiscount.div(100).toDollar()
                } else {
                    binding.cartView.tvAdjustmentDiscountPrize.text = "-${abs(adjustmentDiscount).div(100).toDollar()}"
                }
                binding.cartView.tvTotalPrizeNumber.text = orderTotal?.plus(adjustmentDiscount.div(100)).toDollar()
                orderTotal = orderTotal?.minus(employeeDiscount.div(100))
            }
            orderTotalCount()
        }.autoDispose()
        RxBus.listen(RxEvent.OpenRedeemPoint::class.java).subscribeAndObserveOnMainThread {
            isRedeemPointScreen = true
            redeemPointCategorySelection()
        }.autoDispose()
        RxBus.listen(RxEvent.RedeemProduct::class.java).subscribeAndObserveOnMainThread {
            val cartGroupId = loggedInUserCache.getLoggedInUserCartGroupId() ?: throw Exception("Cart Group not found")
            userStoreViewModel.getCartDetails(cartGroupId)
        }.autoDispose()
        RxBus.listen(RxEvent.RemoveBackButton::class.java).subscribeAndObserveOnMainThread {
            binding.tvCartName.text = resources.getString(R.string.order_Placed)
            binding.llBack.isVisible = false
        }.autoDispose()
        RxBus.listen(RxEvent.CheckOutValidationFailed::class.java).subscribeAndObserveOnMainThread {
            paymentButtonVisibility(false)
            orderTotalCount()
        }.autoDispose()
        binding.openStoreTimeLinearLayout.isSelected = true
        binding.tvOpenAndClose.isSelected = true
    }

    private fun cartItemNotChangeNow() {
        val list = cartAdapter.listOfProductDetails
        list?.filter { it.isChanging == true }?.forEach {
            it.isChanging = false
        }
        cartAdapter.listOfProductDetails = list
    }

    private fun removeCategorySelection() {
        binding.categoryRecycle.alpha = 0.5F
        val listOfMenu = categoryAdapter.listOfMenu
        listOfMenu?.filter { it.isSelected }?.forEach {
            it.isSelected = false
        }
        categoryAdapter.listOfMenu = listOfMenu
    }

    private fun categorySelection() {
        binding.categoryRecycle.alpha = 1F
        val listOfMenu = categoryAdapter.listOfMenu
        listOfMenu?.get(0)?.isSelected = true
        categoryAdapter.listOfMenu = listOfMenu
        CookiesFragment.listOfProduct = listOfMenu?.firstOrNull()
    }

    private fun redeemPointCategorySelection() {
        binding.categoryRecycle.alpha = 1F
        val listOfMenu = categoryAdapter.listOfMenu
        listOfMenu?.get(0)?.isSelected = true
        categoryAdapter.listOfMenu = listOfMenu
        RedeemProductFragment.listOfProduct = listOfProduct
    }

    @SuppressLint("SetTextI18n")
    private fun orderTotalCount() {
        orderSubTotal = 0.00
        var productTotal = 0.00
        listOfProductDetails?.forEach { it ->
            if (it.menuItemRedemption == 0 && it.menuItemComp == 0) {
                it.menuItemPrice?.let {
                    productTotal = it
                }
            } else {
                productTotal = 0.00
            }
            it.menuItemModifiers?.forEach {
                it.options?.forEach { item ->
                    productTotal = productTotal.plus(item.optionPrice!!)
                }
            }
            productTotal = it.menuItemQuantity?.let { it1 -> productTotal.times(it1) } ?: 0.00
            orderSubTotal = orderSubTotal.plus(productTotal)
        }
        orderSubTotalCount = orderSubTotal.div(100)
        employeeDiscount.let {
            if (it != 0.00) {
                employeeDiscount = orderSubTotalCount.times(20)
            }
        }

        orderTax = orderSubTotalCount.times(9.5).div(100)
        orderTotal = orderSubTotalCount + orderTax
        orderTotal = orderTotal?.minus(giftCardAmount.div(100))
        orderTotal = orderTotal?.minus(employeeDiscount.div(100))
        orderTotal = orderTotal?.minus(promoCodeAmount.div(100))
        if (orderTotal.toConvertDecimalFormat() == -0.00) {
            orderTotal = orderTotal?.let { it1 -> kotlin.math.abs(it1) }
        }
        adjustmentDiscount.let {
            if (it != 0.00) {
                binding.cartView.rlAdjustment.isVisible = true
                orderTotal = orderTotal.toConvertDecimalFormat()?.plus(adjustmentDiscount.div(100))
            }
        }
        binding.cartView.tvOrderPrizeNumber.isVisible = true
        binding.cartView.tvTaxNumber.isVisible = true
        binding.cartView.tvTotalPrizeNumber.isVisible = true
        if (employeeDiscount != -0.00) {
            binding.cartView.rlEmployeeDiscount.isVisible = true
        }
        binding.cartView.tvOrderPrizeNumber.text = orderSubTotalCount.toConvertDecimalFormat().toDollar()
        binding.cartView.tvTaxNumber.text = orderTax.toConvertDecimalFormat().toDollar()
        binding.cartView.tvEmployeeDiscountPrize.text = "-" + employeeDiscount.div(100).toConvertDecimalFormat().toDollar()
        binding.cartView.tvTotalPrizeNumber.text = orderTotal.toConvertDecimalFormat().toDollar()
        binding.tvTotalPrizeNumber.text = orderTotal.toConvertDecimalFormat().toDollar()
        if (binding.userStoreViewPager.currentItem == 0) {
            if (orderTotal.toConvertDecimalFormat() == 0.00) {
                binding.cartView.proceedToCheckoutMaterialButton.isVisible = true
                binding.cartView.proceedToPaymentMaterialButton.isVisible = false
                binding.cartView.createOrderMaterialButton.isVisible = false
                binding.cartView.adjustmentsMaterialButton.isVisible = loggedInUserCache.isUserLoggedIn()
                binding.cartView.rlPoint.isVisible = false
            } else {
                binding.cartView.proceedToCheckoutMaterialButton.isVisible = true
                binding.cartView.proceedToPaymentMaterialButton.isVisible = false
                binding.cartView.createOrderMaterialButton.isVisible = false
                binding.cartView.rlPoint.isVisible = false
                binding.cartView.adjustmentsMaterialButton.isVisible = loggedInUserCache.isUserLoggedIn()
            }
        } else if (binding.userStoreViewPager.currentItem == 1) {
            if (orderTotal.toConvertDecimalFormat() == 0.00) {
                binding.cartView.proceedToCheckoutMaterialButton.isVisible = false
                binding.cartView.proceedToPaymentMaterialButton.isVisible = false
                binding.cartView.createOrderMaterialButton.isVisible = true
                binding.cartView.createOrderMaterialButton.isEnabled = true
                binding.cartView.adjustmentsMaterialButton.isVisible = loggedInUserCache.isUserLoggedIn()
                binding.cartView.rlPoint.isVisible = false
            } else {
                binding.cartView.proceedToCheckoutMaterialButton.isVisible = false
                binding.cartView.proceedToPaymentMaterialButton.isVisible = true
                binding.cartView.createOrderMaterialButton.isVisible = false
                binding.cartView.adjustmentsMaterialButton.isVisible = loggedInUserCache.isUserLoggedIn()
                binding.cartView.rlPoint.isVisible = false
            }
            RxBus.publish(RxEvent.EventTotalCheckOut(OrderPrice(orderTotal.toConvertDecimalFormat(), orderSubTotalCount)))
        } else {
            binding.cartView.rlTotal.isVisible = false
            binding.cartView.rlCredit.isVisible = false
            binding.cartView.rlPoint.isVisible = false
            binding.cartView.rlPromocode.isVisible = false
            binding.cartView.rlGiftCard.isVisible = false
            binding.cartView.rlEmployeeDiscount.isVisible = false
            binding.cartView.rlOrderPrice.isVisible = false
            binding.cartView.rlAdjustment.isVisible = false
            binding.cartView.adjustmentsMaterialButton.isVisible = false
            binding.cartView.rlTax.isVisible = false
        }
//        if (orderTotal == 0.00) {
//            binding.cartView.createOrderMaterialButton.isVisible = false
//            binding.cartView.proceedToCheckoutMaterialButton.isVisible = true
//            binding.cartView.adjustmentsMaterialButton.isVisible = false
//            binding.cartView.rlPoint.isVisible = false
//        } else {
//            binding.cartView.createOrderMaterialButton.isVisible = false
//            if (binding.userStoreViewPager.currentItem == 0) binding.cartView.proceedToCheckoutMaterialButton.isVisible = true
//            if (loggedInUserCache.isUserLoggedIn()) binding.cartView.adjustmentsMaterialButton.isVisible = true
//        }

        if (loggedInUserCache.getIsEmployeeMeal() == true) {
            binding.cartView.rlPoint.isVisible = false
            binding.cartView.viewPointBelow.isVisible = false
        }
        binding.cartView.tvPoint.text = orderSubTotalCount.times(10).toInt().toString()

    }

    private fun initAdapter() {
        cartAdapter = CartAdapter(this).apply {
            userStoreCartActionState.subscribeAndObserveOnMainThread {
                when (it) {
                    is CartItemClickStates.CartItemAdditionClick -> {
                        val productQuantity = it.data.menuItemQuantity?.plus(1)
                        it.data.id?.let { it1 -> userStoreViewModel.updateMenuItemQuantity(UpdateMenuItemQuantity(it1, productQuantity)) }
                    }
                    is CartItemClickStates.CartItemCompProductReasonClick -> {
                        val bottomSheetPostMoreItem: BottomSheetCompReason = BottomSheetCompReason.newInstance()
                        bottomSheetPostMoreItem.compReasonClick.subscribeAndObserveOnMainThread { item ->
                            it.data.compReason = item
                        }.autoDispose()
                        bottomSheetPostMoreItem.show(supportFragmentManager, "TAGS")
                    }
                    is CartItemClickStates.CartItemConfirmButtonClick -> {
                        if (it.data.compReason?.type.isNullOrEmpty()) {
                            showToast("Please select a comp reason")
                        } else {
                            userStoreViewModel.compProduct(CompProductRequest(it.data.id, menuItemCompReason = it.data.compReason?.type,menuItemFullComp = true))
                        }
                    }
                    is CartItemClickStates.CartItemDeleteClick -> {
                        it.data.let { item -> deletedCartItemId = item }
                        if (it.data.menuItemRedemption == 1) {
                            it.data.id?.let { item ->
                                userStoreViewModel.deleteCartItem(
                                    item, it.data.menu?.product?.productLoyaltyTier?.tierValue ?: 0
                                )
                            }
                        } else {
                            it.data.id?.let { item -> userStoreViewModel.deleteCartItem(item) }
                        }

                    }
                    is CartItemClickStates.CartItemEditClick -> {
                        if (!it.data.menuItemModifiers.isNullOrEmpty()) {
                            startActivityWithDefaultAnimation(EditCartActivity.getIntent(this@UserStoreActivity, it.data))
                        } else {
                            val editCartFragment = EditCartFragment()
                            EditCartFragment.listOfProduct = it.data
                            EditCartFragment.isRedeemProduct = if (it.data.menuItemRedemption == 0) 0 else 1
                            editCartFragment.show(supportFragmentManager, UserStoreActivity::class.java.name)
                        }
                    }
                    is CartItemClickStates.CartItemSubscriptionClick -> {
                        if (it.data.menuItemQuantity != 1) {
                            val productQuantity = it.data.menuItemQuantity?.minus(1)
                            it.data.id?.let { it1 -> userStoreViewModel.updateMenuItemQuantity(UpdateMenuItemQuantity(it1, productQuantity)) }
                        } else {
                            showToast(getString(R.string.minimum_Quantity))
                        }
                    }
                    is CartItemClickStates.RedeemProductClick -> {
                        if (it.data.menuItemRedemption == 0) {
                            if ((it.data.menuItemQuantity ?: 0) > 1) {
                                it.data.id?.let { it1 ->
                                    userStoreViewModel.redeemCartItem(
                                        UpdateMenuItemQuantity(
                                            cartId = it1
                                        )
                                    )
                                }
                            } else {
                                it.data.id?.let { it1 ->
                                    userStoreViewModel.updateMenuItemQuantity(
                                        UpdateMenuItemQuantity(
                                            cartId = it1, menuItemQuantity = 1, menuItemRedemption = true
                                        ), it.data.menu?.product?.productLoyaltyTier?.tierValue
                                    )
                                }
                            }

                        } else {
                            it.data.id?.let { it1 ->
                                userStoreViewModel.updateMenuItemQuantity(
                                    UpdateMenuItemQuantity(
                                        cartId = it1, menuItemQuantity = 1, menuItemRedemption = false
                                    ), (it.data.menu?.product?.productLoyaltyTier?.tierValue ?: 0).times(-1)
                                )
                            }
                        }

                    }
                }
            }.autoDispose()
        }
        binding.cartView.CartRecycleView.layoutManager = LinearLayoutManager(this, RecyclerView.VERTICAL, false)
        binding.cartView.CartRecycleView.apply {
            adapter = cartAdapter
        }
        categoryAdapter = CategoryAdapter(this).apply {
            userStoreCategoryActionState.subscribeAndObserveOnMainThread { item ->
                hideKeyboard()
                val listOfMenu = categoryAdapter.listOfMenu
                listOfMenu?.filter { it.isSelected }?.forEach {
                    it.isSelected = false
                }
                listOfMenu?.find { it.id == item.id }?.apply {
                    isSelected = true
                }
                categoryAdapter.listOfMenu = listOfMenu
                val listOfFilterItems = listOfMenu?.filter { it.categoryName == item.categoryName }
                if ((listOfFilterItems?.size ?: 0) > 0) {
                    if (isRedeemPointScreen) RedeemProductFragment.listOfProduct = listOfFilterItems?.firstOrNull()?.products
                    else CookiesFragment.listOfProduct = listOfFilterItems?.firstOrNull()
                }
            }.autoDispose()
        }
        binding.categoryRecycle.layoutManager = LinearLayoutManager(this, RecyclerView.VERTICAL, false)
        binding.categoryRecycle.apply {
            adapter = categoryAdapter
        }
    }

    private fun listenToViewModel() {
        userStoreViewModel.userStoreState.subscribeAndObserveOnMainThread {
            when (it) {
                is UserStoreState.ErrorMessage -> {
                    showToast(it.errorMessage)
                }
                is UserStoreState.LoadingState -> {

                }
                is UserStoreState.UserStoreLoadingState -> {
                    if (it.isLoading) {
                        binding.progressBar.isVisible = true
                        binding.categoryRecycle.visibility = View.INVISIBLE
                    } else {
                        binding.progressBar.isVisible = false
                        binding.categoryRecycle.visibility = View.VISIBLE
                    }
                }
                is UserStoreState.CreateOrderLoadingState -> {
                    buttonVisibility(it.isLoading)
                }
                is UserStoreState.SuccessMessage -> {
                    showToast(it.successMessage)
                }
                is UserStoreState.UpdatedCartInfo -> {
                    binding.tvLoyaltyPoint.text = resources.getString(R.string.leaves).plus(":").plus(userStoreViewModel.getLoyaltyPoint())
                    loggedInUserCache.getLoggedInUserCartGroupId()?.let { item -> userStoreViewModel.getCartDetails(item) }
                }
                is UserStoreState.UserLoyaltyPoint -> {
                    binding.tvLoyaltyPoint.isVisible = true
                    binding.tvLoyaltyPoint.text = resources.getString(R.string.leaves).plus(":").plus(it.data.points ?: 0)
                }
                is UserStoreState.CompProductProductResponse -> {
                    loggedInUserCache.getLoggedInUserCartGroupId()?.let { item -> userStoreViewModel.getCartDetails(item) }
                }
                is UserStoreState.RedeemProduct -> {
                    loggedInUserCache.getLoggedInUserCartGroupId()?.let { item -> userStoreViewModel.getCartDetails(item) }
                    binding.tvLoyaltyPoint.text = resources.getString(R.string.leaves).plus(":").plus(userStoreViewModel.getLoyaltyPoint())
                }
                is UserStoreState.DeletedCartItem -> {
                    binding.tvLoyaltyPoint.text = resources.getString(R.string.leaves).plus(":").plus(userStoreViewModel.getLoyaltyPoint())
                    val cartList: ArrayList<CartItem> = cartAdapter.listOfProductDetails as ArrayList<CartItem>
                    if (cartList.contains(deletedCartItemId)) {
                        cartList.remove(deletedCartItemId)
                    }
                    cartAdapter.listOfProductDetails = cartList
                    listOfProductDetails = cartList
                    orderTotalVisibility(cartList)
                    orderTotalCount()
                    if (binding.userStoreViewPager.currentItem == 0) {
                        if (cartList.isNotEmpty()) {
                            var number = 0
                            cartList.forEach { item ->
                                number = number.plus(item.menuItemQuantity!!)
                            }
                            binding.tvCartName.text = resources.getString(R.string.cart).plus(" ($number)")
                        } else {
                            binding.tvCartName.text = resources.getString(R.string.cart)
                        }
                    }
                }
                is UserStoreState.CartDetailsInfo -> {
                    if (it.cartInfo.cartGroup?.status == ABANDONED) {
                        onBackPressed()
                    }
                    binding.tvLoyaltyPoint.text = resources.getString(R.string.leaves).plus(":").plus(userStoreViewModel.getLoyaltyPoint())
                    listOfProductDetails = it.cartInfo.cart as ArrayList<CartItem>
//                    listOfProductDetails = it.cartInfo.cart?.filter { (it.menuItemQuantity ?: 0) > 0 } as ArrayList<CartItem>
                    setCartData(listOfProductDetails)
                    if (binding.userStoreViewPager.currentItem == 0) {
                        if (it.cartInfo.cart.isNotEmpty()) {
                            var number = 0
                            it.cartInfo.cart.forEach { item ->
                                number = number.plus(item.menuItemQuantity!!)
                            }
                            binding.tvCartName.text = resources.getString(R.string.cart).plus(" ($number)")
                        } else {
                            binding.tvCartName.text = resources.getString(R.string.cart)
                        }
                    }
                    orderTotalCount()
                }
                is UserStoreState.MenuInfo -> {
                    if (isRedeemPointScreen) RedeemProductFragment.listOfProduct = it.menuListInfo.menus?.firstOrNull()?.products
                    else CookiesFragment.listOfProduct = it.menuListInfo.menus?.firstOrNull()

                    listOfProduct = it.menuListInfo.menus?.firstOrNull()?.products as ArrayList<ProductsItem>?
                    listOfMenu = it.menuListInfo.menus as ArrayList<MenusItem>
                    setCategoryData(it.menuListInfo)
                }
                is UserStoreState.StoreResponses -> {
                    setStoreOpenAndClose(it.storeResponse)
                }
                is UserStoreState.CreatePosOrder -> {
                    binding.userStoreViewPager.currentItem = 2
                    it.cartInfo?.let { it1 -> RxBus.publish(RxEvent.OpenOrderSuccessDialog(it1)) }
                    binding.llBack.isVisible = false
                    binding.cartView.createOrderMaterialButton.isVisible = false
                    binding.cartView.rlOrderPrice.isVisible = false
                    binding.cartView.rlGiftCard.isVisible = false
                    binding.cartView.rlOrderPrice.isVisible = false
                    binding.cartView.rlTax.isVisible = false
                    binding.cartView.rlTotal.isVisible = false
                    binding.cartView.rlGiftCard.isVisible = false
                    binding.cartView.rlPromocode.isVisible = false
                    binding.cartView.rlAdjustment.isVisible = false
                    binding.cartView.adjustmentsMaterialButton.isVisible = false
                    binding.cartView.customerDetails.isVisible = true
                    binding.cartView.tvOrderHeading.isVisible = true
                    binding.cartView.viewOrder.isVisible = true
                    binding.cartView.viewOrder.isVisible = true
                    val list = cartAdapter.listOfProductDetails
                    list?.filter { item -> item.isChanging == true }?.forEach { item ->
                        item.isChanging = false
                    }
                    cartAdapter.listOfProductDetails = list
                    binding.cartView.customerNameAppCompatTextView.text =
                        loggedInUserCache.getLoyaltyQrResponse()?.fullName.plus(" ").plus(loggedInUserCache.getLoyaltyQrResponse()?.lastName)
                    if (loggedInUserCache.getLoyaltyQrResponse()?.email != "") {
                        binding.cartView.customerEmailAppCompatTextView.text = loggedInUserCache.getLoyaltyQrResponse()?.email
                    }
                    if (loggedInUserCache.getLoyaltyQrResponse()?.phone != "") {
                        binding.cartView.customerPhoneNumberAppCompatTextView.text = loggedInUserCache.getLoyaltyQrResponse()?.phone
                    }
                    binding.cartView.createOrderMaterialButton.isVisible = false
                }
                else -> {

                }
            }
        }.autoDispose()
    }

    private fun orderTotalVisibility(cartList: ArrayList<CartItem>) {
        if (cartList.size == 0) {
            binding.cartView.cartListAndPrizeLinearLayout.isVisible = false
            binding.cartView.emptyMessageAppCompatTextView.isVisible = true
        }
    }

    private fun buttonVisibility(isLoading: Boolean) {
        binding.cartView.createOrderMaterialButton.visibility = GONE
        binding.cartView.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
    }


    private fun paymentButtonVisibility(isLoading: Boolean) {
        binding.cartView.proceedToPaymentMaterialButton.visibility = GONE
        binding.cartView.createOrderMaterialButton.visibility = GONE
        binding.cartView.proceedToCheckoutMaterialButton.visibility = GONE
        binding.cartView.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
    }

    private fun checkOutFragment() {
        binding.userStoreViewPager.currentItem = 1
        binding.cartView.proceedToPaymentMaterialButton.isVisible = true
        binding.cartView.customerDetails.isVisible = false
        binding.cartView.viewOrder.isVisible = false
        binding.cartView.tvOrderHeading.isVisible = false
        setCartData(cartAdapter.listOfProductDetails)
    }

    private fun setCategoryData(menuListInfo: MenuListInfo) {
        menuListInfo.menus?.firstOrNull()?.apply {
            isSelected = true
        }
        categoryAdapter.listOfMenu = menuListInfo.menus
    }

    private fun setCartData(cartItem: List<CartItem>?) {
        if (cartItem?.size != 0) {
            binding.cartView.cartListAndPrizeLinearLayout.isVisible = true
            binding.cartView.emptyMessageAppCompatTextView.isVisible = false
            if (binding.userStoreViewPager.currentItem != 0) {
                cartItem?.filter { it.isChanging == true }?.forEach {
                    it.isChanging = false
                }
            } else {
                cartItem?.filter { it.isChanging == false }?.forEach {
                    it.isChanging = true
                }
            }
            cartAdapter.listOfProductDetails = cartItem
            listOfProductDetails = cartItem as ArrayList<CartItem>?
            binding.cartView.rlTax.isVisible = true
            binding.cartView.rlOrderPrice.isVisible = true
            binding.cartView.rlTotal.isVisible = true
            if (binding.userStoreViewPager.currentItem == 0) binding.cartView.proceedToCheckoutMaterialButton.isVisible = true
            binding.tvTotalPrizeNumber.isVisible = true
            orderTotalCount()
            if (loggedInUserCache.isUserLoggedIn()) {
                if (binding.userStoreViewPager.currentItem != 2) {
                    binding.cartView.adjustmentsMaterialButton.isVisible = true
                    binding.cartView.adjustmentsMaterialButton.isEnabled = true
                }

            }
        } else {
            binding.tvTotalPrizeNumber.isVisible = false
            binding.cartView.cartListAndPrizeLinearLayout.isVisible = false
            binding.cartView.emptyMessageAppCompatTextView.isVisible = true
            cartAdapter.listOfProductDetails = null
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

    private fun onApiCalling() {
        if (loggedInUserCache.getLoggedInUserCartGroupId() != 0) {
            loggedInUserCache.getLoggedInUserCartGroupId()?.let { userStoreViewModel.getCartDetails(it) }
        }
    }

    private fun openLogOutDialog() {
        val alertDialog = AlertDialog.Builder(this).setTitle(resources.getText(R.string.clear))
            .setMessage(resources.getText(R.string.are_you_sure_cancel_and_clear))
            .setNegativeButton(resources.getText(R.string.label_cancel)) { dialog, _ ->
                dialog.dismiss()
            }.setPositiveButton(resources.getText(R.string.label_ok)) { _, _ ->
                if (loggedInUserCache.isUserLoggedIn()) {
                    finish()
                } else {
                    loggedInUserCache.clearLoggedInUserLocalPrefs()
                    startNewActivityWithDefaultAnimation(
                        LoginActivity.getIntent(
                            this@UserStoreActivity, loggedInUserCache.getLocationInfo()?.location?.id
                        )
                    )
                }

            }.show()
        alertDialog.window?.setLayout(800, WindowManager.LayoutParams.WRAP_CONTENT)
    }

    override fun onResume() {
        super.onResume()
        onApiCalling()
        categorySelection()
        userStoreViewModel.loadCurrentStoreResponse()
        resetDisconnectTimer()
    }


    override fun onUserInteraction() {
        binding.headerUserStore.alpha = 1F
        binding.bottomLayout.alpha = 1F
        binding.userStoreView.alpha = 1F
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
                this@UserStoreActivity, loggedInUserCache.getLocationInfo()?.location?.id
            )
        )
        finish()
    }
    private val showToastCallback = Runnable { // Perform any required operation on disconnect
        Timber.tag("SessionTimeOutActivity").d("showToastCallback")
        if (loggedInUserCache.isUserLoggedIn()) {
            binding.headerUserStore.alpha = 0.2F
            binding.bottomLayout.alpha = 0.2F
            binding.userStoreView.alpha = 0.2F
            binding.logOutTextView.isVisible = true
        }
    }

    private fun resetDisconnectTimer() {
        Timber.tag("SessionTimeOutActivity").d("resetDisconnectTimer")
        disconnectHandler.removeCallbacks(disconnectCallback)
        showToastHandler.removeCallbacks(showToastCallback)
        disconnectHandler.postDelayed(disconnectCallback, disconnectTimeOut)
        showToastHandler.postDelayed(showToastCallback, showToastTime)
    }

    private fun stopDisconnectTimer() {
        disconnectHandler.removeCallbacks(disconnectCallback)
        showToastHandler.removeCallbacks(showToastCallback)
    }


    override fun onPause() {
        super.onPause()
        Timber.tag("UserStoreActivity").d("stopDisconnectTimer")
        stopDisconnectTimer()
    }


}