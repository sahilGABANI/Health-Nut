package com.hotbox.terminal.ui.userstore.view

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.hotbox.terminal.ui.userstore.checkout.CheckOutFragment
import com.hotbox.terminal.ui.userstore.cookies.CookiesFragment
import com.hotbox.terminal.ui.userstore.cookies.RedeemProductFragment
import com.hotbox.terminal.ui.userstore.payment.PaymentFragment

class SideNavigationAdapterUserStore(fragmentActivity: FragmentActivity) :
    FragmentStateAdapter(fragmentActivity) {
    override fun getItemCount() = 3

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> {
                CookiesFragment.newInstance()
            }
            1 -> {
                CheckOutFragment.newInstance()
            }
            2 -> {
                PaymentFragment.newInstance()
            }
            else -> {
                CookiesFragment.newInstance()
            }
        }
    }

}