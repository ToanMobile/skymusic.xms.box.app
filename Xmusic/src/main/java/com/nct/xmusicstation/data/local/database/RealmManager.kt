@file:Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")

package com.nct.xmusicstation.data.local.database

import android.annotation.SuppressLint
import com.blankj.utilcode.util.AppUtils
import com.blankj.utilcode.util.FileUtils
import com.blankj.utilcode.util.SDCardUtils
import com.blankj.utilcode.util.ShellUtils
import com.google.gson.JsonObject
import com.nct.xmusicstation.data.local.prefs.pref
import com.nct.xmusicstation.data.model.log.LogDownloadSongInfo
import com.nct.xmusicstation.data.model.log.LogSongErrorInfo
import com.nct.xmusicstation.data.model.log.LogSongPlayInfo
import com.nct.xmusicstation.data.model.song.*
import com.nct.xmusicstation.define.LoudNormDef
import com.nct.xmusicstation.define.PrefDef
import com.nct.xmusicstation.define.RealmDef
import com.nct.xmusicstation.utils.*
import com.orhanobut.logger.Logger
import com.toan_itc.core.kotlinify.collections.isNotNullOrEmpty
import com.vicpin.krealmextensions.*
import io.realm.Realm
import io.realm.Realm.getDefaultInstance
import io.realm.RealmList
import org.joda.time.LocalDateTime
import org.joda.time.format.DateTimeFormat
import java.io.IOException
import java.util.*

/**
 * Created by Toan.IT on 11/04/17.
 * Email: huynhvantoan.itc@gmail.com
 */

class RealmManager : RepositoryData {
    private val TAG = this::class.java.simpleName + ":"
    private val isLog = false

    override fun setAlbumsDownload(albumID: Int?, isUpdate: Boolean) {
        if (isUpdate) {
            SongDetailDownload().query { equalTo(RealmDef.ALBUM_ID, albumID) }.apply {
                if (isNotNullOrEmpty()) {
                    map {
                        it.countError = 0
                    }
                    saveAll()
                }
            }
        }
        var isDownloadComplete = false
        var listSize = 0
        ListAlbum().queryFirst { isNotNull(RealmDef.ID).equalTo(RealmDef.ID, albumID) }?.listSongDetails?.apply {
            if (isNotNullOrEmpty()) {
                listSize = size
                isDownloadComplete = true
                map {
                    if (isLog) Logger.e(
                        TAG + "checkIsSongDetailsDownload=" + (SongDetailDownload().queryFirst { equalTo(RealmDef.SONG_KEY, it.key) }?.title
                            ?: "error") + "isFileExists=" + !FileUtils.isFileExists(
                            getFolderDownloadedSong(
                                it.key,
                                it.streamUrl
                            )
                        ) + "\n getFolderDownloadedSong=" + getFolderDownloadedSong(it.key, it.streamUrl)
                    )
                    val songDownload = SongDetailDownload().queryFirst { equalTo(RealmDef.SONG_KEY, it.key) }
                    if (songDownload == null)
                        isDownloadComplete = false
                    else if (songDownload.isError == true && songDownload.countError <= 3 && !FileUtils.isFileExists(getFolderDownloadedSong(it.key, it.streamUrl)))
                        isDownloadComplete = false
                }
            }
        }
        if (isLog) Logger.e(TAG + "setAlbumsDownload=" + isDownloadComplete + "isUpdate=" + isUpdate)
        AlbumDownload().queryFirst { equalTo(RealmDef.ALBUM_ID, albumID) }.apply {
            if (this != null) {
                totalSong = listSize
                isDownload = isDownloadComplete
                countError = if (isDownloadComplete || isUpdate)
                    0
                else
                    countError.plus(1)
                this.save()
            } else {
                if (isLog) Logger.e("new AlbumDownload")
                val albumDownload = AlbumDownload()
                albumDownload.albumId = albumID ?: -1
                albumDownload.totalSong = listSize
                albumDownload.isDownload = isDownloadComplete
                if (isDownloadComplete || isUpdate)
                    albumDownload.countError = 0
                else
                    albumDownload.countError = albumDownload.countError.plus(1)
                albumDownload.save()
            }
        }
    }

