package com.nct.xmusicstation.library.exoplayer

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.annotation.AttrRes
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.core.net.toUri
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.ext.okhttp.OkHttpDataSource
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.source.dash.DashMediaSource
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.source.smoothstreaming.SsMediaSource
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout
import com.google.android.exoplayer2.upstream.FileDataSource
import com.google.android.exoplayer2.util.Util
import com.nct.xmusicstation.R
import com.orhanobut.logger.Logger
import okhttp3.OkHttpClient


/**
 * Created by ToanDevMobile on 24/2/19.
 * Email:huynhvantoan.itc@gmail.com
 */

class CustomExoPlayerView(context: Context, attrs: AttributeSet?, @AttrRes defStyleAttr: Int) :
    FrameLayout(context, attrs, defStyleAttr) {
    private var contentFrame: AspectRatioFrameLayout? = null
    private var player: ExoPlayer? = null
    private lateinit var dataSourceFactory: OkHttpDataSource.Factory
    private var mTextureView: MyTextureView? = null

    @JvmOverloads
    constructor(context: Context, attrs: AttributeSet? = null) : this(context, attrs, 0)

    fun initializePlayer() {
        val context = context.applicationContext
        LayoutInflater.from(context).inflate(R.layout.exoplayer_custom, this)
        contentFrame = findViewById(R.id.exoPlayer)
        mTextureView = findViewById(R.id.textureView)
        contentFrame?.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FILL
        val okHttpClient = OkHttpClient().newBuilder().build()
        dataSourceFactory = OkHttpDataSource.Factory(okHttpClient)
        dataSourceFactory.setUserAgent(Util.getUserAgent(context, context.getString(R.string.app_name)))
    }

    fun createExoPlayer(playerExoHelper: PlayerExoHelper) {
        player?.apply {
            releasePlayer()
            removeListener(playerExoHelper)
            clearVideoTextureView(mTextureView)
            playerExoHelper.release()
        }
        val trackSelector = DefaultTrackSelector(context)
        player = SimpleExoPlayer.Builder(context).setTrackSelector(trackSelector).build()
        player?.apply {
            setVideoTextureView(mTextureView)
            addListener(playerExoHelper)
        }
    }

    fun checkPlayer(): Boolean = player != null

    fun getPlayer(): Player? = player

    fun setMutePlayer() {
        player?.volume = 0f
    }

    fun runExoPlayer(linkUrl: String?, position: Long = 0L) {
        if (linkUrl.isNullOrEmpty())
            return
        player?.apply {
            setMediaSource(buildMediaResource(linkUrl.toUri()))
            prepare()
            if (position != 0L) {
                seekTo(position)
            }
            Logger.e("runExoPlayer=$linkUrl")
            playWhenReady = true
            volume = 1.0f
        }
    }

    @SuppressLint("SwitchIntDef")
    private fun buildMediaResource(linkStream: Uri): MediaSource {
        val mediaItem = MediaItem.fromUri(linkStream)
        val mediaSourceFactory = when (@C.ContentType val type = Util.inferContentType(linkStream)) {
            C.TYPE_DASH -> DashMediaSource.Factory(dataSourceFactory)
            C.TYPE_SS -> SsMediaSource.Factory(dataSourceFactory)
            C.TYPE_HLS -> HlsMediaSource.Factory(dataSourceFactory)
            C.TYPE_OTHER -> if (linkStream.path?.contains("http") == true) {
                ProgressiveMediaSource.Factory(dataSourceFactory)
            } else
                ProgressiveMediaSource.Factory(FileDataSource.Factory())
            else -> throw IllegalStateException("Unsupported type: $type")
        }
        return mediaSourceFactory.createMediaSource(mediaItem)
    }

    fun releasePlayer() {
        player?.release()
        player = null
    }

    fun isPlaying(): Boolean = player != null && player?.playWhenReady ?: false

    fun setPlayWhenReady(shouldAutoPlay: Boolean) {
        Logger.e("CustomExoPlayerView:setPlayWhenReady$shouldAutoPlay")
        player?.playWhenReady = shouldAutoPlay
    }

    fun setSizeTextView(width: Int, height: Int) = mTextureView?.adaptVideoSize(width, height)

    fun stopExoPlayer() = player?.stop()

    fun replay() = player?.seekTo(0)

    fun seekTo(position: Long) = player?.seekTo(position)

    fun setResizeModeRaw(resizeMode: Int) {
        contentFrame?.resizeMode = resizeMode
    }

    fun switchTargetView(oldPlayerView: CustomExoPlayerView?, newPlayerView: CustomExoPlayerView?) {
        if (oldPlayerView === newPlayerView) {
            return
        }
        if (newPlayerView != null) newPlayerView.player = player
        if (oldPlayerView != null) oldPlayerView.player = null
    }
}
