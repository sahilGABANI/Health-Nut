package com.hotbox.terminal.api.store

import com.hotbox.terminal.api.authentication.LoggedInUserCache
import com.hotbox.terminal.api.authentication.model.AvailableToPrintInfo
import com.hotbox.terminal.api.authentication.model.AvailableToPrintRequest
import com.hotbox.terminal.api.authentication.model.LoginCrewResponse
import com.hotbox.terminal.api.store.model.*
import com.hotbox.terminal.base.network.HotBoxResponseConverter
import com.hotbox.terminal.base.network.model.HotBoxResponses
import io.reactivex.Single

class StoreRepository(
    private val storeRetrofitAPI: StoreRetrofitAPI, private val loggedInUserCache: LoggedInUserCache
) {
    private val hotBoxResponseConverter: HotBoxResponseConverter = HotBoxResponseConverter()

    fun getCurrentStoreInformation(): Single<StoreResponse> {
        val locationId = loggedInUserCache.getLocationInfo()?.location?.id ?: throw Exception("location not found")
        return storeRetrofitAPI.getLocationById(locationId).flatMap { hotBoxResponseConverter.convert(it) }

    }

    fun getEmployee(): Single<EmployeeInfo> {
        return storeRetrofitAPI.getEmployee().flatMap { hotBoxResponseConverter.convert(it) }
//            .flatMap {
//            it.roles?.forEach {
//                if (it.location?.id == 1 && it.role?.id == 11){
//                    loggedInUserCache.setAutoReceiverId(it.user.id)
//                }
//            }
//        }

    }

    fun getCurrentStoreLocation(): Single<String> {
//        return getCurrentStoreInformation().map {
//            it.getSafeAddressName()
//        }
        return Single.just("")
    }

    fun getBufferInformation(): Single<BufferResponse> {
        val locationId = loggedInUserCache.getLocationInfo()?.location?.id ?: throw Exception("location not found")
        return storeRetrofitAPI.getBufferTimes(locationId).flatMap { hotBoxResponseConverter.convert(it) }
    }

    fun getUpdatedBufferTimes(request: BufferTimeRequest): Single<BufferResponse> {
        return storeRetrofitAPI.getUpdatedBufferTimes(request).flatMap { hotBoxResponseConverter.convert(it) }
    }

    fun updateBufferTimeForPickUpOrDelivery(
        isBufferTimePlush: Boolean, isPickUpBufferTime: Boolean
    ): Single<BufferResponse> {
        val locationId = loggedInUserCache.getLocationInfo()?.location?.id ?: throw Exception("location not found")
        val operator = if (isBufferTimePlush) {
            BUFFER_TIME_PLUSH
        } else {
            BUFFER_TIME_MINUS
        }
        val type = if (isPickUpBufferTime) {
            PICKUP_BUFFER_TYPE
        } else {
            DELIVERY_BUFFER_TYPE
        }
        return storeRetrofitAPI.getUpdatedBufferTimes(
            BufferTimeRequest(
                locationId, type, operator
            )
        ).flatMap { hotBoxResponseConverter.convert(it) }
    }


    fun unavailableToPrint(request: AvailableToPrintRequest): Single<HotBoxResponses> {
        return storeRetrofitAPI.unavailableToPrint(request)

    }
    fun updatePrintStatus(request: UpdatePrintStatusRequest): Single<HotBoxResponses> {
        return storeRetrofitAPI.updatePrintStatus(request)

    }
}