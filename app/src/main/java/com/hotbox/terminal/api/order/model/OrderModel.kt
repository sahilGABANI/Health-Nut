package com.hotbox.terminal.api.order.model

import android.os.Parcelable
import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName
import com.hotbox.terminal.api.authentication.model.HealthNutUser
import com.hotbox.terminal.api.userstore.model.Menu
import com.hotbox.terminal.api.userstore.model.MenuItemModifiersCart
import com.hotbox.terminal.base.extension.toDollar
import kotlinx.android.parcel.Parcelize

@Keep
data class OrderResponse(

    @field:SerializedName("orders")
    val orders: List<OrdersInfo>? = null
)

@Keep
data class OrdersInfo(

    @field:SerializedName("order_promised_time")
    val orderPromisedTime: String? = null,

    @field:SerializedName("order_creation_date")
    val orderCreationDate: String? = null,

    @field:SerializedName("order_total")
    var orderTotal: Int? = null,

    @field:SerializedName("guest")
    val guest: List<GuestItem>? = null,

    @field:SerializedName("id")
    val id: Int? = null,

    @field:SerializedName("order_emp_discount")
    val orderEmpDiscount: Double? = null,

    @field:SerializedName("order_refund_amount")
    val orderRefundAmount: Double? = null,
    @field:SerializedName("order_adjustment_amount")
    val orderAdjustmentAmount: Double? = null,

    @field:SerializedName("order_location")
    val orderLocation: OrderLocation? = null,

    @field:SerializedName("order_type")
    val orderType: OrderType? = null,

    @field:SerializedName("status")
    var status: List<StatusItem>? = arrayListOf(),

    @field:SerializedName("user")
    val user: HealthNutUser? = null,

    @field:SerializedName("order_mode")
    val orderMode: OrderMode? = null,

    @field:SerializedName("cart_group")
    val cartGroup: CartGroup? = null,

    var isSelected: Boolean = false

) {
    fun customerFullName(): String {
        val customerFullNameStringBuilder = StringBuilder().apply {
            if (guest?.isEmpty() == true) {
                if (user?.firstName != null) {
                    append("${user.firstName}")
                }
                if (user?.lastName != null) {
                    append(" ${user.lastName}")
                }
            } else {
                if (guest?.firstOrNull()?.guestFirstName != null) {
                    append("${guest.firstOrNull()?.guestFirstName}")
                }
                if (guest?.firstOrNull()?.guestLastName != null) {
                    append(" ${guest.firstOrNull()?.guestLastName}")
                }
            }

        }
        return customerFullNameStringBuilder.toString()
    }
}

data class OrderType(

    @field:SerializedName("id")
    val id: Int? = null,

    @field:SerializedName("subcategory")
    val subcategory: String? = null,

    @field:SerializedName("is_delivery")
    val isDelivery: Boolean? = null,

    @field:SerializedName("order_type_category")
    val orderTypeCategory: OrderTypeCategory? = null
)
@Parcelize
data class OrderTypeCategory(

    @field:SerializedName("category_name")
    val categoryName: String? = null,

    @field:SerializedName("id")
    val id: Int? = null
) : Parcelable
@Parcelize
data class OrderLocation(

    @field:SerializedName("location_name")
    val locationName: String? = null,

    @field:SerializedName("location_address_1")
    val locationAddress1: String? = null,

    @field:SerializedName("location_address_2")
    val locationAddress2: String? = null,

    @field:SerializedName("location_country")
    val locationCountry: String? = null,

    @field:SerializedName("location_city")
    val locationCity: String? = null,

    @field:SerializedName("id")
    val id: Int? = null,

    @field:SerializedName("location_state")
    val locationState: String? = null,

    @field:SerializedName("location_zip")
    val locationZip: String? = null
): Parcelable

data class GuestItem(

    @field:SerializedName("guest_phone")
    val guestPhone: String? = null,

    @field:SerializedName("guest_first_name")
    val guestFirstName: String? = null,

    @field:SerializedName("id")
    val id: Int? = null,

    @field:SerializedName("guest_last_name")
    val guestLastName: String? = null,

    @field:SerializedName("guest_email")
    val guestEmail: String? = null,
) {
    fun fullName(): String {
        val customerFullNameStringBuilder = StringBuilder().apply {
            if (guestFirstName != null) {
                append("$guestFirstName")
            }
            if (guestLastName != null) {
                append(" $guestLastName")
            }
        }
        return customerFullNameStringBuilder.toString()
    }
}

