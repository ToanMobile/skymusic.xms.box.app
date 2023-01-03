package com.nct.xmusicstation.data.model.auth
import com.google.gson.annotations.SerializedName


/**
 * Created by Toan.IT on 12/13/17.
 * Email:Huynhvantoan.itc@gmail.com
 */

data class RetrieveModel(
		@SerializedName("status") val status: String? = "", //fail
		@SerializedName("token") val token: String? = "", //token
		@SerializedName("error") val error: Error? = Error()
)
