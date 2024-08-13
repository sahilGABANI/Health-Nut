package com.hotbox.terminal.api.store.model

import android.location.Location
import com.google.gson.annotations.SerializedName
import com.hotbox.terminal.api.authentication.model.HealthNutUser
import com.hotbox.terminal.api.authentication.model.LocationResponse
import com.hotbox.terminal.api.authentication.model.Role
import com.hotbox.terminal.helper.formatTo
import com.hotbox.terminal.helper.toDate
import timber.log.Timber

data class StoreResponse(

    @field:SerializedName("is_thursday_closed")
    val isThursdayClosed: Int? = null,

    @field:SerializedName("is_saturday_closed")
    val isSaturdayClosed: Int? = null,

    @field:SerializedName("wednesday_open_time")
    val wednesdayOpenTime: String? = null,

    @field:SerializedName("friday_close_time")
    val fridayCloseTime: String? = null,

    @field:SerializedName("friday_open_time")
    val fridayOpenTime: String? = null,

    @field:SerializedName("employee")
    val employee: List<AssignedEmployeeInfo>? = null,

    @field:SerializedName("is_wednesday_closed")
    val isWednesdayClosed: Int? = null,

    @field:SerializedName("location_id")
    val locationId: Int? = null,

    @field:SerializedName("saturday_open_time")
    val saturdayOpenTime: String? = null,

    @field:SerializedName("location_location_description")
    val locationLocationDescription: String? = null,

    @field:SerializedName("monday_close_time")
    val mondayCloseTime: String? = null,

    @field:SerializedName("is_friday_closed")
    val isFridayClosed: Int? = null,

    @field:SerializedName("thursday_open_time")
    val thursdayOpenTime: String? = null,

    @field:SerializedName("saturday_close_time")
    val saturdayCloseTime: String? = null,

    @field:SerializedName("location_location_address_2")
    val locationLocationAddress2: String? = null,

    @field:SerializedName("location_location_address_1")
    val locationLocationAddress1: String? = null,

    @field:SerializedName("location_location_zip")
    val locationLocationZip: String? = null,

    @field:SerializedName("thursday_close_time")
    val thursdayCloseTime: String? = null,

    @field:SerializedName("location_location_phone")
    val locationLocationPhone: String? = null,

    @field:SerializedName("tuesday_open_time")
    val tuesdayOpenTime: String? = null,

    @field:SerializedName("location_location_state")
    val locationLocationState: String? = null,

    @field:SerializedName("location_location_country")
    val locationLocationCountry: String? = null,

    @field:SerializedName("sunday_open_time")
    val sundayOpenTime: String? = null,

    @field:SerializedName("location_location_name")
    val locationLocationName: String? = null,

    @field:SerializedName("is_sunday_closed")
    val isSundayClosed: Int? = null,

    @field:SerializedName("tuesday_close_time")
    val tuesdayCloseTime: String? = null,

    @field:SerializedName("location_location_city")
    val locationLocationCity: String? = null,

    @field:SerializedName("is_tuesday_closed")
    val isTuesdayClosed: Int? = null,

    @field:SerializedName("wednesday_close_time")
    val wednesdayCloseTime: String? = null,

    @field:SerializedName("is_monday_closed")
    val isMondayClosed: Int? = null,

    @field:SerializedName("location_location_timezone")
    val locationLocationTimezone: String? = null,

    @field:SerializedName("monday_open_time")
    val mondayOpenTime: String? = null,

    @field:SerializedName("sunday_close_time")
    val sundayCloseTime: String? = null
) {
    fun getStoreShiftTime(): List<StoreShiftTime> {
        val listOfShift = mutableListOf<StoreShiftTime>()
        listOfShift.add(StoreShiftTime("Sunday", getShiftTime(sundayOpenTime, sundayCloseTime,isSundayClosed)))
        listOfShift.add(StoreShiftTime("Monday", getShiftTime(mondayOpenTime, mondayCloseTime,isMondayClosed)))
        listOfShift.add(StoreShiftTime("Tuesday", getShiftTime(tuesdayOpenTime, tuesdayCloseTime,isTuesdayClosed)))
        listOfShift.add(StoreShiftTime("Wednesday", getShiftTime(wednesdayOpenTime, wednesdayCloseTime,isWednesdayClosed)))
        listOfShift.add(StoreShiftTime("Thursday", getShiftTime(thursdayOpenTime, thursdayCloseTime,isThursdayClosed)))
        listOfShift.add(StoreShiftTime("Friday", getShiftTime(fridayOpenTime, fridayCloseTime,isFridayClosed)))
        listOfShift.add(StoreShiftTime("Saturday", getShiftTime(saturdayOpenTime, saturdayCloseTime,isSaturdayClosed)))
        return listOfShift
    }

    private fun getShiftTime(daysOpen: String?, daysClose: String?,dayIsClose: Int?): String {
        return if (dayIsClose == 1) {
            "Closed"
        } else {
            try {
                daysOpen?.toDate("hh:mm:ss")?.formatTo("hh:mm a") + " - " + daysClose?.toDate("hh:mm:ss")?.formatTo("hh:mm a")
            } catch (e: Exception) {
                Timber.e(e)
                ""
            }
        }
    }

    fun getSafeAddressName(): String {
        val addressStringBuilder = StringBuilder().apply {
            if (locationLocationAddress1 != null && locationLocationAddress1.isNotEmpty()) {
                append(locationLocationAddress1)
            }
            if (locationLocationAddress2 != null && !locationLocationAddress2.isNullOrEmpty()) {
                append(locationLocationAddress2)
            }
            if (locationLocationCity != null && locationLocationCity.isNotEmpty()) {
                append(", $locationLocationCity")
            }
            if (locationLocationState != null && locationLocationState.isNotEmpty()) {
                append(", $locationLocationState")
            }
            if (locationLocationZip != null && locationLocationZip.isNotEmpty()) {
                append(", $locationLocationZip")
            }
        }
        return addressStringBuilder.toString()
    }

    fun getSafePhoneNumber(): String {
        return if (locationLocationPhone == "") {
            "-"
        } else {
            locationLocationPhone ?: "-"
        }

    }
}

