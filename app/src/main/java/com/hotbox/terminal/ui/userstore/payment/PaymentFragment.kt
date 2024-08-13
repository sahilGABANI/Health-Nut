package com.hotbox.terminal.ui.userstore.payment

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.bugsnag.android.Bugsnag
import com.epson.epos2.Epos2Exception
import com.epson.epos2.printer.Printer
import com.google.gson.Gson
import com.hotbox.terminal.R
import com.hotbox.terminal.api.authentication.LoggedInUserCache
import com.hotbox.terminal.api.authentication.model.HealthNutUser
import com.hotbox.terminal.api.order.model.OrderDetailsResponse
import com.hotbox.terminal.api.stripe.ApiService
import com.hotbox.terminal.api.stripe.model.*
import com.hotbox.terminal.api.userstore.model.CreateOrderRequest
import com.hotbox.terminal.api.userstore.model.CreateOrderResponse
import com.hotbox.terminal.application.HotBoxApplication
import com.hotbox.terminal.base.BaseFragment
import com.hotbox.terminal.base.RxBus
import com.hotbox.terminal.base.RxEvent
import com.hotbox.terminal.base.ViewModelFactory
import com.hotbox.terminal.base.extension.*
import com.hotbox.terminal.databinding.FragmentPaymentBinding
import com.hotbox.terminal.helper.*
import com.hotbox.terminal.ui.userstore.viewmodel.UserStoreState
import com.hotbox.terminal.ui.userstore.viewmodel.UserStoreViewModel
import com.hotbox.terminal.utils.Constants
import com.hotbox.terminal.utils.Constants.EMAIL
import com.hotbox.terminal.utils.Constants.MODE_ID
import com.hotbox.terminal.utils.Constants.PHONE
import com.hotbox.terminal.utils.Constants.TRANSACTION_ID_OF_PROCESSOR_FOR_ORDER
import com.hotbox.terminal.utils.Constants.isDebugMode
import com.hotbox.terminal.utils.UserInteractionInterceptor
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory
import timber.log.Timber
import java.io.File
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.properties.Delegates

class PaymentFragment : BaseFragment() {

    private lateinit var createOrderRequest: CreateOrderRequest
    private lateinit var paymentResponse: String
    private var createPosOrder: CreateOrderResponse? = null
    private lateinit var call: Call<String?>
    private lateinit var terminalAccessKey: String
    private lateinit var posAccessKey: String
    private lateinit var apiService: ApiService
    private var promisedTime: String? = null
    private var _binding: FragmentPaymentBinding? = null
    private val binding get() = _binding!!
    private var total by Delegates.notNull<Double>()
    private var subTotal: Int = 0
    private var adjustment: Int = 0
    private var giftCardId: String? = null
    private var enterGiftCardPrize: Int? = null
    private var couponCodeId: Int? = null
    private var creditAmount: Int? = null
    private lateinit var printer: Printer
    private lateinit var bohPrinterHelper: BohPrinterHelper
    private lateinit var androidId: String
    private var bohPrintAddress: String? = null
    private var count: Int? = 0
    private var isSendReceipt: SendReceiptType = SendReceiptType.Nothing

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<UserStoreViewModel>
    private lateinit var userStoreViewModel: UserStoreViewModel

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache

    companion object {
        @JvmStatic
        fun newInstance() = PaymentFragment()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        HotBoxApplication.component.inject(this)
        userStoreViewModel = getViewModelFromFactory(viewModelFactory)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPaymentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        androidId = Settings.Secure.getString(requireContext().contentResolver, Settings.Secure.ANDROID_ID)
        userStoreViewModel.getOrderPromisedTime()
        listenToViewModel()
        listenToViewEvent()
    }

