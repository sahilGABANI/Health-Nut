package com.hotbox.terminal.ui.userstore.guest

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.hotbox.terminal.R
import com.hotbox.terminal.api.authentication.LoggedInUserCache
import com.hotbox.terminal.api.menu.model.ProductsItem
import com.hotbox.terminal.api.userstore.model.*
import com.hotbox.terminal.application.HotBoxApplication
import com.hotbox.terminal.base.BaseDialogFragment
import com.hotbox.terminal.base.RxBus
import com.hotbox.terminal.base.RxEvent
import com.hotbox.terminal.base.ViewModelFactory
import com.hotbox.terminal.base.extension.*
import com.hotbox.terminal.databinding.FragmentTakeNBackDialogBinding
import com.hotbox.terminal.ui.userstore.guest.view.OrderSubItemAdapter
import com.hotbox.terminal.ui.userstore.viewmodel.UserStoreState
import com.hotbox.terminal.ui.userstore.viewmodel.UserStoreViewModel
import com.hotbox.terminal.utils.Constants
import com.hotbox.terminal.utils.UserInteractionInterceptor
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

class TakeNBackDialogFragment : BaseDialogFragment() {

    companion object {
        @SuppressLint("StaticFieldLeak")
        lateinit var orderSubItemAdapter: OrderSubItemAdapter
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

    private var productPrice: Double = 0.00

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<UserStoreViewModel>
    private lateinit var userStoreViewModel: UserStoreViewModel

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache

    private var _binding: FragmentTakeNBackDialogBinding? = null
    private val binding get() = _binding!!
    private val subOrderItem = ArrayList<SubOrderItemData>()
    private var productQuantity = 1
    private var productsItem: ProductsItem? = null
    private var menuItemInstructions: String? = null
    private var isbrack = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        HotBoxApplication.component.inject(this)
        userStoreViewModel = getViewModelFromFactory(viewModelFactory)
        setStyle(STYLE_NORMAL, R.style.MyDialog)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTakeNBackDialogBinding.inflate(inflater, container, false)
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
        listenToViewModel()
        listenToViewEvent()
    }

