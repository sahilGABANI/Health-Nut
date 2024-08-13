package com.hotbox.terminal.api.authentication.model

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

@Keep
data class LoginCrewRequest(
    @field:SerializedName("pin")
    val pin: String,

    @field:SerializedName("location_id")
    val locationId: Int
)

@Keep
data class HotBoxUser(
    @field:SerializedName("user_email")
    val userEmail: String? = null,

    @field:SerializedName("default_perspective")
    val defaultPerspective: String? = null,

    @field:SerializedName("last_name")
    val lastName: String? = null,

    @field:SerializedName("user_creation_date")
    val userCreationDate: String? = null,

    @field:SerializedName("user_last_ordered")
    val userLastOrdered: String? = null,

    @field:SerializedName("user_last_active")
    val userLastActive: String? = null,

    @field:SerializedName("user_mobile_token")
    val userMobileToken: String? = null,

    @field:SerializedName("user_birthday")
    val userBirthday: String? = null,

    @field:SerializedName("user_active")
    val userActive: Int? = null,

    @field:SerializedName("corporate")
    val corporate: Any? = null,

    @field:SerializedName("user_qr_code")
    val userQrCode: String? = null,

    @field:SerializedName("user_phone")
    val userPhone: String? = null,

    @field:SerializedName("id")
    val id: Int,

    @field:SerializedName("first_name")
    val firstName: String? = null,

    @field:SerializedName("user_company")
    val userCompany: String? = null
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



data class UserRolesTags(

    @field:SerializedName("is_employee")
    val isEmployee: Boolean? = null,

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
    val taxId: Any? = null,

    @field:SerializedName("emp_pin")
    val empPin: Int? = 0,

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

data class LoginCrewResponse(

    @field:SerializedName("role")
    val role: Role? = null,

    @field:SerializedName("user_id")
    val userId: String? = null,

    @field:SerializedName("active")
    val active: Boolean? = null,

    @field:SerializedName("is_admin_pin")
    val isAdminPin: Boolean? = null,

    @field:SerializedName("assigned")
    val assigned: String? = null,

    @field:SerializedName("location")
    val location: Location? = null,

    @field:SerializedName("id")
    val id: Int? = null,

    @field:SerializedName("token")
    val token: String? = null,


)

data class Role(

    @field:SerializedName("role_name")
    val roleName: String? = null,

    @field:SerializedName("id")
    val id: Int? = null
)


data class HealthNutUser(

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

    @field:SerializedName("user_birthday")
    val userBirthday: String? = null,

    @field:SerializedName("user_active")
    val userActive: Boolean? = null,

    @field:SerializedName("user_qr_code")
    val userQrCode: String? = null,

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

data class Location(

    @field:SerializedName("location_name")
    val locationName: String? = null,

    @field:SerializedName("id")
    val id: Int? = null
)

data class LocationResponse(

    @field:SerializedName("reader_id")
    val readerId: String? = null,

    @field:SerializedName("location")
    val location: Location? = null,

    @field:SerializedName("id")
    val id: Int? = null,

    @field:SerializedName("guest_mode")
    val guestMode: Boolean? = false,

    @field:SerializedName("print_address")
    val printAddress: String? = null,

    @field:SerializedName("boh_print_address")
    val bohPrintAddress: String? = null,

    @field:SerializedName("poskey")
    val poskey: String? = null,

    @field:SerializedName("terminalkey")
    val terminalkey: String? = null,
)

@Keep
data class LoggedInUser(
    val crewResponse: LoginCrewResponse,
    val hotBoxUser: HealthNutUser,
)


data class AvailableToPrintRequest(

    @field:SerializedName("serial_number")
    val serialNumber: String? = null,

    @field:SerializedName("location_id")
    val locationId: Int? = null,

    @field:SerializedName("status")
    val status: Int? = null
)

data class AvailableToPrintInfo(

    @field:SerializedName("location")
    val location: Location? = null,

    @field:SerializedName("serial_number")
    val serialNumber: String? = null,

    @field:SerializedName("id")
    val id: Int? = null
)