    @SuppressLint("SimpleDateFormat")
    private fun listenToViewModel() {
        userStoreViewModel.userStoreState.subscribeAndObserveOnMainThread {
            when (it) {
                is UserStoreState.ConnectionTokenResponse -> {

                }
                is UserStoreState.ErrorMessage -> {
                    showToast(it.errorMessage)
                }
                is UserStoreState.CreateOrderErrorMessage -> {
                    showToast(it.errorMessage)
                    binding.pendingPayment.progressBar.isVisible = false
                    binding.pendingPayment.tryAgainOrderMaterialButton.isVisible = true
                    binding.pendingPayment.tvDescription.text = resources.getString(R.string.payment_success_but_order_creation_failed)
                    binding.pendingPayment.dialogHeading.text = resources.getString(R.string.order_creation_failed)
                    if (Bugsnag.isStarted()) {
                        Bugsnag.notify(
                            Exception(
                                "End Point :- v1/orders/create-pos-order -------------------- \n -------------------- \n Request Body : ${
                                    Gson().toJson(
                                        createOrderRequest
                                    )
                                } -------------------- \n" + " -------------------- \n Payment Api Response : $paymentResponse"
                            )
                        )
                    }
                }
                is UserStoreState.GetOrderPromisedTime -> {
                    promisedTime = it.getPromisedTime.time
                    binding.paymentSuccessPart.tvOrderPromisedTime.text =
                        it.getPromisedTime.time?.toDate("yyyy-MM-dd HH:mm")?.formatTo("MM/dd/yyyy, HH:mm") ?: ""
                }
                is UserStoreState.CreatePosOrder -> {
                    createPosOrder = it.cartInfo
                    if (loggedInUserCache.getLoyaltyQrResponse()?.phone.isNullOrEmpty()) {
                        binding.paymentSuccessPart.tvPhone.text = "-"
                        binding.paymentSuccessPart.tvSendReceiptPhone.text = "-"
                    } else {
                        binding.paymentSuccessPart.tvPhone.text = loggedInUserCache.getLoyaltyQrResponse()?.phone
                        binding.paymentSuccessPart.tvSendReceiptPhone.text = loggedInUserCache.getLoyaltyQrResponse()?.phone
                    }
                    if (loggedInUserCache.getLoyaltyQrResponse()?.email.isNullOrEmpty()) {
                        binding.paymentSuccessPart.tvEmail.text = "-"
                        binding.paymentSuccessPart.tvSendReceiptEmail.text = "-"
                    } else {
                        binding.paymentSuccessPart.tvEmail.text = loggedInUserCache.getLoyaltyQrResponse()?.email
                        binding.paymentSuccessPart.tvSendReceiptEmail.text = loggedInUserCache.getLoyaltyQrResponse()?.email
                    }
                    RxBus.publish(RxEvent.RemoveBackButton)
                    loggedInUserCache.setLoggedInUserCartGroupId(0)
                    binding.pendingPayment.root.visibility = View.GONE
                    binding.paymentSuccessPart.root.visibility = View.VISIBLE
                    binding.paymentSuccessPart.orderIdTextView.text = "Order #${it.cartInfo?.id}"
                    binding.paymentSuccessPart.tvOrderPromisedTime.text =
                        it.cartInfo?.orderPromisedTime?.toDate("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")?.formatTo("MM/dd/yyyy, HH:mm") ?: ""
                    it.cartInfo?.id?.let { item ->
                        if (loggedInUserCache.isUserLoggedIn()) {
                            userStoreViewModel.updateOrderStatusDetails(Constants.ORDER_STATUS_RECEIVE, item)
                        }
                    }
                    val t: Thread = object : Thread() {
                        override fun run() {
                            autoPrint()
                        }
                    }
                    t.start()

                }
                is UserStoreState.SendReceiptSuccessMessage -> {
                    showToast(it.successMessage)
                }
                else -> {

                }
            }
        }.autoDispose()
    }

    private fun bohPrinterInitialize() {
        Timber.tag("Printer").i("Call Connect BOH Printer")
        bohPrinterHelper = BohPrinterHelper.getInstance(requireActivity())
        bohPrinterHelper.printerInitialize(requireContext())

    }

    private fun autoPrint() {
        bohPrinterInitialize()
        val cart = com.hotbox.terminal.api.order.model.CartGroup(
            promisedTime = promisedTime?.toDate("yyyy-MM-dd HH:mm")?.formatTo("yyyy-MM-dd'T'HH:mm:ss"),
            cart = createPosOrder?.cartGroup?.cart,
            cartCreatedDate = createPosOrder?.cartGroup?.cartCreatedDate,
            id = createPosOrder?.cartGroup?.id
        )
        val orderMode = com.hotbox.terminal.api.order.model.OrderMode(
            id = createPosOrder?.orderMode?.id, modeName = createPosOrder?.orderMode?.modeName
        )
        val orderType = com.hotbox.terminal.api.order.model.OrderType(
            id = createPosOrder?.orderType?.id, subcategory = createPosOrder?.orderType?.subcategory
        )
        val user = HealthNutUser(
            firstName = loggedInUserCache.getLoyaltyQrResponse()?.fullName,
            lastName = loggedInUserCache.getLoyaltyQrResponse()?.lastName,
        )

        val orderDetails = OrderDetailsResponse(
            id = createPosOrder?.id,
            cartGroup = cart,
            orderCreationDate = createPosOrder?.orderCreationDate,
            orderLocation = createPosOrder?.orderLocation,
            orderSubtotal = createPosOrder?.orderSubtotal?.toDouble(),
            orderTotal = createPosOrder?.orderTotal?.toDouble(),
            orderTax = createPosOrder?.orderTax?.toDouble(),
            orderDeliveryFee = createPosOrder?.orderDeliveryFee?.toDouble(),
            orderTip = createPosOrder?.orderTip?.toDouble(),
            orderGiftCardAmount = createPosOrder?.orderGiftCardAmount?.toDouble(),
            orderCouponCodeDiscount = createPosOrder?.orderCouponCodeDiscount?.toDouble(),
            orderEmpDiscount = createPosOrder?.orderEmpDiscount?.toDouble(),
            orderRefundAmount = createPosOrder?.orderRefundAmount?.toDouble(),
            orderAdjustmentAmount = createPosOrder?.orderAdjustmentAmount?.toDouble(),
            orderMode = orderMode,
            orderType = orderType,
            user = user,
            guest = arrayListOf(),
            orderPromisedTime = createPosOrder?.orderPromisedTime?.toDate("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")?.formatTo("yyyy-MM-dd hh:mm a"),
        )
        Timber.tag("okhttpClient")
            .i("orderPromisedTime = ${createPosOrder?.orderPromisedTime?.toDate("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")?.formatTo("yyyy-MM-dd hh:mm a")}")
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
                bohPrinterHelper.runPrintBOHReceiptSequence(orderDetails, bohPrintAddress)
            } catch (e: java.lang.Exception) {
                Timber.tag("runPrintBOHReceiptSequence").e(e)
            }
        } else {
            Timber.tag("AutoReceive").e("----------------- Printer not connected -----------------")
        }
    }

    private fun isPosKeyBYPASS(): Boolean {
        if (loggedInUserCache.getLocationInfo()?.poskey == "BYPASS" && loggedInUserCache.getLocationInfo()?.terminalkey == "BYPASS") {
            return true
        }
        return false
    }

    private fun generateTransactionId(): String {
        val random = Random()
        val transactionId = StringBuilder()

        repeat(6) {
            val digit = random.nextInt(10)
            transactionId.append(digit)
        }
        return transactionId.toString()
    }

    private fun listenToViewEvent() {
        val fohPrintAddress = loggedInUserCache.getLocationInfo()?.printAddress ?: throw Exception("printAddress not found")
        RxBus.listen(RxEvent.EventTotalPayment::class.java).subscribeAndObserveOnMainThread {
            total = it.orderPrice.orderTotal!!
            subTotal = it.orderPrice.employeeDiscount!!
            adjustment = it.orderPrice.adjustmentDiscount ?: 0
            binding.pendingPayment.tvTotalPrizeNumber.text = total.toDollar()
            if (count == 0) {
                binding.pendingPayment.root.visibility = View.VISIBLE
                binding.paymentSuccessPart.root.visibility = View.GONE
                binding.pendingPayment.tryAgainMaterialButton.visibility = View.GONE
                binding.pendingPayment.tryAgainOrderMaterialButton.visibility = View.GONE
                if (isPosKeyBYPASS()) {
                    val now = Calendar.getInstance()
                    now.add(Calendar.MINUTE, 60)
                    createOrderRequest = CreateOrderRequest(
                        userId = if (loggedInUserCache.getLoyaltyQrResponse()?.id != null && loggedInUserCache.getLoyaltyQrResponse()?.id != "") loggedInUserCache.getLoyaltyQrResponse()?.id else null,
                        MODE_ID,
                        orderTip = 0,
                        orderTypeId = loggedInUserCache.getorderTypeId(),
                        orderCartGroupId = loggedInUserCache.getLoggedInUserCartGroupId(),
                        orderLocationId = loggedInUserCache.getLocationInfo()?.location?.id,
                        deliveryAddress = null,
                        guestFirstName = if (loggedInUserCache.getLoyaltyQrResponse()?.fullName?.isNotEmpty() == true) loggedInUserCache.getLoyaltyQrResponse()?.fullName else null,
                        guestLastName = if (loggedInUserCache.getLoyaltyQrResponse()?.lastName?.isNotEmpty() == true) loggedInUserCache.getLoyaltyQrResponse()?.lastName else null,
                        guestPhone = (if (loggedInUserCache.getLoyaltyQrResponse()?.phone?.isNotEmpty() == true) loggedInUserCache.getLoyaltyQrResponse()?.phone.toString() else "N/A"),
                        giftCardId = giftCardId,
                        orderGiftCardAmount = enterGiftCardPrize,
                        couponCodeId = couponCodeId,
                        creditAmount = creditAmount,
                        guestEmail = if (loggedInUserCache.getLoyaltyQrResponse()?.email?.isNotEmpty() == true) loggedInUserCache.getLoyaltyQrResponse()?.email.toString() else null,
                        transactionTerminal = "0000",
                        transactionIdOfProcessor = if (isPosKeyBYPASS()) generateTransactionId() else TRANSACTION_ID_OF_PROCESSOR_FOR_ORDER,
                        transactionChargeId = "4",
                        orderTotal = total.times(100).toInt(),
                        transactionTotalAmount = total.times(100).toInt(),
                        orderAdjustmentAmount = if (it.orderPrice.adjustmentDiscount == 0) null else it.orderPrice.adjustmentDiscount?.times(-1)
                    )

                    userStoreViewModel.createOrder(
                        createOrderRequest
                    )
                } else {
                    apiCalling(total)
                }
                count = 1
            }
        }.autoDispose()
        if (loggedInUserCache.getIsEmployeeMeal() == true) {
            binding.rlPayment.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.green_light_50))
            binding.pendingPayment.llPendingPaymentPart.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.green_light_50))
            binding.paymentSuccessPart.llPaymentSuccessPart.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.green_light_50))
        } else {
            binding.rlPayment.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.color_F0F4F6))
            binding.pendingPayment.llPendingPaymentPart.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.color_F0F4F6))
            binding.paymentSuccessPart.llPaymentSuccessPart.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.color_F0F4F6))
        }

        if (!loggedInUserCache.isUserLoggedIn()) {
            binding.paymentSuccessPart.printReceiptButton.isVisible = false
        }
        if (!isVisible) {
            count = 0
        }
        RxBus.listen(RxEvent.PassPromocodeAndGiftCard::class.java).subscribeAndObserveOnMainThread {
            if (it.giftCardId != "") {
                giftCardId = it.giftCardId
            }
            if (it.giftCardAmount != 0) {
                enterGiftCardPrize = it.giftCardAmount
            }
            if (it.couponCodeId != 0) {
                couponCodeId = it.couponCodeId
            }
        }.autoDispose()
        RxBus.listen(RxEvent.PassCreditAmount::class.java).subscribeAndObserveOnMainThread {
            if (it.creditAmount != 0) {
                creditAmount = it.creditAmount
            }
        }.autoDispose()
        RxBus.listen(RxEvent.OpenOrderSuccessDialog::class.java).subscribeAndObserveOnMainThread {
            createPosOrder = it.orderId
            if (loggedInUserCache.isUserLoggedIn()) {
                createPosOrder?.id?.let { it1 -> userStoreViewModel.updateOrderStatusDetails(Constants.ORDER_STATUS_RECEIVE, it1) }
            }
            binding.paymentSuccessPart.orderIdTextView.text = "Order #${it.orderId.id}"
            binding.paymentSuccessPart.tvOrderPromisedTime.text =
                it.orderId.orderPromisedTime?.toDate("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")?.formatTo("MM/dd/yyyy, HH:mm") ?: ""
            binding.pendingPayment.root.visibility = View.GONE
            binding.paymentSuccessPart.root.visibility = View.VISIBLE
            binding.paymentSuccessPart.root.visibility = View.VISIBLE
            successUi()
            val t: Thread = object : Thread() {
                override fun run() {
                    autoPrint()
                }
            }
            t.start()
        }.autoDispose()
        binding.pendingPayment.tryAgainMaterialButton.throttleClicks().subscribeAndObserveOnMainThread {
            binding.pendingPayment.ivPaymentError.setImageResource(R.drawable.ic_scan_loyalty_card)
            binding.pendingPayment.dialogHeading.text = resources.getString(R.string.pending_payment)
            binding.pendingPayment.tvDescription.text = resources.getString(R.string.complete_your_payment_on_the_payment_terminal)
            binding.pendingPayment.tryAgainMaterialButton.isVisible = false
            apiCalling(total)
        }.autoDispose()

        binding.paymentSuccessPart.printBOHReceiptButton.throttleClicks().subscribeAndObserveOnMainThread {
            val t: Thread = object : Thread() {
                override fun run() {
                    autoPrint()
                }
            }
            t.start()
        }.autoDispose()

        binding.pendingPayment.tryAgainOrderMaterialButton.throttleClicks().subscribeAndObserveOnMainThread {
            binding.pendingPayment.tryAgainOrderMaterialButton.isVisible = false
            userStoreViewModel.createOrder(createOrderRequest)
        }.autoDispose()

        binding.paymentSuccessPart.downArrowImageView.isSelected = true
        binding.paymentSuccessPart.downArrowBackgroundMaterialCardView.throttleClicks().subscribeAndObserveOnMainThread {
            if (binding.paymentSuccessPart.downArrowImageView.isSelected) {
                binding.paymentSuccessPart.downArrowImageView.isSelected = false
                when (isSendReceipt) {
                    SendReceiptType.Email -> {
                        binding.paymentSuccessPart.horizontalView.isVisible = false
                        binding.paymentSuccessPart.tvSendReceiptPhone.isVisible = false
                        binding.paymentSuccessPart.tvSendReceiptEmail.isVisible = false
                        binding.paymentSuccessPart.llButtons.isVisible = false
                        binding.paymentSuccessPart.printBOHReceiptButton.isVisible = false
                    }
                    SendReceiptType.Phone -> {
                        binding.paymentSuccessPart.horizontalView.isVisible = false
                        binding.paymentSuccessPart.tvSendReceiptPhone.isVisible = false
                        binding.paymentSuccessPart.tvSendReceiptEmail.isVisible = false
                        binding.paymentSuccessPart.llButtons.isVisible = false
                        binding.paymentSuccessPart.printBOHReceiptButton.isVisible = false
                    }
                    SendReceiptType.EmailAndPhone -> {
                        binding.paymentSuccessPart.horizontalView.isVisible = false
                        binding.paymentSuccessPart.tvSendReceiptPhone.isVisible = false
                        binding.paymentSuccessPart.tvSendReceiptEmail.isVisible = false
                        binding.paymentSuccessPart.llButtons.isVisible = false
                        binding.paymentSuccessPart.printBOHReceiptButton.isVisible = false
                    }
                    SendReceiptType.Nothing -> {
                        binding.paymentSuccessPart.userDetailsRelative.isVisible = false
                        binding.paymentSuccessPart.llButtons.isVisible = false
                        binding.paymentSuccessPart.horizontalView.isVisible = false
                        binding.paymentSuccessPart.printBOHReceiptButton.isVisible = false
                    }
                }
            } else {
                when (isSendReceipt) {
                    SendReceiptType.Email -> {
                        binding.paymentSuccessPart.llButtons.isVisible = true
                        binding.paymentSuccessPart.printBOHReceiptButton.isVisible = true
                        binding.paymentSuccessPart.tvSendReceiptEmail.isVisible = true
                        binding.paymentSuccessPart.userDetailsRelative.isVisible = true
                        binding.paymentSuccessPart.emailCheckBox.isVisible = false
                        binding.paymentSuccessPart.tvEmail.isVisible = false
                        binding.paymentSuccessPart.editEmail.isVisible = false
                        binding.paymentSuccessPart.horizontalView.isVisible = true
                        if (!binding.paymentSuccessPart.emailCheckBox.isVisible && !binding.paymentSuccessPart.phoneCheckBox.isVisible) {
                            binding.paymentSuccessPart.sendReceiptButton.isVisible = false
                            binding.paymentSuccessPart.tvSendReceiptEmail.isVisible = true
                            binding.paymentSuccessPart.tvSendReceiptPhone.isVisible = true
                        }
                    }
                    SendReceiptType.Phone -> {
                        binding.paymentSuccessPart.llButtons.isVisible = true
                        binding.paymentSuccessPart.printBOHReceiptButton.isVisible = true
                        binding.paymentSuccessPart.tvSendReceiptPhone.isVisible = true
                        binding.paymentSuccessPart.tvSendReceiptEmail.isVisible = false
                        binding.paymentSuccessPart.userDetailsRelative.isVisible = true
                        binding.paymentSuccessPart.phoneCheckBox.isVisible = false
                        binding.paymentSuccessPart.tvPhone.isVisible = false
                        binding.paymentSuccessPart.editPhone.isVisible = false
                        binding.paymentSuccessPart.horizontalView.isVisible = true
                        if (!binding.paymentSuccessPart.emailCheckBox.isVisible && !binding.paymentSuccessPart.phoneCheckBox.isVisible) {
                            binding.paymentSuccessPart.sendReceiptButton.isVisible = false
                            binding.paymentSuccessPart.tvSendReceiptEmail.isVisible = true
                            binding.paymentSuccessPart.tvSendReceiptPhone.isVisible = true
                        }
                    }
                    SendReceiptType.EmailAndPhone -> {
                        binding.paymentSuccessPart.llButtons.isVisible = true
                        binding.paymentSuccessPart.printBOHReceiptButton.isVisible = true
                        binding.paymentSuccessPart.tvSendReceiptPhone.isVisible = true
                        binding.paymentSuccessPart.tvSendReceiptEmail.isVisible = true
                        binding.paymentSuccessPart.horizontalView.isVisible = true
                    }
                    SendReceiptType.Nothing -> {
                        binding.paymentSuccessPart.userDetailsRelative.isVisible = true
                        binding.paymentSuccessPart.llButtons.isVisible = true
                        binding.paymentSuccessPart.printBOHReceiptButton.isVisible = true
                        binding.paymentSuccessPart.horizontalView.isVisible = true
                    }
                }
                binding.paymentSuccessPart.downArrowImageView.isSelected = true
            }
        }.autoDispose()

        binding.paymentSuccessPart.editEmail.throttleClicks().subscribeAndObserveOnMainThread {
            val editEmailOrPhoneFragment = EditEmailOrPhoneFragment.newInstance(EditType.Email, binding.paymentSuccessPart.tvEmail.text.toString())
            editEmailOrPhoneFragment.editSuccess.subscribeAndObserveOnMainThread {
                binding.paymentSuccessPart.tvEmail.text = it
                binding.paymentSuccessPart.tvSendReceiptEmail.text = it
            }.autoDispose()
            editEmailOrPhoneFragment.show(parentFragmentManager, PaymentFragment::class.java.name)
        }.autoDispose()

        binding.paymentSuccessPart.editPhone.throttleClicks().subscribeAndObserveOnMainThread {
            val editEmailOrPhoneFragment = EditEmailOrPhoneFragment.newInstance(EditType.Phone, binding.paymentSuccessPart.tvPhone.text.toString())
            editEmailOrPhoneFragment.editSuccess.subscribeAndObserveOnMainThread {
                binding.paymentSuccessPart.tvPhone.text = it
                binding.paymentSuccessPart.tvSendReceiptPhone.text = it
            }.autoDispose()
            editEmailOrPhoneFragment.show(parentFragmentManager, PaymentFragment::class.java.name)
        }.autoDispose()
        try {
            printer = Printer(Printer.TM_T88, Printer.MODEL_ANK, requireContext())
        } catch (e: Epos2Exception) {
            showToast(e.message.toString())
        }
        binding.paymentSuccessPart.printReceiptButton.throttleClicks().subscribeAndObserveOnMainThread {
            val cart = com.hotbox.terminal.api.order.model.CartGroup(
                promisedTime = createPosOrder?.cartGroup?.promisedTime,
                cart = createPosOrder?.cartGroup?.cart,
                cartCreatedDate = createPosOrder?.cartGroup?.cartCreatedDate,
                id = createPosOrder?.cartGroup?.id
            )
            val orderMode = com.hotbox.terminal.api.order.model.OrderMode(
                id = createPosOrder?.orderMode?.id, modeName = createPosOrder?.orderMode?.modeName
            )
            val orderType = com.hotbox.terminal.api.order.model.OrderType(
                id = createPosOrder?.orderType?.id, subcategory = createPosOrder?.orderType?.subcategory
            )
            val user = HealthNutUser(
                firstName = loggedInUserCache.getLoyaltyQrResponse()?.fullName,
                lastName = loggedInUserCache.getLoyaltyQrResponse()?.lastName,
            )
            var orderTotal = createPosOrder?.orderTotal
            if (orderTotal != null) {
                if (createPosOrder?.orderEmpDiscount != null) {
                    Timber.tag("OkHttpClient").i("Order Total : ${orderTotal.div(100).toDollar()}")
                    Timber.tag("OkHttpClient").i("orderEmpDiscount : ${createPosOrder?.orderEmpDiscount?.div(100).toDollar()}")
                    orderTotal = (orderTotal).minus(createPosOrder?.orderEmpDiscount ?: 0.00)
                    Timber.tag("OkHttpClient").i("Order Total : ${orderTotal.div(100).toDollar()}")
                }
                if (createPosOrder?.orderAdjustmentAmount != null) {
                    Timber.tag("OkHttpClient").i("Order Total : ${orderTotal.div(100).toDollar()}")
                    Timber.tag("OkHttpClient").i("orderAdjustmentAmount : ${createPosOrder?.orderAdjustmentAmount?.div(100).toDollar()}")
                    orderTotal = (orderTotal).plus(createPosOrder?.orderAdjustmentAmount ?: 0.00)
                    Timber.tag("OkHttpClient").i("Order Total : ${orderTotal.div(100).toDollar()}")
                }
            }
            val orderDetails = OrderDetailsResponse(
                id = createPosOrder?.id,
                cartGroup = cart,
                orderPromisedTime = createPosOrder?.orderPromisedTime,
                orderCreationDate = createPosOrder?.orderCreationDate,
                orderLocation = createPosOrder?.orderLocation,
                orderSubtotal = createPosOrder?.orderSubtotal?.toDouble(),
                orderTotal = orderTotal,
                orderTax = createPosOrder?.orderTax?.toDouble(),
                orderDeliveryFee = createPosOrder?.orderDeliveryFee?.toDouble(),
                orderTip = createPosOrder?.orderTip?.toDouble(),
                orderGiftCardAmount = createPosOrder?.orderGiftCardAmount?.toDouble(),
                orderCouponCodeDiscount = createPosOrder?.orderCouponCodeDiscount?.toDouble(),
                orderEmpDiscount = createPosOrder?.orderEmpDiscount,
                orderRefundAmount = createPosOrder?.orderRefundAmount,
                orderAdjustmentAmount = createPosOrder?.orderAdjustmentAmount,
                orderMode = orderMode,
                orderType = orderType,
                user = user,
                guest = arrayListOf()
            )
            val t: Thread = object : Thread() {
                override fun run() {
                    val timeZone = loggedInUserCache.getStoreResponse()?.locationLocationTimezone ?: "America/Los_Angeles"
                    val currentTime = getCurrentsStoreTime(timeZone).formatToStoreTime("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
                    PrintReceiptHelper(requireContext(), requireActivity()).runPrintReceiptSequence(
                        requireContext(), printer, orderDetails, loggedInUserCache, fohPrintAddress, currentTime
                    )
                }
            }
            t.start()

        }.autoDispose()
        binding.paymentSuccessPart.sendReceiptButton.throttleClicks().subscribeAndObserveOnMainThread {
            when {
                binding.paymentSuccessPart.emailCheckBox.isChecked && !binding.paymentSuccessPart.phoneCheckBox.isChecked -> {
                    binding.paymentSuccessPart.emailCheckBox.isChecked = false
                    isSendReceipt = SendReceiptType.Email
                    createPosOrder?.id?.let { it1 ->
                        userStoreViewModel.sendReceipt(
                            orderId = it1, type = EMAIL, email = binding.paymentSuccessPart.tvEmail.text.toString(), phone = null
                        )
                    }
                    binding.paymentSuccessPart.llButtons.isVisible = true
                    binding.paymentSuccessPart.printBOHReceiptButton.isVisible = true
                    binding.paymentSuccessPart.tvSendReceiptEmail.isVisible = true
                    binding.paymentSuccessPart.userDetailsRelative.isVisible = true
                    binding.paymentSuccessPart.emailCheckBox.isVisible = false
                    binding.paymentSuccessPart.tvEmail.isVisible = false
                    binding.paymentSuccessPart.editEmail.isVisible = false
                    binding.paymentSuccessPart.horizontalView.isVisible = true
                    if (!binding.paymentSuccessPart.emailCheckBox.isVisible && !binding.paymentSuccessPart.phoneCheckBox.isVisible) {
                        binding.paymentSuccessPart.sendReceiptButton.isVisible = false
                        binding.paymentSuccessPart.tvSendReceiptEmail.isVisible = true
                        binding.paymentSuccessPart.tvSendReceiptPhone.isVisible = true
                    }
                }
                binding.paymentSuccessPart.phoneCheckBox.isChecked && !binding.paymentSuccessPart.emailCheckBox.isChecked -> {
                    binding.paymentSuccessPart.phoneCheckBox.isChecked = false
                    isSendReceipt = SendReceiptType.Phone
                    binding.paymentSuccessPart.llButtons.isVisible = true
                    binding.paymentSuccessPart.printBOHReceiptButton.isVisible = true
                    binding.paymentSuccessPart.tvSendReceiptPhone.isVisible = true
                    binding.paymentSuccessPart.tvSendReceiptEmail.isVisible = false
                    binding.paymentSuccessPart.userDetailsRelative.isVisible = true
                    binding.paymentSuccessPart.phoneCheckBox.isVisible = false
                    binding.paymentSuccessPart.tvPhone.isVisible = false
                    binding.paymentSuccessPart.editPhone.isVisible = false
                    binding.paymentSuccessPart.horizontalView.isVisible = true
                    createPosOrder?.id?.let { it1 ->
                        userStoreViewModel.sendReceipt(
                            orderId = it1, type = PHONE, email = null, phone = binding.paymentSuccessPart.tvPhone.text.toString()
                        )
                    }
                    if (!binding.paymentSuccessPart.emailCheckBox.isVisible && !binding.paymentSuccessPart.phoneCheckBox.isVisible) {
                        binding.paymentSuccessPart.sendReceiptButton.isVisible = false
                        binding.paymentSuccessPart.tvSendReceiptEmail.isVisible = true
                        binding.paymentSuccessPart.tvSendReceiptPhone.isVisible = true
                    }
                }
                binding.paymentSuccessPart.emailCheckBox.isChecked && binding.paymentSuccessPart.phoneCheckBox.isChecked -> {
                    isSendReceipt = SendReceiptType.EmailAndPhone
                    binding.paymentSuccessPart.sendReceiptButton.isVisible = false
                    binding.paymentSuccessPart.userDetailsRelative.isVisible = false
                    binding.paymentSuccessPart.tvSendReceiptEmail.visibility = View.VISIBLE
                    binding.paymentSuccessPart.tvSendReceiptPhone.visibility = View.VISIBLE
                    createPosOrder?.id?.let { it1 ->
                        userStoreViewModel.sendReceipt(
                            orderId = it1, type = EMAIL, email = binding.paymentSuccessPart.tvEmail.text.toString(), phone = null
                        )
                    }
                    createPosOrder?.id?.let { it1 ->
                        userStoreViewModel.sendReceipt(
                            orderId = it1, type = PHONE, email = null, phone = binding.paymentSuccessPart.tvPhone.text.toString()
                        )
                    }
                }

                else -> {
                    showToast("Please Select Send Receipt type")
                }
            }

        }.autoDispose()

        binding.paymentSuccessPart.goToStartButton.throttleClicks().subscribeAndObserveOnMainThread {
            count = 0
            RxBus.publish(RxEvent.EventGotoStartButton)
        }
    }


    private fun apiCalling(toInt: Double) {
        val builder = provideOkHttpClient(requireContext())
        val retrofit: Retrofit =
            Retrofit.Builder().baseUrl("https://secure.nmi.com/").client(builder).addConverterFactory(ScalarsConverterFactory.create()).build()

        apiService = retrofit.create(ApiService::class.java)

        posAccessKey = loggedInUserCache.getLocationInfo()?.poskey ?: "73977412-fd0e-4c18-a6e3-000000000000"
        terminalAccessKey = loggedInUserCache.getLocationInfo()?.terminalkey ?: "vpac6dhpC65J2f5j54nZjhHPz73ANZfu"
        call = apiService.performSaleTransaction(posAccessKey, terminalAccessKey, toInt)

        call.enqueue(object : Callback<String?> {
            override fun onResponse(call: Call<String?>, response: Response<String?>) {
                if (response.isSuccessful) {
                    Timber.tag("API Calling").i("Success :${response.body()}")
                    paymentResponse = response.body().toString()
                    if (paymentResponse != null) {
                        responseChecker(paymentResponse)
                    }
                } else {
                    binding.pendingPayment.progressBar.isVisible = false
                    binding.pendingPayment.tryAgainMaterialButton.isVisible = true
                }
            }

            override fun onFailure(call: Call<String?>, t: Throwable) {
                binding.pendingPayment.progressBar.isVisible = false
                binding.pendingPayment.tryAgainMaterialButton.isVisible = true
                Timber.tag("API Calling").i("onFailure :$t")
                // Handle network failures or other errors
            }
        })
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

    private fun provideOkHttpClient(
        context: Context
    ): OkHttpClient {
        val cacheSize = 10 * 1024 * 1024 // 10 MiB
        val cacheDir = File(context.cacheDir, "HttpCache")
        val cache = Cache(cacheDir, cacheSize.toLong())
        val builder =
            OkHttpClient.Builder().readTimeout(300, TimeUnit.SECONDS).connectTimeout(300, TimeUnit.SECONDS).writeTimeout(300, TimeUnit.SECONDS)
                .cache(cache)
        if (isDebugMode()) {
            val loggingInterceptor = HttpLoggingInterceptor()
            loggingInterceptor.level = HttpLoggingInterceptor.Level.BODY
            builder.addInterceptor(loggingInterceptor)
        }
        return builder.build()
    }

    fun responseChecker(response: String) {
        val keyValuePairs: List<String> = response.split("&")

        var responseText: String? = null
        var tip: String? = null
        var orderId: String? = null
        var authCode: String? = null
        var transactionId: String? = null
        for (keyValuePair in keyValuePairs) {
            val parts = keyValuePair.split("=".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            if (parts.size == 2) {
                val key = parts[0]
                val value = parts[1]
                if (key == "responsetext") {
                    responseText = value
                }
                if (key == "tip") {
                    tip = value
                }
                if (key == "orderid") {
                    orderId = value
                }
                if (key == "authcode") {
                    authCode = value
                }

                if (key == "transactionid") {
                    transactionId = value
                }
            }
        }
        if (responseText != null && responseText == "Approved") {
            val posAccessKey = loggedInUserCache.getLocationInfo()?.poskey ?: "73977412-fd0e-4c18-a6e3-000000000000"
            createOrderRequest = CreateOrderRequest(
                userId = if (loggedInUserCache.getLoyaltyQrResponse()?.id != null && loggedInUserCache.getLoyaltyQrResponse()?.id != "") loggedInUserCache.getLoyaltyQrResponse()?.id else null,
                MODE_ID,
                orderTip = tip?.toDouble()?.times(100)?.toInt(),
                orderTypeId = loggedInUserCache.getorderTypeId(),
                orderCartGroupId = loggedInUserCache.getLoggedInUserCartGroupId(),
                orderLocationId = loggedInUserCache.getLocationInfo()?.location?.id,
                deliveryAddress = null,
                guestFirstName = if (loggedInUserCache.getLoyaltyQrResponse()?.fullName?.isNotEmpty() == true) loggedInUserCache.getLoyaltyQrResponse()?.fullName else null,
                guestLastName = if (loggedInUserCache.getLoyaltyQrResponse()?.lastName?.isNotEmpty() == true) loggedInUserCache.getLoyaltyQrResponse()?.lastName else null,
                guestPhone = (if (loggedInUserCache.getLoyaltyQrResponse()?.phone?.isNotEmpty() == true) loggedInUserCache.getLoyaltyQrResponse()?.phone.toString() else "N/A"),
                giftCardId = giftCardId,
                orderGiftCardAmount = enterGiftCardPrize,
                couponCodeId = couponCodeId,
                creditAmount = creditAmount,
                guestEmail = if (loggedInUserCache.getLoyaltyQrResponse()?.email?.isNotEmpty() == true) loggedInUserCache.getLoyaltyQrResponse()?.email.toString() else null,
                transactionTerminal = posAccessKey,
                transactionIdOfProcessor = transactionId,
                transactionChargeId = authCode,
                orderTotal = if (total.times(100).toInt() < 0) 0 else total.times(100).toInt(),
                orderSubtotal = if (subTotal < 0) 0 else subTotal,
                transactionTotalAmount = total.times(100).toInt(),
                orderAdjustmentAmount = if (adjustment != 0) adjustment else null
//                transactionReceiptUrl = orderId
            )

            userStoreViewModel.createOrder(
                createOrderRequest
            )
        } else {
            binding.pendingPayment.progressBar.isVisible = false
            binding.pendingPayment.tryAgainMaterialButton.isVisible = true
            println("Response is not Approved")
        }
    }

    private fun successUi() {
        if (!loggedInUserCache.getLoyaltyQrResponse()?.email?.trim().isNullOrEmpty() || !loggedInUserCache.getLoyaltyQrResponse()?.phone?.trim()
                .isNullOrEmpty()
        ) {
            binding.paymentSuccessPart.rlSendReceipt.isVisible = true
            if (loggedInUserCache.getLoyaltyQrResponse()?.email?.trim()?.isNotEmpty() == true) {
                binding.paymentSuccessPart.tvEmail.isVisible = true
                binding.paymentSuccessPart.emailCheckBox.isVisible = true
                binding.paymentSuccessPart.tvSendReceiptEmail.isVisible = false
                binding.paymentSuccessPart.tvSendReceiptEmail.text = loggedInUserCache.getLoyaltyQrResponse()?.email
                binding.paymentSuccessPart.tvEmail.text = loggedInUserCache.getLoyaltyQrResponse()?.email
            }
            if (loggedInUserCache.getLoyaltyQrResponse()?.phone?.trim()?.isNotEmpty() == true) {
                binding.paymentSuccessPart.tvPhone.isVisible = true
                binding.paymentSuccessPart.phoneCheckBox.isVisible = true
                binding.paymentSuccessPart.tvSendReceiptPhone.isVisible = false
                binding.paymentSuccessPart.tvSendReceiptPhone.text = loggedInUserCache.getLoyaltyQrResponse()?.phone
                binding.paymentSuccessPart.tvPhone.text = loggedInUserCache.getLoyaltyQrResponse()?.phone
            }
        } else {
            if (loggedInUserCache.getLoyaltyQrResponse()?.email?.trim()?.isNullOrEmpty() == true) {
                binding.paymentSuccessPart.tvEmail.isVisible = true
                binding.paymentSuccessPart.emailCheckBox.isVisible = true
                binding.paymentSuccessPart.tvSendReceiptEmail.isVisible = false
                binding.paymentSuccessPart.tvSendReceiptEmail.text = "-"
                binding.paymentSuccessPart.tvEmail.text = "-"
            } else {
                binding.paymentSuccessPart.tvEmail.isVisible = true
                binding.paymentSuccessPart.emailCheckBox.isVisible = true
                binding.paymentSuccessPart.tvSendReceiptEmail.isVisible = false
                binding.paymentSuccessPart.tvSendReceiptEmail.text = loggedInUserCache.getLoyaltyQrResponse()?.email
                binding.paymentSuccessPart.tvEmail.text = loggedInUserCache.getLoyaltyQrResponse()?.email
            }
            if (loggedInUserCache.getLoyaltyQrResponse()?.phone?.trim()?.isNullOrEmpty() == true) {
                binding.paymentSuccessPart.tvPhone.isVisible = true
                binding.paymentSuccessPart.phoneCheckBox.isVisible = true
                binding.paymentSuccessPart.tvSendReceiptPhone.isVisible = false
                binding.paymentSuccessPart.tvSendReceiptPhone.text = "-"
                binding.paymentSuccessPart.tvPhone.text = "-"
            } else {
                binding.paymentSuccessPart.tvPhone.isVisible = true
                binding.paymentSuccessPart.phoneCheckBox.isVisible = true
                binding.paymentSuccessPart.tvSendReceiptPhone.isVisible = false
                binding.paymentSuccessPart.tvSendReceiptPhone.text = loggedInUserCache.getLoyaltyQrResponse()?.phone
                binding.paymentSuccessPart.tvPhone.text = loggedInUserCache.getLoyaltyQrResponse()?.phone
            }
        }
    }

    override fun onResume() {
        super.onResume()
        count = 0
    }

    override fun onPause() {
        super.onPause()
//        userStoreViewModel.cancelAction()
    }

    override fun onStart() {
        super.onStart()
        UserInteractionInterceptor.wrapWindowCallback(requireActivity().window, activity)
    }

}