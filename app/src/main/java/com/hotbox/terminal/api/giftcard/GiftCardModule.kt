package com.hotbox.terminal.api.giftcard

import com.hotbox.terminal.api.authentication.LoggedInUserCache
import com.hotbox.terminal.api.checkout.CheckOutRepository
import com.hotbox.terminal.api.checkout.CheckOutRetrofitAPI
import dagger.Module
import dagger.Provides
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
class GiftCardModule {

    @Provides
    @Singleton
    fun provideGiftCardRetrofitAPI(retrofit: Retrofit): GiftCardRetrofitAPI {
        return retrofit.create(GiftCardRetrofitAPI::class.java)
    }


    @Provides
    @Singleton
    fun provideGiftCardRepository(giftCardRetrofitAPI: GiftCardRetrofitAPI,loggedInUserCache: LoggedInUserCache): GiftCardRepository {
        return GiftCardRepository(giftCardRetrofitAPI,loggedInUserCache)
    }
}