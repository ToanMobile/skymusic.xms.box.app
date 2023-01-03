package com.nct.xmusicstation.utils

import android.util.Log
import androidx.lifecycle.*
import com.orhanobut.logger.Logger

/**
 * Created by Toan.IT on 12/3/17.
 * Email:Huynhvantoan.itc@gmail.com
 */

class ForegroundBackgroundListener : LifecycleEventObserver {
    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        Log.d("ProcessLog",event.name)
    }
}