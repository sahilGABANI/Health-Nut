package com.hotbox.terminal.api.giftcard.model

import com.google.gson.annotations.SerializedName

data class BuyVirtualCardRequest(

	@field:SerializedName("transaction_id")
	val transactionId: String? = null,

	@field:SerializedName("gift_card_recipient_email")
	val giftCardRecipientEmail: String? = null,

	@field:SerializedName("gift_card_amout")
	val giftCardAmout: Int? = null,

	@field:SerializedName("gift_card_recipient_first_name")
	val giftCardRecipientFirstName: String? = null,

	@field:SerializedName("gift_card_purchaser_last_name")
	val giftCardPurchaserLastName: String? = null,

	@field:SerializedName("gift_card_purchaser_email")
	val giftCardPurchaserEmail: String? = null,

	@field:SerializedName("gift_card_purchaser_first_name")
	val giftCardPurchaserFirstName: String? = null,

	@field:SerializedName("gift_card_personal_message")
	val giftCardPersonalMessage: String? = null,

	@field:SerializedName("gift_card_code")
	val giftCardCode: String? = null,

	@field:SerializedName("gift_card_recipient_last_name")
	val giftCardRecipientLastName: String? = null,

	@field:SerializedName("transaction_id_of_processor")
	val transactionIdOfProcessor: String? = null,

	@field:SerializedName("transaction_charge_id")
	val transactionChargeId: String? = null,
)

data class BuyPhysicalCardRequest(

	@field:SerializedName("transaction_id")
	val transactionId: String? = null,

	@field:SerializedName("gift_card_amout")
	val giftCardAmout: Int? = null,

	@field:SerializedName("gift_card_code")
	val giftCardCode: String? = null,

	@field:SerializedName("transaction_id_of_processor")
	val transactionIdOfProcessor: String? = null,

	@field:SerializedName("transaction_charge_id")
	val transactionChargeId: String? = null,
)
