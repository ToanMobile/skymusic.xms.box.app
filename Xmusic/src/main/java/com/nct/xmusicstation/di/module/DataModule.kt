package com.nct.xmusicstation.di.module

import com.nct.xmusicstation.data.local.database.RealmManager
import com.toan_itc.core.architecture.AppExecutors
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

/**
 * Created by ToanDev on 28/2/18.
 * Email:Huynhvantoan.itc@gmail.com
 */

@Module
class DataModule {

    @Singleton
    @Provides
    internal fun realmManager(): RealmManager = RealmManager()

    @Singleton
    @Provides
    internal fun appExecutors(): AppExecutors = AppExecutors()
}
