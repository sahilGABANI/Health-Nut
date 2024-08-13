package com.hotbox.terminal.api.checkout

import com.hotbox.terminal.api.authentication.model.HealthNutUser
import com.hotbox.terminal.api.checkout.model.*
import com.hotbox.terminal.base.network.HotBoxResponseConverter
import com.hotbox.terminal.base.network.model.HotBoxResponse
import io.reactivex.Single

class CheckOutRepository(private val checkOutRetrofitAPI: CheckOutRetrofitAPI) {

    private val hotBoxResponseConverter: HotBoxResponseConverter = HotBoxResponseConverter()

    fun getDataFromQRCode(data: String): Single<QRScanResponse> {
        return checkOutRetrofitAPI.getDataFromQRCode(data)
            .flatMap { hotBoxResponseConverter.convertToSingle(it) }
    }
    fun getPhoneLoyaltyData(data: String): Single<LoyaltyWithPhoneResponse> {
        return checkOutRetrofitAPI.getPhoneLoyaltyData(data)
            .flatMap { hotBoxResponseConverter.convertToSingle(it) }
    }

    fun giftCardQRCode(data: String): Single<GiftCardResponse> {
        return checkOutRetrofitAPI.giftCardQRCode(data)
            .flatMap { hotBoxResponseConverter.convertToSingle(it) }
    }

    fun getLoyaltyPoints(userId : String?) :Single<UserLoyaltyPointResponse> {
        return checkOutRetrofitAPI.getLoyaltyPoints(userId)
            .flatMap { hotBoxResponseConverter.convertToSingle(it) }
    }

    fun getUser(userId: String?):Single<HealthNutUser> {
        return checkOutRetrofitAPI.getUser(userId)
            .flatMap { hotBoxResponseConverter.convertToSingle(it) }
    }
    fun applyGiftCard(cardNumber: String):Single<GiftCardData> {
        return checkOutRetrofitAPI.applyGiftCard(cardNumber)
            .flatMap { hotBoxResponseConverter.convertToSingle(it) }
    }
    fun applyPromocode(request: PromoCodeRequest):Single<PromoCodeResponse> {
        return checkOutRetrofitAPI.applyPromocode(request.coupon,request.cartGroupId,request.subtotal?.toInt(),request.deliveryFee)
            .flatMap { hotBoxResponseConverter.convertToSingle(it) }
    }
    fun createUser(request: CreateUserRequest):Single<HotBoxResponse<CreateUserResponse>> {
        return checkOutRetrofitAPI.createUser(request)
    }
}