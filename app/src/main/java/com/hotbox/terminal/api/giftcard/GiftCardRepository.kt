package com.hotbox.terminal.api.giftcard

import com.hotbox.terminal.api.authentication.LoggedInUserCache
import com.hotbox.terminal.api.checkout.model.GiftCardData
import com.hotbox.terminal.api.checkout.model.GiftCardResponse
import com.hotbox.terminal.api.giftcard.model.BuyPhysicalCardRequest
import com.hotbox.terminal.api.giftcard.model.BuyVirtualCardRequest
import com.hotbox.terminal.api.stripe.model.CaptureNewPaymentRequest
import com.hotbox.terminal.api.stripe.model.ResponseItem
import com.hotbox.terminal.base.network.HotBoxResponseConverter
import io.reactivex.Single

class GiftCardRepository(private val giftCardRetrofitAPI: GiftCardRetrofitAPI,private val loggedInUserCache: LoggedInUserCache,) {

    private val hotBoxResponseConverter: HotBoxResponseConverter = HotBoxResponseConverter()
    private lateinit var readerId : String

    fun giftCardQRCode(data: String): Single<GiftCardResponse> {
        return giftCardRetrofitAPI.giftCardQRCode(data)
            .flatMap { hotBoxResponseConverter.convertToSingle(it) }
    }

    fun applyGiftCard(cardNumber: String):Single<GiftCardData> {
        return giftCardRetrofitAPI.applyGiftCard(cardNumber)
            .flatMap { hotBoxResponseConverter.convertToSingle(it) }
    }

    fun buyVirtualGiftCard(request: BuyVirtualCardRequest):Single<GiftCardData> {
        return giftCardRetrofitAPI.buyVirtualGiftCard(request)
            .flatMap { hotBoxResponseConverter.convertToSingle(it) }
    }

    fun buyPhysicalGiftCard(request: BuyPhysicalCardRequest):Single<GiftCardData> {
        return giftCardRetrofitAPI.buyPhysicalGiftCard(request)
            .flatMap { hotBoxResponseConverter.convertToSingle(it) }
    }

    fun captureNewPayment(newPaymentRequest : CaptureNewPaymentRequest): Single<List<ResponseItem>> {
        return giftCardRetrofitAPI.captureNewPayment(newPaymentRequest).flatMap {
            Single.just(it)
        }
    }

}