data class BufferResponse(

    @field:SerializedName("id")
    val id: Int? = null,

    @field:SerializedName("location_takeout_buffer")
    val locationTakeoutBuffer: Int? = null,

    @field:SerializedName("location_delivery_buffer")
    val locationDeliveryBuffer: Int? = null
) {
    fun getSafeTakeOutBufferTime(): Int {
        return locationTakeoutBuffer ?: 0
    }

    fun getSafeDeliveryBufferTime(): Int {
        return locationDeliveryBuffer ?: 0
    }
}

data class BufferTimeRequest(
    @field:SerializedName("location_id")
    val locationId: Int,

    @field:SerializedName("type")
    val type: String,

    @field:SerializedName("operator")
    val operator: String
)

data class EmployeeInfo(

    @field:SerializedName("employee")
    val roles: List<AssignedEmployeeInfo>? = null
)


data class AssignedEmployeeInfo(
    @field:SerializedName("role")
    val role: Role? = null,

    @field:SerializedName("location")
    val location: LocationResponse? = null,

    @field:SerializedName("assigned")
    val assigned: String? = null,

    @field:SerializedName("active")
    val active: Boolean? = false,

    @field:SerializedName("id")
    val id: Int? = null,

    @field:SerializedName("user")
    val user: HealthNutUser? = null
) {
    fun getSafeFullNameWithRoleName(): String {
        val employeeFullNameWithRoleNameStringBuilder = StringBuilder().apply {
            if (role?.roleName != null) {
                append(role.roleName)
            }
            if (user?.firstName != null) {
                append(" - ${user?.firstName}")
            }
            if (user?.lastName != null) {
                append(" ${user?.lastName}")
            }
        }
        return employeeFullNameWithRoleNameStringBuilder.toString()
    }

    fun getSafeFullName(): String {
        val employeeFullNameWithRoleNameStringBuilder = StringBuilder().apply {
            if (user?.firstName != null) {
                append("${user.firstName}")
            }
            if (user?.lastName != null) {
                append(" ${user.lastName}")
            }
        }
        return employeeFullNameWithRoleNameStringBuilder.toString()
    }
}

data class UpdatePrintStatusRequest(

    @field:SerializedName("serial_number")
    val serialNumber: String? = null,

    @field:SerializedName("order_id")
    val orderId: Int? = null
)


data class StoreShiftTime(
    val dayOfWeek: String,
    val time: String
)

const val PICKUP_BUFFER_TYPE = "takeout"
const val DELIVERY_BUFFER_TYPE = "delivery"
const val BUFFER_TIME_PLUSH = "+"
const val BUFFER_TIME_MINUS = "-"