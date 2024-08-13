package com.hotbox.terminal.api.menu.model


import com.google.gson.annotations.SerializedName
import com.hotbox.terminal.api.order.model.OptionsItem
import com.hotbox.terminal.api.userstore.model.ModifiersItem
import com.hotbox.terminal.api.userstore.model.OptionsItemRequest
import com.hotbox.terminal.base.extension.toDollar


data class MenuSectionInfo(
    val productName: String,
    val productDescription: String,
    val productPrice: String,
    val productState: String,
)

data class MenuListInfo(

    @field:SerializedName("menus")
    val menus: List<MenusItem>? = null
)

data class MenusItem(

    @field:SerializedName("menu_group_id")
    val menuGroupId: Int? = null,

    @field:SerializedName("category_name")
    val categoryName: String? = null,

    @field:SerializedName("list_order")
    val listOrder: Int? = null,

    @field:SerializedName("id")
    val id: Int? = null,

    @field:SerializedName("products")
    val products: List<ProductsItem>? = null,

    var isSelected: Boolean = false
)


data class ProductsItem(

    @field:SerializedName("product_tags")
    val productTags: ProductTags? = null,

    @field:SerializedName("product_cals")
    val productCals: Int? = null,

    @field:SerializedName("product_image")
    val productImage: String? = null,

    @field:SerializedName("id")
    val id: String? = null,

    @field:SerializedName("menu_id")
    val menuId: Int? = null,

    @field:SerializedName("product_upccode")
    val productUpccode: Any? = null,

    @field:SerializedName("product_active")
    var productActive: Boolean? = null,

    @field:SerializedName("menu_active")
    var menuActive: Boolean? = null,

    @field:SerializedName("product_description")
    val productDescription: String? = null,

    @field:SerializedName("product_name")
    val productName: String? = null,

    @field:SerializedName("product_base_price")
    val productBasePrice: Double? = null,

    @field:SerializedName("modification")
    val modification: List<ModificationItem>? = null,

    var isRedeemProduct : Boolean? = false,

    @field:SerializedName("product_loyalty_tier")
    var productLoyaltyTier: ProductLoyaltyTier? = null
)


data class ProductTags(

    @field:SerializedName("tag_name")
    val tagName: String? = null,

    @field:SerializedName("active")
    val active: Boolean? = null,

    @field:SerializedName("id")
    val id: Int? = null
)
data class ProductLoyaltyTier(

    @field:SerializedName("tier_name")
    val tierName: String? = null,

    @field:SerializedName("tier_value")
    var tierValue: Int? = 0,

    @field:SerializedName("id")
    val id: Int? = null
)

data class ProductStateRequest(
    @field:SerializedName("active")
    val active: Boolean? = false,

    @field:SerializedName("menu_id")
    val menuId: Int? = null,

    @field:SerializedName("price_override")
    val priceOverride: Int? = null,


    )


data class ModificationItem(

    @field:SerializedName("select_max")
    val selectMax: Int? = null,

    @field:SerializedName("is_required")
    val isRequired: Boolean? = null,

    @field:SerializedName("active")
    val active: Boolean? = null,

    @field:SerializedName("id")
    val id: Int? = null,

    @field:SerializedName("modification_text")
    val modificationText: String? = null,

    @field:SerializedName("type")
    val type: String? = null,

    @field:SerializedName("select_min")
    val selectMin: Int? = null,

    @field:SerializedName("options")
    val options: List<OptionsItem>? = null,

    var selectedOption: Int? = 0,
    var isSelected: Boolean? = false,
    var isLast :Boolean? = false,
    var selectedOptionsItem: ArrayList<OptionsItemRequest> = ArrayList<OptionsItemRequest>()
){
    fun getSafeSelectedItemName(): String {
        val selectedItemStringBuilder = StringBuilder().apply {
            if (options != null) {
                for (i in options.indices) {
                    if (i == 0) {
                        append("${options[i].optionName}")
                    } else {
                        append(", ${options[i].optionName}")
                    }
                }
            }
        }
        return selectedItemStringBuilder.toString()
    }

    fun getSafeSelectedItemPrice(): String {
        val selectedItemStringBuilder = StringBuilder().apply {
            if (options != null) {
                for (i in options.indices) {
                    if (options[i].optionPrice?.equals(0.0) == false) {
                        append("(${options[i].optionPrice?.div(100).toDollar()})")
                    } else {
                        append("")
                    }
                }
            }
        }
        return selectedItemStringBuilder.toString()
    }
}


data class Group(

    @field:SerializedName("mod_group_name")
    val modGroupName: String? = null,

    @field:SerializedName("options")
    val options: List<OptionsItem>? = null,

    @field:SerializedName("active")
    val active: Boolean? = null,

    @field:SerializedName("id")
    val id: Int? = null,

    var selectedOptionsItem: ArrayList<OptionsItemRequest>? = null
)  {
    fun getSafeSelectedItemName(): String {
        val selectedItemStringBuilder = StringBuilder().apply {
            if (options != null) {
                for (i in options.indices) {
                    if (i == 0) {
                        append("${options[i].optionName}")
                    } else {
                        append(", ${options[i].optionName}")
                    }
                }
            }
        }
        return selectedItemStringBuilder.toString()
    }

    fun getSafeSelectedItemPrice(): String {
        val selectedItemStringBuilder = StringBuilder().apply {
            if (options != null) {
                for (i in options.indices) {
                    if (options[i].optionPrice?.equals(0.0) == false) {
                        append("(${options[i].optionPrice?.div(100).toDollar()})")
                    } else {
                        append("")
                    }
                }
            }
        }
        return selectedItemStringBuilder.toString()
    }
}



