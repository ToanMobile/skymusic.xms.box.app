package com.nct.xmusicstation.utils

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.text.TextUtils
import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.blankj.utilcode.constant.MemoryConstants
import com.blankj.utilcode.util.FileUtils
import com.blankj.utilcode.util.PathUtils
import com.blankj.utilcode.util.SDCardUtils
import com.nct.xmusicstation.BuildConfig
import com.nct.xmusicstation.ui.player.PlayerViewModel
import com.orhanobut.logger.Logger
import com.toan_itc.core.richutils.getExtension
import io.reactivex.disposables.Disposable
import io.realm.RealmList
import org.joda.time.DateTime
import org.joda.time.Period
import org.joda.time.Seconds
import org.joda.time.format.PeriodFormatterBuilder
import java.io.File
import java.math.BigInteger
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

/**
 * Created by Toan.IT on 07/3/19.
 * Email:Huynhvantoan.itc@gmail.com
 */

const val isDebug = BuildConfig.BUILD_TYPE != Constants.RELEASE

fun is1970() = DateTime.now().year < 2010

fun artists(listArtist: RealmList<String>?): String = listArtist?.let { TextUtils.join(", ", it) }
    ?: ""

fun removeDisposable(vararg disposable: Disposable?) = disposable.map { it?.dispose() }

fun getDeviceID(context: Context): String {
    var deviceID = ""
    deviceID = generateDeviceIDWithoutSIM(context)
    return deviceID
}

@SuppressLint("HardwareIds", "PrivateApi")
private fun generateDeviceIDWithoutSIM(context: Context): String {
    val deviceID: String
    val pseudoIMEI = ("35"
            + // we make this look like a valid IMEI
            Build.BOARD.length % 10 + Build.BRAND.length % 10
            + Build.CPU_ABI.length % 10 + Build.DEVICE.length % 10
            + Build.DISPLAY.length % 10 + Build.HOST.length % 10
            + Build.ID.length % 10 + Build.MANUFACTURER.length % 10
            + Build.MODEL.length % 10 + Build.PRODUCT.length % 10
            + Build.TAGS.length % 10 + Build.TYPE.length % 10
            + Build.USER.length % 10) // 13 digits

    /**
     * Serial Number Since Android 2.3 (“Gingerbread”) this is available via
     * android.os.Build.SERIAL. Devices without telephony are required to report a unique device
     * ID here; some phones may do so also. Serial number can be identified for the devices such
     * as MIDs (Mobile Internet Devices) or PMPs (Portable Media Players) which are not having
     * telephony services. Device-Id as serial number is available by reading the System
     * Property Value “ro.serialno” To retrieve the serial number for using Device ID, please
     * refer to example code below.
     */
    var serialnum: String? = null
    try {
        val c = Class.forName("android.os.SystemProperties")
        val get = c.getMethod("get", String::class.java, String::class.java)
        serialnum = get.invoke(c, "ro.serialno", "unknown") as String
    } catch (ignored: Exception) {
    }

    /**
     * More specifically, Settings.Secure.ANDROID_ID. A 64-bit number (as a hex string) that is
     * randomly generated on the device's first boot and should remain constant for the lifetime
     * of the device (The value may change if a factory reset is performed on the device.)
     * ANDROID_ID seems a good choice for a unique device identifier. To retrieve the ANDROID_ID
     * for using Device ID, please refer to example code below Disadvantages: Not 100% reliable
     * of Android prior to 2.2 (“Froyo”) devices Also, there has been at least one
     * widely-observed bug in a popular handset from a major manufacturer, where every instance
     * has the same ANDROID_ID.
     */
    val ANDROID_ID = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ANDROID_ID
    )

    deviceID = md5(pseudoIMEI + serialnum + ANDROID_ID).uppercase()

    return deviceID
}

private fun md5(input: String): String {
    try {
        val md = MessageDigest.getInstance("MD5")
        val messageDigest = md.digest(input.toByteArray())
        val number = BigInteger(1, messageDigest)
        var hashtext = number.toString(16)
        // Now we need to zero pad it if you actually want the full 32
        // chars.
        while (hashtext.length < 32) {
            hashtext = "0$hashtext"
        }
        return hashtext
    } catch (e: NoSuchAlgorithmException) {
        throw RuntimeException(e)
    }

}

fun isNotEnoughSpace(sizeStorage: Long): Boolean = sizeStorage <= Constants.MIN_MEMORY_SPACE
/*
@SuppressLint("ObsoleteSdkInt")
fun getFreeSpace(path: String = SDCardUtils.getSDCardPathByEnvironment()): Long {
    try {
        if (path.isEmpty())
            return 0L
        val statFs = StatFs(path)
        return statFs.blockSizeLong * statFs.availableBlocksLong
    } catch (e: Exception) {
        e.printStackTrace()
        return -1L
    }
}*/

fun setColor(context: Context, color: Int, vararg view: TextView) =
    view.forEach { it.setTextColor(ContextCompat.getColor(context, color)) }

fun setSelecter(isSelected: Boolean, vararg view: View) = view.forEach { it.isSelected = isSelected }

