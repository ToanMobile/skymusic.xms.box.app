package com.nct.xmusicstation.data

import com.nct.xmusicstation.BuildConfig
import com.nct.xmusicstation.app.App
import com.nct.xmusicstation.data.local.database.RealmManager
import com.nct.xmusicstation.data.model.auth.GenerateCodeInfo
import com.nct.xmusicstation.data.model.auth.LoginInfo
import com.nct.xmusicstation.data.model.auth.RetrieveModel
import com.nct.xmusicstation.data.model.auth.TokenInfo
import com.nct.xmusicstation.data.remote.ApiService
import com.nct.xmusicstation.utils.toMd5
import com.orhanobut.logger.Logger
import io.reactivex.Flowable
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Created by Toan.IT on 11/3/17.
 * Email:Huynhvantoan.itc@gmail.com
 */
@Singleton
class LoginRepository
@Inject
constructor(val realmManager: RealmManager, private val apiService: ApiService) {
    fun getAccessToken(): Flowable<TokenInfo> {
        //Logger.e("0=" + App.deviceInfoString + "1=" + AppJNI.getString(1) + "hehee=" + AppJNI.getString(2))
        return apiService.getAccessToken(
            App.deviceInfoString,
            BuildConfig.CLIENT_ID,
            BuildConfig.CLIENT_SERCET,
            "access_token"
        )
    }

    fun login(user: String, pass: String): Flowable<LoginInfo> {
        return apiService.login(App.deviceInfoString, user, pass)
    }

    fun getLoginCode(token: String?): Flowable<GenerateCodeInfo> {
        token?.apply {
            val timestamp = System.currentTimeMillis()
            val md5 = (App.deviceInfo.deviceID + timestamp.toString() + BuildConfig.CLIENT_SERCET).toMd5()
            //Logger.d("deviceID=" + App.deviceInfoString + "\n token=" + this + "\n timestamp=" + timestamp + "\n md5=" + md5)
            return apiService.getLoginCode(App.deviceInfoString, this, timestamp, md5)
        }
        return Flowable.empty()
    }

    fun checkLoginCode(token: String?, code: String?): Flowable<RetrieveModel> {
        if (token.isNullOrEmpty() || code.isNullOrEmpty())
            return Flowable.empty()
        //Logger.d("deviceInfoString=" + App.deviceInfoString + "accessToken=" + token + "code=" + code)
        return apiService.checkLogin(App.deviceInfoString, token, code)
    }
}