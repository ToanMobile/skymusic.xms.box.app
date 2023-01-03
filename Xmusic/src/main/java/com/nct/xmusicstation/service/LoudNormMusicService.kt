package com.nct.xmusicstation.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.blankj.utilcode.util.FileUtils
import com.nct.xmusicstation.define.LoudNormDef
import com.nct.xmusicstation.ui.player.PlayerViewModel
import com.nct.xmusicstation.utils.getFolderDownloadedSong
import com.nct.xmusicstation.utils.getFolderDownloadedSongLoudNorm
import com.orhanobut.logger.Logger
import dagger.android.AndroidInjection
import nl.bravobit.ffmpeg.ExecuteBinaryResponseHandler
import nl.bravobit.ffmpeg.FFmpeg
import nl.bravobit.ffmpeg.FFtask
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Created by Toan.IT on 02/10/18.
 * Email:Huynhvantoan.itc@gmail.com
 */

@Singleton
class LoudNormMusicService : Service() {
    @Inject
    lateinit var playerModel: PlayerViewModel
    private val isLog = false
    private var ffTask : FFtask? = null

    companion object {
        var isRunning = false
        const val SYNC_LOUDNORM_MUSIC = "SYNC_LOUDNORM_MUSIC"
        const val STOP_LOUDNORM_MUSIC = "STOP_LOUDNORM_MUSIC"
        const val EXTRA_ALBUM_ID = "EXTRA_ALBUM_ID"
    }

