package com.nct.xmusicstation.scheduler

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.AlarmManagerCompat
import com.nct.xmusicstation.define.ScheduleDef
import com.orhanobut.logger.Logger


/**
 * Created by Toan.IT on 12/14/15.
 * Email:Huynhvantoan.itc@gmail.com
 */

object SchedulerHelper {

    private const val PLAY_ALBUM_RC = 2
    val flags = if (Build.VERSION.SDK_INT >= 30) {
        PendingIntent.FLAG_IMMUTABLE
    } else {
        PendingIntent.FLAG_UPDATE_CURRENT
    }
   /* fun scheduleSyncAlbum() {
        val periodicWork = PeriodicWorkRequest.Builder(SchedulerWorker::class.java, 1,TimeUnit.MINUTES).build()
        WorkManager.getInstance().enqueue(periodicWork)
    }

    fun cancelAllSchedule() = WorkManager.getInstance().cancelAllWork()*/

    fun scheduleSyncSchedule(context: Context, alarmTime: Long,stateSchedule : Int, albumID :Int?) {
        Logger.e("schedulePlayAlbum:$alarmTime"+"stateSchedule="+stateSchedule +"albumID="+albumID)
        val intent = Intent(context, SyncPlayingAlbumReceiver::class.java)
        intent.putExtra(ScheduleDef.STATE_SCHEDULE, stateSchedule)
        intent.putExtra(ScheduleDef.STATE_SCHEDULE_ALBUM_ID, albumID)
        val sender = PendingIntent.getBroadcast(context, PLAY_ALBUM_RC, intent, flags)
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        AlarmManagerCompat.setExact(am,AlarmManager.RTC_WAKEUP, alarmTime, sender)
    }

    fun cancelScheduleSyncSchedule(context: Context) {
        //Logger.e("cancelSchedulePlayAlbum")
        val intent = Intent(context, SyncPlayingAlbumReceiver::class.java)
        val sender = PendingIntent.getBroadcast(context, PLAY_ALBUM_RC, intent, flags)
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.cancel(sender)
    }
}
