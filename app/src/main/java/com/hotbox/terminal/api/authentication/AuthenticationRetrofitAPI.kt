package com.hotbox.terminal.api.authentication

import com.hotbox.terminal.api.authentication.model.*
import com.hotbox.terminal.base.network.ErrorType
import com.hotbox.terminal.base.network.model.HotBoxResponse
import io.reactivex.Single
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface AuthenticationRetrofitAPI {

    @GET("v1/pos/get-pos-location")
    @ErrorType
    fun getLocation(@Query("serial_number") serialNumber: String): Single<HotBoxResponse<LocationResponse>>

//    @GET("v1/pos/get-pos-location/")
//    @ErrorType
//    fun getLocation(): Single<HotBoxResponse<LocationData>>

    @POST("v1/auth/employee-pin-login")
    @ErrorType
    fun loginCrew(@Body request: LoginCrewRequest): Single<HotBoxResponse<LoginCrewResponse>>

    @POST("v1/pos/available-to-print")
    @ErrorType
    fun availableToPrint(@Body request: AvailableToPrintRequest): Single<HotBoxResponse<AvailableToPrintInfo>>


    @POST("v1/auth/employee-pin-login")
    @ErrorType
    fun checkAdminPin(@Body request: LoginCrewRequest): Single<HotBoxResponse<LoginCrewResponse>>

    @GET("v1/users/get-user")
    @ErrorType
    fun getUserDetails(@Query("id") userId :String?): Single<HotBoxResponse<HealthNutUser>>

    @POST("v1/get-location-by-id")
    @ErrorType
    fun getLocationById()
}