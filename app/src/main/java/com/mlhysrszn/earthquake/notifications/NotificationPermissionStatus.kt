package com.mlhysrszn.earthquake.notifications

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

enum class NotificationPermissionStatus {
    GRANTED,
    DENIED,
    SYSTEM_DISABLED,
    NOT_REQUIRED,
}

fun notificationPermissionStatus(context: Context): NotificationPermissionStatus {
    val runtimePermissionGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
    val appNotificationsEnabled = NotificationManagerCompat
        .from(context)
        .areNotificationsEnabled()

    return resolveNotificationPermissionStatus(
        sdkInt = Build.VERSION.SDK_INT,
        runtimePermissionGranted = runtimePermissionGranted,
        appNotificationsEnabled = appNotificationsEnabled,
    )
}

internal fun resolveNotificationPermissionStatus(
    sdkInt: Int,
    runtimePermissionGranted: Boolean,
    appNotificationsEnabled: Boolean,
): NotificationPermissionStatus = when {
    sdkInt >= Build.VERSION_CODES.TIRAMISU && !runtimePermissionGranted ->
        NotificationPermissionStatus.DENIED

    !appNotificationsEnabled -> NotificationPermissionStatus.SYSTEM_DISABLED

    sdkInt < Build.VERSION_CODES.TIRAMISU -> NotificationPermissionStatus.NOT_REQUIRED

    else -> NotificationPermissionStatus.GRANTED
}

internal fun shouldRequestNotificationPermission(
    sdkInt: Int,
    notificationsEnabled: Boolean,
    permissionStatus: NotificationPermissionStatus,
): Boolean =
    notificationsEnabled &&
        sdkInt >= Build.VERSION_CODES.TIRAMISU &&
        permissionStatus == NotificationPermissionStatus.DENIED
