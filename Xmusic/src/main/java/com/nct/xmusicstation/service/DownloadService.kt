package com.nct.xmusicstation.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.blankj.utilcode.util.FileUtils
import com.liulishuo.filedownloader.BaseDownloadTask
import com.liulishuo.filedownloader.FileDownloadListener
import com.liulishuo.filedownloader.FileDownloadQueueSet
import com.liulishuo.filedownloader.FileDownloader
import com.liulishuo.filedownloader.exception.FileDownloadHttpException
import com.liulishuo.filedownloader.exception.FileDownloadOutOfSpaceException
import com.nct.xmusicstation.data.model.song.SongDetail
import com.nct.xmusicstation.define.DownloadDef
import com.nct.xmusicstation.define.SyncDef
import com.nct.xmusicstation.define.YoutubeDef
import com.nct.xmusicstation.event.DownloadProgressEvent
import com.nct.xmusicstation.event.NotHaveEnoughSpaceEvent
import com.nct.xmusicstation.event.PlayingSongEvent
import com.nct.xmusicstation.ui.player.PlayerViewModel
import com.nct.xmusicstation.utils.getFolderDownloadedSongFile
import com.orhanobut.logger.Logger
import com.toan_itc.core.richutils.getExtension
import dagger.android.AndroidInjection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File
import java.net.UnknownHostException
import javax.inject.Inject

class DownloadService : Service() {
    @Inject
    lateinit var playerModel: PlayerViewModel
    private var alBumID: Int = -1
    private var queueSet: FileDownloadQueueSet? = null
    private var tasks = arrayListOf<BaseDownloadTask>()
    private var pathDownload: File = File("")
    private var index = DownloadDef.NO_DOWNLOAD
    private var isSongDownload = false
    private var songKeyFistSong = ""
    private var countFileDownload = 0
    private var isStop = false
    private val isLog = false
    private var downloadType = DOWNLOAD_ALBUM
    //Corouties
    private val viewModelJob = SupervisorJob()
    private val ioScope = viewModelJob + Dispatchers.IO