fun formatHourMinutes(time: String?): DateTime {
    val times = time?.split(":".toRegex())?.dropLastWhile { it.isEmpty() }?.toTypedArray()
    var dateTime = DateTime.now().withTimeAtStartOfDay()
    times?.let {
        dateTime = dateTime.withHourOfDay(Integer.parseInt(times[0]))
        dateTime = dateTime.withMinuteOfHour(Integer.parseInt(times[1]))
    }
    return dateTime
}

fun formatStorageToDisplaySize(byteNum: Double): String {
    //Logger.d("formatStorageToDisplaySize="+byteNum)
    return when {
        byteNum < 0 -> "shouldn't be less than zero!"
        byteNum < MemoryConstants.KB -> String.format("%.3f B", byteNum)
        byteNum < MemoryConstants.MB -> String.format("%.3f KB", byteNum / MemoryConstants.KB)
        byteNum < MemoryConstants.GB -> String.format("%.3f MB", byteNum / MemoryConstants.MB)
        else -> String.format("%.3f GB", byteNum / MemoryConstants.GB)
    }
}

fun getDurationToSecond(duration: Long): String {
    try {
        val format = PeriodFormatterBuilder()
            .minimumPrintedDigits(2)
            .printZeroRarelyFirst()
            .appendHours()
            .appendSeparatorIfFieldsBefore(":")
            .minimumPrintedDigits(2)
            .printZeroIfSupported()
            .appendMinutes()
            .appendSuffix(":")
            .printZeroAlways()
            .minimumPrintedDigits(2)
            .appendSeconds()
            .toFormatter()
        return format.print(Period(Seconds.seconds(duration.toInt())).normalizedStandard())
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return ""
}

fun formatSecondDuration(durationVideo: Long): String {
    return PeriodFormatterBuilder()
        .appendHours()
        .printZeroRarelyFirst()
        .printZeroAlways()
        .appendSeparatorIfFieldsBefore(":")
        .minimumPrintedDigits(2)
        .printZeroAlways()
        .appendMinutes()
        .appendSuffix(":")
        .printZeroAlways()
        .minimumPrintedDigits(2)
        .appendSeconds()
        .toFormatter()
        .print(Period(Seconds.seconds(TimeUnit.MILLISECONDS.toSeconds(durationVideo).toInt())).normalizedStandard())
}

fun getFolderDownloadedSongFile(): File = getFileDeviceStorage()

fun getFolderDownloadedSong(): String = getDeviceStorage()

fun getFolderDownloadedSong(songKey: String?, path: String): String = getDeviceStorage() + songKey + getExtension(path)

fun getFolderDownloadedSongLoudNorm(songKey: String?, path: String): String = getDeviceStorage() + songKey + "_loudnorm" + getExtension(path)

private fun getFileDeviceStorage(): File {
    try {
        if (FileUtils.createOrExistsDir(getDeviceStorage())) {
            return FileUtils.getFileByPath(getDeviceStorage())
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return FileUtils.getFileByPath(getDeviceStorage())
}

fun getDeviceStorage(): String {
    try {
        //storage/emulated/0/Android/data/com.nct.xmsbox/files/Music
        return if (PlayerViewModel.isStorage) {
            PathUtils.getExternalAppMusicPath() + File.separator + "Songs" + File.separator
        } else {
            return SDCardUtils.getMountedSDCardPath().let {
                if (it.size > 1) {
                    SDCardUtils.getMountedSDCardPath()[1] + File.separator +"Android/data/com.nct.xmsbox/files/Music/Songs" + File.separator
                } else {
                    PathUtils.getExternalAppMusicPath() + File.separator + "Songs" + File.separator
                }
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return PathUtils.getExternalMusicPath() + File.separator + "Songs" + File.separator
}

fun getPathDownloadStorage(): String = PathUtils.getExternalAppMusicPath() + File.separator + Constants.INSTALL_APP

fun showDownloadAppByStoreUrl(context: Context, storeUrl: String) {
    val appPackageName = getAppPackageNameFromStoreUrl(storeUrl)
    showDownloadApp(context, appPackageName)
}

private fun showDownloadApp(context: Context, appPackageName: String) {
    try {
        context.startActivity(
            Intent(
                Intent.ACTION_VIEW, Uri.parse("market://details?id=$appPackageName")
            )
        )
    } catch (ex: ActivityNotFoundException) {
        context.startActivity(
            Intent(
                Intent.ACTION_VIEW, Uri.parse(getLinkAppStoreByPackageName(appPackageName))
            )
        )
    }

}

private fun getAppPackageNameFromStoreUrl(storeUrl: String): String {
    val pattern = Pattern.compile("id=([^&]+)")
    val matcher = pattern.matcher(storeUrl)
    return if (matcher.find()) matcher.group(1) else ""
}

private fun getLinkAppStoreByPackageName(packageName: String): String =
    "https://play.google.com/store/apps/details?id=$packageName"

fun String.toMd5(): String {
    val bytes = MessageDigest.getInstance("MD5").digest(this.toByteArray())
    return bytes.joinToString("") { "%02x".format(it) }
}