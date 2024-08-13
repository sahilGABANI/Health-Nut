package com.hotbox.terminal.base.network

import com.hotbox.terminal.BuildConfig
import com.hotbox.terminal.api.authentication.LoggedInUserCache
import com.hotbox.terminal.utils.Constants.isDebugMode
import okhttp3.Interceptor
import okhttp3.Response
import timber.log.Timber
import java.io.IOException

class StripeInterceptorHeaders(
    private val loggedInUserCache: LoggedInUserCache
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()

        val requestBuilder = original.newBuilder()
        if (isDebugMode()) {
//            requestBuilder.header("Authorization", "Bearer sk_test_51H7ofqHBh9c4S8JHZ2Uncluam52UeNXDBtsxsqQSSl3lh5n417UgKTIbwz4olMJXHjUZ91SfKMLelfpmpWRheCZC00rvxVj5pa")
        } else {
            requestBuilder.header("Accept", "application/json")
            requestBuilder.header("Content-Type", "text/plain")
        }

//        val token = loggedInUserCache.getLoggedInUserStripeToken() ?: ""
//        requestBuilder.header("Authorization", "Bearer sk_test_51H7ofqHBh9c4S8JHZ2Uncluam52UeNXDBtsxsqQSSl3lh5n417UgKTIbwz4olMJXHjUZ91SfKMLelfpmpWRheCZC00rvxVj5pa")


        val response: Response
        try {
            response = chain.proceed(requestBuilder.build())
        } catch (t: Throwable) {
            Timber.e("error in InterceptorHeaders:\n${t.message}")
            throw IOException(t.message)
        }
        return response
    }
}