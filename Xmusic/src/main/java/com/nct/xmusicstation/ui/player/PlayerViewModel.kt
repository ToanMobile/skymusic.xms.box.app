@file:Suppress("SENSELESS_COMPARISON")

package com.nct.xmusicstation.ui.player

import android.content.Context
import android.content.Intent
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.viewModelScope
import com.blankj.utilcode.util.*
import com.google.gson.Gson
import com.nct.xmusicstation.R
import com.nct.xmusicstation.app.App
import com.nct.xmusicstation.data.PlayerRepository
import com.nct.xmusicstation.data.local.database.RepositoryData
import com.nct.xmusicstation.data.local.prefs.PreferenceManager
import com.nct.xmusicstation.data.local.prefs.pref
import com.nct.xmusicstation.data.model.log.LogDownloadSongInfo
import com.nct.xmusicstation.data.model.log.LogSongPlayInfo
import com.nct.xmusicstation.data.model.song.*
import com.nct.xmusicstation.data.remote.JsonObject
import com.nct.xmusicstation.define.*
import com.nct.xmusicstation.event.*
import com.nct.xmusicstation.service.*
import com.nct.xmusicstation.utils.*
import com.orhanobut.logger.Logger
import com.toan_itc.core.base.BaseViewModel
import com.toan_itc.core.kotlinify.threads.runInBackground
import com.toan_itc.core.richutils.confirm
import com.toan_itc.core.utils.FileCommonUtils
import com.vicpin.krealmextensions.queryAll
import com.vicpin.krealmextensions.save
import io.reactivex.Flowable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import io.realm.Realm
import io.realm.RealmList
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import okhttp3.*
import okio.buffer
import okio.sink
import org.joda.time.LocalDateTime
import org.joda.time.format.DateTimeFormat
import java.io.File
import java.io.IOException
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * Created by Toan.IT on 12/10/18.
 * Email:Huynhvantoan.itc@gmail.com
 */


