package com.hotbox.terminal.api.deliveries

import com.hotbox.terminal.api.authentication.LoggedInUserCache
import dagger.Module
import dagger.Provides
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
class DeliveriesModule {

    @Provides
    @Singleton
    fun provideDeliveriesRetrofitAPI(retrofit: Retrofit): DeliveriesRetrofitAPI {
        return retrofit.create(DeliveriesRetrofitAPI::class.java)
    }

    @Provides
    @Singleton
    fun provideDeliveriesRepository(
        deliveriesRetrofitAPI: DeliveriesRetrofitAPI,
        loggedInUserCache: LoggedInUserCache
    ): DeliveriesRepository {
        return DeliveriesRepository(deliveriesRetrofitAPI, loggedInUserCache)
    }
}