package com.hotbox.terminal.base.network

import com.hotbox.terminal.api.authentication.LoggedInUserCache
import okhttp3.Interceptor
import okhttp3.Response
import timber.log.Timber
import java.io.IOException

class HotboxInterceptorHeaders(
    private val loggedInUserCache: LoggedInUserCache
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()

        val requestBuilder = original.newBuilder()
        requestBuilder.header("Content-Type", "application/json")
        requestBuilder.header("Accept", "application/json")
        requestBuilder.header("api-key", "4ffdbf01-18a0-455b-8b41-14f1f1ab4c95")
        val token = loggedInUserCache.getLoginUserToken() ?: ""
        if (token.isNotEmpty()) {
            // authenticated user
            requestBuilder.header("Authorization", "Bearer $token")
        }

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