class PlayerViewModel
@Inject internal constructor(private val repository: PlayerRepository, private val okHttpClient: OkHttpClient) : BaseViewModel(), RepositoryData, LifecycleObserver {
    private var isLostConnection: Boolean = false
    private var isSendLogDownload = false
    private var timerSyncDisposable: Disposable? = null
    private var timerCheckUpdateDisposable: Disposable? = null
    private var syncDataUpdateDisposable: Disposable? = null
    private var jobSync: MutableList<Job>? = null
    private val isLog = false

    companion object {
        var checkSyncDataDelete = true
        var albumID = -1
        var isPlaying = true
        var isChangeAlbum = true
        var hasShuffle = true
        var isShuffle = false
        var isComplete = false
        var isStorage = SDCardDef.STORAGE
    }

    init {
        checkStorage()
    }

    override fun onCleared() {
        super.onCleared()
        clearListData()
        getCompositeDisposable().clear()
        removeDisposable(syncDataUpdateDisposable, timerSyncDisposable, timerCheckUpdateDisposable)
    }

    //DATABASE
    fun clearAll(isRemoveAll: Boolean) {
        repository.clearAll(isRemoveAll)
        if (isRemoveAll) {
            runInBackground {
                FileUtils.deleteFilesInDir(getFolderDownloadedSong())
            }
        }
    }

    override fun getListSongDetailsFindAlbum(albumID: Int?): List<SongDetail>? = repository.realmManager.getListSongDetailsFindAlbum(albumID)

    override fun getListAllSongDownload(): List<SongDetailDownload?>? = repository.realmManager.getListAllSongDownload()

    override fun getSizeListAllSong(): Long = repository.realmManager.getSizeListAllSong()

    override fun getSizeListDownloadAll(): Long = repository.realmManager.getSizeListDownloadAll()

    override fun getSizeListDownloadAlbum(albumID: Int?): Int = repository.realmManager.getSizeListDownloadAlbum(albumID)

    override fun getAlbumSchedule(): List<Schedule> = repository.realmManager.getAlbumSchedule()

    override fun getFistAlbumDetails(): ListAlbum? = repository.realmManager.getFistAlbumDetails()

    override fun getAlbumDetails(albumID: Int?): ListAlbum? = repository.getAlbumDetails(albumID)

    override fun setAlbumsDownload(albumID: Int?, isUpdate: Boolean) = repository.realmManager.setAlbumsDownload(albumID, isUpdate)

    override fun getAlbumNextDownload(albumIDPlaying: Int?): Int? = repository.realmManager.getAlbumNextDownload(albumIDPlaying)

    override fun insertSongDetails(albumID: Int?, songInfo: RealmList<SongDetail>) = repository.realmManager.insertSongDetails(albumID, songInfo)

    override fun checkIsAlbumDownload(albumID: Int?): Boolean = repository.realmManager.checkIsAlbumDownload(albumID)

    override fun runConvertLoudNorm(): Boolean = repository.realmManager.runConvertLoudNorm()

    override fun setShuffleListSongAlbum(albumID: Int?) = repository.realmManager.setShuffleListSongAlbum(albumID)

    override fun saveSongDetailsDownload(keySong: String, albumID: Int, isSongError: Boolean, callbackSuccess: () -> Unit?) =
        repository.realmManager.saveSongDetailsDownload(keySong, albumID, isSongError, callbackSuccess)

    override fun getSizeListSongPlay(albumID: Int?): Int = repository.realmManager.getSizeListSongPlay(albumID)

    override fun getSongPlay1970(songIndex: Int): SongDetail? = repository.realmManager.getSongPlay1970(songIndex)

    override fun getSongPlay(albumID: Int, songIndex: Int): SongDetail? = repository.realmManager.getSongPlay(albumID, songIndex)

    override fun getListSongDownloadError(albumID: Int?): List<SongDetailDownload> = repository.realmManager.getListSongDownloadError(albumID)

    override fun getFistSongDetailsFindAlbum(albumID: Int?): SongDetail? = repository.realmManager.getFistSongDetailsFindAlbum(albumID)

    override fun checkIsSongDetailsDownload(keySong: String): Boolean = repository.realmManager.checkIsSongDetailsDownload(keySong)

    override fun checkIsStorage(): Boolean = repository.realmManager.checkIsStorage()

    override fun setStorage(isSDCard: Boolean) = repository.realmManager.setStorage(isSDCard)

    override fun checkIsSongDownloadError(keySong: String): Boolean = repository.realmManager.checkIsSongDownloadError(keySong)

    override fun setAlbumsShuffle(albumID: Int?, IsShuffle: Boolean) = repository.realmManager.setAlbumsShuffle(albumID, IsShuffle)

    override fun checkIsAlbumShuffle(albumID: Int?): Boolean = repository.realmManager.checkIsAlbumShuffle(albumID)

    override fun setSongLoudNorm(keySong: String, index: Int) = repository.realmManager.setSongLoudNorm(keySong, index)

    override fun getListSongNotLoudNorm(albumID: Int?): List<SongDetailDownload>? = repository.realmManager.getListSongNotLoudNorm(albumID)

    override fun getListSongNotCheckLoudNorm(albumID: Int?): List<SongDetailDownload>? = repository.realmManager.getListSongNotCheckLoudNorm(albumID)

    override fun addLogSongDownload(songID: String?, albumID: Int?, isSongdownload: Boolean, message: String?) =
        repository.realmManager.addLogSongDownload(songID, albumID, isSongdownload, message)

    override fun addLogSongError(songID: String?, albumID: Int?, message: String?) = repository.realmManager.addLogSongError(songID, albumID, message)

    override fun addLogSongPlay(songID: String?, albumID: Int?) = repository.realmManager.addLogSongPlay(songID, albumID)

    override fun getLogSongDownload(): List<LogDownloadSongInfo>? = repository.realmManager.getLogSongDownload()

    override fun getLogSongPlay(): List<LogSongPlayInfo>? = repository.realmManager.getLogSongPlay()

    override fun getLogDatabase(): String = repository.realmManager.getLogDatabase()

    override fun resetConfig(): String = repository.realmManager.resetConfig()

    override fun getRealm(): Realm = repository.realmManager.getRealm()

    override fun closeRealm() = repository.realmManager.closeRealm()

    override fun getOnTopSchedule(albumID: Int?): Boolean = repository.realmManager.getOnTopSchedule(albumID)

    override fun isOntopOntime(albumID: Int?): Boolean = repository.realmManager.isOntopOntime(albumID)

    override fun isOntime(albumID: Int?): Boolean = repository.realmManager.isOntime(albumID)

    fun getListScheduleToday(): List<Schedule>? = getAlbumSchedule().filter {
        it.scheduleType == LocalDateTime().toString(DateTimeFormat.forPattern("EEEE").withLocale(Locale.ENGLISH)).uppercase()
    }

    fun getListSong(): List<SongDetail>? = getListSongDetailsFindAlbum(albumID)

    fun syncData(context: Context?) {
        context?.let {
            pref {
                put(PrefDef.PRE_FIST_START, true)
            }
            val intentSync = Intent(it, SyncService::class.java)
            intentSync.action = SyncService.START_APP
            it.startService(intentSync)
            timerCheckUpdate(it)
            timerSyncData(it)
        }
    }

    fun syncDataAndService(context: Context?, syncService: String = SyncDef.SERVICE_SCHEDULE) {
        if (isLog) Logger.e("syncDataAndService::$syncService")
        if (context == null) return
        viewModelScope.launch(mainScope) {
            checkInfoStorage()
            if (isLog) Logger.e("syncDataAlbum:Shuffle=${getAlbumDetails(albumID)?.shuffle} albumID=$albumID isChangeAlbum=$isChangeAlbum hasShuffle=$hasShuffle")
            if (getAlbumDetails(albumID)?.shuffle == true && isChangeAlbum && !hasShuffle) {
                hasShuffle = true
                setShuffleListSongAlbum(albumID)
            }
            when (syncService) {
                SyncDef.SERVICE_SCHEDULE -> runServiceSchedule(context)
                SyncDef.SERVICE_DOWNLOAD -> runServiceDownload(context)
                SyncDef.SERVICE_ONTIME -> runServicePlaySong(context, PlaybackStatusDef.ONTIME_ONTOP)
                SyncDef.SERVICE_PLAY -> runServicePlaySong(context)
            }
        }
    }

    fun clearListData() {
        if (isLog) Logger.e("clearListData")
        closeRealm()
        albumID = -1
        MediaService.isPlaying = false
        jobSync?.clear()
        jobSync = null
    }

    //CHECK UPDATE
    private fun timerCheckUpdate(context: Context) {
        removeDisposable(timerCheckUpdateDisposable)
        timerCheckUpdateDisposable = Flowable.interval(1, if (isDebug) TimeUnit.MINUTES else TimeUnit.HOURS).map {
            runServiceCleanTemp(context)
            checkForUpdate(context)
        }.subscribe {
            sendLogPlayTracking()
            sendLogDownloadTracking()
        }
    }

    private fun timerSyncData(context: Context) {
        removeDisposable(timerSyncDisposable)
        timerSyncDisposable = Flowable.interval(1, 1, TimeUnit.MINUTES).subscribe {
            val syncIntent = Intent(context, SyncService::class.java)
            syncIntent.action = SyncService.SYNC_DATA
            context.startService(syncIntent)
        }
    }

    fun updateApp() = installUpdateApp(false, "", "", "", 1, "", false)

    fun checkForUpdate(context: Context) {
        if (isLostConnection) return
        repository.getUpdateVersion().subscribeOn(Schedulers.io()).map {
            //val update = Update(true, "", true, "http://skymusic.com.vn/install?xms_3.1.0.apk","http://skymusic.com.vn/install?xms_3.1.0.apk", 2,"","",true)
            val update = it.update
            if (update != null && update.enable == true) {
                when (update.force) {
                    true -> installUpdateApp(false, update.url, update.urlUpdate, update.leastVersion, update.versionUpdate, update.pathUpdate, true)
                    else -> context.confirm(
                        context.resources.getString(R.string.update_app),
                        { showDownloadAppByStoreUrl(context, update.url) },
                        "",
                        context.resources.getString(R.string.update_yes),
                        context.resources.getString(R.string.later)
                    )
                }
            }
            true
        }.doOnError {
            it.printStackTrace()
            addLogSongError(Constants.SONGKEY, Constants.ALBUMID, it.message)
            if (isLog) Logger.d("Error")
        }.subscribe()
    }

    private fun installUpdateApp(
        isForceUpdateXMS: Boolean,
        linkUrl: String,
        linkUrlUpdate: String,
        updateVersion: String,
        versionCodeUpdate: Int,
        pathUpdate: String,
        autoUpdate: Boolean
    ) {
        val pathDownloadAPK: String
        when (autoUpdate) {
            true -> {
                if (isLog) Logger.d(
                    "downloadFileUpdate:isAppInstalled:${AppUtils.isAppInstalled(Constants.XMS_UPDATE)} isAppRoot:${AppUtils.isAppRoot()} isForceUpdateXMS:$isForceUpdateXMS \n linkUrlUpdate:$linkUrlUpdate getAppVersionCode: ${
                        AppUtils.getAppVersionCode(
                            Constants.XMS_UPDATE
                        )
                    }"
                )
                val installAppUpdate =
                    (!AppUtils.isAppInstalled(Constants.XMS_UPDATE) && AppUtils.isAppRoot() && !isForceUpdateXMS) || (linkUrlUpdate.isNotEmpty() && AppUtils.getAppVersionCode(
                        Constants.XMS_UPDATE
                    ) != versionCodeUpdate && !isForceUpdateXMS)
                val linkDownload = if (installAppUpdate) {
                    Logger.e("installUpdate")
                    pathDownloadAPK = getPathDownloadStorage().replace(Constants.INSTALL_APP, "Update.apk")
                    linkUrlUpdate
                } else {
                    Logger.e("installXMS")
                    pathDownloadAPK = getPathDownloadStorage()
                    linkUrl
                }
                if (FileUtils.isFileExists(pathDownloadAPK)) FileUtils.delete(pathDownloadAPK)
                val fileCheckUpdate = PathUtils.getExternalAppMusicPath() + File.separator + "xMusic.txt"
                if (FileUtils.isFileExists(fileCheckUpdate)) FileUtils.delete(fileCheckUpdate)
                if (pathUpdate.isNotEmpty()) FileIOUtils.writeFileFromString(fileCheckUpdate, pathUpdate)
                else if (pathUpdate == "Default") FileIOUtils.writeFileFromString(fileCheckUpdate, pathDownloadAPK)
                Logger.e("linkDownload=$linkDownload")
                val downloadRequest = Request.Builder().url(linkDownload).build()
                okHttpClient.newCall(downloadRequest).enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        if (isLog) Logger.d("error:" + e.message)
                        if (!installAppUpdate) addLogSongError(Constants.SONGKEY, Constants.ALBUMID, e.message)
                    }

                    override fun onResponse(call: Call, response: Response) {
                        if (response.isSuccessful) {
                            val apkFile = FileUtils.getFileByPath(pathDownloadAPK)
                            val body = response.body
                            val sink = apkFile.sink().buffer()
                            body?.source().use { input ->
                                sink.use { output ->
                                    (input?.let { output.writeAll(it) })
                                }
                            }
                            if (isLog) Logger.e(
                                "downloadFileUpdate:completed" + "isAppInstalled=" + AppUtils.isAppInstalled(Constants.XMS_UPDATE) + "isFileExists=" + FileCommonUtils.isFileExists(
                                    pathDownloadAPK
                                )
                            )
                            if (installAppUpdate) {
                                val isInstall = DangerousUtils.installAppSilent(FileUtils.getFileByPath(pathDownloadAPK), "-r")
                                if (isLog) Logger.e("downloadFileUpdate:isInstall==$isInstall")
                                installUpdateApp(true, linkUrl, linkUrlUpdate, updateVersion, versionCodeUpdate, pathUpdate, autoUpdate)
                            } else {
                                //if (isLog) Logger.e("downloadFileUpdate:Path" + apkFile)
                                if (AppUtils.isAppInstalled(Constants.XMS_UPDATE)) {
                                    try {
                                        AppUtils.launchApp(Constants.XMS_UPDATE)
                                        sendEventBus(ExitAppEvent(isRemoveAll = false, isExit = true))
                                    } catch (e: Exception) {
                                    }
                                    //TODO:Not uninstall because login again
                                    //runDelayed({AppUtils.uninstallAppSilent(BuildConfig.APPLICATION_ID,true)},500)
                                } else if (FileCommonUtils.isFileExists(pathDownloadAPK)) {
                                    sendEventBus(UpdateVersionEvent(updateVersion))
                                }
                            }
                        }
                    }
                })
            }
            false -> {
                AppUtils.installApp(getPathDownloadStorage())
            }
        }
    }

    //UPDATE UI
    fun seekTo(context: Context, seekPosition: Long) = runServicePlaySong(context, PlaybackStatusDef.SEEK, seekPosition)

    private fun checkStorage() {
        isStorage = try {
            if (SDCardUtils.isSDCardEnableByEnvironment()) {
                var listSdCard = emptyList<SDCardUtils.SDCardInfo>()
                try {
                    listSdCard = SDCardUtils.getSDCardInfo()
                } catch (e: Exception) {
                    SDCardDef.STORAGE
                }
                if (listSdCard.size > 1) {
                    if (!checkIsStorage()) {
                        AlbumDownload().queryAll().map {
                            it.isDownload = false
                            it.save()
                        }
                        setStorage(isSDCard = true)
                    }
                    SDCardDef.USB
                } else {
                    if (checkIsStorage()) {
                        AlbumDownload().queryAll().map {
                            it.isDownload = false
                            it.save()
                        }
                        setStorage(isSDCard = false)
                    }
                    SDCardDef.STORAGE
                }
            } else {
                SDCardDef.STORAGE
            }
        } catch (e: Exception) {
            SDCardDef.STORAGE
        }
    }

    fun checkInfoStorage(): Boolean {
        val sizeStorage: Long = if (isStorage) {
            SDCardUtils.getInternalAvailableSize()
        } else {
            SDCardUtils.getSDCardInfo()[1].availableSize
        }
        sendEventBus(DownloadSongStorageEvent(isStorage, ConvertUtils.byte2FitMemorySize(sizeStorage, 2)))
        return isNotEnoughSpace(sizeStorage)
    }

    fun updatePlayingProgressEvent(currentPosition: Long, duration: Long, isUpdate: Boolean = false) {
        sendEventBus(UpdatePlayingProgressEvent(currentPosition, duration, isUpdate))
    }

    //GET API
    fun sendLogPlayStart(albumId: String, key: String, title: String, artists: String) {
        if (isLostConnection) return
        //if (isLog) Logger.d("sendLogPlayStart")
        //Logger.e("sendLogPlayStart:"+albumId+"key="+ key+"title="+ title+"artists="+ artists)
        getCompositeDisposable().add(repository.sendLogPlayStart(albumId, key, title, artists).subscribeOn(Schedulers.io()).subscribe())
    }

    private fun sendLogDownloadTracking() {
        if (isLostConnection && isSendLogDownload) return
        getLogSongDownload()?.apply {
            if (size == 0) return
            getCompositeDisposable().add(repository.sendLogDownloadTracking(Gson().toJson(this)).subscribeOn(Schedulers.io()).subscribe({
                when (it.status) {
                    CallApiDef.OK -> {
                        isSendLogDownload = true
                        if (isLog) Logger.d("Send Log OK")
                        repository.realmManager.clearLogDownload()
                    }
                    else -> {
                        if (isLog) Logger.d("Error")
                        //addLogSongError(Constants.SONGKEY, Constants.ALBUMID, "Send Log Download " + it.status)
                    }
                }
            }, {
                it.printStackTrace()
                addLogSongError(Constants.SONGKEY, Constants.ALBUMID, it.message)
            }))
        }
    }

    fun sendLogPlayTracking(songKey: String = "", albumID: Int = -1, logSongPlayInfo: LogSongPlayInfo? = null) {
        if (isLostConnection) return
        val listSong = if (logSongPlayInfo != null) {
            Gson().toJson(mutableListOf(logSongPlayInfo))
        } else {
            Gson().toJson(getLogSongPlay())
        }
        //Logger.e("sendLogPlayTracking=$listSong")
        getCompositeDisposable().add(repository.sendLogPlayTracking(listSong).subscribeOn(Schedulers.io()).subscribe({
            when (it.status) {
                CallApiDef.OK -> {
                    if (isLog) Logger.d("Send Log OK")
                    if (songKey.isEmpty()) repository.realmManager.clearLogPlay()
                }
                else -> {
                    if (isLog) Logger.d("Error")
                    if (songKey.isNotEmpty()) addLogSongPlay(songKey, albumID)
                }
            }
        }, {
            it.printStackTrace()
            addLogSongError(Constants.SONGKEY, Constants.ALBUMID, it.message)
            if (songKey.isNotEmpty()) addLogSongPlay(songKey, albumID)
        }))
    }

    fun getUserInfo() {
        if (isLostConnection) return
        getCompositeDisposable().add(repository.getUserProfile().subscribeOn(Schedulers.io()).subscribe({
            pref {
                put(PrefDef.PRE_USER, it)
                sendEventBus(UpdateUserInfoEvent())
            }
        }, {
            it.printStackTrace()
            addLogSongError(Constants.SONGKEY, Constants.ALBUMID, it.message)
            if (isLog) Logger.d("Error")
            sendEventBus(UpdateUserInfoEvent())
        }))
    }

    fun syncDataAlbum(context: Context, scheduleState: Int?, albumId: Int) {
        if (scheduleState == ScheduleDef.STATE_ONTIME) {
            if (isLog) Logger.d("syncDataAlbum:SERVICE_ONTIME")
            albumID = albumId
            syncDataAndService(context, SyncDef.SERVICE_ONTIME)
            return
        }
        if (isLostConnection) {
            if (isLog) Logger.d("syncDataAlbum:syncDataAndService=$scheduleState")
            syncDataAndService(context)
            return
        }
        if (scheduleState != ScheduleDef.STATE_FIST_PLAY) {
            if (isLog) Logger.e("syncDataAlbum:scheduleState=$scheduleState")
            syncDataAndService(context)
        }
        if (isLog) Logger.e("syncDataAlbum:syncDataUpdateDisposable=$scheduleState")
        jobSync = mutableListOf()
        var countData = 0
        syncDataUpdateDisposable = repository.syncDataAlbum()
            .subscribeOn(Schedulers.io())
            .map { albumInfo ->
                isComplete = false
                if (isLog) Logger.e("syncDataComplete=$isComplete")
                albumInfo.setting?.apply {
                    if (isLog) Logger.e("setting:$this")
                    repository.updateStatus(resetConfig()).subscribe({ status ->
                        if (status) {
                            when {
                                restart -> try {
                                    ShellUtils.execCmd("reboot now", AppUtils.isAppRoot(), isDebug)
                                    jobSync = null
                                    removeDisposable(syncDataUpdateDisposable)
                                } catch (e: IOException) {
                                    e.printStackTrace()
                                }
                                logOutRemove -> {
                                    sendEventBus(ExitAppEvent(true))
                                    jobSync = null
                                    removeDisposable(syncDataUpdateDisposable)
                                }
                                logOut -> {
                                    sendEventBus(ExitAppEvent())
                                    jobSync = null
                                    removeDisposable(syncDataUpdateDisposable)
                                }
                            }
                        }
                    }, {
                        it.printStackTrace()
                    })
                }
                albumInfo
            }.map { albumInfo ->
                albumInfo.listAlbum?.let { listAlbum ->
                    viewModelScope.launch(ioScope) {
                        listAlbum.map { album ->
                            album?.apply {
                                jobSync?.plusAssign(launch {
                                    var response: JsonObject<ListAlbum>? = null
                                    try {
                                        response = repository.getApiService().getAlbumDetail(App.deviceInfoString, PreferenceManager.getString(PrefDef.PRE_TOKEN), id.toString())
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                    response?.data?.listSongDetails?.let { listSong ->
                                        if (isLog) Logger.e("id=" + id + "checkIsAlbumShuffle=" + checkIsAlbumShuffle(id) + "shuffle=" + shuffle + "listSong=" + listSong.size)
                                        if (shuffle != checkIsAlbumShuffle(id)) setAlbumsShuffle(id, shuffle)
                                        if (shuffle) {
                                            listSong.shuffle()
                                            if (isLog) Logger.e("setShuffleListSongAlbum:" + listSong.map { it.title })
                                        }
                                        //TODO TEST
                                        //listSong.find { it.key == "youtube_xmsvn_2677_xmsvn_nfs8NYg7yQM" }?.online = 1
                                        /*Logger.e("listSong1111==${listSong.map { it.toString() }}")
                                        listSong.forEachIndexed { index, songDetail ->
                                            if(index == 1) {
                                                songDetail.online = YoutubeDef.PLAY_VIDEO
                                                songDetail.streamUrl = "https://storage.googleapis.com/exoplayer-test-media-1/gen-3/screens/dash-vod-single-segment/video-avc-baseline-480.mp4"
                                            }
                                            if(index == 0){
                                                songDetail.online = YoutubeDef.PLAY_VIDEO
                                                songDetail.streamUrl = "https://bestvpn.org/html5demos/assets/dizzy.mp4"
                                            }
                                        }
                                        Logger.e("listSong1111==${listSong.map { it.toString() }}")*/
                                        insertSongDetails(id, listSong)
                                        countData += 1
                                    } ?: run {
                                        countData += 1
                                    }
                                    if (isLog) Logger.e("countData=$countData listAlbum.size=${listAlbum.size}")
                                    if (countData == listAlbum.size) {
                                        syncDataComplete(context, shuffle, albumInfo.setting?.sendLog)
                                    }
                                })
                            } ?: run {
                                syncDataAndService(context)
                            }
                        }
                        jobSync?.forEach {
                            it.join()
                        }
                    }
                }
            }.subscribe({}, {
                it.printStackTrace()
                isComplete = true
                syncDataAndService(context)
                addLogSongError(Constants.SONGKEY, Constants.ALBUMID, it.message)
                if (isLog) Logger.d("playAlbumOnSyncCompleted:Error")
            }, {
                removeDisposable(syncDataUpdateDisposable)
            })
    }

    private fun syncDataComplete(context: Context, isshuffle: Boolean = false, sendLog: Boolean? = false) {
        isComplete = true
        syncDataAndService(context)
        checkSyncDataDelete = true
        isShuffle = isshuffle
        repository.realmManager.removeSongTemp()
        if (sendLog == true) repository.sendLogDatabase(getLogDatabase())
    }

    fun syncLinkSongDetails(keySong: String) = repository.updateLinkSongDetails(keySong)

    //RUN SERVICE
    private fun runServiceDownload(context: Context, state: String = PlaybackStatusDef.PLAYING) {
        if (isLog) Logger.d("runServiceDownload=$albumID")
        if (albumID == -1) return
        if (isLostConnection) {
            if (isLog) Logger.d("isLostConnection=$albumID")
            runServicePlaySong(context)
            return
        }
        if (checkInfoStorage()) {
            sendEventBus(NotHaveEnoughSpaceEvent())
            runServicePlaySong(context)
            return
        }
        //if (isLog)
        Logger.e("checkIsDownloadDone=" + runConvertLoudNorm() + "LoudNormMusicService=" + LoudNormMusicService.isRunning + "MediaService=" + MediaService.isPlaying)
        val intent = Intent(context, DownloadService::class.java)
        intent.action = DownloadService.DOWNLOAD_ALBUM
        intent.putExtra(DownloadService.EXTRA_ALBUM_ID, albumID)
        context.startService(intent)
        if (runConvertLoudNorm() && MediaService.isPlaying && !LoudNormMusicService.isRunning) {
            val intentLoudNorm = Intent(context, LoudNormMusicService::class.java)
            intentLoudNorm.action = LoudNormMusicService.SYNC_LOUDNORM_MUSIC
            intentLoudNorm.putExtra(LoudNormMusicService.EXTRA_ALBUM_ID, albumID)
            context.startService(intentLoudNorm)
        } else if (!runConvertLoudNorm() && LoudNormMusicService.isRunning) {
            val intentLoudNorm = Intent(context, LoudNormMusicService::class.java)
            intentLoudNorm.action = LoudNormMusicService.STOP_LOUDNORM_MUSIC
            context.startService(intentLoudNorm)
        }
    }

    private fun runServiceSchedule(context: Context) {
        if (isLog) Logger.d("runServiceSchedule")
        val scheduleService = Intent(context, ScheduleService::class.java)
        scheduleService.action = ScheduleService.SYNC_DATA_SCHEDULE
        context.startService(scheduleService)
    }

    private fun runServiceCleanTemp(context: Context) {
        val deleteFileService = Intent(context, DeleteFileService::class.java)
        deleteFileService.action = DeleteFileService.SYNC_DELETE_TEMP
        context.startService(deleteFileService)
    }

    fun runServicePlaySong(context: Context, state: String = PlaybackStatusDef.PLAYING, seekPosition: Long = 0L) {
        if (isLog) Logger.e("runServicePlaySong=$state MediaService.isPlaying=${MediaService.isPlaying} isPlaying=$isPlaying")
        when (state) {
            PlaybackStatusDef.PLAYING -> {
                if (!MediaService.isPlaying && isPlaying) {
                    if (isLog) Logger.d("PlaybackStatusDef.PLAYING")
                    val intent = Intent(context, MediaService::class.java)
                    intent.action = PlaybackStatusDef.PLAYING
                    context.startService(intent)
                }
            }
            PlaybackStatusDef.STOP_NOW -> {
                if (MediaService.isPlaying) {
                    if (isLog) Logger.d("PlaybackStatusDef.STOP")
                    val intent = Intent(context, MediaService::class.java)
                    intent.action = PlaybackStatusDef.STOP_NOW
                    context.startService(intent)
                }
            }
            PlaybackStatusDef.SEEK -> {
                if (MediaService.isPlaying) {
                    if (isLog) Logger.d("PlaybackStatusDef.SEEK")
                    val intent = Intent(context, MediaService::class.java)
                    intent.action = PlaybackStatusDef.SEEK
                    intent.putExtra(MediaService.EXTRA_SEEK, seekPosition)
                    context.startService(intent)
                }
            }
            PlaybackStatusDef.COMPLETE_STOP -> {
                if (isLog) Logger.d("PlaybackStatusDef.COMPLETE_STOP")
                val intent = Intent(context, MediaService::class.java)
                intent.action = PlaybackStatusDef.COMPLETE_STOP
                context.startService(intent)
            }
            PlaybackStatusDef.ONTIME_ONTOP -> {
                if (isLog) Logger.d("PlaybackStatusDef.ONTIME")
                val intent = Intent(context, MediaService::class.java)
                intent.action = PlaybackStatusDef.ONTIME_ONTOP
                context.startService(intent)
            }
        }
    }
}
