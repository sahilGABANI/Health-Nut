package com.hotbox.terminal.api.stripe

import com.hotbox.terminal.api.authentication.LoggedInUserCache
import dagger.Module
import dagger.Provides
import retrofit2.Retrofit
import javax.inject.Named
import javax.inject.Singleton

@Module
class StripeAPIModule {
    @Provides
    @Singleton
    fun provideStripeRetrofitAPI(@Named("StripeRetrofit") retrofit: Retrofit): StripeRetrofitAPI {
        return retrofit.create(StripeRetrofitAPI::class.java)
    }

    @Provides
    @Singleton
    fun providesStripeRepository(stripeRetrofitAPI: StripeRetrofitAPI,loggedInUserCache: LoggedInUserCache) : StripeRepository {
        return StripeRepository(stripeRetrofitAPI,loggedInUserCache)
    }
}