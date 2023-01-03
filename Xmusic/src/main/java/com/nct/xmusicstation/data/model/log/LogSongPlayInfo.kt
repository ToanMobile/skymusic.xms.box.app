package com.nct.xmusicstation.data.model.log

import io.realm.RealmObject

open class LogSongPlayInfo : RealmObject() {
    var key: String? = ""
    var albumId: Int? = 0
    var timestamp: Long? = 0L

    override fun toString(): String {
        return "LogSongPlayInfo(key=$key, albumId=$albumId, timestamp=$timestamp)"
    }

}