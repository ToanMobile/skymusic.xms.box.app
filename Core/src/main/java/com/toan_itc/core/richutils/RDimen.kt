@file:JvmName("RichUtils")
@file:JvmMultifileClass

package com.toan_itc.core.richutils

import android.content.Context
import android.content.res.Resources


fun Int.toPx() = (this * Resources.getSystem().displayMetrics.density).toInt()

fun Int.toDp() = (this / Resources.getSystem().displayMetrics.density).toInt()

fun Float.toPx() = (this * Resources.getSystem().displayMetrics.density).toInt()

fun Float.toDp() = (this / Resources.getSystem().displayMetrics.density).toInt()

fun Int.toDimen() = Resources.getSystem().getDimension(this)

/**
 * get pixel size from DimenRes
 *
 * @param[resource] dimen res to convert
 * @return proper pixel size
 */
fun Context.dimen(resource: Int): Int = resources.getDimensionPixelSize(resource)

fun androidx.fragment.app.Fragment.dimenRes(resource: Int) : Float =resources.getDimension(resource)
