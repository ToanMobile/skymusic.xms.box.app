package com.toan_itc.core.richutils

import android.content.ContentValues.TAG
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import android.webkit.MimeTypeMap
import java.io.File

/**
 * check uri is media uri
 * @return True if Uri is a MediaStore Uri.
 */
fun isMediaUri(uri: Uri?): Boolean = if (uri != null) {
    "media".equals(uri.authority, ignoreCase = true)
} else false


/**
 * Convert File into Uri.
 */
fun getUri(file: File?): Uri? {
    return if (file != null) {
        Uri.fromFile(file)
    } else null
}

/**
 * @return The MIME type for the given file.
 */
fun Context.getMimeType(file: File): String? {
    val extension = getExtension(file.name)
    return if (extension.isNotEmpty()) MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.substring(1)) else "application/octet-stream"
}

/**
 * Gets the extension of a file name, like ".png" or ".jpg".
 */
fun getExtension(uri: String?): String {
    if (uri == null) {
        return ""
    }

    val dot = uri.lastIndexOf(".")
    return if (dot >= 0) {
        uri.substring(dot, dot + 4)
    } else {
        ".mp3"
    }
}

/**
 * Attempt to retrieve the thumbnail of given File from the MediaStore. This
 * should not be called on the UI thread.
 */
fun Context.getThumbnail(file: File): Bitmap? {
    return getThumbnail(getUri(file), getMimeType(file))
}

/**
 * Attempt to retrieve the thumbnail of given Uri from the MediaStore. This
 * should not be called on the UI thread.
 */
fun Context.getThumbnail(uri: Uri): Bitmap? {
    return getThumbnail(uri, getMimeType(File(uri.path)))
}

/**
 * Attempt to retrieve the thumbnail of given Uri from the MediaStore. This
 * should not be called on the UI thread.
 */
fun Context.getThumbnail(uri: Uri?, mimeType: String? = ""): Bitmap? {
    if (!isMediaUri(uri)) {
        Log.e(TAG, "You can only retrieve thumbnails for images and videos.")
        return null
    }

    val cursor = this.contentResolver.query(uri?: Uri.EMPTY, null, null, null, null)
    if(cursor == null) return null
    cursor.use {
        val results = generateSequence { if (cursor.moveToNext()) cursor else null }.map {
            val id = it.getInt(0)
            when {
                mimeType!!.contains("video") -> MediaStore.Video.Thumbnails.getThumbnail(contentResolver, id.toLong(), MediaStore.Video.Thumbnails.MINI_KIND, null)
                mimeType!!.contains("image/*") -> MediaStore.Images.Thumbnails.getThumbnail(contentResolver, id.toLong(), MediaStore.Images.Thumbnails.MINI_KIND, null)
                else -> null
            }
        }.filter { it != null }.toList()

        return if (results.isNotEmpty()) {
            results[0]
        } else {
            null
        }
    }
}