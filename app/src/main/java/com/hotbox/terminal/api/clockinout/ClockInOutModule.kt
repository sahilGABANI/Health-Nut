package com.hotbox.terminal.api.clockinout

import dagger.Module
import dagger.Provides
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
class ClockInOutModule {

    @Provides
    @Singleton
    fun provideClockInOutRetrofitAPI(retrofit: Retrofit): ClockInOutRetrofitAPI {
        return retrofit.create(ClockInOutRetrofitAPI::class.java)
    }


    @Provides
    @Singleton
    fun provideClockInOutRepository(clockInOutRetrofitAPI: ClockInOutRetrofitAPI): ClockInOutRepository {
        return ClockInOutRepository(clockInOutRetrofitAPI)
    }
}