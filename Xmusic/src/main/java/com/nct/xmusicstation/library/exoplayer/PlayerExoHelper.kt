package com.nct.xmusicstation.library.exoplayer

import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.video.VideoSize
import com.nct.xmusicstation.callback.IPlayerStateListener
import com.orhanobut.logger.Logger

/**
 * Created by ToanDevMobile on 24/10/18.
 * Email:huynhvantoan.itc@gmail.com
 */

class PlayerExoHelper internal constructor(playerView: CustomExoPlayerView) : Player.Listener {
    private var mCustomExoPlayerView: CustomExoPlayerView = playerView
    private var linkUrl = ""
    private var mPlayerStateListener: IPlayerStateListener? = null

    init {
        mCustomExoPlayerView.initializePlayer()
        mCustomExoPlayerView.createExoPlayer(this)
    }

    internal fun updateLink(linkUrl: String) {
        this.linkUrl = linkUrl
    }

    internal fun runExoPlayer(linkUrl: String? = "", position: Long = 0L) {
        if (linkUrl.isNullOrEmpty()) {
            mCustomExoPlayerView.runExoPlayer(this.linkUrl, position)
        } else {
            mCustomExoPlayerView.runExoPlayer(linkUrl, position)
        }
    }

    internal fun checkPlayer(): Boolean = mCustomExoPlayerView.checkPlayer()

    internal fun setMutePlayer() = mCustomExoPlayerView.setMutePlayer()

    internal fun releaseExoPlayer() = mCustomExoPlayerView.releasePlayer()

    internal fun replay() = mCustomExoPlayerView.replay()

    internal fun playExoPlayer() = mCustomExoPlayerView.setPlayWhenReady(true)

    internal fun pauseExoPlayer() = mCustomExoPlayerView.setPlayWhenReady(false)

    internal fun stopExoPlayer() = mCustomExoPlayerView.stopExoPlayer()

    internal fun seekTo(position: Long) = mCustomExoPlayerView.seekTo(position)

    internal fun isPlaying(): Boolean = mCustomExoPlayerView.isPlaying()

    internal fun setPlayerStateListener(mPlayerStateListener: IPlayerStateListener) {
        this.mPlayerStateListener = mPlayerStateListener
    }

    internal fun trackingTime() {
        getExoPlayer()?.apply {
            mPlayerStateListener?.onVideoTracking(progress = currentPosition, duration = duration)
        }
    }

    internal fun setResizeMode(resizeMode: Int) = mCustomExoPlayerView.setResizeModeRaw(resizeMode)

    internal fun switchTargetView(oldPlayerView: CustomExoPlayerView?, newPlayerView: CustomExoPlayerView?) {
        mCustomExoPlayerView.switchTargetView(oldPlayerView, newPlayerView)
    }

    internal fun getExoPlayer(): Player? = mCustomExoPlayerView.getPlayer()

    internal fun setView(view: CustomExoPlayerView?) {
        view?.apply {
            mCustomExoPlayerView.setSizeTextView(width, height)
        }
    }

    internal fun release() {
        mPlayerStateListener = null
    }

    override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
        Logger.e("onPlayerStateChanged=$playbackState")
        mPlayerStateListener?.apply {
            when (playbackState) {
                Player.STATE_BUFFERING -> onVideoLoading()
                Player.STATE_READY -> onVideoStarted()
                Player.STATE_ENDED -> onVideoEnd()
                Player.STATE_IDLE -> {}
            }
        }
    }

    override fun onPlayerError(error: ExoPlaybackException) {
        super.onPlayerError(error)
        mPlayerStateListener?.onVideoError(error)
        error.printStackTrace()
    }

    override fun onVideoSizeChanged(videoSize: VideoSize) {
        super.onVideoSizeChanged(videoSize)
        // Logger.e("onVideoSizeChanged=width:${width}_height${height}_pixelWidthHeightRatio$pixelWidthHeightRatio")
        mPlayerStateListener?.onVideoSizeChanged(videoSize.width)
    }

}
