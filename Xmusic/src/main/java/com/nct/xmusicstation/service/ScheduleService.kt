package com.nct.xmusicstation.service

import android.app.AlarmManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.nct.xmusicstation.data.model.song.Schedule
import com.nct.xmusicstation.define.PlaybackStatusDef
import com.nct.xmusicstation.define.ScheduleDef
import com.nct.xmusicstation.define.SyncDef
import com.nct.xmusicstation.event.ScheduleAlbumEvent
import com.nct.xmusicstation.event.ScheduleNextAlbumEvent
import com.nct.xmusicstation.scheduler.SchedulerHelper
import com.nct.xmusicstation.ui.player.PlayerViewModel
import com.nct.xmusicstation.utils.Constants
import com.nct.xmusicstation.utils.formatHourMinutes
import com.nct.xmusicstation.utils.is1970
import com.orhanobut.logger.Logger
import com.toan_itc.core.kotlinify.collections.isNotNullOrEmpty
import dagger.android.AndroidInjection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.joda.time.DateTime
import org.joda.time.LocalDateTime
import org.joda.time.format.DateTimeFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext

/**
 * Created by Toan.IT on 02/10/18.
 * Email:Huynhvantoan.itc@gmail.com
 */

@Singleton
class ScheduleService : Service(), CoroutineScope {
    @Inject
    lateinit var playerModel: PlayerViewModel
    private val isLog = true
    private var scheduleCoroutine = SupervisorJob()

    companion object {
        private var timeDayEnd = 0L
        var indexSchedule: Int = 0
        var indexNextSchedule: Int = 0
        const val SYNC_DATA_SCHEDULE = "SYNC_DATA_SCHEDULE"
    }

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default

