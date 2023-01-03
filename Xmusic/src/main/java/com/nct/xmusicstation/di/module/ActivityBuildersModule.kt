package com.nct.xmusicstation.di.module

import com.nct.xmusicstation.app.MainActivity
import com.nct.xmusicstation.di.ActivityScope
import dagger.Module
import dagger.android.ContributesAndroidInjector

/**
 * Created by ToanDev on 28/2/18.
 * Email:Huynhvantoan.itc@gmail.com
 */
@Suppress("unused")
@Module
abstract class ActivityBuildersModule {

    @ActivityScope
    @ContributesAndroidInjector(modules = [(FragmentBuildersModule::class)])
    internal abstract fun contributeMainActivity(): MainActivity
}
