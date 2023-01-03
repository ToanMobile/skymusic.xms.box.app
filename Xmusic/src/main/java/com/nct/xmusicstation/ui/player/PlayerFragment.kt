@file:Suppress("unused", "UNUSED_PARAMETER")

package com.nct.xmusicstation.ui.player

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context.BIND_AUTO_CREATE
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.databinding.DataBindingComponent
import androidx.databinding.DataBindingUtil
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.ui.TimeBar
import com.liulishuo.filedownloader.FileDownloader
import com.nct.xmusicstation.R
import com.nct.xmusicstation.app.App
import com.nct.xmusicstation.binding.FragmentDataBindingComponent
import com.nct.xmusicstation.callback.IPlayerStateListener
import com.nct.xmusicstation.data.local.prefs.PreferenceManager
import com.nct.xmusicstation.data.model.auth.UserInfo
import com.nct.xmusicstation.data.model.song.SongDetail
import com.nct.xmusicstation.databinding.PlayerFragmentBinding
import com.nct.xmusicstation.define.PlaybackStatusDef
import com.nct.xmusicstation.define.PrefDef
import com.nct.xmusicstation.event.*
import com.nct.xmusicstation.library.CenterLayoutManager
import com.nct.xmusicstation.library.exoplayer.PlayerExoHelper
import com.nct.xmusicstation.scheduler.SchedulerHelper
import com.nct.xmusicstation.service.DownloadService
import com.nct.xmusicstation.service.MediaService
import com.nct.xmusicstation.service.ScheduleService
import com.nct.xmusicstation.service.SyncService
import com.nct.xmusicstation.ui.base.BaseDataEventFragment
import com.nct.xmusicstation.ui.login.LoginFragment
import com.nct.xmusicstation.ui.player.adapter.ListSongAdapter
import com.nct.xmusicstation.ui.player.adapter.ScheduleAdapter
import com.nct.xmusicstation.utils.*
import com.orhanobut.logger.Logger
import com.toan_itc.core.architecture.autoCleared
import com.toan_itc.core.kotlinify.reactive.runSafeOnMain
import com.toan_itc.core.richutils._switchFragment
import com.toan_itc.core.richutils.removeFragment
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.realm.RealmList
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.joda.time.DateTimeZone
import org.joda.time.LocalDateTime
import java.util.concurrent.TimeUnit

/**
 * Created by Toan.IT on 12/10/18.
 * Email:Huynhvantoan.itc@gmail.com
 */