data class SectionInfo(
    val orderId: String,
    val guest: String,
    val total: String,
    val orderType: String,
    val status: String,
    val promiseTime: String,
    val orderPlace: String
)

data class OrderDetailsInfo(
    val productName: String,
    val productDetails: String,
    val productPrize: String,
    val productQuantity: String,
    val cardBow: String,
    val specialInstructions: String
)

@Keep
data class OrderDetailItem(

    @field:SerializedName("product_image")
    val productImage: String? = null,

    @field:SerializedName("cart_group_id")
    val cartGroupId: Int? = null,

    @field:SerializedName("menu_item_quantity")
    val menuItemQuantity: Int? = null,

    @field:SerializedName("menu_item_price")
    val menuItemPrice: Double? = null,

    @field:SerializedName("id")
    val id: Int? = null,

    @field:SerializedName("menu_item_modifiers")
    val menuItemModifiers: List<MenuItemModifiersItem>? = null,

    @field:SerializedName("menu_item_instructions")
    val menuItemInstructions: Any? = null,

    @field:SerializedName("product_description")
    val productDescription: String? = null,

    @field:SerializedName("product_name")
    val productName: String? = null,

    @field:SerializedName("menu_id")
    val menuId: Int? = null
)

@Keep
data class OrderDetail(
    @field:SerializedName("order_tip")
    val orderTip: Double? = null,

    @field:SerializedName("order_type_id")
    val orderTypeId: Int? = null,

    @field:SerializedName("is_open")
    val isOpen: Boolean? = null,

    @field:SerializedName("order_cart_group_id")
    val orderCartGroupId: Int? = null,

    @field:SerializedName("discount")
    val discount: Int? = null,

    @field:SerializedName("location_state")
    val locationState: String? = null,

    @field:SerializedName("code_name")
    val codeName: Any? = null,

    @field:SerializedName("order_instructions")
    val orderInstructions: String? = null,

    @field:SerializedName("order_promised_time")
    val orderPromisedTime: String? = null,

    @field:SerializedName("order_status")
    val orderStatus: String? = null,

    @field:SerializedName("coupon_code_id")
    val couponCodeId: Any? = null,

    @field:SerializedName("location_name")
    val locationName: String? = null,

    @field:SerializedName("order_delivery_fee")
    val orderDeliveryFee: Double? = null,

    @field:SerializedName("order_tax")
    val orderTax: Double? = null,

    @field:SerializedName("location_address_1")
    val locationAddress1: String? = null,

    @field:SerializedName("location_address_2")
    val locationAddress2: String? = null,

    @field:SerializedName("location_city")
    val locationCity: String? = null,

    @field:SerializedName("order_creation_date")
    val orderCreationDate: String? = null,

    @field:SerializedName("order_total")
    val orderTotal: Double? = null,

    @field:SerializedName("id")
    val id: Int? = null,

    @field:SerializedName("order_subtotal")
    val orderSubtotal: Double? = null,

    @field:SerializedName("location_zip")
    val locationZip: String? = null,

    @field:SerializedName("items")
    val items: List<OrderDetailItem>? = null,

    @field:SerializedName("order_type")
    val orderType: String? = null,

    @field:SerializedName("order_delivery_address")
    val orderDeliveryAddress: String? = null,

    @field:SerializedName("order_gift_card_amount")
    val orderGiftCardAmount: Double? = null,

    @field:SerializedName("credit_amount")
    val creditAmount: Double? = null,

    @field:SerializedName("order_coupon_code_discount")
    val orderCouponCodeDiscount: Double? = null,

    @field:SerializedName("order_status_history")
    val orderStatusHistory: List<StatusItem>? = null,

    @field:SerializedName("guest_name")
    val guestName: String? = null,

    @field:SerializedName("guest_phone")
    val guestPhone: String? = null,

    ) {
    fun getSafeOrderId(): String {
        val orderIdStringBuilder = StringBuilder().apply {
            if (id != null) {
                append("Order #$id")
            }
        }
        return orderIdStringBuilder.toString()
    }
}

