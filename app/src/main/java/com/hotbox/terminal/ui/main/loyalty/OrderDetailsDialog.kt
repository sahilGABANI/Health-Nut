package com.hotbox.terminal.ui.main.loyalty

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.hotbox.terminal.R
import com.hotbox.terminal.api.authentication.model.HealthNutUser
import com.hotbox.terminal.api.order.model.CartItem
import com.hotbox.terminal.api.order.model.OrderDetailsResponse
import com.hotbox.terminal.api.order.model.RefundDialogStates
import com.hotbox.terminal.api.stripe.model.PaymentStatus
import com.hotbox.terminal.application.HotBoxApplication
import com.hotbox.terminal.base.BaseDialogFragment
import com.hotbox.terminal.base.ViewModelFactory
import com.hotbox.terminal.base.extension.*
import com.hotbox.terminal.databinding.FragmentOrderDetailsDialogBinding
import com.hotbox.terminal.databinding.FragmentSnoozeItemDialogBinding
import com.hotbox.terminal.ui.main.loyalty.view.LoyaltyPointAdapter
import com.hotbox.terminal.ui.main.loyalty.viewmodel.LoyaltyState
import com.hotbox.terminal.ui.main.orderdetail.RefundFragmentDialog
import com.hotbox.terminal.ui.main.orderdetail.view.OrderDetailsAdapter
import com.hotbox.terminal.ui.main.orderdetail.viewmodel.OrderDetailsViewModel
import com.hotbox.terminal.ui.main.orderdetail.viewmodel.OrderDetailsViewState
import javax.inject.Inject

class OrderDetailsDialog : BaseDialogFragment() {

    companion object {
        const val INTENT_CART_GROUP = "Intent Cart Group"
        @JvmStatic
        fun newInstance(orderId: Int): OrderDetailsDialog {
            val args = Bundle()
            args.putInt(INTENT_CART_GROUP, orderId) 
            val fragment = OrderDetailsDialog()
            fragment.arguments = args
            return fragment
        }
    }
    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<OrderDetailsViewModel>
    private lateinit var orderDetailsViewModel: OrderDetailsViewModel
    private lateinit var orderDetailsAdapter: OrderDetailsAdapter
    
