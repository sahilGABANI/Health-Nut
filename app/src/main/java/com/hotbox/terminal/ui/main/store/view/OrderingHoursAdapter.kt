package com.hotbox.terminal.ui.main.store.view

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.hotbox.terminal.api.store.model.StoreShiftTime

class OrderingHoursAdapter(private val context: Context) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private var adapterItems = listOf<AdapterItem>()

    var listOfOrderingHours: List<StoreShiftTime>? = null
        set(value) {
            field = value
            updateAdapterItems()
        }

    @SuppressLint("NotifyDataSetChanged")
    @Synchronized
    private fun updateAdapterItems() {
        val adapterItem = mutableListOf<AdapterItem>()

        listOfOrderingHours?.forEach { details ->
            adapterItem.add(AdapterItem.OrderingHoursDetailsItem(details))
        }
        this.adapterItems = adapterItem
        notifyDataSetChanged()
    }

    @SuppressLint("CheckResult")
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ViewType.OrderingHoursViewItemType.ordinal -> {
                OrderingHoursViewHolder(OrderingHoursView(context))
            }
            else -> throw IllegalAccessException("Unsupported ViewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val adapterItem = adapterItems.getOrNull(position) ?: return
        when (adapterItem) {
            is AdapterItem.OrderingHoursDetailsItem -> {
                (holder.itemView as OrderingHoursView).bind(adapterItem.storeShiftTime)
            }
        }
    }

    override fun getItemCount(): Int {
        return adapterItems.size
    }

    override fun getItemViewType(position: Int): Int {
        return adapterItems[position].type
    }

    private class OrderingHoursViewHolder(view: View) : RecyclerView.ViewHolder(view)

    sealed class AdapterItem(val type: Int) {
        data class OrderingHoursDetailsItem(val storeShiftTime: StoreShiftTime) :
            AdapterItem(ViewType.OrderingHoursViewItemType.ordinal)
    }

    private enum class ViewType {
        OrderingHoursViewItemType
    }
}