package com.nct.xmusicstation.di.module

import com.nct.xmusicstation.di.ServiceScope
import com.nct.xmusicstation.service.*
import dagger.Module
import dagger.android.ContributesAndroidInjector


/**
 * Created by ToanDev on 28/2/18.
 * Email:Huynhvantoan.itc@gmail.com
 */

@Suppress("unused")
@Module
abstract class ServiceModule {

    @ServiceScope
    @ContributesAndroidInjector
    internal abstract fun  bindSyncService(): SyncService

    @ServiceScope
    @ContributesAndroidInjector
    internal abstract fun  bindDownloadService(): DownloadService

    @ServiceScope
    @ContributesAndroidInjector
    internal abstract fun  bindMediaService(): MediaService

    @ServiceScope
    @ContributesAndroidInjector
    internal abstract fun  bindScheduleService(): ScheduleService

    @ServiceScope
    @ContributesAndroidInjector
    internal abstract fun  bindDeleteFileService(): DeleteFileService

    @ServiceScope
    @ContributesAndroidInjector
    internal abstract fun  bindConvertMusicService(): LoudNormMusicService
}