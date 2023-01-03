package com.nct.xmusicstation.data.model.song

import io.realm.RealmObject


/**
 * Created by Toan.IT on 12/13/17.
 * Email:Huynhvantoan.itc@gmail.com
 */

open class CheckStorage : RealmObject() {
    var isSdCard: Boolean? = false //false

    override fun toString(): String {
        return "CheckStorage(isSdCard=$isSdCard)"
    }

}
