package com.nct.xmusicstation.data.model.auth

import com.google.gson.annotations.SerializedName

/**
 * Created by Toan.IT on 4/22/15.
 * Email:Huynhvantoan.itc@gmail.com
 */

data class TokenInfo(
		@SerializedName("status") val status: String = "", //ok
		@SerializedName("token") val token: String = "" //eyJhbGciOiJIUzI1NiJ9...
)