    override fun setAlbumsShuffle(albumID: Int?, IsShuffle: Boolean) {
        if (isLog) Logger.d(TAG + "setAlbumsShuffle")
        AlbumDownload().queryFirst { isNotNull(RealmDef.ALBUM_ID).equalTo(RealmDef.ALBUM_ID, albumID) }
            ?.apply { isShuffle = IsShuffle }?.save()
    }

    override fun setStorage(isSDCard: Boolean) {
        val checkStorage = CheckStorage()
        checkStorage.isSdCard = isSDCard
        checkStorage.save()
    }

    override fun checkIsStorage(): Boolean = CheckStorage().isSdCard ?: false

    override fun checkIsAlbumDownload(albumID: Int?): Boolean = AlbumDownload().queryFirst { equalTo(RealmDef.ALBUM_ID, albumID) }?.isDownload ?: false

    override fun runConvertLoudNorm(): Boolean = AlbumDownload().query { equalTo(RealmDef.IS_DOWNLOAD, false) }.isEmpty() && Setting().queryFirst()?.loudNorm ?: false

    override fun checkIsAlbumShuffle(albumID: Int?): Boolean = AlbumDownload().queryFirst { equalTo(RealmDef.ALBUM_ID, albumID) }?.isShuffle ?: false

    override fun saveSongDetailsDownload(keySong: String, albumID: Int, isSongError: Boolean, callbackSuccess: () -> Unit?) {
        SongDetailDownload().queryFirst { equalTo(RealmDef.SONG_KEY, keySong) }.apply {
            if (isLog) Logger.d(TAG + "saveSongDetailsDownload")
            if (this != null) {
                albumId = albumID
                isError = isSongError
                countError = if (isSongError)
                    countError.plus(1)
                else
                    0
                this.save()
            } else {
                convertSongDetailsToSongDownload(keySong, albumID, isSongError).save()
            }
        }
        if (!isSongError) {
            addLogSongDownload(keySong, albumID, false, "")
        }
        callbackSuccess()
    }

    private fun convertSongDetailsToSongDownload(keySong: String, albumID: Int, isSongError: Boolean): SongDetailDownload {
        val songDetailDownload = SongDetailDownload()
        SongDetail().queryFirst { equalTo(RealmDef.SONG_KEY, keySong) }?.apply {
            songDetailDownload.key = key
            songDetailDownload.albumId = albumID
            songDetailDownload.title = title ?: ""
            songDetailDownload.artists = artists(artists)
            songDetailDownload.streamUrl = streamUrl
            songDetailDownload.isError = isSongError
            songDetailDownload.countError = 0
        } ?: run {
            songDetailDownload.key = keySong
            songDetailDownload.albumId = albumID
            songDetailDownload.isError = isSongError
            songDetailDownload.countError = 0
        }
        return songDetailDownload
    }

    override fun getSizeListSongPlay(albumID: Int?): Int {
        return when {
            is1970() -> SongDetailDownload()
                .query { equalTo(RealmDef.IS_ERROR, false) }
                .filterNot { it.key.startsWith("sk_") || it.key.startsWith("user_") }.count()
            albumID == -1 -> ListAlbum().queryFirst()?.listSongDetails?.size ?: 0
            else -> ListAlbum().queryFirst { equalTo(RealmDef.ID, albumID) }?.listSongDetails?.size ?: 0
        }
    }

    override fun getSongPlay1970(songIndex: Int): SongDetail? = SongDetail().queryFirst {
        equalTo(
            RealmDef.SONG_KEY,
            SongDetailDownload().query { equalTo(RealmDef.IS_ERROR, false) }.filterNot { it.key.startsWith("sk_") || it.key.startsWith("user_") }[songIndex].key
        )
    }

    override fun getSongPlay(albumID: Int, songIndex: Int): SongDetail? = ListAlbum().queryFirst { equalTo(RealmDef.ID, albumID) }?.listSongDetails?.get(songIndex)

    override fun getAlbumSchedule(): List<Schedule> = Schedule().queryAll()

    override fun getOnTopSchedule(albumID: Int?): Boolean = Schedule().queryFirst { equalTo(RealmDef.ALBUM_ID, albumID) }?.ontop ?: false

