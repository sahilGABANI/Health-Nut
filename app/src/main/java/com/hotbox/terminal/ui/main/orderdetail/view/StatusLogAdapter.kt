package com.hotbox.terminal.ui.main.orderdetail.view

import android.annotation.SuppressLint
import android.content.Context
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.hotbox.terminal.api.order.model.StatusItem
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class StatusLogAdapter(private val context: Context) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var adapterItems = listOf<AdapterItem>()

    private val statusLogStateSubject: PublishSubject<StatusItem> = PublishSubject.create()
    private val statusLogActionState: Observable<StatusItem> = statusLogStateSubject.hide()

    var listOfStatusLog: List<StatusItem>? = null
        set(value) {
            field = value
            updateAdapterItems()
        }

    @SuppressLint("NotifyDataSetChanged")
    @Synchronized
    private fun updateAdapterItems() {
        val adapterItem = mutableListOf<AdapterItem>()

        listOfStatusLog?.forEach {
            adapterItem.add(AdapterItem.StatusLogItem(it))
        }

        this.adapterItems = adapterItem
        notifyDataSetChanged()
    }

    private class StatusLogViewHolder(view: StatusLogView) : RecyclerView.ViewHolder(view)

    @SuppressLint("CheckResult")
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ViewType.StatusLogItemType.ordinal -> {
                StatusLogViewHolder(StatusLogView(context).apply {
                    statusLogActionState.subscribe { statusLogStateSubject.onNext(it) }
                })
            }
            else -> throw IllegalAccessException("Unsupported ViewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val adapterItem = adapterItems.getOrNull(position) ?: return
        when (adapterItem) {
            is AdapterItem.StatusLogItem -> {
                (holder.itemView as StatusLogView).bind(adapterItem.statusItem)
            }
        }
    }

    override fun getItemCount(): Int {
        return adapterItems.size
    }

    private enum class ViewType {
        StatusLogItemType
    }

    sealed class AdapterItem(val type: Int) {
        data class StatusLogItem(val statusItem: StatusItem) :
            AdapterItem(ViewType.StatusLogItemType.ordinal)
    }
}