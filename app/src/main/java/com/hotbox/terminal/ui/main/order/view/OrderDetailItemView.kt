package com.hotbox.terminal.ui.main.order.view

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import androidx.core.content.ContextCompat
import com.hotbox.terminal.R
import com.hotbox.terminal.api.order.model.OrdersInfo
import com.hotbox.terminal.base.ConstraintLayoutWithLifecycle
import com.hotbox.terminal.base.extension.subscribeAndObserveOnMainThread
import com.hotbox.terminal.base.extension.throttleClicks
import com.hotbox.terminal.base.extension.toDollar
import com.hotbox.terminal.databinding.OrderDetailItemBinding
import com.hotbox.terminal.helper.formatTo
import com.hotbox.terminal.helper.toDate
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class OrderDetailItemView(context: Context) : ConstraintLayoutWithLifecycle(context) {

    private var orderTotal: Double? = 0.00
    private var binding: OrderDetailItemBinding? = null
    private lateinit var orderinfo: OrdersInfo

    init {
        inflateUi()
    }

    private val orderStateSubject: PublishSubject<OrdersInfo> = PublishSubject.create()
    val orderActionState: Observable<OrdersInfo> = orderStateSubject.hide()

    private fun inflateUi() {
        val view = View.inflate(context, R.layout.order_detail_item, this)
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        binding = OrderDetailItemBinding.bind(view)
    }

    @SuppressLint("SetTextI18n")
    fun bind(orderInfo: OrdersInfo) {
        orderinfo = orderInfo
        binding?.apply {
            orderIdTextView.text = "#${orderInfo.id}"
            if (!orderInfo.guest.isNullOrEmpty()) {
                customerNameTextView.text = orderInfo.guest.first().fullName()
            } else {
                if (!orderInfo.user?.fullName().isNullOrEmpty()) {
                    customerNameTextView.text = orderInfo.user?.fullName()
                } else {
                    customerNameTextView.text = ""
                }
            }
            orderTotal = orderInfo.orderTotal?.toDouble()
            if (orderInfo.orderEmpDiscount != null && orderInfo.orderEmpDiscount != 0.00) {
                orderTotal = orderInfo.orderEmpDiscount.let { orderTotal?.minus(it) }
            }
            if (orderInfo.orderRefundAmount != null && orderInfo.orderRefundAmount != 0.00) {
                orderTotal = orderInfo.orderRefundAmount.let { orderTotal?.minus(it) }
            }
            if (orderInfo.orderAdjustmentAmount != null && orderInfo.orderAdjustmentAmount != 0.00) {
                orderTotal = orderInfo.orderAdjustmentAmount.let { orderTotal?.plus(it) }
            }
            orderTotal?.let {
                val price = if ((orderTotal ?: 0.00) < 0.00) {
                    "$0.00"
                } else {
                    ((orderTotal)?.div(100)).toDollar()
                }
                orderTotalAppCompatTextView.text = price
            }
            orderDateAndTimeTextView.text = orderInfo.orderCreationDate?.toDate("yyyy-MM-dd hh:mm:ss a")?.formatTo("MM/dd/yyyy, hh:mm a")
            promisedTimeTextView.text = orderInfo.orderPromisedTime?.toDate("yyyy-MM-dd hh:mm a")?.formatTo("MM/dd/yyyy, hh:mm a")
            orderTypeTextView.text = orderInfo.orderType?.subcategory
            if (orderInfo.status?.isNotEmpty() == true && !orderInfo.status.isNullOrEmpty()) {
                statusTextView.text = orderInfo.status!!.last().orderStatus
                orderIdDotBackgroundImageView.isActivated = orderInfo.status!!.last().orderStatus == "new"
                when (orderInfo.status!!.last().orderStatus?.lowercase()) {
                    resources.getString(R.string.completed).lowercase() -> statusBackgroundCardView.setCardBackgroundColor(
                        ContextCompat.getColor(
                            context, R.color.color_CFE2FE
                        )
                    )
                    resources.getString(R.string.making).lowercase() -> statusBackgroundCardView.setCardBackgroundColor(
                        ContextCompat.getColor(
                            context, R.color.color_C0F2EC
                        )
                    )
                    resources.getString(R.string.delivered).lowercase() -> statusBackgroundCardView.setCardBackgroundColor(
                        ContextCompat.getColor(
                            context, R.color.color_CFE2FE
                        )
                    )
                    resources.getString(R.string.received).lowercase() -> statusBackgroundCardView.setCardBackgroundColor(
                        ContextCompat.getColor(
                            context, R.color.color_FFE0C2
                        )
                    )
                    resources.getString(R.string.dispatched).lowercase() -> statusBackgroundCardView.setCardBackgroundColor(
                        ContextCompat.getColor(
                            context, R.color.color_C0F2EC
                        )
                    )

                    resources.getString(R.string.assigned).lowercase() -> statusBackgroundCardView.setCardBackgroundColor(
                        ContextCompat.getColor(
                            context, R.color.color_FAEDBF
                        )
                    )
                    else -> statusBackgroundCardView.setCardBackgroundColor(ContextCompat.getColor(context, R.color.light_green))
                }
            } else {
                statusTextView.text = "-"
            }


            orderinfo = orderInfo

            orderDetailLinearLayout.isSelected = orderInfo.isSelected
            nextArrowImageView.isSelected = orderInfo.isSelected
            orderDetailLinearLayout.throttleClicks().subscribeAndObserveOnMainThread {
                orderDetailLinearLayout.isSelected = true
                nextArrowImageView.isSelected = true
                orderStateSubject.onNext(orderinfo)
            }.autoDispose()
        }
    }

    override fun onDestroy() {
        binding = null
        super.onDestroy()
    }
}