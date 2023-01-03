package com.nct.xmusicstation.data.model.auth
import com.google.gson.annotations.SerializedName


/**
 * Created by Toan.IT on 12/13/17.
 * Email:Huynhvantoan.itc@gmail.com
 */

data class GenerateCodeInfo(
		@SerializedName("status") val status: String? = "", //ok
		@SerializedName("code") val code: String? = "", //772968
		@SerializedName("expires_in") val expiresIn: Int? = 0 //300
)