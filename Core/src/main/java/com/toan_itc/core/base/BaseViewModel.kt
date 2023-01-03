package com.toan_itc.core.base

import androidx.lifecycle.ViewModel
import com.orhanobut.logger.Logger
import com.toan_itc.core.base.event.Event
import io.reactivex.disposables.CompositeDisposable
import io.realm.Realm
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.greenrobot.eventbus.EventBus


/**
 * Created by ToanDev on 11/30/17.
 * Email:Huynhvantoan.itc@gmail.com
 */

abstract class BaseViewModel : ViewModel() {
    private val mCompositeDisposable: CompositeDisposable = CompositeDisposable()
    private val viewModelJob = SupervisorJob()
    protected val mainScope = viewModelJob + Dispatchers.Default
    protected val ioScope = viewModelJob + Dispatchers.IO
    protected val uiScope = viewModelJob + Dispatchers.Main
    override fun onCleared() {
        super.onCleared()
        Logger.d("onCleared")
        Realm.getDefaultInstance()?.let {
            if(!it.isClosed)
                it.close()
        }
        viewModelJob.cancel()
        mCompositeDisposable.dispose()
    }

    fun getCompositeDisposable(): CompositeDisposable {
        return mCompositeDisposable
    }

    fun <T : Event> sendEventBus(event: T) =  EventBus.getDefault().post(event)
}
