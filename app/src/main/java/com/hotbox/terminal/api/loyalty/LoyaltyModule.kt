package com.hotbox.terminal.api.loyalty

import com.hotbox.terminal.api.authentication.LoggedInUserCache
import dagger.Module
import dagger.Provides
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
class LoyaltyModule {

    @Provides
    @Singleton
    fun provideLoyaltyRetrofitAPI(retrofit: Retrofit): LoyaltyRetrofitAPI {
        return retrofit.create(LoyaltyRetrofitAPI::class.java)
    }


    @Provides
    @Singleton
    fun provideLoyaltyRepository(giftCardRetrofitAPI: LoyaltyRetrofitAPI,loggedInUserCache: LoggedInUserCache): LoyaltyRepository {
        return LoyaltyRepository(giftCardRetrofitAPI,loggedInUserCache)
    }
}