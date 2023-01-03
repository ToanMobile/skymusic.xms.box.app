package com.nct.xmusicstation.scheduler

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.nct.xmusicstation.define.ScheduleDef
import com.nct.xmusicstation.service.SyncService

/**
 * Created by Toan.IT on 12/14/15.
 * Email:Huynhvantoan.itc@gmail.com
 */
class SyncPlayingAlbumReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val syncIntent = Intent(context, SyncService::class.java)
        syncIntent.action = SyncService.SYNC_DATA
        syncIntent.putExtra(ScheduleDef.STATE_SCHEDULE, intent.extras?.getInt(ScheduleDef.STATE_SCHEDULE) ?: ScheduleDef.STATE_PLAY)
        syncIntent.putExtra(ScheduleDef.STATE_SCHEDULE_ALBUM_ID, intent.extras?.getInt(ScheduleDef.STATE_SCHEDULE_ALBUM_ID) ?: -1)
        context.startService(syncIntent)
    }
}
