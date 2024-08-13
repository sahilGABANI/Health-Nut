package com.hotbox.terminal.ui.wifi.view

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.hotbox.terminal.api.wifi.WifiInfo
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class WifiAdapter(private val context: Context) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var adapterItems = listOf<AdapterItem>()

    private val wifiStateSubject: PublishSubject<WifiInfo> = PublishSubject.create()
    val wifiActionState: Observable<WifiInfo> = wifiStateSubject.hide()

    var listOfWifi: ArrayList<WifiInfo>? = null
        set(value) {
            field = value
            updateAdapterItems()
        }

    @SuppressLint("NotifyDataSetChanged")
    @Synchronized
    private fun updateAdapterItems() {
        val adapterItem = mutableListOf<AdapterItem>()

        listOfWifi?.forEach {
            adapterItem.add(AdapterItem.WifiViewItem(it))
        }

        this.adapterItems = adapterItem
        notifyDataSetChanged()
    }


    @SuppressLint("CheckResult")
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ViewType.WIfiViewItemType.ordinal -> {
                WifiViewHolder(WifiView(context).apply {
                    wifiActionState.subscribe { wifiStateSubject.onNext(it) }
                })
            }
            else -> throw IllegalAccessException("Unsupported ViewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val adapterItem = adapterItems.getOrNull(position) ?: return
        when (adapterItem) {
            is AdapterItem.WifiViewItem -> {
                (holder.itemView as WifiView).bind(adapterItem.wifiInfo)
            }
        }

    }

    override fun getItemCount(): Int {
        return listOfWifi?.size ?: 0
    }

    private class WifiViewHolder(view: View) : RecyclerView.ViewHolder(view)

    private enum class ViewType {
        WIfiViewItemType
    }

    sealed class AdapterItem(val type: Int) {
        data class WifiViewItem(val wifiInfo: WifiInfo) :
            AdapterItem(ViewType.WIfiViewItemType.ordinal)
    }
}