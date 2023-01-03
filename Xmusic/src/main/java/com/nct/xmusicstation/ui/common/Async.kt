package com.nct.xmusicstation.ui.common

import com.nct.xmusicstation.callback.OnCompleted
import com.nct.xmusicstation.data.model.song.ListAlbum
import java.util.concurrent.Callable
import kotlin.system.measureNanoTime

open class Async constructor(private var task: Callable<ListAlbum?>, callback: OnCompleted?) : Runnable {
    private var callback: OnCompleted? = null

    init {
        this.callback = callback
    }

    override fun run() {
        measureNanoTime {
            try {
                val output = task.call()
                callback?.onCompleted(output)
            } catch (e: Exception) {
                e.printStackTrace()
                callback?.onError(e)
            }
        }
    }

}