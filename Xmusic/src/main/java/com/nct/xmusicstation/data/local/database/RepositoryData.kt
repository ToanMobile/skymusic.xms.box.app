package com.nct.xmusicstation.data.local.database

import com.nct.xmusicstation.data.model.log.LogDownloadSongInfo
import com.nct.xmusicstation.data.model.log.LogSongPlayInfo
import com.nct.xmusicstation.data.model.song.ListAlbum
import com.nct.xmusicstation.data.model.song.Schedule
import com.nct.xmusicstation.data.model.song.SongDetail
import com.nct.xmusicstation.data.model.song.SongDetailDownload
import io.realm.Realm
import io.realm.RealmList

/**
 * Created by Toan.IT on 11/3/17.
 * Email:Huynhvantoan.itc@gmail.com
 */

interface RepositoryData {

    fun setAlbumsDownload(albumID: Int?, isUpdate: Boolean = false)

    fun checkIsAlbumDownload(albumID: Int?): Boolean

    fun setStorage(isSDCard: Boolean = false)

    fun checkIsStorage(): Boolean

    fun setSongLoudNorm(keySong: String, index: Int)

    fun getListSongNotLoudNorm(albumID: Int?): List<SongDetailDownload>?

    fun getListSongNotCheckLoudNorm(albumID: Int?): List<SongDetailDownload>?

    fun setAlbumsShuffle(albumID: Int?, IsShuffle: Boolean = false)

    fun setShuffleListSongAlbum(albumID: Int?)

    fun checkIsAlbumShuffle(albumID: Int?): Boolean

    fun runConvertLoudNorm(): Boolean

    fun saveSongDetailsDownload(keySong: String, albumID: Int, isSongError: Boolean, callbackSuccess: () -> Unit? = {})

    fun getSizeListSongPlay(albumID: Int?): Int

    fun getSongPlay1970(songIndex: Int): SongDetail?

    fun getSongPlay(albumID: Int, songIndex: Int): SongDetail?

    fun getListSongDownloadError(albumID: Int?): List<SongDetailDownload>

    fun checkIsSongDetailsDownload(keySong: String): Boolean

    fun checkIsSongDownloadError(keySong: String): Boolean

    fun getFistSongDetailsFindAlbum(albumID: Int?): SongDetail?

    fun getListSongDetailsFindAlbum(albumID: Int?): List<SongDetail>?

    fun getListAllSongDownload(): List<SongDetailDownload?>?

    fun getSizeListDownloadAll(): Long

    fun getSizeListDownloadAlbum(albumID: Int?): Int

    fun getSizeListAllSong(): Long

    fun getAlbumSchedule(): List<Schedule>

    fun getOnTopSchedule(albumID: Int?): Boolean

    fun isOntopOntime(albumID: Int?): Boolean

    fun isOntime(albumID: Int?): Boolean

    fun getAlbumNextDownload(albumIDPlaying: Int?): Int?

    fun insertSongDetails(albumID: Int?, songInfo: RealmList<SongDetail>)

    fun getAlbumDetails(albumID: Int?): ListAlbum?

    fun getFistAlbumDetails(): ListAlbum?

    //LOG

    fun addLogSongDownload(songID: String?, albumID: Int?, isSongdownload: Boolean, message: String?)

    fun addLogSongPlay(songID: String?, albumID: Int?)

    fun addLogSongError(songID: String?, albumID: Int?, message: String?)

    fun getLogSongDownload(): List<LogDownloadSongInfo>?

    fun getLogSongPlay(): List<LogSongPlayInfo>?

    fun getLogDatabase(): String

    fun resetConfig(): String

    fun getRealm(): Realm

    fun closeRealm()
}