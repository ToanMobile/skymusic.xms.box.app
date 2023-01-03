package com.nct.xmusicstation.app

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.multidex.MultiDex
import com.auth0.android.jwt.JWT
import com.blankj.utilcode.util.AppUtils
import com.blankj.utilcode.util.CrashUtils
import com.blankj.utilcode.util.Utils
import com.google.gson.Gson
import com.liulishuo.filedownloader.FileDownloader
import com.nct.xmusicstation.BuildConfig
import com.nct.xmusicstation.R
import com.nct.xmusicstation.data.local.database.Migration
import com.nct.xmusicstation.data.local.prefs.PreferenceManager
import com.nct.xmusicstation.data.model.auth.DeviceInfo
import com.nct.xmusicstation.di.AppInjector
import com.nct.xmusicstation.library.OkHttp3Connection
import com.nct.xmusicstation.utils.Constants
import com.nct.xmusicstation.utils.ForegroundBackgroundListener
import com.nct.xmusicstation.utils.isDebug
import com.orhanobut.logger.AndroidLogAdapter
import com.orhanobut.logger.Logger
import com.orhanobut.logger.PrettyFormatStrategy
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasAndroidInjector
import io.reactivex.plugins.RxJavaPlugins
import io.reactivex.rxjava3.exceptions.OnErrorNotImplementedException
import io.realm.Realm
import io.realm.RealmConfiguration
import java.io.IOException
import java.net.SocketException
import javax.inject.Inject

class App : Application(), HasAndroidInjector {
    @Inject
    lateinit var androidInjector: DispatchingAndroidInjector<Any>

    override fun androidInjector(): AndroidInjector<Any> = androidInjector

    private lateinit var appObserver: ForegroundBackgroundListener

    companion object {
        lateinit var instance: App
            private set
        lateinit var deviceInfo: DeviceInfo
            private set
        lateinit var deviceInfoString: String
            private set
    }

    fun getUserIdFromToken(token: String): Int? {
        if (token.isNotEmpty()) {
            val jwt = JWT(token)
            return jwt.getClaim("uid").asInt()
        }
        return 0
    }

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        MultiDex.install(this)
    }

    override fun onCreate() {
        super.onCreate()
        setupLogger()
        setupUtils()
        setupData()
        rxJava()
        initFileDownload()
        AppInjector.init(this)
    }

    private fun setupLogger() {
        val formatStrategy = PrettyFormatStrategy.newBuilder()
            .tag(getString(R.string.app_name))
            .build()
        Logger.addLogAdapter(object : AndroidLogAdapter(formatStrategy) {
            override fun isLoggable(priority: Int, tag: String?): Boolean = isDebug
        })
        ProcessLifecycleOwner.get()
            .lifecycle
            .addObserver(ForegroundBackgroundListener()
                .also { appObserver = it })
    }

    private fun setupData() {
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)
        deviceInfo = DeviceInfo(this)
        deviceInfoString = Gson().toJson(deviceInfo)
        //Logger.e("deviceInfo=" + deviceInfo.toString() + "deviceInfoString=" + deviceInfoString)
        PreferenceManager.initialize(this, BuildConfig.APP_NAME)
        getRealmConfiguration()
        initializeRealmDatabase()
    }

    @SuppressLint("MissingPermission")
    private fun setupUtils() {
        Utils.init(this)
        CrashUtils.init { crashInfo ->
            Logger.d("CrashUtils=$crashInfo")
            if (!isDebug) AppUtils.relaunchApp()
        }
    }

    private fun getRealmConfiguration(): RealmConfiguration {
        Realm.init(this)
        return RealmConfiguration.Builder()
            .name(Constants.DB_Realm)
            .allowQueriesOnUiThread(true)
            .allowWritesOnUiThread(true)
            .deleteRealmIfMigrationNeeded()
            .compactOnLaunch()
            .migration(Migration())
            .schemaVersion(Constants.RealmVersion)
            .build()
    }

    private fun initializeRealmDatabase() {
        val realmConfiguration = getRealmConfiguration()
        Realm.setDefaultConfiguration(realmConfiguration)
        Realm.compactRealm(getRealmConfiguration())
    }

    override fun onLowMemory() {
        super.onLowMemory()
        //Logger.e("onLowMemory \n onLowMemory \n onLowMemory")
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        //Logger.e("onTrimMemory \n onTrimMemory \n onTrimMemory:$level")
    }

    private fun initFileDownload() {
        //.addNetworkInterceptor(Interceptor { chain: Interceptor.Chain ->
        //                        val request = chain.request().newBuilder().addHeader("Connection", "close").build()
        //                        chain.proceed(request)
        //                    })
        FileDownloader.setupOnApplicationOnCreate(this).connectionCreator(OkHttp3Connection.Creator())
        /*val config = FileDownloadUrlConnection.Configuration()
        config.connectTimeout(15_000)
        config.readTimeout(15_000)
        FileDownloader.setupOnApplicationOnCreate(this)
            .connectionCreator(config)
            .commit()*/
        // connection.addHeader(name, value);
    }

    private fun rxJava() {
        RxJavaPlugins.setErrorHandler { e ->
            Logger.d("Undeliverable exception received, not sure what to do" + e.message)
            if (e is OnErrorNotImplementedException || e is IOException || e is SocketException || e is InterruptedException
                || e is NullPointerException || e is IllegalArgumentException || e is IllegalStateException
            ) {
                e.printStackTrace()
                return@setErrorHandler
            }
        }
    }
}