    private fun listenToViewModel() {
        userStoreViewModel.userStoreState.subscribeAndObserveOnMainThread {
            when (it) {
                is UserStoreState.LoadingState -> {
                }
                is UserStoreState.AddToCartProductResponse -> {
                    var cartGroupId = 0
                    it.addToCartResponse.cartGroup?.id?.let { cartGroupId = it }
                    loggedInUserCache.setLoggedInUserCartGroupId(cartGroupId)
                    RxBus.publish(RxEvent.EventCartGroupIdListen(cartGroupId))
                    this.dismiss()

                }
                is UserStoreState.ErrorMessage -> {
                    showToast(it.errorMessage)
                }
                is UserStoreState.SubProductState -> {
                    productsItem = it.productsItem
//                    binding.addToCartMaterialButton.isEnabled = true
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

    @SuppressLint("SetTextI18n", "ResourceType")
    private fun initUI(productsItem: ProductsItem) {
        binding.productNameTextView.text = productsItem.productName
        binding.orderPrizeTextView.text = productsItem.productBasePrice?.div(100).toDollar()
        binding.productCalTextView.text = "(${productsItem.productCals.toString().plus(" cal")})"

        productsItem.productCals?.let {
            binding.productCalTextView.text = it.toString().plus(" cal")
        }
        if (productsItem.productCals == null) {
            binding.productCalTextView.text = "(${resources.getText(R.string._0_cal)})"
        }
        productsItem.productDescription?.let {
            binding.productDescriptionTextView.text = it
        }
        Glide.with(requireContext()).load(productsItem.productImage).placeholder(R.drawable.ic_launcher_logo).error(R.drawable.ic_launcher_logo)
            .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC).into(binding.productImageView)
        initAdapter()
    }


    private fun productPriceCount() {
        val list = orderSubItemAdapter.listOfOrderSubItem
        productPrice = productsItem?.productBasePrice!!
        list?.forEach {
            it.subProductList?.forEach {
                if (it.isCheck == true) {
                    productPrice = it.optionPrice?.let { it1 -> productPrice.plus(it1) }!!
                }
            }
        }
        binding.orderPrizeTextView.text = productPrice.div(100).times(productQuantity).toDollar()
    }

    @SuppressLint("SimpleDateFormat")
    private fun listenToViewEvent() {
        binding.closeButtonMaterialCardView.throttleClicks().subscribeAndObserveOnMainThread {
            dismiss()
        }.autoDispose()
        binding.additionMaterialCardView.throttleClicks().subscribeAndObserveOnMainThread {
            productQuantity++
            productPriceCount()
            binding.orderPrizeTextView.text = (productPrice.div(100).times(productQuantity)).toDollar()
            binding.productQuantityAppCompatTextView.text = productQuantity.toString()
        }.autoDispose()
        binding.subtractionMaterialCardView.throttleClicks().subscribeAndObserveOnMainThread {
            if (productQuantity != 1) {
                productQuantity--
                productPriceCount()
                binding.orderPrizeTextView.text = (productPrice.div(100).times(productQuantity)).toDollar()
                binding.productQuantityAppCompatTextView.text = productQuantity.toString()
            }
        }.autoDispose()
        binding.addToCartMaterialButton.throttleClicks().subscribeAndObserveOnMainThread {
            requireActivity().hideKeyboard()
            addToCartProcess()
        }.autoDispose()
    }

    private fun initAdapter() {
        orderSubItemAdapter = OrderSubItemAdapter(requireContext()).apply {
            subProductActionState.subscribeAndObserveOnMainThread {
                productPriceCount()
                addSelection()

            }.autoDispose()
        }
        binding.rvCookies.apply {
            adapter = orderSubItemAdapter
        }
        orderSubItemAdapter.listOfOrderSubItem = getOrderSubItem()
        addSelection()
    }

    private fun addSelection() {
        val listofModification = ArrayList<MenuItemModifiersItemRequest>()


        val list = orderSubItemAdapter.listOfOrderSubItem
        list?.forEachIndexed { index, it ->
            val listOfOption = arrayListOf<OptionsItemRequest>()
            listOfOption.clear()
            it.optionsItem.forEachIndexed { index, item ->
                if (item.isCheck == true) {
                    listOfOption.add(
                        OptionsItemRequest(
                            id = item.id,
                            optionPrice = item.optionPrice?.toInt(),
                            optionName = item.optionName,
                            optionImage = item.optionImage,
                            active = item.active
                        )
                    )
                }
            }
            if (listOfOption.isNotEmpty()) {
                listofModification.add(
                    MenuItemModifiersItemRequest(
                        selectMax = it.modifiers?.selectMax,
                        isRequired = if (it.modifiers?.isRequired == true) 1 else 0,
                        active = it.modifiers?.active,
                        selectMin = it.modifiers?.selectMin,
                        id = it.modifiers?.id,
                        modificationText = it.modifiers?.modificationText,
                        options = listOfOption,
                        type = it.modifiers?.type
                    )
                )
            }
        }
        if (listofModification.isNotEmpty()) {
            val selectedOptionName = StringBuilder().apply {
                listofModification.forEach {
                    if (listofModification.last() == it) {
                        append("${it.modificationText}: ")
                        it.options?.forEach { item ->
                            if (it.options.last() == item) {
                                append("${item.optionName}")
                            } else {
                                append("${item.optionName}, ")
                            }
                        }
                    } else {
                        append("${it.modificationText}: ")
                        it.options?.forEach { item ->
                            if (it.options.last() == item) {
                                append("${item.optionName}")
                            } else {
                                append("${item.optionName}, ")
                            }
                        }
                        append(" | ")
                    }
                }
            }
            binding.selectedOptionTextView.removeAllViews()
            listofModification.forEach { item ->
                item.modificationText?.let {
                    binding.selectedOptionTextView.isVisible = true
                    val v: View = View.inflate(context, R.layout.selected_option_view, null)
                    if (item == listofModification.lastOrNull()) {
                        v.findViewById<AppCompatTextView>(R.id.productTextview).text = "$it:"
                        v.findViewById<AppCompatTextView>(R.id.productTextDescription).text = item.getSafeSelectedItemName()
                        v.findViewById<AppCompatTextView>(R.id.tvComma).isVisible = false
                        binding.selectedOptionTextView.addView(v)
                    } else {
                        v.findViewById<AppCompatTextView>(R.id.productTextview).text = "$it:"
                        v.findViewById<AppCompatTextView>(R.id.productTextDescription).text = item.getSafeSelectedItemName()
                        binding.selectedOptionTextView.addView(v)
                    }
                }
            }
        }
    }

    private fun getOrderSubItem(): List<SubOrderItemData> {
        for (i in productsItem?.modification?.indices!!) {
            if (i == productsItem!!.modification!!.lastIndex) {
                subOrderItem.add(
                    SubOrderItemData(
                        resources.getString(R.string.cookies),
                        productsItem?.modification?.get(i)?.options,
                        modifiers = productsItem?.modification!!.get(i),
                        isLastItem = true
                    )
                )
            } else {
                subOrderItem.add(
                    SubOrderItemData(
                        resources.getString(R.string.cookies),
                        productsItem?.modification?.get(i)?.options,
                        modifiers = productsItem?.modification!!.get(i)
                    )
                )
            }

        }
//        if(subOrderItem.isNotEmpty()) binding.view.isVisible = true
        return subOrderItem.toList()
    }

    private fun addToCartProcess() {
        isbrack = false
        val now = Calendar.getInstance()
        now.add(Calendar.MINUTE, 30)
        val df = SimpleDateFormat("yyy-MM-dd HH:mm:ss")
        val promisedTime = df.format(now.time)
        var addToCart: AddToCartRequest? = null
        menuItemInstructions = if (binding.specialInstructionsEditText.text.toString() == "") {
            null
        } else {
            binding.specialInstructionsEditText.text.toString()
        }
        if (productsItem?.modification.isNullOrEmpty()) {
            if (loggedInUserCache.getLoggedInUserCartGroupId() == 0) {
                addToCart = AddToCartRequest(
                    locationId = loggedInUserCache.getLocationInfo()?.location?.id,
                    orderTypeId = loggedInUserCache.getorderTypeId(),
                    modeId = Constants.MODE_ID,
                    promisedTime = promisedTime,
                    menuId = menuId,
                    menuItemQuantity = productQuantity,
                    menuItemModifiers = null,
                    menuItemInstructions = menuItemInstructions,
//                    menuItemRedemption = isRedeemProduct != 0,
//                    menuItemComp = loggedInUserCache.getIsEmployeeMeal() == true
                )
            } else {
                addToCart = AddToCartRequest(
                    locationId = loggedInUserCache.getLocationInfo()?.location?.id,
                    orderTypeId = loggedInUserCache.getorderTypeId(),
                    modeId = Constants.MODE_ID,
                    promisedTime = promisedTime,
                    menuId = menuId,
                    menuItemQuantity = productQuantity,
                    menuItemModifiers = null,
                    cartGroupId = loggedInUserCache.getLoggedInUserCartGroupId(),
                    menuItemInstructions = menuItemInstructions,
//                    menuItemRedemption = isRedeemProduct != 0,
//                    menuItemComp = loggedInUserCache.getIsEmployeeMeal() == true

                )
            }
            userStoreViewModel.addToCartProduct(addToCart)
        } else {
            val list = orderSubItemAdapter.listOfOrderSubItem
            val listofModification = ArrayList<MenuItemModifiersItemRequest>()
            list?.forEachIndexed { index, it ->
                val listOfOption = arrayListOf<OptionsItemRequest>()
                listOfOption.clear()
                it.optionsItem.forEachIndexed { index, item ->
                    if (item.isCheck == true) {
                        listOfOption.add(
                            OptionsItemRequest(
                                id = item.id,
                                optionPrice = item.optionPrice?.toInt(),
                                optionName = item.optionName,
                                optionImage = item.optionImage,
                                active = item.active
                            )
                        )
                    }
                }
                if (listOfOption.isNotEmpty()) {
                    listofModification.add(
                        MenuItemModifiersItemRequest(
                            selectMax = it.modifiers?.selectMax,
                            isRequired = if (it.modifiers?.isRequired == true) 1 else 0,
                            active = it.modifiers?.active,
                            selectMin = it.modifiers?.selectMin,
                            id = it.modifiers?.id,
                            modificationText = it.modifiers?.modificationText,
                            options = listOfOption,
                            type = it.modifiers?.type
                        )
                    )
                } else {
                    if (it.modifiers?.selectMin != 0 && it.modifiers?.isRequired == true) {
                        isbrack = true
                        showToast("you can select minimum ${it.modifiers.selectMin} option in ${it.modifiers.modificationText}")
                    }
                }
            }
            if (!isbrack) {
                if (loggedInUserCache.getLoggedInUserCartGroupId() == 0) {
                    addToCart = AddToCartRequest(
                        locationId = loggedInUserCache.getLocationInfo()?.location?.id,
                        orderTypeId = loggedInUserCache.getorderTypeId(),
                        modeId = Constants.MODE_ID,
                        promisedTime = promisedTime,
                        menuId = menuId,
                        menuItemQuantity = productQuantity,
                        menuItemModifiers = listofModification,
                        menuItemInstructions = menuItemInstructions,
//                        menuItemRedemption = isRedeemProduct != 0,
//                        menuItemComp = loggedInUserCache.getIsEmployeeMeal() == true
                    )
                } else {
                    addToCart = AddToCartRequest(
                        locationId = loggedInUserCache.getLocationInfo()?.location?.id,
                        orderTypeId = loggedInUserCache.getorderTypeId(),
                        modeId = Constants.MODE_ID,
                        promisedTime = promisedTime,
                        menuId = menuId,
                        menuItemQuantity = productQuantity,
                        menuItemModifiers = listofModification,
                        cartGroupId = loggedInUserCache.getLoggedInUserCartGroupId(),
                        menuItemInstructions = menuItemInstructions,
//                        menuItemRedemption = isRedeemProduct != 0,
//                        menuItemComp = loggedInUserCache.getIsEmployeeMeal() == true
                    )
                }
                if (!addToCart.menuItemModifiers.isNullOrEmpty()) {
                    userStoreViewModel.addToCartProduct(addToCart)
                }
            }
        }


    }

    private fun isValidate(): Boolean {
        return when {
            binding.specialInstructionsEditText.isFieldBlank() -> {
                showToast(getText(R.string.blank_special).toString())
                false
            }

            else -> true
        }
    }

    override fun onResume() {
        super.onResume()
        userStoreViewModel.getProductDetails(listOfProduct?.id)
    }

    override fun onStart() {
        super.onStart()
        UserInteractionInterceptor.wrapWindowCallback(dialog?.window, activity)
    }
}