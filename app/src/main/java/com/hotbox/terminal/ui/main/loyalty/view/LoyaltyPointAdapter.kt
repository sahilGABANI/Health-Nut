package com.hotbox.terminal.ui.main.loyalty.view

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.hotbox.terminal.api.checkout.model.LoyaltyPhoneHistoryInfo
import com.hotbox.terminal.base.extension.subscribeAndObserveOnMainThread
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class LoyaltyPointAdapter(private val context: Context) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {


    private var adapterItems = listOf<AdapterItem>()
    private val loyaltyPointStateSubject: PublishSubject<LoyaltyPhoneHistoryInfo> = PublishSubject.create()
    val loyaltyPointActionState: Observable<LoyaltyPhoneHistoryInfo> = loyaltyPointStateSubject.hide()
    var listOfLoyaltyPoint: List<LoyaltyPhoneHistoryInfo>? = null
        set(value) {
            field = value
            updateAdapterItems()
        }

    @SuppressLint("NotifyDataSetChanged")
    @Synchronized
    private fun updateAdapterItems() {
        val adapterItem = mutableListOf<AdapterItem>()

        listOfLoyaltyPoint?.forEach { details ->
            adapterItem.add(AdapterItem.LoyaltyPointItem(details))
        }
        this.adapterItems = adapterItem
        notifyDataSetChanged()
    }

    @SuppressLint("CheckResult")
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ViewType.LoyaltyPointViewItemType.ordinal -> {
                LoyaltyPointViewHolder(LoyaltyPointView(context).apply {
                    loyaltyPointActionState.subscribe { loyaltyPointStateSubject.onNext(it) }
                })
            }
            else -> throw IllegalAccessException("Unsupported ViewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val adapterItem = adapterItems.getOrNull(position) ?: return
        when (adapterItem) {
            is AdapterItem.LoyaltyPointItem -> {
                (holder.itemView as LoyaltyPointView).bind(adapterItem.loyaltyPhoneHistoryInfo)
            }
        }
    }

    override fun getItemCount(): Int {
        return adapterItems.size
    }

    override fun getItemViewType(position: Int): Int {
        return adapterItems[position].type
    }

    private class LoyaltyPointViewHolder(view: View) : RecyclerView.ViewHolder(view)

    sealed class AdapterItem(val type: Int) {
        data class LoyaltyPointItem(val loyaltyPhoneHistoryInfo: LoyaltyPhoneHistoryInfo) : AdapterItem(ViewType.LoyaltyPointViewItemType.ordinal)
    }

    private enum class ViewType {
        LoyaltyPointViewItemType
    }
}