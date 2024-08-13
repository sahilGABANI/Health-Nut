package com.hotbox.terminal.api.stripe.model

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

enum class PaymentStatus {
    InProgress,
    Success,
    Failed,
    CancelledByUser,
    DeclineByHostOrCard,
    TimeoutOnUserInput
}

@Keep
enum class SendReceiptType {
    Email,
    Phone,
    EmailAndPhone,
    Nothing

}

data class ResponseItem(

    @field:SerializedName("host_response_code")
    val hostResponseCode: String? = null,

    @field:SerializedName("host_transaction_reference")
    val hostTransactionReference: String? = null,

    @field:SerializedName("status")
    val status: String? = null,

    @field:SerializedName("tip_amount")
    val tipAmount: String? = null,

    @field:SerializedName("total_amount")
    val totalAmount: String? = null,

    @field:SerializedName("authorization_no")
    val authorizationNo: String? = null,

    @field:SerializedName("transaction_amount")
    val transactionAmount: String? = null,

    @field:SerializedName("host_response_text")
    val hostResponseText: String? = null,

    @field:SerializedName("merchant_id")
    val merchantId: String? = null,

    @field:SerializedName("type")
    val type: String? = null,

    @field:SerializedName("tac_denial")
    val tacDenial: String? = null,

    @field:SerializedName("customer_card_description")
    val customerCardDescription: String? = null,

    @field:SerializedName("tac_default")
    val tacDefault: String? = null,

    @field:SerializedName("transaction_time")
    val transactionTime: String? = null,

    @field:SerializedName("customer_language")
    val customerLanguage: String? = null,

    @field:SerializedName("avs_result")
    val avsResult: String? = null,

    @field:SerializedName("terminal_id")
    val terminalId: String? = null,

    @field:SerializedName("transaction_date")
    val transactionDate: String? = null,

    @field:SerializedName("batch_no")
    val batchNo: String? = null,

    @field:SerializedName("cvm_result")
    val cvmResult: String? = null,

    @field:SerializedName("host_response_isocode")
    val hostResponseIsocode: String? = null,

    @field:SerializedName("tac_online")
    val tacOnline: String? = null,
    @field:SerializedName("customer_name")
    val customerName: String? = null,

    @field:SerializedName("reference_no")
    val referenceNo: String? = null
) {
    fun getPaymentStatus(): PaymentStatus {
        return when (status) {
            "cancelled_by_user" -> PaymentStatus.CancelledByUser
            "decline_by_host_or_card" -> PaymentStatus.DeclineByHostOrCard
            "approved" -> PaymentStatus.Success
            "timeout_on_user_input" -> PaymentStatus.TimeoutOnUserInput
            else -> PaymentStatus.InProgress
        }
    }
}


data class CaptureNewPaymentRequest(

    @field:SerializedName("iConnRESTRequest")
    val iConnRESTRequest: IConnRESTRequest? = null,


    @field:SerializedName("endpoint")
    val endpoint: String = "/tsi/v1/payment",

    @field:SerializedName("resource")
    val resource: Resource? = null
)

data class IConnRESTRequest(

    @field:SerializedName("posAccessKey")
    val posAccessKey: String? = null,

    @field:SerializedName("terminalAccessKey")
    val terminalAccessKey: String? = null
)

data class Resource(

    @field:SerializedName("amount")
    val amount: Int,

    @field:SerializedName("type")
    val type: String? = "sale"
)

@Keep
enum class EditType(val type: String, val displayType: String) {
    Email("email", "Email"),
    Phone("phone", "Phone")
}
