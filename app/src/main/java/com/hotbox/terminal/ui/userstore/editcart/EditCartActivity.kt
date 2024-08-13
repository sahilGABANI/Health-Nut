package com.hotbox.terminal.ui.userstore.editcart

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.text.Editable
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.gson.Gson
import com.hotbox.terminal.R
import com.hotbox.terminal.api.authentication.LoggedInUserCache
import com.hotbox.terminal.api.menu.model.ModificationItem
import com.hotbox.terminal.api.menu.model.ProductsItem
import com.hotbox.terminal.api.store.model.StoreResponse
import com.hotbox.terminal.api.userstore.model.*
import com.hotbox.terminal.application.HotBoxApplication
import com.hotbox.terminal.base.BaseActivity
import com.hotbox.terminal.base.RxBus
import com.hotbox.terminal.base.RxEvent
import com.hotbox.terminal.base.ViewModelFactory
import com.hotbox.terminal.base.extension.*
import com.hotbox.terminal.databinding.ActivityEditCartBinding
import com.hotbox.terminal.helper.formatTo
import com.hotbox.terminal.helper.formatToStoreTime
import com.hotbox.terminal.helper.getCurrentsStoreTime
import com.hotbox.terminal.helper.toDate
import com.hotbox.terminal.ui.login.LoginActivity
import com.hotbox.terminal.ui.userstore.UserStoreActivity
import com.hotbox.terminal.ui.userstore.customize.view.ModificationAdapter
import com.hotbox.terminal.ui.userstore.customize.view.ModifierOptionAdapter
import com.hotbox.terminal.ui.userstore.customize.view.OrderOptionAdapter
import com.hotbox.terminal.ui.userstore.viewmodel.UserStoreState
import com.hotbox.terminal.ui.userstore.viewmodel.UserStoreViewModel
import com.hotbox.terminal.utils.Constants
import com.hotbox.terminal.utils.FlowLayoutManager
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import kotlin.collections.ArrayList

class EditCartActivity : BaseActivity() {

    companion object {
        const val CART_ITEM = "CART_ITEM"
        fun getIntent(context: Context, productsItem: CartItem?): Intent {
            val intent = Intent(context, EditCartActivity::class.java)
            val gson = Gson()
            val json: String = gson.toJson(productsItem)
            intent.putExtra(CART_ITEM, json)
            return intent
        }
    }

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<UserStoreViewModel>
    private lateinit var userStoreViewModel: UserStoreViewModel

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache

