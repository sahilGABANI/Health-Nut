package com.hotbox.terminal.api.deliveries

import com.hotbox.terminal.api.order.model.OrderResponse
import com.hotbox.terminal.api.order.model.OrdersInfo
import com.hotbox.terminal.base.network.model.HotBoxResponse
import io.reactivex.Single
import retrofit2.http.GET
import retrofit2.http.Query

interface DeliveriesRetrofitAPI {

    @GET("v1/pos/get-pos-orders")
    fun getDeliveriesOrder(@Query("location_id") locationId: Int,@Query("order_status") orderStatus :String? = null): Single<HotBoxResponse<List<OrdersInfo>>>
}