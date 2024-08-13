package com.hotbox.terminal.base.network.model

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

@Keep
data class HotBoxResponse<T>(
    @field:SerializedName("status")
    val status: Int? = null,

    @field:SerializedName("data")
    val data: T? = null,

    @field:SerializedName("message")
    val message: String? = null,
)


@Keep
data class HotBoxResponses(
    @field:SerializedName("status")
    val status: Int? = null,



    @field:SerializedName("message")
    val message: String? = null,
)

@Keep
data class HotBoxCommonResponse(
    @field:SerializedName("success")
    val success: Boolean = false,

    @field:SerializedName("message")
    val message: String? = null,

    @field:SerializedName("data")
    val data: String? = null
)

@Keep
data class HotBoxError(
    @field:SerializedName("status")
    val status: Int? = null,

    @field:SerializedName("message")
    val message: String? = null,

    @field:SerializedName("code")
    val code: String? = null,
) {
    val safeErrorMessage: String
        get() = message ?: "Something went wrong. Please try after sometime"
}

sealed class ErrorResult<out T> {
    data class ErrorMessage<T>(val errorResponse: T) : ErrorResult<T>()
    data class ErrorThrowable(val throwable: Throwable) : ErrorResult<Nothing>()
}