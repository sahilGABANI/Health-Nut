package com.hotbox.terminal.api.clockinout

import com.hotbox.terminal.api.clockinout.model.ClockInOutHistoryResponse
import com.hotbox.terminal.api.clockinout.model.SubmitTimeRequest
import com.hotbox.terminal.api.clockinout.model.TimeResponse
import com.hotbox.terminal.base.network.HotBoxResponseConverter
import com.hotbox.terminal.base.network.model.HotBoxCommonResponse
import com.hotbox.terminal.helper.formatTo
import com.hotbox.terminal.helper.toDate
import io.reactivex.Single

class ClockInOutRepository(
    private val clockInOutRetrofitAPI: ClockInOutRetrofitAPI
) {

    private val hotBoxResponseConverter: HotBoxResponseConverter = HotBoxResponseConverter()

    fun submitTime(submitTimeRequest: SubmitTimeRequest): Single<HotBoxCommonResponse> {
        return clockInOutRetrofitAPI.submitTime(submitTimeRequest)
            .flatMap { hotBoxResponseConverter.convertCommonResponse(it) }
    }

    fun getCurrentTime(userId: Int): Single<TimeResponse> {
        return clockInOutRetrofitAPI.getCurrentTime(userId)
            .flatMap { hotBoxResponseConverter.convertToSingle(it) }
    }

    fun clockInOutData(userId: Int, month: Int, year: Int): Single<ClockInOutHistoryResponse> {
        val monthYear = "${month}-${year}".toDate("MM-yyyy")?.formatTo("MM-yyyy").toString()
        return clockInOutRetrofitAPI.getClockInOutTime(userId, monthYear)
            .flatMap { hotBoxResponseConverter.convertToSingle(it) }
    }
}