package com.hotbox.terminal.api.stripe

import com.hotbox.terminal.api.authentication.LoggedInUserCache
import com.hotbox.terminal.api.stripe.model.CaptureNewPaymentRequest
import com.hotbox.terminal.api.stripe.model.IConnRESTRequest
import com.hotbox.terminal.api.stripe.model.ResponseItem
import io.reactivex.Single

class StripeRepository(private val stripeRetrofitAPI: StripeRetrofitAPI, private val loggedInUserCache: LoggedInUserCache) {

    fun captureNewPayment(newPaymentRequest: CaptureNewPaymentRequest): Single<List<ResponseItem>> {
        val newPayment = newPaymentRequest.copy(
            iConnRESTRequest = IConnRESTRequest(
                posAccessKey = loggedInUserCache.getLocationInfo()?.poskey ,
                terminalAccessKey = loggedInUserCache.getLocationInfo()?.terminalkey
            )
        )
        return stripeRetrofitAPI.captureNewPayment(newPayment).flatMap {
            Single.just(it)
        }
    }
}