    override fun onCreate() {
        super.onCreate()
        AndroidInjection.inject(this)
    }

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) return START_STICKY
        if (isLog) Logger.e("ScheduleService=" + intent.action)
        when (intent.action) {
            SYNC_DATA_SCHEDULE -> {
                scheduleCoroutine.cancel()
                scheduleCoroutine = SupervisorJob()
                launch(scheduleCoroutine) {
                    try {
                        val listSchedule = playerModel.getAlbumSchedule()
                        val now = DateTime.now().millis
                        var playingSchedule: Schedule? = null
                        var nextSchedule: Schedule? = null
                        var onTopSchedule: Schedule? = null
                        var nextScheduleFrom = 0L
                        //if(isLog) Logger.e("listSchedule="+listSchedule.toString()+"local="+LocalDate().dayOfWeek().getAsText(Locale.ENGLISH))
                        val schedules = listSchedule.filter {
                            it.scheduleType == LocalDateTime().toString(DateTimeFormat.forPattern("EEEE").withLocale(Locale.ENGLISH)).uppercase()
                        }
                        //Logger.e("schedule=$schedules")
                        if (listSchedule.isEmpty()) {
                            // Not have any schedule, sync first album
                            val album = playerModel.getFistAlbumDetails()
                            with(PlayerViewModel) {
                                albumID = album?.id ?: -1
                                isPlaying = true
                            }
                            playerModel.sendEventBus(ScheduleNextAlbumEvent(null))
                            playerModel.syncDataAndService(this@ScheduleService, SyncDef.SERVICE_DOWNLOAD)
                            if (isLog) Logger.e("ScheduleService:NoSchedule=" + PlayerViewModel.albumID + "playAlbumFist=" + album.toString())
                            return@launch
                        }
                        if (is1970()) {
                            PlayerViewModel.isPlaying = true
                            playerModel.sendEventBus(ScheduleAlbumEvent(-1, null))
                            playerModel.runServicePlaySong(this@ScheduleService, PlaybackStatusDef.PLAYING)
                            return@launch
                        }
                        if (schedules.isNotNullOrEmpty()) {
                            val schedulesSort = schedules.sortedByDescending { it.ontop || it.ontime }
                            for (index in schedules.indices) {
                                val schedule = schedulesSort[index]
                                if (isLog) Logger.e("schedule=$schedule")
                                val from = formatHourMinutes(schedule.fromTime).millis
                                val to = formatHourMinutes(schedule.toTime).millis
                                if (now in from until to) {
                                    if (schedule.ontop || schedule.ontime) {
                                        onTopSchedule = schedule
                                        indexNextSchedule = schedules.indexOfFirst { it.id == schedule.id }
                                    }
                                    indexSchedule = schedules.indexOfFirst { it.id == schedule.id }
                                    playingSchedule = schedule
                                    if (isLog) Logger.e("onTopSchedule=$onTopSchedule")
                                    if (isLog) Logger.e("indexSchedule=$indexSchedule")
                                    if (isLog) Logger.e("playingSchedule=$schedule")
                                    if (index == schedules.size - 1) {
                                        timeDayEnd = to + (5 * 60 * 1000)
                                        if (isLog) Logger.e("timeDayEnd=$timeDayEnd")
                                    }
                                    break
                                }
                            }
                            schedules.mapIndexed { index, schedule ->
                                val from = formatHourMinutes(schedule.fromTime).millis
                                if (from > now && (nextScheduleFrom == 0L || from < nextScheduleFrom)) {
                                    indexNextSchedule = index
                                    nextScheduleFrom = from
                                    nextSchedule = schedule
                                    return@mapIndexed
                                }
                            }
                            if (isLog) Logger.e("AlbumEndDay=$timeDayEnd timeNow=${now}")
                            if (timeDayEnd != 0L && now > timeDayEnd) {
                                timeDayEnd = 0L
                                PlayerViewModel.isPlaying = false
                                playerModel.sendEventBus(ScheduleAlbumEvent(-1, schedules[0]))
                                playerModel.runServicePlaySong(this@ScheduleService, PlaybackStatusDef.STOP_NOW)
                                if (isLog) Logger.e("AlbumEndDay=STOP")
                                return@launch
                            }
                            if (playingSchedule != null) {
                                playingSchedule.let { schedule ->
                                    if (isLog) Logger.e(
                                        "ScheduleService:playingSchedule:==" + schedule.toString() + "onTopSchedule=" + onTopSchedule.toString() + "PlayerViewModel.albumID==" + PlayerViewModel.albumID + "isOnTop=" + MediaService.isOnTop + "now=" + now + "onTopSchedulemili=" + formatHourMinutes(
                                            onTopSchedule?.toTime
                                        ).millis
                                    )
                                    if (isLog) Logger.e("nextSchedule=" + nextSchedule?.toString())
                                    if (isLog) Logger.e("MediaService.isOnTop=" + MediaService.isOnTop + "onTopSchedule?.ontop=" + onTopSchedule?.ontop + "nextSchedule?.ontime:" + (((nextSchedule != null && nextSchedule?.ontime == false) || nextSchedule == null)) + "MediaService.isStopOnTop::" + MediaService.isStopOnTop + "MediaService.isPlaying==" + MediaService.isPlaying)
                                    if (isLog) Logger.e("now=" + now + "onTopSchedule?.toTime=" + formatHourMinutes(onTopSchedule?.toTime).millis)
                                    if (MediaService.isOnTop
                                        && onTopSchedule?.ontop == true
                                        && ((nextSchedule != null && nextSchedule?.ontime == false) || nextSchedule == null)
                                        && now < formatHourMinutes(onTopSchedule.toTime).millis
                                        && (MediaService.isPlaying || MediaService.isStopOnTop)
                                    ) {
                                        if (isLog) Logger.e("ScheduleService:return")
                                        return@launch
                                    }
                                    with(PlayerViewModel) {
                                        if (albumID != schedule.albumId) {
                                            isChangeAlbum = true
                                            hasShuffle = false
                                            if (MediaService.isOnTop && (!schedule.ontop || !schedule.ontime)) hasShuffle = true
                                        }
                                        albumID = schedule.albumId ?: -1
                                        isPlaying = true
                                    }
                                    playerModel.sendEventBus(ScheduleNextAlbumEvent(null))
                                    if (isLog) Logger.e("playingSchedule=" + schedule.toString() + "formatHourMinutes=" + formatHourMinutes(schedule.fromTime).millis + "now=" + now +"\n nextSchedule"+nextSchedule+ "\n nextScheduleFrom=" + nextScheduleFrom + "now=" + now)
                                    if (nextSchedule != null) {
                                        if (nextSchedule?.ontop == true || nextSchedule?.ontime == true) {
                                            if (nextScheduleFrom >= now) {
                                                if (isLog) Logger.e("RUN:nextScheduleFrom: MediaService.state_ontop=true \n $nextScheduleFrom")
                                                runSchedule(nextScheduleFrom, ScheduleDef.STATE_ONTIME, nextSchedule?.albumId)
                                            }
                                        }
                                        playerModel.syncDataAndService(this@ScheduleService, SyncDef.SERVICE_DOWNLOAD)
                                        playerModel.sendEventBus(ScheduleAlbumEvent(indexSchedule, schedule))
                                    } else {
                                        if (isLog) Logger.e("schedule.ontop|schedule.ontime=$schedule")
                                        if (schedule.ontop || schedule.ontime) {
                                            if (MediaService.isPlaying && PlayerViewModel.albumID == schedule.albumId) return@launch
                                            if (formatHourMinutes(schedule.fromTime).millis >= now) {
                                                if (isLog) Logger.e("RUN:playingScheduleFrom: MediaService.state_ontop=false \n ${formatHourMinutes(schedule.fromTime).millis}")
                                                runSchedule(formatHourMinutes(schedule.fromTime).millis, ScheduleDef.STATE_ONTIME, schedule.albumId)
                                            } else {
                                                if (isLog) Logger.e("RUN:playingScheduleFrom: MediaService.state_ontop=false \n ${SyncDef.SERVICE_ONTIME}")
                                                playerModel.syncDataAndService(this@ScheduleService, SyncDef.SERVICE_ONTIME)
                                                playerModel.sendEventBus(ScheduleAlbumEvent(indexSchedule, schedule))
                                            }
                                        } else {
                                            if (isLog) Logger.e("playingSchedule=" + schedule.toString() + "formatHourMinutes=" + formatHourMinutes(schedule.fromTime).millis + "now=" + now)
                                            playerModel.syncDataAndService(this@ScheduleService, SyncDef.SERVICE_DOWNLOAD)
                                            playerModel.sendEventBus(ScheduleAlbumEvent(indexSchedule, schedule))
                                        }
                                    }
                                }
                            } else {
                                if (nextSchedule == null || nextScheduleFrom == 0L) {
                                    if (isLog) Logger.e("ScheduleService:nextSchedule==null:runDefaultAlbumFist=")
                                    PlayerViewModel.isPlaying = false
                                    playerModel.sendEventBus(ScheduleAlbumEvent(-1, schedules[0]))
                                    playerModel.runServicePlaySong(this@ScheduleService, PlaybackStatusDef.COMPLETE_STOP)
                                    return@launch
                                }
                                if (nextScheduleFrom == 0L) {
                                    nextSchedule = schedules[0]
                                    nextScheduleFrom = formatHourMinutes(nextSchedule!!.fromTime).millis
                                } else if (nextScheduleFrom <= now) {
                                    nextScheduleFrom = nextScheduleFrom.plus(AlarmManager.INTERVAL_DAY)
                                }
                                if (isLog) Logger.e("ScheduleService:nextSchedule=" + nextSchedule.toString() + "PlayerViewModel.albumID=" + PlayerViewModel.albumID)
                                with(PlayerViewModel) {
                                    albumID = nextSchedule?.albumId ?: -1
                                    isPlaying = false
                                }
                                //if (isLog) Logger.e("PRE_ON_TOP:state_ontop albumID=${MediaService.albumIdOnTop}")
                                if (isLog) Logger.e("ScheduleService:nextSchedule:==" + nextSchedule.toString() + "PlayerViewModel.albumID==" + PlayerViewModel.albumID)
                                playerModel.syncDataAndService(this@ScheduleService, SyncDef.SERVICE_DOWNLOAD)
                                playerModel.sendEventBus(ScheduleNextAlbumEvent(nextSchedule))
                                playerModel.runServicePlaySong(this@ScheduleService, PlaybackStatusDef.COMPLETE_STOP)
                                runSchedule(nextScheduleFrom, ScheduleDef.STATE_NEXT_SCHEDULE, -1)
                                return@launch
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        playerModel.addLogSongError(Constants.SONGKEY, Constants.ALBUMID, e.message)
                    }
                }
            }
        }
        return START_STICKY
    }

    private fun runSchedule(alarmTime: Long, stateSchedule: Int, albumID: Int?) {
        SchedulerHelper.cancelScheduleSyncSchedule(this)
        SchedulerHelper.scheduleSyncSchedule(this, alarmTime, stateSchedule, albumID)
    }

    override fun onDestroy() {
        if (isLog) Logger.e("onDestroy:ScheduleService")
        super.onDestroy()
    }
}