package com.nct.xmusicstation.ui.login

import com.nct.xmusicstation.app.App
import com.nct.xmusicstation.data.LoginRepository
import com.nct.xmusicstation.data.local.prefs.PreferenceManager
import com.nct.xmusicstation.data.local.prefs.pref
import com.nct.xmusicstation.data.model.auth.GenerateCodeInfo
import com.nct.xmusicstation.define.CallApiDef
import com.nct.xmusicstation.define.PrefDef
import com.nct.xmusicstation.event.CodeEvent
import com.nct.xmusicstation.event.ErrorEvent
import com.nct.xmusicstation.event.FragmentEvent
import com.nct.xmusicstation.event.ProgressTimerEvent
import com.nct.xmusicstation.utils.Constants
import com.orhanobut.logger.Logger
import com.toan_itc.core.base.BaseViewModel
import io.reactivex.Flowable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.ReplaySubject
import java.util.*
import javax.inject.Inject

/**
 * Created by Toan.IT on 12/10/18.
 * Email:Huynhvantoan.itc@gmail.com
 */


class LoginViewModel
@Inject
internal constructor(loginRepository: LoginRepository) : BaseViewModel(){
    private val TAG = "LoginViewModel:::"
    private var loginCode: GenerateCodeInfo? = null
    private var token: String = ""
    private val repository = loginRepository
    private var checkingLoginCode: Boolean = false
    private val CHECK_LOGIN_CODE_INTERVAL = 10000L
    private var checkLoginCodeTimer: Timer? = null
    private var checkLoginCodeExpireTimer: Timer? = null
    private val countDownSubject = ReplaySubject.create<Int>()
    private val isLog = false

    init {
        pref { put(PrefDef.LOGIN, false) }
        runTimer()
        getLoginCode()
    }

    override fun onCleared() {
        super.onCleared()
        stopCheckLoginCodeTimer()
        stopCheckLoginCodeExpireTimer()
    }

    fun login(user: String, pass: String) {
        if(isLog) Logger.d("Login")
        getCompositeDisposable().add(repository.login(user, pass)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    when (it.token != null && it.user != null) {
                        true -> {
                            it?.token.let {
                                pref {
                                    put(PrefDef.PRE_TOKEN, it)
                                    put(PrefDef.LOGIN, true)
                                }
                                App().getUserIdFromToken(it)
                                if(isLog) Logger.d("TOKEN=:$it" + "deviceID=" + App.deviceInfo.deviceID)
                                checkingLoginCode = false
                                stopCheckLoginCodeTimer()
                                sendEventBus(FragmentEvent())
                            }
                            it?.user.let {
                                PreferenceManager.put(PrefDef.PRE_USER, it)
                            }
                        }
                        else -> sendEventBus(ErrorEvent(it.error?.msg))
                    }
                }, {
                    repository.realmManager.addLogSongError(Constants.SONGKEY, Constants.ALBUMID, it.message)
                    it.printStackTrace()
                }))
    }

    fun renewCodeClick() = getLoginCode()

    private fun runTimer() {
        getCompositeDisposable().add(countDownSubject
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ timeCount ->
                    loginCode?.expiresIn?.let {
                        if (it > 0) {
                            var progressValue = timeCount * 100 / it
                            if (progressValue < 1) {
                                progressValue = 1
                            } else if (progressValue == 100) {
                                progressValue = 0
                            }
                            if(isLog) Logger.d("progressValue=$progressValue")
                            sendEventBus(ProgressTimerEvent((it - timeCount).toString(), progressValue))
                        }
                    }
                }, {
                    it.printStackTrace()
                    repository.realmManager.addLogSongError(Constants.SONGKEY, Constants.ALBUMID, it.message)
                }))
    }

    private fun getLoginCode() {
        getCompositeDisposable().add(repository.getAccessToken()
                .flatMap {
                    if(isLog) Logger.d(TAG + it.toString())
                    when (it.status) {
                        CallApiDef.OK -> {
                            token = it.token
                            repository.getLoginCode(token)
                        }
                        else -> {
                            sendEventBus(ErrorEvent(it.toString()))
                            Flowable.empty()
                        }
                    }
                }
                .map {
                    loginCode = it
                }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    loginCode?.apply {
                        sendEventBus(CodeEvent(code))
                        startCheckLoginCodeExpireTimer()
                        startCheckLoginCodeTimer()
                    }
                }, {
                    it.printStackTrace()
                    repository.realmManager.addLogSongError(Constants.SONGKEY, Constants.ALBUMID, it.message)
                }))
    }

    private fun checkLoginCode(code: String) {
        if (checkingLoginCode || token.isEmpty()) return
        checkingLoginCode = true
        if(isLog) Logger.d("token=" + token + "code=" + code)
        getCompositeDisposable().add(repository.checkLoginCode(token, code)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    if(isLog) Logger.d(TAG + it.toString())
                    when (it.status) {
                        CallApiDef.OK -> {
                            it?.token?.let {
                                if(it.isNotEmpty()) {
                                    pref {
                                        put(PrefDef.PRE_TOKEN, it)
                                        put(PrefDef.LOGIN, true)
                                    }
                                    App().getUserIdFromToken(it)
                                    checkingLoginCode = false
                                    stopCheckLoginCodeTimer()
                                    if (isLog) Logger.d("TOKEN=:$it" + "deviceID=" + App.deviceInfo.deviceID)
                                    sendEventBus(FragmentEvent())
                                }
                            }
                        }
                        else -> {
                            if(isLog) Logger.d("checkLoginCode:Error")
                            checkingLoginCode = false
                            if(isLog) Logger.d(it.toString())
                        }
                    }
                }, {
                    repository.realmManager.addLogSongError(Constants.SONGKEY, Constants.ALBUMID, it.message)
                    it.printStackTrace()
                    if(isLog) Logger.d("Error")
                }))
    }

    private fun startCheckLoginCodeTimer() {
        stopCheckLoginCodeTimer()
        checkLoginCodeTimer = Timer()
        checkLoginCodeTimer?.schedule(object : TimerTask() {
            override fun run() {
                checkLoginCode(loginCode?.code!!)
            }
        }, CHECK_LOGIN_CODE_INTERVAL, CHECK_LOGIN_CODE_INTERVAL)
    }

    private fun startCheckLoginCodeExpireTimer() {
        stopCheckLoginCodeExpireTimer()
        checkLoginCodeExpireTimer = Timer()
        checkLoginCodeExpireTimer?.schedule(object : TimerTask() {
            var timeCount = 0
            override fun run() {
                timeCount++
                countDownSubject.onNext(timeCount)
                loginCode?.expiresIn?.let {
                    if (timeCount >= it) {
                        stopCheckLoginCodeExpireTimer()
                        getLoginCode()
                    }
                }
            }
        }, 0, Constants.TIMER_DELAY)
    }

    private fun stopCheckLoginCodeExpireTimer() {
        checkLoginCodeExpireTimer?.let {
            it.cancel()
            it.purge()
            checkLoginCodeExpireTimer = null
        }
    }

    private fun stopCheckLoginCodeTimer() {
        checkLoginCodeTimer?.let {
            it.cancel()
            it.purge()
            checkLoginCodeTimer = null
        }
    }

}
