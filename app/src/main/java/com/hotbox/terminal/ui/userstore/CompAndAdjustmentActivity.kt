package com.hotbox.terminal.ui.userstore

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.hotbox.terminal.R
import com.hotbox.terminal.api.authentication.LoggedInUserCache
import com.hotbox.terminal.api.userstore.model.*
import com.hotbox.terminal.application.HotBoxApplication
import com.hotbox.terminal.base.BaseActivity
import com.hotbox.terminal.base.RxBus
import com.hotbox.terminal.base.RxEvent
import com.hotbox.terminal.base.ViewModelFactory
import com.hotbox.terminal.base.extension.*
import com.hotbox.terminal.databinding.ActivityCompAndAdjustmentBinding
import com.hotbox.terminal.ui.userstore.editcart.EditCartActivity
import com.hotbox.terminal.ui.userstore.view.CartAdapter
import com.hotbox.terminal.ui.userstore.viewmodel.UserStoreState
import com.hotbox.terminal.ui.userstore.viewmodel.UserStoreViewModel
import com.hotbox.terminal.utils.Constants
import javax.inject.Inject
import kotlin.math.abs

class CompAndAdjustmentActivity : BaseActivity() {

    private var discountAmount: Double? = 0.00
    private var adjustmentAmount: Double? = 0.00
    private var giftCardAmount: Double? = 0.00
    private var promoCodeAmount: Double? = 0.00

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<UserStoreViewModel>
    private lateinit var userStoreViewModel: UserStoreViewModel

    private lateinit var binding: ActivityCompAndAdjustmentBinding
    private lateinit var cartAdapter: CartAdapter
    private lateinit var deletedCartItemId: CartItem
    private var orderSubTotal: Double = 0.0
    private var orderTax: Double = 0.0
    private var orderSubTotalCount: Double = 0.00
    private var orderTotal: Double? = 0.0
    private var adjustmentType: AdjustmentType = AdjustmentType.ADJUSTMENT_POSITIVE_TYPE
    var listOfProductDetails: ArrayList<CartItem>? = null


