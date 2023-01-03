package com.nct.xmusicstation.event

import com.nct.xmusicstation.define.SDCardDef
import com.toan_itc.core.base.event.Event

/**
 * Created by Toan.IT on 7/31/17.
 * Email:Huynhvantoan.itc@gmail.com
 */

class DownloadSongStorageEvent(var isInternalStorage: Boolean = SDCardDef.STORAGE, var availableBytes: String = "0MB") : Event()
