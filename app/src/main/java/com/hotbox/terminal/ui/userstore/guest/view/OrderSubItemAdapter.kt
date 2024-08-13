package com.hotbox.terminal.ui.userstore.guest.view

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.hotbox.terminal.api.userstore.model.SubOrderItemData
import com.hotbox.terminal.base.extension.subscribeAndObserveOnMainThread
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject


class OrderSubItemAdapter(private val context: Context) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var adapterItems = listOf<AdapterItem>()

    private val subProductStateSubject: PublishSubject<SubOrderItemData> = PublishSubject.create()
    val subProductActionState: Observable<SubOrderItemData> = subProductStateSubject.hide()

    var listOfOrderSubItem: List<SubOrderItemData>? = null
        set(value) {
            field = value
            updateAdapterItems()
        }

    @SuppressLint("NotifyDataSetChanged")
    @Synchronized
    private fun updateAdapterItems() {
        val adapterItem = mutableListOf<AdapterItem>()

        listOfOrderSubItem?.forEach {
            adapterItem.add(AdapterItem.OrderSubItem(it))
        }

        this.adapterItems = adapterItem
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ViewType.OrderSubItemViewType.ordinal -> {
                OrderSubItemViewHolder(OrderSubItemView(context).apply {
                    subProductActionState.subscribe { subProductStateSubject.onNext(it) }
                })
            }
            else -> throw IllegalAccessException("Unsupported ViewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {

        val adapterItem = adapterItems.getOrNull(position) ?: return
        when (adapterItem) {
            is AdapterItem.OrderSubItem -> {
                (holder.itemView as OrderSubItemView).bind(adapterItem.subOrderItemData,position)
            }
        }
    }

    override fun getItemCount(): Int {
        return adapterItems.size
    }

    override fun getItemViewType(position: Int): Int {
        return adapterItems[position].type
    }

    private class OrderSubItemViewHolder(view: View) : RecyclerView.ViewHolder(view)

    sealed class AdapterItem(val type: Int) {
        data class OrderSubItem(val subOrderItemData: SubOrderItemData) : AdapterItem(ViewType.OrderSubItemViewType.ordinal)
    }

    private enum class ViewType {
        OrderSubItemViewType
    }

}