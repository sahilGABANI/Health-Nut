package com.hotbox.terminal.api.checkout

import com.hotbox.terminal.api.authentication.model.HealthNutUser
import com.hotbox.terminal.api.checkout.model.*
import com.hotbox.terminal.base.network.model.HotBoxResponse
import io.reactivex.Single
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface CheckOutRetrofitAPI {

    @GET("v1/loyalty/get-loyalty-by-qr")
    fun getDataFromQRCode(@Query("token") data :String) :Single<HotBoxResponse<QRScanResponse>>

    @GET("v1/gift-cart/get-gift-card-by-qr")
    fun giftCardQRCode(@Query("token") data :String) :Single<HotBoxResponse<GiftCardResponse>>

    @GET("v1/loyalty/get-loyalties")
    fun getLoyaltyPoints(@Query("id") id: String?):Single<HotBoxResponse<UserLoyaltyPointResponse>>

    @GET("v1/loyalty/get-loyalties")
    fun getPhoneLoyaltyData(@Query("user_phone") id: String?):Single<HotBoxResponse<LoyaltyWithPhoneResponse>>

    @GET("v1/users/get-user")
    fun getUser(@Query("id") userId :String?) :Single<HotBoxResponse<HealthNutUser>>

    @GET("v1/gift-cart/get-gift-card")
    fun applyGiftCard(@Query("gift_card_code") giftCardCode :String?) :Single<HotBoxResponse<GiftCardData>>

    @GET("v1/cart/get-coupon-discount")
    fun applyPromocode(@Query("coupon") coupon: String?, @Query("cart_group_id") cartGroupId: Int?, @Query("subtotal") subtotal: Int?, @Query("delivery_fee") deliveryFee: Int?) :Single<HotBoxResponse<PromoCodeResponse>>

    @POST("v1/auth/register")
    fun createUser(@Body request: CreateUserRequest) :Single<HotBoxResponse<CreateUserResponse>>
}