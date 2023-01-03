package com.nct.xmusicstation.library.audio

import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.AudioManager.OnAudioFocusChangeListener
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.annotation.IntDef
import androidx.annotation.RequiresApi
import androidx.media.AudioAttributesCompat
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy.SOURCE

/**
 * Compatibility version of an [AudioFocusRequest].
 */
class AudioFocusRequestCompat private constructor(val focusGain: Int,
                                                  internal val onAudioFocusChangeListener: OnAudioFocusChangeListener?,
                                                  internal val focusChangeHandler: Handler?,
                                                  val audioAttributesCompat: AudioAttributesCompat?,
                                                  private val mPauseOnDuck: Boolean,
                                                  private val mAcceptsDelayedFocusGain: Boolean) {

    internal val audioAttributes: AudioAttributes?
        @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
        get() = if (audioAttributesCompat != null)
            audioAttributesCompat.unwrap() as AudioAttributes
        else
            null

    internal val audioFocusRequest: AudioFocusRequest
        @RequiresApi(Build.VERSION_CODES.O)
        get() = AudioFocusRequest.Builder(focusGain)
                .setAudioAttributes(audioAttributes!!)
                .setAcceptsDelayedFocusGain(mAcceptsDelayedFocusGain)
                .setWillPauseWhenDucked(mPauseOnDuck)
                .setOnAudioFocusChangeListener(onAudioFocusChangeListener!!, focusChangeHandler!!)
                .build()

    @Retention(SOURCE)
    @IntDef(AudioManager.AUDIOFOCUS_GAIN, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
    annotation class FocusGain

    fun willPauseWhenDucked(): Boolean {
        return mPauseOnDuck
    }

    fun acceptsDelayedFocusGain(): Boolean {
        return mAcceptsDelayedFocusGain
    }

    /**
     * Builder for an [AudioFocusRequestCompat].
     */
    class Builder {
        private var mFocusGain: Int = 0
        private var mOnAudioFocusChangeListener: OnAudioFocusChangeListener? = null
        private var mFocusChangeHandler: Handler? = null
        private var mAudioAttributesCompat: AudioAttributesCompat? = null

        // Flags
        private var mPauseOnDuck: Boolean = false
        private var mAcceptsDelayedFocusGain: Boolean = false

        constructor(@FocusGain focusGain: Int) {
            mFocusGain = focusGain
        }

        constructor(requestToCopy: AudioFocusRequestCompat) {
            mFocusGain = requestToCopy.focusGain
            mOnAudioFocusChangeListener = requestToCopy.onAudioFocusChangeListener
            mFocusChangeHandler = requestToCopy.focusChangeHandler
            mAudioAttributesCompat = requestToCopy.audioAttributesCompat
            mPauseOnDuck = requestToCopy.mPauseOnDuck
            mAcceptsDelayedFocusGain = requestToCopy.mAcceptsDelayedFocusGain
        }

        fun setFocusGain(@FocusGain focusGain: Int): Builder {
            mFocusGain = focusGain
            return this
        }

        @JvmOverloads
        fun setOnAudioFocusChangeListener(listener: OnAudioFocusChangeListener,
                                          handler: Handler = Handler(Looper.getMainLooper())): Builder {
            mOnAudioFocusChangeListener = listener
            mFocusChangeHandler = handler
            return this
        }

        fun setAudioAttributes(attributes: AudioAttributesCompat): Builder {
            mAudioAttributesCompat = attributes
            return this
        }

        fun setWillPauseWhenDucked(pauseOnDuck: Boolean): Builder {
            mPauseOnDuck = pauseOnDuck
            return this
        }

        fun setAcceptsDelayedFocusGain(acceptsDelayedFocusGain: Boolean): Builder {
            mAcceptsDelayedFocusGain = acceptsDelayedFocusGain
            return this
        }

        fun build(): AudioFocusRequestCompat {
            return AudioFocusRequestCompat(mFocusGain,
                    mOnAudioFocusChangeListener,
                    mFocusChangeHandler,
                    mAudioAttributesCompat,
                    mPauseOnDuck,
                    mAcceptsDelayedFocusGain)
        }
    }
}
