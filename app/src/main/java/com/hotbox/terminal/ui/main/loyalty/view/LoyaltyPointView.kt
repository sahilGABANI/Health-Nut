package com.hotbox.terminal.ui.main.loyalty.view

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import androidx.core.content.ContextCompat
import com.hotbox.terminal.R
import com.hotbox.terminal.api.checkout.model.LoyaltyPhoneHistoryInfo
import com.hotbox.terminal.api.order.model.OrdersInfo
import com.hotbox.terminal.base.ConstraintLayoutWithLifecycle
import com.hotbox.terminal.base.extension.subscribeAndObserveOnMainThread
import com.hotbox.terminal.base.extension.throttleClicks
import com.hotbox.terminal.base.extension.toDollar
import com.hotbox.terminal.databinding.LoyaltyPointViewBinding
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class LoyaltyPointView(context: Context) : ConstraintLayoutWithLifecycle(context) {

    private var binding: LoyaltyPointViewBinding? = null
    private val loyaltyPointStateSubject: PublishSubject<LoyaltyPhoneHistoryInfo> = PublishSubject.create()
    val loyaltyPointActionState: Observable<LoyaltyPhoneHistoryInfo> = loyaltyPointStateSubject.hide()

    init {
        inflateUi()
    }

    private fun inflateUi() {
        val view = View.inflate(context, R.layout.loyalty_point_view, this)
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        binding = LoyaltyPointViewBinding.bind(view)
    }

    @SuppressLint("SetTextI18n")
    fun bind(loyaltyPhoneHistoryInfo: LoyaltyPhoneHistoryInfo) {
        binding?.apply {
            if (loyaltyPhoneHistoryInfo.order?.id != null) {
                productTextview.text = "#${loyaltyPhoneHistoryInfo.order.id}"
            } else {
                productTextview.text = "-"
            }
            if ( loyaltyPhoneHistoryInfo.order?.orderTotal != null) {
                totalTextview.text = loyaltyPhoneHistoryInfo.order.orderTotal.toDouble().div(100).toDollar()
            } else{
                totalTextview.text = "-"
            }

            loyaltyPhoneHistoryInfo.appliedPoints?.let { it1 ->
                if (it1 < 0) {
                    leavesTextview.setTextColor(ContextCompat.getColor(context, R.color.red))
                } else {
                    leavesTextview.setTextColor(ContextCompat.getColor(context, R.color.green_light))
                }
            }

            leavesTextview.text = loyaltyPhoneHistoryInfo.appliedPoints.toString()
            llLoyaltyDetails.throttleClicks().subscribeAndObserveOnMainThread {
                if (loyaltyPhoneHistoryInfo.order?.id != null) {
                    loyaltyPointStateSubject.onNext(loyaltyPhoneHistoryInfo)
                }
            }.autoDispose()
        }
    }

    override fun onDestroy() {
        binding = null
        super.onDestroy()
    }
}