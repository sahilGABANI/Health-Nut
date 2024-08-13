package com.hotbox.terminal.api.stripe

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface ApiService {

    @GET("api/transact.php?type=sale")
    fun performSaleTransaction(
        @Query("poi_device_id") poiDeviceId: String?,
        @Query("security_key") securityKey: String?,
        @Query("amount") amount: Double,
        @Query("poi_prompt_tip") poiPromptTip :Boolean = true,
        @Query("poi_enable_keyed") poiEnableKeyed :Boolean = true,
        @Query("poi_prompt_signature") poiPromptSignature :Boolean = false,
        @Query("poi_prompt_quicktip_percentages") poi_prompt_quicktip_percentages :String = "15.00,18.00,20.00",

        ): Call<String?>
}