package com.hotbox.terminal.ui.main.view

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.hotbox.terminal.ui.main.deliveries.DeliveriesFragment
import com.hotbox.terminal.ui.main.giftcard.GiftCardFragment
import com.hotbox.terminal.ui.main.loyalty.LoyaltyFragment
import com.hotbox.terminal.ui.main.menu.MenuFragment
import com.hotbox.terminal.ui.main.order.OrdersFragment
import com.hotbox.terminal.ui.main.settings.SettingsFragment
import com.hotbox.terminal.ui.main.store.StoreFragment
import com.hotbox.terminal.ui.main.timemanagement.TimeManagementFragment

class SideNavigationAdapter(fragmentActivity: FragmentActivity) :
    FragmentStateAdapter(fragmentActivity) {
    override fun getItemCount() = 7

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> {
                OrdersFragment.newInstance()
            }
            1 -> {
                DeliveriesFragment.newInstance()
            }
            2 -> {
                MenuFragment.newInstance()
            }
            3-> {
                StoreFragment.newInstance()
            }
            4-> {
                LoyaltyFragment.newInstance()
            }
            5 -> {
                GiftCardFragment.newInstance()
            }
            6 -> {
                SettingsFragment.newInstance()
            }
            else -> {
                OrdersFragment.newInstance()
            }
        }
    }
}