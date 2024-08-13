package com.hotbox.terminal.api.userstore.model

import android.os.Parcelable
import androidx.annotation.Keep
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import com.hotbox.terminal.api.authentication.model.HealthNutUser
import com.hotbox.terminal.api.menu.model.ModificationItem
import com.hotbox.terminal.api.order.model.MenuItemModifiersItem
import com.hotbox.terminal.api.order.model.OptionsItem
import com.hotbox.terminal.api.order.model.OrderLocation
import com.hotbox.terminal.base.RxEvent
import com.hotbox.terminal.base.extension.toDollar
import kotlinx.android.parcel.Parcelize
import java.lang.reflect.Type


data class SubOrderItemData(
    var productName: String,

    var subProductList: List<OptionsItem>? = null,

    var optionImage: String? = null,

    var optionsItem: ArrayList<OptionsItem> = ArrayList<OptionsItem>(),
    val modifiers: ModificationItem? = null,
    val isLastItem : Boolean = false
)

data class AddToCartRequest(

    @field:SerializedName("promised_time")
    val promisedTime: String? = null,

    @field:SerializedName("menu_item_instructions")
    val menuItemInstructions: String? = null,

    @field:SerializedName("order_type_id")
    val orderTypeId: Int? = null,

    @field:SerializedName("user_id")
    val userId: String? = null,

    @field:SerializedName("initiated_id")
    val initiatedId: String? = null,

    @field:SerializedName("mode_id")
    val modeId: Int? = null,

    @field:SerializedName("menu_item_quantity")
    val menuItemQuantity: Int? = null,

    @field:SerializedName("menu_item_modifiers")
    val menuItemModifiers: List<MenuItemModifiersItemRequest>? = null,

    @field:SerializedName("location_id")
    val locationId: Int? = null,

    @field:SerializedName("menu_id")
    val menuId: Int? = null,

    @field:SerializedName("cart_group_id")
    val cartGroupId: Int? = null,

    @field:SerializedName("menu_item_redemption")
    val menuItemRedemption: Boolean? = null,

    @field:SerializedName("menu_item_comp")
    val menuItemComp: Boolean? = null,

    )


data class CompProductRequest(
    @field:SerializedName("cart_id")
    val cartId: Int? = null,

    @field:SerializedName("menu_item_comp")
    val menuItemComp: Boolean? = null,
    @field:SerializedName("menu_item_full_comp")
    val menuItemFullComp: Boolean? = null,

    @field:SerializedName("menu_item_comp_reason")
    val menuItemCompReason: String? = null,

    )

data class MenuItemModifiersItemRequest(

    @field:SerializedName("select_max")
    val selectMax: Int? = null,

    @field:SerializedName("is_required")
    val isRequired: Int? = null,

    @field:SerializedName("active")
    val active: Boolean? = null,

    @field:SerializedName("id")
    val id: Int? = null,

    @field:SerializedName("modification_text")
    val modificationText: String? = null,

    @field:SerializedName("select_min")
    val selectMin: Int? = null,

    @field:SerializedName("type")
    val type: String? = null,

    @field:SerializedName("options")
    val options: List<OptionsItemRequest>? = null,

) {
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
                        append("(${options[i].optionPrice?.div(100)?.toDouble().toDollar()})")
                    } else {
                        append("")
                    }
                }
            }
        }
        return selectedItemStringBuilder.toString()
    }
}

data class MenuItemModifiersGuestItemRequest(

    @field:SerializedName("select_max")
    val selectMax: Int? = null,

    @field:SerializedName("is_required")
    val isRequired: Int? = null,

    @field:SerializedName("pmg_active")
    val pmgActive: Int? = null,

    @field:SerializedName("product_id")
    val productId: Int? = null,

    @field:SerializedName("options")
    val options: OptionsItemRequest? = null,

    @field:SerializedName("active")
    val active: Int? = null,

    @field:SerializedName("id")
    val id: Int? = null,

    @field:SerializedName("modification_text")
    val modificationText: String? = null,

    @field:SerializedName("select_min")
    val selectMin: Int? = null
)

