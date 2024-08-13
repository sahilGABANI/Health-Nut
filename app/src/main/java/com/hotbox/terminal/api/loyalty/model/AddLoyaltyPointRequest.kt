package com.hotbox.terminal.api.loyalty.model

import com.google.gson.annotations.SerializedName

data class AddLoyaltyPointRequest(

	@field:SerializedName("user_id")
	val userId: String? = null,

	@field:SerializedName("admin_id")
	val adminId: String? = null,

	@field:SerializedName("order_id")
	val orderId: Int? = null
)
data class Admin(

	@field:SerializedName("id")
	val id: Int? = null
)

data class User(

	@field:SerializedName("id")
	val id: Int? = null
)

data class AddLoyaltyPointResponse(

	@field:SerializedName("applied_points")
	val appliedPoints: Int? = null,

	@field:SerializedName("date_created")
	val dateCreated: String? = null,

	@field:SerializedName("admin")
	val admin: Admin? = null,

	@field:SerializedName("id")
	val id: Int? = null,

	@field:SerializedName("user")
	val user: User? = null,

	@field:SerializedName("order")
	val order: Order? = null
)

data class Order(

	@field:SerializedName("id")
	val id: Int? = null,

	@field:SerializedName("order_total")
	val orderTotal: Int? = null,
)


data class OrderLoyaltyInfo(

	@field:SerializedName("user_email")
	val userEmail: String? = null,

	@field:SerializedName("data")
	val data: List<OrderLoyaltyItem>? = arrayListOf(),

	@field:SerializedName("user_id")
	val userId: String? = null,

	@field:SerializedName("user_phone")
	val userPhone: String? = null,

	@field:SerializedName("points")
	val points: Int? = null
)


data class OrderLoyaltyItem(

	@field:SerializedName("applied_points")
	val appliedPoints: Int? = null,

	@field:SerializedName("date_created")
	val dateCreated: String? = null,

	@field:SerializedName("id")
	val id: Int? = null,

	@field:SerializedName("order")
	val order: Order? = null
)
