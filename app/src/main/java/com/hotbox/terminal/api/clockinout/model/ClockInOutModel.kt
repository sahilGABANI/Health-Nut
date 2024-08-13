package com.hotbox.terminal.api.clockinout.model

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName
import com.hotbox.terminal.helper.formatTo
import com.hotbox.terminal.helper.toDate
import timber.log.Timber

@Keep
data class SubmitTimeRequest(
    @field:SerializedName("user_id")
    var user_id: Int,

    @field:SerializedName("action")
    var action: String,
)

@Keep
data class TimeResponse(
    @field:SerializedName("id")
    var id: Int,

    @field:SerializedName("user_id")
    var userId: Int?,

    @field:SerializedName("action")
    var action: String?,

    @field:SerializedName("action_time")
    var actionTime: String?,
) {
    fun getClockType(): ClockType {
        return if (action == ClockType.ClockOut.type) ClockType.ClockOut else ClockType.ClockIn
    }

    fun getActionFormattedTime(dateFormat: String = "hh:mm a"): String {
        return try {
            actionTime?.toDate()?.formatTo(dateFormat) ?: ""
        } catch (e: Exception) {
            Timber.e(e)
            ""
        }
    }

    fun isInitClockTime(): Boolean {
        return id == 0
    }
}

@Keep
data class ClockInOutHistoryResponse(
    @field:SerializedName("times")
    val listOfTimeResponse: List<TimeResponse>? = null
)

@Keep
enum class ClockType(val type: String, val displayType: String) {
    ClockIn("clock-in", "Clock in"),
    ClockOut("clock-out", "Clock out")
}