/*
package com.nct.xmusicstation.scheduler

import android.content.Context
import android.content.Intent
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.nct.xmusicstation.service.SyncService
import com.orhanobut.logger.Logger

class SchedulerWorker(context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {
    override fun doWork(): Result {
        return try{
            Logger.e("SchedulerWorker:success")
            val syncIntent = Intent(applicationContext, SyncService::class.java)
            syncIntent.action = SyncService.SYNC_DATA
            applicationContext.startService(syncIntent)
            Result.success()
        }catch (e: Exception){
            Logger.e("SchedulerWorker:failure")
            e.printStackTrace()
            Result.failure()
        }
    }
}*/
