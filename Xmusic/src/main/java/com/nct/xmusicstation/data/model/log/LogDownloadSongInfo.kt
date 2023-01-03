package com.nct.xmusicstation.data.model.log

import io.realm.RealmObject
import io.realm.annotations.PrimaryKey

open class LogDownloadSongInfo : RealmObject() {
    @PrimaryKey
    var key: String = ""
    var albumId: Int? = 0 //13559
    var isError: Boolean? = false
    var messageError: String? = ""

    override fun toString(): String {
        return "LogDownloadSongInfo(key=$key, albumId=$albumId, isError=$isError)"
    }

}
