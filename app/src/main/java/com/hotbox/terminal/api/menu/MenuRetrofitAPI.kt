package com.hotbox.terminal.api.menu

import com.hotbox.terminal.api.menu.model.MenuListInfo
import com.hotbox.terminal.api.menu.model.ProductStateRequest
import com.hotbox.terminal.api.menu.model.ProductsItem
import com.hotbox.terminal.base.network.model.HotBoxResponse
import io.reactivex.Single
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface MenuRetrofitAPI {

    @GET("v1/menus/get-menus-and-products")
    fun getMenuByLocation(@Query("location_id") locationId: Int,@Query("platform") platform: String): Single<HotBoxResponse<MenuListInfo>>

    @POST("v1/pos/update-menu")
    fun updateMenuState(@Body request: ProductStateRequest): Single<HotBoxResponse<ProductsItem>>
}