    override fun isOntopOntime(albumID: Int?): Boolean {
        val album = Schedule().queryFirst { equalTo(RealmDef.ALBUM_ID, albumID) }
        Logger.e("isOntopOntime:::"+ album.toString())
        return (album?.ontop ?: false) || (album?.ontime ?: false)
    }

    override fun isOntime(albumID: Int?): Boolean = Schedule().queryFirst { equalTo(RealmDef.ALBUM_ID, albumID) }?.ontime ?: false

    override fun getListSongDownloadError(albumID: Int?): List<SongDetailDownload> = SongDetailDownload().query {
        equalTo(RealmDef.ALBUM_ID, albumID).equalTo(RealmDef.IS_ERROR, true).lessThanOrEqualTo(RealmDef.COUNT_ERROR, 3)
    }

    override fun setShuffleListSongAlbum(albumID: Int?) {
        if (checkIsAlbumDownload(albumID)) {
            if (isLog) Logger.d(TAG + "setShuffleListSongAlbum:checkIsAlbumDownload$albumID")
            ListAlbum().queryFirst { isNotNull(RealmDef.ID).equalTo(RealmDef.ID, albumID) }?.apply {
                if (listSongDetails?.size!! > 0) {
                    listSongDetails?.shuffle()
                    if (isLog) Logger.d(TAG + "setShuffleListSongAlbum:" + listSongDetails?.map { it.title })
                    save()
                }
            }
        }
    }

    override fun checkIsSongDetailsDownload(keySong: String): Boolean =
        SongDetailDownload().queryFirst { equalTo(RealmDef.SONG_KEY, keySong) }?.isError == false

    override fun checkIsSongDownloadError(keySong: String): Boolean {
        SongDetailDownload().queryFirst { equalTo(RealmDef.SONG_KEY, keySong) }?.apply {
            return isError == true && countError > 3
        } ?: run { return false }
        return false
    }

    fun insertDataAlbum(albumInfo: AlbumInfo) {
        if (isLog) Logger.d(TAG + "insertDataAlbum$albumInfo")
        albumDownloadNeedRemove(albumInfo.listAlbum)
        clearCache()
        albumInfo.save()
    }

    override fun insertSongDetails(albumID: Int?, songInfo: RealmList<SongDetail>) {
        val listAlbum = ListAlbum().queryFirst { isNotNull(RealmDef.ID).equalTo(RealmDef.ID, albumID) }
        listAlbum?.apply {
            listSongDetails = songInfo
            save()
            if (isLog) Logger.d(TAG + "listSongDetails=" + listSongDetails?.size + "albumID=" + albumID)
        }
        setAlbumsDownload(albumID, true)
    }

    fun removeSongTemp() {
        if (isLog) Logger.e(TAG + "removeSongTemp")
        loop@ for (songDownload: SongDetailDownload in SongDetailDownload().queryAll()) {
            for (songDetails: SongDetail in SongDetail().queryAll()) {
                if (songDownload.key == songDetails.key) {
                    continue@loop
                }
            }
            if (isLog) Logger.e("removeSongDetailsDownload=${songDownload.title}")
            SongDetailDownload().delete { equalTo(RealmDef.SONG_KEY, songDownload.key) }
        }
    }

    private fun albumDownloadNeedRemove(listAlbum: RealmList<ListAlbum?>?) {
        if (isLog) Logger.e(TAG + "albumNeedRemove:listAlbum=" + listAlbum?.size)
        if (listAlbum.isNullOrEmpty()) {
            AlbumDownload().deleteAll()
            return
        }
        AlbumDownload().query {
            loop@ for (album: AlbumDownload in this.findAll()) {
                for (albumNew: ListAlbum? in listAlbum) {
                    if ((album.albumId ?: -1) == albumNew?.id) {
                        continue@loop
                    }
                }
                if (isLog) Logger.e(TAG + "albumNeedRemove=${album.albumId}")
                AlbumDownload().delete { equalTo(RealmDef.ALBUM_ID, album.albumId) }
            }
        }
    }

