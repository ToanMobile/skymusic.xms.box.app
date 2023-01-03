package com.nct.xmusicstation.scheduler

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.nct.xmusicstation.service.ScheduleService
import com.orhanobut.logger.Logger

/**
 * Created by Toan.IT on 12/14/15.
 * Email:Huynhvantoan.itc@gmail.com
 */

class SyncScheduleReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Logger.e("SyncScheduleReceiver")
        val scheduleService = Intent(context, ScheduleService::class.java)
        scheduleService.action = ScheduleService.SYNC_DATA_SCHEDULE
        context.startService(scheduleService)
    }
}
