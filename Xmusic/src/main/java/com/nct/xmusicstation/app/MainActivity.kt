package com.nct.xmusicstation.app

import android.Manifest
import android.content.Intent
import com.nct.xmusicstation.R
import com.nct.xmusicstation.data.local.prefs.PreferenceManager
import com.nct.xmusicstation.define.PrefDef
import com.nct.xmusicstation.service.DownloadService
import com.nct.xmusicstation.service.MediaService
import com.nct.xmusicstation.service.ScheduleService
import com.nct.xmusicstation.ui.login.LoginFragment
import com.nct.xmusicstation.ui.player.PlayerFragment
import com.toan_itc.core.base.CoreBaseActivity
import com.toan_itc.core.richutils.addFragment
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasAndroidInjector
import permissions.dispatcher.NeedsPermission
import permissions.dispatcher.RuntimePermissions
import javax.inject.Inject

@RuntimePermissions
class MainActivity : CoreBaseActivity(), HasAndroidInjector {

    @Inject
    lateinit var dispatchingInjector: DispatchingAndroidInjector<Any>

    override fun androidInjector(): AndroidInjector<Any> = dispatchingInjector

    @NeedsPermission(
        Manifest.permission.INTERNET, Manifest.permission.ACCESS_NETWORK_STATE, Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.INSTALL_PACKAGES, Manifest.permission.DELETE_PACKAGES, Manifest.permission.REQUEST_INSTALL_PACKAGES
    )
    fun initPermission() {
    }

    override fun initViews() {
        initPermissionWithPermissionCheck()
        when (PreferenceManager.getBoolean(PrefDef.LOGIN, false)) {
            true -> addFragment(PlayerFragment.newInstance(), R.id.container)
            else -> addFragment(LoginFragment.newInstance(), R.id.container)
        }
    }

    override fun setLayoutResourceID(): Int = R.layout.main_activity

    override fun initData() {

    }

    override fun onDestroy() {
        stopService(Intent(this, ScheduleService::class.java))
        stopService(Intent(this, DownloadService::class.java))
        stopService(Intent(this, MediaService::class.java))
        super.onDestroy()
    }
}