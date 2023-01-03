package com.nct.xmusicstation.data.model.song
import com.google.gson.annotations.SerializedName
import io.realm.RealmList
import io.realm.RealmObject
import io.realm.annotations.Ignore
import io.realm.annotations.Index
import io.realm.annotations.PrimaryKey

/**
 * Created by Toan.IT on 12/13/17.
 * Email:Huynhvantoan.itc@gmail.com
 */

open class AlbumInfo : RealmObject() {
    @PrimaryKey
    @SerializedName("id")
    var id: Int? = 1
	@SerializedName("status")
	var status: String? = "" //ok
	@SerializedName("schedule")
	var schedule: RealmList<Schedule?>? = RealmList()
	@SerializedName("data")
	var listAlbum: RealmList<ListAlbum?>? = RealmList()
	@SerializedName("setting")
	var setting: Setting? = Setting()
	@SerializedName("totalRecords")
	var totalRecords: Int = 0 //8
	@SerializedName("loadMore")
	var loadMore: Boolean? = false //false
    @SerializedName("update_data")
    var updateData: Int? = 1 //1:update 0: no update

	override fun toString(): String {
		return "AlbumInfo(id=$id, status=$status, setting=$setting, schedule=${schedule?.map { it.toString() }}, listAlbum=${listAlbum?.map { it.toString() }},, totalRecords=$totalRecords, loadMore=$loadMore, updateData=$updateData)"
	}

}

open class Schedule : RealmObject() {
	@PrimaryKey
	@SerializedName("id")
	var id: Int? = 0 //861
	@SerializedName("userId")
	var userId: Int? = 0 //1835
	@SerializedName("albumId")
	var albumId: Int? = 0 //13559
    @SerializedName("schedule_type")
    var scheduleType: String? = "" //13559
	@SerializedName("fromTime")
	var fromTime: String? = "" //06:30
	@SerializedName("toTime")
	var toTime: String? = "" //12:18
	@SerializedName("albumName")
	var albumName: String? = "" //test2
    @SerializedName("ontop")
    var ontop: Boolean = false //false
    @SerializedName("ontime")
    var ontime: Boolean = false //false

	override fun toString(): String {
		return "Schedule(id=$id, userId=$userId, albumId=$albumId, scheduleType=$scheduleType, fromTime=$fromTime, toTime=$toTime, albumName=$albumName, ontop=$ontop, ontime=$ontime)"
	}
}

open class ListAlbum : RealmObject() {
	@PrimaryKey
	@SerializedName("id")
	var id: Int? = 0 //13559
	@SerializedName("userId")
	var userId: Int? = 0 //1835
	@SerializedName("name")
	var name: String? = "" //test2
	@SerializedName("desc")
	var desc: String? = ""
	@SerializedName("image")
	var image: String? = "" //http://xmusic.img.nixcdn.com/xmusicstation/album/2017/11/06/2/a/2af1d2dc8fe14eaa8 04dba51ad12c6f1_600_600.jpg
	@SerializedName("totalSongs")
	var totalSongs: Int? = 0 //2
	@SerializedName("type")
	var type: String? = "" //PRIVATE
	@SerializedName("shuffle")
	var shuffle: Boolean = false //false
	@SerializedName("totalduration")
	var totalduration: Long = 0L //false
	@SerializedName("songDetails")
	var listSongDetails: RealmList<SongDetail>? = RealmList()

	override fun toString(): String {
		return "ListAlbum(id=$id, userId=$userId, name=$name, desc=$desc, image=$image, totalSongs=$totalSongs, type=$type, shuffle=$shuffle, songDetails=${listSongDetails?.map { it.toString() }})"
	}
}

open class SongDetail : RealmObject() {
    var id : Int = 0
	@Index
	var key: String = "0" //u3HcHGo4XNZt
	var title: String? = "" //Chia Tay
	var artists: RealmList<String>? = RealmList()
	var duration: Long = 0L
	var kbit: Int = 0 //224
	var online: Int? = 0 //0normal //1 stream //2 Download
	var streamUrl: String = "" //http://aredir.nixcdn.com/6b1f8d7823bafc50d446f7a2ee97a92a/5a30f591/NhacCuaTui95 0/ChiaTay-BuiAnhTuan-5183978_hq.mp3
	override fun toString(): String {
		return "SongDetail(key='$key', title=$title, online=$online, streamUrl=$streamUrl')"
	}

}

open class Setting : RealmObject() {
	@SerializedName("loudNorm")
	var loudNorm: Boolean = false
	@SerializedName("logOut")
	@Ignore
	val logOut: Boolean = false
	@SerializedName("logOutRemove")
	@Ignore
	val logOutRemove: Boolean = false
	@SerializedName("sendLog")
	@Ignore
	val sendLog: Boolean = false
	@SerializedName("restart")
	@Ignore
	val restart: Boolean = false

	override fun toString(): String {
		return "Setting(loudNorm=$loudNorm, logOut=$logOut, logOutRemove=$logOutRemove, sendLog=$sendLog, restart=$restart)"
	}
}