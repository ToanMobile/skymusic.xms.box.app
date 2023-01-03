package com.nct.xmusicstation.callback

import com.google.android.exoplayer2.ExoPlaybackException

interface IPlayerStateListener {
    fun onVideoLoading()

    fun onVideoEnd()

    fun onVideoStarted()

    fun onVideoError(error : ExoPlaybackException?)

    fun onVideoTracking(progress: Long, duration : Long)

    fun onVideoSizeChanged(width : Int)
}