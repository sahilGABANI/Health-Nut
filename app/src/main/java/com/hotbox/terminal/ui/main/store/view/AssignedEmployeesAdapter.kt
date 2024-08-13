package com.hotbox.terminal.ui.main.store.view

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.hotbox.terminal.api.store.model.AssignedEmployeeInfo
import io.reactivex.subjects.PublishSubject

class AssignedEmployeesAdapter(private val context: Context) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val orderingHoursStateSubject: PublishSubject<AssignedEmployeeInfo> = PublishSubject.create()
    private val orderingHoursActionState = orderingHoursStateSubject.hide()
    private var adapterItems = listOf<AdapterItem>()

    var listOfAssignedEmployees: List<AssignedEmployeeInfo>? = null
        set(value) {
            field = value
            updateAdapterItems()
        }

    @SuppressLint("NotifyDataSetChanged")
    @Synchronized
    private fun updateAdapterItems() {
        val adapterItem = mutableListOf<AdapterItem>()

        listOfAssignedEmployees?.forEach { details ->
            adapterItem.add(AdapterItem.AssignedEmployeesDetailsItem(details))
        }
        this.adapterItems = adapterItem
        notifyDataSetChanged()
    }

    @SuppressLint("CheckResult")
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ViewType.AssignedEmployeesViewItemType.ordinal -> {
                AssignedEmployeesViewHolder(AssignedEmployeesView(context).apply {
                    orderingHoursActionState.subscribe { orderingHoursStateSubject.onNext(it) }
                })
            }
            else -> throw IllegalAccessException("Unsupported ViewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val adapterItem = adapterItems.getOrNull(position) ?: return
        when (adapterItem) {
            is AdapterItem.AssignedEmployeesDetailsItem -> {
                (holder.itemView as AssignedEmployeesView).bind(adapterItem.assignedEmployeeInfo)
            }
        }
    }

    override fun getItemCount(): Int {
        return adapterItems.size
    }

    override fun getItemViewType(position: Int): Int {
        return adapterItems[position].type
    }

    private class AssignedEmployeesViewHolder(view: View) : RecyclerView.ViewHolder(view)

    sealed class AdapterItem(val type: Int) {
        data class AssignedEmployeesDetailsItem(val assignedEmployeeInfo: AssignedEmployeeInfo) :
            AdapterItem(ViewType.AssignedEmployeesViewItemType.ordinal)
    }

    private enum class ViewType {
        AssignedEmployeesViewItemType
    }
}