package com.nct.xmusicstation.ui.common

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.nct.xmusicstation.app.MainActivity

/**
 * Created by Toan.IT on 4/24/17.
 * Email:Huynhvantoan.itc@gmail.com
 */

class StartAppOnBootCompletedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val i = Intent(context, MainActivity::class.java)
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(i)
        }
    }
}
