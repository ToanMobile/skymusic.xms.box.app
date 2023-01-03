package com.nct.xmusicstation.service

import android.annotation.SuppressLint
import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.nct.xmusicstation.define.ScheduleDef
import com.nct.xmusicstation.ui.player.PlayerViewModel
import dagger.android.AndroidInjection
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Created by Toan.IT on 02/10/18.
 * Email:Huynhvantoan.itc@gmail.com
 */

@Singleton
class SyncService : Service() {

    @Inject lateinit var playerModel: PlayerViewModel

    companion object {
        const val START_APP = "START_APP"
        const val SYNC_DATA = "SYNC_DATA"
        const val STOP_SYNC = "STOP_SYNC"
    }

    override fun onCreate() {
        super.onCreate()
        AndroidInjection.inject(this)
    }

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) return START_STICKY
        //Logger.e("SyncService:onHandleIntent=" + intent.action)
        when (intent.action) {
            START_APP -> syncFistStartApp()
            SYNC_DATA -> {
                intent.extras?.apply {
                    syncData(getInt(ScheduleDef.STATE_SCHEDULE), getInt(ScheduleDef.STATE_SCHEDULE_ALBUM_ID))
                } ?: run { syncData() }
            }
            STOP_SYNC -> stopSelf()
            else      -> syncData()
        }
        return START_STICKY
    }

    @SuppressLint("CheckResult") private fun syncFistStartApp() {
        //Logger.e("SyncService: syncFistStartApp")
        playerModel.checkForUpdate(this@SyncService)
        playerModel.getUserInfo()
        syncData(ScheduleDef.STATE_FIST_PLAY)
    }

    private fun syncData(scheduleState: Int? = ScheduleDef.STATE_PLAY, albumID: Int = -1) {
        if (scheduleState != ScheduleDef.STATE_PLAY) {
            with(PlayerViewModel) {
                isChangeAlbum = true
                hasShuffle = false
                isPlaying = true
            }
        }
        playerModel.syncDataAlbum(this, scheduleState, albumID)
    }
}