package com.nct.xmusicstation.data.model.auth

import com.google.gson.annotations.SerializedName

/**
* Created by Toan.IT on 2/21/17.
* Email:Huynhvantoan.itc@gmail.com
*/

class NetworkError {

    var code: NetworkErrorCode? = null
    var msg: String? = null

    enum class NetworkErrorCode {
        @SerializedName("3")
        ACCESS_DENIED,
        @SerializedName("102")
        TOKEN_INVALID,
        @SerializedName("104")
        TOKEN_EXPIRED,
        @SerializedName("110")
        LOGIN_CODE_EXPIRED,
        @SerializedName("111")
        LOGIN_CODE_NOT_ASSIGNED,
        @SerializedName("112")
        LOGIN_CODE_INVALID,
        @SerializedName("200")
        USER_NOT_EXISTS,
        @SerializedName("204")
        USER_STOP_WORKING
    }
}
