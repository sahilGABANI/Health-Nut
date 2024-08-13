package com.hotbox.terminal.ui.userstore.cookies

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.hotbox.terminal.R
import com.hotbox.terminal.api.authentication.LoggedInUserCache
import com.hotbox.terminal.api.menu.model.MenusItem
import com.hotbox.terminal.api.menu.model.ProductLoyaltyTier
import com.hotbox.terminal.api.menu.model.ProductsItem
import com.hotbox.terminal.api.userstore.model.AddToCartRequest
import com.hotbox.terminal.application.HotBoxApplication
import com.hotbox.terminal.base.BaseFragment
import com.hotbox.terminal.base.RxBus
import com.hotbox.terminal.base.RxEvent
import com.hotbox.terminal.base.ViewModelFactory
import com.hotbox.terminal.base.extension.getViewModelFromFactory
import com.hotbox.terminal.base.extension.showToast
import com.hotbox.terminal.base.extension.subscribeAndObserveOnMainThread
import com.hotbox.terminal.databinding.FragmentCookiesBinding
import com.hotbox.terminal.ui.userstore.AddToCartDialogFragment
import com.hotbox.terminal.ui.userstore.cookies.view.UserStoreProductAdapter
import com.hotbox.terminal.ui.userstore.customize.CustomizeOrderActivity
import com.hotbox.terminal.ui.userstore.viewmodel.UserStoreState
import com.hotbox.terminal.ui.userstore.viewmodel.UserStoreViewModel
import com.hotbox.terminal.utils.Constants
import com.hotbox.terminal.utils.Constants.CHIPS_AND_BOTTLED_DRINKS_MENU_ID
import com.hotbox.terminal.utils.Constants.DRESSINGS_MENU_ID
import com.hotbox.terminal.utils.UserInteractionInterceptor
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

class CookiesFragment : BaseFragment() {

    private var _binding: FragmentCookiesBinding? = null
    private val binding get() = _binding!!
    private var menuId: Int? = 0
    private var productLoyaltyTier: ProductLoyaltyTier? = null
    private var isClick = false

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<UserStoreViewModel>
    private lateinit var userStoreViewModel: UserStoreViewModel

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache

    companion object {
        @SuppressLint("StaticFieldLeak")
        private lateinit var userStoreProductAdapter: UserStoreProductAdapter

        var listOfProduct: MenusItem? = null
            set(value) {
                field = value
                updateItems()
            }

        private fun updateItems() {
            if (this::userStoreProductAdapter.isInitialized) {
                val list = listOfProduct?.products?.filter { it.productActive == true && it.menuActive == true }
                RxBus.publish(RxEvent.HideProgressBar)
                userStoreProductAdapter.listOfProductDetails = list
            }
        }



        @JvmStatic
        fun newInstance() = CookiesFragment()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        HotBoxApplication.component.inject(this)
        userStoreViewModel = getViewModelFromFactory(viewModelFactory)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCookiesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        listenToViewModel()
        listenToViewEvent()
    }

    private fun listenToViewEvent() {
        initAdapter()
//        if (!loggedInUserCache.isUserLoggedIn()) binding.llSearch.isVisible = false
       if (loggedInUserCache.getIsEmployeeMeal() == true) binding.llCookiesFragment.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.green_light_50));
       binding.llSearch.isVisible = false

        RxBus.listen(RxEvent.HideProgressBar::class.java).subscribeAndObserveOnMainThread {
            binding.progressBar.isVisible = false
        }.autoDispose()
    }

    private fun initAdapter() {
        userStoreProductAdapter = UserStoreProductAdapter(requireContext()).apply {
            userStoreProductActionState.subscribeAndObserveOnMainThread {
                if (listOfProduct?.id == CHIPS_AND_BOTTLED_DRINKS_MENU_ID || listOfProduct?.id == DRESSINGS_MENU_ID) {
                    addToCartProcess(it)
                } else {
                    if (isClick) {
                        return@subscribeAndObserveOnMainThread
                    } else{
                        isClick = true
                        userStoreViewModel.getProductDetails(it.id)
                        menuId = it.menuId
                        productLoyaltyTier = it.productLoyaltyTier
                    }
                }


            }.autoDispose()
        }
        binding.productDetailsRecycleView.apply {
            adapter = userStoreProductAdapter
        }
        updateItems()
    }
    private fun addToCartProcess(productsItem: ProductsItem) {
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
                        menuId = productsItem.menuId,
                        menuItemQuantity = 1,
                        menuItemInstructions = null,
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
                        modeId = Constants.MODE_ID,
                        promisedTime = promisedTime,
                        menuId = productsItem.menuId,
                        menuItemQuantity = 1,
                        cartGroupId = loggedInUserCache.getLoggedInUserCartGroupId(),
                        menuItemInstructions = null,
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
                        modeId = Constants.MODE_ID,
                        promisedTime = promisedTime,
                        menuId = productsItem.menuId,
                        menuItemQuantity = 1,
                        menuItemInstructions =  null ,
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
                        modeId = Constants.MODE_ID,
                        promisedTime = promisedTime,
                        menuId = productsItem.menuId,
                        menuItemQuantity = 1,
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

    private fun listenToViewModel() {
        userStoreViewModel.userStoreState.subscribeAndObserveOnMainThread {
            when (it) {
                is UserStoreState.ErrorMessage -> {
                    showToast(it.errorMessage)
                }
                is UserStoreState.SuccessMessage -> {

                }
                is UserStoreState.AddToCartProductResponse -> {
                    var cartGroupId = 0
                    it.addToCartResponse.cartGroup?.id?.let { cartGroupId = it }
                    loggedInUserCache.setLoggedInUserCartGroupId(cartGroupId)
                    RxBus.publish(RxEvent.EventCartGroupIdListen(cartGroupId))
                }
                is UserStoreState.LoadingState -> {
                    binding.progressBar.isVisible = it.isLoading
                    isClick = it.isLoading
                    if (it.isLoading){
                        requireActivity().window.setFlags(
                            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
                    } else {
                        requireActivity().window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
                    }
                }
                is UserStoreState.SubProductState -> {
                    isClick = false
                    requireActivity().window.clearFlags(
                        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
                    if (it.productsItem.modification.isNullOrEmpty()) {
                        it.productsItem.productLoyaltyTier = productLoyaltyTier
                        val addToCartDialogFragment = AddToCartDialogFragment()
                        AddToCartDialogFragment.listOfProduct = it.productsItem
                        AddToCartDialogFragment.menuId = menuId
                        AddToCartDialogFragment.isRedeemProduct = 0
                        addToCartDialogFragment.show(parentFragmentManager, "")
                    } else {
                        it.productsItem.productLoyaltyTier = productLoyaltyTier
                        startActivity(CustomizeOrderActivity.getIntent(requireContext(), it.productsItem, menuId))
                    }
                }
                else -> {

                }
            }
        }.autoDispose()
    }

    override fun onResume() {
        super.onResume()
        requireActivity().window.clearFlags(
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
        val list = listOfProduct?.products?.filter { it.menuActive == true && it.productActive == true }
        list?.forEach {
            it.isRedeemProduct = false
        }
        userStoreProductAdapter.listOfProductDetails = list
    }


    override fun onStart() {
        super.onStart()
        binding.progressBar.isVisible = true
        userStoreProductAdapter.listOfProductDetails = arrayListOf()
        UserInteractionInterceptor.wrapWindowCallback(requireActivity().window, activity)
    }

}