package com.hotbox.terminal.api.checkout

import dagger.Module
import dagger.Provides
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
class CheckOutModule {

    @Provides
    @Singleton
    fun provideCheckOutRetrofitAPI(retrofit: Retrofit): CheckOutRetrofitAPI {
        return retrofit.create(CheckOutRetrofitAPI::class.java)
    }


    @Provides
    @Singleton
    fun provideClockInOutRepository(checkOutRetrofitAPI: CheckOutRetrofitAPI): CheckOutRepository {
        return CheckOutRepository(checkOutRetrofitAPI)
    }
}