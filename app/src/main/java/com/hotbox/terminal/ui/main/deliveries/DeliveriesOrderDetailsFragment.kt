package com.hotbox.terminal.ui.main.deliveries

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.icu.text.SimpleDateFormat
import android.icu.util.TimeZone
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import com.hotbox.terminal.R
import com.hotbox.terminal.api.authentication.LoggedInUserCache
import com.hotbox.terminal.api.order.model.*
import com.hotbox.terminal.application.HotBoxApplication
import com.hotbox.terminal.base.BaseFragment
import com.hotbox.terminal.base.RxBus
import com.hotbox.terminal.base.RxEvent
import com.hotbox.terminal.base.ViewModelFactory
import com.hotbox.terminal.base.extension.*
import com.hotbox.terminal.databinding.FragmentDeliveriesOrderDetailsBinding
import com.hotbox.terminal.helper.*
import com.hotbox.terminal.ui.main.orderdetail.PrintReceiptDialog
import com.hotbox.terminal.ui.main.orderdetail.RefundFragmentDialog
import com.hotbox.terminal.ui.main.orderdetail.SendReceiptDialogFragment
import com.hotbox.terminal.ui.main.orderdetail.view.OrderDetailsAdapter
import com.hotbox.terminal.ui.main.orderdetail.view.StatusLogAdapter
import com.hotbox.terminal.ui.main.orderdetail.viewmodel.OrderDetailsViewModel
import com.hotbox.terminal.ui.main.orderdetail.viewmodel.OrderDetailsViewState
import timber.log.Timber
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.math.abs
import kotlin.properties.Delegates

class DeliveriesOrderDetailsFragment : BaseFragment() {


    companion object {
        private const val ORDER_ID = "orderId"
        private const val FROM_ORDER = "fromOrder"

        @JvmStatic
        fun newInstance(orderId: Int?, fromOrder: Boolean? = false): DeliveriesOrderDetailsFragment {
            val args = Bundle()
            orderId?.let { args.putInt(ORDER_ID, it) }
            fromOrder?.let { args.putBoolean(FROM_ORDER, it) }
            val fragment = DeliveriesOrderDetailsFragment()
            fragment.arguments = args
            return fragment
        }
    }

