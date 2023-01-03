package com.nct.xmusicstation.utils

import android.content.Context
import android.graphics.Bitmap
import android.widget.ImageView
import com.nct.xmusicstation.R

fun glideHelp(context: Context, url: String, imageView: ImageView) {
    GlideApp.with(context)
            .load(url)
            .placeholder(R.drawable.cover)
            .error(R.drawable.cover)
            .into(imageView)
}

fun glideHelpBitmap(context: Context, bitmap: Bitmap, imageView: ImageView) {
    GlideApp.with(context)
            .load(bitmap)
            .dontAnimate()
            .into(imageView)
}

fun glideHelpDrawable(context: Context, drawable: Int, imageView: ImageView) {
    GlideApp.with(context)
            .load(drawable)
            .dontAnimate()
            .into(imageView)
}

