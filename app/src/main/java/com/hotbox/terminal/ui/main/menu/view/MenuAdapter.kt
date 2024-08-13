package com.hotbox.terminal.ui.main.menu.view

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.hotbox.terminal.api.menu.model.MenuSectionInfo
import com.hotbox.terminal.api.menu.model.ProductsItem
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class MenuAdapter(private val context: Context) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var adapterItems = listOf<AdapterItem>()

    private val menuStateSubject: PublishSubject<ProductsItem> = PublishSubject.create()
    val menuActionState: Observable<ProductsItem> = menuStateSubject.hide()

    var headerInfo: MenuSectionInfo? = null
        set(value) {
            field = value
            updateAdapterItems()
        }

    var listOfMenu: List<ProductsItem>? = null
        set(value) {
            field = value
            updateAdapterItems()
        }

    @SuppressLint("NotifyDataSetChanged")
    @Synchronized
    private fun updateAdapterItems() {
        val adapterItem = mutableListOf<AdapterItem>()

        headerInfo?.let { header ->
            adapterItem.add(AdapterItem.MenuHeaderItem(header))
        }

        listOfMenu?.forEach { it ->
            adapterItem.add(AdapterItem.MenuDetailItem(it))
        }
        this.adapterItems = adapterItem
        notifyDataSetChanged()
    }

    @SuppressLint("CheckResult")
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ViewType.MenuHeaderViewItemType.ordinal -> {
                MenuViewHolder(MenuHeaderItemView(context))
            }
            ViewType.MenuDetailsViewItemType.ordinal -> {
                MenuViewHolder(MenuDetailItemView(context).apply {
                    menuActionState.subscribe { menuStateSubject.onNext(it) }
                })
            }
            else -> throw IllegalAccessException("Unsupported ViewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val adapterItem = adapterItems.getOrNull(position) ?: return
        when (adapterItem) {
            is AdapterItem.MenuHeaderItem -> {
                (holder.itemView as MenuHeaderItemView).bind(adapterItem.sectionInfo)
            }
            is AdapterItem.MenuDetailItem -> {
                (holder.itemView as MenuDetailItemView).bind(adapterItem.productsItem)
            }
        }
    }

    private class MenuViewHolder(view: View) : RecyclerView.ViewHolder(view)

    sealed class AdapterItem(val type: Int) {
        data class MenuHeaderItem(val sectionInfo: MenuSectionInfo) :
            AdapterItem(ViewType.MenuHeaderViewItemType.ordinal)

        data class MenuDetailItem(val productsItem: ProductsItem) :
            AdapterItem(ViewType.MenuDetailsViewItemType.ordinal)
    }

    override fun getItemCount(): Int {
        return adapterItems.size
    }

    override fun getItemViewType(position: Int): Int {
        return adapterItems[position].type
    }

    private enum class ViewType {
        MenuHeaderViewItemType,
        MenuDetailsViewItemType
    }
}