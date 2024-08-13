package com.hotbox.terminal.ui.userstore.customize.view

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.hotbox.terminal.api.menu.model.ModificationItem
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class ModificationAdapter(private val context: Context) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private val modificationStateSubject: PublishSubject<ModificationItem> = PublishSubject.create()
    val modificationActionState: Observable<ModificationItem> = modificationStateSubject.hide()
    private var adapterItems = listOf<AdapterItem>()

    var listOfModification: List<ModificationItem>? = null
        set(value) {
            field = value
            updateAdapterItems()
        }

    @SuppressLint("NotifyDataSetChanged")
    @Synchronized
    private fun updateAdapterItems() {
        val adapterItem = mutableListOf<AdapterItem>()

        listOfModification?.forEach {
            adapterItem.add(AdapterItem.ModificationOptionItem(it))
        }

        this.adapterItems = adapterItem
        notifyDataSetChanged()
    }

    @SuppressLint("CheckResult")
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ViewType.OrderOptionItemItemViewType.ordinal -> {
                OrderOptionItemViewHolder(ModificationView(context).apply {
                    modificationActionState.subscribe { modificationStateSubject.onNext(it) }
                })
            }
            else -> throw IllegalAccessException("Unsupported ViewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val adapterItem = adapterItems.getOrNull(position) ?: return
        when (adapterItem) {
            is AdapterItem.ModificationOptionItem -> {
                (holder.itemView as ModificationView).bind(adapterItem.subOrderItemData)
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
        data class ModificationOptionItem(val subOrderItemData: ModificationItem) : AdapterItem(ViewType.OrderOptionItemItemViewType.ordinal)
    }

    private enum class ViewType {
        OrderOptionItemItemViewType
    }
}