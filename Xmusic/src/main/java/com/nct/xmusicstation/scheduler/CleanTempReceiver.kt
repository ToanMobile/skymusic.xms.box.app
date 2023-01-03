package com.nct.xmusicstation.scheduler

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.nct.xmusicstation.service.DeleteFileService

/**
 * Created by Toan.IT on 12/14/15.
 * Email:Huynhvantoan.itc@gmail.com
 */

class CleanTempReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val deleteFileService = Intent(context, DeleteFileService::class.java)
        deleteFileService.action = DeleteFileService.SYNC_DELETE_TEMP
        context.startService(deleteFileService)
    }
}
