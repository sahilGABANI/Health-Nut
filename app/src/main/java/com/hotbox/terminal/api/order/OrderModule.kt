package com.hotbox.terminal.api.order

import com.hotbox.terminal.api.authentication.LoggedInUserCache
import dagger.Module
import dagger.Provides
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
class OrderModule {

    @Provides
    @Singleton
    fun provideOrderRetrofitAPI(retrofit: Retrofit): OrderRetrofitAPI {
        return retrofit.create(OrderRetrofitAPI::class.java)
    }

    @Provides
    @Singleton
    fun provideOrderRepository(
        orderRetrofitAPI: OrderRetrofitAPI,
        loggedInUserCache: LoggedInUserCache
    ): OrderRepository {
        return OrderRepository(orderRetrofitAPI, loggedInUserCache)
    }
}