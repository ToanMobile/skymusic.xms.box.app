package com.nct.xmusicstation.data.model.song
import com.google.gson.annotations.SerializedName


/**
 * Created by Toan.IT on 12/13/17.
 * Email:Huynhvantoan.itc@gmail.com
 */

open class SongDetails{
    @SerializedName("status")
    var status: String? = "" //ok
    @SerializedName("data")
    var song: DataSongDetail? = DataSongDetail()

    override fun toString(): String {
        return "SongDetails(status=$status, song=$song)"
    }
}

open class DataSongDetail{
    @SerializedName("key")
    var key: String = "" //u3HcHGo4XNZt
    @SerializedName("title")
    var title: String? = "" //Chia Tay
    @SerializedName("artists")
    var artists: MutableList<String?> = mutableListOf()
    @SerializedName("streamUrl")
    var streamUrl: String = "" //http://aredir.nixcdn.com/6b1f8d7823bafc50d446f7a2ee97a92a/5a30f591/NhacCuaTui95 0/ChiaTay-BuiAnhTuan-5183978_hq.mp3
    @SerializedName("duration")
    var duration: Int? = 0 //230
    @SerializedName("copyright")
    var copyright: Boolean? = false //false
    @SerializedName("kbit")
    var kbit: Int? = 0 //224
    @SerializedName("online")
    var online: Int? = 0 //1 stream 0//normal

    override fun toString(): String {
        return "SongDetail(key=$key, title=$title, artists=$artists, streamUrl=$streamUrl, duration=$duration, copyright=$copyright, kbit=$kbit, online=$online)"
    }
}