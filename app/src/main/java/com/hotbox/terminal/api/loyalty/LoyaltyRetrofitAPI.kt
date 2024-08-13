package com.hotbox.terminal.api.loyalty

import com.hotbox.terminal.api.authentication.model.HealthNutUser
import com.hotbox.terminal.api.checkout.model.LoyaltyWithPhoneResponse
import com.hotbox.terminal.api.loyalty.model.AddLoyaltyPointRequest
import com.hotbox.terminal.api.loyalty.model.AddLoyaltyPointResponse
import com.hotbox.terminal.api.loyalty.model.OrderLoyaltyInfo
import com.hotbox.terminal.api.order.model.OrderDetailsResponse
import com.hotbox.terminal.base.network.ErrorType
import com.hotbox.terminal.base.network.model.HotBoxResponse
import io.reactivex.Single
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface LoyaltyRetrofitAPI {

    @GET("v1/loyalty/get-loyalties")
    fun getPhoneLoyaltyData(@Query("user_phone") id: String?): Single<HotBoxResponse<LoyaltyWithPhoneResponse>>

    @GET("v1/pos/get-order-details")
    fun getOrderDetails(@Query("order_id") OrderId: Long): Single<HotBoxResponse<OrderDetailsResponse>>

    @GET("v1/users/get-user")
    @ErrorType
    fun getUserDetails(@Query("id") userId :String?): Single<HotBoxResponse<HealthNutUser>>

    @POST("v1/pos/apply-loyalty-points")
    fun addLoyaltyPoint(@Body AddLoyaltyPointRequest : AddLoyaltyPointRequest) :Single<HotBoxResponse<AddLoyaltyPointResponse>>

    @GET("v1/pos/get-order-loyalty")
    fun getOrderLoyalty(@Query("order_id") orderId : Long) :Single<HotBoxResponse<OrderLoyaltyInfo>>
}