    companion object {
        const val DOWNLOAD_ALBUM = "DOWNLOAD_ALBUM"
        const val DOWNLOAD_ALBUM_NOW = "DOWNLOAD_ALBUM_NOW"
        const val EXTRA_ALBUM_ID = "EXTRA_ALBUM_ID"
        const val STOP_DOWNLOAD = "STOP_DOWNLOAD"
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.apply {
            if (isLog) Logger.d(
                "DownloadService:onHandleIntent=" + intent.action + "index=" + index + "alBumIDNow=" + alBumID + "EXTRA_ALBUM_ID=" + intent.extras?.getInt(
                    EXTRA_ALBUM_ID
                )
            )
            when (intent.action) {
                DOWNLOAD_ALBUM -> {
                    intent.extras?.apply {
                        downloadType = DOWNLOAD_ALBUM
                        startDownloadAlbum(albumId = getInt(EXTRA_ALBUM_ID))
                    }
                }
                DOWNLOAD_ALBUM_NOW -> {
                    if(downloadType == DOWNLOAD_ALBUM_NOW) return@apply
                    intent.extras?.apply {
                        alBumID = 0
                        downloadType = DOWNLOAD_ALBUM_NOW
                        resetAfterDownload()
                        startDownloadAlbum(albumId = getInt(EXTRA_ALBUM_ID))
                    }
                }
                STOP_DOWNLOAD -> {
                    downloadType = STOP_DOWNLOAD
                    isStop = true
                    stopSelf()
                }
                else -> if (isLog) Logger.e("Error:EXTRA_ALBUM_DETAIL")
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        if (isLog) Logger.e("onCreate")
        isStop = false
        initBuilder()
    }

    private fun initBuilder() {
        AndroidInjection.inject(this)
        pathDownload = getFolderDownloadedSongFile()
        tasks.clear()
        queueSet = FileDownloadQueueSet(mListenerDownload)
        if (isLog) Logger.d("pathDownload=$pathDownload")
    }

    private val mListenerDownload = object : FileDownloadListener() {
        override fun warn(task: BaseDownloadTask?) {
            if (isLog) Logger.d("warn=" + task.toString())
        }

        override fun completed(task: BaseDownloadTask?) {
            CoroutineScope(ioScope).launch {
                try {
                    task?.apply {
                        if (playerModel.checkInfoStorage()) {
                            playerModel.sendEventBus(NotHaveEnoughSpaceEvent())
                            resetAfterDownload()
                            stopSelf()
                            return@launch
                        }
                        if (isLog) Logger.wtf("Complete:key=" + tag.toString() + "alBumID=" + alBumID)
                        playerModel.saveSongDetailsDownload(tag.toString(), alBumID, false)
                        runPlayService(isSongFistDownload = true)
                        checkDownloadComplete()
                    }
                } catch (e: java.lang.Exception) {
                    e.printStackTrace()
                }
            }
        }

        override fun pending(task: BaseDownloadTask?, soFarBytes: Int, totalBytes: Int) {
            //Logger.d("pending:filename="+task?.filename+"path="+task?.path)
        }

        override fun error(task: BaseDownloadTask?, e: Throwable?) {
            CoroutineScope(ioScope).launch {
                try {
                    when (e) {
                        is FileDownloadOutOfSpaceException -> {
                            playerModel.sendEventBus(NotHaveEnoughSpaceEvent())
                            resetAfterDownload()
                            stopSelf()
                        }
                        is FileDownloadHttpException -> retryDownloadAlbum()
                        is UnknownHostException -> {
                            runPlayService()
                            stopSelf()
                        }
                    }
                    runPlayService(isSongFistDownload = true)
                    task?.apply {
                        if (isLog) Logger.e("error:" + this.toString() + "alBumID=" + alBumID + "tag=" + tag.toString() + "\n message=" + e?.message)
                        e?.printStackTrace()
                        playerModel.saveSongDetailsDownload(tag.toString(), alBumID, true)
                        playerModel.addLogSongError(tag.toString(), alBumID, e?.message)
                        playerModel.syncLinkSongDetails(tag.toString())
                        //Check error return initBuilder
                        initBuilder()
                        checkDownloadComplete()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        override fun progress(task: BaseDownloadTask?, soFarBytes: Int, totalBytes: Int) {
            try {
                if (!isSongDownload) return
                if (isSongDownload && task?.tag.toString() == songKeyFistSong) postProgressEvent(soFarBytes, totalBytes)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        override fun paused(task: BaseDownloadTask?, soFarBytes: Int, totalBytes: Int) {

        }
    }

    private fun retryDownloadAlbum() {
        resetAfterDownload()
        initBeforeDownload()
        queueSet?.apply {
            CoroutineScope(ioScope).launch {
                try {
                    playerModel.getListSongDetailsFindAlbum(alBumID)?.let { listSongs ->
                        if (listSongs.isNotEmpty()) {
                            val listSongError = playerModel.getListSongDownloadError(alBumID)
                            if (isLog) Logger.e("retryDownloadAlbum:" + alBumID + "\n listSong=" + listSongs.size + "listSongError=" + listSongError.map { it.title })
                            listSongs.map { songDetails ->
                                songDetails.apply {
                                    if (listSongError.find { it.key == key } == null && online == YoutubeDef.NORMAL) {
                                        if (checkFileExist(key, streamUrl)) {
                                            playerModel.saveSongDetailsDownload(key, alBumID, false)
                                        } else if (!playerModel.checkIsSongDownloadError(key)) {
                                            addTaskDownload(this)
                                        }
                                    }
                                }
                            }
                            if (isLog) Logger.e("retry:tasks:" + tasks.size + "\n tasks=" + tasks.map { it.filename })
                            if (tasks.size > 0) {
                                index = DownloadDef.DOWNLOADING
                                downloadSequentially(tasks)
                                start()
                            } else {
                                runPlayService()
                                playerModel.setAlbumsDownload(alBumID)
                                startDownloadNextAlbum()
                            }
                            if (isLog) Logger.e("retryDownloadAlbum:Size=" + tasks.size)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun addTaskDownload(songDetail: SongDetail?) {
        songDetail?.apply {
            tasks.add(FileDownloader.getImpl().create(streamUrl).setTag(key).setPath(getFile(key, streamUrl)))
        }
    }

    private fun startDownloadAlbum(albumId: Int = 0) {
        CoroutineScope(ioScope).launch {
            try {
                if (alBumID != albumId && !playerModel.checkIsAlbumDownload(albumId)) {
                    if (isLog) Logger.e(
                        "startDownloadAlbum:albumDifferent=true:albumId: $albumId=" + "index=" + index + "alBumID=" + alBumID + "checkIsAlbumDownload=" + playerModel.checkIsAlbumDownload(
                            albumId
                        )
                    )
                    resetAfterDownload()
                    alBumID = albumId
                    playerModel.getFistSongDetailsFindAlbum(alBumID)?.apply {
                        if (online == YoutubeDef.YOUTUBE) {
                            isSongDownload = false
                        } else {
                            isSongDownload = !checkFileExist(key, streamUrl)
                            if (isSongDownload) {
                                songKeyFistSong = this.key
                                playerModel.sendEventBus(PlayingSongEvent(this, true))
                            } else {
                                runPlayService(isSongFistDownload = true)
                            }
                        }
                    } ?: run {
                        if (isLog) Logger.e("RUN:startDownloadAlbum=$alBumID")
                        startDownloadAlbum(alBumID)
                    }
                } else {
                    if (isLog) Logger.e(
                        "startDownloadAlbum:albumDifferent=false::albumId: $albumId+ index=" + index + "alBumID=" + alBumID + "checkIsAlbumDownload=" + playerModel.checkIsAlbumDownload(
                            albumId
                        )
                    )
                    val checkAlbumDownload = playerModel.checkIsAlbumDownload(albumId)
                    if (checkAlbumDownload) {
                        runPlayService()
                        if (index != DownloadDef.DOWNLOADING) startDownloadNextAlbum()
                        return@launch
                    }
                }
                resetAfterDownload()
                initBeforeDownload()
                queueSet?.apply {
                    alBumID = albumId
                    playerModel.getListSongDetailsFindAlbum(alBumID)?.let { it ->
                        if (it.isNotEmpty()) {
                            val listSongError = playerModel.getListSongDownloadError(alBumID)
                            if (isLog) Logger.e("startDownloadFistAlbum:" + alBumID + "\n listSong=" + it.size + "listSongError=" + listSongError.map { it.title })
                            it.mapIndexed { index, songDetails ->
                                songDetails.apply {
                                    if (index == 0) {
                                        if (online == YoutubeDef.YOUTUBE) {
                                            isSongDownload = false
                                        } else {
                                            isSongDownload = !checkFileExist(key, streamUrl)
                                            if (isLog) Logger.e("isSongDownload:" + isSongDownload)
                                            if (isLog) Logger.e("retryDownloadAlbum:" + pathDownload.absolutePath + File.separator + key + getExtension(streamUrl))
                                            if (isSongDownload) {
                                                songKeyFistSong = key
                                                playerModel.sendEventBus(PlayingSongEvent(this, true))
                                            } else {
                                                runPlayService(isSongFistDownload = true)
                                            }
                                        }
                                    }
                                    if (online == YoutubeDef.YOUTUBE) {
                                        playerModel.saveSongDetailsDownload(key, alBumID, false)
                                    } else if (checkFileExist(key, streamUrl)) {
                                        if (listSongError.find { it.key == key } == null) {
                                            playerModel.saveSongDetailsDownload(key, alBumID, false)
                                        }
                                    } else {
                                        if (listSongError.find { it.key == key } != null) {
                                            addTaskDownload(this)
                                        } else {
                                            addTaskDownload(this)
                                        }
                                    }
                                }
                            }
                            if (isLog) Logger.e("startDownloadAlbum:tasks:" + tasks.size + "\n tasks=" + tasks.map { it.filename })
                            if (tasks.size > 0) {
                                index = DownloadDef.DOWNLOADING
                                downloadSequentially(tasks)
                                start()
                            } else {
                                runPlayService()
                                playerModel.setAlbumsDownload(alBumID)
                                startDownloadNextAlbum()
                            }
                            if (isLog) Logger.e("startDownloadAlbum:Size=" + tasks.size)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun runPlayService(isSongFistDownload: Boolean = false) {
        if (isLog) Logger.e("isSongFistDownload=$isSongFistDownload isSongDownload=$isSongDownload")
        if (isSongFistDownload) {
            if (isSongDownload) {
                isSongDownload = false
                playerModel.syncDataAndService(this@DownloadService, SyncDef.SERVICE_PLAY)
            }
        } else {
            if (!isSongDownload) {
                playerModel.syncDataAndService(this@DownloadService, SyncDef.SERVICE_PLAY)
            }
        }
    }

    private fun startDownloadNextAlbum() {
        resetAfterDownload()
        initBeforeDownload()
        CoroutineScope(ioScope).launch {
            try {
                if (isLog) Logger.e("startDownloadNextAlbum:index=$index")
                if (index == DownloadDef.DOWNLOADING || index == DownloadDef.DOWNLOAD_NEXT_ALBUM || isStop) return@launch
                val albumIDNextDownload = playerModel.getAlbumNextDownload(alBumID)
                if (isLog) Logger.d(
                    "albumNextDownload=" + albumIDNextDownload + "checkIsAlbumDownload=" + playerModel.checkIsAlbumDownload(
                        albumIDNextDownload
                    )
                )
                if (albumIDNextDownload == -1) return@launch
                if (playerModel.checkIsAlbumDownload(albumIDNextDownload)) {
                    //startDownloadNextAlbum()
                    return@launch
                }
                queueSet?.apply {
                    playerModel.getListSongDetailsFindAlbum(albumIDNextDownload)?.let {
                        if (it.isNotEmpty()) {
                            alBumID = albumIDNextDownload ?: -1
                            val listSongError = playerModel.getListSongDownloadError(alBumID)
                            if (isLog) Logger.e("startDownloadNextAlbum:" + albumIDNextDownload + "\n listSong=" + it.size + "listSongError=" + listSongError.map { it.title })
                            it.map { songDetails ->
                                songDetails.apply {
                                    if (online == YoutubeDef.YOUTUBE) {
                                        playerModel.saveSongDetailsDownload(key, alBumID, false)
                                    } else if (checkFileExist(key, streamUrl)) {
                                        if (listSongError.find { it.key == key } == null) {
                                            playerModel.saveSongDetailsDownload(key, alBumID, false)
                                        }
                                    } else {
                                        if (listSongError.find { it.key == key } != null) {
                                            addTaskDownload(this)
                                        } else {
                                            addTaskDownload(this)
                                        }
                                    }
                                }
                            }
                            if (isLog) Logger.e("startDownloadNextAlbum:tasks:" + tasks.size + "\n tasks=" + tasks.map { it.filename })
                            if (tasks.size > 0) {
                                index = DownloadDef.DOWNLOAD_NEXT_ALBUM
                                downloadSequentially(tasks)
                                start()
                            } else {
                                runPlayService()
                                playerModel.setAlbumsDownload(alBumID)
                                startDownloadNextAlbum()
                            }
                            if (isLog) Logger.e("downloadContextAlbum:Size=" + tasks.size)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun initBeforeDownload() {
        if (isLog) Logger.e("initBeforeDownload")
        countFileDownload = 0
        if (playerModel.checkInfoStorage()) {
            if (isLog) Logger.e("checkInfoStorage")
            queueSet = null
            playerModel.sendEventBus(NotHaveEnoughSpaceEvent())
            resetAfterDownload(true)
            stopSelf()
            return
        }
        if (alBumID == -1 || DeleteFileService.IS_RUNNING_CLEAR_TEMP || index == DownloadDef.DOWNLOADING) {
            if (isLog) Logger.e("initBeforeDownload::RETURN")
            return
        }
    }

    private fun resetAfterDownload(isDestroy: Boolean = false) {
        if (isLog) Logger.e("resetAfterDownload")
        tasks.clear()
        index = DownloadDef.NO_DOWNLOAD
        FileDownloader.getImpl()?.apply {
            if (isServiceConnected) {
                if (isLog) Logger.e("resetAfterDownload:isDestroy=true")
                clearAllTaskData()
                if (isDestroy) {
                    queueSet = null
                    unBindService()
                }
            }
        }
    }

    private fun checkDownloadComplete() {
        countFileDownload++
        if (countFileDownload == tasks.size || tasks.isEmpty()) {
            playerModel.setAlbumsDownload(alBumID)
            countFileDownload = 0
            index = DownloadDef.DOWNLOAD_OK
            resetAfterDownload()
            startDownloadNextAlbum()
        }
    }

    private fun checkFileExist(keySong: String, path: String): Boolean =
        FileUtils.isFileExists(pathDownload.absolutePath + File.separator + keySong + getExtension(path))

    private fun postProgressEvent(soFarBytes: Int, totalBytes: Int) {
        if (!MediaService.isPlaying) {
            var progressPercent: Float
            progressPercent = if (totalBytes == 0) {
                0f
            } else {
                (soFarBytes * 100 / totalBytes).toFloat()
            }
            if (progressPercent > 100) {
                songKeyFistSong = ""
                isSongDownload = false
                progressPercent = -1f
            }
            playerModel.sendEventBus(DownloadProgressEvent(progressPercent))
        }
    }

    private fun getFile(key: String, path: String): String = pathDownload.absolutePath + File.separator + key + getExtension(path)

    override fun onDestroy() {
        if (isLog) Logger.e("onDestroy:DownloadService")
        viewModelJob.cancel()
        resetAfterDownload(true)
        super.onDestroy()
    }

}