    fun updateLinkSongDetails(songKey: String, songDetails: SongDetails?) {
        songDetails?.song?.streamUrl?.let { linkUrl ->
            if (linkUrl.isNotEmpty()) {
                SongDetail().query { equalTo(RealmDef.SONG_KEY, songKey) }.map { song ->
                    if (isLog) Logger.e("updateLinkSongDetails:linkNew=$linkUrl and linkOld=${song.streamUrl}")
                    song.streamUrl = linkUrl
                }
            }
        }
    }

    override fun getListAllSongDownload(): List<SongDetailDownload?>? = SongDetailDownload().queryAll()

    override fun getSizeListAllSong(): Long = SongDetail().count()

    override fun getSizeListDownloadAlbum(albumID: Int?): Int = SongDetailDownload().query { equalTo(RealmDef.ALBUM_ID, albumID) }.size

    override fun getSizeListDownloadAll(): Long = SongDetailDownload().count()

    override fun getFistAlbumDetails(): ListAlbum? = ListAlbum().queryFirst()

    override fun getAlbumDetails(albumID: Int?): ListAlbum? =
        ListAlbum().queryFirst { isNotNull(RealmDef.ID).equalTo(RealmDef.ID, albumID) }

    override fun getFistSongDetailsFindAlbum(albumID: Int?): SongDetail? {
        ListAlbum().queryFirst { equalTo(RealmDef.ID, albumID) }?.listSongDetails?.let {
            return if (it.size > 0)
                it[0]
            else
                null
        }
        addLogSongError(Constants.SONGKEY, albumID, "getFistSongDetailsFindAlbum=null")
        return null
    }

    override fun getListSongDetailsFindAlbum(albumID: Int?): List<SongDetail>? =
        ListAlbum().queryFirst { isNotNull(RealmDef.ID).equalTo(RealmDef.ID, albumID) }?.listSongDetails

    @SuppressLint("DefaultLocale")
    override fun getAlbumNextDownload(albumIDPlaying: Int?): Int? {
        var albumIDNextDownload = -1
        Schedule().query {
            equalTo(
                "scheduleType",
                LocalDateTime().toString(DateTimeFormat.forPattern("EEEE").withLocale(Locale.ENGLISH)).toUpperCase()
            )
        }.apply {
            if (size > 0) {
                map {
                    if (!checkIsAlbumDownload(it.albumId)) {
                        if (isLog) Logger.d("listSchedule:albumIDNextDownload=${it.albumId}")
                        albumIDNextDownload = it.albumId ?: -1
                        return albumIDNextDownload
                    }
                }
            }
            ListAlbum().queryAll().apply {
                if (size > 0) {
                    if (isLog) Logger.d(TAG + "ListAlbum:listAlbum=${this}")
                    map {
                        if (!checkIsAlbumDownload(it.id)) {
                            if (isLog) Logger.d("ListAlbum:albumIDNextDownload=${it.id}")
                            albumIDNextDownload = it.id ?: -1
                            return albumIDNextDownload
                        }
                    }
                    if (isLog) Logger.d("getAlbumNextDownload:albumIDPlaying=$albumIDPlaying::albumIDNextDownload=$albumIDNextDownload")
                }
            }
        }
        return albumIDNextDownload
    }

    override fun setSongLoudNorm(keySong: String, index: Int) {
        SongDetailDownload().queryFirst { equalTo(RealmDef.SONG_KEY, keySong) }?.apply {
            loudNorm = index
            save()
        }
    }

    override fun getListSongNotCheckLoudNorm(albumID: Int?): List<SongDetailDownload>? = SongDetailDownload().query {
        equalTo(RealmDef.ALBUM_ID, albumID)?.equalTo(
            RealmDef.LOUDNORM,
            LoudNormDef.FILE_RAW
        )
    }

    override fun getListSongNotLoudNorm(albumID: Int?): List<SongDetailDownload>? = SongDetailDownload().query {
        equalTo(RealmDef.ALBUM_ID, albumID)?.equalTo(
            RealmDef.LOUDNORM,
            LoudNormDef.FILE_NEED_LOUDNORM
        )
    }

