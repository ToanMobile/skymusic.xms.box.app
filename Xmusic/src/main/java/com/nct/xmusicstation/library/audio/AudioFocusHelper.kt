/*
 * Copyright 2017 Google Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.nct.xmusicstation.library.audio

import android.content.Context
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.AudioManager.OnAudioFocusChangeListener
import android.os.Build
import android.util.Log
import androidx.annotation.NonNull
import androidx.annotation.RequiresApi
import androidx.media.AudioAttributesCompat

/**
 * A class to help request and abandon audio focus, with proper handling of API 26+
 * audio focus changes.
 */
class AudioFocusHelper
/**
 * Creates an AudioFocusHelper given a {@see Context}.
 *
 *
 * This does not request audio focus.
 *
 * @param context The current context.
 */
(@NonNull context: Context) {

    private val mImpl: AudioFocusHelperImpl
    private var mDefaultChangeListener: DefaultAudioFocusListener? = null

    init {
        val audioManager = context
                .applicationContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mImpl = AudioFocusHelperImplApi26(audioManager)
        } else {
            mImpl = AudioFocusHelperImplBase(audioManager)
        }
    }

    /**
     * Builds an [OnAudioFocusChangeListener] to control an
     * [AudioFocusAwarePlayer] in response to audio focus changes.
     *
     *
     * This function is intended to be used in conjuction with an [AudioFocusRequestCompat]
     * as follows:
     * `
     * AudioFocusRequestCompat focusRequest =
     * new AudioFocusRequestCompat.Builder(AudioManager.AUDIOFOCUS_GAIN)
     * .setOnAudioFocusChangeListener(audioFocusHelper.getListenerForPlayer(player))
     * // etc...
     * .build();
    ` *
     *
     * @param player The player that will respond to audio focus changes.
     * @return An [OnAudioFocusChangeListener] to control the player.
     */
    fun getListenerForPlayer(@NonNull player: AudioFocusAwarePlayer): OnAudioFocusChangeListener {
        if (mDefaultChangeListener != null && mDefaultChangeListener!!.player == player) {
            return mDefaultChangeListener as DefaultAudioFocusListener
        }
        mDefaultChangeListener = DefaultAudioFocusListener(mImpl, player)
        return mDefaultChangeListener as DefaultAudioFocusListener
    }

    /**
     * Requests audio focus for the player.
     *
     * @param audioFocusRequestCompat The audio focus request to perform.
     * @return `true` if audio focus was granted, `false` otherwise.
     */
    fun requestAudioFocus(audioFocusRequestCompat: AudioFocusRequestCompat): Boolean {
        return mImpl.requestAudioFocus(audioFocusRequestCompat)
    }

    /**
     * Abandons audio focus.
     *
     * @param audioFocusRequestCompat The audio focus request to abandon.
     */
    fun abandonAudioFocus(audioFocusRequestCompat: AudioFocusRequestCompat) {
        mImpl.abandonAudioFocus()
    }

    internal interface AudioFocusHelperImpl {
        fun requestAudioFocus(audioFocusRequestCompat: AudioFocusRequestCompat): Boolean

        fun abandonAudioFocus()

        fun willPauseWhenDucked(): Boolean
    }

    private open class AudioFocusHelperImplBase internal constructor(internal val mAudioManager: AudioManager) : AudioFocusHelperImpl {
        internal var mAudioFocusRequestCompat: AudioFocusRequestCompat? = null

        override fun requestAudioFocus(audioFocusRequestCompat: AudioFocusRequestCompat): Boolean {
            // Save the focus request.
            mAudioFocusRequestCompat = audioFocusRequestCompat

            // Check for possible problems...
            if (audioFocusRequestCompat.acceptsDelayedFocusGain()) {
                val message = "Cannot request delayed focus on API " + Build.VERSION.SDK_INT

                // Make an exception to allow the developer to more easily find this code path.
                val stackTrace = UnsupportedOperationException(message)
                        .fillInStackTrace()
                Log.w(TAG, "Cannot request delayed focus", stackTrace)
            }

            Log.e("no problem", "ok")
            mAudioFocusRequestCompat?.apply {
                val listener = onAudioFocusChangeListener
                val streamType = audioAttributesCompat?.legacyStreamType!!
                val focusGain = focusGain
                return mAudioManager.requestAudioFocus(listener, streamType, focusGain) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
            }
            return false
        }

        override fun abandonAudioFocus() {
            if (mAudioFocusRequestCompat == null) {
                return
            }

            mAudioManager.abandonAudioFocus(
                    mAudioFocusRequestCompat!!.onAudioFocusChangeListener)
        }

        override fun willPauseWhenDucked(): Boolean {
            if (mAudioFocusRequestCompat == null) {
                return false
            }

            val audioAttributes = mAudioFocusRequestCompat!!.audioAttributesCompat

            val pauseWhenDucked = mAudioFocusRequestCompat!!.willPauseWhenDucked()
            val isSpeech = audioAttributes != null && audioAttributes.contentType === AudioAttributesCompat.CONTENT_TYPE_SPEECH
            return pauseWhenDucked || isSpeech
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private class AudioFocusHelperImplApi26 internal constructor(audioManager: AudioManager) : AudioFocusHelperImplBase(audioManager) {
        private var mAudioFocusRequest: AudioFocusRequest? = null

        override fun requestAudioFocus(audioFocusRequestCompat: AudioFocusRequestCompat): Boolean {
            // Save and unwrap the compat object.
            mAudioFocusRequestCompat = audioFocusRequestCompat
            mAudioFocusRequest = audioFocusRequestCompat.audioFocusRequest

            return mAudioManager.requestAudioFocus(mAudioFocusRequest!!) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        }

        override fun abandonAudioFocus() {
            mAudioManager.abandonAudioFocusRequest(mAudioFocusRequest!!)
        }
    }

    /**
     * Implementation of an Android Oreo inspired [OnAudioFocusChangeListener].
     */
    private class DefaultAudioFocusListener (private val mImpl: AudioFocusHelperImpl, internal val player: AudioFocusAwarePlayer) : OnAudioFocusChangeListener {

        private var mResumeOnFocusGain = false

        override fun onAudioFocusChange(focusChange: Int) {
            when (focusChange) {
                AudioManager.AUDIOFOCUS_GAIN -> {
                    Log.e("AAA", "1")
                    if (mResumeOnFocusGain) {
                        player.play()
                        mResumeOnFocusGain = false
                    } else if (player.isPlaying) {
                        player.setVolume(MEDIA_VOLUME_DEFAULT)
                    }
                }
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                    Log.e("AAA", "2")
                    if (!mImpl.willPauseWhenDucked()) {
                        player.setVolume(MEDIA_VOLUME_DUCK)
                        return
                    }
                    Log.e("AAA", "3")
                    mResumeOnFocusGain = player.isPlaying
                    player.pause()
                }

                // This stream doesn't duck, so fall through and handle it the
                // same as if it were an AUDIOFOCUS_LOSS_TRANSIENT.
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                    Log.e("AAA", "3")
                    mResumeOnFocusGain = player.isPlaying
                    player.pause()
                }
                AudioManager.AUDIOFOCUS_LOSS -> {
                    Log.e("AAA", "4")
                    mResumeOnFocusGain = false
                    player.stop()
                    mImpl.abandonAudioFocus()
                }
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK -> Log.e("AAA", "5")
                AudioManager.AUDIOFOCUS_REQUEST_DELAYED -> Log.e("AAA", "6")
                AudioManager.AUDIOFOCUS_REQUEST_FAILED -> Log.e("AAA", "7")
            }
        }

        companion object {

            private val MEDIA_VOLUME_DEFAULT = 1.0f
            private val MEDIA_VOLUME_DUCK = 0.2f
        }
    }

    companion object {
        private val TAG = "AudioFocusHelper"
    }
}
