package com.hotbox.terminal.ui.userstore.guest.view

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.hotbox.terminal.api.menu.model.ModificationItem
import com.hotbox.terminal.api.userstore.model.ModifiersItem

class OrderSubProductAdapter(private val context: Context) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var adapterItems = listOf<AdapterItem>()

    var listOfOrderSubItem: List<ModificationItem>? = null
        set(value) {
            field = value
            updateAdapterItems()
        }

    @Synchronized
    private fun updateAdapterItems() {
        val adapterItem = mutableListOf<AdapterItem>()

        listOfOrderSubItem?.forEach {
            adapterItem.add(AdapterItem.OrderSubProductItem(it))
        }

        this.adapterItems = adapterItem
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ViewType.OrderSubItemViewType.ordinal -> {
                OrderSubProductViewHolder(SubProductItemView(context))
            }
            else -> throw IllegalAccessException("Unsupported ViewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val adapterItem = adapterItems.getOrNull(position) ?: return
        when (adapterItem) {
            is AdapterItem.OrderSubProductItem -> {
                (holder.itemView as SubProductItemView).bind(adapterItem.subOrderItemData)
            }
        }
    }

    override fun getItemCount(): Int {
        return adapterItems.size
    }

    override fun getItemViewType(position: Int): Int {
        return adapterItems[position].type
    }

    private class OrderSubProductViewHolder(view: View) : RecyclerView.ViewHolder(view)

    sealed class AdapterItem(val type: Int) {
        data class OrderSubProductItem(val subOrderItemData: ModificationItem) : AdapterItem(ViewType.OrderSubItemViewType.ordinal)
    }

    private enum class ViewType {
        OrderSubItemViewType
    }
}