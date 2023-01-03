package com.nct.xmusicstation.ui.player

import com.toan_itc.core.base.BaseView

/**
 * Created by Toan.IT on 11/30/17.
 * Email:Huynhvantoan.itc@gmail.com
 */

interface PlayerView : BaseView {

    fun createPlayer()

    fun stopPlayer()

    fun quitApp()

    fun showLoginPage()
}
