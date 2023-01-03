package com.nct.xmusicstation.di.component

import com.nct.xmusicstation.app.App
import com.nct.xmusicstation.di.module.*
import dagger.BindsInstance
import dagger.Component
import dagger.android.support.AndroidSupportInjectionModule
import javax.inject.Singleton

/**
 * Created by ToanDev on 28/2/18.
 * Email:Huynhvantoan.itc@gmail.com
 */

@Singleton
@Component(modules = [(AndroidSupportInjectionModule::class),
    (NetworkModule::class),
    (DataModule::class),
    (ServiceModule::class),
    (ActivityBuildersModule::class),
    (ViewModelModule::class)])
interface AppComponent {
    @Component.Builder
    interface Builder {
        @BindsInstance
        fun application(application: App): Builder

        fun build(): AppComponent
    }

    fun inject(app: App)
}
