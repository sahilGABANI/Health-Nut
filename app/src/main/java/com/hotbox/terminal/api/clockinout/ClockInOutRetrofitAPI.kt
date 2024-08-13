package com.hotbox.terminal.api.clockinout

import com.hotbox.terminal.api.clockinout.model.ClockInOutHistoryResponse
import com.hotbox.terminal.api.clockinout.model.SubmitTimeRequest
import com.hotbox.terminal.api.clockinout.model.TimeResponse
import com.hotbox.terminal.base.network.ErrorType
import com.hotbox.terminal.base.network.model.HotBoxCommonResponse
import com.hotbox.terminal.base.network.model.HotBoxResponse
import io.reactivex.Single
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface ClockInOutRetrofitAPI {

    @POST("v1/submit-time")
    @ErrorType
    fun submitTime(@Body submitTimeRequest: SubmitTimeRequest): Single<HotBoxCommonResponse>


    @GET("v1/get-current-time/{user_id}")
    @ErrorType
    fun getCurrentTime(@Path("user_id") userId: Int): Single<HotBoxResponse<TimeResponse>>

    @GET("v1/get-time-history/{user_id}/{month}")
    @ErrorType
    fun getClockInOutTime(
        @Path("user_id") userId: Int,
        @Path("month") monthYear: String
    ): Single<HotBoxResponse<ClockInOutHistoryResponse>>
}