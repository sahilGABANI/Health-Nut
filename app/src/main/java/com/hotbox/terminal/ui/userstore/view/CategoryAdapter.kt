package com.hotbox.terminal.ui.userstore.view

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.hotbox.terminal.api.menu.model.MenusItem
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class CategoryAdapter(private val context: Context) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var adapterItems = listOf<AdapterItem>()

    private val userStoreCategoryStateSubject: PublishSubject<MenusItem> = PublishSubject.create()
    val userStoreCategoryActionState: Observable<MenusItem> = userStoreCategoryStateSubject.hide()

    var listOfMenu: List<MenusItem>? = null
        set(value) {
            field = value
            updateAdapterItems()
        }


    @SuppressLint("NotifyDataSetChanged")
    @Synchronized
    private fun updateAdapterItems() {
        val adapterItem = mutableListOf<AdapterItem>()

        listOfMenu?.forEach {
            adapterItem.add(AdapterItem.UserStoreCategoryItem(it))
        }

        this.adapterItems = adapterItem
        notifyDataSetChanged()
    }

    @SuppressLint("CheckResult")
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ViewType.UserStoreCategoryViewType.ordinal -> {
                CategoryItemViewHolder(CategoryItemView(context).apply {
                    userStoreCategoryActionState.subscribe { userStoreCategoryStateSubject.onNext(it) }
                })
            }
            else -> throw IllegalAccessException("Unsupported ViewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val adapterItem = adapterItems.getOrNull(position) ?: return
        when (adapterItem) {
            is AdapterItem.UserStoreCategoryItem -> {
                (holder.itemView as CategoryItemView).bind(adapterItem.menusItem,position)
            }
        }
    }

    override fun getItemCount(): Int {
        return adapterItems.size
    }

    sealed class AdapterItem(val type: Int) {
        data class UserStoreCategoryItem(val menusItem: MenusItem) : AdapterItem(ViewType.UserStoreCategoryViewType.ordinal)
    }

    override fun getItemViewType(position: Int): Int {
        return adapterItems[position].type
    }

    private class CategoryItemViewHolder(view: View) : RecyclerView.ViewHolder(view)

    private enum class ViewType {
        UserStoreCategoryViewType
    }
}