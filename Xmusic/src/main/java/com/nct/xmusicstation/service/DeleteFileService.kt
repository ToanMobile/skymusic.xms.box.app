package com.nct.xmusicstation.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.blankj.utilcode.util.FileUtils
import com.nct.xmusicstation.data.model.song.SongDetailDownload
import com.nct.xmusicstation.ui.player.PlayerViewModel
import com.nct.xmusicstation.utils.getFolderDownloadedSong
import com.nct.xmusicstation.utils.removeDisposable
import com.orhanobut.logger.Logger
import com.toan_itc.core.kotlinify.reactive.runSafeOnThread
import dagger.android.AndroidInjection
import io.reactivex.Flowable
import io.reactivex.disposables.Disposable
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Created by Toan.IT on 02/10/18.
 * Email:Huynhvantoan.itc@gmail.com
 */

@Singleton
class DeleteFileService : Service() {
    @Inject
    lateinit var playerModel: PlayerViewModel
    private var removeDisposable: Disposable? = null
    private var isLog = false

    companion object {
        const val SYNC_DELETE_TEMP = "SYNC_DELETE_TEMP"
        var IS_RUNNING_CLEAR_TEMP = false
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
        if (isLog) Logger.e("DeleteFileService")
        when (intent.action) {
            SYNC_DELETE_TEMP -> deleteFileSongOld()
        }
        return START_STICKY
    }

    private fun deleteFileSongOld() {
        IS_RUNNING_CLEAR_TEMP = false
        removeDisposable(removeDisposable)
        removeDisposable = Flowable.just(true)
            .runSafeOnThread()
            .filter { PlayerViewModel.checkSyncDataDelete }
            .flatMap {
                val list = playerModel.getListAllSongDownload()
                if (isLog) Logger.e("list=" + list?.size + "checkSyncDataDelete=" + PlayerViewModel.checkSyncDataDelete)
                if (list?.size!! > 0) {
                    Flowable.just(list)
                } else {
                    Flowable.empty()
                }
            }
            .filter { it.isNotEmpty() }
            .map { listSong ->
                if (isLog) Logger.e("listFilesInDir=" + FileUtils.listFilesInDir(getFolderDownloadedSong())?.size + "getSizeListAllSong=" + playerModel.getSizeListAllSong())
                PlayerViewModel.checkSyncDataDelete = false
                IS_RUNNING_CLEAR_TEMP = true
                FileUtils.listFilesInDir(getFolderDownloadedSong())?.let { listFile ->
                    loop@ for (file: File in listFile) {
                        if (file.path.contains(".")) {
                            val songKey = file.path.substring(
                                file.path.lastIndexOf(File.separator) + 1,
                                file.path.lastIndexOf(".")
                            )
                            for (song: SongDetailDownload? in listSong) {
                                if (song?.key.equals(songKey)) {
                                    continue@loop
                                }
                            }
                            if (isLog) Logger.e("file.path=" + file.path)
                            FileUtils.delete(getFolderDownloadedSong(songKey, file.path))
                        }
                    }
                }
            }
            .subscribe({
                IS_RUNNING_CLEAR_TEMP = false
            }, {
                it.printStackTrace()
            })
    }

    override fun onDestroy() {
        if (isLog) Logger.e("onDestroy:DeleteFileService")
        super.onDestroy()
    }
}