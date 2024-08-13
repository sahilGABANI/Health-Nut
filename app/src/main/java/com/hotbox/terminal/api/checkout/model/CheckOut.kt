package com.hotbox.terminal.api.checkout.model

import com.google.gson.annotations.SerializedName

data class QRScanResponse(
    @field:SerializedName("phone")
    val phone: String? = null,

    @field:SerializedName("fullName")
    val fullName: String? = null,

    @field:SerializedName("id")
    val id: String? = null,

    @field:SerializedName("email")
    val email: String? = null,

    @field:SerializedName("points")
    var points: Int? = null,

    val lastName :String? = ""
)
data class LoyaltyWithPhoneResponse(

    @field:SerializedName("user_email")
    val userEmail: String? = null,

    @field:SerializedName("data")
    val data: List<LoyaltyPhoneHistoryInfo>? = null,

    @field:SerializedName("user_id")
    val userId: String? = null,

    @field:SerializedName("user_phone")
    val userPhone: String? = null,

    @field:SerializedName("points")
    val points: Int? = null
)

data class LoyaltyPhoneHistoryInfo(

    @field:SerializedName("applied_points")
    val appliedPoints: Int? = null,

    @field:SerializedName("date_created")
    val dateCreated: String? = null,

    @field:SerializedName("id")
    val id: Int? = null,

    @field:SerializedName("order")
    val order: Order? = null
)

data class Order(

    @field:SerializedName("order_total")
    val orderTotal: Int? = null,

    @field:SerializedName("id")
    val id: Int? = null
)

data class HistoryItem(

    @field:SerializedName("applied_points")
    val appliedPoints: Int? = null,

    @field:SerializedName("date_created")
    val dateCreated: String? = null,

    @field:SerializedName("order_id")
    val orderId: Int? = null,

    @field:SerializedName("order_total")
    val orderTotal: Int? = null,

    @field:SerializedName("order")
    val order: Order? = null
)

data class UserLoyaltyPointResponse(

    @field:SerializedName("data")
    val history: List<HistoryItem>? = null,

    @field:SerializedName("points")
    val points: Int? = null
)

data class GiftCardResponse(

    @field:SerializedName("gift_card_amount")
    val  giftCardAmount: Double? = null,

    @field:SerializedName("gift_card_redemption")
    val giftCardRedemption: Int? = null,

    @field:SerializedName("id")
    val id: String? = null
)
data class PromoCodeRequest(

    @field:SerializedName("delivery_fee")
    val deliveryFee: Int? = null,

    @field:SerializedName("coupon")
    val coupon: String? = null,

    @field:SerializedName("subtotal")
    val subtotal: Double? = null,

    @field:SerializedName("cart_group_id")
    val cartGroupId: Int? = null
)
data class PromoCodeResponse(

    @field:SerializedName("valid")
    val valid: Boolean? = null,

    @field:SerializedName("coupon_code_id")
    val couponCodeId: Int? = null,

    @field:SerializedName("deduction_mode")
    val deductionMode: String? = null,

    @field:SerializedName("discount")
    val discount: Int? = null,

    @field:SerializedName("deduction_type")
    val deductionType: String? = null
)

data class CreateUserRequest(
    @field:SerializedName("user_mobile_token")
    val userMobileToken: String? = null,

    @field:SerializedName("subscribed")
    val subscribed: Boolean? = null,

    @field:SerializedName("user_birthday")
    val userBirthday: String? = null,

    @field:SerializedName("user_email")
    val userEmail: String? = null,

    @field:SerializedName("user_password")
    val userPassword: String? = null,

    @field:SerializedName("email_receipt")
    val emailReceipt: Boolean? = null,

    @field:SerializedName("last_name")
    val lastName: String? = null,

    @field:SerializedName("user_phone")
    val userPhone: String? = null,

    @field:SerializedName("text_receipt")
    val textReceipt: Boolean? = null,

    @field:SerializedName("first_name")
    val firstName: String? = null,

    @field:SerializedName("user_company")
    val userCompany: String? = null
)


