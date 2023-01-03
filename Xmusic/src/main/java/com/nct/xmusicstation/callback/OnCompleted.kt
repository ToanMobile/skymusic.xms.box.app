package com.nct.xmusicstation.callback

import com.nct.xmusicstation.data.model.song.ListAlbum

interface OnCompleted {
    fun onCompleted(listAlbum: ListAlbum?)
    fun onError(t: Throwable?)
}