    private var _binding: FragmentDeliveriesOrderDetailsBinding? = null
    private val binding get() = _binding!!
    private lateinit var statusLogAdapter: StatusLogAdapter
    private lateinit var orderDetailsAdapter: OrderDetailsAdapter
    private var orderId by Delegates.notNull<Int>()
    private var fromOrder: Boolean? = false
    private var orderStatus: String = ""

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<OrderDetailsViewModel>
    private lateinit var orderDetailsViewModel: OrderDetailsViewModel

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        orderId = arguments?.getInt(ORDER_ID, 0) ?: throw IllegalStateException("No args provided")
        fromOrder = arguments?.getBoolean(FROM_ORDER, false) ?: throw IllegalStateException("No args provided")
        HotBoxApplication.component.inject(this)
        orderDetailsViewModel = getViewModelFromFactory(viewModelFactory)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDeliveriesOrderDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        listenToViewModel()
        listenToViewEvent()
    }

    private fun listenToViewEvent() {
        initAdapter()
        binding.orderDetailsHeaderLayout.backImageView.throttleClicks().subscribeAndObserveOnMainThread {
            onBackPressed()
        }.autoDispose()
        binding.customerDetails.customerLocationAppCompatTextView.isVisible = false
        binding.orderDetailsHeaderLayout.assignDriverButton.throttleClicks().subscribeAndObserveOnMainThread {
            orderStatus = resources.getString(R.string.cancelled).toLowerCase()
            if (orderStatus.isNotEmpty()) {
                cancelOrderDialog()
            }
        }.autoDispose()
        binding.orderDetailsHeaderLayout.receivedOrderButton.throttleClicks().subscribeAndObserveOnMainThread {
            orderStatus = resources.getString(R.string.received).toLowerCase()
            orderDetailsViewModel.updateOrderStatusDetails(orderStatus.lowercase(), orderId)
            binding.orderDetailsHeaderLayout.receivedOrderButton.isVisible = false
        }.autoDispose()
        binding.mapDetails.zoomMapAppCompatTextView.throttleClicks().subscribeAndObserveOnMainThread {
            val zoomMapDialogFragment = ZoomMapDialogFragment()
            zoomMapDialogFragment.show(parentFragmentManager, DeliveriesFragment::class.java.name)
        }.autoDispose()
    }


    private fun cancelOrderDialog() {
        val alertDialog = AlertDialog.Builder(requireContext()).setTitle(resources.getText(R.string.cancel_order))
            .setMessage(resources.getText(R.string.are_you_sure_cancel_and_clear))
            .setNegativeButton(resources.getText(R.string.label_no)) { dialog, _ ->
                dialog.dismiss()
            }.setPositiveButton(resources.getText(R.string.label_yes)) { _, _ ->
                orderDetailsViewModel.updateOrderStatusDetails(orderStatus.lowercase(), orderId)
                binding.orderDetailsHeaderLayout.assignDriverButton.isVisible = false
            }.show()
        alertDialog.window?.setLayout(800, WindowManager.LayoutParams.WRAP_CONTENT)
    }

    private fun listenToViewModel() {
        orderDetailsViewModel.orderDetailsState.subscribeAndObserveOnMainThread {
            when (it) {
                is OrderDetailsViewState.StatusResponse -> {
//                    setData(it.statusLogInfo)
                }
                is OrderDetailsViewState.ErrorMessage -> {
                    showToast(it.errorMessage)
                }
                is OrderDetailsViewState.LoadingState -> {

                }
                is OrderDetailsViewState.SuccessMessage -> {
                    showToast(it.successMessage)
                }
                is OrderDetailsViewState.UpdateStatusResponse -> {
                    if (it.updatedOrderStatusResponse.orderStatus == getString(R.string.refunded)) {
                        binding.orderDetailsHeaderLayout.refundButton.isVisible = false
                    }
                    orderDetailsViewModel.loadOrderDetailsItem(orderId)
                }
                is OrderDetailsViewState.CustomerDetails -> {
                    binding.customerDetails.customerNameAppCompatTextView.text = it.customerDetails.fullName()
                    binding.customerDetails.customerPhoneNumberAppCompatTextView.text = it.customerDetails.userPhone
                    binding.customerDetails.customerEmailAppCompatTextView.text = it.customerDetails.userEmail
                }
                is OrderDetailsViewState.OrderDetailItemResponse -> {
                    initUI(it.orderDetails)
                }
                is OrderDetailsViewState.SendReceiptSuccessMessage -> {
                    showToast(it.successMessage)
                }
                else -> {}
            }
        }.autoDispose()
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private fun initUI(orderDetails: OrderDetailsResponse) {
        orderDetails.orderInstructions?.let {
            binding.orderListLayout.specialTextLinear.isVisible = true
            binding.orderListLayout.specialInstructionsTextView.text = it.toString()
        }
        if (!orderDetails.status.isNullOrEmpty()) {
            setStatusData(orderDetails.status)
        }

        setOrderDetailsData(orderDetails.cartGroup?.cart)
        if (!orderDetails.guest.isNullOrEmpty()) {
            binding.customerDetails.customerNameAppCompatTextView.text = orderDetails.guest.first()?.fullName()
            binding.customerDetails.customerPhoneNumberAppCompatTextView.text = orderDetails.guest.first()?.guestPhone ?: "N/A"
            binding.customerDetails.customerEmailAppCompatTextView.text = orderDetails.guest.first()?.guestEmail ?: "-"
            binding.customerDetails.customerLocationAppCompatTextView.text = "-"
        } else {
            if (orderDetails.user != null) {
                binding.customerDetails.customerNameAppCompatTextView.text = orderDetails.user.fullName()
                binding.customerDetails.customerPhoneNumberAppCompatTextView.text = orderDetails.user.userPhone
                binding.customerDetails.customerEmailAppCompatTextView.text = orderDetails.user.userEmail
                binding.customerDetails.customerLocationAppCompatTextView.text = "-"
            }
        }

        if (orderDetails.delivery?.isNotEmpty() == true) {
            binding.mapDetails.root.isVisible = true
            if (!orderDetails.delivery.firstOrNull()?.orderDeliveryAddress.isNullOrEmpty()) {
                binding.mapDetails.tvDestination.isVisible = true
                binding.mapDetails.tvHeadingDestination.isVisible = true
                binding.mapDetails.tvDestination.text = orderDetails.delivery.firstOrNull()?.orderDeliveryAddress
            }
            if (!orderDetails.delivery.firstOrNull()?.orderDeliveryLocation.isNullOrEmpty()) {
                binding.mapDetails.deliveryLocationAppCompatTextView.isVisible = true
                binding.mapDetails.tvHeadingDeliveryLocation.isVisible = true
                binding.mapDetails.ivDelivery.isVisible = true
                binding.mapDetails.deliveryLocationAppCompatTextView.text = orderDetails.delivery.firstOrNull()?.orderDeliveryLocation
            }
            if (orderDetails.delivery.firstOrNull()?.orderDeliveryInstructions?.trim()?.isNotEmpty() == true) {
                binding.mapDetails.tvMessageToDriver.isVisible = true
                binding.mapDetails.tvHeadingMessageToDriver.isVisible = true
                binding.mapDetails.tvMessageToDriver.text = orderDetails.delivery.firstOrNull()?.orderDeliveryInstructions ?: "-"
            }
            val desiredTimeZone = java.util.TimeZone.getTimeZone("America/Los_Angeles")
            java.util.TimeZone.setDefault(desiredTimeZone)
            if (!orderDetails.delivery.firstOrNull()?.orderDeliveryEstimatedPickup.isNullOrEmpty()) {
                binding.mapDetails.tvEstimatedPickup.isVisible = true
                binding.mapDetails.tvHeadingEstimatedPickup.isVisible = true
                val timeZone = loggedInUserCache.getStoreResponse()?.locationLocationTimezone ?: "America/Los_Angeles"
                val orderDeliveryEstimatedPickup = orderDetails.delivery.firstOrNull()?.orderDeliveryEstimatedPickup?.toStoreTimeZoneDate(
                    "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"
                )
                val storeCurrentTime = getCurrentsStoreTime(timeZone).formatToStoreTime("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").toDate("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
                val diff: Long? = (storeCurrentTime?.time?.let { orderDeliveryEstimatedPickup?.time?.minus(it) })

                val differnce: Long? = diff?.let { it1 -> TimeUnit.MINUTES.convert(it1, TimeUnit.MILLISECONDS) }
                if (differnce != null) {
                    if (differnce > 0) {
                        if (differnce > 59) {
                            val hours: Long = differnce / 60 // since both are ints, you get an int
                            val minutes: Long = differnce % 60
                            if (minutes.toString() != "0") binding.mapDetails.tvEstimatedPickup.text =
                                hours.toString().plus(" Hours ").plus(minutes.toString().plus(" Minutes")) else hours.toString().plus(" Hours ")
                        } else {
                            binding.mapDetails.tvEstimatedPickup.text = differnce.toString().plus(" Minutes")
                        }
                    } else {
                        binding.mapDetails.tvEstimatedPickup.text = "---"
                    }
                }
            }
            if (!orderDetails.delivery.firstOrNull()?.orderDeliveryEstimatedDropoff.isNullOrEmpty()) {
                binding.mapDetails.tvEstimatedDelivery.isVisible = true
                binding.mapDetails.tvHeadingEstimatedDelivery.isVisible = true
                val timeZone = loggedInUserCache.getStoreResponse()?.locationLocationTimezone ?: "America/Los_Angeles"
                val orderDeliveryEstimatedDropoff = orderDetails.delivery.firstOrNull()?.orderDeliveryEstimatedDropoff?.toStoreTimeZoneDate(
                    "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"
                )
                val storeCurrentTime = getCurrentsStoreTime(timeZone).formatToStoreTime("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").toDate("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",TimeZone.getTimeZone(timeZone))
                val diff: Long? = (storeCurrentTime?.time?.let { orderDeliveryEstimatedDropoff?.time?.minus(it) })
                val differnce: Long? = diff?.let { it1 -> TimeUnit.MINUTES.convert(it1, TimeUnit.MILLISECONDS) }
                if (differnce != null) {
                    if (differnce > 0) {
                        if (differnce > 59) {
                            val hours: Long = differnce / 60 // since both are ints, you get an int

                            val minutes: Long = differnce % 60
                            if (minutes.toString() != "0") binding.mapDetails.tvEstimatedDelivery.text =
                                hours.toString().plus(" Hours ").plus(minutes.toString().plus(" Minutes")) else hours.toString().plus(" Hours ")
                        } else {
                            binding.mapDetails.tvEstimatedDelivery.text = differnce.toString().plus(" Minutes")
                        }
                    } else {
                        binding.mapDetails.tvEstimatedDelivery.text = "---"
                    }
                }
            }
            if (!orderDetails.delivery.firstOrNull()?.orderDeliveryEstimatedPickup.isNullOrEmpty()) {
                binding.mapDetails.tvHeadingTrackingLink.isVisible = true
                binding.mapDetails.tvTrackingLink.isVisible = true
                binding.mapDetails.ivOrderTracking.isVisible = true
                binding.mapDetails.tvTrackingLink.text = orderDetails.delivery.firstOrNull()?.orderDeliveryUrl
            }
        } else {
            binding.mapDetails.root.isVisible = false
        }
        orderDetails.orderTotal = orderDetails.orderTotal?.minus((orderDetails.orderEmpDiscount ?: 0.00))
        orderDetails.orderTotal = orderDetails.orderTotal?.minus((orderDetails.orderRefundAmount ?: 0.00))
        orderDetails.orderTotal = orderDetails.orderTotal?.plus((orderDetails.orderAdjustmentAmount?.let { abs(it) } ?: 0.00))
        orderDetails.orderTotal?.let {
            binding.orderDetailsHeaderLayout.orderPrizeTextView.text = ((it).div(100)).toDollar()
            binding.orderListLayout.deliveryOrderPrizeLayout.tvTotalPrizeNumber.text = ((it).div(100)).toDollar()
        }
        orderDetails.orderSubtotal?.let {
            binding.orderListLayout.deliveryOrderPrizeLayout.tvOrderPrizeNumber.text = ((it).div(100)).toDollar()

        }
        orderDetails.orderTax?.let {
            if (!it.equals(0.0)) {
                binding.orderListLayout.deliveryOrderPrizeLayout.orderTaxRelativeLayout.isVisible = true
                binding.orderListLayout.deliveryOrderPrizeLayout.tvTaxPrize.text = ((it).div(100)).toDollar()
            }
        }
        orderDetails.orderTip?.let {
            if (!it.equals(0.0)) {
                binding.orderListLayout.deliveryOrderPrizeLayout.orderTipRelativeLayout.isVisible = true
                binding.orderListLayout.deliveryOrderPrizeLayout.tvTipsPrize.text = ((it).div(100)).toDollar()
            }
        }
        orderDetails.orderDeliveryFee?.let {
            if (!it.equals(0.0)) {
                binding.orderListLayout.deliveryOrderPrizeLayout.orderDeliveryRelativeLayout.isVisible = true
                binding.orderListLayout.deliveryOrderPrizeLayout.tvDeliveryCharge.text = ((it).div(100)).toDollar()
            }
        }
        orderDetails.orderRefundAmount?.let {
            if (it != 0.00) {
                binding.orderListLayout.deliveryOrderPrizeLayout.rlRefund.isVisible = true
                binding.orderListLayout.deliveryOrderPrizeLayout.tvRefundAmount.text = "-${it.div(100).toDollar()}"
            }
        }
        orderDetails.orderEmpDiscount?.let {
            if (!it.equals(0.0)) {
                binding.orderListLayout.deliveryOrderPrizeLayout.orderEmployeeDiscountRelativeLayout.isVisible = true
                binding.orderListLayout.deliveryOrderPrizeLayout.tvEmployeeDiscountPrize.text = "-${((it).div(100)).toMinusDollar().toDollar()}"
            }
        }
        orderDetails.orderCouponCodeDiscount?.let {
            if (!it.equals(0.0)) {
                binding.orderListLayout.deliveryOrderPrizeLayout.orderPromocodeRelativeLayout.isVisible = true
                binding.orderListLayout.deliveryOrderPrizeLayout.tvPromocodeDiscountPrize.text = "-${((it).div(100)).toMinusDollar().toDollar()}"
            }
        }
        orderDetails.orderGiftCardAmount?.let {
            if (!it.equals(0.0)) {
                binding.orderListLayout.deliveryOrderPrizeLayout.orderCardAndBowRelativeLayout.isVisible = true
                binding.orderListLayout.deliveryOrderPrizeLayout.tvCardAndBowCharge.text = "-${((it).div(100)).toMinusDollar().toDollar()}"
            }
        }
        orderDetails.orderCreditAmount?.let {
            if (!it.equals(0.0)) {
                binding.orderListLayout.deliveryOrderPrizeLayout.rlCredit.isVisible = true
                binding.orderListLayout.deliveryOrderPrizeLayout.tvCreditAmount.text = "-${((it).div(100)).toMinusDollar().toDollar()}"
            }
        }
        orderDetails.orderAdjustmentAmount?.let {
            if (!it.equals(0.0)) {
                if (it > 0) {
                    binding.orderListLayout.deliveryOrderPrizeLayout.rlAdjustment.isVisible = true
                    binding.orderListLayout.deliveryOrderPrizeLayout.tvAdjustmentCharge.text = (it).div(100).toDollar()
                } else {
                    binding.orderListLayout.deliveryOrderPrizeLayout.rlAdjustment.isVisible = true
                    binding.orderListLayout.deliveryOrderPrizeLayout.tvAdjustmentCharge.text = "-".plus((abs(it).div(100)).toDollar())
                }
            }
        }
        binding.orderDetailsHeaderLayout.orderIdTextView.text = orderDetails.getSafeOrderId()
        binding.orderDetailsHeaderLayout.orderStatusTextview.text = orderDetails.orderType?.subcategory.toString()
        val timeZone = loggedInUserCache.getStoreResponse()?.locationLocationTimezone ?: "America/Los_Angeles"
        binding.orderDetailsHeaderLayout.orderDateAndTimeTextView.text = orderDetails.orderCreationDate?.toStoreTimeZoneDate("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")?.formatTo("MM/dd/yyyy, hh:mm a", TimeZone.getTimeZone(timeZone))
        binding.orderListLayout.timeTextView.text = orderDetails.orderPromisedTime?.toDate("yyyy-MM-dd hh:mm a")?.formatTo("MM/dd/yyyy, hh:mm a")
        binding.customerDetails.btnPrintReceipt.throttleClicks().subscribeAndObserveOnMainThread {
            val printReceiptDialog = PrintReceiptDialog.newInstance(orderDetails).apply {
                printReceiptDismissed.subscribeAndObserveOnMainThread {
                    dismiss()
                }.autoDispose()
            }
            printReceiptDialog.show(parentFragmentManager, PrintReceiptDialog::class.java.name)
        }.autoDispose()
        binding.customerDetails.btnSendReceipt.throttleClicks().subscribeAndObserveOnMainThread {
            val sendReceiptDialogFragment = SendReceiptDialogFragment.newInstance(
                binding.customerDetails.customerEmailAppCompatTextView.text.toString(),
                binding.customerDetails.customerPhoneNumberAppCompatTextView.text.toString()
            ).apply {
                refundDialogState.subscribeAndObserveOnMainThread {
                    when (it) {
                        is SendReceiptStates.SendReceiptOnEmail -> {
                            dismiss()
                            orderDetails.id?.let { it1 -> orderDetailsViewModel.sendReceipt(it1, "Email", it.email, null) }
                            binding.customerDetails.customerEmailAppCompatTextView.text = it.email
                        }
                        is SendReceiptStates.SendReceiptOnPhone -> {
                            dismiss()
                            orderDetails.id?.let { it1 -> orderDetailsViewModel.sendReceipt(it1, "Phone", null, it.phone) }
                            binding.customerDetails.customerPhoneNumberAppCompatTextView.text = it.phone
                        }
                        is SendReceiptStates.SendReceiptOnPhoneAndEmail -> {
                            dismiss()
                            orderDetails.id?.let { it1 -> orderDetailsViewModel.sendReceipt(it1, "Email", it.email, null) }
                            orderDetails.id?.let { it1 -> orderDetailsViewModel.sendReceipt(it1, "Phone", null, it.phone) }
                            binding.customerDetails.customerPhoneNumberAppCompatTextView.text = it.phone
                            binding.customerDetails.customerEmailAppCompatTextView.text = it.email
                        }
                    }
                }.autoDispose()
            }
            sendReceiptDialogFragment.show(parentFragmentManager, SendReceiptDialogFragment::class.java.name)
        }.autoDispose()
        if (orderDetails.status?.first()?.orderStatus != resources.getString(R.string.new_text)
                .toLowerCase() && loggedInUserCache.isAdmin() && orderDetails.orderTotal != 0.00 && orderDetails.status?.first()?.orderStatus != resources.getString(
                R.string.refunded
            ).toLowerCase()
        ) {
            binding.orderDetailsHeaderLayout.refundButton.isVisible = true
        }
        binding.orderDetailsHeaderLayout.refundButton.throttleClicks().subscribeAndObserveOnMainThread {
            val printReceiptDialog = RefundFragmentDialog.newInstance(orderDetails).apply {
                refundDialogState.subscribeAndObserveOnMainThread {
                    when (it) {
                        is RefundDialogStates.DismissedRefundDialog -> {
                            this.dismiss()
                        }
                        is RefundDialogStates.GetRefund -> {
                            this.dismiss()
                            orderDetailsViewModel.loadOrderDetailsItem(orderId)
                            binding.orderDetailsHeaderLayout.refundButton.isVisible = false
//                            orderDetailsViewModel.updateOrderStatusDetails(resources.getString(R.string.refunded).toLowerCase(), orderId)
                        }
                    }
                }.autoDispose()
            }
            printReceiptDialog.show(parentFragmentManager, PrintReceiptDialog::class.java.name)
        }.autoDispose()
        if (!orderDetails.status.isNullOrEmpty()) {
            when (orderDetails.status?.first()?.orderStatus) {
                resources.getString(R.string.new_text).lowercase() -> {
                    binding.orderDetailsHeaderLayout.assignDriverButton.setText(R.string.cancel_order)
                    orderStatus = resources.getString(R.string.received).toLowerCase()
                    binding.orderDetailsHeaderLayout.receivedOrderButton.isVisible = true
                    binding.orderDetailsHeaderLayout.assignDriverButton.isVisible = true
                    binding.orderDetailsHeaderLayout.assignDriverButton.icon = resources.getDrawable(R.drawable.ic_complete_order_icon)
                    binding.orderDetailsHeaderLayout.newDotSelect.isVisible = true
                    binding.orderDetailsHeaderLayout.receivedDotSelect.isVisible = false
                    binding.orderDetailsHeaderLayout.packagingDotSelect.isVisible = false
                    binding.orderDetailsHeaderLayout.assignDotSelect.isVisible = false
                    binding.orderDetailsHeaderLayout.dispatchedDotSelect.isVisible = false
                    binding.orderDetailsHeaderLayout.deliveredDotSelect.isVisible = false
                    binding.orderDetailsHeaderLayout.newTextView.setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
                    binding.orderDetailsHeaderLayout.receivedTextView.setTextColor(ContextCompat.getColor(requireContext(), R.color.grey_999999))
                    binding.orderDetailsHeaderLayout.packagingTextView.setTextColor(ContextCompat.getColor(requireContext(), R.color.grey_999999))
                    binding.orderDetailsHeaderLayout.assignedAppCompatTextView.setTextColor(
                        ContextCompat.getColor(
                            requireContext(), R.color.grey_999999
                        )
                    )
                    binding.orderDetailsHeaderLayout.dispatchedAppCompatTextView.setTextColor(
                        ContextCompat.getColor(
                            requireContext(), R.color.grey_999999
                        )
                    )
                    binding.orderDetailsHeaderLayout.deliveredAppCompatTextView.setTextColor(
                        ContextCompat.getColor(
                            requireContext(), R.color.grey_999999
                        )
                    )
                }
                resources.getString(R.string.received).lowercase() -> {
                    orderStatus = resources.getString(R.string.cancelled)
                    binding.orderDetailsHeaderLayout.assignDriverButton.setText(R.string.cancel_order)
                    binding.orderDetailsHeaderLayout.assignDriverButton.isVisible = true
                    binding.orderDetailsHeaderLayout.receivedOrderButton.isVisible = false
                    binding.orderDetailsHeaderLayout.assignDriverButton.icon = resources.getDrawable(R.drawable.ic_complete_order_icon)
                    binding.orderDetailsHeaderLayout.newDotSelect.isVisible = false
                    binding.orderDetailsHeaderLayout.receivedDotSelect.isVisible = true
                    binding.orderDetailsHeaderLayout.packagingDotSelect.isVisible = false
                    binding.orderDetailsHeaderLayout.assignDotSelect.isVisible = false
                    binding.orderDetailsHeaderLayout.dispatchedDotSelect.isVisible = false
                    binding.orderDetailsHeaderLayout.deliveredDotSelect.isVisible = false
                    binding.orderDetailsHeaderLayout.newTextView.setTextColor(ContextCompat.getColor(requireContext(), R.color.grey_999999))
                    binding.orderDetailsHeaderLayout.receivedTextView.setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
                    binding.orderDetailsHeaderLayout.packagingTextView.setTextColor(ContextCompat.getColor(requireContext(), R.color.grey_999999))
                    binding.orderDetailsHeaderLayout.assignedAppCompatTextView.setTextColor(
                        ContextCompat.getColor(
                            requireContext(), R.color.grey_999999
                        )
                    )
                    binding.orderDetailsHeaderLayout.dispatchedAppCompatTextView.setTextColor(
                        ContextCompat.getColor(
                            requireContext(), R.color.grey_999999
                        )
                    )
                    binding.orderDetailsHeaderLayout.deliveredAppCompatTextView.setTextColor(
                        ContextCompat.getColor(
                            requireContext(), R.color.grey_999999
                        )
                    )
                }
                resources.getString(R.string.cancelled).lowercase() -> {
                    binding.orderDetailsHeaderLayout.assignDriverButton.isVisible = false
                    binding.orderDetailsHeaderLayout.receivedOrderButton.isVisible = false
                    binding.orderDetailsHeaderLayout.newDotSelect.isVisible = false
                    binding.orderDetailsHeaderLayout.receivedDotSelect.isVisible = false
                    binding.orderDetailsHeaderLayout.packagingDotSelect.isVisible = false
                    binding.orderDetailsHeaderLayout.assignDotSelect.isVisible = false
                    binding.orderDetailsHeaderLayout.dispatchedDotSelect.isVisible = false
                    binding.orderDetailsHeaderLayout.deliveredDotSelect.isVisible = false
                    binding.orderDetailsHeaderLayout.cancelledTextView.isVisible = true
                    binding.orderDetailsHeaderLayout.cancelledDotSelect.isVisible = true
                    binding.orderDetailsHeaderLayout.cancelledView.isVisible = true
                    binding.orderDetailsHeaderLayout.newTextView.setTextColor(ContextCompat.getColor(requireContext(), R.color.grey_999999))
                    binding.orderDetailsHeaderLayout.receivedTextView.setTextColor(ContextCompat.getColor(requireContext(), R.color.grey_999999))
                    binding.orderDetailsHeaderLayout.packagingTextView.setTextColor(ContextCompat.getColor(requireContext(), R.color.grey_999999))
                    binding.orderDetailsHeaderLayout.assignedAppCompatTextView.setTextColor(
                        ContextCompat.getColor(
                            requireContext(), R.color.grey_999999
                        )
                    )
                    binding.orderDetailsHeaderLayout.dispatchedAppCompatTextView.setTextColor(
                        ContextCompat.getColor(
                            requireContext(), R.color.grey_999999
                        )
                    )
                    binding.orderDetailsHeaderLayout.deliveredAppCompatTextView.setTextColor(
                        ContextCompat.getColor(
                            requireContext(), R.color.grey_999999
                        )
                    )
                    binding.orderDetailsHeaderLayout.cancelledTextView.setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
                }
                resources.getString(R.string.refunded).lowercase() -> {
                    binding.orderDetailsHeaderLayout.assignDriverButton.isVisible = false
                    binding.orderDetailsHeaderLayout.receivedOrderButton.isVisible = false
                    binding.orderDetailsHeaderLayout.newDotSelect.isVisible = false
                    binding.orderDetailsHeaderLayout.receivedDotSelect.isVisible = false
                    binding.orderDetailsHeaderLayout.packagingDotSelect.isVisible = false
                    binding.orderDetailsHeaderLayout.assignDotSelect.isVisible = false
                    binding.orderDetailsHeaderLayout.dispatchedDotSelect.isVisible = false
                    binding.orderDetailsHeaderLayout.deliveredDotSelect.isVisible = false
                    binding.orderDetailsHeaderLayout.cancelledTextView.isVisible = true
                    binding.orderDetailsHeaderLayout.cancelledTextView.text = resources.getString(R.string.refunded)
                    binding.orderDetailsHeaderLayout.cancelledDotSelect.isVisible = true
                    binding.orderDetailsHeaderLayout.cancelledView.isVisible = true
                    binding.orderDetailsHeaderLayout.newTextView.setTextColor(ContextCompat.getColor(requireContext(), R.color.grey_999999))
                    binding.orderDetailsHeaderLayout.receivedTextView.setTextColor(ContextCompat.getColor(requireContext(), R.color.grey_999999))
                    binding.orderDetailsHeaderLayout.packagingTextView.setTextColor(ContextCompat.getColor(requireContext(), R.color.grey_999999))
                    binding.orderDetailsHeaderLayout.assignedAppCompatTextView.setTextColor(
                        ContextCompat.getColor(
                            requireContext(), R.color.grey_999999
                        )
                    )
                    binding.orderDetailsHeaderLayout.dispatchedAppCompatTextView.setTextColor(
                        ContextCompat.getColor(
                            requireContext(), R.color.grey_999999
                        )
                    )
                    binding.orderDetailsHeaderLayout.deliveredAppCompatTextView.setTextColor(
                        ContextCompat.getColor(
                            requireContext(), R.color.grey_999999
                        )
                    )
                    binding.orderDetailsHeaderLayout.cancelledTextView.setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
                }
                resources.getString(R.string.making).lowercase() -> {
                    orderStatus = resources.getString(R.string.cancelled)
                    binding.orderDetailsHeaderLayout.assignDriverButton.text = resources.getString(R.string.cancel_order)
                    binding.orderDetailsHeaderLayout.receivedOrderButton.isVisible = false
                    binding.orderDetailsHeaderLayout.assignDriverButton.isVisible = true
                    binding.orderDetailsHeaderLayout.newDotSelect.isVisible = false
                    binding.orderDetailsHeaderLayout.receivedDotSelect.isVisible = false
                    binding.orderDetailsHeaderLayout.packagingDotSelect.isVisible = true
                    binding.orderDetailsHeaderLayout.assignDotSelect.isVisible = false
                    binding.orderDetailsHeaderLayout.dispatchedDotSelect.isVisible = false
                    binding.orderDetailsHeaderLayout.deliveredDotSelect.isVisible = false
                    binding.orderDetailsHeaderLayout.newTextView.setTextColor(ContextCompat.getColor(requireContext(), R.color.grey_999999))
                    binding.orderDetailsHeaderLayout.receivedTextView.setTextColor(ContextCompat.getColor(requireContext(), R.color.grey_999999))
                    binding.orderDetailsHeaderLayout.packagingTextView.setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
                    binding.orderDetailsHeaderLayout.assignedAppCompatTextView.setTextColor(
                        ContextCompat.getColor(
                            requireContext(), R.color.grey_999999
                        )
                    )
                    binding.orderDetailsHeaderLayout.dispatchedAppCompatTextView.setTextColor(
                        ContextCompat.getColor(
                            requireContext(), R.color.grey_999999
                        )
                    )
                    binding.orderDetailsHeaderLayout.deliveredAppCompatTextView.setTextColor(
                        ContextCompat.getColor(
                            requireContext(), R.color.grey_999999
                        )
                    )
                }
                resources.getString(R.string.assigned).lowercase() -> {
                    orderStatus = resources.getString(R.string.cancelled)
                    binding.orderDetailsHeaderLayout.assignDriverButton.text = resources.getString(R.string.cancel_order)
                    binding.orderDetailsHeaderLayout.receivedOrderButton.isVisible = false
                    binding.orderDetailsHeaderLayout.assignDriverButton.isVisible = true
                    binding.orderDetailsHeaderLayout.newDotSelect.isVisible = false
                    binding.orderDetailsHeaderLayout.receivedDotSelect.isVisible = false
                    binding.orderDetailsHeaderLayout.packagingDotSelect.isVisible = false
                    binding.orderDetailsHeaderLayout.assignDotSelect.isVisible = true
                    binding.orderDetailsHeaderLayout.dispatchedDotSelect.isVisible = false
                    binding.orderDetailsHeaderLayout.deliveredDotSelect.isVisible = false
                    binding.orderDetailsHeaderLayout.newTextView.setTextColor(ContextCompat.getColor(requireContext(), R.color.grey_999999))
                    binding.orderDetailsHeaderLayout.receivedTextView.setTextColor(ContextCompat.getColor(requireContext(), R.color.grey_999999))
                    binding.orderDetailsHeaderLayout.packagingTextView.setTextColor(ContextCompat.getColor(requireContext(), R.color.grey_999999))
                    binding.orderDetailsHeaderLayout.assignedAppCompatTextView.setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
                    binding.orderDetailsHeaderLayout.dispatchedAppCompatTextView.setTextColor(
                        ContextCompat.getColor(
                            requireContext(), R.color.grey_999999
                        )
                    )
                    binding.orderDetailsHeaderLayout.deliveredAppCompatTextView.setTextColor(
                        ContextCompat.getColor(
                            requireContext(), R.color.grey_999999
                        )
                    )
                }
                resources.getString(R.string.dispatched).lowercase() -> {
                    orderStatus = resources.getString(R.string.cancelled)
                    binding.orderDetailsHeaderLayout.assignDriverButton.text = resources.getString(R.string.cancel_order)
                    binding.orderDetailsHeaderLayout.receivedOrderButton.isVisible = false
                    binding.orderDetailsHeaderLayout.assignDriverButton.isVisible = true
                    binding.orderDetailsHeaderLayout.newDotSelect.isVisible = false
                    binding.orderDetailsHeaderLayout.receivedDotSelect.isVisible = false
                    binding.orderDetailsHeaderLayout.packagingDotSelect.isVisible = false
                    binding.orderDetailsHeaderLayout.assignDotSelect.isVisible = false
                    binding.orderDetailsHeaderLayout.dispatchedDotSelect.isVisible = true
                    binding.orderDetailsHeaderLayout.deliveredDotSelect.isVisible = false
                    binding.orderDetailsHeaderLayout.newTextView.setTextColor(ContextCompat.getColor(requireContext(), R.color.grey_999999))
                    binding.orderDetailsHeaderLayout.receivedTextView.setTextColor(ContextCompat.getColor(requireContext(), R.color.grey_999999))
                    binding.orderDetailsHeaderLayout.packagingTextView.setTextColor(ContextCompat.getColor(requireContext(), R.color.grey_999999))
                    binding.orderDetailsHeaderLayout.assignedAppCompatTextView.setTextColor(
                        ContextCompat.getColor(
                            requireContext(), R.color.grey_999999
                        )
                    )
                    binding.orderDetailsHeaderLayout.dispatchedAppCompatTextView.setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
                    binding.orderDetailsHeaderLayout.deliveredAppCompatTextView.setTextColor(
                        ContextCompat.getColor(
                            requireContext(), R.color.grey_999999
                        )
                    )
                }
                resources.getString(R.string.delivered).lowercase() -> {
                    orderStatus = resources.getString(R.string.assigned)
                    binding.orderDetailsHeaderLayout.assignDriverButton.isVisible = false
                    binding.orderDetailsHeaderLayout.receivedOrderButton.isVisible = false
                    binding.orderDetailsHeaderLayout.newDotSelect.isVisible = false
                    binding.orderDetailsHeaderLayout.receivedDotSelect.isVisible = false
                    binding.orderDetailsHeaderLayout.packagingDotSelect.isVisible = false
                    binding.orderDetailsHeaderLayout.assignDotSelect.isVisible = false
                    binding.orderDetailsHeaderLayout.dispatchedDotSelect.isVisible = false
                    binding.orderDetailsHeaderLayout.deliveredDotSelect.isVisible = true
                    binding.orderDetailsHeaderLayout.newTextView.setTextColor(ContextCompat.getColor(requireContext(), R.color.grey_999999))
                    binding.orderDetailsHeaderLayout.receivedTextView.setTextColor(ContextCompat.getColor(requireContext(), R.color.grey_999999))
                    binding.orderDetailsHeaderLayout.packagingTextView.setTextColor(ContextCompat.getColor(requireContext(), R.color.grey_999999))
                    binding.orderDetailsHeaderLayout.assignedAppCompatTextView.setTextColor(
                        ContextCompat.getColor(
                            requireContext(), R.color.grey_999999
                        )
                    )
                    binding.orderDetailsHeaderLayout.dispatchedAppCompatTextView.setTextColor(
                        ContextCompat.getColor(
                            requireContext(), R.color.grey_999999
                        )
                    )
                    binding.orderDetailsHeaderLayout.deliveredAppCompatTextView.setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
                }
                else -> {
                    binding.orderDetailsHeaderLayout.assignDriverButton.isVisible = false
                    binding.orderDetailsHeaderLayout.receivedOrderButton.isVisible = false
                    binding.orderDetailsHeaderLayout.newDotSelect.isVisible = false
                    binding.orderDetailsHeaderLayout.receivedDotSelect.isVisible = false
                    binding.orderDetailsHeaderLayout.packagingDotSelect.isVisible = false
                    binding.orderDetailsHeaderLayout.assignDotSelect.isVisible = false
                    binding.orderDetailsHeaderLayout.newTextView.setTextColor(ContextCompat.getColor(requireContext(), R.color.grey_999999))
                    binding.orderDetailsHeaderLayout.receivedTextView.setTextColor(ContextCompat.getColor(requireContext(), R.color.grey_999999))
                    binding.orderDetailsHeaderLayout.packagingTextView.setTextColor(ContextCompat.getColor(requireContext(), R.color.grey_999999))
                    binding.orderDetailsHeaderLayout.assignedAppCompatTextView.setTextColor(
                        ContextCompat.getColor(
                            requireContext(), R.color.grey_999999
                        )
                    )
                }
            }
        }
    }

    private fun initAdapter() {
        statusLogAdapter = StatusLogAdapter(requireContext())
        binding.statusLogDetails.rvStatusLog.apply {
            adapter = statusLogAdapter
        }
        orderDetailsAdapter = OrderDetailsAdapter(requireContext())
        binding.orderListLayout.rvOrderDetailsView.apply {
            adapter = orderDetailsAdapter
        }
    }

    private fun setOrderDetailsData(orderDetailsInfo: List<CartItem>?) {
        orderDetailsAdapter.listOfOrderDetailsInfo = orderDetailsInfo
    }

    private fun setStatusData(statusLogInfo: List<StatusItem>?) {
        statusLogAdapter.listOfStatusLog = statusLogInfo
    }

    override fun onResume() {
        super.onResume()
        RxBus.listen(RxEvent.CloseOrderDetailsScreen::class.java).subscribeAndObserveOnMainThread {
            if (isVisible) onBackPressed()
        }.autoDispose()
        RxBus.publish(RxEvent.HideShowEditTextMainActivity(false))
        orderDetailsViewModel.loadOrderDetailsItem(orderId)
    }

    override fun onPause() {
        super.onPause()
        RxBus.publish(RxEvent.HideShowEditTextMainActivity(true))
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        if (fromOrder == true) RxBus.publish(RxEvent.VisibleOrderFragment) else RxBus.publish(RxEvent.VisibleDeliveryFragment)
        val manager: FragmentManager? = fragmentManager
        val transaction: FragmentTransaction? = manager?.beginTransaction()
        if (fromOrder == true) transaction?.addToBackStack("CLOSE_DELIVERY") else transaction?.addToBackStack("CLOSE_DELIVERY_FRAGMENT")
        transaction?.remove(this)
        transaction?.commit()
    }
}