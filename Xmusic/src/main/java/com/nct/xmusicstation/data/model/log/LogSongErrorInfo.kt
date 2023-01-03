package com.nct.xmusicstation.data.model.log

import io.realm.RealmObject
import io.realm.annotations.PrimaryKey

open class LogSongErrorInfo : RealmObject() {
    @PrimaryKey
    var songId: String? = ""
    var albumId: Int? = 0
    var messageError: String? = ""
    var timestamp: Long? = 0L

    override fun toString(): String {
        return "LogSongErrorInfo(songId=$songId, albumId=$albumId, messageError=$messageError, time=$timestamp)"
    }
}