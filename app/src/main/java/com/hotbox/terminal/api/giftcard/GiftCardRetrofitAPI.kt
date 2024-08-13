package com.hotbox.terminal.api.giftcard

import com.hotbox.terminal.api.checkout.model.GiftCardData
import com.hotbox.terminal.api.checkout.model.GiftCardResponse
import com.hotbox.terminal.api.giftcard.model.BuyPhysicalCardRequest
import com.hotbox.terminal.api.giftcard.model.BuyVirtualCardRequest
import com.hotbox.terminal.api.stripe.model.CaptureNewPaymentRequest
import com.hotbox.terminal.api.stripe.model.ResponseItem
import com.hotbox.terminal.base.network.model.HotBoxResponse
import io.reactivex.Single
import retrofit2.http.*

interface GiftCardRetrofitAPI {

    @GET("v1/gift-cart/get-gift-card-by-qr")
    fun giftCardQRCode(@Query("token") data :String) : Single<HotBoxResponse<GiftCardResponse>>

    @GET("v1/gift-cart/get-gift-card")
    fun applyGiftCard(@Query("gift_card_code") giftCardCode :String?) :Single<HotBoxResponse<GiftCardData>>

    @POST("v1/pos/buy-gift-card")
    fun buyVirtualGiftCard(@Body request: BuyVirtualCardRequest) :Single<HotBoxResponse<GiftCardData>>

    @POST("v1/pos/buy-gift-card")
    fun buyPhysicalGiftCard(@Body request: BuyPhysicalCardRequest) :Single<HotBoxResponse<GiftCardData>>

    @POST("https://veritas.rest.iconncloud.net/tsi/v1/payment")
    fun captureNewPayment(@Body newPaymentRequest : CaptureNewPaymentRequest) : Single<List<ResponseItem>>

}