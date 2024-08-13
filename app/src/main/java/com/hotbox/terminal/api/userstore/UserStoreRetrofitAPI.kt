package com.hotbox.terminal.api.userstore

import com.hotbox.terminal.api.authentication.model.HealthNutUser
import com.hotbox.terminal.api.checkout.model.LoyaltyWithPhoneResponse
import com.hotbox.terminal.api.checkout.model.QRScanResponse
import com.hotbox.terminal.api.checkout.model.UserLoyaltyPointResponse
import com.hotbox.terminal.api.menu.model.MenuListInfo
import com.hotbox.terminal.api.menu.model.ProductsItem
import com.hotbox.terminal.api.order.model.OrderStatusRequest
import com.hotbox.terminal.api.order.model.UpdatedOrderStatusResponse
import com.hotbox.terminal.api.userstore.model.*
import com.hotbox.terminal.base.network.model.HotBoxCommonResponse
import com.hotbox.terminal.base.network.model.HotBoxResponse
import com.hotbox.terminal.base.network.model.HotBoxResponses
import io.reactivex.Single
import retrofit2.http.*

interface UserStoreRetrofitAPI {

    @GET("v1/menus/get-menus-and-products")
    fun getMenuProductByLocation(@Query("location_id") locationId: Int,@Query("platform") platform: String): Single<HotBoxResponse<MenuListInfo>>

    @GET("v1/products/get-product")
    fun getProductDetails(
        @Query("product_id") productId: String? = null,
        @Query("platform") platform: String
    ): Single<HotBoxResponse<ProductsItem>>

    @POST("v1/cart/add-to-cart")
    fun addToCartProduct(@Body request: AddToCartRequest): Single<HotBoxResponse<AddToCartResponse>>

    @POST("v1/cart/redeem-cart-item")
    fun redeemCartItem(@Body request: UpdateMenuItemQuantity): Single<HotBoxResponse<AddToCartResponse>>

    @GET("v1/cart/get-cart")
    fun getCartDetails(@Query("cart_group_id") cartGroupId: Int): Single<HotBoxResponse<CartInfoDetails>>

    @POST("v1/cart/update-cart")
    fun updateMenuItemQuantity(@Body request: UpdateMenuItemQuantity): Single<HotBoxResponse<UpdateCartResponse>>

    @GET("v1/cart/delete-cart")
    fun deleteCartItem(@Query("cart_id") id: Int): Single<HotBoxResponse<DeleteCartItemRequest>>

    @POST("v1/orders/create-pos-order")
    fun createOrder(@Body request: CreateOrderRequest) :Single<HotBoxResponse<CreateOrderResponse>>

    @POST("v1/clear-cart")
    fun clearCart(@Body request: DeleteCartItemRequest) :Single<HotBoxCommonResponse>

    @GET("v1/cart/get-next-available-time")
    fun getOrderPromisedTime(@Query("location_id") locationId :Int,@Query("order_type") orderTypeId :Int) :Single<HotBoxResponse<GetPromisedTime>>

    @POST("v1/pos/comp-cart-item")
    fun compProduct(@Body request: CompProductRequest): Single<HotBoxResponse<AddToCartResponse>>

    @GET("v1/cart/get-employee-discount")
    fun employeeDiscount(@Query("sub_total") subTotal: Int): Single<HotBoxResponse<EmployeeDiscountResponse>>

    @GET("v1/loyalty/get-loyalties")
    fun getLoyaltyPoints(@Query("id") id: Int?):Single<HotBoxResponse<UserLoyaltyPointResponse>>


    @GET("v1/loyalty/get-loyalty-by-qr")
    fun getDataFromQRCode(@Query("token") data :String) :Single<HotBoxResponse<QRScanResponse>>

    @GET("v1/loyalty/get-loyalties")
    fun getPhoneLoyaltyData(@Query("user_phone") id: String?):Single<HotBoxResponse<LoyaltyWithPhoneResponse>>

    @GET("v1/users/get-user")
    fun getUser(@Query("id") userId :String?) :Single<HotBoxResponse<HealthNutUser>>

    @GET("v1/pos/send-receipt")
    fun sendReceipt(@Query("order_id") orderId :Int?,@Query("type")type :String,@Query("email") email :String? =null,@Query("phone") phone :String? =null) :Single<HotBoxResponses>

    @POST("v1/pos/update-order-status")
    fun updateOrderStatus(@Body request: OrderStatusRequest) :Single<HotBoxResponse<UpdatedOrderStatusResponse>>
}