package com.hotbox.terminal.ui.main.order.view

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.hotbox.terminal.api.order.model.OrdersInfo
import com.hotbox.terminal.api.order.model.SectionInfo
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class OrderAdapter(private val context: Context) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var adapterItems = listOf<AdapterItem>()
    private val orderStateSubject: PublishSubject<OrdersInfo> = PublishSubject.create()
    val orderActionState: Observable<OrdersInfo> = orderStateSubject.hide()


    var headerInfo: SectionInfo? = null
        set(value) {
            field = value
            updateAdapterItems()
        }

    var listOfOrder: List<OrdersInfo>? = null
        set(value) {
            field = value
            updateAdapterItems()
        }

    @SuppressLint("NotifyDataSetChanged")
    @Synchronized
    private fun updateAdapterItems() {
        val adapterItem = mutableListOf<AdapterItem>()

        headerInfo?.let { header ->
            adapterItem.add(AdapterItem.OrderHeaderItem(header))
        }

        listOfOrder?.forEach { it ->
            adapterItem.add(AdapterItem.OrderDetailItem(it))
        }
        this.adapterItems = adapterItem
        notifyDataSetChanged()
    }

    @SuppressLint("CheckResult")
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ViewType.OrderHeaderViewItemType.ordinal -> {
                OrderViewHolder(OrderHeaderItemView(context))
            }
            ViewType.OrderDetailViewItemType.ordinal -> {
                OrderViewHolder(OrderDetailItemView(context).apply {
                    orderActionState.subscribe { orderStateSubject.onNext(it) }
                })
            }
            else -> throw IllegalAccessException("Unsupported ViewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val adapterItem = adapterItems.getOrNull(position) ?: return
        when (adapterItem) {
            is AdapterItem.OrderHeaderItem -> {
                (holder.itemView as OrderHeaderItemView).bind(adapterItem.sectionInfo)
            }
            is AdapterItem.OrderDetailItem -> {
                (holder.itemView as OrderDetailItemView).bind(adapterItem.orderInfo)
            }
        }
    }

    override fun getItemCount(): Int {
        return adapterItems.size
    }

    override fun getItemViewType(position: Int): Int {
        return adapterItems[position].type
    }

    private class OrderViewHolder(view: View) : RecyclerView.ViewHolder(view)

    sealed class AdapterItem(val type: Int) {
        data class OrderHeaderItem(val sectionInfo: SectionInfo) :
            AdapterItem(ViewType.OrderHeaderViewItemType.ordinal)

        data class OrderDetailItem(val orderInfo: OrdersInfo) :
            AdapterItem(ViewType.OrderDetailViewItemType.ordinal)
    }

    private enum class ViewType {
        OrderHeaderViewItemType,
        OrderDetailViewItemType
    }
}