data class CreateUserResponse(

    @field:SerializedName("user_email")
    val userEmail: String? = null,

    @field:SerializedName("default_perspective")
    val defaultPerspective: String? = null,

    @field:SerializedName("last_name")
    val lastName: String? = null,

    @field:SerializedName("user_last_ordered")
    val userLastOrdered: Any? = null,

    @field:SerializedName("user_marketing")
    val userMarketing: UserMarketing? = null,

    @field:SerializedName("user_last_active")
    val userLastActive: Any? = null,

    @field:SerializedName("token")
    val token: String? = null,

    @field:SerializedName("user_birthday")
    val userBirthday: Any? = null,

    @field:SerializedName("user_active")
    val userActive: Boolean? = null,

    @field:SerializedName("user_qr_code")
    val userQrCode: String? = null,

    @field:SerializedName("user_last_version")
    val userLastVersion: Any? = null,

    @field:SerializedName("user_roles_tags")
    val userRolesTags: UserRolesTags? = null,

    @field:SerializedName("user_phone")
    val userPhone: String? = null,

    @field:SerializedName("id")
    val id: String? = null,

    @field:SerializedName("first_name")
    val firstName: String? = null,

    @field:SerializedName("user_company")
    val userCompany: Any? = null
) {
    fun fullName(): String {
        val customerFullNameStringBuilder = StringBuilder().apply {
            if (firstName != null) {
                append("$firstName")
            }
            if (lastName != null) {
                append(" $lastName")
            }
        }
        return customerFullNameStringBuilder.toString()
    }
}

data class UserCreditResponse(

    @field:SerializedName("credits")
    val credits: Int? = null,

    @field:SerializedName("history")
    val history: List<CreditHistoryItem>? = null
)

data class CreditHistoryItem(

    @field:SerializedName("date_created")
    val dateCreated: String? = null,

    @field:SerializedName("applied_credits")
    val appliedCredits: Int? = null,

    @field:SerializedName("order_id")
    val orderId: Any? = null
)

data class UserMarketing(

    @field:SerializedName("subscribed")
    val subscribed: Boolean? = null,

    @field:SerializedName("email_receipt")
    val emailReceipt: Boolean? = null,

    @field:SerializedName("id")
    val id: Int? = null,

    @field:SerializedName("text_receipt")
    val textReceipt: Boolean? = null
)

data class UserRolesTags(

    @field:SerializedName("is_employee")
    val isEmployee: Boolean? = null,

    @field:SerializedName("emp_pin")
    val empPin: Any? = null,

    @field:SerializedName("notes")
    val notes: Any? = null,

    @field:SerializedName("tax_exempt")
    val taxExempt: Boolean? = null,

    @field:SerializedName("is_ambassador")
    val isAmbassador: Boolean? = null,

    @field:SerializedName("id")
    val id: Int? = null,

    @field:SerializedName("vip")
    val vip: Boolean? = null,

    @field:SerializedName("tax_id")
    val taxId: Any? = null
)

data class GiftCardData(

    @field:SerializedName("gift_card_recipient_email")
    val giftCardRecipientEmail: String? = null,

    @field:SerializedName("gift_card_amout")
    val giftCardAmout: Double? = null,

    @field:SerializedName("gift_card_redemption")
    val giftCardRedemption: Int? = null,

    @field:SerializedName("gift_card_recipient_first_name")
    val giftCardRecipientFirstName: String? = null,

    @field:SerializedName("gift_card_purchaser_last_name")
    val giftCardPurchaserLastName: String? = null,

    @field:SerializedName("gift_card_purchaser_email")
    val giftCardPurchaserEmail: String? = null,

    @field:SerializedName("gift_card_purchased")
    val giftCardPurchased: String? = null,

    @field:SerializedName("gift_card_purchaser_first_name")
    val giftCardPurchaserFirstName: String? = null,

    @field:SerializedName("id")
    val id: String? = null,

    @field:SerializedName("gift_card_code")
    val giftCardCode: String? = null,

    @field:SerializedName("gift_card_recipient_last_name")
    val giftCardRecipientLastName: String? = null
)
