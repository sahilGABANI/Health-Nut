package com.hotbox.terminal.ui.userstore.customize.view

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.hotbox.terminal.api.order.model.OptionsItem
import com.hotbox.terminal.api.userstore.model.OptionsItemRequest
import com.hotbox.terminal.base.extension.subscribeAndObserveOnMainThread
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class SelectedOptionAdapter(private val context: Context) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val selectedOptionStateSubject: PublishSubject<OptionsItemRequest> = PublishSubject.create()
    val selectedOptionActionState: Observable<OptionsItemRequest> = selectedOptionStateSubject.hide()

    private var adapterItems = listOf<AdapterItem>()

    var listOfOrderSubItem: List<OptionsItemRequest>? = null
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
                OrderOptionItemViewHolder(SelectedOptionView(context).apply {
                    selectedOptionActionState.subscribe { selectedOptionStateSubject.onNext(it) }
                })
            }
            else -> throw IllegalAccessException("Unsupported ViewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val adapterItem = adapterItems.getOrNull(position) ?: return
        when (adapterItem) {
            is AdapterItem.OrderOptionItem -> {
                (holder.itemView as SelectedOptionView).bind(adapterItem.subOrderItemData)
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
        data class OrderOptionItem(val subOrderItemData: OptionsItemRequest) : AdapterItem(ViewType.OrderOptionItemItemViewType.ordinal)
    }

    private enum class ViewType {
        OrderOptionItemItemViewType
    }
}