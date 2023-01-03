package com.nct.xmusicstation.data.model.auth

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import com.google.gson.annotations.SerializedName
import com.nct.xmusicstation.utils.getDeviceID
import com.orhanobut.logger.Logger

/**
* Created by Toan.IT on 4/1/15.
* Email:Huynhvantoan.itc@gmail.com
*/

class DeviceInfo(context: Context) {

    @SerializedName("device_id")
    var deviceID: String = getDeviceID(context)
    @SerializedName("device_name")
    var deviceName = Build.MODEL
    @SerializedName("app_name")
    var appName = "xms"
    @SerializedName("app_version")
    lateinit var appVersion: String
    @SerializedName("os_name")
    var osName = "android"
    @SerializedName("os_version")
    var osVersion = "" + Build.VERSION.SDK_INT

    init {
        val pInfo: PackageInfo
        try {
            pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            appVersion = pInfo.versionName
        } catch (e: PackageManager.NameNotFoundException) {
            Logger.d(e.message)
        }
    }

    override fun toString(): String {
        return "DeviceInfo(deviceID='$deviceID', deviceName='$deviceName', appName='$appName', appVersion='$appVersion', osName='$osName', osVersion='$osVersion')"
    }
}
