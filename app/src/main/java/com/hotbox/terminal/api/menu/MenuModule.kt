package com.hotbox.terminal.api.menu

import com.hotbox.terminal.api.authentication.LoggedInUserCache
import dagger.Module
import dagger.Provides
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
class MenuModule {

    @Provides
    @Singleton
    fun provideMenuRetrofitAPI(retrofit: Retrofit): MenuRetrofitAPI {
        return retrofit.create(MenuRetrofitAPI::class.java)
    }

    @Provides
    @Singleton
    fun provideMenuRepository(
        menuRetrofitAPI: MenuRetrofitAPI,
        loggedInUserCache: LoggedInUserCache
    ): MenuRepository {
        return MenuRepository(menuRetrofitAPI,loggedInUserCache)
    }
}