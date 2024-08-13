package com.hotbox.terminal.ui.userstore.editcart

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.hotbox.terminal.R
import com.hotbox.terminal.api.authentication.LoggedInUserCache
import com.hotbox.terminal.api.menu.model.ProductsItem
import com.hotbox.terminal.api.userstore.model.AddToCartRequest
import com.hotbox.terminal.api.userstore.model.CartItem
import com.hotbox.terminal.api.userstore.model.CompProductRequest
import com.hotbox.terminal.api.userstore.model.UpdateMenuItemQuantity
import com.hotbox.terminal.application.HotBoxApplication
import com.hotbox.terminal.base.BaseDialogFragment
import com.hotbox.terminal.base.RxBus
import com.hotbox.terminal.base.RxEvent
import com.hotbox.terminal.base.ViewModelFactory
import com.hotbox.terminal.base.extension.*
import com.hotbox.terminal.databinding.FragmentAddToCartDialogBinding
import com.hotbox.terminal.databinding.FragmentEditCartBinding
import com.hotbox.terminal.ui.userstore.AddToCartDialogFragment
import com.hotbox.terminal.ui.userstore.customize.CustomizeOrderActivity
import com.hotbox.terminal.ui.userstore.viewmodel.UserStoreState
import com.hotbox.terminal.ui.userstore.viewmodel.UserStoreViewModel
import com.hotbox.terminal.utils.Constants
import com.hotbox.terminal.utils.UserInteractionInterceptor
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

class EditCartFragment : BaseDialogFragment() {

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<UserStoreViewModel>
    private lateinit var userStoreViewModel: UserStoreViewModel
    private var _binding: FragmentEditCartBinding? = null
    private val binding get() = _binding!!
    private var productsItem: ProductsItem? = null
    private var productQuantity = 1
    private var isCompProduct: Boolean? = false
    private var nowRedeemed: Int? = 0

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache

    companion object {
        @SuppressLint("StaticFieldLeak")
        var listOfProduct: CartItem? = null
            set(value) {
                field = value
                updateItems()
            }
        var isRedeemProduct: Int? = 0
            set(value) {
                field = value
                updateItems()
            }

        private fun updateItems() {

        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        HotBoxApplication.component.inject(this)
        userStoreViewModel = getViewModelFromFactory(viewModelFactory)
        if(loggedInUserCache.getIsEmployeeMeal() == true) {
            setStyle(STYLE_NORMAL, R.style.MyGreenDialog)
        }else{
            setStyle(STYLE_NORMAL, R.style.MyDialog)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentEditCartBinding.inflate(inflater, container, false)
        dialog?.window?.requestFeature(Window.FEATURE_NO_TITLE)
        dialog?.window?.decorView?.apply {
            systemUiVisibility =
                (View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        listenToViewModel()
        listenToViewEvent()
    }

    private fun listenToViewModel() {
        userStoreViewModel.userStoreState.subscribeAndObserveOnMainThread {
            when (it) {
                is UserStoreState.LoadingState -> {
                    buttonVisibility(it.isLoading)
                }
                is UserStoreState.AddToCartProductResponse -> {
                    if (loggedInUserCache.getIsEmployeeMeal() == true) {
                        var cartGroupId = 0
                        it.addToCartResponse.cartGroup?.id?.let { cartGroupId = it }
                        loggedInUserCache.setLoggedInUserCartGroupId(cartGroupId)
                        userStoreViewModel.compProduct(CompProductRequest(it.addToCartResponse.id, true, menuItemCompReason = "employee_meal"))
                    } else {
                        if (isRedeemProduct == 1) {
                            var cartGroupId = 0
                            it.addToCartResponse.cartGroup?.id?.let { cartGroupId = it }
                            loggedInUserCache.setLoggedInUserCartGroupId(cartGroupId)
                            if ((it.addToCartResponse.menuItemQuantity ?: 0) > 1) {
                                it.addToCartResponse.id.let { it1 ->
                                    userStoreViewModel.redeemCartItem(
                                        UpdateMenuItemQuantity(
                                            cartId = it1
                                        ), listOfProduct?.menu?.product?.productLoyaltyTier?.tierValue ?: 0
                                    )
                                }
                            } else {
                                it.addToCartResponse.id.let { it1 ->
                                    userStoreViewModel.updateMenuItemQuantity(
                                        UpdateMenuItemQuantity(
                                            cartId = it1, menuItemQuantity = 1, menuItemRedemption = true
                                        ), listOfProduct?.menu?.product?.productLoyaltyTier?.tierValue ?: 0
                                    )
                                }
                            }
                        } else {
                            var cartGroupId = 0
                            it.addToCartResponse.cartGroup?.id?.let { cartGroupId = it }
                            loggedInUserCache.setLoggedInUserCartGroupId(cartGroupId)
                            RxBus.publish(RxEvent.EventCartGroupIdListen(cartGroupId))
                            this.dismiss()
                        }

                    }
                }
                is UserStoreState.CompProductProductResponse -> {
                    val cartGroupId = loggedInUserCache.getLoggedInUserCartGroupId()
                    RxBus.publish(RxEvent.EventCartGroupIdListen(cartGroupId ?: 0))
                    dismiss()
                }
                is UserStoreState.RedeemProduct -> {
                    val cartGroupId = loggedInUserCache.getLoggedInUserCartGroupId() ?: 0
                    RxBus.publish(RxEvent.EventCartGroupIdListen(cartGroupId))
                    dismiss()
                }
                is UserStoreState.UpdatedCartInfo -> {
                    if (loggedInUserCache.getIsEmployeeMeal() == true) {
                        val cartGroupId = loggedInUserCache.getLoggedInUserCartGroupId()
                        RxBus.publish(RxEvent.EventCartGroupIdListen(cartGroupId ?: 0))
                        dismiss()
                    } else {
                        if (isRedeemProduct != 1 && it.cartInfo?.menuItemRedemption == false && nowRedeemed == 1) {
                            if ((it.cartInfo.menuItemQuantity ?: 0) > 1) {
                                it.cartInfo.id.let { it1 ->
                                    userStoreViewModel.redeemCartItem(
                                        UpdateMenuItemQuantity(
                                            cartId = it1
                                        ), listOfProduct?.menu?.product?.productLoyaltyTier?.tierValue ?: 0
                                    )
                                }
                            } else {
                                isRedeemProduct = 0
                                it.cartInfo.id.let { it1 ->
                                    userStoreViewModel.updateMenuItemQuantity(
                                        UpdateMenuItemQuantity(
                                            cartId = it1, menuItemQuantity = 1, menuItemRedemption = true
                                        ), listOfProduct?.menu?.product?.productLoyaltyTier?.tierValue ?: 0
                                    )
                                }
                            }
                        } else {
                            val cartGroupId = loggedInUserCache.getLoggedInUserCartGroupId()
                            RxBus.publish(RxEvent.EventCartGroupIdListen(cartGroupId ?: 0))
                            dismiss()
                        }
                    }

                }
                is UserStoreState.ErrorMessage -> {
                    showToast(it.errorMessage)
                }
                is UserStoreState.SubProductState -> {
                    productsItem = it.productsItem
                    binding.addToCartMaterialButton.isEnabled = true
                    initUI(it.productsItem)
                }
                is UserStoreState.SuccessMessage -> {
                    showToast(it.successMessage)
                }
                else -> {

                }
            }
        }.autoDispose()
    }

    private fun buttonVisibility(isLoading: Boolean) {
        binding.addToCartMaterialButton.visibility = if (isLoading) View.GONE else View.VISIBLE
        if (binding.llRedeem.isVisible) {
            binding.llRedeem.visibility = if (isLoading) View.GONE else View.VISIBLE
        }
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
    }

    @SuppressLint("SetTextI18n")
    private fun initUI(productsItem: ProductsItem?) {
        binding.productNameTextView.text = productsItem?.productName
        productQuantity = listOfProduct?.menuItemQuantity ?: 0
        binding.productQuantityAppCompatTextView.text = productQuantity.toString()
        if (isRedeemProduct != 0) {
            binding.orderPrizeTextView.text = listOfProduct?.menu?.product?.productLoyaltyTier?.tierValue.toString()
            binding.orderPrizeTextView.leftDrawable(resources.getDrawable(R.drawable.ic_trophy_icon))
        } else {
            binding.leaveTextView.isVisible = false
            binding.orderPrizeTextView.clearRightDrawable()
            binding.addToCartMaterialButton.text = getText(R.string.add_to_cart)
            binding.orderPrizeTextView.text = productsItem?.productBasePrice?.div(100).toDollar()
        }
        if (listOfProduct?.menu?.product?.productLoyaltyTier?.tierValue != 0 && listOfProduct?.menu?.product?.productLoyaltyTier?.tierValue != null && loggedInUserCache.getLoyaltyQrResponse()?.id != null && loggedInUserCache.getLoyaltyQrResponse()?.id != ""
            && listOfProduct?.menuItemRedemption == 0 && listOfProduct?.menuItemComp == 0) {
            binding.tvProductPoint.text = listOfProduct?.menu?.product?.productLoyaltyTier?.tierValue.toString().plus(" ").plus(getString(R.string.leaves))
            binding.llRedeem.isVisible = true
        } else {
            binding.tvProductPoint.isVisible = false
            binding.llRedeem.isVisible = false
        }
        binding.productDescriptionTextView.text = productsItem?.productDescription
        productsItem?.productCals?.let {
            binding.productCalTextView.text = it.toString().plus(" cal")
        }
        Glide.with(requireContext()).load(productsItem?.productImage).placeholder(R.drawable.ic_launcher_logo).error(R.drawable.ic_launcher_logo)
            .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC).into(binding.productImageView)
        binding.llRedeem.throttleClicks().subscribeAndObserveOnMainThread {
            if ((productsItem?.productLoyaltyTier?.tierValue ?: 0) <= (userStoreViewModel.getLoyaltyPoint())) {
                nowRedeemed = 1
                addToCartProcess()
            } else {
                showToast("You don't have enough leaves to redeem")
            }
        }.autoDispose()
    }

    @SuppressLint("SimpleDateFormat")
    private fun listenToViewEvent() {
        binding.ivClose.throttleClicks().subscribeAndObserveOnMainThread {
            dismiss()
        }.autoDispose()
        if (listOfProduct?.menuItemInstructions != null) {
            binding.specialInstructionsEditText.setText(listOfProduct?.menuItemInstructions.toString())
        }

        binding.ivAddition.throttleClicks().subscribeAndObserveOnMainThread {
            productQuantity++
            binding.orderPrizeTextView.text = listOfProduct?.menuItemPrice?.div(100)?.times(productQuantity).toDollar()
            binding.productQuantityAppCompatTextView.text = productQuantity.toString()
        }.autoDispose()
        binding.ivSubtraction.throttleClicks().subscribeAndObserveOnMainThread {
            if (productQuantity != 1) {
                productQuantity--
                binding.orderPrizeTextView.text = (listOfProduct?.menuItemPrice?.times(productQuantity))?.div(100).toDollar()
                binding.productQuantityAppCompatTextView.text = productQuantity.toString()
            }
        }.autoDispose()
        binding.addToCartMaterialButton.throttleClicks().subscribeAndObserveOnMainThread {
            if (loggedInUserCache.getIsEmployeeMeal() == true) isCompProduct = true
            addToCartProcess()
        }.autoDispose()

    }

    private fun addToCartProcess() {
        if (binding.specialInstructionsEditText.text?.trim()?.isEmpty() == true){
            userStoreViewModel.updateMenuItemQuantity(UpdateMenuItemQuantity(listOfProduct?.id,productQuantity))
        } else {
            userStoreViewModel.updateMenuItemQuantity(UpdateMenuItemQuantity(listOfProduct?.id,productQuantity, menuItemInstructions = binding.specialInstructionsEditText.text?.trim().toString()))
        }
    }

    override fun onResume() {
        super.onResume()
        userStoreViewModel.getProductDetails(listOfProduct?.menu?.product?.id)
    }

    override fun onStart() {
        super.onStart()
        UserInteractionInterceptor.wrapWindowCallback(requireActivity().window,requireActivity())
    }

}