data class ModifiersItem(

    @field:SerializedName("date_updated")
    val dateUpdated: Any? = null,

    @field:SerializedName("date_created")
    val dateCreated: Any? = null,

    @field:SerializedName("pmg_active")
    val pmgActive: Int? = null,

    @field:SerializedName("mod_group_id")
    val modGroupId: Any? = null,

    @field:SerializedName("active")
    val active: Boolean? = null,

    @field:SerializedName("product_category_id")
    val productCategoryId: Int? = null,

    @field:SerializedName("select_max")
    val selectMax: Int? = null,

    @field:SerializedName("is_required")
    val isRequired: Boolean? = null,

    @field:SerializedName("product_id")
    val productId: Int? = null,

    @field:SerializedName("options")
    var options: List<OptionsItem>? = null,

    @field:SerializedName("id")
    val id: Int? = null,

    @field:SerializedName("modification_text")
    val modificationText: String? = null,

    @field:SerializedName("select_min")
    val selectMin: Int? = null,

    var selectedOption: Int? = 0,
    var selectedOptionsItem: OptionsItemRequest? = null
)

data class AddToCartResponse(

    @field:SerializedName("cart")
    val cart: AddToCartDetails? = null,

    @field:SerializedName("id")
    val id: Int,

    @field:SerializedName("cart_group")
    val cartGroup: CartGroup? = null,

    @field:SerializedName("menu_item_quantity")
    val menuItemQuantity: Int? = null,
)

data class EmployeeDiscountResponse(

    @field:SerializedName("discount")
    val discount: Int? = null,
)
@Parcelize
data class CartGroup(

    @field:SerializedName("id")
    val id: Int? = null,

    @field:SerializedName("promised_time")
    val promisedTime: String? = null,

    @field:SerializedName("status")
    val status: String? = null,

    @field:SerializedName("cart_created_date")
    val cartCreatedDate: String? = null,

    @field:SerializedName("cart")
    val cart: List<com.hotbox.terminal.api.order.model.CartItem>? = null
) :Parcelable

data class UpdateMenuItemQuantity(

    @field:SerializedName("cart_id")
    val cartId: Int? = null,

    @field:SerializedName("menu_item_quantity")
    val menuItemQuantity: Int? = null,

    @field:SerializedName("menu_item_instructions")
    val menuItemInstructions: String? = null,

    @field:SerializedName("menu_item_modifiers")
    val menuItemModifiers: List<MenuItemModifiersItemRequest>? = null,

    @field:SerializedName("menu_item_redemption")
    val menuItemRedemption:Boolean? = null


)

data class DeleteCartItemRequest(
    @field:SerializedName("id")
    val id: Int? = null
)


data class AddToCartDetails(


    @field:SerializedName("menu_item_quantity")
    val menuItemQuantity: Int? = null,

    @field:SerializedName("menu_item_price")
    val menuItemPrice: Double? = null,

    @field:SerializedName("id")
    val id: Int? = null,

    @field:SerializedName("menu_item_instructions")
    val menuItemInstructions: Any? = null,

    @field:SerializedName("menu_item_modifiers")
    val menuItemModifiers: String? = null,

    @field:SerializedName("menu_id")
    val menuId: Int? = null
) {
    fun getData(): List<MenuItemModifiersItemRequest> {
        val gson = Gson()
        val jsonOutput = menuItemModifiers
        val listType: Type = object : TypeToken<List<MenuItemModifiersItemRequest>>() {}.type
        val posts: List<MenuItemModifiersItemRequest> = gson.fromJson(jsonOutput, listType)

        return posts
    }
}

data class CartInfoDetails(

    @field:SerializedName("charges")
    val charges: Charges? = null,

    @field:SerializedName("tip")
    val tip: List<String>? = null,

    @field:SerializedName("cart")
    val cart: List<CartItem>? = null,

    @field:SerializedName("cart_group")
    val cartGroup: CartGroup? = null,
)

data class Charges(

    @field:SerializedName("delivery_fee")
    val deliveryFee: Int? = null,

    @field:SerializedName("tax")
    val tax: Double? = null
)

