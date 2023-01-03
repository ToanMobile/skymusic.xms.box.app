package com.nct.xmusicstation.ui.base

import android.os.Build
import android.view.Gravity
import android.view.View
import android.widget.TextView
import com.google.android.material.snackbar.Snackbar
import com.nct.xmusicstation.R
import com.nct.xmusicstation.app.App
import com.orhanobut.logger.Logger
import com.toan_itc.core.base.BaseViewModel
import com.toan_itc.core.base.CoreBaseDataFragment

/**
 * Created by ToanDev on 28/2/18.
 * Email:Huynhvantoan.itc@gmail.com
 */

abstract class BaseDataEventFragment<VM : BaseViewModel> : CoreBaseDataFragment<VM>() {

    override fun onDestroy() {
        Logger.v("onDestroy:" + this.javaClass.simpleName)
        super.onDestroy()
    }

    fun showSnackBar(message: String) {
        view?.let {
            val snackBar = Snackbar.make(it, message, Snackbar.LENGTH_LONG)
            val view = snackBar.view
            val tv = view.findViewById(R.id.snackbar_text) as TextView
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                tv.textAlignment = View.TEXT_ALIGNMENT_CENTER
            } else {
                tv.gravity = Gravity.CENTER_HORIZONTAL
            }
            snackBar.show()
        }
    }
}
