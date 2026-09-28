package com.mlhysrszn.earthquake.notifications

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationPermissionStatusTest {
    @Test
    fun `pre runtime-permission Android reports required or system-disabled`() {
        assertEquals(
            NotificationPermissionStatus.NOT_REQUIRED,
            resolveNotificationPermissionStatus(
                sdkInt = 32,
                runtimePermissionGranted = false,
                appNotificationsEnabled = true,
            ),
        )
        assertEquals(
            NotificationPermissionStatus.SYSTEM_DISABLED,
            resolveNotificationPermissionStatus(
                sdkInt = 32,
                runtimePermissionGranted = false,
                appNotificationsEnabled = false,
            ),
        )
    }

    @Test
    fun `runtime permission and app notification setting determine status`() {
        assertEquals(
            NotificationPermissionStatus.DENIED,
            resolveNotificationPermissionStatus(
                sdkInt = 33,
                runtimePermissionGranted = false,
                appNotificationsEnabled = false,
            ),
        )
        assertEquals(
            NotificationPermissionStatus.SYSTEM_DISABLED,
            resolveNotificationPermissionStatus(
                sdkInt = 33,
                runtimePermissionGranted = true,
                appNotificationsEnabled = false,
            ),
        )
        assertEquals(
            NotificationPermissionStatus.GRANTED,
            resolveNotificationPermissionStatus(
                sdkInt = 33,
                runtimePermissionGranted = true,
                appNotificationsEnabled = true,
            ),
        )
    }

    @Test
    fun `runtime permission is requested only when enabling after denial`() {
        assertTrue(
            shouldRequestNotificationPermission(
                sdkInt = 33,
                notificationsEnabled = true,
                permissionStatus = NotificationPermissionStatus.DENIED,
            ),
        )
        assertFalse(
            shouldRequestNotificationPermission(
                sdkInt = 33,
                notificationsEnabled = false,
                permissionStatus = NotificationPermissionStatus.DENIED,
            ),
        )
        assertFalse(
            shouldRequestNotificationPermission(
                sdkInt = 32,
                notificationsEnabled = true,
                permissionStatus = NotificationPermissionStatus.NOT_REQUIRED,
            ),
        )
    }
}
