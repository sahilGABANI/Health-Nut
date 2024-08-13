package com.hotbox.terminal.di

import android.app.Application
import android.content.Context
import com.hotbox.terminal.api.authentication.AuthenticationModule
import com.hotbox.terminal.api.checkout.CheckOutModule
import com.hotbox.terminal.api.clockinout.ClockInOutModule
import com.hotbox.terminal.api.deliveries.DeliveriesModule
import com.hotbox.terminal.api.giftcard.GiftCardModule
import com.hotbox.terminal.api.loyalty.LoyaltyModule
import com.hotbox.terminal.api.menu.MenuModule
import com.hotbox.terminal.api.order.OrderModule
import com.hotbox.terminal.api.store.StoreModule
import com.hotbox.terminal.api.stripe.StripeAPIModule
import com.hotbox.terminal.api.userstore.UserStoreModule
import com.hotbox.terminal.api.viewmodelmodule.HotBoxViewModelProvider
import com.hotbox.terminal.application.HotBox
import com.hotbox.terminal.base.network.NetworkModule
import com.hotbox.terminal.base.network.StripeNetworkModule
import com.hotbox.terminal.base.prefs.PrefsModule
import dagger.Component
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
class HotboxAppModule(val app: Application) {

    @Provides
    @Singleton
    fun provideApplication(): Application {
        return app
    }

    @Provides
    @Singleton
    fun provideContext(): Context {
        return app
    }
}

@Singleton
@Component(
    modules = [
        HotboxAppModule::class,
        NetworkModule::class,
        AuthenticationModule::class,
        PrefsModule::class,
        OrderModule::class,
        HotBoxViewModelProvider::class,
        ClockInOutModule::class,
        StoreModule::class,
        DeliveriesModule::class,
        UserStoreModule::class,
        MenuModule::class,
        CheckOutModule::class,
        StripeNetworkModule::class,
        StripeAPIModule::class,
        GiftCardModule::class,
        LoyaltyModule::class
    ]
)

interface HotBoxAppComponent : BaseAppComponent {
    fun inject(app: HotBox)
}