    //LOG
    override fun addLogSongDownload(songID: String?, albumID: Int?, isSongdownload: Boolean, message: String?) {
        LogDownloadSongInfo().apply {
            key = songID ?: ""
            albumId = albumID
            isError = isSongdownload
            messageError = message
            save()
        }
    }

    override fun addLogSongPlay(songID: String?, albumID: Int?) {
        if (isLog) Logger.d(TAG + "addLogSongPlay=" + songID + "albumId=" + albumID)
        LogSongPlayInfo().apply {
            key = songID
            albumId = albumID
            timestamp = System.currentTimeMillis()
            save()
        }
    }

    override fun addLogSongError(songID: String?, albumID: Int?, message: String?) {
        /*getDefaultInstance().executeTransaction { realm ->
            try {
                val listLogError = realm.where(LogSongErrorInfo::class.java).findAll()
                Logger.d("addLogSongError:count=${listLogError.size}")
                if (listLogError.size > 100) {
                    for (i in 0..99) {
                        listLogError[i]?.deleteFromRealm()
                    }
                } else {
                    LogSongErrorInfo().apply {
                        songId = songID
                        albumId = albumID
                        messageError = message
                        timestamp = System.currentTimeMillis()
                        realm.copyToRealmOrUpdate(this)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }*/
    }

    override fun getLogSongDownload(): List<LogDownloadSongInfo>? = LogDownloadSongInfo().queryAll()

    override fun getLogSongPlay(): List<LogSongPlayInfo>? = LogSongPlayInfo().queryAll()

    override fun getLogDatabase(): String {
        val jsonLog = JsonObject()
        val runtime = Runtime.getRuntime()
        val usedMemory = runtime.totalMemory() - runtime.freeMemory()
        val availableMemory = runtime.maxMemory() - usedMemory
        jsonLog.addProperty("SongDetailDownload", SongDetailDownload().queryAll().toString())
        jsonLog.addProperty("AlbumDownload", AlbumDownload().queryAll().toString())
        jsonLog.addProperty("AlbumInfo", AlbumInfo().queryAll().toString())
        jsonLog.addProperty("MemoryFree", formatStorageToDisplaySize(availableMemory.toDouble()))
        jsonLog.addProperty("Storage", SDCardUtils.getSDCardInfo().toString())
        jsonLog.addProperty(
            "CPU", try {
                ShellUtils.execCmd("dumpsys cpuinfo", AppUtils.isAppRoot(), true).toString()
            } catch (e: IOException) {
                e.printStackTrace()
            }.toString()
        )
        //if(isLog)
        if (isLog) Logger.e("jsonLog=$jsonLog")
        return jsonLog.toString()
    }

    override fun resetConfig(): String {
        val jsonSetting = JsonObject()
        jsonSetting.addProperty("logOut", false)
        jsonSetting.addProperty("logOutRemove", false)
        jsonSetting.addProperty("sendLog", false)
        jsonSetting.addProperty("restart", false)
        if (isLog) Logger.e("resetConfig=$jsonSetting")
        return jsonSetting.toString()
    }

    fun clearLogDownload() = LogDownloadSongInfo().deleteAll()

    fun clearLogPlay() = LogSongPlayInfo().deleteAll()

    private fun clearCache() {
        AlbumInfo().deleteAll()
        ListAlbum().deleteAll()
        Schedule().deleteAll()
        SongDetail().deleteAll()
        Setting().deleteAll()
    }

    fun clearAll(isRemoveAll: Boolean) {
        if (isRemoveAll) {
            AlbumDownload().deleteAll()
            SongDetailDownload().deleteAll()
            LogSongPlayInfo().deleteAll()
            LogDownloadSongInfo().deleteAll()
            LogSongErrorInfo().deleteAll()
            pref {
                put(PrefDef.LOGIN, false)
                put(PrefDef.PRE_USER, "")
                put(PrefDef.PRE_LAST_SONG, "")
            }
            clearCache()
        } else {
            LogSongPlayInfo().deleteAll()
            LogSongErrorInfo().deleteAll()
        }
    }

    override fun getRealm(): Realm = getDefaultInstance()

    override fun closeRealm() {
        getDefaultInstance().apply {
            if (!isClosed) {
                if (isLog) Logger.e("closeRealm")
                close()
            }
        }
    }
}
