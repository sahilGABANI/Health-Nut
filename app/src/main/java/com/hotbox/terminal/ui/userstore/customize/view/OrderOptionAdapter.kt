package com.hotbox.terminal.ui.userstore.customize.view

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.hotbox.terminal.api.order.model.OptionsItem
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject


class OrderOptionAdapter(private val context: Context) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private val optionStateSubject: PublishSubject<OptionsItem> = PublishSubject.create()
    val optionActionState: Observable<OptionsItem> = optionStateSubject.hide()
    private var adapterItems = listOf<AdapterItem>()

    var listOfOrderSubItem: List<OptionsItem>? = null
        set(value) {
            field = value
            updateAdapterItems()
        }

    @SuppressLint("NotifyDataSetChanged")
    @Synchronized
    private fun updateAdapterItems() {
        val adapterItem = mutableListOf<AdapterItem>()

        listOfOrderSubItem?.forEach {
            adapterItem.add(AdapterItem.OrderOptionItem(it))
        }

        this.adapterItems = adapterItem
        notifyDataSetChanged()
    }

    @SuppressLint("CheckResult")
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ViewType.OrderOptionItemItemViewType.ordinal -> {
                OrderOptionItemViewHolder(OrderOptionView(context).apply {
                    optionActionState.subscribe { optionStateSubject.onNext(it)}
                })
            }
            else -> throw IllegalAccessException("Unsupported ViewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val adapterItem = adapterItems.getOrNull(position) ?: return
        when (adapterItem) {
            is AdapterItem.OrderOptionItem -> {
                (holder.itemView as OrderOptionView).bind(adapterItem.subOrderItemData)
            }
        }
    }

    override fun getItemCount(): Int {
        return adapterItems.size
    }

    override fun getItemViewType(position: Int): Int {
        return adapterItems[position].type
    }

    private class OrderOptionItemViewHolder(view: View) : RecyclerView.ViewHolder(view)

    sealed class AdapterItem(val type: Int) {
        data class OrderOptionItem(val subOrderItemData: OptionsItem) : AdapterItem(ViewType.OrderOptionItemItemViewType.ordinal)
    }

    private enum class ViewType {
        OrderOptionItemItemViewType
    }

}