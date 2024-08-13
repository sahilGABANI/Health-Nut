package com.hotbox.terminal.api.store

import com.hotbox.terminal.api.authentication.LoggedInUserCache
import dagger.Module
import dagger.Provides
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
class StoreModule {

    @Provides
    @Singleton
    fun provideStoreRetrofitAPI(retrofit: Retrofit): StoreRetrofitAPI {
        return retrofit.create(StoreRetrofitAPI::class.java)
    }


    @Provides
    @Singleton
    fun provideStoreRepository(
        storeRetrofitAPI: StoreRetrofitAPI,
        loggedInUserCache: LoggedInUserCache
    ): StoreRepository {
        return StoreRepository(storeRetrofitAPI, loggedInUserCache)
    }
}