data class CartItem(

    @field:SerializedName("promised_time")
    val promisedTime: String? = null,

    @field:SerializedName("order_type_id")
    val orderTypeId: Int? = null,

    @field:SerializedName("product_image")
    val productImage: String? = null,

    @field:SerializedName("product_type_id")
    val productTypeId: Int? = null,

    @field:SerializedName("menu_item_instructions")
    val menuItemInstructions: String? = null,

    @field:SerializedName("menu_item_comp_reason")
    val menuItemCompReason: String? = null,

    @field:SerializedName("product_name")
    val productName: String? = null,

    @field:SerializedName("location_id")
    val locationId: Int? = null,

    @field:SerializedName("menu_item_redemption")
    var menuItemRedemption: Int? = 0,

    @field:SerializedName("cart_group_id")
    val cartGroupId: Int? = null,

    @field:SerializedName("menu_item_quantity")
    var menuItemQuantity: Int? = null,

    @field:SerializedName("menu_item_price")
    var menuItemPrice: Double? = null,

    @field:SerializedName("menu_item_value")
    var menuItemValue: Double? = null,

    @field:SerializedName("id")
    val id: Int? = null,

    @field:SerializedName("menu_item_modifiers")
    val menuItemModifiers: List<MenuItemModifiersCart>? = null,

    @field:SerializedName("product_description")
    val productDescription: String? = null,

    @field:SerializedName("menu_id")
    val menuId: Int? = null,

    @field:SerializedName("menu_item_comp")
    val menuItemComp: Int? = 0,

    @field:SerializedName("menu")
    val menu: Menu? = null,

    var isChanging: Boolean? = true,
    var compReason: CompReasonType? = null,
    var isVisibleComp :Boolean? = false
)
@Parcelize
data class MenuItemModifiersCart(

    @field:SerializedName("select_max")
    val selectMax: Int? = null,

    @field:SerializedName("is_required")
    val isRequired: Int? = null,

    @field:SerializedName("active")
    val active: Boolean? = null,

    @field:SerializedName("id")
    val id: Int? = null,

    @field:SerializedName("modification_text")
    val modificationText: String? = null,

    @field:SerializedName("select_min")
    val selectMin: Int? = null,

    @field:SerializedName("type")
    val type: String? = null,

    @field:SerializedName("options")
    val options: List<OptionsItem>? = null,
) :Parcelable {
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


data class GroupCart(

    @field:SerializedName("options")
    val options: List<OptionsItem>? = null,

    @field:SerializedName("mod_group_name")
    val modGroupName: String? = null,

    @field:SerializedName("active")
    val active: Boolean? = null,

    @field:SerializedName("id")
    val id: Int? = null,

    var selectedOptionsItem: ArrayList<OptionsItemRequest>? = null
) {
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
@Parcelize
data class Menu(

    @field:SerializedName("product")
    val product: Product? = null,

    @field:SerializedName("id")
    val id: Int? = null
):Parcelable
@Parcelize
data class Product(

    @field:SerializedName("product_cals")
    val productCals: Int? = null,

    @field:SerializedName("product_image")
    val productImage: String? = null,

    @field:SerializedName("id")
    val id: String? = null,

    @field:SerializedName("product_description")
    val productDescription: String? = null,

    @field:SerializedName("product_name")
    val productName: String? = null,

    @field:SerializedName("product_base_price")
    val productBasePrice: Double? = null,

    @field:SerializedName("product_loyalty_tier")
    val productLoyaltyTier: ProductLoyaltyTier? = null,
):Parcelable

@Parcelize
data class ProductLoyaltyTier(

    @field:SerializedName("tier_value")
    val tierValue: Int? = 0,

    @field:SerializedName("id")
    val id: Int? = null
):Parcelable

data class CreateOrderRequest(

    @field:SerializedName("user_id")
    val userId: String? = null,

    @field:SerializedName("order_mode_id")
    val orderModeId: Int? = null,

    @field:SerializedName("order_tip")
    val orderTip: Int? = null,

    @field:SerializedName("order_type_id")
    val orderTypeId: Int? = null,

    @field:SerializedName("order_cart_group_id")
    val orderCartGroupId: Int? = null,

    @field:SerializedName("gift_card_id")
    val giftCardId: String? = null,

    @field:SerializedName("order_gift_card_amount")
    val orderGiftCardAmount: Int? = null,
    @field:SerializedName("order_adjustment_amount")
    val orderAdjustmentAmount: Int? = null,

    @field:SerializedName("coupon_code_id")
    val couponCodeId: Int? = null,

    @field:SerializedName("order_instructions")
    val orderInstructions: String? = null,

    @field:SerializedName("order_promised_time")
    val orderPromisedTime: String? = null,

    @field:SerializedName("order_delivery_fee")
    val orderDeliveryFee: Int? = null,

    @field:SerializedName("order_tax")
    val orderTax: Double? = null,

    @field:SerializedName("order_location_id")
    val orderLocationId: Int? = null,

    @field:SerializedName("transaction_amount")
    val orderTotal: Int? = null,

    @field:SerializedName("transaction_total_amount")
    val transactionTotalAmount: Int? = null,

    @field:SerializedName("emp_discount")
    val orderSubtotal: Int? = null,

    @field:SerializedName("customer_id")
    val customerId: String? = null,

    @field:SerializedName("delivery_tier")
    val deliveryTier: String? = null,

    @field:SerializedName("transaction_id")
    val transactionId: String? = null,

    @field:SerializedName("lat")
    val lat: String? = null,

    @field:SerializedName("long")
    val long: String? = null,

    @field:SerializedName("delivery_address")
    val deliveryAddress: String? = null,

    @field:SerializedName("guest_first_name")
    val guestFirstName: String? = null,

    @field:SerializedName("guest_last_name")
    val guestLastName: String? = null,

    @field:SerializedName("guest_name")
    val guestName: String? = null,

    @field:SerializedName("guest_phone")
    val guestPhone: String? = null,

    @field:SerializedName("guest_email")
    val guestEmail: String? = null,

    @field:SerializedName("credit_amount")
    val creditAmount: Int? = null,

    @field:SerializedName("transaction_terminal")
    val transactionTerminal: String? = null,

    @field:SerializedName("transaction_id_of_processor")
    val transactionIdOfProcessor: String? = null,

    @field:SerializedName("transaction_charge_id")
    val transactionChargeId: String? = null,

    @field:SerializedName("transaction_receipt_url")
    val transactionReceiptUrl: String? = null,

    )
data class GetPromisedTime(

    @field:SerializedName("time")
    val time: String? = null
)

data class OrderPrice(
    var orderTotal: Double? = null,
    var orderSubtotal: Double? = null,
    var orderTax: Double? = null,
    var employeeDiscount: Int? = 0,
    var adjustmentDiscount: Int? = 0
)

data class UserDetails(
    val name: String? = null,
    val surName: String? = null,
    val phone: String? = null,
    val email: String? = null,
)


data class Group(

    @field:SerializedName("mod_group_name")
    val modGroupName: String? = null,

    @field:SerializedName("options")
    val options: List<OptionsItemRequest>? = null,

    @field:SerializedName("active")
    val active: Boolean? = null,

    @field:SerializedName("id")
    val id: Int? = null
)

data class OptionsItemRequest(

    @field:SerializedName("option_image")
    val optionImage: String? = null,

    @field:SerializedName("option_price")
    val optionPrice: Int? = null,

    @field:SerializedName("active")
    val active: Boolean? = null,

    @field:SerializedName("option_name")
    val optionName: String? = null,

    @field:SerializedName("id")
    val id: Int? = null
)

data class MenuItemModifiersItemRequested(

    @field:SerializedName("select_max")
    val selectMax: Int? = null,

    @field:SerializedName("is_required")
    val isRequired: Int? = null,

    @field:SerializedName("active")
    val active: Boolean? = null,

    @field:SerializedName("id")
    val id: Int? = null,

    @field:SerializedName("modification_text")
    val modificationText: String? = null,

    @field:SerializedName("select_min")
    val selectMin: Int? = null,

    @field:SerializedName("group")
    val group: Group? = null
)


data class UpdateCartResponse(

    @field:SerializedName("menu_item_value")
    val menuItemValue: Int? = null,

    @field:SerializedName("menu_item_quantity")
    val menuItemQuantity: Int? = null,

    @field:SerializedName("menu_item_redemption")
    val menuItemRedemption: Boolean? = false,

    @field:SerializedName("id")
    val id: Int? = null,

    @field:SerializedName("menu_item_modifiers")
    val menuItemModifiers: List<MenuItemModifiersItem>? = null
)

@Parcelize
data class OrderMode(

    @field:SerializedName("id")
    val id: Int? = null,

    @field:SerializedName("mode_name")
    val modeName: String? = null
): Parcelable
@Parcelize
data class OrderType(

    @field:SerializedName("id")
    val id: Int? = null,

    @field:SerializedName("subcategory")
    val subcategory: String? = null,

    @field:SerializedName("is_delivery")
    val isDelivery: Boolean? = null
) : Parcelable
@Parcelize
data class Transaction(

    @field:SerializedName("transaction_status")
    val transactionStatus: String? = null,

    @field:SerializedName("transaction_charge_id")
    val transactionChargeId: String? = null,

    @field:SerializedName("transaction_amount")
    val transactionAmount: Int? = null,

    @field:SerializedName("transaction_time")
    val transactionTime: String? = null,

    @field:SerializedName("transaction_receipt_url")
    val transactionReceiptUrl: String? = null,

    @field:SerializedName("id")
    val id: Int? = null,

    @field:SerializedName("transaction_method")
    val transactionMethod: String? = null,

    @field:SerializedName("transaction_currency")
    val transactionCurrency: String? = null
): Parcelable
@Parcelize

data class GiftCard(

    @field:SerializedName("id")
    val id: Int? = null,

    @field:SerializedName("gift_card_code")
    val giftCardCode: String? = null
): Parcelable

@Parcelize
data class CreateOrderResponse(

    @field:SerializedName("coupon_code")
    val couponCode: String? = null,

    @field:SerializedName("order_tip")
    val orderTip: Int? = null,

    @field:SerializedName("gift_card")
    val giftCard: GiftCard? = null,

    @field:SerializedName("order_mode")
    val orderMode: OrderMode? = null,

    @field:SerializedName("order_gift_card_amount")
    val orderGiftCardAmount: Int? = null,

    @field:SerializedName("cart_group")
    val cartGroup: CartGroup? = null,

    @field:SerializedName("order_location")
    val orderLocation: OrderLocation? = null,

    @field:SerializedName("order_instructions")
    val orderInstructions: String? = null,

    @field:SerializedName("order_promised_time")
    val orderPromisedTime: String? = null,

    @field:SerializedName("order_delivery_fee")
    val orderDeliveryFee: Int? = null,

    @field:SerializedName("order_tax")
    val orderTax: Int? = null,

    @field:SerializedName("order_coupon_code_discount")
    val orderCouponCodeDiscount: Int? = null,

    @field:SerializedName("order_emp_discount")
    val orderEmpDiscount: Double? = 0.00,

    @field:SerializedName("order_refund_amount")
    val orderRefundAmount: Double? = 0.00,

    @field:SerializedName("order_adjustment_amount")
    val orderAdjustmentAmount: Double? =  0.00,

    @field:SerializedName("order_creation_date")
    val orderCreationDate: String? = null,

    @field:SerializedName("order_credit_amount")
    val orderCreditAmount: Int? = null,

    @field:SerializedName("order_total")
    var orderTotal: Double? = null,

    @field:SerializedName("id")
    val id: Int? = null,

    @field:SerializedName("order_subtotal")
    val orderSubtotal: Int? = null,

    @field:SerializedName("order_type")
    val orderType: OrderType? = null,

    @field:SerializedName("transaction")
    val transaction: Transaction? = null
) : Parcelable
@Keep
enum class CompReasonType(val type: String, val displayType: String) {
    LongTicketTime("long_ticket_time", "Long Ticket Time"),
    WrongToGoFood("wrong_to-go_food", "Wrong To Go Food"),
    DidNotLike("did_not_like", "Did Not Like"),
    ForeignObject("foreign_object", "Foreign Object"),
    Spill("spill", "Spill"),
    Management("management", "Management"),
    MissedItem("missed_item", "Missed Item"),
    Training("training", "Training"),
    Marketing("marketing", "Marketing"),
    EntryError("entry_error", "Entry Error"),
    EightySix("eightySix", "Eighty Six"),
    GuestChangedMind("guest_changed_mind", "Guest Changed Mind"),
    Test("test", "Test"),

}
@Keep
enum class AdjustmentType {
    ADJUSTMENT_POSITIVE_TYPE,
    ADJUSTMENT_NEGATIVE_TYPE,

}


sealed class CartItemClickStates{
    data class CartItemAdditionClick(val data :CartItem) :CartItemClickStates()
    data class CartItemSubscriptionClick(val data :CartItem) :CartItemClickStates()
    data class CartItemDeleteClick(val data :CartItem) :CartItemClickStates()
    data class CartItemEditClick(val data :CartItem) :CartItemClickStates()
    data class CartItemCompProductReasonClick(val data :CartItem) :CartItemClickStates()
    data class CartItemConfirmButtonClick(val data :CartItem) :CartItemClickStates()
    data class RedeemProductClick(val data :CartItem) :CartItemClickStates()
}
