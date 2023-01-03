@file:JvmName("RichUtils")
@file:JvmMultifileClass
@file:Suppress("UNCHECKED_CAST")


package com.toan_itc.core.richutils

import android.util.Log
import java.text.DecimalFormat

inline fun <T, R> T.tryCatch(block: (T) -> R): R {
    try {
        return block(this)
    } catch (e: Exception) {
        Log.e("tag", "I/O Exception", e)
        throw e
    }
}

internal inline fun <R> getValue(block: () -> R, def: Any?): R =
        try {
            block()
        } catch (e: Exception) {
            def as R
        }

internal inline fun <T, R> T.convert(block: (T) -> R, def: Any?): R =
        try {
            block(this)
        } catch (e: Exception) {
            def as R
        }

internal inline fun <T, R> T.convertAcceptNull(block: (T) -> R, def: Any?): R? =
        try {
            block(this)
        } catch (e: Exception) {
            def as R
        }


/**
 * formatting number like 1,000,000
 */
fun toNumFormat(num: String): String {
    val df = DecimalFormat("#,###")
    return df.format(Integer.parseInt(num))
}