package com.hotbox.terminal.ui.main.timemanagement.view

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.hotbox.terminal.api.clockinout.model.ClockInOutHistoryResponse
import com.hotbox.terminal.api.clockinout.model.TimeResponse
import io.reactivex.subjects.PublishSubject

class ClockInOutAdapter(private val context: Context) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var adapterItems = listOf<AdapterItem>()
    private val clockInOutStateSubject: PublishSubject<ClockInOutHistoryResponse> = PublishSubject.create()
    private val clockInOutActionState = clockInOutStateSubject.hide()

    var listOfClockInOut: List<TimeResponse>? = null
        set(value) {
            field = value
            updateAdapterItems()
        }

    @SuppressLint("NotifyDataSetChanged")
    @Synchronized
    private fun updateAdapterItems() {
        val adapterItem = mutableListOf<AdapterItem>()

        listOfClockInOut?.forEach { details ->
            adapterItem.add(AdapterItem.ClockInOutDetailItem(details))
        }
        this.adapterItems = adapterItem
        notifyDataSetChanged()
    }

    @SuppressLint("CheckResult")
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ViewType.ClockInOutDetailViewItemType.ordinal -> {
                ClockInOutViewHolder(ClockInOutDetailItemView(context).apply {
                    clockInOutActionState.subscribe { clockInOutStateSubject.onNext(it) }
                })
            }
            else -> throw IllegalAccessException("Unsupported ViewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val adapterItem = adapterItems.getOrNull(position) ?: return
        when (adapterItem) {
            is AdapterItem.ClockInOutDetailItem -> {
                (holder.itemView as ClockInOutDetailItemView).bind(adapterItem.clockInOutDetailsInfo)
            }
        }
    }

    override fun getItemCount(): Int {
        return adapterItems.size
    }

    override fun getItemViewType(position: Int): Int {
        return adapterItems[position].type
    }

    private class ClockInOutViewHolder(view: View) : RecyclerView.ViewHolder(view)

    sealed class AdapterItem(val type: Int) {
        data class ClockInOutDetailItem(val clockInOutDetailsInfo: TimeResponse) :
            AdapterItem(ViewType.ClockInOutDetailViewItemType.ordinal)
    }

    private enum class ViewType {
        ClockInOutDetailViewItemType
    }
}