data class MenuItemModifiersItem(

    @field:SerializedName("select_max")
    val selectMax: Int? = null,

    @field:SerializedName("is_required")
    val isRequired: Int? = null,

    @field:SerializedName("pmg_active")
    val pmgActive: Int? = null,

    @field:SerializedName("product_id")
    val productId: Int? = null,

    @field:SerializedName("options")
    val options: List<OptionsItem>? = null,

    @field:SerializedName("mod_group_id")
    val modGroupId: Int? = null,

    @field:SerializedName("active")
    val active: Boolean? = null,

    @field:SerializedName("id")
    val id: Int? = null,

    @field:SerializedName("modification_text")
    val modificationText: String? = null,

    @field:SerializedName("select_min")
    val selectMin: Int? = null,

    @field:SerializedName("product_category_id")
    val productCategoryId: Any? = null
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
data class OptionsItem(

    @field:SerializedName("option_price")
    val optionPrice: Double? = null,

    @field:SerializedName("mod_group_id")
    val modGroupId: Int? = null,

    @field:SerializedName("active")
    val active: Boolean? = null,

    @field:SerializedName("option_recommendation")
    val optionRecommendation: Int? = null,

    @field:SerializedName("option_name")
    val optionName: String? = null,

    @field:SerializedName("id")
    val id: Int? = null,

    @field:SerializedName("option_image")
    val optionImage: String? = null,

    var isCheck: Boolean? = false
) :Parcelable

data class StatusLogInfo(

    @field:SerializedName("status")
    val status: List<StatusItem>? = null
)

data class StatusItem(

    @field:SerializedName("order_status")
    val orderStatus: String? = null,

    @field:SerializedName("user_id")
    val userId: Int? = null,

    @field:SerializedName("id")
    val id: Int? = null,

    @field:SerializedName("order_id")
    val orderId: Int? = null,

    @field:SerializedName("timestamp")
    val timestamp: String? = null,

    @field:SerializedName("user")
    var user: HealthNutUser? = null,
) {

}

data class UpdatedOrderStatusResponse(

    @field:SerializedName("order_status")
    val orderStatus: String? = null,

    @field:SerializedName("user_id")
    val userId: Int? = null,

    @field:SerializedName("id")
    val id: Int? = null,

    @field:SerializedName("order_id")
    val orderId: Int? = null,

    @field:SerializedName("timestamp")
    val timestamp: String? = null
)

data class OrderStatusRequest(

    @field:SerializedName("order_status")
    val orderStatus: String? = null,

    @field:SerializedName("user_id")
    val userId: String? = null,

    @field:SerializedName("order_id")
    val orderId: Int? = null
)

data class OrderDetailsResponse(

    @field:SerializedName("delivery")
    val delivery: List<DeliveryItem>? = null,

    @field:SerializedName("coupon_code")
    val couponCode: Any? = null,

    @field:SerializedName("order_tip")
    val orderTip: Double? = null,

    @field:SerializedName("order_refund_amount")
    val orderRefundAmount: Double? = null,

    @field:SerializedName("gift_card")
    val giftCard: Any? = null,

    @field:SerializedName("order_mode")
    val orderMode: OrderMode? = null,

    @field:SerializedName("order_gift_card_amount")
    val orderGiftCardAmount: Double? = null,

    @field:SerializedName("cart_group")
    val cartGroup: CartGroup? = null,

    @field:SerializedName("order_location")
    val orderLocation: OrderLocation? = null,

    @field:SerializedName("order_instructions")
    val orderInstructions: Any? = null,

    @field:SerializedName("order_promised_time")
    val orderPromisedTime: String? = null,

    @field:SerializedName("order_delivery_fee")
    val orderDeliveryFee: Double? = null,

    @field:SerializedName("order_tax")
    val orderTax: Double? = null,

    @field:SerializedName("order_coupon_code_discount")
    val orderCouponCodeDiscount: Double? = null,
    @field:SerializedName("order_emp_discount")
    val orderEmpDiscount: Double? = null,

    @field:SerializedName("order_creation_date")
    val orderCreationDate: String? = null,

    @field:SerializedName("order_credit_amount")
    val orderCreditAmount: Double? = null,


    @field:SerializedName("order_adjustment_amount")
    val orderAdjustmentAmount: Double? = null,

    @field:SerializedName("order_total")
    var orderTotal: Double? = null,

    @field:SerializedName("guest")
    val guest: List<GuestItem?>? = null,

    @field:SerializedName("id")
    val id: Int? = null,

    @field:SerializedName("order_subtotal")
    val orderSubtotal: Double? = null,

    @field:SerializedName("user")
    val user: HealthNutUser? = null,

    @field:SerializedName("order_type")
    val orderType: OrderType? = null,

    @field:SerializedName("status")
    val status: List<StatusItem>? = null,

    @field:SerializedName("transaction")
    val transaction: Transaction? = null
) : java.io.Serializable {
    fun getSafeOrderId(): String {
        val orderIdStringBuilder = StringBuilder().apply {
            if (id != null) {
                append("Order #$id")
            }
        }
        return orderIdStringBuilder.toString()
    }
}

data class Transaction(

    @field:SerializedName("id")
    val id: Int? = null,

    @field:SerializedName("transaction_id_of_processor")
    val transactionIdOfProcessor: String? = null
)
@Parcelize
data class CartItem(

    @field:SerializedName("menu_item_redemption")
    val menuItemRedemption: Int? = null,

    @field:SerializedName("menu_item_quantity")
    val menuItemQuantity: Int? = null,

    @field:SerializedName("menu_item_price")
    val menuItemPrice: Double? = null,

    @field:SerializedName("id")
    val id: Int? = null,

    @field:SerializedName("menu_item_modifiers")
    val menuItemModifiers: List<MenuItemModifiersCart>? = null,

    @field:SerializedName("menu_item_instructions")
    val menuItemInstructions: String? = null,

    @field:SerializedName("menu_item_comp")
    val menuItemComp: Int? = null,

    @field:SerializedName("menu")
    val menu: Menu? = null,
) :Parcelable

data class CartGroup(

    @field:SerializedName("promised_time")
    val promisedTime: String? = null,

    @field:SerializedName("id")
    val id: Int? = null,

    @field:SerializedName("cart_created_date")
    val cartCreatedDate: String? = null,

    @field:SerializedName("cart")
    val cart: List<CartItem>? = null
)

data class OrderMode(

    @field:SerializedName("id")
    val id: Int? = null,

    @field:SerializedName("mode_name")
    val modeName: String? = null
)

data class DeliveryItem(

    @field:SerializedName("order_delivery_estimated_pickup")
    val orderDeliveryEstimatedPickup: String? = null,

    @field:SerializedName("order_delivery_location")
    val orderDeliveryLocation: String? = null,

    @field:SerializedName("order_delivery_instructions")
    val orderDeliveryInstructions: String? = null,

    @field:SerializedName("order_delivery_estimated_dropoff")
    val orderDeliveryEstimatedDropoff: String? = null,

    @field:SerializedName("order_delivery_url")
    val orderDeliveryUrl: String? = null,

    @field:SerializedName("id")
    val id: Int? = null,

    @field:SerializedName("order_delivery_address")
    val orderDeliveryAddress: String? = null,

    @field:SerializedName("order_delivery_apartment_suite")
    val orderDeliveryApartmentSuite: Any? = null
)
data class GetPrintQueueInfo(

    @field:SerializedName("data")
    val data: List<GetPrintQueueItem>? = null
)

data class GetPrintQueueItem(

    @field:SerializedName("serial_number")
    val serialNumber: String? = null,

    @field:SerializedName("id")
    val id: Int? = null,

    @field:SerializedName("order_id")
    val orderId: Int? = null,

    @field:SerializedName("date_printed")
    val datePrinted: Any? = null,

    @field:SerializedName("date_assigned")
    val dateAssigned: String? = null
)

sealed class RefundDialogStates {
    data class DismissedRefundDialog(val data :OrderDetailsResponse) : RefundDialogStates()
    data class GetRefund(val data :OrderDetailsResponse) : RefundDialogStates()
}

sealed class SendReceiptStates {
    data class SendReceiptOnEmail(val email :String) : SendReceiptStates()
    data class SendReceiptOnPhone(val phone :String) : SendReceiptStates()
    data class SendReceiptOnPhoneAndEmail(val email :String,val phone :String) : SendReceiptStates()
}