package com.hotbox.terminal.api.authentication

import com.google.gson.Gson
import com.hotbox.terminal.api.authentication.model.*
import com.hotbox.terminal.api.checkout.model.QRScanResponse
import com.hotbox.terminal.api.store.model.StoreResponse
import com.hotbox.terminal.base.prefs.LocalPrefs
import timber.log.Timber

class LoggedInUserCache(private val localPrefs: LocalPrefs) {
    private var loggedInUser: LoggedInUser? = null

    enum class PreferenceKey(val identifier: String) {
        LOGGED_IN_USER_JSON_KEY("loggedInUser"),
        LOGGED_IN_USER_TOKEN("token"),
        LOCATION_INFO("location_info_json"),
        LOGGED_IN_USER_DETAIL_JSON_KEY("logged_in_user_detail"),
        CART_GROUP_ID("cart_group_id"),
        RANDOM_NUMBER("random_number"),
        STRIPE_TOKEN("stripe_token"),
        LOYALTY_USER_DETAILS("loyalty_qr_scan_user_response"),
        ORDER_TYPE_ID("order_type_id"),
        IS_EMPLOYEE_MEAL("is_employee_meal"),
        EMPLOYEE_USER_ID_EMPLOYEE_MEAL("employee_user_id_employee_meal"),
        AUTO_RECEIVER_ID("AutoReceiverId"),
        LOCATION_PHONE("location_phone"),
        STORE_RESPONSE("store_response")
    }

    private var locationInfoLocalPref: LocationResponse?
        get() {
            val locationResponseJson =
                localPrefs.getString(PreferenceKey.LOCATION_INFO.identifier, null)
            try {
                return Gson().fromJson(locationResponseJson, LocationResponse::class.java)
            } catch (e: Exception) {
                Timber.e(e, "Failed to parse logged in user from json string")
            }
            return null
        }
        set(value) {
            val locationResponseJson = Gson().toJson(value)
            localPrefs.putString(PreferenceKey.LOCATION_INFO.identifier, locationResponseJson)
        }

    private var loyaltyUserDetails: QRScanResponse?
        get() {
            val loyaltyQRScanResponseJson =
                localPrefs.getString(PreferenceKey.LOYALTY_USER_DETAILS.identifier, null)
            try {
                return Gson().fromJson(loyaltyQRScanResponseJson, QRScanResponse::class.java)
            } catch (e: Exception) {
                Timber.e(e, "Failed to parse logged in user from json string")
            }
            return null
        }
        set(value) {
            val loyaltyQRScanResponseJson = Gson().toJson(value)
            localPrefs.putString(PreferenceKey.LOYALTY_USER_DETAILS.identifier, loyaltyQRScanResponseJson)
        }

    private var storeInfoLocalPref: StoreResponse?
        get() {
            val storeResponseJson =
                localPrefs.getString(PreferenceKey.STORE_RESPONSE.identifier, null)
            try {
                return Gson().fromJson(storeResponseJson, StoreResponse::class.java)
            } catch (e: Exception) {
                Timber.e(e, "Failed to parse logged in user from json string")
            }
            return null
        }
        set(value) {
            val storeResponseJson = Gson().toJson(value)
            localPrefs.putString(PreferenceKey.STORE_RESPONSE.identifier, storeResponseJson)
        }

    private var loggedInUserTokenLocalPref: String?
        get() {
            return localPrefs.getString(PreferenceKey.LOGGED_IN_USER_TOKEN.identifier, null)
        }
        set(value) {
            localPrefs.putString(PreferenceKey.LOGGED_IN_USER_TOKEN.identifier, value)
        }

    private var cartGroupId: Int?
        get() {
            return localPrefs.getInt(PreferenceKey.CART_GROUP_ID.identifier, 0)
        }
        set(value) {
            value?.let { localPrefs.putInt(PreferenceKey.CART_GROUP_ID.identifier, it) }
        }

    private var random: Int?
        get() {
            return localPrefs.getInt(PreferenceKey.RANDOM_NUMBER.identifier, 0)
        }
        set(value) {
            value?.let { localPrefs.putInt(PreferenceKey.RANDOM_NUMBER.identifier, it) }
        }

    private var orderTypeId: Int?
        get() {
            return localPrefs.getInt(PreferenceKey.ORDER_TYPE_ID.identifier, 0)
        }
        set(value) {
            value?.let { localPrefs.putInt(PreferenceKey.ORDER_TYPE_ID.identifier, it) }
        }

    private var loggedInUserStripeTokenLocalPref: String?
        get() {
            return localPrefs.getString(PreferenceKey.STRIPE_TOKEN.identifier, null)
        }
        set(value) {
            localPrefs.putString(PreferenceKey.STRIPE_TOKEN.identifier, value)
        }

    private var isEmployeeMeal: Boolean?
        get() {
            return localPrefs.getBoolean(PreferenceKey.IS_EMPLOYEE_MEAL.identifier, false)
        }
        set(value) {
            value?.let { localPrefs.putBoolean(PreferenceKey.IS_EMPLOYEE_MEAL.identifier, it) }
        }
    private var employeeUserIdEmployeeMeal: String?
        get() {
            return localPrefs.getString(PreferenceKey.EMPLOYEE_USER_ID_EMPLOYEE_MEAL.identifier, null)
        }
        set(value) {
            value?.let { localPrefs.putString(PreferenceKey.EMPLOYEE_USER_ID_EMPLOYEE_MEAL.identifier, it) }
        }
    private var autoReceiverUserId: String?
        get() {
            return localPrefs.getString(PreferenceKey.AUTO_RECEIVER_ID.identifier, null)
        }
        set(value) {
            value?.let { localPrefs.putString(PreferenceKey.AUTO_RECEIVER_ID.identifier, it) }
        }

