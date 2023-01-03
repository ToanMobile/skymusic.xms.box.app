package com.nct.xmusicstation.data.model.song
import com.nct.xmusicstation.define.LoudNormDef
import io.realm.RealmObject
import io.realm.annotations.PrimaryKey

/**
 * Created by Toan.IT on 12/13/17.
 * Email:Huynhvantoan.itc@gmail.com
 */

open class AlbumDownload :RealmObject() {
	@PrimaryKey
	var albumId: Int? = 0
	var totalSong: Int? = 0
    var isDownload: Boolean? = false
    var isShuffle: Boolean? = false
    var countError: Int = 0
    override fun toString(): String {
        return "AlbumDownload(albumId=$albumId, totalSong=$totalSong, isDownload=$isDownload, isShuffle=$isShuffle, countError=$countError)"
    }
}

open class SongDetailDownload :RealmObject() {
	@PrimaryKey
	var key: String = ""
	var albumId: Int? = 0 //13559
    var title: String? = "" //Chia Tay
    var artists: String?=""
    var streamUrl: String = "" //http://aredir.nixcdn.com/6b1f8d7823bafc50d446f7a2ee97a92a/5a30f591/NhacCuaTui95 0/ChiaTay-BuiAnhTuan-5183978_hq.mp3
    var isError: Boolean? = false
    var loudNorm: Int? = LoudNormDef.FILE_RAW
    var countError: Int = 0
    override fun toString(): String {
        return "SongDetailDownload(key='$key', albumId=$albumId, title=$title, artists=$artists, streamUrl='$streamUrl', isError=$isError, loudNorm=$loudNorm, countError=$countError)"
    }
}