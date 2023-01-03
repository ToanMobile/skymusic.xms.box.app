package com.nct.xmusicstation.event

import com.nct.xmusicstation.data.model.song.SongDetail
import com.toan_itc.core.base.event.Event

/**
 * Created by Toan.IT on 12/20/17.
 * Email:Huynhvantoan.itc@gmail.com
 */


class PlayingSongEvent(val song: SongDetail?, val isDownload : Boolean = false):Event()
