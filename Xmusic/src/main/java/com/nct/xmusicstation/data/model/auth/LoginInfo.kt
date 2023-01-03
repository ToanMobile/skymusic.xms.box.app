package com.nct.xmusicstation.data.model.auth

import com.google.gson.annotations.SerializedName

data class LoginInfo(

	@field:SerializedName("expires_at")
	val expiresAt: Long? = null,

	@field:SerializedName("user")
	val user: User? = null,

	@field:SerializedName("status")
	val status: String? = null,

	@field:SerializedName("token")
	val token: String? = null,

	@SerializedName("error")
	val error: Error? = Error()
)