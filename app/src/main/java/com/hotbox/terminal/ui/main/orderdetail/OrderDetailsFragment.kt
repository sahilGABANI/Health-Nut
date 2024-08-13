package com.hotbox.terminal.ui.main.orderdetail

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.icu.text.SimpleDateFormat
import android.icu.util.TimeZone
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.core.content.ContextCompat.getColor
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import com.hotbox.terminal.R
import com.hotbox.terminal.api.authentication.LoggedInUserCache
import com.hotbox.terminal.api.authentication.model.HealthNutUser
import com.hotbox.terminal.api.order.model.*
import com.hotbox.terminal.application.HotBoxApplication
import com.hotbox.terminal.base.BaseFragment
import com.hotbox.terminal.base.RxBus
import com.hotbox.terminal.base.RxEvent
import com.hotbox.terminal.base.ViewModelFactory
import com.hotbox.terminal.base.extension.*
import com.hotbox.terminal.databinding.FragmentOrderDetailsBinding
import com.hotbox.terminal.helper.formatTo
import com.hotbox.terminal.helper.toDate
import com.hotbox.terminal.helper.toStoreTimeZoneDate
import com.hotbox.terminal.ui.main.orderdetail.view.OrderDetailsAdapter
import com.hotbox.terminal.ui.main.orderdetail.view.StatusLogAdapter
import com.hotbox.terminal.ui.main.orderdetail.viewmodel.OrderDetailsViewModel
import com.hotbox.terminal.ui.main.orderdetail.viewmodel.OrderDetailsViewState
import timber.log.Timber
import java.util.*
import javax.inject.Inject
import kotlin.math.abs
import kotlin.properties.Delegates

class OrderDetailsFragment : BaseFragment() {

    companion object {
        private const val ORDER_ID = "orderId"
        private const val ORDER_USER_ID = "orderUserId"
        private const val ORDER_CART_GROUP_ID = "orderCartGroupId"

        @JvmStatic
        fun newInstance(orderId: Int?, orderUserId: Int? = null, orderCartGroupId: Int? = null): OrderDetailsFragment {
            val args = Bundle()
            orderId?.let { args.putInt(ORDER_ID, it) }
            orderUserId?.let { args.putInt(ORDER_USER_ID, it) }
            orderCartGroupId?.let { args.putInt(ORDER_CART_GROUP_ID, it) }
            val fragment = OrderDetailsFragment()
            fragment.arguments = args
            return fragment
        }
    }

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<OrderDetailsViewModel>

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache

    private lateinit var orderDetailsViewModel: OrderDetailsViewModel
    private var orderId :Int = 0
    private var orderUserId by Delegates.notNull<Int>()
    private var orderCartGroupId by Delegates.notNull<Int>()
    private lateinit var statusLogAdapter: StatusLogAdapter
    private lateinit var orderDetailsAdapter: OrderDetailsAdapter
    private var _binding: FragmentOrderDetailsBinding? = null
    private val binding get() = _binding!!
    private var orderStatus: String = ""
    private var isPickUpOrder: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        orderId = arguments?.getInt(ORDER_ID, 0) ?: throw IllegalStateException("No args provided")
        orderUserId = arguments?.getInt(ORDER_USER_ID, 0) ?: throw IllegalStateException("No args provided")
        orderCartGroupId = arguments?.getInt(ORDER_CART_GROUP_ID, 0) ?: throw IllegalStateException("No args provided")
        HotBoxApplication.component.inject(this)
        orderDetailsViewModel = getViewModelFromFactory(viewModelFactory)
        allApiCalling()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOrderDetailsBinding.inflate(inflater, container, false)
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
        binding.orderDetailsHeaderLayout.completedOrderButton.throttleClicks().subscribeAndObserveOnMainThread {
            orderStatus = resources.getString(R.string.cancelled).toLowerCase()
            if (orderStatus.isNotEmpty()) {
                cancelOrderDialog()
            }
        }.autoDispose()

