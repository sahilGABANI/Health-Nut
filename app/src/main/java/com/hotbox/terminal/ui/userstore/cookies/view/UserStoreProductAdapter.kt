package com.hotbox.terminal.ui.userstore.cookies.view

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.hotbox.terminal.api.menu.model.ProductsItem
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class UserStoreProductAdapter(private val context: Context) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var adapterItems = listOf<AdapterItem>()

    private val userStoreProductStateSubject: PublishSubject<ProductsItem> = PublishSubject.create()
    val userStoreProductActionState: Observable<ProductsItem> = userStoreProductStateSubject.hide()

    var listOfProductDetails: List<ProductsItem>? = null
        set(value) {
            field = value
            updateAdapterItems()
        }

    @SuppressLint("NotifyDataSetChanged")
    @Synchronized
    private fun updateAdapterItems() {
        val adapterItem = mutableListOf<AdapterItem>()

        listOfProductDetails?.forEach {
            adapterItem.add(AdapterItem.UserStoreProductItem(it))
        }

        this.adapterItems = adapterItem
        notifyDataSetChanged()
    }

    @SuppressLint("CheckResult")
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ViewType.UserStoreProductViewType.ordinal -> {
                UserStoreProductViewHolder(UserStoreProductView(context).apply {
                    userStoreProductActionState.subscribe { userStoreProductStateSubject.onNext(it) }
                })
            }
            else -> throw IllegalAccessException("Unsupported ViewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val adapterItem = adapterItems.getOrNull(position) ?: return
        when (adapterItem) {
            is AdapterItem.UserStoreProductItem -> {
                (holder.itemView as UserStoreProductView).bind(adapterItem.productInfo)
            }
        }
    }

    override fun getItemCount(): Int {
        return adapterItems.size
    }

    override fun getItemViewType(position: Int): Int {
        return adapterItems[position].type
    }

    private class UserStoreProductViewHolder(view: View) : RecyclerView.ViewHolder(view)


    sealed class AdapterItem(val type: Int) {
        data class UserStoreProductItem(val productInfo: ProductsItem) :
            AdapterItem(ViewType.UserStoreProductViewType.ordinal)
    }

    private enum class ViewType {
        UserStoreProductViewType
    }
}