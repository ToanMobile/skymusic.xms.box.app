package com.nct.xmusicstation.event

import com.toan_itc.core.base.event.Event

/**
 * Created by Toan.IT on 14/03/18.
 * Email:Huynhvantoan.itc@gmail.com
 */
class ExitAppEvent(val isRemoveAll : Boolean = false, val isExit : Boolean = false) : Event()