        binding.orderDetailsHeaderLayout.receivedOrderButton.throttleClicks().subscribeAndObserveOnMainThread {
            if (orderStatus.isNotEmpty()) {
                orderDetailsViewModel.updateOrderStatusDetails(orderStatus.lowercase(), orderId)
                binding.orderDetailsHeaderLayout.receivedOrderButton.isVisible = false
            }
        }.autoDispose()

    }

    private fun cancelOrderDialog() {
        val alertDialog = AlertDialog.Builder(requireContext()).setTitle(resources.getText(R.string.cancel_order))
            .setMessage(resources.getText(R.string.are_you_sure_cancel_and_clear))
            .setNegativeButton(resources.getText(R.string.label_no)) { dialog, _ ->
                dialog.dismiss()
            }.setPositiveButton(resources.getText(R.string.label_yes)) { _, _ ->
                orderDetailsViewModel.updateOrderStatusDetails(orderStatus.lowercase(), orderId)
                binding.orderDetailsHeaderLayout.completedOrderButton.isVisible = false
            }.show()
        alertDialog.window?.setLayout(800, WindowManager.LayoutParams.WRAP_CONTENT)
    }


    private fun listenToViewModel() {
        orderDetailsViewModel.orderDetailsState.subscribeAndObserveOnMainThread {
            when (it) {
                is OrderDetailsViewState.ErrorMessage -> {
                    showToast(it.errorMessage)
                }
                is OrderDetailsViewState.LoadingState -> {
                    binding.progressBar.isVisible = it.isLoading
                }
                is OrderDetailsViewState.SuccessMessage -> {
                    showToast(it.successMessage)
                }
                is OrderDetailsViewState.OrderDetailItemResponse -> {
//                    setOrderDetailsData(it.orderDetails.items)
                    initUI(it.orderDetails)
                }
                is OrderDetailsViewState.CustomerDetails -> {
                    binding.customerDetails.customerNameAppCompatTextView.text = it.customerDetails.fullName()
                    binding.customerDetails.customerPhoneNumberAppCompatTextView.text = it.customerDetails.userPhone
                    binding.customerDetails.customerEmailAppCompatTextView.text = it.customerDetails.userEmail
                }
                is OrderDetailsViewState.UpdateStatusResponse -> {
                    if(it.updatedOrderStatusResponse.orderStatus == getString(R.string.refunded)) {
                        binding.orderDetailsHeaderLayout.refundButton.isVisible = false
                    }
                    orderDetailsViewModel.loadOrderDetailsItem(orderId)
                }
                is OrderDetailsViewState.CaptureNewPaymentIntent -> {

                }
                is OrderDetailsViewState.SendReceiptSuccessMessage -> {
                    showToast(it.successMessage)
                }
                else -> {

                }
            }
        }.autoDispose()
    }

    @SuppressLint("SetTextI18n")
    private fun initUI(orderDetails: OrderDetailsResponse) {
        orderDetails.orderInstructions?.let {
            binding.orderListLayout.specialTextLinear.isVisible = true
            binding.orderListLayout.specialInstructionsTextView.text = it.toString()
        }
        if(!orderDetails.status.isNullOrEmpty()) {
            if (orderDetails.status.last().user == null) {
                orderDetails.status.last().user =  HealthNutUser(firstName = orderDetails.guest?.firstOrNull()?.guestFirstName?: "", lastName =orderDetails.guest?.firstOrNull()?.guestLastName?: "")
                setStatusLogInfo(orderDetails.status)
            }
            setStatusLogInfo(orderDetails.status)
        }
        if (orderDetails.orderType?.subcategory == resources.getString(R.string.pickup)) {
            isPickUpOrder = true
            binding.orderDetailsHeaderLayout.completedTextView.isVisible = true
            binding.orderDetailsHeaderLayout.assignedAppCompatTextView.isVisible = false
            binding.orderDetailsHeaderLayout.dispatchedViewLinear.isVisible = false
            binding.orderDetailsHeaderLayout.dispatchedAppCompatTextView.isVisible = false
            binding.orderDetailsHeaderLayout.deliveredViewLinear.isVisible = false
            binding.orderDetailsHeaderLayout.deliveredAppCompatTextView.isVisible = false
        } else {
            isPickUpOrder = false
            binding.orderDetailsHeaderLayout.completedTextView.isVisible = true
            binding.orderDetailsHeaderLayout.assignedAppCompatTextView.isVisible = false
            binding.orderDetailsHeaderLayout.dispatchedViewLinear.isVisible = false
            binding.orderDetailsHeaderLayout.dispatchedAppCompatTextView.isVisible = false
            binding.orderDetailsHeaderLayout.deliveredViewLinear.isVisible = false
            binding.orderDetailsHeaderLayout.deliveredAppCompatTextView.isVisible = false
        }
        setOrderDetailsData(orderDetails.cartGroup?.cart)
        if(!orderDetails.guest.isNullOrEmpty()) {
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
        binding.orderDetailsHeaderLayout.orderStatusTextview.text = orderDetails.orderType?.subcategory.toString()
        orderDetails.orderTotal = orderDetails.orderTotal?.minus((orderDetails.orderEmpDiscount ?:0.00))
        orderDetails.orderTotal = orderDetails.orderTotal?.minus((orderDetails.orderRefundAmount ?:0.00))
        orderDetails.orderTotal = orderDetails.orderTotal?.plus((orderDetails.orderAdjustmentAmount ?:0.00))
        val total = orderDetails.orderTotal
        if (total != null) {
            orderDetails.orderTotal = if (total < 0) 0.00 else total
        }
        orderDetails.orderTotal?.let {
            binding.orderDetailsHeaderLayout.orderPrizeTextView.text = ((it).div(100)).toDollar()
            binding.orderListLayout.orderPrizePart.tvTotalPrizeNumber.text = ((it).div(100)).toDollar()
        }
        orderDetails.orderRefundAmount?.let {
            if (it != 0.00) {
                binding.orderListLayout.orderPrizePart.rlRefund.isVisible = true
                binding.orderListLayout.orderPrizePart.tvRefundAmount.text = "-${it.div(100).toDollar()}"
            }
        }
        orderDetails.orderSubtotal?.let {
            binding.orderListLayout.orderPrizePart.tvOrderPrizeNumber.text = ((it).div(100)).toDollar()

        }
        orderDetails.orderTax?.let {
            if (!it.equals(0.0)) {
                binding.orderListLayout.orderPrizePart.orderTaxRelativeLayout.isVisible = true
                binding.orderListLayout.orderPrizePart.tvTaxPrize.text = ((it).div(100)).toDollar()
            }
        }
        orderDetails.orderTip?.let {
            if (!it.equals(0.0)) {
                binding.orderListLayout.orderPrizePart.orderTipRelativeLayout.isVisible = true
                binding.orderListLayout.orderPrizePart.tvTipsPrize.text = ((it).div(100)).toDollar()
            }
        }
        orderDetails.orderDeliveryFee?.let {
            if (!it.equals(0.0)) {
                binding.orderListLayout.orderPrizePart.orderDeliveryRelativeLayout.isVisible = true
                binding.orderListLayout.orderPrizePart.tvDeliveryCharge.text = ((it).div(100)).toDollar()
            }
        }
        orderDetails.orderCouponCodeDiscount?.let {
            if (!it.equals(0.0)) {
                binding.orderListLayout.orderPrizePart.orderPromocodeRelativeLayout.isVisible = true
                binding.orderListLayout.orderPrizePart.tvPromocodeDiscountPrize.text = "-${((it).div(100)).toMinusDollar().toDollar()}"
            }
        }
        orderDetails.orderEmpDiscount?.let {
            if (!it.equals(0.0)) {
                binding.orderListLayout.orderPrizePart.orderEmployeeDiscountRelativeLayout.isVisible = true
                binding.orderListLayout.orderPrizePart.tvEmployeeDiscountPrize.text = "-${((it).div(100)).toMinusDollar().toDollar()}"
            }
        }
        orderDetails.orderGiftCardAmount?.let {
            if (!it.equals(0.0)) {
                binding.orderListLayout.orderPrizePart.orderCardAndBowRelativeLayout.isVisible = true
                binding.orderListLayout.orderPrizePart.tvCardAndBowCharge.text = "-${((it).div(100)).toMinusDollar().toDollar()}"
            }
        }
        orderDetails.orderCreditAmount?.let {
            if (!it.equals(0.0)) {
                binding.orderListLayout.orderPrizePart.rlCredit.isVisible = true
                binding.orderListLayout.orderPrizePart.tvCreditAmount.text = "-${((it).div(100)).toMinusDollar().toDollar()}"
            }
        }
        orderDetails.orderAdjustmentAmount?.let {
            if (!it.equals(0.0)) {
                if (it > 0){
                    binding.orderListLayout.orderPrizePart.rlAdjustment.isVisible = true
                    binding.orderListLayout.orderPrizePart.tvAdjustmentCharge.text = "${((it).div(100)).toDollar()}"
                } else{
                    binding.orderListLayout.orderPrizePart.rlAdjustment.isVisible = true
                    binding.orderListLayout.orderPrizePart.tvAdjustmentCharge.text = "-${(abs(it).div(100)).toDollar()}"
                }
            }
        }
        binding.orderDetailsHeaderLayout.orderIdTextView.text = orderDetails.getSafeOrderId()
        val timeZone = loggedInUserCache.getStoreResponse()?.locationLocationTimezone ?: "America/Los_Angeles"
        binding.orderDetailsHeaderLayout.orderDateAndTimeTextView.text = orderDetails.orderCreationDate?.toStoreTimeZoneDate("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")?.formatTo("MM/dd/yyyy, hh:mm a",
            TimeZone.getTimeZone(timeZone))
        orderDetails.orderPromisedTime?.let {
            binding.orderListLayout.timeTextView.text = it.toDate("yyyy-MM-dd hh:mm a")?.formatTo("MM/dd/yyyy, hh:mm a")
        }
        binding.customerDetails.btnPrintReceipt.throttleClicks().subscribeAndObserveOnMainThread {
            val printReceiptDialog = PrintReceiptDialog.newInstance(orderDetails).apply {
                printReceiptDismissed.subscribeAndObserveOnMainThread {
                    dismiss()
                }.autoDispose()
            }
            printReceiptDialog.show(parentFragmentManager,PrintReceiptDialog::class.java.name)
        }.autoDispose()
        binding.customerDetails.btnSendReceipt.throttleClicks().subscribeAndObserveOnMainThread {
            val sendReceiptDialogFragment = SendReceiptDialogFragment.newInstance(binding.customerDetails.customerEmailAppCompatTextView.text.toString(),binding.customerDetails.customerPhoneNumberAppCompatTextView.text.toString()).apply {
                refundDialogState.subscribeAndObserveOnMainThread {
                    when (it) {
                        is SendReceiptStates.SendReceiptOnEmail -> {
                            dismiss()
                            orderDetails.id?.let { it1 -> orderDetailsViewModel.sendReceipt(it1,"Email", it.email,null) }
                            binding.customerDetails.customerEmailAppCompatTextView.text =it.email
                        }
                        is SendReceiptStates.SendReceiptOnPhone -> {
                            dismiss()
                            orderDetails.id?.let { it1 -> orderDetailsViewModel.sendReceipt(it1,"Phone", null,it.phone) }
                            binding.customerDetails.customerPhoneNumberAppCompatTextView.text = it.phone
                        }
                        is SendReceiptStates.SendReceiptOnPhoneAndEmail -> {
                            dismiss()
                            orderDetails.id?.let { it1 -> orderDetailsViewModel.sendReceipt(it1,"Email", it.email,null) }
                            orderDetails.id?.let { it1 -> orderDetailsViewModel.sendReceipt(it1,"Phone", null,it.phone) }
                            binding.customerDetails.customerPhoneNumberAppCompatTextView.text =it.phone
                            binding.customerDetails.customerEmailAppCompatTextView.text =it.email
                        }
                    }
                }.autoDispose()
            }
            sendReceiptDialogFragment.show(parentFragmentManager, SendReceiptDialogFragment::class.java.name)
        }.autoDispose()
        if(orderDetails.status?.first()?.orderStatus != resources.getString(R.string.new_text).toLowerCase()  && loggedInUserCache.isAdmin() && orderDetails.orderTotal != 0.00 && orderDetails.status?.first()?.orderStatus != resources.getString(R.string.refunded).toLowerCase()) {
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
            printReceiptDialog.show(parentFragmentManager,PrintReceiptDialog::class.java.name)
        }.autoDispose()
        if (orderDetails.orderType?.subcategory != resources.getString(R.string.pickup)) {
            if(orderDetails.status?.isNotEmpty() == true ) {
                when (orderDetails.status?.first()?.orderStatus) {
                    resources.getString(R.string.new_text).lowercase() -> {
                        orderStatus = resources.getString(R.string.received).toLowerCase()
                        binding.orderDetailsHeaderLayout.completedOrderButton.text = resources.getString(R.string.cancel_order)
                        binding.orderDetailsHeaderLayout.completedOrderButton.isVisible = true
                        binding.orderDetailsHeaderLayout.receivedOrderButton.isVisible = true
                        binding.orderDetailsHeaderLayout.newDotSelect.isVisible = true
                        binding.orderDetailsHeaderLayout.receivedDotSelect.isVisible = false
                        binding.orderDetailsHeaderLayout.packagingDotSelect.isVisible = false
                        binding.orderDetailsHeaderLayout.completedDotSelect.isVisible = false
                        binding.orderDetailsHeaderLayout.newTextView.setTextColor(getColor(requireContext(), R.color.black))
                        binding.orderDetailsHeaderLayout.receivedTextView.setTextColor(getColor(requireContext(), R.color.grey_999999))
                        binding.orderDetailsHeaderLayout.packagingTextView.setTextColor(getColor(requireContext(), R.color.grey_999999))
                        binding.orderDetailsHeaderLayout.completedTextView.setTextColor(getColor(requireContext(), R.color.grey_999999))
                    }
                    resources.getString(R.string.received).lowercase() -> {
                        orderStatus = resources.getString(R.string.cancelled).toLowerCase()
                        binding.orderDetailsHeaderLayout.receivedOrderButton.isVisible = false
                        binding.orderDetailsHeaderLayout.completedOrderButton.text = resources.getString(R.string.cancel_order)
                        binding.orderDetailsHeaderLayout.completedOrderButton.isVisible = true
                        binding.orderDetailsHeaderLayout.newDotSelect.isVisible = false
                        binding.orderDetailsHeaderLayout.receivedDotSelect.isVisible = true
                        binding.orderDetailsHeaderLayout.packagingDotSelect.isVisible = false
                        binding.orderDetailsHeaderLayout.completedDotSelect.isVisible = false
                        binding.orderDetailsHeaderLayout.newTextView.setTextColor(getColor(requireContext(), R.color.grey_999999))
                        binding.orderDetailsHeaderLayout.receivedTextView.setTextColor(getColor(requireContext(), R.color.black))
                        binding.orderDetailsHeaderLayout.packagingTextView.setTextColor(getColor(requireContext(), R.color.grey_999999))
                        binding.orderDetailsHeaderLayout.completedTextView.setTextColor(getColor(requireContext(), R.color.grey_999999))
                    }
                    resources.getString(R.string.making).lowercase() -> {
                        orderStatus = resources.getString(R.string.completed)
                        binding.orderDetailsHeaderLayout.completedOrderButton.isVisible = false
                        binding.orderDetailsHeaderLayout.receivedOrderButton.isVisible = false
                        binding.orderDetailsHeaderLayout.completedOrderButton.text = resources.getString(R.string.complete_order)
                        binding.orderDetailsHeaderLayout.newDotSelect.isVisible = false
                        binding.orderDetailsHeaderLayout.receivedDotSelect.isVisible = false
                        binding.orderDetailsHeaderLayout.packagingDotSelect.isVisible = true
                        binding.orderDetailsHeaderLayout.completedDotSelect.isVisible = false
                        binding.orderDetailsHeaderLayout.newTextView.setTextColor(getColor(requireContext(), R.color.grey_999999))
                        binding.orderDetailsHeaderLayout.receivedTextView.setTextColor(getColor(requireContext(), R.color.grey_999999))
                        binding.orderDetailsHeaderLayout.packagingTextView.setTextColor(getColor(requireContext(), R.color.black))
                        binding.orderDetailsHeaderLayout.completedTextView.setTextColor(getColor(requireContext(), R.color.grey_999999))
                    }
                    resources.getString(R.string.completed).lowercase() -> {
                        binding.orderDetailsHeaderLayout.completedOrderButton.isVisible = false
                        binding.orderDetailsHeaderLayout.receivedOrderButton.isVisible = false
                        binding.orderDetailsHeaderLayout.newDotSelect.isVisible = false
                        binding.orderDetailsHeaderLayout.receivedDotSelect.isVisible = false
                        binding.orderDetailsHeaderLayout.packagingDotSelect.isVisible = false
                        binding.orderDetailsHeaderLayout.completedDotSelect.isVisible = true
                        binding.orderDetailsHeaderLayout.newTextView.setTextColor(getColor(requireContext(), R.color.grey_999999))
                        binding.orderDetailsHeaderLayout.receivedTextView.setTextColor(getColor(requireContext(), R.color.grey_999999))
                        binding.orderDetailsHeaderLayout.packagingTextView.setTextColor(getColor(requireContext(), R.color.grey_999999))
                        binding.orderDetailsHeaderLayout.completedTextView.setTextColor(getColor(requireContext(), R.color.black))
                    }
                    resources.getString(R.string.cancelled).lowercase() -> {
                        binding.orderDetailsHeaderLayout.completedOrderButton.isVisible = false
                        binding.orderDetailsHeaderLayout.newDotSelect.isVisible = false
                        binding.orderDetailsHeaderLayout.receivedOrderButton.isVisible = false
                        binding.orderDetailsHeaderLayout.receivedDotSelect.isVisible = false
                        binding.orderDetailsHeaderLayout.packagingDotSelect.isVisible = false
                        binding.orderDetailsHeaderLayout.completedDotSelect.isVisible = false
                        binding.orderDetailsHeaderLayout.cancelledTextView.isVisible = true
                        binding.orderDetailsHeaderLayout.cancelledDotSelect.isVisible = true
                        binding.orderDetailsHeaderLayout.cancelledView.isVisible = true
                        binding.orderDetailsHeaderLayout.cancelledTextView.setTextColor(getColor(requireContext(), R.color.black))
                        binding.orderDetailsHeaderLayout.newTextView.setTextColor(getColor(requireContext(), R.color.grey_999999))
                        binding.orderDetailsHeaderLayout.receivedTextView.setTextColor(getColor(requireContext(), R.color.grey_999999))
                        binding.orderDetailsHeaderLayout.packagingTextView.setTextColor(getColor(requireContext(), R.color.grey_999999))
                        binding.orderDetailsHeaderLayout.completedTextView.setTextColor(getColor(requireContext(), R.color.grey_999999))
                    }
                    resources.getString(R.string.refunded).lowercase() -> {
                        binding.orderDetailsHeaderLayout.completedOrderButton.isVisible = false
                        binding.orderDetailsHeaderLayout.newDotSelect.isVisible = false
                        binding.orderDetailsHeaderLayout.receivedOrderButton.isVisible = false
                        binding.orderDetailsHeaderLayout.receivedDotSelect.isVisible = false
                        binding.orderDetailsHeaderLayout.packagingDotSelect.isVisible = false
                        binding.orderDetailsHeaderLayout.completedDotSelect.isVisible = false
                        binding.orderDetailsHeaderLayout.cancelledTextView.isVisible = true
                        binding.orderDetailsHeaderLayout.cancelledTextView.text = resources.getString(R.string.refunded)
                        binding.orderDetailsHeaderLayout.cancelledDotSelect.isVisible = true
                        binding.orderDetailsHeaderLayout.cancelledView.isVisible = true
                        binding.orderDetailsHeaderLayout.cancelledTextView.setTextColor(getColor(requireContext(), R.color.black))
                        binding.orderDetailsHeaderLayout.newTextView.setTextColor(getColor(requireContext(), R.color.grey_999999))
                        binding.orderDetailsHeaderLayout.receivedTextView.setTextColor(getColor(requireContext(), R.color.grey_999999))
                        binding.orderDetailsHeaderLayout.packagingTextView.setTextColor(getColor(requireContext(), R.color.grey_999999))
                        binding.orderDetailsHeaderLayout.completedTextView.setTextColor(getColor(requireContext(), R.color.grey_999999))
                    }
                    else -> {
                        binding.orderDetailsHeaderLayout.completedOrderButton.isVisible = false
                        binding.orderDetailsHeaderLayout.newDotSelect.isVisible = false
                        binding.orderDetailsHeaderLayout.receivedOrderButton.isVisible = false
                        binding.orderDetailsHeaderLayout.receivedDotSelect.isVisible = false
                        binding.orderDetailsHeaderLayout.packagingDotSelect.isVisible = false
                        binding.orderDetailsHeaderLayout.completedDotSelect.isVisible = false
                        binding.orderDetailsHeaderLayout.newTextView.setTextColor(getColor(requireContext(), R.color.grey_999999))
                        binding.orderDetailsHeaderLayout.receivedTextView.setTextColor(getColor(requireContext(), R.color.grey_999999))
                        binding.orderDetailsHeaderLayout.packagingTextView.setTextColor(getColor(requireContext(), R.color.grey_999999))
                        binding.orderDetailsHeaderLayout.completedTextView.setTextColor(getColor(requireContext(), R.color.grey_999999))
                    }
                }
            }
        } else {
            if(orderDetails.status?.isNotEmpty() == true ) {
                when (orderDetails.status?.first()?.orderStatus) {
                    resources.getString(R.string.new_text).lowercase() -> {
                        orderStatus = resources.getString(R.string.received).toLowerCase()
                        binding.orderDetailsHeaderLayout.completedOrderButton.text = resources.getString(R.string.cancel_order)
                        binding.orderDetailsHeaderLayout.completedOrderButton.isVisible = true
                        binding.orderDetailsHeaderLayout.receivedOrderButton.isVisible = true
                        binding.orderDetailsHeaderLayout.newDotSelect.isVisible = true
                        binding.orderDetailsHeaderLayout.receivedDotSelect.isVisible = false
                        binding.orderDetailsHeaderLayout.packagingDotSelect.isVisible = false
                        binding.orderDetailsHeaderLayout.completedDotSelect.isVisible = false
                        binding.orderDetailsHeaderLayout.newTextView.setTextColor(getColor(requireContext(), R.color.black))
                        binding.orderDetailsHeaderLayout.receivedTextView.setTextColor(getColor(requireContext(), R.color.grey_999999))
                        binding.orderDetailsHeaderLayout.packagingTextView.setTextColor(getColor(requireContext(), R.color.grey_999999))
                        binding.orderDetailsHeaderLayout.assignedAppCompatTextView.setTextColor(getColor(requireContext(), R.color.grey_999999))
                    }
                    resources.getString(R.string.received).lowercase() -> {
                        orderStatus = resources.getString(R.string.cancelled).toLowerCase()
                        binding.orderDetailsHeaderLayout.completedOrderButton.text = resources.getString(R.string.cancel_order)
                        binding.orderDetailsHeaderLayout.newDotSelect.isVisible = false
                        binding.orderDetailsHeaderLayout.receivedDotSelect.isVisible = true
                        binding.orderDetailsHeaderLayout.packagingDotSelect.isVisible = false
                        binding.orderDetailsHeaderLayout.completedDotSelect.isVisible = false
                        binding.orderDetailsHeaderLayout.completedOrderButton.isVisible = true
                        binding.orderDetailsHeaderLayout.newTextView.setTextColor(getColor(requireContext(), R.color.grey_999999))
                        binding.orderDetailsHeaderLayout.receivedTextView.setTextColor(getColor(requireContext(), R.color.black))
                        binding.orderDetailsHeaderLayout.packagingTextView.setTextColor(getColor(requireContext(), R.color.grey_999999))
                        binding.orderDetailsHeaderLayout.assignedAppCompatTextView.setTextColor(getColor(requireContext(), R.color.grey_999999))
                        binding.orderDetailsHeaderLayout.dispatchedAppCompatTextView.setTextColor(getColor(requireContext(), R.color.grey_999999))
                        binding.orderDetailsHeaderLayout.deliveredAppCompatTextView.setTextColor(getColor(requireContext(), R.color.grey_999999))
                    }
                    resources.getString(R.string.making).lowercase() -> {
                        orderStatus = resources.getString(R.string.assigned)
                        binding.orderDetailsHeaderLayout.completedOrderButton.text = resources.getString(R.string.assign_driver)
                        binding.orderDetailsHeaderLayout.newDotSelect.isVisible = false
                        binding.orderDetailsHeaderLayout.receivedDotSelect.isVisible = false
                        binding.orderDetailsHeaderLayout.packagingDotSelect.isVisible = true
                        binding.orderDetailsHeaderLayout.assignDotSelect.isVisible = false
                        binding.orderDetailsHeaderLayout.completedOrderButton.isVisible = false
                        binding.orderDetailsHeaderLayout.newTextView.setTextColor(getColor(requireContext(), R.color.grey_999999))
                        binding.orderDetailsHeaderLayout.receivedTextView.setTextColor(getColor(requireContext(), R.color.grey_999999))
                        binding.orderDetailsHeaderLayout.packagingTextView.setTextColor(getColor(requireContext(), R.color.black))
                        binding.orderDetailsHeaderLayout.assignedAppCompatTextView.setTextColor(getColor(requireContext(), R.color.grey_999999))
                        binding.orderDetailsHeaderLayout.dispatchedAppCompatTextView.setTextColor(getColor(requireContext(), R.color.grey_999999))
                        binding.orderDetailsHeaderLayout.deliveredAppCompatTextView.setTextColor(getColor(requireContext(), R.color.grey_999999))
                    }
                    resources.getString(R.string.assigned).lowercase() -> {
                        binding.orderDetailsHeaderLayout.completedOrderButton.isVisible = false
                        binding.orderDetailsHeaderLayout.newDotSelect.isVisible = false
                        binding.orderDetailsHeaderLayout.receivedDotSelect.isVisible = false
                        binding.orderDetailsHeaderLayout.packagingDotSelect.isVisible = false
                        binding.orderDetailsHeaderLayout.assignDotSelect.isVisible = false
                        binding.orderDetailsHeaderLayout.completedDotSelect.isVisible = true
                        binding.orderDetailsHeaderLayout.newTextView.setTextColor(getColor(requireContext(), R.color.grey_999999))
                        binding.orderDetailsHeaderLayout.receivedTextView.setTextColor(getColor(requireContext(), R.color.grey_999999))
                        binding.orderDetailsHeaderLayout.packagingTextView.setTextColor(getColor(requireContext(), R.color.grey_999999))
                        binding.orderDetailsHeaderLayout.assignedAppCompatTextView.setTextColor(getColor(requireContext(), R.color.black))
                        binding.orderDetailsHeaderLayout.dispatchedAppCompatTextView.setTextColor(getColor(requireContext(), R.color.grey_999999))
                        binding.orderDetailsHeaderLayout.deliveredAppCompatTextView.setTextColor(getColor(requireContext(), R.color.grey_999999))
                    }
                    resources.getString(R.string.dispatched).lowercase() -> {
                        binding.orderDetailsHeaderLayout.completedOrderButton.isVisible = false
                        binding.orderDetailsHeaderLayout.newDotSelect.isVisible = false
                        binding.orderDetailsHeaderLayout.receivedDotSelect.isVisible = false
                        binding.orderDetailsHeaderLayout.packagingDotSelect.isVisible = false
                        binding.orderDetailsHeaderLayout.assignDotSelect.isVisible = false
                        binding.orderDetailsHeaderLayout.dispatchedDotSelect.isVisible = true
                        binding.orderDetailsHeaderLayout.newTextView.setTextColor(getColor(requireContext(), R.color.grey_999999))
                        binding.orderDetailsHeaderLayout.receivedTextView.setTextColor(getColor(requireContext(), R.color.grey_999999))
                        binding.orderDetailsHeaderLayout.packagingTextView.setTextColor(getColor(requireContext(), R.color.grey_999999))
                        binding.orderDetailsHeaderLayout.assignedAppCompatTextView.setTextColor(getColor(requireContext(), R.color.grey_999999))
                        binding.orderDetailsHeaderLayout.dispatchedAppCompatTextView.setTextColor(getColor(requireContext(), R.color.black))
                        binding.orderDetailsHeaderLayout.deliveredAppCompatTextView.setTextColor(getColor(requireContext(), R.color.grey_999999))
                    }
                    resources.getString(R.string.delivered).lowercase() -> {
                        binding.orderDetailsHeaderLayout.completedOrderButton.isVisible = false
                        binding.orderDetailsHeaderLayout.newDotSelect.isVisible = false
                        binding.orderDetailsHeaderLayout.receivedDotSelect.isVisible = false
                        binding.orderDetailsHeaderLayout.packagingDotSelect.isVisible = false
                        binding.orderDetailsHeaderLayout.assignDotSelect.isVisible = false
                        binding.orderDetailsHeaderLayout.dispatchedDotSelect.isVisible = false
                        binding.orderDetailsHeaderLayout.deliveredDotSelect.isVisible = true
                        binding.orderDetailsHeaderLayout.newTextView.setTextColor(getColor(requireContext(), R.color.grey_999999))
                        binding.orderDetailsHeaderLayout.receivedTextView.setTextColor(getColor(requireContext(), R.color.grey_999999))
                        binding.orderDetailsHeaderLayout.packagingTextView.setTextColor(getColor(requireContext(), R.color.grey_999999))
                        binding.orderDetailsHeaderLayout.assignedAppCompatTextView.setTextColor(getColor(requireContext(), R.color.grey_999999))
                        binding.orderDetailsHeaderLayout.dispatchedAppCompatTextView.setTextColor(getColor(requireContext(), R.color.grey_999999))
                        binding.orderDetailsHeaderLayout.deliveredAppCompatTextView.setTextColor(getColor(requireContext(), R.color.black))
                    }
                    resources.getString(R.string.cancelled).lowercase() -> {
                        binding.orderDetailsHeaderLayout.completedOrderButton.isVisible = false
                        binding.orderDetailsHeaderLayout.newDotSelect.isVisible = false
                        binding.orderDetailsHeaderLayout.receivedDotSelect.isVisible = false
                        binding.orderDetailsHeaderLayout.packagingDotSelect.isVisible = false
                        binding.orderDetailsHeaderLayout.completedDotSelect.isVisible = false
                        binding.orderDetailsHeaderLayout.cancelledTextView.isVisible = true
                        binding.orderDetailsHeaderLayout.cancelledDotSelect.isVisible = true
                        binding.orderDetailsHeaderLayout.cancelledView.isVisible = true
                        binding.orderDetailsHeaderLayout.newTextView.setTextColor(getColor(requireContext(), R.color.grey_999999))
                        binding.orderDetailsHeaderLayout.receivedTextView.setTextColor(getColor(requireContext(), R.color.grey_999999))
                        binding.orderDetailsHeaderLayout.packagingTextView.setTextColor(getColor(requireContext(), R.color.grey_999999))
                        binding.orderDetailsHeaderLayout.assignedAppCompatTextView.setTextColor(getColor(requireContext(), R.color.grey_999999))
                        binding.orderDetailsHeaderLayout.dispatchedAppCompatTextView.setTextColor(getColor(requireContext(), R.color.grey_999999))
                        binding.orderDetailsHeaderLayout.cancelledTextView.setTextColor(getColor(requireContext(), R.color.black))
                    }
                    else -> {
                        binding.orderDetailsHeaderLayout.completedOrderButton.isVisible = false
                        binding.orderDetailsHeaderLayout.newDotSelect.isVisible = false
                        binding.orderDetailsHeaderLayout.receivedDotSelect.isVisible = false
                        binding.orderDetailsHeaderLayout.packagingDotSelect.isVisible = false
                        binding.orderDetailsHeaderLayout.completedDotSelect.isVisible = false
                        binding.orderDetailsHeaderLayout.newTextView.setTextColor(getColor(requireContext(), R.color.grey_999999))
                        binding.orderDetailsHeaderLayout.receivedTextView.setTextColor(getColor(requireContext(), R.color.grey_999999))
                        binding.orderDetailsHeaderLayout.packagingTextView.setTextColor(getColor(requireContext(), R.color.grey_999999))
                        binding.orderDetailsHeaderLayout.completedTextView.setTextColor(getColor(requireContext(), R.color.grey_999999))
                        binding.orderDetailsHeaderLayout.completedTextView.setTextColor(getColor(requireContext(), R.color.grey_999999))
                    }
                }
            }
        }

    }


    private fun setOrderDetailsData(orderDetailsInfo: List<CartItem>?) {
        orderDetailsAdapter.listOfOrderDetailsInfo = orderDetailsInfo
    }

    private fun setStatusLogInfo(statusLogInfo: List<StatusItem>?) {
        statusLogAdapter.listOfStatusLog = statusLogInfo
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

    private fun allApiCalling() {
        orderDetailsViewModel.loadOrderDetailsItem(orderId)
        if (orderUserId != 0) {
            orderDetailsViewModel.loadUserDetails(orderUserId)
        }

    }

    override fun onResume() {
        super.onResume()
        RxBus.listen(RxEvent.CloseOrderDetailsScreen::class.java).subscribeAndObserveOnMainThread {
            if (isVisible) onBackPressed()
        }.autoDispose()
        RxBus.publish(RxEvent.HideShowEditTextMainActivity(false))
    }

    override fun onPause() {
        super.onPause()
        RxBus.publish(RxEvent.HideShowEditTextMainActivity(true))
    }

    override fun onBackPressed() {
        super.onBackPressed()
        RxBus.publish(RxEvent.VisibleOrderFragment)
        val manager: FragmentManager? = fragmentManager
        val transaction: FragmentTransaction? = manager?.beginTransaction()
        manager?.popBackStack()
        transaction?.remove(this)
        transaction?.commit()
    }

}