package com.hotbox.terminal.ui.userstore.view

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.hotbox.terminal.api.order.model.OptionsItem

class SubItemAdapter(private val context: Context) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private var adapterItems = listOf<AdapterItem>()

    var listOfSubProductDetails: List<OptionsItem>? = null
        set(value) {
            field = value
            updateAdapterItems()
        }

    @SuppressLint("NotifyDataSetChanged")
    @Synchronized
    private fun updateAdapterItems() {
        val adapterItem = mutableListOf<AdapterItem>()

        listOfSubProductDetails?.forEach {
            adapterItem.add(AdapterItem.UserStoreCartSubItem(it))
        }

        this.adapterItems = adapterItem
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ViewType.SubItemViewType.ordinal -> {
                SubItemViewHolder(SubItemView(context))
            }
            else -> throw IllegalAccessException("Unsupported ViewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val adapterItem = adapterItems.getOrNull(position) ?: return
        when (adapterItem) {
            is AdapterItem.UserStoreCartSubItem -> {
                (holder.itemView as SubItemView).bind(adapterItem.optionsItem,position+1)
            }
        }
    }

    override fun getItemCount(): Int {
        return adapterItems.size
    }

    override fun getItemViewType(position: Int): Int {
        return adapterItems[position].type
    }

    private class SubItemViewHolder(view: View) : RecyclerView.ViewHolder(view)

    sealed class AdapterItem(val type: Int) {
        data class UserStoreCartSubItem(val optionsItem: OptionsItem) :
            AdapterItem(ViewType.SubItemViewType.ordinal)
    }

    private enum class ViewType {
        SubItemViewType
    }
}