    override fun onCreate() {
        super.onCreate()
        AndroidInjection.inject(this)
    }

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if(intent == null) return START_STICKY
        if(isLog) Logger.d("LoudNormMusicService" + intent.action)
        when (intent.action) {
            SYNC_LOUDNORM_MUSIC -> {
                intent.extras?.apply {
                    checkBMPMusic(getInt(EXTRA_ALBUM_ID))
                }
            }
            STOP_LOUDNORM_MUSIC -> {
                //TODO TEST
                Logger.e("STOP_LOUDNORM_MUSIC:sendQuitSignal")
                ffTask?.sendQuitSignal()
                stopSelf()
            }
        }
        return START_STICKY
    }

    private fun checkBMPMusic(albumIdPlay: Int) {
        if(isLog) Logger.e("checkBMPMusic==== $isRunning")
        if (isRunning || !FFmpeg.getInstance(this).isSupported)
            return
        isRunning = true
        val ffmpeg = FFmpeg.getInstance(this)
        PlayerViewModel.checkSyncDataDelete = false
        val listSongNotCheckLoudNorm = playerModel.getListSongNotCheckLoudNorm(albumIdPlay)
        if (listSongNotCheckLoudNorm.isNullOrEmpty()) {
            val listSongNotLoudNorm = playerModel.getListSongNotLoudNorm(albumIdPlay)
            if(isLog) Logger.e("listSongNotLoudNorm=$listSongNotLoudNorm")
            listSongNotLoudNorm?.map { songDownload ->
                val dataSourcePath = getFolderDownloadedSong(songDownload.key, songDownload.streamUrl)
                if(isLog) Logger.e("songDetails=" + songDownload.title +"key=" + songDownload.key + "path=" + FileUtils.isFileExists(dataSourcePath))
                if (FileUtils.isFileExists(dataSourcePath))
                    runFFmpeg(ffmpeg, false, songDownload.key, dataSourcePath)
            }
        } else {
            if(isLog) Logger.e("listSongNotCheckLoudNorm=$listSongNotCheckLoudNorm")
            listSongNotCheckLoudNorm.map { songDownload ->
                if(isLog) Logger.e("songDetails=" + songDownload.title)
                runFFmpeg(ffmpeg, true, songDownload.key)
            }
        }
        if(isLog) Logger.e("isRunning==== false")
        PlayerViewModel.checkSyncDataDelete = true
        isRunning = false
    }

    private fun runFFmpeg(ffmpeg: FFmpeg, isCheck: Boolean = false, keySong: String = "", dataSourcePath: String = "", kbit: Int = 128) {
        return if (isCheck)
            execute(ffmpeg, isCheck, keySong, dataSourcePath,  "-i ${getFolderDownloadedSong(keySong, dataSourcePath)} -af ebur128 -f null -")
        else {
            when {
                kbit >= 320 -> execute(ffmpeg, isCheck, keySong, dataSourcePath, "-i ${getFolderDownloadedSong(keySong, dataSourcePath)} -af loudnorm=I=-16:dual_mono=true:print_format=summary -ab 320k ${getFolderDownloadedSongLoudNorm(
                    keySong, dataSourcePath
                )}")
                kbit in 128..256 -> execute(ffmpeg, isCheck, keySong, dataSourcePath, "-i ${getFolderDownloadedSong(keySong, dataSourcePath)} -af loudnorm=I=-16:dual_mono=true:print_format=summary -ab 256k ${getFolderDownloadedSongLoudNorm(
                    keySong, dataSourcePath
                )}")
                else -> execute(ffmpeg, isCheck, keySong, dataSourcePath,   "-i ${getFolderDownloadedSong(keySong, dataSourcePath)} -af loudnorm=I=-16:dual_mono=true:print_format=summary ${getFolderDownloadedSongLoudNorm(
                    keySong, dataSourcePath
                )}")
            }
        }
    }

    private fun execute(ffmpeg: FFmpeg, isCheck: Boolean, keySong: String = "", dataSourcePath: String, cmd : String) {
        if(isLog) Logger.e("cmd=${cmd.split(" ")}")
        if(ffTask?.isProcessCompleted == false)
            return
        ffTask = ffmpeg.execute(cmd.split(" ").toTypedArray(), object : ExecuteBinaryResponseHandler() {

            override fun onStart() {
                //if(isLog) Logger.e("onStart")
            }

            override fun onProgress(message: String?) {
                //if(isLog) Logger.e("onProgress:$message")
            }

            override fun onFailure(message: String?) {
                if(isLog) Logger.e("onFailure:$message")
            }

            override fun onSuccess(message: String?) {
                if(isLog) Logger.e("onSuccess:$message isCheck= $isCheck")
                if(isCheck){
                    if (message!!.isNotEmpty() && message.contains("Integrated loudness:")) {
                        try {
                            val stringLUFS = message.substring(message.indexOf("Integrated loudness:"), message.indexOf("Threshold"))
                            if (isLog) Logger.e("stringLUFS::==$stringLUFS")
                            var bpm = 12.0
                            try {
                                bpm = stringLUFS.substring(stringLUFS.indexOf("-") + 1, stringLUFS.indexOf(".") + 2).toDouble()
                            }catch (e: Exception){
                                e.printStackTrace()
                            }
                            if (isLog) Logger.e("bpm::==$bpm")
                            if (bpm in 12.0..15.0) {
                                if (isLog) Logger.e("XmusicFFmpegCHECK_RETURN_FILE_LOUDNORM")
                                playerModel.setSongLoudNorm(keySong, LoudNormDef.FILE_CHECK_OK)
                            } else {
                                if (isLog) Logger.e("XmusicFFmpegCHECK_RETURN_FILE_NEED_LOUDNORM")
                                playerModel.setSongLoudNorm(keySong, LoudNormDef.FILE_NEED_LOUDNORM)
                            }
                        }catch (e: Exception){
                            e.printStackTrace()
                            playerModel.setSongLoudNorm(keySong, LoudNormDef.FILE_CHECK_ERROR)
                        }
                    }else{
                        if(isLog) Logger.e("XmusicFFmpegCHECK_RETURN_CODE_ERROR")
                        playerModel.setSongLoudNorm(keySong, LoudNormDef.FILE_CHECK_ERROR)
                    }
                }else{
                    if(FileUtils.isFileExists(getFolderDownloadedSongLoudNorm(keySong, dataSourcePath))&& dataSourcePath.isNotEmpty()){
                        playerModel.setSongLoudNorm(keySong, LoudNormDef.FILE_LOUDNORM)
                        FileUtils.move(getFolderDownloadedSongLoudNorm(keySong, dataSourcePath), dataSourcePath)
                        if(isLog) Logger.e("XmusicFFmpeg.Convert_RETURN_CODE_SUCCESS==")
                    }else{
                        if(isLog) Logger.e("XmusicFFmpeg.Convert_RETURN_CODE_ERROR")
                        playerModel.setSongLoudNorm(keySong, LoudNormDef.FILE_LOUDNORM_ERROR)
                        FileUtils.delete(getFolderDownloadedSongLoudNorm(keySong, dataSourcePath))
                    }
                }
            }

            override fun onFinish() {
                //if(isLog) Logger.e("onFinish")
            }
        })
    }
    override fun onDestroy() {
        if(isLog) Logger.d("onDestroy:LoudNormMusicService")
        super.onDestroy()
    }
}