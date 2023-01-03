package com.nct.xmusicstation.data.model.auth

import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName

/**
 * Created by Toan.IT on 4/22/15.
 * Email:Huynhvantoan.itc@gmail.com
 */
class NetworkResponse {

    var status: String? = ""
    var token: String = ""
    //var user: UserAccount? = null
    @SerializedName("expires_at")
    var expiresAt: Long = 0
    var error: NetworkError? = null
    var data: JsonElement? = null
    //var schedule: List<AlbumSchedule>? = null
    var code: String? = ""
    @SerializedName("expires_in")
    var expiresIn: Int = 0

}
