package com.nct.xmusicstation.data.model.auth

import com.google.gson.annotations.SerializedName


/**
 * Created by Toan.IT on 12/13/17.
 * Email:Huynhvantoan.itc@gmail.com
 */
data class UpdateInfo(
        @SerializedName("status") val status: String? = "", //ok
        @SerializedName("data") val update: Update? = Update()
)

data class Update(
        @SerializedName("enable") val enable: Boolean? = false, //false
        @SerializedName("msg") val msg: String? = "", //Nâng cấp v1.0.0 mới nhất nhé.
        @SerializedName("force") val force: Boolean? = false, //true
        @SerializedName("url") val url: String = "", //https://play.google.com/store/apps/details?id=com.nct.xmusicstation
        @SerializedName("urlUpdate") val urlUpdate: String = "",
        @SerializedName("versionUpdate") val versionUpdate: Int = 2,
        @SerializedName("pathUpdate") val pathUpdate: String = "",
        @SerializedName("leastVersion") val leastVersion: String = "", //0.9.0
        @SerializedName("validOsName") val validOsName: Boolean? = false //true
) {
    override fun toString(): String {
        return "Update(enable=$enable, msg=$msg, force=$force, url=$url, leastVersion=$leastVersion, validOsName=$validOsName)"
    }
}