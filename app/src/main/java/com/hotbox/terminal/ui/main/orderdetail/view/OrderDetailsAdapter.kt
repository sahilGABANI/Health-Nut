package com.hotbox.terminal.ui.main.orderdetail.view

import android.annotation.SuppressLint
import android.content.Context
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.hotbox.terminal.api.order.model.CartItem
import com.hotbox.terminal.api.order.model.OrderDetailItem
import com.hotbox.terminal.api.order.model.OrderDetailsInfo
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class OrderDetailsAdapter(private val context: Context) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var adapterItems = listOf<AdapterItem>()
    private val orderDetailsStateSubject: PublishSubject<OrderDetailsInfo> = PublishSubject.create()
    private val orderDetailsState: Observable<OrderDetailsInfo> = orderDetailsStateSubject.hide()

    var listOfOrderDetailsInfo: List<CartItem>? = null
        set(value) {
            field = value
            updateAdapterItems()
        }

    @SuppressLint("NotifyDataSetChanged")
    @Synchronized
    private fun updateAdapterItems() {
        val adapterItem = mutableListOf<AdapterItem>()
        listOfOrderDetailsInfo?.forEach {
            adapterItem.add(AdapterItem.OrderDetailsItem(it))
        }

        this.adapterItems = adapterItem
        notifyDataSetChanged()
    }

    @SuppressLint("CheckResult")
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ViewType.OrderDetailsItemType.ordinal -> {
                OrderDetailsViewHolder(OrderDetailsView(context).apply {
                    orderDetailsState.subscribe { orderDetailsStateSubject.onNext(it) }
                })
            }
            else -> throw IllegalAccessException("Unsupported ViewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val adapterItem = adapterItems.getOrNull(position) ?: return
        when (adapterItem) {
            is AdapterItem.OrderDetailsItem -> {
                (holder.itemView as OrderDetailsView).bind(adapterItem.orderDetailsInfo)
            }
        }
    }

    override fun getItemCount(): Int {
        return listOfOrderDetailsInfo?.size ?: 0
    }

    private class OrderDetailsViewHolder(view: OrderDetailsView) : RecyclerView.ViewHolder(view)

    private enum class ViewType {
        OrderDetailsItemType
    }

    sealed class AdapterItem(val type: Int) {
        data class OrderDetailsItem(val orderDetailsInfo: CartItem) :
            AdapterItem(ViewType.OrderDetailsItemType.ordinal)
    }
}