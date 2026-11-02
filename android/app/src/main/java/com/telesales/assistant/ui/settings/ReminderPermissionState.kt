package com.telesales.assistant.ui.settings

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

data class ReminderPermissionState(val notificationsGranted: Boolean, val exactAlarmsGranted: Boolean)

fun interface ReminderPermissionStateProvider {
    fun current(): ReminderPermissionState
}

class AndroidReminderPermissionStateProvider(private val context: Context) : ReminderPermissionStateProvider {
    override fun current(): ReminderPermissionState {
        val notifications = Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        val exact = Build.VERSION.SDK_INT < 31 || context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()
        return ReminderPermissionState(notifications, exact)
    }
}
