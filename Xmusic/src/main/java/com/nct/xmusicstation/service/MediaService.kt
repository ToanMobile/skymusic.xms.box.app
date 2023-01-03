package com.nct.xmusicstation.service

import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.text.TextUtils
import android.util.SparseArray
import androidx.core.net.toUri
import at.huber.youtubeExtractor.VideoMeta
import at.huber.youtubeExtractor.YouTubeExtractor
import at.huber.youtubeExtractor.YtFile
import com.blankj.utilcode.util.FileUtils
import com.blankj.utilcode.util.SDCardUtils
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.ui.PlayerNotificationManager
import com.google.android.exoplayer2.ui.PlayerNotificationManager.*
import com.google.android.exoplayer2.upstream.FileDataSource
import com.nct.xmusicstation.R
import com.nct.xmusicstation.app.MainActivity
import com.nct.xmusicstation.data.local.prefs.PreferenceManager
import com.nct.xmusicstation.data.local.prefs.pref
import com.nct.xmusicstation.data.model.log.LogSongPlayInfo
import com.nct.xmusicstation.data.model.song.SongDetail
import com.nct.xmusicstation.define.PlaybackStatusDef
import com.nct.xmusicstation.define.PrefDef
import com.nct.xmusicstation.define.YoutubeDef
import com.nct.xmusicstation.event.DownloadProgressEvent
import com.nct.xmusicstation.event.PlayingSongEvent
import com.nct.xmusicstation.event.UpdateAlbumChangeEvent
import com.nct.xmusicstation.event.YoutubeEvent
import com.nct.xmusicstation.ui.player.PlayerViewModel
import com.nct.xmusicstation.utils.*
import com.orhanobut.logger.Logger
import com.toan_itc.core.kotlinify.reactive.runSafeOnThread
import dagger.android.AndroidInjection
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import okhttp3.OkHttpClient
import org.greenrobot.eventbus.EventBus
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Created by Toan.IT on 02/10/18.
 * Email:Huynhvantoan.itc@gmail.com
 */
@Singleton
@Suppress("ReplaceCallWithComparison", "RedundantCompanionReference", "StaticFieldLeak")
class MediaService : Service() {
    val TAG: String = "MediaService \n "

    @Inject
    lateinit var playerModel: PlayerViewModel

    @Inject
    lateinit var okHttpClient: OkHttpClient

    private val NOTIFICATION_ID = 101
    private val channelID = "com.xmusicstation"
    private var currentPlayingAlbum = -1
    private var isCheckComplete = false
    private var currentPlayingSongIndex = -1
    private var albumIdBeforeOnTop = -1
    private var song_index: Int = 0
    private var isTryPlay: Int = -1

    //Task
    private var delayNextSongDisposable: Disposable? = null
    private var completeDisposable: Disposable? = null
    private var playDisposable: Disposable? = null
    private var completedVolumeDisposable: Disposable? = null

    //ExoPlayer
    private var player: ExoPlayer? = null

    //Notification
    private var songDetails: SongDetail? = null
    private var playerNotificationManager: PlayerNotificationManager? = null
    private val isLog = false
    private var isOnTime = false

