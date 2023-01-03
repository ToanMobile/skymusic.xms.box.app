package com.nct.xmusicstation.event

import com.toan_itc.core.base.event.Event

/**
 * Created by Toan.IT on 12/13/17.
 * Email:Huynhvantoan.itc@gmail.com
 */

class YoutubeEvent(val isStart: Boolean,val urlStream : String = "") : Event() {
    override fun toString(): String {
        return "YoutubeEvent(isStart=$isStart, urlStream='$urlStream')"
    }
}