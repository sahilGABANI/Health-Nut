package com.hotbox.terminal.api.order

import com.hotbox.terminal.api.authentication.LoggedInUserCache
import com.hotbox.terminal.api.authentication.model.HealthNutUser
import com.hotbox.terminal.api.authentication.model.HotBoxUser
import com.hotbox.terminal.api.order.model.*
import com.hotbox.terminal.base.network.HotBoxResponseConverter
import com.hotbox.terminal.base.network.model.HotBoxResponse
import com.hotbox.terminal.base.network.model.HotBoxResponses
import com.hotbox.terminal.utils.Constants.DEFAULT_AUTO_RECEIVED_USER_ID
import com.hotbox.terminal.utils.Constants.ORDER_STATUS_CANCELLED
import io.reactivex.Single

class OrderRepository(
    private val orderRetrofitAPI: OrderRetrofitAPI, private val loggedInUserCache: LoggedInUserCache
) {
    private val hotBoxResponseConverter: HotBoxResponseConverter = HotBoxResponseConverter()

    fun getOrderData(currentDate: String, orderType: String?, orderStatus: String?): Single<List<OrdersInfo>> {
        val locationId = loggedInUserCache.getLocationInfo()?.location?.id ?: throw Exception("location not found")
        return orderRetrofitAPI.getAllOrder(locationId, orderType, orderStatus).flatMap { hotBoxResponseConverter.convertToSingle(it) }
    }

    fun getOrderDetailsData(OrderId: Int): Single<OrderDetailsResponse> {
        return orderRetrofitAPI.getOrderDetails(OrderId).flatMap { hotBoxResponseConverter.convertToSingle(it) }
    }

    fun refundPayment(OrderId: Int,amount :Int): Single<HotBoxResponses> {
        return orderRetrofitAPI.refundPayment(OrderId,amount)
    }

    fun getStatusLogData(OrderId: Int): Single<StatusLogInfo> {
        val statusList = arrayListOf<StatusItem>()
        statusList.clear()
        return Single.just(StatusLogInfo(statusList))
    }

    fun getCartGroupDetail(cartGroupId: Int): Single<StatusLogInfo> {
        return orderRetrofitAPI.getCartGroupDetail(cartGroupId).flatMap { hotBoxResponseConverter.convertToSingle(it) }
    }

    fun getUserDetails(userId: Int): Single<HotBoxUser> {
        return orderRetrofitAPI.getUserDetails(userId).flatMap { hotBoxResponseConverter.convertToSingle(it) }
    }

    fun updateOrderStatus(orderStatus: String, orderId: Int): Single<UpdatedOrderStatusResponse> {
        val userId = loggedInUserCache.getLoggedInUserId() ?: throw IllegalStateException("User it not login")
        val autoReceiverId = loggedInUserCache.getAutoReceiverId()
        return if (orderStatus == ORDER_STATUS_CANCELLED) {
            orderRetrofitAPI.updateOrderStatus(
                OrderStatusRequest(orderStatus = orderStatus, orderId = orderId, userId = userId)
            ).flatMap { hotBoxResponseConverter.convertToSingle(it) }
        } else {
            val autoReceiverUserId = if (!autoReceiverId.isNullOrEmpty()) autoReceiverId else DEFAULT_AUTO_RECEIVED_USER_ID
            orderRetrofitAPI.updateOrderStatus(
                OrderStatusRequest(orderStatus, autoReceiverUserId.toString(), orderId)
            ).flatMap { hotBoxResponseConverter.convertToSingle(it) }

        }
    }

    fun sendReceipt(orderId :Int,type :String,email :String?,phone :String?): Single<HotBoxResponse<HealthNutUser>> {
        return orderRetrofitAPI.sendReceipt(orderId,type, email, phone).flatMap {
            hotBoxResponseConverter.convertToSingleWithFullResponse(it)
        }
    }
    fun getPrintQueue(serialNumber :String): Single<List<GetPrintQueueItem>> {
        return orderRetrofitAPI.getPrintQueue(serialNumber).flatMap {
            hotBoxResponseConverter.convertToSingle(it)
        }
    }
}