    companion object {

        const val EMPLOYEE_DISCOUNT = "EMPLOYEE_DISCOUNT"
        const val ADJUSTMENT_DISCOUNT = "ADJUSTMENT_DISCOUNT"
        const val GIFT_CARD_DISCOUNT = "GIFT_CARD_DISCOUNT"
        const val PROMOCODE_DISCOUNT = "PROMOCODE_DISCOUNT"
        fun getIntent(
            context: Context, employeeDiscount: Double, adjustmentDiscount: Double, giftCardAmount: Double, promoCodeAmount: Double
        ): Intent {
            val intent = Intent(context, CompAndAdjustmentActivity::class.java)
            intent.putExtra(EMPLOYEE_DISCOUNT, employeeDiscount)
            intent.putExtra(ADJUSTMENT_DISCOUNT, adjustmentDiscount)
            intent.putExtra(GIFT_CARD_DISCOUNT, giftCardAmount)
            intent.putExtra(PROMOCODE_DISCOUNT, promoCodeAmount)
            return intent
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCompAndAdjustmentBinding.inflate(layoutInflater)
        HotBoxApplication.component.inject(this)
        setContentView(binding.root)
        userStoreViewModel = getViewModelFromFactory(viewModelFactory)
        listenToViewModel()
        window.decorView.apply {
            systemUiVisibility = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_FULLSCREEN
        }
        initUI()
    }

    private fun initUI() {
        intent?.let {
            discountAmount = it.getDoubleExtra(EMPLOYEE_DISCOUNT, 0.00)
            adjustmentAmount = it.getDoubleExtra(ADJUSTMENT_DISCOUNT, 0.00)
            promoCodeAmount = it.getDoubleExtra(PROMOCODE_DISCOUNT, 0.00)
            giftCardAmount = it.getDoubleExtra(GIFT_CARD_DISCOUNT, 0.00)
        }
        initAdapter()
        val arrayAdapter =
            ArrayAdapter.createFromResource(this@CompAndAdjustmentActivity, R.array.adjustmentArray, android.R.layout.simple_spinner_dropdown_item)
        binding.autoCompleteStatus.setAdapter(arrayAdapter)
        binding.locationAppCompatTextView.text = loggedInUserCache.getLocationInfo()?.location?.locationName ?: ""
        binding.iAmFinishMaterialButton.throttleClicks().subscribeAndObserveOnMainThread {
            finish()
        }.autoDispose()
        binding.autoCompleteStatus.throttleClicks().subscribeAndObserveOnMainThread {
            hideKeyboard(binding.autoCompleteStatus.rootView)
            binding.autoCompleteStatus.setAdapter(arrayAdapter)
            binding.autoCompleteStatus.showDropDown()
        }.autoDispose()
        binding.autoCompleteStatus.onItemClickListener = AdapterView.OnItemClickListener { parent, _, position, _ ->
            when (position) {
                0 -> {
                    adjustmentType = AdjustmentType.ADJUSTMENT_POSITIVE_TYPE
                    clearAdjustment()
                }
                1 -> {
                    adjustmentType = AdjustmentType.ADJUSTMENT_NEGATIVE_TYPE
                    clearAdjustment()
                }
                else -> {
                    adjustmentType = AdjustmentType.ADJUSTMENT_POSITIVE_TYPE
                    clearAdjustment()
                }
            }
        }
        discountAmount?.let { discountAmount = it.div(100) }
        adjustmentAmount?.let {
            adjustmentAmount = it
            if (it != 0.00) {
                if (it > 0) {
                    binding.amountEditText.setText(it.div(100).toDollar())
                    binding.confirmMaterialButton.text = resources.getString(R.string.update_adjustment)
                    binding.clearMaterialButton.isVisible = true
                } else {
                    binding.amountEditText.setText("-".plus(abs(it).div(100).toDollar()))
                    binding.confirmMaterialButton.text = resources.getString(R.string.update_adjustment)
                    binding.clearMaterialButton.isVisible = true
                }
            }
        }
        promoCodeAmount?.let {
            if (it != 0.00) {
                binding.rlPromocode.isVisible = true
                binding.tvPromoCodeDiscount.text = "-".plus(it.div(100))
            }
        }
        giftCardAmount?.let {
            if (it != 0.00) {
                binding.rlGiftCard.isVisible = true
                binding.tvCardAndBowCharge.text = "-".plus(it.div(100))
            }
        }
        binding.employeeDiscountMaterialButton.throttleClicks().subscribeAndObserveOnMainThread {
            if (discountAmount == 0.00) {
                if (orderTotal == 0.00) {
                    showToast("Unable to use employee discount")
                } else {
                    discountAmount = orderSubTotalCount.times(20).div(100)
                    if ((discountAmount ?: 0.00) < (orderTotal ?: 0.00)) {
                        binding.rlEmployeeDiscount.isVisible = true
                        binding.employeeDiscountMaterialButton.isEnabled = false
                        orderTotalCount()
                        discountAmount?.let {
                            RxBus.publish(RxEvent.AddEmployeeDiscount(it.times(100)))
                        }
                        println("OkHttpClient  : discountAmount  =====> $discountAmount")
                    } else {
                        discountAmount = 0.00
                        showToast("employee discount price more than total so that Unable to use employee discount")
                    }
                }
            } else {
                showToast("Already used employee discount")
            }
        }.autoDispose()
        binding.adjustmentsMaterialButton.throttleClicks().subscribeAndObserveOnMainThread {
            binding.amountEditText.setText("$")
            binding.rlAmount.isVisible = true
            binding.adjustmentsMaterialButton.isVisible = false
            binding.amountEditText.requestFocus()
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(binding.amountEditText, InputMethodManager.SHOW_IMPLICIT)
        }.autoDispose()
        binding.clearMaterialButton.throttleClicks().subscribeAndObserveOnMainThread {
            clearAdjustment()
        }.autoDispose()
        binding.confirmMaterialButton.throttleClicks().subscribeAndObserveOnMainThread {
            hideKeyboard(binding.confirmMaterialButton.rootView)
            if (isAdjustmentValidate()) {
                if (orderTotal != null) {
                    if (adjustmentType == AdjustmentType.ADJUSTMENT_POSITIVE_TYPE) {
                        val adjustment = binding.amountEditText.text.toString()
                        val amount = (adjustment.removePrefix("$").toDouble() * 100)
                        binding.confirmMaterialButton.text = resources.getString(R.string.update_adjustment)
                        adjustmentAmount = amount
                        adjustmentAmount = (adjustmentAmount ?: 0.00).times(1)
                        binding.rlAdjustment.isVisible = true
                        binding.clearMaterialButton.isVisible = true
                        adjustmentAmount?.let {
                            RxBus.publish(RxEvent.AddAdjustmentDiscount(it))
                        }
                        orderTotalCount()
                        binding.tvAdjustmentDiscountPrize.text = amount.div(100).toDollar()
                    } else {
                        var orderTotal = adjustmentAmount?.div(100)?.let { it1 -> orderTotal?.minus(it1).toConvertDecimalFormat() }
                        val adjustment = binding.amountEditText.text.toString()
                        val amount = (adjustment.removePrefix("$").toDouble() * 100)
                        if (orderTotal != null) {
                            if ((orderTotal ?:0.00) >= amount.div(100)) {
                                binding.rlAdjustment.isVisible = true
                                binding.clearMaterialButton.isVisible = true
                                adjustmentAmount = amount
                                binding.confirmMaterialButton.text = resources.getString(R.string.update_adjustment)
                                binding.tvAdjustmentDiscountPrize.text = "-".plus(amount.div(100).toDollar())
                                adjustmentAmount = adjustmentAmount!!.times(-1)
                                adjustmentAmount?.let {
                                    RxBus.publish(RxEvent.AddAdjustmentDiscount(it))
                                }
                                orderTotalCount()
                            } else {
                                binding.amountEditText.setText("$")
                                showToast("please enter Valid Adjustment Amount")
                            }
                        }
                    }

                }
            }
        }.autoDispose()
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
                        var bottomSheetPostMoreItem: BottomSheetCompReason = BottomSheetCompReason.newInstance()
                        bottomSheetPostMoreItem.compReasonClick.subscribeAndObserveOnMainThread { item ->
                            it.data.compReason = item
                            cartAdapter.notifyDataSetChanged()
                        }.autoDispose()
                        bottomSheetPostMoreItem.show(supportFragmentManager, "TAGS")
                    }
                    is CartItemClickStates.CartItemConfirmButtonClick -> {
                        if (it.data.compReason?.type.isNullOrEmpty()) {
                            showToast("Please select a comp reason")
                        } else {
                            userStoreViewModel.compProduct(
                                CompProductRequest(
                                    it.data.id, menuItemCompReason = it.data.compReason?.type, menuItemFullComp = true
                                )
                            )
                        }
                    }
                    is CartItemClickStates.CartItemDeleteClick -> {
                        it.data.let { item -> deletedCartItemId = item }
                        it.data.id?.let { item -> userStoreViewModel.deleteCartItem(item) }
                    }
                    is CartItemClickStates.CartItemEditClick -> {
                        if (!it.data.menuItemModifiers.isNullOrEmpty()) {
                            startActivityWithDefaultAnimation(EditCartActivity.getIntent(this@CompAndAdjustmentActivity, it.data))
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

                    }
                }
            }.autoDispose()
        }
        binding.cartRecycleView.layoutManager = LinearLayoutManager(this, RecyclerView.VERTICAL, false)
        binding.cartRecycleView.apply {
            adapter = cartAdapter
        }
    }

    private fun clearAdjustment() {
        hideKeyboard()
        binding.amountEditText.setText("$")
        adjustmentAmount = 0.00
        binding.confirmMaterialButton.text = resources.getString(R.string.confirm_adjustment)
        binding.clearMaterialButton.isVisible = false
        binding.rlAdjustment.isVisible = false
        orderTotalCount()
        RxBus.publish(RxEvent.AddAdjustmentDiscount(0.00))
    }

    private fun listenToViewModel() {
        userStoreViewModel.userStoreState.subscribeAndObserveOnMainThread {
            when (it) {
                is UserStoreState.ErrorMessage -> {
                    showToast(it.errorMessage)
                }
                is UserStoreState.LoadingState -> {
                    showLoading(it.isLoading)
                }
                is UserStoreState.SuccessMessage -> {
                    showToast(it.successMessage)
                }
                is UserStoreState.UpdatedCartInfo -> {
                    if (this::cartAdapter.isInitialized) {
                        val updatedCartItemId = it.cartInfo?.id
                        val productQuantity = it.cartInfo?.menuItemQuantity
                        val cartList = cartAdapter.listOfProductDetails
                        cartList?.find { item -> updatedCartItemId == item.id }?.apply {
                            menuItemQuantity = productQuantity
                        }
                        cartAdapter.listOfProductDetails = cartList
                        listOfProductDetails = cartList as ArrayList<CartItem>?
                        orderTotalCount()
                    }
                }
                is UserStoreState.CompProductProductResponse -> {
                    userStoreViewModel.getCartDetails(loggedInUserCache.getLoggedInUserCartGroupId() ?: 0)
                }
                is UserStoreState.DeletedCartItem -> {
                    val cartList: ArrayList<CartItem> = cartAdapter.listOfProductDetails as ArrayList<CartItem>
                    if (cartList.contains(deletedCartItemId)) {
                        cartList.remove(deletedCartItemId)
                    }
                    cartAdapter.listOfProductDetails = cartList
                    listOfProductDetails = cartList
                    orderTotalCount()

                }
                is UserStoreState.CartDetailsInfo -> {
                    listOfProductDetails = it.cartInfo.cart as ArrayList<CartItem>?
                    setCartData(it.cartInfo.cart)
                    orderTotalCount()
                }
                is UserStoreState.MenuInfo -> {
                }
                is UserStoreState.StoreResponses -> {

                }
                else -> {

                }
            }
        }.autoDispose()
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.isVisible = isLoading
    }

    private fun setCartData(cartItem: ArrayList<CartItem>?) {
        if (cartItem?.size != 0) {
            cartItem?.filter { it.isChanging == false }?.forEach {
                it.isChanging = true
            }
            cartItem?.forEach {
                it.isVisibleComp = true
            }
            cartAdapter.listOfProductDetails = cartItem
            listOfProductDetails = cartItem as ArrayList<CartItem>?
            binding.rlTax.isVisible = true
            binding.rlOrderPrice.isVisible = true
            binding.rlTotal.isVisible = true
            binding.tvTotalPrizeNumber.isVisible = true
            orderTotalCount()
        } else {
            binding.tvTotalPrizeNumber.isVisible = false
            cartAdapter.listOfProductDetails = null
        }
    }

    private fun orderTotalCount() {
        orderSubTotal = 0.00
        var productTotal = 0.00
        listOfProductDetails?.forEach { it ->
            it.menuItemPrice?.let {
                productTotal = it
            }
            it.menuItemModifiers?.forEach {
                it.options?.forEach {
                    productTotal = productTotal.plus(it.optionPrice!!)
                }
            }
            productTotal = it.menuItemQuantity?.let { it1 -> productTotal?.times(it1) } ?: 0.00
            orderSubTotal = orderSubTotal.plus(productTotal)
        }
        orderSubTotalCount = orderSubTotal.div(100)
        discountAmount?.let {
            if (it != 0.00) {
                discountAmount = orderSubTotalCount.times(20).div(100)
            }
        }

        adjustmentAmount?.let {
            if (adjustmentAmount != 0.00) {
                binding.rlAdjustment.isVisible = true
                if (it > 0) {
                    binding.tvAdjustmentDiscountPrize.text = abs(it).div(100).toDollar()
                } else {
                    binding.tvAdjustmentDiscountPrize.text = "-".plus(abs(it).div(100).toDollar())
                }
            }
        }
        orderTax = orderSubTotalCount.times(9.5).div(100)
        orderTotal = orderSubTotalCount + orderTax
        promoCodeAmount?.let {
            if (it != 0.00) {
                orderTotal = orderTotal!! - it.div(100)
                binding.rlPromocode.isVisible = true
                binding.tvPromoCodeDiscount.text = "-".plus(it.div(100).toDollar())
            }
        }
        giftCardAmount?.let {
            if (it != 0.00) {
                orderTotal = orderTotal!! - it.div(100)
                binding.rlGiftCard.isVisible = true
                binding.tvCardAndBowCharge.text = "-".plus(it.div(100).toDollar())
            }
        }
        discountAmount?.let {
            if (it != 0.00) {
                binding.rlEmployeeDiscount.isVisible = true
            }
            orderTotal = (orderTotal ?: 0.00) - it
        }
        adjustmentAmount?.let {
            if (adjustmentAmount != 0.00) {
                orderTotal = orderTotal.toConvertDecimalFormat() + it.div(100)
            }
        }
        binding.tvOrderPrizeNumber.isVisible = true
        binding.tvTaxNumber.isVisible = true
        binding.tvTotalPrizeNumber.isVisible = true
        binding.tvOrderPrizeNumber.text = orderSubTotalCount.toConvertDecimalFormat().toDollar()
        binding.tvTaxNumber.text = orderTax.toConvertDecimalFormat().toDollar()
        binding.tvEmployeeDiscountPrize.text = "-" + discountAmount.toConvertDecimalFormat().toDollar()
        binding.tvTotalPrizeNumber.text = orderTotal.toConvertDecimalFormat().toDollar()
        binding.tvTotalPrizeNumber.text = orderTotal.toConvertDecimalFormat().toDollar()
    }

    private fun isAdjustmentValidate(): Boolean {
        return when {
            binding.amountEditText.isFieldBlank() -> {
                Toast.makeText(this@CompAndAdjustmentActivity, getString(R.string.invalid_amount), Toast.LENGTH_SHORT).show()
                false
            }
            binding.amountEditText.text.toString() == "$" -> {
                Toast.makeText(this@CompAndAdjustmentActivity, getString(R.string.invalid_amount), Toast.LENGTH_SHORT).show()
                false
            }
            else -> true
        }
    }

    override fun onResume() {
        super.onResume()
        userStoreViewModel.getCartDetails(loggedInUserCache.getLoggedInUserCartGroupId() ?: 0)
    }
}