class PlayerFragment : BaseDataEventFragment<PlayerViewModel>(), PlayerView,
    TimeBar.OnScrubListener, IPlayerStateListener {
    private var binding by autoCleared<PlayerFragmentBinding>()
    private var dataBindingComponent: DataBindingComponent = FragmentDataBindingComponent(this)
    private val isLog = false
    private var disposable: Disposable? = null
    private var playerExoHelper: PlayerExoHelper? = null

    companion object {
        fun newInstance() = PlayerFragment()
    }

    private var isUserSeeking: Boolean = false
    private var mService: MediaService? = null
    private var mBound: Boolean = false
    private var adapterListSong: ListSongAdapter? = null
    private var adapterListSchedule: ScheduleAdapter? = null
    private var isAlbumChange = false
    private var albumId: Int? = 0
    private var indexAlbumPlay = -1

    private val mConnection = object : ServiceConnection {
        override fun onServiceConnected(componentName: ComponentName, iBinder: IBinder) {
            try {
                mBound = true
                mService = (iBinder as? MediaService.MediaPlayerBinder)?.service
                mService?.apply {
                    activity?.apply {
                        if (!isFinishing) {
                            if (isPlaying()) {
                                val lastPlaySong = PreferenceManager.getEntity(
                                    PrefDef.PRE_LAST_SONG,
                                    SongDetail::class.java
                                )
                                lastPlaySong?.apply {
                                    Logger.e("PlayerFragment:refreshList:11111=")
                                    refreshList(this, false)
                                    updateCurrentPlayingSongInfo(title, artists)
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        override fun onServiceDisconnected(componentName: ComponentName) {
            mBound = false
            mService = null
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val dataBinding = DataBindingUtil.inflate<PlayerFragmentBinding>(
            inflater,
            setLayoutResourceID(),
            container,
            false,
            dataBindingComponent
        )
        binding = dataBinding
        return dataBinding.root
    }

    override fun getViewModel(): Class<PlayerViewModel> = PlayerViewModel::class.java

    override fun setLayoutResourceID(): Int = R.layout.player_fragment

    @SuppressLint("SetTextI18n")
    override fun initView() {
        initListScheduleAndSong()
        with(binding) {
            logout.setOnClickListener { quitApp() }
            txtVersions.text = "Version: " + App.deviceInfo.appVersion
            timeBar.addListener(this@PlayerFragment)
            deviceInfoTvStorage.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_device, 0, 0)
            btnUpdateVersions.setOnClickListener {
                this@PlayerFragment.viewModel.updateApp()
            }
        }
    }

    override fun initData() {
        createPlayer()
        viewModel.syncData(context)
    }

    override fun createPlayer() {
        try {
            activity?.let {
                if (!it.isFinishing) {
                    if (!mBound) {
                        val intent = Intent(it, MediaService::class.java)
                        it.bindService(intent, mConnection, BIND_AUTO_CREATE)
                    }
                    playerExoHelper = PlayerExoHelper(binding.playerView)
                    playerExoHelper?.setPlayerStateListener(this)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            createPlayer()
        }
    }

    override fun stopPlayer() {
        activity?.let {
            val intent = Intent(it, MediaService::class.java)
            intent.action = PlaybackStatusDef.STOP_NOW
            it.startService(intent)
        }
    }


    override fun quitApp() {
        if (isLog) Logger.e("quitApp")
        context?.let {
            AlertDialog.Builder(it, R.style.DialogeTheme)
                .setTitle(R.string.notice)
                .setMessage(R.string.logout_prompt)
                .setPositiveButton(
                    R.string.logout_only
                ) { _, _ ->
                    exitApp()
                    showLoginPage()
                }
                .setNegativeButton(R.string.no, null)
                .setNeutralButton(
                    R.string.logout_and_clear_data
                ) { _, _ ->
                    exitApp(true)
                    showLoginPage()
                }
                .show()
        }
    }

    override fun showLoginPage() {
        if (isLog) Logger.e("showLoginPage")
        activity?.apply {
            removeFragment(this@PlayerFragment)
            _switchFragment(null, LoginFragment.newInstance(), R.id.container)
        }
    }

    fun updateCurrentPlayingSongInfo(songTitle: String?, artistNames: RealmList<String>?) {
        with(binding) {
            playerSongName.text = songTitle
            playerSongName.isSelected = true
            playerSinger.text = artists(artistNames)
        }
    }

    private fun exitApp(isRemoveAll: Boolean = false) {
        cancelAllService()
        viewModel.clearAll(isRemoveAll)
    }

    private fun cancelAllService() {
        stopPlayer()
        FileDownloader.getImpl()?.apply {
            if (isServiceConnected) {
                if (isLog) Logger.e("PlayerFragment:FileDownloaderunBindService")
                pauseAll()
                clearAllTaskData()
                unBindService()
            }
        }
        if (mBound) {
            if (isLog) Logger.e("PlayerFragment:unbindService")
            mBound = false
            activity?.unbindService(mConnection)
        }
        context?.let {
            with(SchedulerHelper) {
                cancelScheduleSyncSchedule(it)
            }
            if (isLog) Logger.e("PlayerFragment:cancelAllService")
            val syncIntent = Intent(it, SyncService::class.java)
            syncIntent.action = SyncService.STOP_SYNC
            it.startService(syncIntent)
            val intentSchedule = Intent(it, ScheduleService::class.java)
            it.stopService(intentSchedule)
            val downloadIntent = Intent(it, DownloadService::class.java)
            downloadIntent.action = DownloadService.STOP_DOWNLOAD
            it.startService(downloadIntent)
            val mediaIntent = Intent(it, MediaService::class.java)
            it.stopService(mediaIntent)
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onExitAppEvent(event: ExitAppEvent) {
        activity?.apply {
            if (isLog) Logger.e("onExitAppEvent")
            exitApp(event.isRemoveAll)
            if (event.isExit) finishAffinity() else showLoginPage()
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onDownloadSongStorageEvent(event: DownloadSongStorageEvent) {
        event.apply {
            with(binding) {
                deviceInfoTvStorage.setCompoundDrawablesWithIntrinsicBounds(
                    0,
                    if (isInternalStorage) R.drawable.ic_device else R.drawable.ic_sdcard,
                    0,
                    0
                )
                deviceInfoTvStorage.text = availableBytes
                //Show info song
                txtTotalSongAll.text = String.format(
                    getString(R.string.total_song_all),
                    this@PlayerFragment.viewModel.getSizeListDownloadAll()
                )
                txtTotalSongAlbum.text = String.format(
                    getString(R.string.total_song_album),
                    this@PlayerFragment.viewModel.getSizeListDownloadAlbum(albumID = albumId)
                )
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onUpdateUserInfoEvent(event: UpdateUserInfoEvent) {
        PreferenceManager.getEntity(PrefDef.PRE_USER, UserInfo::class.java)?.user?.apply {
            binding.txtBranch.text = username
        }
    }

    @SuppressLint("StringFormatMatches")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onDownloadFistSongEvent(it: DownloadProgressEvent) {
        with(binding) {
            if (it.progressPercent < 0) {
                showHide(timeBar, true)
                showHide(playerTimeCurrent, true)
                showHide(playerTimeElapse, true)
                showHide(playerDownloadProgressBar, false)
                showHide(playerDownloadProgress, false)
                playerDownloadProgress.text = ""
                playerDownloadProgressBar.progress = 0
            } else {
                showHide(timeBar, false)
                showHide(playerTimeCurrent, false)
                showHide(playerTimeElapse, false)
                showHide(playerDownloadProgressBar, true)
                showHide(playerDownloadProgress, true)
                playerDownloadProgress.text =
                    getString(R.string.downloading_progress_pattern, it.progressPercent)
                playerDownloadProgressBar.progress = it.progressPercent.toInt()
            }
        }
    }

    private fun showHide(view: View, show: Boolean) {
        view.visibility = if (show) View.VISIBLE else View.INVISIBLE
    }

    @SuppressLint("SetTextI18n")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onScheduleAlbumEvent(albumSchedule: ScheduleAlbumEvent) {
        Logger.e("onScheduleAlbumEvent::$albumSchedule")
        with(binding) {
            if (albumSchedule.albumSchedule == null) {
                rcvSchedule.isGone = true
                return
            }
            if (txtKbps.isGone)
                txtKbps.isVisible = true
            if (rcvSchedule.isGone)
                rcvSchedule.isVisible = true
            if (rcvListSong.isGone)
                rcvListSong.isVisible = true
            isAlbumChange = true
            indexAlbumPlay = adapterListSchedule?.getListData?.indexOfFirst { it.albumId == albumSchedule.albumSchedule.albumId } ?: -1
            adapterListSchedule?.refreshData(this@PlayerFragment.viewModel.getListScheduleToday(), indexAlbumPlay)
            if (indexAlbumPlay == -1)
                return
            albumSchedule.albumSchedule.apply {
                this@PlayerFragment.viewModel.getAlbumDetails(albumId)?.apply {
                    this@PlayerFragment.albumId = albumId
                    txtAlbumShuffle.isSelected = shuffle
                    context?.let {
                        glideHelp(it, this.image ?: "", imgPlaylist)
                        glideHelp(it, this.image ?: "", imgPlaylistSmall)
                    }
                    txtAlbumName.text = albumName
                    txtAlbumSize.text = totalSongs.toString() + getString(R.string.total_song)
                    txtAlbumTime.text = getDurationToSecond(totalduration)
                }
                txtAlbumOnTime.isSelected = ontime
            }
            rcvSchedule.scrollToPosition(indexAlbumPlay)
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onSongInfoEvent(it: PlayingSongEvent) {
        with(binding) {
            refreshList(it.song, it.isDownload)
            if (!it.isDownload) {
                if (it.song != null) {
                    if (isLog) Logger.e("onSongInfoEvent===" + it.song.toString())
                    if (!it.song.title.isNullOrEmpty()) {
                        it.song.apply {
                            playerSongName.text = title
                            playerSongName.isSelected = true
                            playerSinger.text = artists(artists)
                            txtKbps.text = String.format(getString(R.string.kbps), kbit)
                        }
                    } else {
                        val lastPlaySong =
                            PreferenceManager.getEntity(PrefDef.PRE_LAST_SONG, SongDetail::class.java)
                        lastPlaySong?.apply {
                            updateCurrentPlayingSongInfo(title, artists)
                        }
                    }
                } else {
                    rcvListSong.isGone = true
                    txtKbps.isGone = true
                    txtAlbumName.text = ""
                    txtAlbumSize.text = ""
                    txtAlbumTime.text = ""
                    playerSongName.text = ""
                    playerSongName.isSelected = false
                    playerSinger.text = ""
                }
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onProgressEvent(it: UpdatePlayingProgressEvent) {
        with(binding) {
            it.apply {
                if (isUpdate) {
                    with(timeBar) {
                        setDuration(duration)
                        setPosition(currentPosition)
                    }
                } else {
                    playerTimeCurrent.text = formatSecondDuration(currentPosition)
                    playerTimeElapse.text = formatSecondDuration(duration - currentPosition)
                    timeBar.setPosition(currentPosition)
                }
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onNextSongEvent(nextAlbumSchedule: ScheduleNextAlbumEvent) {
        with(binding) {
            adapterListSchedule?.refreshData(this@PlayerFragment.viewModel.getListScheduleToday(), indexAlbumPlay)
            if (nextAlbumSchedule.nextAlbumSchedule != null) {
                nextAlbumSchedule.nextAlbumSchedule?.apply {
                    if (isLog) Logger.e("onNextSongEvent=" + nextAlbumSchedule.nextAlbumSchedule.toString())
                    this@PlayerFragment.viewModel.getAlbumDetails(albumId!!)?.apply {
                        isAlbumChange = true
                        errorView.isVisible = true
                        errorView.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_empty, 0, 0)
                        errorView.text =
                            String.format("Playlist kế: %s\n%s - %s", name, fromTime, toTime)
                        rcvListSong.isGone = true
                    }
                }
            } else {
                rcvListSong.isVisible = true
                errorView.isGone = true
                playerDownloadProgress.isGone = true
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onNotHaveEnoughSpaceEvent(notHaveEnoughSpaceEvent: NotHaveEnoughSpaceEvent) {
        with(binding) {
            if (rcvListSong.isVisible)
                rcvListSong.isGone = true
            if (errorView.isVisible && errorView.text == getString(R.string.not_enough_memory))
                return
            errorView.isVisible = true
            errorView.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_disconnect, 0, 0)
            errorView.text = getString(R.string.not_enough_memory)
        }
    }

    @SuppressLint("SetTextI18n")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onUpdateVersionEvent(updateVersionEvent: UpdateVersionEvent) {
        with(binding.btnUpdateVersions) {
            text = "Update Version: " + updateVersionEvent.updateVersion
            isVisible = true
        }
        context?.apply {
            val intentSync = Intent(this, SyncService::class.java)
            intentSync.action = SyncService.SYNC_DATA
            startService(intentSync)
        }
    }

    override fun onScrubMove(timeBar: TimeBar, position: Long) {
        with(binding) {
            playerTimeCurrent.text = formatSecondDuration(position)
            playerTimeElapse.text = formatSecondDuration(position)
        }
    }

    override fun onScrubStart(timeBar: TimeBar, position: Long) {

    }

    override fun onScrubStop(timeBar: TimeBar, position: Long, canceled: Boolean) {
        if (isLog) Logger.e("onScrubStop")
        context?.apply {
            viewModel.seekTo(this, position)
        }
    }

    private fun initListScheduleAndSong() {
        with(binding) {
            txtTimeSchedule.text =
                LocalDateTime.now(DateTimeZone.getDefault()).toString("EEEE dd/MM/yyyy")
            rcvSchedule.layoutManager = CenterLayoutManager(context)
            rcvListSong.layoutManager = CenterLayoutManager(context)
            adapterListSong = ListSongAdapter()
            adapterListSchedule = ScheduleAdapter()
            rcvSchedule.adapter = adapterListSchedule
            rcvListSong.adapter = adapterListSong
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun updateAlbumChangeEvent(updateAlbumChangeEvent: UpdateAlbumChangeEvent) {
        isAlbumChange = true
        indexAlbumPlay = ScheduleService.indexSchedule
    }

    private fun refreshList(songDetail: SongDetail?, isDownload: Boolean = false) {
        disposable?.dispose()
        disposable = Observable.interval(1, TimeUnit.SECONDS)
            .filter {
                PlayerViewModel.isComplete
            }
            .map {
                viewModel.getListSong()
            }
            .runSafeOnMain()
            .subscribe({
                initList(it, songDetail, isDownload)
                disposable?.dispose()
            }, {
                it.printStackTrace()
                disposable?.dispose()
            })
    }

    private fun initList(listSongDetails: List<SongDetail>?, songDetail: SongDetail?, isDownload: Boolean) {
        with(binding) {
            if (rcvListSong.isGone && songDetail != null && !isDownload && errorView.isGone)
                rcvListSong.isVisible = true
            if (isAlbumChange || adapterListSchedule?.isCheckListNull() == true) {
                isAlbumChange = false
                txtTimeSchedule.text =
                    LocalDateTime.now(DateTimeZone.getDefault()).toString("EEEE dd/MM/yyyy")
                this@PlayerFragment.viewModel.getListScheduleToday()?.apply {
                    adapterListSchedule?.refreshData(this, indexAlbumPlay)
                } ?: run {
                    rcvSchedule.isGone = true
                    rcvListSong.isGone = true
                }
                if (isLog) Logger.e("PlayerFragment:initList:refreshList=" + listSongDetails?.map { it.title })
                adapterListSong?.refreshData(listSongDetails)
                if (songDetail != null) {
                    //if(isLog) Logger.e("PlayerFragment:listSong=" + listSongDetails.toString())
                    listSongDetails?.mapIndexed { index, songdetail ->
                        if (songDetail.id == songdetail.id) {
                            if (isLog) Logger.e("PlayerFragment:isAlbumChange:refreshList=" + this.toString() + "songdetails=" + songdetail.toString() + "indexAlbumPlay=$indexAlbumPlay")
                            adapterListSong?.songPlayFist(index)
                            rcvListSong.smoothScrollToPosition(index)
                        }
                    }
                } else {

                }
            } else {
                adapterListSchedule?.apply {
                    if (isCheckListNull()) {
                        if (isLog) Logger.e("PlayerFragment:Fist11111=" + this@PlayerFragment.viewModel.getListScheduleToday()?.map { it.toString() })
                        this@PlayerFragment.viewModel.getListScheduleToday()?.apply {
                            adapterListSchedule?.refreshData(this, indexAlbumPlay)
                            if (rcvSchedule.isGone)
                                rcvSchedule.isVisible = true
                        } ?: run {
                            rcvSchedule.isGone = true
                            rcvListSong.isGone = true
                        }
                    }
                }
                if (isLog) Logger.e("PlayerFragment:initList:adapterListSong=" + adapterListSong)
                adapterListSong?.apply {
                    if (isCheckListNull() || txtAlbumShuffle.isSelected != PlayerViewModel.isShuffle) {
                        if (isLog) Logger.e("PlayerFragment:getList()=" + this@PlayerFragment.viewModel.getListSong()?.map { it.title })
                        refreshData(this@PlayerFragment.viewModel.getListSong())
                    }
                    if (songDetail != null) {
                        if (isLog) Logger.e("PlayerFragment:refreshList=" + getList()?.map { it.title })
                        getList()?.mapIndexed { index, songdetail ->
                            if (songDetail.id == songdetail.id) {
                                songPlay(index)
                                rcvListSong.smoothScrollToPosition(index)
                            }
                        }
                    }
                }
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun youtubeEventEvent(youtubeEvent: YoutubeEvent) {
        if (isLog) Logger.e("youtube=$youtubeEvent")
        if (youtubeEvent.isStart) {
            binding.playerView.isVisible = true
            playerExoHelper?.runExoPlayer(youtubeEvent.urlStream)
        } else {
            binding.playerView.isGone = true
        }
    }

    override fun onVideoStarted() {
        if (!MediaService.isPlaying) MediaService.isPlaying = true
    }

    override fun onVideoEnd() {
        MediaService.isPlaying = false
        mService?.initNextSong()
    }

    override fun onVideoError(error: ExoPlaybackException?) {
        error?.printStackTrace()
    }

    override fun onVideoLoading() {

    }

    override fun onVideoSizeChanged(width: Int) {

    }

    override fun onVideoTracking(progress: Long, duration: Long) {

    }

    override fun onDestroyView() {
        cancelAllService()
        if (isLog) Logger.v("onDestroyView:" + this.javaClass.simpleName)
        if (EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().unregister(this)
        }
        playerExoHelper?.releaseExoPlayer()
        playerExoHelper?.release()
        binding.timeBar.removeListener(this)
        super.onDestroyView()
    }

    override fun onStart() {
        super.onStart()
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this)
        }
    }
}