@file:Suppress("SENSELESS_COMPARISON")

package com.nct.xmusicstation.data

import android.annotation.SuppressLint
import com.nct.xmusicstation.BuildConfig
import com.nct.xmusicstation.app.App
import com.nct.xmusicstation.data.local.database.RealmManager
import com.nct.xmusicstation.data.local.prefs.PreferenceManager
import com.nct.xmusicstation.data.local.prefs.pref
import com.nct.xmusicstation.data.model.auth.Status
import com.nct.xmusicstation.data.model.auth.TokenInfo
import com.nct.xmusicstation.data.model.auth.UpdateInfo
import com.nct.xmusicstation.data.model.auth.UserInfo
import com.nct.xmusicstation.data.model.song.AlbumInfo
import com.nct.xmusicstation.data.model.song.ListAlbum
import com.nct.xmusicstation.data.remote.ApiService
import com.nct.xmusicstation.define.CallApiDef
import com.nct.xmusicstation.define.PrefDef
import com.nct.xmusicstation.utils.Constants
import com.orhanobut.logger.Logger
import io.reactivex.Flowable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Created by Toan.IT on 11/3/17.
 * Email:Huynhvantoan.itc@gmail.com
 */
@Singleton
class PlayerRepository
@Inject
constructor(val realmManager: RealmManager, private val apiService: ApiService) {
    private val isLog = false

    fun getUpdateVersion(): Flowable<UpdateInfo> =
        apiService.getUpdateVersion(App.deviceInfoString, PreferenceManager.getString(PrefDef.PRE_TOKEN))

    fun getUserProfile(): Flowable<UserInfo> =
        apiService.getUserProfile(App.deviceInfoString, PreferenceManager.getString(PrefDef.PRE_TOKEN))

    private fun refreshToken(): Flowable<TokenInfo> {
        if (isLog) Logger.d(
            "deviceInfoString=" + App.deviceInfoString + "PRE_TOKEN=" + PreferenceManager.getString(PrefDef.PRE_TOKEN) + "userId=" + App().getUserIdFromToken(
                PreferenceManager.getString(PrefDef.PRE_TOKEN)
            ).toString()
        )
        return apiService.refreshToken(
            App.deviceInfoString,
            BuildConfig.CLIENT_ID,
            BuildConfig.CLIENT_SERCET,
            "refresh_token_box",
            PreferenceManager.getString(PrefDef.PRE_TOKEN),
            App().getUserIdFromToken(PreferenceManager.getString(PrefDef.PRE_TOKEN)).toString()
        )
    }

    fun sendLogPlayStart(
        albumId: String,
        key: String,
        title: String,
        artists: String
    ): Flowable<Status> {
        if (isLog) Logger.e("sendLogPlayStart:"+App.deviceInfoString +"token="+ PreferenceManager.getString(PrefDef.PRE_TOKEN)+"albumId="+albumId+"key="+ key+"title="+ title+"artists="+ artists)
        return apiService.trackPlaySongStart(
            App.deviceInfoString,
            PreferenceManager.getString(PrefDef.PRE_TOKEN),
            albumId,
            key,
            title,
            artists
        )
    }

    fun sendLogDownloadTracking(data: String): Flowable<Status> {
        if (isLog) Logger.d(
            "deviceInfoString=" + App.deviceInfoString + "PRE_TOKEN=" + PreferenceManager.getString(
                PrefDef.PRE_TOKEN
            ) + "\n data=" + data
        )
        return apiService.trackDownloadSong(App.deviceInfoString, PreferenceManager.getString(PrefDef.PRE_TOKEN), data)
    }

    fun sendLogDatabase(data: String) {
        if (data.isEmpty())
            return
        //if(isLog) Logger.d("deviceInfoString=" + App.deviceInfoString + "PRE_TOKEN=" + PreferenceManager.getString(PrefDef.PRE_TOKEN) + "\n data=" + data)
        apiService.sendLog(App.deviceInfoString, PreferenceManager.getString(PrefDef.PRE_TOKEN), data).subscribe()
    }

    fun sendLogPlayTracking(data: String): Flowable<Status> {
        if (isLog) Logger.d(
            "deviceInfoString=" + App.deviceInfoString + "PRE_TOKEN=" + PreferenceManager.getString(
                PrefDef.PRE_TOKEN
            ) + "\n data=" + data
        )
        return apiService.trackPlaySong(App.deviceInfoString, PreferenceManager.getString(PrefDef.PRE_TOKEN), data)
    }

    fun syncDataAlbum(): Flowable<AlbumInfo> {
        return apiService.getBrandAlbums(App.deviceInfoString, PreferenceManager.getString(PrefDef.PRE_TOKEN), 1, 100)
            .filter {
                //Logger.e("updateData======="+it.updateData +"PRE_FIST_START==="+PreferenceManager.getBoolean(PrefDef.PRE_FIST_START, true))
                if (PreferenceManager.getBoolean(PrefDef.PRE_FIST_START, true)) {
                    it != null
                } else {
                    (it != null && it.updateData == 1) || (it != null && realmManager.getSizeListAllSong().toInt() == 0)
                }
            }
            .flatMap {
                when (it.status) {
                    CallApiDef.OK -> {
                        //TODO TEST
                        /* it.schedule?.map {
                             if (isLog) Logger.d("schedule=$it")
                             it?.let { schedule ->
                                 if (schedule.albumId == 136428) {
                                     schedule.fromTime = "14:55"
                                     schedule.toTime = "15:00"
                                     schedule.ontop = true
                                 }
                                 if (schedule.albumId == 204744) {
                                     schedule.fromTime = "11:00"
                                     schedule.toTime = "22:30"
                                 }
                             }
                         }*/
                        realmManager.insertDataAlbum(it)
                        Flowable.just(it)
                    }
                    else -> {
                        refreshToken()
                            .flatMap { data ->
                                if (isLog) Logger.d("refreshToken=$data")
                                if(data.token.isNotEmpty()){
                                    pref {
                                        put(PrefDef.PRE_TOKEN, data.token)
                                    }
                                }
                                Flowable.just(AlbumInfo())
                            }
                    }
                }
            }
    }

    fun getApiService(): ApiService = apiService

    @SuppressLint("CheckResult")
    fun updateLinkSongDetails(key: String) {
        apiService.getSongDetail(App.deviceInfoString, PreferenceManager.getString(PrefDef.PRE_TOKEN), key)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                realmManager.updateLinkSongDetails(key, it)
            }
    }

    fun updateStatus(setting: String): Flowable<Boolean> {
        if (isLog) Logger.e("setting=$setting")
        return apiService.updateStatus(
            App.deviceInfoString,
            PreferenceManager.getString(PrefDef.PRE_TOKEN),
            Constants.UPDATE_DATA_OK,
            setting
        ).flatMap {
            if(it.status.contentEquals(CallApiDef.OK))
                Flowable.just(true)
            else
                Flowable.just(false)
        }
    }

    fun getAlbumDetails(albumID: Int?): ListAlbum? = realmManager.getAlbumDetails(albumID)

    fun clearAll(isRemoveAll: Boolean) = realmManager.clearAll(isRemoveAll)

}