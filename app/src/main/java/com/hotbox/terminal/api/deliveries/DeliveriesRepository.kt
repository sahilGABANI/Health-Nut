package com.hotbox.terminal.api.deliveries

import com.hotbox.terminal.api.authentication.LoggedInUserCache
import com.hotbox.terminal.api.order.model.OrderResponse
import com.hotbox.terminal.api.order.model.OrdersInfo
import com.hotbox.terminal.base.network.HotBoxResponseConverter
import io.reactivex.Single

class DeliveriesRepository(
    private val deliveriesRetrofitAPI: DeliveriesRetrofitAPI,
    private val loggedInUserCache: LoggedInUserCache
) {
    private val hotBoxResponseConverter: HotBoxResponseConverter = HotBoxResponseConverter()

    fun getDeliveriesOrderData(currentDate: String, orderType: String,orderStatus :String?): Single<List<OrdersInfo>> {
        val locationId =
            loggedInUserCache.getLocationInfo()?.location?.id ?: throw Exception("location not found")
        return deliveriesRetrofitAPI.getDeliveriesOrder(locationId,orderStatus).flatMap { hotBoxResponseConverter.convertToSingle(it) }
    }
}