package com.hotbox.terminal.api.store

import com.hotbox.terminal.api.authentication.model.AvailableToPrintInfo
import com.hotbox.terminal.api.authentication.model.AvailableToPrintRequest
import com.hotbox.terminal.api.authentication.model.LoginCrewResponse
import com.hotbox.terminal.api.store.model.*
import com.hotbox.terminal.base.network.ErrorType
import com.hotbox.terminal.base.network.model.HotBoxResponse
import com.hotbox.terminal.base.network.model.HotBoxResponses
import io.reactivex.Single
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface StoreRetrofitAPI {
    @GET("v1/pos/get-location")
    @ErrorType
    fun getLocationById(@Query("location_id") locationId: Int): Single<HotBoxResponse<StoreResponse>>

    @GET("v1/admin/employees/get-employees")
    @ErrorType
    fun getEmployee(): Single<HotBoxResponse<EmployeeInfo>>

    @GET("v1/pos/get-buffer-time")
    @ErrorType
    fun getBufferTimes(@Query("location_id") locationId: Int): Single<HotBoxResponse<BufferResponse>>

    @POST("v1/pos/update-buffer-time")
    @ErrorType
    fun getUpdatedBufferTimes(@Body bufferTimeRequest: BufferTimeRequest): Single<HotBoxResponse<BufferResponse>>

    @POST("v1/pos/available-to-print")
    @ErrorType
    fun unavailableToPrint(@Body request: AvailableToPrintRequest): Single<HotBoxResponses>

    @POST("v1/pos/update-print-status")
    @ErrorType
    fun updatePrintStatus(@Body request: UpdatePrintStatusRequest): Single<HotBoxResponses>

}