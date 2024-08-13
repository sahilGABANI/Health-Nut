package com.hotbox.terminal.ui.userstore

import android.annotation.SuppressLint
import android.os.Bundle
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
import com.hotbox.terminal.api.userstore.model.CompProductRequest
import com.hotbox.terminal.api.userstore.model.UpdateMenuItemQuantity
import com.hotbox.terminal.application.HotBoxApplication
import com.hotbox.terminal.base.BaseDialogFragment
import com.hotbox.terminal.base.RxBus
import com.hotbox.terminal.base.RxEvent
import com.hotbox.terminal.base.ViewModelFactory
import com.hotbox.terminal.base.extension.*
import com.hotbox.terminal.databinding.FragmentAddToCartDialogBinding
import com.hotbox.terminal.ui.userstore.customize.CustomizeOrderActivity
import com.hotbox.terminal.ui.userstore.viewmodel.UserStoreState
import com.hotbox.terminal.ui.userstore.viewmodel.UserStoreViewModel
import com.hotbox.terminal.utils.Constants.MODE_ID
import com.hotbox.terminal.utils.UserInteractionInterceptor
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

class AddToCartDialogFragment : BaseDialogFragment() {

    companion object {
        @SuppressLint("StaticFieldLeak")
        var listOfProduct: ProductsItem? = null
            set(value) {
                field = value
                updateItems()
            }

        var menuId: Int? = null
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

    private var buttonisVisible: Boolean = false

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<UserStoreViewModel>
    private lateinit var userStoreViewModel: UserStoreViewModel
    private var _binding: FragmentAddToCartDialogBinding? = null
    private val binding get() = _binding!!
    private var productsItem: ProductsItem? = null
    private var productQuantity = 1
    private var isCompProduct: Boolean? = false

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache

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
    ): View {
        _binding = FragmentAddToCartDialogBinding.inflate(inflater, container, false)
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
        initUI(listOfProduct)
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
                                        ),listOfProduct?.productLoyaltyTier?.tierValue ?: 0
                                    )
                                }
                            } else {
                                it.addToCartResponse.id.let { it1 ->
                                    userStoreViewModel.updateMenuItemQuantity(
                                        UpdateMenuItemQuantity(
                                            cartId = it1, menuItemQuantity = 1, menuItemRedemption = true
                                        ),listOfProduct?.productLoyaltyTier?.tierValue ?: 0
                                    )
                                }
                            }
                        }else {
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
                is UserStoreState.ErrorMessage -> {
                    showToast(it.errorMessage)
                }
                is UserStoreState.SubProductState -> {
//                    productsItem = it.productsItem
                    binding.addToCartMaterialButton.isEnabled = true
//                    initUI(it.productsItem)
                }
                is UserStoreState.SuccessMessage -> {
                    showToast(it.successMessage)
                }
                is UserStoreState.UpdatedCartInfo -> {
                    RxBus.publish(RxEvent.EventCartGroupIdListen(loggedInUserCache.getLoggedInUserCartGroupId() ?: 0))
                    dismiss()
                }
                is UserStoreState.RedeemProduct -> {
                    RxBus.publish(RxEvent.EventCartGroupIdListen(loggedInUserCache.getLoggedInUserCartGroupId() ?: 0))
                    dismiss()
                }
                else -> {

                }
            }
        }.autoDispose()
    }

    private fun buttonVisibility(isLoading: Boolean) {
        binding.addToCartMaterialButton.visibility = if (isLoading) View.GONE else View.VISIBLE
        if(buttonisVisible) {
            binding.llRedeem.visibility = if (isLoading) View.GONE else View.VISIBLE
        }
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
    }

    @SuppressLint("SetTextI18n")
    private fun initUI(productsItem: ProductsItem?) {
        binding.productNameTextView.text = productsItem?.productName
        if (isRedeemProduct != 0) {
            binding.orderPrizeTextView.text = isRedeemProduct.toString()
            binding.addToCartMaterialButton.text = getText(R.string.redeem_products)
            binding.ivSubtraction.isVisible = false
            binding.productQuantityAppCompatTextView.isVisible = false
            binding.ivAddition.isVisible = false
            binding.orderPrizeTextView.leftDrawable(resources.getDrawable(R.drawable.ic_trophy_icon))
        } else {
            binding.leaveTextView.isVisible = false
            binding.orderPrizeTextView.clearRightDrawable()
            binding.addToCartMaterialButton.text = getText(R.string.add_to_cart)
            binding.orderPrizeTextView.text = productsItem?.productBasePrice?.div(100).toDollar()
        }
        if (productsItem?.productLoyaltyTier?.tierValue != 0 && productsItem?.productLoyaltyTier?.tierValue != null && loggedInUserCache.getLoyaltyQrResponse()?.id != null && loggedInUserCache.getLoyaltyQrResponse()?.id != "") {
            binding.tvProductPoint.text = productsItem.productLoyaltyTier?.tierValue.toString().plus(" ").plus(getString(R.string.leaves))
            binding.llRedeem.isVisible = loggedInUserCache.getIsEmployeeMeal() != true
            buttonisVisible = loggedInUserCache.getIsEmployeeMeal() != true
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
                isRedeemProduct = 1
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
        binding.ivAddition.throttleClicks().subscribeAndObserveOnMainThread {
            productQuantity++
            binding.orderPrizeTextView.text = listOfProduct?.productBasePrice?.div(100)?.times(productQuantity).toDollar()
            binding.productQuantityAppCompatTextView.text = productQuantity.toString()
        }.autoDispose()
        binding.ivSubtraction.throttleClicks().subscribeAndObserveOnMainThread {
            if (productQuantity != 1) {
                productQuantity--
                binding.orderPrizeTextView.text = listOfProduct?.productBasePrice?.div(100)?.times(productQuantity).toDollar()
                binding.productQuantityAppCompatTextView.text = productQuantity.toString()
            }
        }.autoDispose()
        binding.addToCartMaterialButton.throttleClicks().subscribeAndObserveOnMainThread {
            if (loggedInUserCache.getIsEmployeeMeal() == true) isCompProduct = true
            addToCartProcess()
        }.autoDispose()
        binding.tvCustomize.throttleClicks().subscribeAndObserveOnMainThread {
            startActivity(CustomizeOrderActivity.getIntent(requireContext(), productsItem, menuId))
            dismiss()
        }.autoDispose()

    }

    private fun addToCartProcess() {
        val now = Calendar.getInstance()
        now.add(Calendar.MINUTE, 30)
        val df = SimpleDateFormat("yyy-MM-dd HH:mm:ss")
        val promisedTime = df.format(now.time)
        if (productsItem?.modification.isNullOrEmpty()) {
            if (loggedInUserCache.getLoggedInUserCartGroupId() == 0) {
                userStoreViewModel.addToCartProduct(
                    AddToCartRequest(
                        locationId = loggedInUserCache.getLocationInfo()?.location?.id,
                        orderTypeId = loggedInUserCache.getorderTypeId(),
                        modeId = MODE_ID,
                        promisedTime = promisedTime,
                        menuId = menuId,
                        menuItemQuantity = productQuantity,
                        menuItemInstructions = binding.specialInstructionsEditText.text.toString().ifEmpty { null },
                        userId = if (loggedInUserCache.getLoyaltyQrResponse()?.id != "" && loggedInUserCache.getLoyaltyQrResponse()?.id != null) loggedInUserCache.getLoyaltyQrResponse()?.id else null,
                        initiatedId = if (loggedInUserCache.getLoggedInUserId() != "" && loggedInUserCache.getLoggedInUserId() != null) loggedInUserCache.getLoggedInUserId() else null,
//                        menuItemRedemption = isRedeemProduct != 0,
//                        menuItemComp = loggedInUserCache.getIsEmployeeMeal() == true
                    )
                )
            } else {
                userStoreViewModel.addToCartProduct(
                    AddToCartRequest(
                        locationId = loggedInUserCache.getLocationInfo()?.location?.id,
                        orderTypeId = loggedInUserCache.getorderTypeId(),
                        modeId = MODE_ID,
                        promisedTime = promisedTime,
                        menuId = menuId,
                        menuItemQuantity = productQuantity,
                        cartGroupId = loggedInUserCache.getLoggedInUserCartGroupId(),
                        menuItemInstructions = binding.specialInstructionsEditText.text.toString().ifEmpty { null },
                        userId = if (loggedInUserCache.getLoyaltyQrResponse()?.id != "" && loggedInUserCache.getLoyaltyQrResponse()?.id != null) loggedInUserCache.getLoyaltyQrResponse()?.id else null,
                        initiatedId = if (loggedInUserCache.getLoggedInUserId() != "" && loggedInUserCache.getLoggedInUserId() != null) loggedInUserCache.getLoggedInUserId() else null,
//                        menuItemRedemption = isRedeemProduct != 0,
//                        menuItemComp = loggedInUserCache.getIsEmployeeMeal() == true
                    )
                )
            }
        } else {
            if (loggedInUserCache.getLoggedInUserCartGroupId() == 0) {
                userStoreViewModel.addToCartProduct(
                    AddToCartRequest(
                        locationId = loggedInUserCache.getLocationInfo()?.location?.id,
                        orderTypeId = loggedInUserCache.getorderTypeId(),
                        modeId = MODE_ID,
                        promisedTime = promisedTime,
                        menuId = menuId,
                        menuItemQuantity = productQuantity,
                        menuItemInstructions = binding.specialInstructionsEditText.text.toString().ifEmpty { null },
                        userId = if (loggedInUserCache.getLoyaltyQrResponse()?.id != "" && loggedInUserCache.getLoyaltyQrResponse()?.id != null) loggedInUserCache.getLoyaltyQrResponse()?.id else null,
                        initiatedId = if (loggedInUserCache.getLoggedInUserId() != "" && loggedInUserCache.getLoggedInUserId() != null) loggedInUserCache.getLoggedInUserId() else null,
//                        menuItemRedemption = isRedeemProduct != 0,
//                        menuItemComp = loggedInUserCache.getIsEmployeeMeal() == true
                    )
                )
            } else {
                userStoreViewModel.addToCartProduct(
                    AddToCartRequest(
                        locationId = loggedInUserCache.getLocationInfo()?.location?.id,
                        orderTypeId = loggedInUserCache.getorderTypeId(),
                        modeId = MODE_ID,
                        promisedTime = promisedTime,
                        menuId = menuId,
                        menuItemQuantity = productQuantity,
                        cartGroupId = loggedInUserCache.getLoggedInUserCartGroupId(),
                        userId = if (loggedInUserCache.getLoyaltyQrResponse()?.id != "" && loggedInUserCache.getLoyaltyQrResponse()?.id != null) loggedInUserCache.getLoyaltyQrResponse()?.id else null,
                        initiatedId = if (loggedInUserCache.getLoggedInUserId() != "" && loggedInUserCache.getLoggedInUserId() != null) loggedInUserCache.getLoggedInUserId() else null,
//                        menuItemRedemption = isRedeemProduct != 0,
//                        menuItemComp = loggedInUserCache.getIsEmployeeMeal() == true
                    )
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        UserInteractionInterceptor.wrapWindowCallback(requireActivity().window, activity)
        userStoreViewModel.getProductDetails(listOfProduct?.id)
    }
}