    private var _binding: FragmentOrderDetailsDialogBinding? = null
    private val binding get() = _binding!!
    private var orderId :Int =0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        HotBoxApplication.component.inject(this)
        orderDetailsViewModel = getViewModelFromFactory(viewModelFactory)
        setStyle(STYLE_NORMAL, R.style.MyDialog)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOrderDetailsDialogBinding.inflate(inflater, container, false)
        dialog?.window?.requestFeature(Window.FEATURE_NO_TITLE)
        dialog?.window?.decorView?.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        arguments?.let {
            orderId = it.getInt(INTENT_CART_GROUP)
            if (orderId != 0) {
                orderDetailsViewModel.loadOrderDetailsItem(orderId)
            }
        }
        listenToViewEvent()
        listenToViewModel()
    }

    private fun listenToViewEvent() {
        initAdapter()
        binding.closeButtonMaterialCardView.throttleClicks().subscribeAndObserveOnMainThread {
            dismiss()
        }.autoDispose()
    }
    private fun listenToViewModel() {
        orderDetailsViewModel.orderDetailsState.subscribeAndObserveOnMainThread {
            when (it) {
                is OrderDetailsViewState.ErrorMessage -> {
                    showToast(it.errorMessage)
                }
                is OrderDetailsViewState.LoadingState -> {

                }

                is OrderDetailsViewState.OrderDetailItemResponse -> {
                    it.orderDetails.id?.let {
                        orderId = it
                    }
                    initOrderDetailsUI(it.orderDetails)
                }
                else -> {

                }
            }
        }.autoDispose()
    }

    private fun initOrderDetailsUI(orderDetails: OrderDetailsResponse) {
        orderDetails.orderInstructions?.let {
            binding.specialTextLinear.isVisible = true
            binding.specialInstructionsTextView.text = it.toString()
        }
        if (!orderDetails.status.isNullOrEmpty()) {
            if (orderDetails.status.last().user == null) {
                orderDetails.status.last().user = HealthNutUser(
                    firstName = orderDetails.guest?.firstOrNull()?.guestFirstName ?: "",
                    lastName = orderDetails.guest?.firstOrNull()?.guestLastName ?: ""
                )
            }
        }
        orderDetails.cartGroup?.cart?.let { setOrderDetailsData(it) }
        orderDetails.orderTotal = orderDetails.orderTotal?.minus((orderDetails.orderEmpDiscount ?: 0.00))
        orderDetails.orderTotal = orderDetails.orderTotal?.minus((orderDetails.orderRefundAmount ?: 0.00))
        val total = orderDetails.orderTotal
        if (total != null) {
            orderDetails.orderTotal = if (total < 0) 0.00 else total
        }
        orderDetails.orderTotal?.let {

            binding.orderPrizePart.tvTotalPrizeNumber.text = ((it).div(100)).toDollar()
        }
        orderDetails.orderRefundAmount?.let {
            if (it != 0.00) {
                binding.orderPrizePart.rlRefund.isVisible = true
                binding.orderPrizePart.tvRefundAmount.text = "-${it.div(100).toDollar()}"
            }
        }
        orderDetails.orderSubtotal?.let {
            binding.orderPrizePart.tvOrderPrizeNumber.text = ((it).div(100)).toDollar()

        }
        orderDetails.orderTax?.let {
            if (!it.equals(0.0)) {
                binding.orderPrizePart.orderTaxRelativeLayout.isVisible = true
                binding.orderPrizePart.tvTaxPrize.text = ((it).div(100)).toDollar()
            }
        }
        orderDetails.orderTip?.let {
            if (!it.equals(0.0)) {
                binding.orderPrizePart.orderTipRelativeLayout.isVisible = true
                binding.orderPrizePart.tvTipsPrize.text = ((it).div(100)).toDollar()
            }
        }
        orderDetails.orderDeliveryFee?.let {
            if (!it.equals(0.0)) {
                binding.orderPrizePart.orderDeliveryRelativeLayout.isVisible = true
                binding.orderPrizePart.tvDeliveryCharge.text = ((it).div(100)).toDollar()
            }
        }
        orderDetails.orderCouponCodeDiscount?.let {
            if (!it.equals(0.0)) {
                binding.orderPrizePart.orderPromocodeRelativeLayout.isVisible = true
                binding.orderPrizePart.tvPromocodeDiscountPrize.text = "-${((it).div(100)).toMinusDollar().toDollar()}"
            }
        }
        orderDetails.orderEmpDiscount?.let {
            if (!it.equals(0.0)) {
                binding.orderPrizePart.orderEmployeeDiscountRelativeLayout.isVisible = true
                binding.orderPrizePart.tvEmployeeDiscountPrize.text = "-${((it).div(100)).toMinusDollar().toDollar()}"
            }
        }
        orderDetails.orderGiftCardAmount?.let {
            if (!it.equals(0.0)) {
                binding.orderPrizePart.orderCardAndBowRelativeLayout.isVisible = true
                binding.orderPrizePart.tvCardAndBowCharge.text = "-${((it).div(100)).toMinusDollar().toDollar()}"
            }
        }
        orderDetails.orderCreditAmount?.let {
            if (!it.equals(0.0)) {
                binding.orderPrizePart.rlCredit.isVisible = true
                binding.orderPrizePart.tvCreditAmount.text = "-${((it).div(100)).toMinusDollar().toDollar()}"
            }
        }
        binding.orderId.text = orderDetails.getSafeOrderId()
    }

    private fun setOrderDetailsData(cart: List<CartItem>) {
        orderDetailsAdapter.listOfOrderDetailsInfo = cart
    }

    private fun initAdapter() {
        orderDetailsAdapter = OrderDetailsAdapter(requireContext())
        binding.rvOrderDetailsView.apply {
            adapter = orderDetailsAdapter
        }
    }

}