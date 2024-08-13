package com.hotbox.terminal.base.network

import android.content.Context
import com.google.gson.GsonBuilder
import com.hotbox.terminal.BuildConfig
import com.hotbox.terminal.api.authentication.LoggedInUserCache
import com.hotbox.terminal.utils.Constants.isDebugMode
import dagger.Module
import dagger.Provides
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton


@Module
class StripeNetworkModule {

    @Provides
    @Singleton
    fun provideStripeInterceptorHeaders(loggedInUserCache: LoggedInUserCache): StripeInterceptorHeaders {
        return StripeInterceptorHeaders(loggedInUserCache)
    }

    @Provides
    @Singleton
    @Named("StripeOkHttpClient")
    fun provideStripeOkHttpClient(
        context: Context,
        stripeInterceptorHeaders: StripeInterceptorHeaders
    ): OkHttpClient {
        val cacheSize = 10 * 1024 * 1024 // 10 MiB
        val cacheDir = File(context.cacheDir, "HttpCache")
        val cache = Cache(cacheDir, cacheSize.toLong())
        val builder = OkHttpClient.Builder()
            .readTimeout(120, TimeUnit.SECONDS)
            .connectTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(240, TimeUnit.SECONDS)
            .cache(cache)
            .addInterceptor(stripeInterceptorHeaders)
        if (isDebugMode()) {
            val loggingInterceptor = HttpLoggingInterceptor()
            loggingInterceptor.level = HttpLoggingInterceptor.Level.BODY
            builder.addInterceptor(loggingInterceptor)
        }
        return builder.build()
    }

    @Provides
    @Singleton
    @Named("StripeRetrofit")
    fun provideRetrofit(@Named("StripeOkHttpClient") okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://newstripeterminal.herokuapp.com/")
            .client(okHttpClient)
            .addCallAdapterFactory(RxJava2CallAdapterFactory.createAsync()) // Using create async means all api calls are automatically created asynchronously using OkHttp's thread pool
            .addConverterFactory(
                ScalarsConverterFactory.create()
            )
            .build()
    }
}