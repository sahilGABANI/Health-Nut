package com.hotbox.terminal.api.userstore

import com.hotbox.terminal.api.authentication.LoggedInUserCache
import com.hotbox.terminal.api.authentication.model.HealthNutUser
import com.hotbox.terminal.api.checkout.model.LoyaltyWithPhoneResponse
import com.hotbox.terminal.api.checkout.model.QRScanResponse
import com.hotbox.terminal.api.checkout.model.UserLoyaltyPointResponse
import com.hotbox.terminal.api.menu.model.MenuListInfo
import com.hotbox.terminal.api.menu.model.ProductsItem
import com.hotbox.terminal.api.order.model.OrderStatusRequest
import com.hotbox.terminal.api.order.model.UpdatedOrderStatusResponse
import com.hotbox.terminal.api.userstore.model.*
import com.hotbox.terminal.base.network.HotBoxResponseConverter
import com.hotbox.terminal.base.network.model.HotBoxCommonResponse
import com.hotbox.terminal.base.network.model.HotBoxResponses
import com.hotbox.terminal.utils.Constants
import com.hotbox.terminal.utils.Constants.KIOSK
import com.hotbox.terminal.utils.Constants.POS
import io.reactivex.Single

class UserStoreRepository(private val userStoreRetrofitAPI: UserStoreRetrofitAPI, private val loggedInUserCache: LoggedInUserCache) {

    private val hotBoxResponseConverter: HotBoxResponseConverter = HotBoxResponseConverter()

    private var loyaltyPoint: Int = 0

    fun getMenuProductByLocation(): Single<MenuListInfo> {
        val locationId = loggedInUserCache.getLocationInfo()?.location?.id ?: throw Exception("location not found")
        val platfrom = if (loggedInUserCache.isUserLoggedIn()) {
            POS
        } else {
            KIOSK
        }
        return userStoreRetrofitAPI.getMenuProductByLocation(locationId, platfrom).flatMap {
            hotBoxResponseConverter.convertToSingle(it)
        }
    }

    fun getMenuProductByLocation(productId: String?): Single<ProductsItem> {
        val platfrom = if (loggedInUserCache.isUserLoggedIn()) {
            POS
        } else {
            KIOSK
        }
        return userStoreRetrofitAPI.getProductDetails(productId, platfrom).flatMap {
            hotBoxResponseConverter.convertToSingle(it)
        }
    }


    fun getLoyaltyPoints(userId: Int?): Single<UserLoyaltyPointResponse> {
        return userStoreRetrofitAPI.getLoyaltyPoints(userId).flatMap { hotBoxResponseConverter.convertToSingle(it) }.doOnSuccess {
            loyaltyPoint = it.points ?: 0
        }
    }

    fun getPhoneLoyaltyData(data: String): Single<LoyaltyWithPhoneResponse> {
        return userStoreRetrofitAPI.getPhoneLoyaltyData(data).flatMap { hotBoxResponseConverter.convertToSingle(it) }.doOnSuccess {
            loyaltyPoint = it.points ?: 0
        }
    }

    fun getUser(userId: String?): Single<HealthNutUser> {
        return userStoreRetrofitAPI.getUser(userId).flatMap { hotBoxResponseConverter.convertToSingle(it) }
    }

    fun addToCartProduct(request: AddToCartRequest): Single<AddToCartResponse> {
        return userStoreRetrofitAPI.addToCartProduct(request).flatMap {
            hotBoxResponseConverter.convertToSingle(it)
        }
    }

    fun redeemCartItem(request: UpdateMenuItemQuantity, point: Int? = 0): Single<AddToCartResponse> {
        return userStoreRetrofitAPI.redeemCartItem(request).flatMap {
            hotBoxResponseConverter.convertToSingle(it)
        }.doOnSuccess {
            loyaltyPoint -= point ?: 0
        }
    }

    fun getCartDetails(cartGroupId: Int): Single<CartInfoDetails> {
        return userStoreRetrofitAPI.getCartDetails(cartGroupId).flatMap {
            hotBoxResponseConverter.convertToSingle(it)
        }
    }

    fun updateMenuItemQuantity(request: UpdateMenuItemQuantity, point: Int?): Single<UpdateCartResponse> {
        return userStoreRetrofitAPI.updateMenuItemQuantity(request).flatMap {
            hotBoxResponseConverter.convertToSingle(it)
        }.doOnSuccess {
            loyaltyPoint -= point ?: 0
        }
    }

    fun deleteCartItem(cartId: Int, point: Int? = 0): Single<DeleteCartItemRequest> {
        return userStoreRetrofitAPI.deleteCartItem(cartId).flatMap {
            hotBoxResponseConverter.convertToSingle(it)
        }.doOnSuccess {
            loyaltyPoint += point ?: 0
        }
    }

    fun createOrder(request: CreateOrderRequest): Single<CreateOrderResponse> {
        return userStoreRetrofitAPI.createOrder(request).flatMap {
            hotBoxResponseConverter.convertToSingle(it)
        }
    }

    fun clearCart(request: DeleteCartItemRequest): Single<HotBoxCommonResponse> {
        return userStoreRetrofitAPI.clearCart(request).flatMap {
            hotBoxResponseConverter.convertCommonResponse(it)
        }
    }

    fun getOrderPromisedTime(): Single<GetPromisedTime> {
        val locationId = loggedInUserCache.getLocationInfo()?.location?.id ?: throw Exception("location not found")
        val orderTypeId = loggedInUserCache.getorderTypeId() ?: throw Exception("location not found")
        return userStoreRetrofitAPI.getOrderPromisedTime(locationId, orderTypeId).flatMap {
            hotBoxResponseConverter.convertToSingle(it)
        }
    }

    fun compProduct(request: CompProductRequest): Single<AddToCartResponse> {
        return userStoreRetrofitAPI.compProduct(request).flatMap {
            hotBoxResponseConverter.convertToSingle(it)
        }
    }

    fun employeeDiscount(toInt: Int): Single<EmployeeDiscountResponse> {
        return userStoreRetrofitAPI.employeeDiscount(toInt).flatMap {
            hotBoxResponseConverter.convertToSingle(it)
        }
    }

    fun getDataFromQRCode(data: String): Single<QRScanResponse> {
        return userStoreRetrofitAPI.getDataFromQRCode(data).flatMap { hotBoxResponseConverter.convertToSingle(it) }.doOnSuccess {
            loyaltyPoint = it.points ?: 0
        }
    }


    fun getCurrentLoyaltyPoint(): Int {
        return loyaltyPoint
    }

    fun sendReceipt(orderId: Int, type: String, email: String?, phone: String?): Single<HotBoxResponses> {
        return userStoreRetrofitAPI.sendReceipt(orderId, type, email, phone)
    }

    fun updateOrderStatus(orderStatus: String, orderId: Int): Single<UpdatedOrderStatusResponse> {
        val autoReceiverId = loggedInUserCache.getAutoReceiverId()
        return userStoreRetrofitAPI.updateOrderStatus(
            OrderStatusRequest(orderStatus = orderStatus, orderId = orderId, userId = autoReceiverId)
        ).flatMap { hotBoxResponseConverter.convertToSingle(it) }

    }
}