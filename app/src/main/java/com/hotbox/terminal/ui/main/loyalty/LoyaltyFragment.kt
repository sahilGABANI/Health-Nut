package com.hotbox.terminal.ui.main.loyalty

import android.os.Bundle
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.hotbox.terminal.R
import com.hotbox.terminal.api.authentication.LoggedInUserCache
import com.hotbox.terminal.api.authentication.model.HealthNutUser
import com.hotbox.terminal.api.checkout.model.LoyaltyPhoneHistoryInfo
import com.hotbox.terminal.api.checkout.model.LoyaltyWithPhoneResponse
import com.hotbox.terminal.api.loyalty.model.AddLoyaltyPointRequest
import com.hotbox.terminal.api.order.model.CartItem
import com.hotbox.terminal.api.order.model.OrderDetailsResponse
import com.hotbox.terminal.application.HotBoxApplication
import com.hotbox.terminal.base.*
import com.hotbox.terminal.base.extension.*
import com.hotbox.terminal.databinding.FragmentLoyaltyBinding
import com.hotbox.terminal.ui.main.loyalty.view.LoyaltyPointAdapter
import com.hotbox.terminal.ui.main.loyalty.viewmodel.LoyaltyState
import com.hotbox.terminal.ui.main.loyalty.viewmodel.LoyaltyViewModel
import com.hotbox.terminal.ui.main.orderdetail.view.OrderDetailsAdapter
import com.hotbox.terminal.ui.userstore.AdminPinDialogFragment
import javax.inject.Inject

class LoyaltyFragment : BaseFragment() {
    companion object {
        @JvmStatic
        fun newInstance() = LoyaltyFragment()
    }


    private var _binding: FragmentLoyaltyBinding? = null
    private val binding get() = _binding!!
    private lateinit var loyaltyPointAdapter: LoyaltyPointAdapter
    private lateinit var orderDetailsAdapter: OrderDetailsAdapter
    private var viewLoyaltyInfo = LoyaltyWithPhoneResponse()

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<LoyaltyViewModel>
    private lateinit var loyaltyViewModel: LoyaltyViewModel

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache

    private var orderId: Int = 0
    private var userId: String = ""
    private var viewLoyaltyUserId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        HotBoxApplication.component.inject(this)
        loyaltyViewModel = getViewModelFromFactory(viewModelFactory)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoyaltyBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        listenToViewModel()
        listenToViewEvent()
    }

    private fun listenToViewEvent() {
        initAdapter()
        viewOrAddLoyaltySelection(false)
        val loyalty = getColoredSpanned(resources.getString(R.string.leaves), ContextCompat.getColor(requireContext(), R.color.black))
        val number = getColoredSpanned(resources.getString(R.string._0), ContextCompat.getColor(requireContext(), R.color.orange))
        binding.viewLoyalty.tvLeaves.text = Html.fromHtml("$loyalty: $number")
        binding.viewLoyalty.searchButton.throttleClicks().subscribeAndObserveOnMainThread {
            requireActivity().hideKeyboard(binding.viewLoyalty.searchButton.rootView)
            if (isPhoneValidate()) {
                binding.viewLoyalty.userEmailAppCompatTextView.text = "-"
                binding.viewLoyalty.userPhoneNumberAppCompatTextView.text = "-"
                binding.viewLoyalty.userNameAppCompatTextView.text = "-"
                loyaltyViewModel.getPhoneLoyaltyData(binding.viewLoyalty.phoneSearchEditText.text.toString())
            }
        }.autoDispose()
        binding.viewAddLoyalty.searchPhoneButton.throttleClicks().subscribeAndObserveOnMainThread {
            requireActivity().hideKeyboard(binding.viewAddLoyalty.searchPhoneButton.rootView)
            if (isPhoneForAddLeavesValidate()) {
                loyaltyViewModel.getPhoneUserData(binding.viewAddLoyalty.phoneSearchEditText.text.toString())
            }
        }.autoDispose()

        binding.viewAddLoyalty.searchButton.throttleClicks().subscribeAndObserveOnMainThread {
            requireActivity().hideKeyboard(binding.viewAddLoyalty.searchButton.rootView)
            binding.viewAddLoyalty.llUserDetails.visibility = View.INVISIBLE
            if (isOrderValidate()) {
                resetAddLoyaltyScreen()
                loyaltyViewModel.loadOrderDetailsItem(binding.viewAddLoyalty.orderIdEditText.text?.trim().toString().toLong())
                loyaltyViewModel.getOrderLoyalty(binding.viewAddLoyalty.orderIdEditText.text?.trim().toString().toLong())
            }
        }.autoDispose()

        binding.viewAddLoyalty.addLeavesButton.throttleClicks().subscribeAndObserveOnMainThread {
            requireActivity().hideKeyboard(binding.viewAddLoyalty.addLeavesButton.rootView)
            if (userId.isNotEmpty() && orderId != 0) {
                if (loggedInUserCache.getLoggedInUser()?.crewResponse?.isAdminPin != true) {
                    val adminPinDialogFragment = AdminPinDialogFragment().apply {
                        adminPinSuccess.subscribeAndObserveOnMainThread {
                            val addLoyaltyPointRequest = AddLoyaltyPointRequest(
                                userId = userId, adminId = it, orderId = orderId
                            )
                            loyaltyViewModel.addLoyaltyPoint(addLoyaltyPointRequest)
                        }.autoDispose()
                    }
                    adminPinDialogFragment.show(parentFragmentManager, AdminPinDialogFragment::class.java.name)
                } else {
                    val addLoyaltyPointRequest = AddLoyaltyPointRequest(
                        userId = userId, adminId = loggedInUserCache.getLoggedInUserId(), orderId = orderId
                    )
                    loyaltyViewModel.addLoyaltyPoint(addLoyaltyPointRequest)
                }
            } else {
                when {
                    userId.isEmpty() && orderId != 0 -> {
                        showToast("plz enter user phone for user loyalty")
                    }
                    userId.isNotEmpty() && orderId == 0 -> {
                        showToast("plz enter order id")
                    }
                    else -> {
                        showToast("plz enter order id and user Phone")
                    }
                }
            }
        }.autoDispose()
        binding.viewLoyaltySelectLinear.throttleClicks().subscribeAndObserveOnMainThread {
            viewOrAddLoyaltySelection(false)
            binding.viewAddLoyalty.orderIdEditText.setText("")
            resetAddLoyaltyScreen()
        }.autoDispose()

        binding.addLoyaltySelectLinear.throttleClicks().subscribeAndObserveOnMainThread {
            binding.viewAddLoyalty.orderIdEditText.setText("")
            viewOrAddLoyaltySelection(true)
        }.autoDispose()
        RxBus.listen(RxEvent.ClearLoyaltyScreen::class.java).subscribeAndObserveOnMainThread {
            binding.viewAddLoyalty.orderIdEditText.setText("")
            resetAddLoyaltyScreen()
            resetViewLoyaltyScreen()
        }.autoDispose()
    }

    private fun initAdapter() {
        loyaltyPointAdapter = LoyaltyPointAdapter(requireContext()).apply {
            loyaltyPointActionState.subscribeAndObserveOnMainThread {
                val orderDetailsDialog = OrderDetailsDialog.newInstance(it.order?.id ?: 0)
                orderDetailsDialog.show(parentFragmentManager,OrderDetailsDialog::class.java.name)
            }.autoDispose()
        }

        binding.viewLoyalty.rvLoyalty.apply {
            adapter = loyaltyPointAdapter
            layoutManager = LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, false)
        }

        orderDetailsAdapter = OrderDetailsAdapter(requireContext())
        binding.viewAddLoyalty.rvOrderDetailsView.apply {
            adapter = orderDetailsAdapter
        }
    }

    private fun listenToViewModel() {
        loyaltyViewModel.loyaltyState.subscribeAndObserveOnMainThread { state ->
            when (state) {
                is LoyaltyState.ErrorMessage -> {
                    showToast(state.errorMessage)
                }
                is LoyaltyState.SuccessMessage -> {

                }
                is LoyaltyState.LoadingState -> {
                    viewLoyaltyVisibility(state.isLoading)
                }
                is LoyaltyState.OrderLoadingState -> {
                    addLoyaltyVisibility(state.isLoading)
                }
                is LoyaltyState.AddScreenPhoneLoadingState -> {
                    addUserVisibility(state.isLoading)
                }
                is LoyaltyState.PhoneLoyaltyData -> {
                    state.data?.userId?.let {
                        viewLoyaltyUserId = it
                        loyaltyViewModel.getUser(state.data.userId)
                    }
                    if (state.data != null) {
                        viewLoyaltyInfo = state.data
                        if (state.data.points != null) {
                            val loyalty =
                                getColoredSpanned(resources.getString(R.string.leaves), ContextCompat.getColor(requireContext(), R.color.black))
                            val number = getColoredSpanned(state.data.points.toString(), ContextCompat.getColor(requireContext(), R.color.orange))
                            binding.viewLoyalty.tvLeaves.text = Html.fromHtml("$loyalty: $number")
                            if (state.data.data?.isNotEmpty() == true){
                                loyaltyPointAdapter.listOfLoyaltyPoint = state.data.data?.reversed()
                                binding.viewLoyalty.tvEmpty.isVisible = false
                            }else {
                                binding.viewLoyalty.tvEmpty.isVisible = true
                                loyaltyPointAdapter.listOfLoyaltyPoint = arrayListOf()
                                binding.viewLoyalty.tvEmpty.text = resources.getString(R.string.leaves_empty_message)
                            }
                        } else {
                            val loyalty =
                                getColoredSpanned(resources.getString(R.string.leaves), ContextCompat.getColor(requireContext(), R.color.black))
                            val number = getColoredSpanned("0", ContextCompat.getColor(requireContext(), R.color.orange))
                            binding.viewLoyalty.tvLeaves.text = Html.fromHtml("$loyalty: $number")
                            if (state.data.data != null && state.data.userId != null) {
                                if (state.data.data.isNotEmpty()){
                                    loyaltyPointAdapter.listOfLoyaltyPoint = state.data.data.reversed()
                                    binding.viewLoyalty.tvEmpty.isVisible = false
                                }else {
                                    binding.viewLoyalty.tvEmpty.isVisible = true
                                    loyaltyPointAdapter.listOfLoyaltyPoint = arrayListOf()
                                    binding.viewLoyalty.tvEmpty.text = resources.getString(R.string.leaves_empty_message)
                                }
                            } else {
                                binding.viewLoyalty.tvEmpty.isVisible = true
                                loyaltyPointAdapter.listOfLoyaltyPoint = arrayListOf()
                                binding.viewLoyalty.tvEmpty.text = resources.getString(R.string.leaves_empty_message)
                            }
                        }
                    } else {
                        binding.viewLoyalty.tvEmpty.isVisible = true
                        loyaltyPointAdapter.listOfLoyaltyPoint = arrayListOf()
                        binding.viewLoyalty.tvEmpty.text = resources.getString(R.string.leaves_empty_message)
                    }
                }
                is LoyaltyState.OrderDetailItemResponse -> {
                    state.orderDetails.id?.let {
                        orderId = it
                    }
                    initOrderDetailsUI(state.orderDetails)
                }
                is LoyaltyState.PhoneUserData -> {
                    state.data?.userId?.let {
                        userId = it
                        loyaltyViewModel.getUser(state.data.userId)
                    }
                    if (state.data?.userId == null) {
                        binding.viewAddLoyalty.customerEmailAppCompatTextView.text = "-"
                        binding.viewAddLoyalty.customerPhoneNumberAppCompatTextView.text = "-"
                        binding.viewAddLoyalty.customerNameAppCompatTextView.text = "-"
                        binding.viewAddLoyalty.leavesTextview.text = "-"
                        showToast("no customer found")
                    } else {
                        if (state.data.points != null) {
                            val loyalty = getColoredSpanned(resources.getString(R.string.leaves), ContextCompat.getColor(requireContext(), R.color.black))
                            val number = getColoredSpanned(state.data.points.toString(), ContextCompat.getColor(requireContext(), R.color.orange))
                            binding.viewAddLoyalty.leavesTextview.text = Html.fromHtml("$loyalty :$number")
                        } else {
                            val loyalty = getColoredSpanned(resources.getString(R.string.leaves), ContextCompat.getColor(requireContext(), R.color.black))
                            val number = getColoredSpanned("0", ContextCompat.getColor(requireContext(), R.color.orange))
                            binding.viewAddLoyalty.leavesTextview.text = Html.fromHtml("$loyalty :$number")
                        }
                    }
                }
                is LoyaltyState.UserDetails -> {
                    if (binding.viewLoyalty.root.isVisible) {
                        binding.viewLoyalty.userNameAppCompatTextView.text = state.data.fullName()
                        binding.viewLoyalty.userEmailAppCompatTextView.text = state.data.userEmail
                        binding.viewLoyalty.userPhoneNumberAppCompatTextView.text = state.data.userPhone
                    } else {
                        binding.viewAddLoyalty.customerNameAppCompatTextView.text = state.data.fullName()
                        binding.viewAddLoyalty.customerEmailAppCompatTextView.text = state.data.userEmail
                        binding.viewAddLoyalty.customerPhoneNumberAppCompatTextView.text = state.data.userPhone
                    }
                }
                is LoyaltyState.AddLoyaltyPointInfo -> {
                    resetAddLeavesUserDetails()
                    visibleMessage(true)
                }
                is LoyaltyState.OrdersLoyaltyInfo -> {
                    if (state.data.data?.isNotEmpty() == true) {
                        binding.viewAddLoyalty.llUserDetails.visibility = View.VISIBLE
                        resetAddLeavesUserDetails()
                        visibleMessage(true)
                    } else {
                        binding.viewAddLoyalty.llUserDetails.visibility = View.VISIBLE
                        resetAddLeavesUserDetails()
                        visibleMessage(false)
                    }
                }
            }
        }.autoDispose()
    }

    private fun viewLoyaltyVisibility(loading: Boolean) {
        binding.viewLoyalty.searchButton.visibility = if (loading) View.INVISIBLE else View.VISIBLE
        binding.viewLoyalty.progressBar.visibility = if (loading) View.VISIBLE else View.INVISIBLE
        binding.viewLoyalty.ListProgressBar.visibility = if (loading) View.VISIBLE else View.INVISIBLE
        binding.viewLoyalty.rvLoyalty.visibility = if (!loading) View.VISIBLE else View.INVISIBLE
        if (loading) binding.viewLoyalty.tvEmpty.isVisible = false
    }

    private fun visibleMessage(isVisible: Boolean) {
        binding.viewAddLoyalty.tvLeavesApplied.visibility = if (isVisible) View.VISIBLE else  View.INVISIBLE
        binding.viewAddLoyalty.rlPhoneSearch.visibility = if (isVisible) View.INVISIBLE else View.VISIBLE
        binding.viewAddLoyalty.customerEmailAppCompatTextView.visibility = if (isVisible) View.INVISIBLE else View.VISIBLE
        binding.viewAddLoyalty.phoneSearchEditText.visibility = if (isVisible) View.INVISIBLE else View.VISIBLE
        binding.viewAddLoyalty.customerPhoneNumberAppCompatTextView.visibility = if (isVisible) View.INVISIBLE else View.VISIBLE
        binding.viewAddLoyalty.customerNameAppCompatTextView.visibility = if (isVisible) View.INVISIBLE else View.VISIBLE
        binding.viewAddLoyalty.leavesTextview.visibility = if (isVisible) View.INVISIBLE else View.VISIBLE
        binding.viewAddLoyalty.addLeavesButton.visibility = if (isVisible) View.INVISIBLE else View.VISIBLE
    }

    private fun addLoyaltyVisibility(loading: Boolean) {
        binding.viewAddLoyalty.searchButton.visibility = if (loading) View.INVISIBLE else View.VISIBLE
        binding.viewAddLoyalty.progressBar.visibility = if (loading) View.VISIBLE else View.INVISIBLE
    }
    private fun addUserVisibility(loading: Boolean) {
        binding.viewAddLoyalty.searchPhoneButton.visibility = if (loading) View.INVISIBLE else View.VISIBLE
        binding.viewAddLoyalty.phoneSearchProgressBar.visibility = if (loading) View.VISIBLE else View.INVISIBLE
    }

    private fun resetAddLoyaltyScreen() {
        orderId = 0
        userId = ""
        binding.viewAddLoyalty.phoneSearchEditText.setText("")
        resetAddLeavesUserDetails()
        binding.viewAddLoyalty.orderId.text = ""
        orderDetailsAdapter.listOfOrderDetailsInfo = arrayListOf()
        binding.viewAddLoyalty.specialTextLinear.isVisible = false
        binding.viewAddLoyalty.orderPrizePart.tvOrderPrizeNumber.text = "$0.00"
        binding.viewAddLoyalty.orderPrizePart.tvTaxPrize.text = "$0.00"
        binding.viewAddLoyalty.orderPrizePart.tvTotalPrizeNumber.text = "$0.00"
        binding.viewAddLoyalty.orderPrizePart.rlCredit.isVisible = false
        binding.viewAddLoyalty.orderPrizePart.rlRefund.isVisible = false
        binding.viewAddLoyalty.orderPrizePart.orderCardAndBowRelativeLayout.isVisible = false
        binding.viewAddLoyalty.orderPrizePart.orderEmployeeDiscountRelativeLayout.isVisible = false
        binding.viewAddLoyalty.orderPrizePart.orderPromocodeRelativeLayout.isVisible = false
        binding.viewAddLoyalty.orderPrizePart.orderTipRelativeLayout.isVisible = false
        binding.viewAddLoyalty.orderPrizePart.orderDeliveryRelativeLayout.isVisible = false
        binding.viewAddLoyalty.llUserDetails.visibility = View.INVISIBLE
    }
    private fun resetAddLeavesUserDetails() {
        binding.viewAddLoyalty.customerEmailAppCompatTextView.text = "-"
        binding.viewAddLoyalty.phoneSearchEditText.setText("")
        binding.viewAddLoyalty.customerPhoneNumberAppCompatTextView.text = "-"
        binding.viewAddLoyalty.customerNameAppCompatTextView.text = "-"
        binding.viewAddLoyalty.leavesTextview.text = "-"
    }
    private fun resetViewLoyaltyScreen() {
        viewLoyaltyUserId = ""
        binding.viewLoyalty.phoneSearchEditText.setText("")
        binding.viewLoyalty.userEmailAppCompatTextView.text = "-"
        binding.viewLoyalty.userPhoneNumberAppCompatTextView.text = "-"
        binding.viewLoyalty.userNameAppCompatTextView.text = "-"
        orderDetailsAdapter.listOfOrderDetailsInfo = arrayListOf()
        loyaltyPointAdapter.listOfLoyaltyPoint = arrayListOf()
        val loyalty = getColoredSpanned(resources.getString(R.string.leaves), ContextCompat.getColor(requireContext(), R.color.black))
        val number = getColoredSpanned(resources.getString(R.string._0), ContextCompat.getColor(requireContext(), R.color.orange))
        binding.viewLoyalty.tvLeaves.text = Html.fromHtml("$loyalty: $number")
    }

    private fun initOrderDetailsUI(orderDetails: OrderDetailsResponse) {
        orderDetails.orderInstructions?.let {
            binding.viewAddLoyalty.specialTextLinear.isVisible = true
            binding.viewAddLoyalty.specialInstructionsTextView.text = it.toString()
        }
        if (!orderDetails.status.isNullOrEmpty()) {
            if (orderDetails.status.last().user == null) {
                orderDetails.status.last().user = HealthNutUser(
                    firstName = orderDetails.guest?.firstOrNull()?.guestFirstName ?: "",
                    lastName = orderDetails.guest?.firstOrNull()?.guestLastName ?: ""
                )
            }
        }
        setOrderDetailsData(orderDetails.cartGroup?.cart)
        orderDetails.orderTotal = orderDetails.orderTotal?.minus((orderDetails.orderEmpDiscount ?: 0.00))
        orderDetails.orderTotal = orderDetails.orderTotal?.minus((orderDetails.orderRefundAmount ?: 0.00))
        val total = orderDetails.orderTotal
        if (total != null) {
            orderDetails.orderTotal = if (total < 0) 0.00 else total
        }
        orderDetails.orderTotal?.let {

            binding.viewAddLoyalty.orderPrizePart.tvTotalPrizeNumber.text = ((it).div(100)).toDollar()
        }
        orderDetails.orderRefundAmount?.let {
            if (it != 0.00) {
                binding.viewAddLoyalty.orderPrizePart.rlRefund.isVisible = true
                binding.viewAddLoyalty.orderPrizePart.tvRefundAmount.text = "-${it.div(100).toDollar()}"
            }
        }
        orderDetails.orderSubtotal?.let {
            binding.viewAddLoyalty.orderPrizePart.tvOrderPrizeNumber.text = ((it).div(100)).toDollar()

        }
        orderDetails.orderTax?.let {
            if (!it.equals(0.0)) {
                binding.viewAddLoyalty.orderPrizePart.orderTaxRelativeLayout.isVisible = true
                binding.viewAddLoyalty.orderPrizePart.tvTaxPrize.text = ((it).div(100)).toDollar()
            }
        }
        orderDetails.orderTip?.let {
            if (!it.equals(0.0)) {
                binding.viewAddLoyalty.orderPrizePart.orderTipRelativeLayout.isVisible = true
                binding.viewAddLoyalty.orderPrizePart.tvTipsPrize.text = ((it).div(100)).toDollar()
            }
        }
        orderDetails.orderDeliveryFee?.let {
            if (!it.equals(0.0)) {
                binding.viewAddLoyalty.orderPrizePart.orderDeliveryRelativeLayout.isVisible = true
                binding.viewAddLoyalty.orderPrizePart.tvDeliveryCharge.text = ((it).div(100)).toDollar()
            }
        }
        orderDetails.orderCouponCodeDiscount?.let {
            if (!it.equals(0.0)) {
                binding.viewAddLoyalty.orderPrizePart.orderPromocodeRelativeLayout.isVisible = true
                binding.viewAddLoyalty.orderPrizePart.tvPromocodeDiscountPrize.text = "-${((it).div(100)).toMinusDollar().toDollar()}"
            }
        }
        orderDetails.orderEmpDiscount?.let {
            if (!it.equals(0.0)) {
                binding.viewAddLoyalty.orderPrizePart.orderEmployeeDiscountRelativeLayout.isVisible = true
                binding.viewAddLoyalty.orderPrizePart.tvEmployeeDiscountPrize.text = "-${((it).div(100)).toMinusDollar().toDollar()}"
            }
        }
        orderDetails.orderGiftCardAmount?.let {
            if (!it.equals(0.0)) {
                binding.viewAddLoyalty.orderPrizePart.orderCardAndBowRelativeLayout.isVisible = true
                binding.viewAddLoyalty.orderPrizePart.tvCardAndBowCharge.text = "-${((it).div(100)).toMinusDollar().toDollar()}"
            }
        }
        orderDetails.orderCreditAmount?.let {
            if (!it.equals(0.0)) {
                binding.viewAddLoyalty.orderPrizePart.rlCredit.isVisible = true
                binding.viewAddLoyalty.orderPrizePart.tvCreditAmount.text = "-${((it).div(100)).toMinusDollar().toDollar()}"
            }
        }
        binding.viewAddLoyalty.orderId.text = orderDetails.getSafeOrderId()

    }

    private fun viewOrAddLoyaltySelection(viewLoyalty: Boolean) {
        binding.viewLoyaltySelectLinear.isSelected = !viewLoyalty
        binding.viewLoyaltyTextview.isSelected = !viewLoyalty
        binding.viewLoyalty.root.isVisible = !viewLoyalty
        binding.addLoyaltyTextview.isSelected = viewLoyalty
        binding.addLoyaltySelectLinear.isSelected = viewLoyalty
        binding.viewAddLoyalty.root.isVisible = viewLoyalty
    }

    private fun setOrderDetailsData(orderDetailsInfo: List<CartItem>?) {
        orderDetailsAdapter.listOfOrderDetailsInfo = orderDetailsInfo
    }

    private fun isPhoneValidate(): Boolean {
        return when {
            binding.viewLoyalty.phoneSearchEditText.isNotValidPhoneLength() -> {
                showToast(getString(R.string.invalid_phone))
                false
            }
            binding.viewLoyalty.phoneSearchEditText.isPhoneNumber() -> {
                showToast(getString(R.string.invalid_phone))
                false
            }
            binding.viewLoyalty.phoneSearchEditText.isFieldBlank() -> {
                showToast(getString(R.string.invalid_phone_number))
                false
            }
            else -> true
        }
    }

    private fun isPhoneForAddLeavesValidate(): Boolean {
        return when {
            binding.viewAddLoyalty.phoneSearchEditText.isNotValidPhoneLength() -> {
                showToast(getString(R.string.invalid_phone))
                false
            }
            binding.viewAddLoyalty.phoneSearchEditText.isPhoneNumber() -> {
                showToast(getString(R.string.invalid_phone))
                false
            }
            binding.viewAddLoyalty.phoneSearchEditText.isFieldBlank() -> {
                showToast(getString(R.string.invalid_phone_number))
                false
            }
            else -> true
        }
    }

    private fun isOrderValidate(): Boolean {
        return when {
            binding.viewAddLoyalty.orderIdEditText.isFieldBlank() -> {
                showToast(getString(R.string.invalid_order_id))
                false
            }

            else -> true
        }
    }


    private fun getColoredSpanned(text: String, color: Int): String {
        return "<font color=$color>$text</font>"
    }

}