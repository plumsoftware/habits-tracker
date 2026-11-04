package ru.plumsoftware.habitstracker.data.service

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

object StepCounterManager {
    fun hasPermission(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED

    fun start(context: Context) {
        ContextCompat.startForegroundService(
            context, Intent(context, StepCounterService::class.java).setAction(StepCounterService.ACTION_START)
        )
    }

    fun stop(context: Context) { context.stopService(Intent(context, StepCounterService::class.java)) }
}
