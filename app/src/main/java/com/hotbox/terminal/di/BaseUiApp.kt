package com.hotbox.terminal.di

import android.app.Application
import com.hotbox.terminal.ui.userstore.guest.GuestProductDetailsDialogFragment
import com.hotbox.terminal.ui.login.LoginActivity
import com.hotbox.terminal.ui.main.CheckInDialogFragment
import com.hotbox.terminal.ui.main.MainActivity
import com.hotbox.terminal.ui.main.deliveries.DeliveriesFragment
import com.hotbox.terminal.ui.main.deliveries.DeliveriesOrderDetailsFragment
import com.hotbox.terminal.ui.main.giftcard.GiftCardFragment
import com.hotbox.terminal.ui.main.loyalty.LoyaltyFragment
import com.hotbox.terminal.ui.main.loyalty.OrderDetailsDialog
import com.hotbox.terminal.ui.main.menu.MenuFragment
import com.hotbox.terminal.ui.main.menu.view.MenuDetailItemView
import com.hotbox.terminal.ui.main.order.OrdersFragment
import com.hotbox.terminal.ui.main.orderdetail.OrderDetailsFragment
import com.hotbox.terminal.ui.main.orderdetail.PrintReceiptDialog
import com.hotbox.terminal.ui.main.orderdetail.RefundFragmentDialog
import com.hotbox.terminal.ui.main.settings.SettingsFragment
import com.hotbox.terminal.ui.main.store.StoreFragment
import com.hotbox.terminal.ui.main.timemanagement.TimeManagementFragment
import com.hotbox.terminal.ui.splash.SplashActivity
import com.hotbox.terminal.ui.userstore.*
import com.hotbox.terminal.ui.userstore.checkout.CheckOutFragment
import com.hotbox.terminal.ui.userstore.cookies.CookiesFragment
import com.hotbox.terminal.ui.userstore.cookies.RedeemProductFragment
import com.hotbox.terminal.ui.userstore.cookies.view.UserStoreProductView
import com.hotbox.terminal.ui.userstore.customize.CustomizeOrderActivity
import com.hotbox.terminal.ui.userstore.editcart.EditCartActivity
import com.hotbox.terminal.ui.userstore.editcart.EditCartFragment
import com.hotbox.terminal.ui.userstore.guest.TakeNBackDialogFragment
import com.hotbox.terminal.ui.userstore.loyaltycard.JoinLoyaltyProgramDialog
import com.hotbox.terminal.ui.userstore.loyaltycard.LoyaltyCardFragment
import com.hotbox.terminal.ui.userstore.payment.BohPrintBottomSheetFragment
import com.hotbox.terminal.ui.userstore.payment.PaymentFragment
import com.hotbox.terminal.ui.userstore.view.CartItemView
import com.hotbox.terminal.ui.wifi.AvailableWifiActivity
import com.hotbox.terminal.ui.wifi.NoWifiActivity
import com.hotbox.terminal.ui.wifi.SelectWifiActivity

abstract class BaseUiApp : Application() {

    abstract fun setAppComponent(baseAppComponent: BaseAppComponent)
    abstract fun getAppComponent(): BaseAppComponent
}

interface BaseAppComponent {
    fun inject(app: Application)
    fun inject(loginActivity: LoginActivity)
    fun inject(splashActivity: SplashActivity)
    fun inject(noWifiActivity: NoWifiActivity)
    fun inject(availableWifiActivity: AvailableWifiActivity)
    fun inject(selectWifiActivity: SelectWifiActivity)
    fun inject(checkInDialogFragment: CheckInDialogFragment)
    fun inject(mainActivity: MainActivity)
    fun inject(timeManagementFragment: TimeManagementFragment)
    fun inject(storeFragment: StoreFragment)
    fun inject(ordersFragment: OrdersFragment)
    fun inject(orderDetailsFragment: OrderDetailsFragment)
    fun inject(deliveriesFragment: DeliveriesFragment)
    fun inject(deliveriesOrderDetailsFragment: DeliveriesOrderDetailsFragment)
    fun inject(userStoreActivity: UserStoreActivity)
    fun inject(cookiesFragment: CookiesFragment)
    fun inject(menuFragment: MenuFragment)
    fun inject(guestProductDetailsDialogFragment: GuestProductDetailsDialogFragment)
    fun inject(takeNBackDialogFragment: TakeNBackDialogFragment)
    fun inject(paymentFragment: PaymentFragment)
    fun inject(checkOutFragment: CheckOutFragment)
    fun inject(joinLoyaltyProgramDialog: JoinLoyaltyProgramDialog)
    fun inject(giftCardFragment: GiftCardFragment)
    fun inject(addToCartDialogFragment: AddToCartDialogFragment)
    fun inject(customizeOrderActivity: CustomizeOrderActivity)
    fun inject(userStoreWelcomeActivity: UserStoreWelcomeActivity)
    fun inject(editCartActivity: EditCartActivity)
    fun inject(redeemProductFragment: RedeemProductFragment)
    fun inject(selectEmployeeActivity: SelectEmployeeActivity)
    fun inject(printReceiptDialog: PrintReceiptDialog)
    fun inject(cartItemView: CartItemView)
    fun inject(adminPinDialogFragment: AdminPinDialogFragment)
    fun inject(compAndAdjustmentActivity: CompAndAdjustmentActivity)
    fun inject(userStoreProductView: UserStoreProductView)
    fun inject(loyaltyCardFragment: LoyaltyCardFragment)
    fun inject(menuDetailItemView: MenuDetailItemView)
    fun inject(orderTypeBottomSheet: OrderTypeBottomSheet)
    fun inject(editCartFragment: EditCartFragment)
    fun inject(refundFragmentDialog: RefundFragmentDialog)
    fun inject(loyaltyFragment: LoyaltyFragment)
    fun inject(orderDetailsDialog: OrderDetailsDialog)
    fun inject(bohPrintBottomSheetFragment: BohPrintBottomSheetFragment)
    fun inject(settingsFragment: SettingsFragment)
}

fun BaseUiApp.getComponent(): BaseAppComponent {
    return this.getAppComponent()
}