    private lateinit var binding: ActivityEditCartBinding
    private lateinit var orderOptionAdapter: OrderOptionAdapter
    private lateinit var modificationAdapter: ModificationAdapter
    private lateinit var modifierOptionAdapter: ModifierOptionAdapter
    private var cartItem: CartItem? = null
    private var singleSelection: Boolean? = false
    private var menuId = 1
    private var selectMax = 1
    private var selectedModificationItem: ModificationItem? = null
    private var addToCart: AddToCartRequest? = null
    private var productQuantity = 1
    private var productPrice = 0.00
    private var isbrack = false
    private var productsItem: ProductsItem? = null
    private var menuItemInstructions: String? = null
    private var isCompProduct: Boolean? = false
    private var isRedeemProduct: Boolean? = false
    var DISCONNECT_TIMEOUT: Long = 14400000 // 240 min = 240 * 60 * 1000 ms
    var SHOW_TOAST_TIMER: Long = 12600000   // 210 min = 5 * 60 * 1000 ms

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditCartBinding.inflate(layoutInflater)
        HotBoxApplication.component.inject(this)
        userStoreViewModel = getViewModelFromFactory(viewModelFactory)
        setContentView(binding.root)
        window.decorView.apply {
            systemUiVisibility = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_FULLSCREEN
        }
        intent.let {
            val productsDetails = it.getStringExtra(CART_ITEM)
            val gson = Gson()
            cartItem = gson.fromJson(productsDetails, CartItem::class.java)
            isCompProduct = cartItem?.menuItemComp == 1
        }
        listenToViewModel()
        userStoreViewModel.getProductDetails(cartItem?.menu?.product?.id)
        userStoreViewModel.loadCurrentStoreResponse()
    }

    private fun listenToViewModel() {
        userStoreViewModel.userStoreState.subscribeAndObserveOnMainThread {
            when (it) {
                is UserStoreState.ErrorMessage -> {
                    showToast(it.errorMessage)
                }
                is UserStoreState.SuccessMessage -> {
                    showToast(it.successMessage)
                }
                is UserStoreState.LoadingState -> {
                    buttonVisibility(it.isLoading)
                }
                is UserStoreState.StoreResponses -> {
                    setStoreOpenAndClose(it.storeResponse)
                }
                is UserStoreState.AddToCartProductResponse -> {
                    var cartGroupId = 0
                    it.addToCartResponse.cartGroup?.id?.let { cartGroupId = it }
                    if (isCompProduct == true) {
                        loggedInUserCache.setLoggedInUserCartGroupId(cartGroupId)
//                        RxBus.publish(RxEvent.EventCartGroupIdListen(cartGroupId))
                        startActivityWithDefaultAnimation(UserStoreActivity.getIntent(this, false))
                    } else {
                        loggedInUserCache.setLoggedInUserCartGroupId(cartGroupId)
                        RxBus.publish(RxEvent.EventCartGroupIdListen(cartGroupId))
                        startActivityWithDefaultAnimation(UserStoreActivity.getIntent(this, false))
                    }

                }
                is UserStoreState.SubProductState -> {
                    productsItem = it.productsItem
                    initUI()
                    val modificationList = it.productsItem.modification
                    modificationList?.get(0)?.isSelected = true
                    modificationList?.last()?.isLast = true

                    val list = ArrayList<ModificationItem>()
                    cartItem?.menuItemModifiers?.forEach {
                        val selectedOptionList = kotlin.collections.ArrayList<OptionsItemRequest>()
                        selectedOptionList.clear()
                        it.options?.forEach {
                            val optionsItemRequest = OptionsItemRequest(
                                id = it.id, optionImage = it.optionImage, optionName = it.optionName, optionPrice = it.optionPrice?.toInt()

                            )
                            selectedOptionList.add(optionsItemRequest)
                        }
                        modificationList?.filter { item -> it.id == item.id }?.forEach {
                            it.selectedOptionsItem = selectedOptionList
                        }
                        val modificationItem = ModificationItem(
                            id = it.id,
                            modificationText = it.modificationText,
                            active = it.active,
                            selectMax = it.selectMax,
                            selectMin = it.selectMin,
                            isRequired = it.isRequired == 1,
                            selectedOptionsItem = selectedOptionList,
                            type = it.type,
                            options = it.options
                        )
                        list.add(modificationItem)
                    }
                    list.firstOrNull()?.isSelected = true
                    modifierOptionAdapter.listOfModification = list
                    cartItem?.menuItemModifiers?.forEach { menuItemModifiersCart ->
                        menuItemModifiersCart.options?.forEach { optionItem ->
                            modificationList?.forEach { modificationItem ->
                                modificationItem.options?.forEach {
                                    if (optionItem.id == it.id) {
                                        it.isCheck = true
                                    }
                                }
                            }
                        }
                    }

                    orderOptionAdapter.listOfOrderSubItem = modificationList?.get(0)?.options?.filter { it.active == true } ?: arrayListOf()
                    modificationAdapter.listOfModification = modificationList?.filter { it.active == true }
                    modificationSelection(modificationList?.get(0))
                    productPriceCount()
                }
                is UserStoreState.UpdatedCartInfo -> {
                    if (isRedeemProduct == true && it.cartInfo?.menuItemRedemption == false) {
                        if ((it.cartInfo.menuItemQuantity ?: 0) > 1) {
                            it.cartInfo.id.let { it1 ->
                                userStoreViewModel.redeemCartItem(
                                    UpdateMenuItemQuantity(
                                        cartId = it1
                                    ),cartItem?.menu?.product?.productLoyaltyTier?.tierValue ?: 0
                                )
                            }
                        } else {
                            it.cartInfo.id.let { it1 ->
                                userStoreViewModel.updateMenuItemQuantity(
                                    UpdateMenuItemQuantity(
                                        cartId = it1, menuItemQuantity = 1, menuItemRedemption = true
                                    ), cartItem?.menu?.product?.productLoyaltyTier?.tierValue ?: 0
                                )
                            }
                        }
                    } else {
                        finish()
                    }

                }
                is UserStoreState.RedeemProduct -> {
                    loggedInUserCache.setLoggedInUserCartGroupId(loggedInUserCache.getLoggedInUserCartGroupId() ?: 0)
                    RxBus.publish(RxEvent.EventCartGroupIdListen(loggedInUserCache.getLoggedInUserCartGroupId() ?: 0))
                    finish()
                }
                else -> {

                }
            }
        }.autoDispose()
    }

    private fun initUI() {
        productQuantity = cartItem?.menuItemQuantity!!
        binding.cartView.productQuantityAppCompatTextView.text = productQuantity.toString()

        cartItem!!.menuItemInstructions?.let {
            binding.specialInstructionsEditText.text = Editable.Factory.getInstance().newEditable(it)
        }
        initAdapter()
        binding.backLinearLayout.throttleClicks().subscribeAndObserveOnMainThread {
            onBackPressed()
        }.autoDispose()
        binding.rlEmployeeDetails.visibility = View.VISIBLE
        binding.rlGuest.visibility = View.GONE
        if (!loggedInUserCache.isUserLoggedIn()) {
            binding.rlEmployeeDetails.visibility = View.GONE
            binding.rlGuest.visibility = View.VISIBLE
        } else {
            binding.rlEmployeeDetails.visibility = View.VISIBLE
            binding.rlGuest.visibility = View.GONE
            binding.employeeNameAppCompatTextView.text = loggedInUserCache.getLoggedInUserFullName()
            binding.loggedInUserRoleTextView.text = loggedInUserCache.getLoggedInUserRole()
        }
        if (cartItem?.menu?.product?.productLoyaltyTier?.tierValue != 0 && cartItem?.menu?.product?.productLoyaltyTier?.tierValue != null) {
            binding.cartView.tvProductPoint.text =  cartItem?.menu?.product?.productLoyaltyTier?.tierValue.toString().plus(" ").plus(getString(R.string.leaves))
        } else {
            binding.cartView.tvProductPoint.isVisible = false
        }
        if (loggedInUserCache.getIsEmployeeMeal() == false) {
            binding.llOption.isSelected = false
            binding.llModification.isSelected = false
            if (loggedInUserCache.getLoyaltyQrResponse()?.fullName != "" && loggedInUserCache.getLoyaltyQrResponse()?.fullName != null) {
                binding.tvLoyaltyName.isVisible = true
                binding.tvLoyaltyPoint.isVisible = true
                binding.tvLoyaltyName.text = loggedInUserCache.getLoyaltyQrResponse()?.fullName
                binding.tvLoyaltyPoint.text =
                    resources.getString(R.string.leaves).plus(":").plus(userStoreViewModel.getLoyaltyPoint().toString())
            }
        } else {
            binding.llOption.isSelected = true
            binding.tvLoyaltyName.isVisible = true
            binding.llModification.isSelected = true
            binding.llOptionAndModifier.setBackgroundColor(ContextCompat.getColor(this@EditCartActivity, R.color.green_light_50))
            binding.cartViewLinear.setBackgroundColor(ContextCompat.getColor(this@EditCartActivity, R.color.green_light_50))
            binding.cartView.clViewModifiers.setBackgroundColor(ContextCompat.getColor(this@EditCartActivity, R.color.green_light_50))
            binding.tvLoyaltyName.text = loggedInUserCache.getLoyaltyQrResponse()?.fullName
        }
        if (loggedInUserCache.getLoyaltyQrResponse()?.id != "" && loggedInUserCache.getLoyaltyQrResponse()?.id != null &&
            cartItem?.menu?.product?.productLoyaltyTier?.tierValue != 0 && cartItem?.menu?.product?.productLoyaltyTier?.tierValue != null &&
            cartItem?.menuItemComp == 0  &&  loggedInUserCache.getIsEmployeeMeal() == false) {
            binding.cartView.llRedeem.isVisible = true
        }
        binding.cartView.ivAddition.throttleClicks().subscribeAndObserveOnMainThread {
            productQuantity++
            binding.cartView.orderPrizeTextView.text = productPrice.div(100).times(productQuantity).toDollar()
            binding.cartView.productQuantityAppCompatTextView.text = productQuantity.toString()
        }.autoDispose()
        binding.cartView.ivSubtraction.throttleClicks().subscribeAndObserveOnMainThread {
            if (productQuantity != 1) {
                productQuantity--
                binding.cartView.orderPrizeTextView.text = productPrice.div(100).times(productQuantity).toDollar()
                binding.cartView.productQuantityAppCompatTextView.text = productQuantity.toString()
            }
        }.autoDispose()
        binding.cartView.addToCartMaterialButton.throttleClicks().subscribeAndObserveOnMainThread {
            editCartItemProcess()
        }.autoDispose()
        binding.cartView.llRedeem.throttleClicks().subscribeAndObserveOnMainThread {
            isRedeemProduct = false
            if ((cartItem?.menu?.product?.productLoyaltyTier?.tierValue ?: 0) <= (userStoreViewModel.getLoyaltyPoint())) {
                isRedeemProduct = true
                editCartItemProcess()
            } else {
                isRedeemProduct = false
                showToast("You don't have enough leaves to redeem")
            }
        }.autoDispose()
        binding.locationAppCompatTextView.text = loggedInUserCache.getLocationInfo()?.location?.locationName ?: throw Exception("location not found")
        binding.liveTimeTextClock.format12Hour = "hh:mm a"
        binding.cartView.productNameTextView.text = productsItem?.productName
        binding.cartView.tvDescription.text = productsItem?.productDescription
        Glide.with(this).load(productsItem?.productImage).placeholder(R.drawable.ic_launcher_logo).error(R.drawable.ic_launcher_logo)
            .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC).into(binding.cartView.productImageView)
        binding.cartView.productPrize.text = productsItem?.productBasePrice?.div(100).toDollar()
        binding.cartView.orderPrizeTextView.text = productsItem?.productBasePrice?.div(100).toDollar()
        binding.cartView.ivClose.throttleClicks().subscribeAndObserveOnMainThread {
            finish()
        }.autoDispose()
    }

    private fun setStoreOpenAndClose(storeResponse: StoreResponse) {
        val timeZone = storeResponse.locationLocationTimezone ?: "America/Los_Angeles"
        binding.liveTimeTextClock.timeZone = loggedInUserCache.getStoreResponse()?.locationLocationTimezone ?: "America/Los_Angeles"
        val c = Calendar.getInstance()
        val dayOfWeek = c[Calendar.DAY_OF_WEEK]
        val currentTime = getCurrentsStoreTime(timeZone).formatToStoreTime("HH:mm:ss")
        when (dayOfWeek - 1) {
            0 -> {
                val isSundayClosed = storeResponse.isSundayClosed == 1
                binding.openStoreTimeLinearLayout.isSelected = false
                binding.tvOpenAndClose.isSelected = false
                binding.tvOpenAndClose.text = resources.getText(if (isSundayClosed) R.string.close else R.string.open)
                if (!isSundayClosed) {
                    val openTime = storeResponse.sundayOpenTime?.toDate("HH:mm:ss")?.formatTo("HH:mm:ss")
                    val closeTime = storeResponse.sundayCloseTime?.toDate("HH:mm:ss")?.formatTo("HH:mm:ss")
                    if (openTime != null && closeTime != null) {
                        val isOpen = openTime < currentTime && closeTime > currentTime
                        binding.openStoreTimeLinearLayout.isSelected = isOpen
                        binding.tvOpenAndClose.isSelected = isOpen
                        binding.tvOpenAndClose.text = resources.getText(if (isOpen) R.string.open else R.string.close)
                    }
                }
            }
            1 -> {
                val isMondayClosed = storeResponse.isMondayClosed == 1
                binding.openStoreTimeLinearLayout.isSelected = false
                binding.tvOpenAndClose.isSelected = false
                binding.tvOpenAndClose.text = resources.getText(if (isMondayClosed) R.string.close else R.string.open)

                if (!isMondayClosed) {
                    val openTime = storeResponse.mondayOpenTime?.toDate("HH:mm:ss")?.formatTo("HH:mm:ss")
                    val closeTime = storeResponse.mondayCloseTime?.toDate("HH:mm:ss")?.formatTo("HH:mm:ss")

                    if (openTime != null && closeTime != null) {
                        val isOpen = openTime < currentTime && closeTime > currentTime

                        binding.openStoreTimeLinearLayout.isSelected = isOpen
                        binding.tvOpenAndClose.isSelected = isOpen
                        binding.tvOpenAndClose.text = resources.getText(if (isOpen) R.string.open else R.string.close)
                    }
                }
            }
            2 -> {
                val isTuesdayClosed = storeResponse.isTuesdayClosed == 1
                binding.openStoreTimeLinearLayout.isSelected = false
                binding.tvOpenAndClose.isSelected = false
                binding.tvOpenAndClose.text = resources.getText(if (isTuesdayClosed) R.string.close else R.string.open)

                if (!isTuesdayClosed) {
                    val openTime = storeResponse.tuesdayOpenTime?.toDate("HH:mm:ss")?.formatTo("HH:mm:ss")
                    val closeTime = storeResponse.tuesdayCloseTime?.toDate("HH:mm:ss")?.formatTo("HH:mm:ss")

                    if (openTime != null && closeTime != null) {
                        val isOpen = openTime < currentTime && closeTime > currentTime

                        binding.openStoreTimeLinearLayout.isSelected = isOpen
                        binding.tvOpenAndClose.isSelected = isOpen
                        binding.tvOpenAndClose.text = resources.getText(if (isOpen) R.string.open else R.string.close)
                    }
                }
            }
            3 -> {
                val isWednesdayClosed = storeResponse.isWednesdayClosed == 1
                binding.openStoreTimeLinearLayout.isSelected = false
                binding.tvOpenAndClose.isSelected = false
                binding.tvOpenAndClose.text = resources.getText(if (isWednesdayClosed) R.string.close else R.string.open)

                if (!isWednesdayClosed) {
                    val openTime = storeResponse.wednesdayOpenTime?.toDate("HH:mm:ss")?.formatTo("HH:mm:ss")
                    val closeTime = storeResponse.wednesdayCloseTime?.toDate("HH:mm:ss")?.formatTo("HH:mm:ss")

                    if (openTime != null && closeTime != null) {
                        val isOpen = openTime < currentTime && closeTime > currentTime

                        binding.openStoreTimeLinearLayout.isSelected = isOpen
                        binding.tvOpenAndClose.isSelected = isOpen
                        binding.tvOpenAndClose.text = resources.getText(if (isOpen) R.string.open else R.string.close)
                    }
                }
            }
            4 -> {
                val isThursdayClosed = storeResponse.isThursdayClosed == 1
                binding.openStoreTimeLinearLayout.isSelected = false
                binding.tvOpenAndClose.isSelected = false
                binding.tvOpenAndClose.text = resources.getText(if (isThursdayClosed) R.string.close else R.string.open)

                if (!isThursdayClosed) {
                    val openTime = storeResponse.thursdayOpenTime?.toDate("HH:mm:ss")?.formatTo("HH:mm:ss")
                    val closeTime = storeResponse.thursdayCloseTime?.toDate("HH:mm:ss")?.formatTo("HH:mm:ss")

                    if (openTime != null && closeTime != null) {
                        val isOpen = openTime < currentTime && closeTime > currentTime

                        binding.openStoreTimeLinearLayout.isSelected = isOpen
                        binding.tvOpenAndClose.isSelected = isOpen
                        binding.tvOpenAndClose.text = resources.getText(if (isOpen) R.string.open else R.string.close)
                    }
                }
            }
            5 -> {
                val isFridayClosed = storeResponse.isFridayClosed == 1
                binding.openStoreTimeLinearLayout.isSelected = false
                binding.tvOpenAndClose.isSelected = false
                binding.tvOpenAndClose.text = resources.getText(if (isFridayClosed) R.string.close else R.string.open)

                if (!isFridayClosed) {
                    val openTime = storeResponse.fridayOpenTime?.toDate("HH:mm:ss")?.formatTo("HH:mm:ss")
                    val closeTime = storeResponse.fridayCloseTime?.toDate("HH:mm:ss")?.formatTo("HH:mm:ss")

                    if (openTime != null && closeTime != null) {
                        val isOpen = openTime < currentTime && closeTime > currentTime

                        binding.openStoreTimeLinearLayout.isSelected = isOpen
                        binding.tvOpenAndClose.isSelected = isOpen
                        binding.tvOpenAndClose.text = resources.getText(if (isOpen) R.string.open else R.string.close)
                    }
                }
            }
            6 -> {
                val isSaturdayClosed = storeResponse.isSaturdayClosed == 1
                binding.openStoreTimeLinearLayout.isSelected = false
                binding.tvOpenAndClose.isSelected = false
                binding.tvOpenAndClose.text = resources.getText(if (isSaturdayClosed) R.string.close else R.string.open)

                if (!isSaturdayClosed) {
                    val openTime = storeResponse.saturdayOpenTime?.toDate("HH:mm:ss")?.formatTo("HH:mm:ss")
                    val closeTime = storeResponse.saturdayCloseTime?.toDate("HH:mm:ss")?.formatTo("HH:mm:ss")

                    if (openTime != null && closeTime != null) {
                        val isOpen = openTime < currentTime && closeTime > currentTime

                        binding.openStoreTimeLinearLayout.isSelected = isOpen
                        binding.tvOpenAndClose.isSelected = isOpen
                        binding.tvOpenAndClose.text = resources.getText(if (isOpen) R.string.open else R.string.close)
                    }
                }
            }
        }
    }

    private fun initAdapter() {
        val optionList = productsItem?.modification?.get(0)?.options

        orderOptionAdapter = OrderOptionAdapter(this).apply {
            optionActionState.subscribeAndObserveOnMainThread { item ->
                if (singleSelection == true) {
                    val listOfOption = orderOptionAdapter.listOfOrderSubItem
                    listOfOption?.filter { it.isCheck == true }?.forEach {
                        it.isCheck = false
                    }
                    listOfOption?.find { it.id == item.id }?.apply {
                        isCheck = true
                        addedSelection()
                    }
                    orderOptionAdapter.listOfOrderSubItem = listOfOption?.filter { it.active == true } ?: arrayListOf()
                } else {
                    val listOfOption = orderOptionAdapter.listOfOrderSubItem
                    if (item.isCheck == true) {
                        listOfOption?.find { it.id == item.id }?.apply {
                            isCheck = false
                            addedSelection()
                        }
                    } else {
                        if (selectedModificationItem?.selectedOptionsItem == null) {
                            if (0 < selectedModificationItem?.selectMax!!) {
                                listOfOption?.find { it.id == item.id }?.apply {
                                    isCheck = !isCheck!!
                                    addedSelection()
                                }
                            } else {
                                showToast("you have select maximum option")
                            }
                        } else {
                            if (selectedModificationItem?.selectedOptionsItem!!.size < selectedModificationItem?.selectMax!!) {
                                listOfOption?.find { it.id == item.id }?.apply {
                                    isCheck = !isCheck!!
                                    addedSelection()
                                }
                            } else {
                                showToast("you have select maximum option")
                            }
                        }

                    }

                    orderOptionAdapter.listOfOrderSubItem = listOfOption?.filter { it.active == true } ?: arrayListOf()
                }

            }.autoDispose()

        }

        binding.rvOption.layoutManager = FlowLayoutManager()

        binding.rvOption.apply {
            adapter = orderOptionAdapter
        }

        modificationAdapter = ModificationAdapter(this).apply {
            modificationActionState.subscribeAndObserveOnMainThread { item ->
                val listOfModification = modificationAdapter.listOfModification
                listOfModification?.filter { it.isSelected == true }?.forEach {
                    it.isSelected = false
                }
                listOfModification?.find { it.id == item.id }?.apply {
                    isSelected = true
                    modificationSelection(item)
                }
                modificationAdapter.listOfModification = listOfModification?.filter { it.active == true }
            }.autoDispose()
        }
        binding.rvModification.layoutManager = LinearLayoutManager(this, RecyclerView.HORIZONTAL, false)

        binding.rvModification.apply {
            adapter = modificationAdapter
        }
        modifierOptionAdapter = ModifierOptionAdapter(this).apply {
            selectedOptionActionState.subscribeAndObserveOnMainThread { item ->
                val list = modifierOptionAdapter.listOfModification
                list?.forEach {
                    orderOptionAdapter.listOfOrderSubItem?.find { it.id?.equals(item.id)!! }?.apply {
                        isCheck = false
                    }
                    if (it.selectedOptionsItem.contains(item)) {
                        val listOfModification = modificationAdapter.listOfModification
                        listOfModification?.forEach {
                            it.options?.find {
                                it.id == item.id
                            }?.apply {
                                isCheck = false
                            }
                        }
                        it.selectedOptionsItem.remove(item)
                    }
                    if (it.isSelected == true) {
                        orderOptionAdapter.listOfOrderSubItem = orderOptionAdapter.listOfOrderSubItem?.filter { it.active == true } ?: arrayListOf()
                    }
                }
                modifierOptionAdapter.listOfModification = list
            }.autoDispose()
        }
        binding.cartView.rvModifiers.layoutManager = LinearLayoutManager(this, RecyclerView.VERTICAL, false)
        binding.cartView.rvModifiers.apply {
            adapter = modifierOptionAdapter
        }

    }

    private fun addedSelection() {
        val list = modificationAdapter.listOfModification
        val listofModification = ArrayList<MenuItemModifiersItemRequest>()
        list?.forEachIndexed { index, it ->
            val listOfOption = ArrayList<OptionsItemRequest>()
            it.options?.forEachIndexed { index, item ->
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
            it.selectedOptionsItem = listOfOption
            modifierOptionAdapter.listOfModification = list
            listofModification.add(
                MenuItemModifiersItemRequest(
                    selectMax = it.selectMax,
                    isRequired = if (it.isRequired == true) 1 else 0,
                    active = it.active,
                    selectMin = it.selectMin,
                    id = it.id,
                    modificationText = it.modificationText,
                    type = it.type,
                    options = it.selectedOptionsItem

                )
            )
        }
        addToCart = AddToCartRequest(
            locationId = loggedInUserCache.getLocationInfo()?.location?.id,
            orderTypeId = loggedInUserCache.getorderTypeId(),
            modeId = Constants.MODE_ID,
            promisedTime = "",
            menuId = menuId,
            menuItemQuantity = productQuantity,
            menuItemModifiers = listofModification
        )
        productPriceCount()
    }

    private fun modificationSelection(modificationItem: ModificationItem?) {
        if (modificationItem?.isRequired == true && modificationItem.selectMax == 1) {
            singleSelection = true
            selectMax = modificationItem.selectMax
            binding.tvOptionRequire.text = "${modificationItem.selectMax}".plus(" Option required").toUpperCase()
            binding.tvOptionRequire.setTextColor(resources.getColor(R.color.green_light))
        } else {
            singleSelection = false
            if (modificationItem?.selectMax != null) {
                selectMax = modificationItem?.selectMax
            }
            binding.tvOptionRequire.setTextColor(resources.getColor(R.color.grey))
            binding.tvOptionRequire.text = "Optional".toUpperCase()
        }
        selectedModificationItem = modificationItem
        productsItem?.modification?.forEach {
            if (it.id == modificationItem?.id) {
                orderOptionAdapter.listOfOrderSubItem = it.options?.filter { it.active == true } ?: arrayListOf()
            }
        }
    }

    private fun productPriceCount() {
        val list = modificationAdapter.listOfModification
        productPrice = productsItem?.productBasePrice!!
        list?.forEach {
            it.options?.forEach {
                if (it.isCheck == true) {
                    productPrice = it.optionPrice?.let { it1 -> productPrice.plus(it1) }!!
                }
            }
        }
        binding.cartView.orderPrizeTextView.text = productPrice.div(100).times(productQuantity).toDollar()
        binding.tvTotalPrizeNumber.text = productPrice.div(100).times(productQuantity).toDollar()
    }

    private fun editCartItemProcess() {
        val list = modificationAdapter.listOfModification
        val listofModification = ArrayList<MenuItemModifiersItemRequest>()
        val now = Calendar.getInstance()
        now.add(Calendar.MINUTE, 30)
        isbrack = false
        menuItemInstructions = if (binding.specialInstructionsEditText.text.toString() == "") {
            null
        } else {
            binding.specialInstructionsEditText.text.toString()
        }
        val df = SimpleDateFormat("yyy-MM-dd HH:mm:ss")
        val promisedTime = df.format(now.time)
        list?.forEachIndexed { index, it ->
            val listOfOption = ArrayList<OptionsItemRequest>()
            it.options?.forEachIndexed { index, item ->
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
            it.selectedOptionsItem = listOfOption
            if (it.isRequired == true) {
                if (it.selectedOptionsItem.isNotEmpty()) {
                    if (it.selectedOptionsItem.size <= it.selectMax!!) {
                        listofModification.add(
                            MenuItemModifiersItemRequest(
                                selectMax = it.selectMax,
                                isRequired = if (it.isRequired == true) 1 else 0,
                                active = it.active,
                                selectMin = it.selectMin,
                                id = it.id,
                                modificationText = it.modificationText,
                                options = it.selectedOptionsItem,
                                type = it.type

                            )
                        )
                    } else {
                        if (it.selectMin != 0 && it.isRequired == true) {
                            isbrack = true
                            showToast("you can select minimum ${it.selectMin} option in ${it.modificationText}")
                        }
                    }
                } else {
                    isbrack = true
                    showToast("you can select minimum ${it.selectMin} option in ${it.modificationText}")
                }
            } else {
                if (it.selectedOptionsItem.isNotEmpty()) {
                    if (it.selectedOptionsItem.size <= it.selectMax!!) {
                        listofModification.add(
                            MenuItemModifiersItemRequest(
                                selectMax = it.selectMax,
                                isRequired = if (it.isRequired == true) 1 else 0,
                                active = it.active,
                                selectMin = it.selectMin,
                                id = it.id,
                                modificationText = it.modificationText,
                                options = it.selectedOptionsItem,
                                type = it.type

                            )
                        )
                    } else {
                        if (it.selectMin != 0 && it.isRequired == true) {
                            isbrack = true
                            showToast("you can select minimum ${it.selectMin} option in ${it.modificationText}")
                        }
                    }
                }
            }
        }

        if (listofModification.isNotEmpty()) {
            val updateMenuItemQuantity = UpdateMenuItemQuantity(
                cartId = cartItem?.id,
                menuItemQuantity = productQuantity,
                menuItemModifiers = listofModification,
                menuItemInstructions = binding.specialInstructionsEditText.text.toString().ifEmpty { null },
            )
            if (!isbrack) {
                userStoreViewModel.updateMenuItemQuantity(updateMenuItemQuantity)
            }
        }
    }

    private fun buttonVisibility(isLoading: Boolean) {
        binding.cartView.addToCartMaterialButton.visibility = if (isLoading) View.GONE else View.VISIBLE
        if (binding.cartView.llRedeem.isVisible) {
            binding.cartView.llRedeem.visibility = if (isLoading) View.GONE else View.VISIBLE
        }
        binding.cartView.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
    }

    override fun onUserInteraction() {
        binding.headerUserStore.alpha = 1F
        binding.llBottom.alpha = 1F
        binding.userStoreView.alpha = 1F
        Timber.tag("MainActivity").d("onUserInteraction")
        resetDisconnectTimer()
    }

    private val disconnectHandler = Handler {
        Timber.tag("MainActivity").d("disconnectHandler")
        false
    }

    private val showToastHandler = Handler {
        Timber.tag("SessionTimeOutActivity").d("showToastHandler")
        false
    }

    private val disconnectCallback = Runnable { // Perform any required operation on disconnect
        Timber.tag("MainActivity").d("disconnectCallback")
        loggedInUserCache.clearLoggedInUserLocalPrefs()
        Toast.makeText(applicationContext, "Crew time out", Toast.LENGTH_LONG).show()
        startNewActivityWithDefaultAnimation(
            LoginActivity.getIntent(
                this@EditCartActivity, loggedInUserCache.getLocationInfo()?.location?.id ?: null
            )
        )
        finish()
    }

    private val showToastCallback = Runnable { // Perform any required operation on disconnect
        Timber.tag("MainActivity").d("showToastCallback")
        if (loggedInUserCache.isUserLoggedIn()) {
            binding.headerUserStore.alpha = 0.2F
            binding.llBottom.alpha = 0.2F
            binding.userStoreView.alpha = 0.2F
        }

    }

    private fun resetDisconnectTimer() {
        Timber.tag("MainActivity").d("resetDisconnectTimer")
        disconnectHandler.removeCallbacks(disconnectCallback)
        showToastHandler.removeCallbacks(showToastCallback)
        disconnectHandler.postDelayed(disconnectCallback, DISCONNECT_TIMEOUT)
        showToastHandler.postDelayed(showToastCallback, SHOW_TOAST_TIMER)
    }

    fun stopDisconnectTimer() {
        disconnectHandler.removeCallbacks(disconnectCallback)
        showToastHandler.removeCallbacks(showToastCallback)
    }


    override fun onPause() {
        super.onPause()
        Timber.tag("MainActivity").d("stopDisconnectTimer")
        stopDisconnectTimer()
    }
}