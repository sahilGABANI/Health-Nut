package com.hotbox.terminal.api.viewmodelmodule

import com.hotbox.terminal.api.authentication.AuthenticationRepository
import com.hotbox.terminal.api.checkout.CheckOutRepository
import com.hotbox.terminal.api.clockinout.ClockInOutRepository
import com.hotbox.terminal.api.deliveries.DeliveriesRepository
import com.hotbox.terminal.api.giftcard.GiftCardRepository
import com.hotbox.terminal.api.loyalty.LoyaltyRepository
import com.hotbox.terminal.api.menu.MenuRepository
import com.hotbox.terminal.api.order.OrderRepository
import com.hotbox.terminal.api.store.StoreRepository
import com.hotbox.terminal.api.stripe.StripeRepository
import com.hotbox.terminal.api.userstore.UserStoreRepository
import com.hotbox.terminal.ui.login.viewmodel.LoginViewModel
import com.hotbox.terminal.ui.main.deliveries.viewmodel.DeliveriesViewModel
import com.hotbox.terminal.ui.main.giftcard.viewmodel.GiftCardViewModel
import com.hotbox.terminal.ui.main.loyalty.viewmodel.LoyaltyViewModel
import com.hotbox.terminal.ui.main.menu.viewModel.MenuViewModel
import com.hotbox.terminal.ui.main.order.viewmodel.OrderViewModel
import com.hotbox.terminal.ui.main.orderdetail.viewmodel.OrderDetailsViewModel
import com.hotbox.terminal.ui.main.store.viewmodel.StoreViewModel
import com.hotbox.terminal.ui.main.viewmodel.ClockInOutViewModel
import com.hotbox.terminal.ui.splash.viewmodel.LocationViewModel
import com.hotbox.terminal.ui.userstore.checkout.viewmodel.CheckOutViewModel
import com.hotbox.terminal.ui.userstore.viewmodel.UserStoreViewModel
import dagger.Module
import dagger.Provides

@Module
class HotBoxViewModelProvider {

    @Provides
    fun provideLoginViewModel(
        authenticationRepository: AuthenticationRepository
    ): LoginViewModel {
        return LoginViewModel(
            authenticationRepository
        )
    }

    @Provides
    fun provideStartViewModel(
        authenticationRepository: AuthenticationRepository
    ): LocationViewModel {
        return LocationViewModel(
            authenticationRepository
        )
    }

    @Provides
    fun provideClockInOutViewModel(
        clockInOutRepository: ClockInOutRepository,
        storeRepository: StoreRepository
    ): ClockInOutViewModel {
        return ClockInOutViewModel(
            clockInOutRepository,
            storeRepository
        )
    }

    @Provides
    fun provideStoreViewModel(
        storeRepository: StoreRepository,
        orderRepository: OrderRepository
    ): StoreViewModel {
        return StoreViewModel(
            storeRepository,
            orderRepository
        )
    }

    @Provides
    fun provideOrderViewModel(
        orderRepository: OrderRepository
    ): OrderViewModel {
        return OrderViewModel(
            orderRepository
        )
    }

    @Provides
    fun provideOrderDetailsViewModel(
        orderRepository: OrderRepository,
        stripeRepository: StripeRepository
    ): OrderDetailsViewModel {
        return OrderDetailsViewModel(
            orderRepository,
            stripeRepository
        )
    }

    @Provides
    fun provideDeliveriesViewModel(
        deliveriesRepository: DeliveriesRepository
    ): DeliveriesViewModel {
        return DeliveriesViewModel(
            deliveriesRepository
        )
    }

    @Provides
    fun provideUserStoreViewModel(
        userStoreRepository: UserStoreRepository,
        stripeRepository: StripeRepository,
        storeRepository: StoreRepository,
        authenticationRepository : AuthenticationRepository,
        checkOutRepository :CheckOutRepository
    ): UserStoreViewModel {
        return UserStoreViewModel(
            userStoreRepository,
            stripeRepository,
            storeRepository,
            authenticationRepository,
            checkOutRepository
        )
    }

    @Provides
    fun provideMenuViewModel(
        menuRepository: MenuRepository
    ): MenuViewModel {
        return MenuViewModel(
            menuRepository
        )
    }

    @Provides
    fun provideCheckOutViewModel(
        checkOutRepository: CheckOutRepository
    ): CheckOutViewModel {
        return CheckOutViewModel(
            checkOutRepository
        )
    }

    @Provides
    fun provideGiftCardViewModel(
        giftCardRepository: GiftCardRepository,
        stripeRepository: StripeRepository
    ): GiftCardViewModel {
        return GiftCardViewModel(giftCardRepository,stripeRepository)
    }
    @Provides
    fun provideLoyaltyViewModel(
        loyaltyRepository: LoyaltyRepository
    ): LoyaltyViewModel {
        return LoyaltyViewModel(loyaltyRepository)
    }

}