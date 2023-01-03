package com.nct.xmusicstation.event

import com.nct.xmusicstation.data.model.song.Schedule
import com.toan_itc.core.base.event.Event

/**
* Created by Toan.IT on 5/7/17.
* Email:Huynhvantoan.itc@gmail.com
*/

class ScheduleAlbumEvent(val indexAlbumPlay: Int, val albumSchedule: Schedule?): Event() {
    override fun toString(): String {
        return "ScheduleAlbumEvent(indexAlbumPlay=$indexAlbumPlay, albumSchedule=$albumSchedule)"
    }
}