    companion object {
        var isOnTop = false
        var isPlaying = false
        var isStopOnTop = false
        const val EXTRA_SEEK = "EXTRA_SEEK"
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.apply {
            when (action) {
                PlaybackStatusDef.PLAYING -> {
                    isCheckComplete = false
                    currentPlayingSongIndex = -1
                    isTryPlay = -1
                    if (isLog) Logger.e("ACTION_PLAY_ALBUM_ON_SYNC_COMPLETED")
                    openNextSong()
                    PlayerViewModel.isChangeAlbum = false
                }
                PlaybackStatusDef.SEEK -> {
                    extras?.getLong(EXTRA_SEEK)?.let {
                        if (isLog) Logger.d("ACTION_SEEK=$it")
                        seekTo(it)
                    }
                }
                PlaybackStatusDef.COMPLETE_STOP -> {
                    if (isLog) Logger.d("COMPLETE_STOP")
                    isOnTop = false
                    isCheckComplete = true
                    isPlaying = false
                }
                PlaybackStatusDef.STOP_NOW -> {
                    isOnTop = false
                    isCheckComplete = false
                    if (isLog) Logger.d("ACTION_STOP")
                    playerModel.updatePlayingProgressEvent(-1, -1)
                    playerModel.clearListData()
                    stopPlaySong()
                    removeDisposable(
                        delayNextSongDisposable,
                        completeDisposable,
                        playDisposable,
                        completedVolumeDisposable
                    )
                    releasePlayer()
                }
                PlaybackStatusDef.ONTIME_ONTOP -> {
                    if (isLog) Logger.e("ACTION=ONTIME_ONTOP " + "indexNextSchedule=" + ScheduleService.indexNextSchedule)
                    PlayerViewModel.isChangeAlbum = false
                    ScheduleService.indexSchedule = ScheduleService.indexNextSchedule
                    isCheckComplete = false
                    currentPlayingSongIndex = -1
                    isTryPlay = -1
                    fadeOutInVolume(isFadeOutNow = true)
                }
            }
        }
        return START_STICKY
    }

    override fun onCreate() {
        super.onCreate()
        AndroidInjection.inject(this)
        initExoPlayer()
        initNotification()
    }

    private fun initExoPlayer() {
        player = SimpleExoPlayer.Builder(this@MediaService).build()
        player?.apply {
            addListener(object : Player.Listener {
                override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
                    try {
                        if (playWhenReady && playbackState == Player.STATE_READY) {
                            //if(isLog) Logger.e("STATE_READY")
                            isTryPlay = -1
                            if (!Companion.isPlaying) Companion.isPlaying = true
                            playerModel.updatePlayingProgressEvent(
                                getPositionSong(),
                                getDurationSong(),
                                true
                            )
                            songDetails?.let { song ->
                                if (PreferenceManager.getBoolean(PrefDef.PRE_FIST_START, true)) {
                                    pref {
                                        put(PrefDef.PRE_FIST_START, false)
                                        put(PrefDef.PRE_LAST_SONG, song)
                                    }
                                }
                                checkSongComplete(song)
                                if (isLog) Logger.d("onMusicStateListener:onPlay=$song")
                                playerModel.sendEventBus(DownloadProgressEvent(-1f))
                                playerModel.sendEventBus(PlayingSongEvent(song))
                            }
                        } else if (playWhenReady && playbackState == Player.STATE_ENDED) {
                            //if(isLog) Logger.e("STATE_ENDED")
                            initNextSong()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        openNextSong()
                    }
                }

                override fun onPlayerError(error: ExoPlaybackException) {
                    if (isLog) Logger.d("onPlayerError" + error.message)
                    error.printStackTrace()
                    openNextSong()
                }
            })
        }
    }

