package com.hotbox.terminal.api.menu

import com.hotbox.terminal.api.authentication.LoggedInUserCache
import com.hotbox.terminal.api.menu.model.MenuListInfo
import com.hotbox.terminal.api.menu.model.MenusItem
import com.hotbox.terminal.api.menu.model.ProductStateRequest
import com.hotbox.terminal.api.menu.model.ProductsItem
import com.hotbox.terminal.base.network.HotBoxResponseConverter
import com.hotbox.terminal.utils.Constants.PLATFORM
import io.reactivex.Single

class MenuRepository(
    private val menuRetrofitAPI: MenuRetrofitAPI, private val loggedInUserCache: LoggedInUserCache
) {

    private val hotBoxResponseConverter: HotBoxResponseConverter = HotBoxResponseConverter()

    fun getMenuByLocation(): Single<MenuListInfo> {
        val locationId = loggedInUserCache.getLocationInfo()?.location?.id ?: throw Exception("location not found")
        return menuRetrofitAPI.getMenuByLocation(locationId, PLATFORM).flatMap {
            hotBoxResponseConverter.convertToSingle(it)
        }
    }

    fun updateMenuState(request: ProductStateRequest): Single<ProductsItem> {
        return menuRetrofitAPI.updateMenuState(request).flatMap {
            hotBoxResponseConverter.convertToSingle(it)
        }
    }
}