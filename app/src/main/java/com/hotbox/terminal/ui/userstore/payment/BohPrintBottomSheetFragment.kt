package com.hotbox.terminal.ui.userstore.payment

import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.hotbox.terminal.R
import com.hotbox.terminal.api.authentication.LoggedInUserCache
import com.hotbox.terminal.api.authentication.model.HealthNutUser
import com.hotbox.terminal.api.order.model.OrderDetailsResponse
import com.hotbox.terminal.api.userstore.model.CreateOrderResponse
import com.hotbox.terminal.application.HotBoxApplication
import com.hotbox.terminal.base.BaseBottomSheetDialogFragment
import com.hotbox.terminal.base.extension.subscribeAndObserveOnMainThread
import com.hotbox.terminal.base.extension.throttleClicks
import com.hotbox.terminal.databinding.FragmentBohPrintBottomSheetBinding
import com.hotbox.terminal.helper.BohPrinterHelper
import com.hotbox.terminal.helper.formatTo
import com.hotbox.terminal.helper.toDate
import timber.log.Timber
import javax.inject.Inject

class BohPrintBottomSheetFragment : BaseBottomSheetDialogFragment() {

    private var creatOrderInfo: CreateOrderResponse? = null
    private lateinit var promisedTime: String
    private var _binding: FragmentBohPrintBottomSheetBinding? = null
    private val binding get() = _binding!!
    private lateinit var bohPrinterHelper: BohPrinterHelper
    private var bohPrintAddress: String? = null

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache

    companion object {
        private const val CREATE_POS_ORDER_INFO = "CREATE_POS_ORDER_INFO"
        private const val PROMISED_TIME = "PROMISED_TIME"
        fun newInstance(createPosOrder: CreateOrderResponse, promisedTime: String?): BohPrintBottomSheetFragment {
            val args = Bundle()
            val fragment = BohPrintBottomSheetFragment()
            args.putParcelable(CREATE_POS_ORDER_INFO,createPosOrder)
            args.putString(PROMISED_TIME,promisedTime)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        HotBoxApplication.component.inject(this)
        setStyle(STYLE_NORMAL, R.style.BSDialogThemeRegular)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBohPrintBottomSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val bottomSheet = (view.parent as View)
        bottomSheet.backgroundTintMode = PorterDuff.Mode.CLEAR
        bottomSheet.setBackgroundColor(Color.TRANSPARENT)

        dialog?.apply {
            val behavior: BottomSheetBehavior<*> = BottomSheetBehavior.from(bottomSheet)
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
            behavior.peekHeight = 0
            behavior.addBottomSheetCallback(mBottomSheetBehaviorCallback)
        }

        listenToViewEvent()
    }

    private val mBottomSheetBehaviorCallback: BottomSheetBehavior.BottomSheetCallback = object : BottomSheetBehavior.BottomSheetCallback() {
        override fun onStateChanged(bottomSheet: View, newState: Int) {
            if (newState == BottomSheetBehavior.STATE_COLLAPSED || newState == BottomSheetBehavior.STATE_HIDDEN) {
                dismiss()
            }
        }

        override fun onSlide(bottomSheet: View, slideOffset: Float) {}
    }

    private fun listenToViewEvent() {
        bohPrinterInitialize()
        arguments?.let {
            creatOrderInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S_V2) {
                it.getParcelable(CREATE_POS_ORDER_INFO)
            } else {
                it.getParcelable(CREATE_POS_ORDER_INFO)
            }
            promisedTime = it.getString(PROMISED_TIME, "")
        }

        binding.bohPrintMaterialButton.throttleClicks().subscribeAndObserveOnMainThread {
            creatOrderInfo?.let { it1 -> autoPrint(it1) }
        }.autoDispose()
        binding.closeButtonMaterialCardView.throttleClicks().subscribeAndObserveOnMainThread {
            dismiss()
        }.autoDispose()
    }

    private fun bohPrinterInitialize() {
        Timber.tag("Printer").i("Call Connect BOH Printer")
        bohPrinterHelper = BohPrinterHelper.getInstance(requireActivity())
        bohPrinterHelper.printerInitialize(requireContext())

    }
    private fun autoPrint(createPosOrder : CreateOrderResponse) {
        val cart = com.hotbox.terminal.api.order.model.CartGroup(
            promisedTime = promisedTime?.toDate("yyyy-MM-dd HH:mm")?.formatTo("yyyy-MM-dd'T'HH:mm:ss"),
            cart = createPosOrder.cartGroup?.cart,
            cartCreatedDate = createPosOrder.cartGroup?.cartCreatedDate,
            id = createPosOrder.cartGroup?.id
        )
        val orderMode = com.hotbox.terminal.api.order.model.OrderMode(
            id = createPosOrder.orderMode?.id, modeName = createPosOrder.orderMode?.modeName
        )
        val orderType = com.hotbox.terminal.api.order.model.OrderType(
            id = createPosOrder.orderType?.id, subcategory = createPosOrder.orderType?.subcategory
        )
        val user = HealthNutUser(
            firstName = loggedInUserCache.getLoyaltyQrResponse()?.fullName,
            lastName = loggedInUserCache.getLoyaltyQrResponse()?.lastName,
        )

        val orderDetails = OrderDetailsResponse(
            id = createPosOrder.id,
            cartGroup = cart,
            orderCreationDate = createPosOrder.orderCreationDate,
            orderLocation = createPosOrder.orderLocation,
            orderSubtotal = createPosOrder.orderSubtotal?.toDouble(),
            orderTotal = createPosOrder.orderTotal?.toDouble(),
            orderTax = createPosOrder.orderTax?.toDouble(),
            orderDeliveryFee = createPosOrder.orderDeliveryFee?.toDouble(),
            orderTip = createPosOrder.orderTip?.toDouble(),
            orderGiftCardAmount = createPosOrder.orderGiftCardAmount?.toDouble(),
            orderCouponCodeDiscount = createPosOrder.orderCouponCodeDiscount?.toDouble(),
            orderEmpDiscount = createPosOrder.orderEmpDiscount?.toDouble(),
            orderRefundAmount = createPosOrder.orderRefundAmount?.toDouble(),
            orderMode = orderMode,
            orderType = orderType,
            user = user,
            guest = arrayListOf(),
            orderPromisedTime = promisedTime?.toDate("yyyy-MM-dd HH:mm")?.formatTo("yyyy-MM-dd'T'HH:mm:ss"),
        )
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


}