    private fun initNotification() {
        playerNotificationManager = Builder(applicationContext, NOTIFICATION_ID, channelID, object : MediaDescriptionAdapter {
            override fun createCurrentContentIntent(player: Player): PendingIntent? {
                val notIntent = Intent(applicationContext, MainActivity::class.java)
                notIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                val flags = if (Build.VERSION.SDK_INT >= 30) {
                    PendingIntent.FLAG_IMMUTABLE
                } else {
                    PendingIntent.FLAG_UPDATE_CURRENT
                }
                return PendingIntent.getActivity(
                    applicationContext,
                    0,
                    notIntent,
                    flags
                )
            }

            override fun getCurrentContentText(player: Player): String =
                artists(songDetails?.artists)

            override fun getCurrentContentTitle(player: Player): String =
                songDetails?.title ?: ""

            override fun getCurrentLargeIcon(
                player: Player,
                callback: BitmapCallback
            ): Bitmap? = BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher_round)

        }).build()
        playerNotificationManager?.setPlayer(player)
    }

    fun initNextSong() {
        playPauseSong(false)
        removeDisposable(
            delayNextSongDisposable,
            completeDisposable,
            completedVolumeDisposable
        )
        openNextSong()
    }

    private fun runExoPlayer(songPath: String, songDetail: SongDetail) {
        this.songDetails = songDetail
        player?.apply {
            if (songPath.isNotEmpty()) {
                stop()
                setMediaSource(ProgressiveMediaSource.Factory(FileDataSource.Factory()).createMediaSource(MediaItem.fromUri(songPath.toUri())))
                prepare()
                if (isLog) Logger.e("runExoPlayer:songPath=$songPath")
                playWhenReady = true
                volume = 1.0f
            }
        }
    }

    private fun seekTo(milliSeconds: Long) {
        if (milliSeconds == 0L) return
        player?.seekTo(milliSeconds)
    }

    private fun isChangeAlbum(): Boolean = PlayerViewModel.albumID != currentPlayingAlbum || PlayerViewModel.isChangeAlbum

    private fun openNextSong(isRetry: Boolean = false) {
        if (isLog) Logger.e("currentPlayingSongIndex= $currentPlayingSongIndex isChangeAlbum=" + isChangeAlbum())
        if ((currentPlayingSongIndex >= 0 && isChangeAlbum()) || currentPlayingSongIndex == -1) {
            PlayerViewModel.isChangeAlbum = false
            currentPlayingAlbum = PlayerViewModel.albumID
            currentPlayingSongIndex = -1
            isTryPlay = 0
            playerModel.sendEventBus(UpdateAlbumChangeEvent())
            val albumNow = playerModel.getListScheduleToday()?.first { it.albumId == currentPlayingAlbum }
            isOnTop = albumNow?.ontop ?: false
            isOnTime = albumNow?.ontime ?: false
            if (isLog) Logger.e("isOnTop==$isOnTop isOnTime=$isOnTime song_index=$song_index currentPlayingAlbum:$currentPlayingAlbum")
        }
        if (isLog) Logger.e("openNextSong:.currentPlayingSongIndex:$currentPlayingSongIndex" + "currentPlayingAlbum=" + currentPlayingAlbum + "PlayerViewModel.albumID=" + PlayerViewModel.albumID + "isCheckComplete=" + isCheckComplete + "isChangeAlbum:" + isChangeAlbum())
        if (isCheckComplete) {
            currentPlayingSongIndex = 0
            isCheckComplete = false
            playerModel.updatePlayingProgressEvent(-1, -1)
            playerModel.sendEventBus(PlayingSongEvent(null))
            removeDisposable(completeDisposable)
            removeDisposable(completedVolumeDisposable)
            stopPlaySong()
            return
        }
        if (isLog) Logger.d("openNextSong1111:.currentPlayingSongIndex==$currentPlayingSongIndex" + "currentPlayingAlbum=" + currentPlayingAlbum + "PlayerViewModel.albumID=" + PlayerViewModel.albumID)
        playerModel.getSizeListSongPlay(currentPlayingAlbum).let { size ->
            if (isLog) Logger.d("openNextSong:.currentPlayingSongIndex:11111==$currentPlayingSongIndex\n size=$size")
            if (!isOnTop && song_index > 0) {
                if (currentPlayingAlbum == albumIdBeforeOnTop) {
                    currentPlayingSongIndex = song_index
                    if (isLog) Logger.d("openNextSong:onTop.isContinue:00000==$currentPlayingSongIndex")
                    pref {
                        song_index = 0
                    }
                } else if (currentPlayingAlbum != albumIdBeforeOnTop) {
                    if (isLog) Logger.d("openNextSong:onTop.isContinue:00000==$currentPlayingSongIndex")
                    pref {
                        song_index = 0
                        albumIdBeforeOnTop = currentPlayingAlbum
                    }
                }
            }
            if (isLog) Logger.e("openNextSong:.currentPlayingSongIndex:2222==$currentPlayingSongIndex size=${size - 1} isOnTop=$isOnTop isOnTime=$isOnTime")
            if (currentPlayingSongIndex == (size - 1) && isOnTop) {
                //TODO if (!isOnTime) {
                isPlaying = false
                isStopOnTop = true
                return@let
                //}
            }
            if (isRetry && isOnTop) {
                if (isLog) Logger.e("openNextSong:.DOWNLOAD_ALBUM_NOW:==$currentPlayingSongIndex size=${size - 1} isOnTop=$isOnTop currentPlayingAlbum=$currentPlayingAlbum")
                val intent = Intent(this, DownloadService::class.java)
                intent.action = DownloadService.DOWNLOAD_ALBUM_NOW
                intent.putExtra(DownloadService.EXTRA_ALBUM_ID, currentPlayingAlbum)
                startService(intent)
            }
            if (currentPlayingSongIndex == size || (currentPlayingSongIndex == 0 && isOnTop && isTryPlay != -1 && isTryPlay < 3)) {
                if (isLog) Logger.e("openNextSong:.currentPlayingSongIndex:2222==$currentPlayingSongIndex")
                isTryPlay++
                currentPlayingSongIndex = 0
            } else {
                currentPlayingSongIndex++
                if (isLog) Logger.d("openNextSong:.currentPlayingSongIndex:33333=$currentPlayingSongIndex\n size=$size")
            }
            if (isLog) Logger.d("openNextSong:.currentPlayingSongIndex:5555=$currentPlayingSongIndex \n size=$size")
            if (size > 0 && currentPlayingSongIndex < size) {
                try {
                    if (isLog) Logger.e("openNextSong:.currentPlayingAlbum=$currentPlayingAlbum currentPlayingSongIndex=$currentPlayingSongIndex")
                    val list = if (is1970()) playerModel.getSongPlay1970(currentPlayingSongIndex)
                    else playerModel.getSongPlay(currentPlayingAlbum, currentPlayingSongIndex)
                    list?.apply {
                        if (isLog) Logger.e("openNextSong:songDetails=${this.title}")
                        playSong(this)
                    } ?: run {
                        delayNextSong()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    delayNextSong()
                }
            } else delayNextSong()
        }
    }

    private fun delayNextSong() {
        if (isLog) Logger.e("delayNextSong:::delayNextSong:::delayNextSong")
        if (isOnTop && isTryPlay == -1) {
            isTryPlay = 0
        }
        removeDisposable(delayNextSongDisposable)
        delayNextSongDisposable = Observable.timer(5, TimeUnit.SECONDS).subscribe {
            openNextSong(isRetry = true)
        }
    }

    private fun playSong(songDetail: SongDetail) {
        if (songDetail.online == YoutubeDef.YOUTUBE) {
            try {
                object : YouTubeExtractor(this) {
                    override fun onExtractionComplete(ytFiles: SparseArray<YtFile>?, vMeta: VideoMeta?) {
                        if (ytFiles != null) {
                            val downloadUrl: String = ytFiles[22].url
                            EventBus.getDefault().post(YoutubeEvent(true, downloadUrl))
                        }
                    }
                }.extract(songDetail.streamUrl)
            } catch (e: Exception) {
                e.printStackTrace()
                delayNextSong()
            }
            ///////////
            /*removeDisposable(getLinkYoutubeDisposable)
            getLinkYoutubeDisposable =
                extractor.extract(songDetail.streamUrl.substring(songDetail.streamUrl.indexOf("v=") + 2))
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({ extraction ->
                        Logger.e("extraction=$extraction")
                        EventBus.getDefault().post(YoutubeEvent(true, (extraction.streams.first() as Stream.VideoStream).url))
                    }, { t ->
                        t.printStackTrace()
                        delayNextSong()
                    })*/
            return
        }
        if (isLog) Logger.e("checkIsSongDetailsDownload==" + playerModel.checkIsSongDetailsDownload(songDetail.key))
        if (isLog) Logger.e("songDetail.key==" + songDetail.key)
        if (!playerModel.checkIsSongDetailsDownload(songDetail.key)) {
            delayNextSong()
            return
        }
        removeDisposable(playDisposable)
        var keySong = ""
        playDisposable = Observable.just(true).runSafeOnThread().map {
            keySong = songDetail.key
            val dataSourcePath = getFolderDownloadedSong(keySong, songDetail.streamUrl)
            if (isLog) Logger.e(
                TAG + "SuperpoweredExample=" + dataSourcePath + "currentPlayingSongIndex=" + PlayerViewModel.albumID + "\n isExists=" + FileUtils.isFileExists(
                    dataSourcePath
                )
            )
            if (isLog) Logger.d("deviceStoragePath=${getDeviceStorage()}")
            dataSourcePath.takeIf { it.isNotEmpty() && !it.startsWith(getDeviceStorage()) }?.let {
                Logger.e("deviceStoragePath=" + getDeviceStorage() + "MIN_MEMORY_SPACE=" + Constants.MIN_MEMORY_SPACE)

                val totalSize = if (SDCardUtils.isSDCardEnableByEnvironment()) SDCardUtils.getSDCardInfo()[1].availableSize else SDCardUtils.getInternalAvailableSize()
                if (totalSize <= 1L) {
                    return@map dataSourcePath
                }
                if (totalSize >= Constants.MIN_MEMORY_SPACE) {
                    val tempFile = File.createTempFile("temp", null)
                    if (isLog) Logger.d(TAG + "playSongUSB::Temp" + tempFile.path)
                    tempFile.deleteOnExit()
                    FileUtils.copy(FileUtils.getFileByPath(dataSourcePath), tempFile)
                    return@map tempFile.path
                } else {
                    openNextSong()
                }
            }
            dataSourcePath
        }
            .map { dataSourcePath ->
                if (isLog) Logger.d(
                    TAG + dataSourcePath + "playSong::FileExists:" + FileUtils.isFileExists(
                        dataSourcePath
                    )
                )
                if (FileUtils.isFileExists(dataSourcePath)) {
                    return@map dataSourcePath
                } else {
                    playerModel.addLogSongError(
                        keySong,
                        PlayerViewModel.albumID,
                        getString(R.string.album_not_download)
                    )
                    if (isLog) Logger.d(TAG + "playSong::DownloadFileAlbum:" + PlayerViewModel.albumID)
                    openNextSong()
                    return@map ""
                }
            }.observeOn(AndroidSchedulers.mainThread()).subscribe({ filePath ->
                if (isLog) Logger.d("filePath=$filePath" + "keySong=" + keySong)
                if (filePath.isNotEmpty()) {
                    if (songDetail.online == YoutubeDef.PLAY_VIDEO) {
                        EventBus.getDefault().post(YoutubeEvent(true, filePath))
                    } else {
                        EventBus.getDefault().post(YoutubeEvent(false))
                        runExoPlayer(filePath, songDetail)
                    }
                }
            }, {
                it.printStackTrace()
                playerModel.addLogSongError(songDetail.key, PlayerViewModel.albumID, it.message)
            })
    }

    private fun checkSongComplete(songDetail: SongDetail) {
        var checkSendLogPlay = true
        var checkSendLog = true
        var countTimerSong = 0.0
        var duration = -1L
        val song = LogSongPlayInfo().apply {
            key = songDetail.key; albumId = PlayerViewModel.albumID; timestamp =
            System.currentTimeMillis()
        }
        if (!isOnTop) {
            if (isLog) Logger.e("PRE_RUN:SaveCurrentPlayingSongIndex=$currentPlayingSongIndex")
            song_index = currentPlayingSongIndex
            albumIdBeforeOnTop = currentPlayingAlbum
        }
        removeDisposable(completeDisposable)
        completeDisposable = Flowable.interval(1, TimeUnit.SECONDS).onBackpressureDrop()
            .observeOn(AndroidSchedulers.mainThread()).map {
                if (duration <= 0) duration = getDurationSong()
                it
            }.subscribeOn(Schedulers.computation()).map {
                if (countTimerSong > -1) countTimerSong++
                if (it in 6..15 && checkSendLogPlay) {
                    checkSendLogPlay = false
                    playerModel.sendLogPlayStart(
                        PlayerViewModel.albumID.toString(),
                        songDetail.key,
                        songDetail.title.toString(),
                        songDetail.artists?.let { TextUtils.join(", ", it) } ?: "")
                }
                //if(isLog) Logger.e("countTimerSong="+countTimerSong.div(TimeUnit.MILLISECONDS.toSeconds(duration))+"checkSendLog="+checkSendLog)
                if (countTimerSong.div(TimeUnit.MILLISECONDS.toSeconds(duration)) >= 0.8 && checkSendLog) {
                    checkSendLog = false
                    countTimerSong = -1.0
                    Logger.e("sendLogPlayTrackingsendLogPlayTrackingsendLogPlayTrackingsendLogPlayTracking")
                    playerModel.sendLogPlayTracking(songDetail.key, PlayerViewModel.albumID, song)
                }
            }.observeOn(AndroidSchedulers.mainThread()).doOnNext {
                if (!checkSendLog) {
                    if (duration > 0 && duration - getPositionSong() <= 5000) {
                        checkSendLog = true
                        if (isLog) Logger.d("run:Check:fadeOutInVolume")
                        fadeOutInVolume()
                    }
                }
                //if(isLog) Logger.e("getPositionSong=${getPositionSong()}")
                playerModel.updatePlayingProgressEvent(getPositionSong(), getDurationSong())
            }.subscribe({}, {
                it.printStackTrace()
                playerModel.addLogSongError(songDetail.key, PlayerViewModel.albumID, it.message)
            })
    }

    private fun stopPlaySong() {
        if (isPlaying()) playPauseSong(false)
        isPlaying = false
    }

    fun isPlaying(): Boolean = player?.playWhenReady ?: false

    private fun getPositionSong(): Long = player?.currentPosition ?: 0L

    private fun getDurationSong(): Long = player?.duration ?: 0L

    private fun playPauseSong(play: Boolean) {
        if (isLog) Logger.d("playPauseSong====")
        player?.playWhenReady = play
    }

    override fun onDestroy() {
        if (isLog) Logger.e("onDestroy:MediaService")
        isPlaying = false
        removeDisposable(
            delayNextSongDisposable,
            completeDisposable,
            playDisposable,
            completedVolumeDisposable
        )
        releasePlayer()
        super.onDestroy()
    }

    private fun fadeOutInVolume(isFadeOutNow: Boolean = false) {
        removeDisposable(completedVolumeDisposable)
        if (isLog) Logger.e("fadeOutInVolume:isChangeAlbum:" + isChangeAlbum() + "isFadeOutNow=" + isFadeOutNow)
        if (isChangeAlbum() || isFadeOutNow) {
            var volume = 1.0f
            completedVolumeDisposable = Observable.interval(150, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .take(if (isFadeOutNow) 50 else (1.0f / 0.03f).toLong()).subscribe({
                    volume -= 0.03f
                    if (volume < 0.1f) volume = 0.1f
                    if (isPlaying()) player?.volume = volume
                }, {
                    playerModel.addLogSongError(Constants.SONGKEY, Constants.ALBUMID, it.message)
                    if (isLog) Logger.d("Error")
                }, {
                    if (isLog) Logger.d("volume:fadeOut=$volume")
                    removeDisposable(completedVolumeDisposable)
                    if (isFadeOutNow) initNextSong()
                })
        }
    }

    private fun releasePlayer() {
        playerNotificationManager?.setPlayer(null)
        player?.apply {
            stop()
            release()
            if (isLog) Logger.e("releasePlayer")
        }
        player = null
    }

    private val binder = MediaPlayerBinder()

    override fun onBind(intent: Intent): IBinder? = binder

    /**
     * A class for clients binding to this service. The client will be passed an object of this
     * class via its onServiceConnected(ComponentName, IBinder) callback.
     */
    inner class MediaPlayerBinder : Binder() {
        /**
         * Returns the instance of this service for a client to make method calls on it.
         *
         * @return the instance of this service.
         */
        val service: MediaService
            get() = this@MediaService
    }

    override fun onUnbind(intent: Intent?): Boolean {
        releasePlayer()
        return false
    }
}