    private var locationPhone: String?
        get() {
            return localPrefs.getString(PreferenceKey.LOCATION_PHONE.identifier, null)
        }
        set(value) {
            value?.let { localPrefs.putString(PreferenceKey.LOCATION_PHONE.identifier, it) }
        }


    init {
        loadLoggedInUserFromLocalPrefs()
    }

    fun getLoginUserToken(): String? {
        return loggedInUserTokenLocalPref
    }

    fun isUserLoggedIn(): Boolean {
        return loggedInUser != null
    }

    fun getLoggedInUser(): LoggedInUser? {
        return loggedInUser
    }

    fun getLoggedInUserCartGroupId(): Int? {
        return cartGroupId
    }

    fun setLoggedInUserCartGroupId(cartGroupId: Int) {
        this.cartGroupId = cartGroupId
    }

    fun getLoggedInUserId(): String? {
        return getLoggedInUser()?.crewResponse?.userId
    }

    fun setLoggedInUserRandomNumber(cartGroupId: Int) {
        this.random = cartGroupId
    }

    fun getLoggedInUserRandomNumber(): Int {
        return random ?: 0
    }

    fun getLoggedInUserDetail(): HealthNutUser? {
        return loggedInUser?.hotBoxUser
    }

    fun getLoggedInUserFullName(): String {
        val hotBoxUser = getLoggedInUserDetail()
        return "${hotBoxUser?.firstName ?: ""} ${hotBoxUser?.lastName ?: ""}"
    }

    fun getLoggedInUserRole(): String {
        return getLoggedInUser()?.crewResponse?.role?.roleName ?: ""
    }

    private fun getLoggedInUserRoleId(): Int {
        return getLoggedInUser()?.crewResponse?.role?.id ?: 0
    }

    fun isAdmin(): Boolean {
        return when (getLoggedInUserRoleId()) {
            1, 2, 3, 5
            -> true
            else -> false
        }
    }

    fun setLoggedInUserToken(token: String) {
        loggedInUserTokenLocalPref = token
    }

    fun setLoggedInUserStripeToken(token: String) {
        loggedInUserStripeTokenLocalPref = token
    }

    fun getLoggedInUserStripeToken(): String? {
        return loggedInUserStripeTokenLocalPref
    }

    fun setLoggedInUser(loginCrewResponse: LoginCrewResponse, hotBoxUser: HealthNutUser) {
        localPrefs.putString(
            PreferenceKey.LOGGED_IN_USER_JSON_KEY.identifier,
            Gson().toJson(loginCrewResponse)
        )
        localPrefs.putString(
            PreferenceKey.LOGGED_IN_USER_DETAIL_JSON_KEY.identifier,
            Gson().toJson(hotBoxUser)
        )
        loadLoggedInUserFromLocalPrefs()
    }

    private fun loadLoggedInUserFromLocalPrefs() {
        val userJsonString =
            localPrefs.getString(PreferenceKey.LOGGED_IN_USER_JSON_KEY.identifier, null)
        val userDetailJsonString =
            localPrefs.getString(PreferenceKey.LOGGED_IN_USER_DETAIL_JSON_KEY.identifier, null)
        var loggedInUser: LoggedInUser? = null
        if (userJsonString != null && userDetailJsonString != null) {
            try {
                loggedInUser = LoggedInUser(
                    Gson().fromJson(userJsonString, LoginCrewResponse::class.java),
                    Gson().fromJson(userDetailJsonString, HealthNutUser::class.java)
                )
            } catch (e: Exception) {
                Timber.e(e, "Failed to parse logged in user from json string")
            }
        }
        this.loggedInUser = loggedInUser
    }

    fun clearLoggedInUserLocalPrefs() {
        clearUserPreferences()
    }

    /**
     * Clear previous user preferences, if the current logged in user is different
     */
    private fun clearUserPreferences() {
        try {
            loggedInUser = null
            for (preferenceKey in PreferenceKey.values()) {
                if (preferenceKey.identifier != PreferenceKey.LOCATION_INFO.identifier) localPrefs.removeValue(preferenceKey.identifier)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    fun getLocationInfo(): LocationResponse? {
        return locationInfoLocalPref
    }

    fun setLocationInfo(locationResponse: LocationResponse) {
        locationInfoLocalPref = locationResponse
    }

    fun getLoyaltyQrResponse() :QRScanResponse? {
        return loyaltyUserDetails
    }
    fun setLoyaltyQrResponse(qrScanResponse: QRScanResponse){
        loyaltyUserDetails =  qrScanResponse
    }

    fun getorderTypeId() :Int ? {
        return orderTypeId
    }
    fun setorderTypeId(id: Int){
        orderTypeId =  id
    }

    fun getIsEmployeeMeal() :Boolean ? {
        return isEmployeeMeal
    }
    fun setIsEmployeeMeal(id: Boolean){
        isEmployeeMeal =  id
    }
    fun getemployeeUserIdEmployeeMeal() :String ? {
        return employeeUserIdEmployeeMeal
    }
    fun setemployeeUserIdEmployeeMeal(id: String?){
        employeeUserIdEmployeeMeal =  id
    }

    fun getAutoReceiverId() :String? {
        return autoReceiverUserId
    }
    fun setAutoReceiverId(address :String) {
        autoReceiverUserId = address
    }

    fun getlocationPhone() :String? {
        return locationPhone
    }
    fun setlocationPhone(phone :String) {
        locationPhone = phone
    }
    fun getStoreResponse() :StoreResponse? {
        return storeInfoLocalPref
    }
    fun setStoreResponse(storeResponse :StoreResponse) {
        this.storeInfoLocalPref = storeResponse
    }
}