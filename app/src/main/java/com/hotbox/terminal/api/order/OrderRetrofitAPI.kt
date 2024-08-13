package com.hotbox.terminal.api.order

import com.hotbox.terminal.api.authentication.model.HealthNutUser
import com.hotbox.terminal.api.authentication.model.HotBoxUser
import com.hotbox.terminal.api.order.model.*
import com.hotbox.terminal.base.network.model.HotBoxCommonResponse
import com.hotbox.terminal.base.network.model.HotBoxResponse
import com.hotbox.terminal.base.network.model.HotBoxResponses
import io.reactivex.Single
import retrofit2.http.*

interface OrderRetrofitAPI {

//    @GET("v1/get-all-orders")
//    fun getAllOrder(@Query("location_id") locationId: Int, @Query("order_date") orderDate: String,@Query("order_type") orderType :String,@Query("order_status") orderStatus :String? = null): Single<HotBoxResponse<OrderResponse>>

    @GET("v1/pos/get-pos-orders")
    fun getAllOrder(@Query("location_id") locationId: Int,@Query("order_type") ordersType :String? = null,@Query("order_status") orderStatus :String? = null): Single<HotBoxResponse<List<OrdersInfo>>>

    @GET("v1/pos/get-order-details")
    fun getOrderDetails(@Query("order_id") OrderId :Int): Single<HotBoxResponse<OrderDetailsResponse>>


    @GET("v1/admin/orders/refund")
    fun refundPayment(@Query("id") OrderId :Int,@Query("amount") amount :Int): Single<HotBoxResponses>

    @GET("v1/get-orders-status/{order_id}")
    fun getOrderStatusDetails(@Path("order_id") OrderId :Int): Single<HotBoxResponse<StatusLogInfo>>

    @GET("v1/get-cart/{cart_group_id}")
    fun getCartGroupDetail(@Path("cart_group_id") cartGroupId :Int): Single<HotBoxResponse<StatusLogInfo>>

    @GET("v1/get-user/{user_id}")
    fun getUserDetails(@Path("user_id") userId :Int): Single<HotBoxResponse<HotBoxUser>>

    @POST("v1/pos/update-order-status")
    fun updateOrderStatus(@Body request: OrderStatusRequest) :Single<HotBoxResponse<UpdatedOrderStatusResponse>>

    @GET("v1/pos/send-receipt")
    fun sendReceipt(@Query("order_id") orderId :Int?,@Query("type")type :String,@Query("email") email :String? =null,@Query("phone") phone :String? =null) :Single<HotBoxResponse<HealthNutUser>>

    @GET("v1/pos/get-print-queue")
    fun getPrintQueue(@Query("serial_number") serialNumber :String): Single<HotBoxResponse<List<GetPrintQueueItem>>>
}