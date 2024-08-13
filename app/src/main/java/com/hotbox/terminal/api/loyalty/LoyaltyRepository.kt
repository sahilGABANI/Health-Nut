package com.hotbox.terminal.api.loyalty

import com.hotbox.terminal.api.authentication.LoggedInUserCache
import com.hotbox.terminal.api.authentication.model.HealthNutUser
import com.hotbox.terminal.api.checkout.model.LoyaltyWithPhoneResponse
import com.hotbox.terminal.api.loyalty.model.AddLoyaltyPointRequest
import com.hotbox.terminal.api.loyalty.model.AddLoyaltyPointResponse
import com.hotbox.terminal.api.loyalty.model.OrderLoyaltyInfo
import com.hotbox.terminal.api.order.model.OrderDetailsResponse
import com.hotbox.terminal.base.network.HotBoxResponseConverter
import com.hotbox.terminal.base.network.model.HotBoxResponse
import io.reactivex.Single

class LoyaltyRepository(private val loyaltyRetrofitAPI: LoyaltyRetrofitAPI, private val loggedInUserCache: LoggedInUserCache) {
    private val hotBoxResponseConverter: HotBoxResponseConverter = HotBoxResponseConverter()

    fun getPhoneLoyaltyData(data: String): Single<HotBoxResponse<LoyaltyWithPhoneResponse>> {
        return loyaltyRetrofitAPI.getPhoneLoyaltyData(data).flatMap { hotBoxResponseConverter.convertToSingleWithFullResponse(it) }
    }

    fun getOrderDetailsData(OrderId: Long): Single<OrderDetailsResponse> {
        return loyaltyRetrofitAPI.getOrderDetails(OrderId).flatMap { hotBoxResponseConverter.convertToSingle(it) }
    }

     fun getUserDetails(id: String?): Single<HealthNutUser> {
        return loyaltyRetrofitAPI.getUserDetails(id).flatMap { hotBoxResponseConverter.convertToSingle(it) }
    }

    fun addLoyaltyPoint(request: AddLoyaltyPointRequest): Single<HotBoxResponse<AddLoyaltyPointResponse>> {
        return loyaltyRetrofitAPI.addLoyaltyPoint(request).flatMap { hotBoxResponseConverter.convertToSingleWithFullResponse(it) }
    }

    fun getOrderLoyalty(orderId :Long): Single<HotBoxResponse<OrderLoyaltyInfo>> {
        return loyaltyRetrofitAPI.getOrderLoyalty(orderId).flatMap { hotBoxResponseConverter.convertToSingleWithFullResponse(it) }
    }
}