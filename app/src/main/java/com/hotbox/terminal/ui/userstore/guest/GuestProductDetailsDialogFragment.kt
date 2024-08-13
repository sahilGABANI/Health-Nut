package com.hotbox.terminal.ui.userstore.guest

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Toast
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.hotbox.terminal.R
import com.hotbox.terminal.api.authentication.LoggedInUserCache
import com.hotbox.terminal.api.menu.model.ProductsItem
import com.hotbox.terminal.api.userstore.model.AddToCartRequest
import com.hotbox.terminal.application.HotBoxApplication
import com.hotbox.terminal.base.BaseDialogFragment
import com.hotbox.terminal.base.RxBus
import com.hotbox.terminal.base.RxEvent
import com.hotbox.terminal.base.ViewModelFactory
import com.hotbox.terminal.base.extension.*
import com.hotbox.terminal.databinding.FragmentGuestProductDetailsDialogBinding
import com.hotbox.terminal.ui.userstore.viewmodel.UserStoreState
import com.hotbox.terminal.ui.userstore.viewmodel.UserStoreViewModel
import com.hotbox.terminal.utils.Constants
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

class GuestProductDetailsDialogFragment : BaseDialogFragment() {

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

    private var _binding: FragmentGuestProductDetailsDialogBinding? = null
    private val binding get() = _binding!!

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<UserStoreViewModel>
    private lateinit var userStoreViewModel: UserStoreViewModel
    private var productsItem: ProductsItem? = null
    private var productQuantity = 1

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        HotBoxApplication.component.inject(this)
        userStoreViewModel = getViewModelFromFactory(viewModelFactory)
        setStyle(STYLE_NORMAL, R.style.MyDialog)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGuestProductDetailsDialogBinding.inflate(inflater, container, false)
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
        listenToViewEvent()
        listenToViewModel()
    }

    private fun listenToViewModel() {
        userStoreViewModel.userStoreState.subscribeAndObserveOnMainThread {
            when (it) {
                is UserStoreState.ErrorMessage -> {
                    Toast.makeText(requireContext(), it.errorMessage, Toast.LENGTH_SHORT).show()
                }
                is UserStoreState.SuccessMessage -> {
                    showToast(it.successMessage)
                }
                is UserStoreState.SubProductState -> {
                    productsItem = it.productsItem
                    initUI(it.productsItem)
                }
                is UserStoreState.AddToCartProductResponse -> {
                    var cartGroupId = 0
                    it.addToCartResponse.cartGroup?.id?.let { cartGroupId = it }
                    loggedInUserCache.setLoggedInUserCartGroupId(cartGroupId)
                    RxBus.publish(RxEvent.EventCartGroupIdListen(cartGroupId))
                    this.dismiss()
                }
                else -> {

                }
            }
        }.autoDispose()
    }


    private fun listenToViewEvent() {
        binding.closeButtonMaterialCardView.throttleClicks().subscribeAndObserveOnMainThread {
            dismiss()
        }.autoDispose()
        binding.additionMaterialCardView.throttleClicks().subscribeAndObserveOnMainThread {
            productQuantity++
            binding.orderPrizeTextView.text = ((listOfProduct?.productBasePrice)?.div(100)?.times(productQuantity)).toDollar()
            binding.productQuantityAppCompatTextView.text = productQuantity.toString()
        }.autoDispose()
        binding.subtractionMaterialCardView.throttleClicks().subscribeAndObserveOnMainThread {
            if (productQuantity != 1) {
                productQuantity--
                binding.orderPrizeTextView.text = ((listOfProduct?.productBasePrice)?.div(100)?.times(productQuantity)).toDollar()
                binding.productQuantityAppCompatTextView.text = productQuantity.toString()
            }
        }.autoDispose()

        binding.addToCartMaterialButton.throttleClicks().subscribeAndObserveOnMainThread {
            addToCartProcess()
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
                        modeId = Constants.MODE_ID,
                        promisedTime = promisedTime,
                        menuId = menuId,
                        menuItemQuantity = productQuantity,
                        menuItemInstructions = binding.specialInstructionsEditText.text.toString().ifEmpty { null },
//                        menuItemRedemption = isRedeemProduct != 0,
//                        menuItemComp = loggedInUserCache.getIsEmployeeMeal() == true
                    )
                )
            } else {
                userStoreViewModel.addToCartProduct(
                    AddToCartRequest(
                        locationId = loggedInUserCache.getLocationInfo()?.location?.id,
                        orderTypeId = loggedInUserCache.getorderTypeId(),
                        modeId = Constants.MODE_ID,
                        promisedTime = promisedTime,
                        menuId = menuId,
                        menuItemQuantity = productQuantity,
                        cartGroupId = loggedInUserCache.getLoggedInUserCartGroupId(),
                        menuItemInstructions = binding.specialInstructionsEditText.text.toString().ifEmpty { null },
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
                        modeId = Constants.MODE_ID,
                        promisedTime = promisedTime,
                        menuId = menuId,
                        menuItemQuantity = productQuantity,
                        menuItemInstructions = binding.specialInstructionsEditText.text.toString().ifEmpty { null },
//                        menuItemRedemption = isRedeemProduct != 0,
//                        menuItemComp = loggedInUserCache.getIsEmployeeMeal() == true
                    )
                )
            } else {
                userStoreViewModel.addToCartProduct(
                    AddToCartRequest(
                        locationId = loggedInUserCache.getLocationInfo()?.location?.id,
                        orderTypeId = loggedInUserCache.getorderTypeId(),
                        modeId = Constants.MODE_ID,
                        promisedTime = promisedTime,
                        menuId = menuId,
                        menuItemQuantity = productQuantity,
                        cartGroupId = loggedInUserCache.getLoggedInUserCartGroupId(),
                        menuItemInstructions = binding.specialInstructionsEditText.text.toString().ifEmpty { null },
//                        menuItemRedemption = isRedeemProduct != 0,
//                        menuItemComp = loggedInUserCache.getIsEmployeeMeal() == true
                    )
                )
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun initUI(productsItem: ProductsItem) {
        binding.productNameTextView.text = productsItem.productName
        binding.orderPrizeTextView.text = productsItem.productBasePrice?.div(100).toDollar()
        binding.productCalTextView.text = "(${productsItem.productCals.toString().plus(" cal")})"
        binding.productDescriptionTextView.text = productsItem.productDescription
        Glide.with(requireContext()).load(productsItem.productImage).placeholder(R.drawable.ic_launcher_logo).error(R.drawable.ic_launcher_logo)
            .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC).into(binding.productImageView)

    }



    override fun onResume() {
        super.onResume()
        userStoreViewModel.getProductDetails(listOfProduct?.id)
    }

}