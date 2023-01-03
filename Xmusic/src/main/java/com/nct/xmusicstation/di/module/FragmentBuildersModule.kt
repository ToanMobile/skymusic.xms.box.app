package com.nct.xmusicstation.di.module

import com.nct.xmusicstation.di.FragmentScope
import com.nct.xmusicstation.ui.login.LoginFragment
import com.nct.xmusicstation.ui.player.PlayerFragment
import dagger.Module
import dagger.android.ContributesAndroidInjector

/**
 * Created by ToanDev on 28/2/18.
 * Email:Huynhvantoan.itc@gmail.com
 */

@Suppress("unused")
@Module
abstract class FragmentBuildersModule {

    @FragmentScope
    @ContributesAndroidInjector
    internal abstract fun contributeLoginFragment(): LoginFragment

    @FragmentScope
    @ContributesAndroidInjector
    internal abstract fun contributePlayerFragment(): PlayerFragment

}
