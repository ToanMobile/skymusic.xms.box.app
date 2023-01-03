package com.nct.xmusicstation.data.model.auth
import com.google.gson.annotations.SerializedName


/**
 * Created by Toan.IT on 12/13/17.
 * Email:Huynhvantoan.itc@gmail.com
 */
data class UserInfo(
        @SerializedName("status") val status: String? = "", //ok
        @SerializedName("data") val user: User? = User()
)

data class User(
        @SerializedName("id") val id: Int? = 0, //1835
        @SerializedName("username") val username: String? = "", //xmsdemo
        @SerializedName("fullname") val fullname: String? = "", //XMS Demo
        @SerializedName("email") val email: String? = "",
        @SerializedName("address") val address: String? = "",
        @SerializedName("imageAvatar") val imageAvatar: String? = "", //http://xmusic.img.nixcdn.com/xmusicstation/avatar/2017/08/21/7/0/70abf315d4794 4dfb5315f81ca66fb7f_252_252.jpg
        @SerializedName("imageCover") val imageCover: String? = "",
        @SerializedName("setting") val setting: Int? = 0 //1
)