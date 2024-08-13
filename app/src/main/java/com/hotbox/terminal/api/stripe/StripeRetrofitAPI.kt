package com.hotbox.terminal.api.stripe

import com.hotbox.terminal.api.stripe.model.CaptureNewPaymentRequest
import com.hotbox.terminal.api.stripe.model.ResponseItem
import io.reactivex.Single
import retrofit2.http.*

interface StripeRetrofitAPI {


    // production :- https://veritas.rest.iconncloud.net/tsi/v1/payment
    // dev :- https://veritas.rest.uat.iconncloud.net/tsi/v1/payment
    @POST("https://veritas.rest.uat.iconncloud.net/tsi/v1/payment")
    fun captureNewPayment(@Body newPaymentRequest : CaptureNewPaymentRequest) : Single<List<ResponseItem>>

}