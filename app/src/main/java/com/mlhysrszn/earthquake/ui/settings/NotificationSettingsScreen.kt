package com.mlhysrszn.earthquake.ui.settings

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mlhysrszn.earthquake.R
import com.mlhysrszn.earthquake.domain.model.NotificationPreferences
import com.mlhysrszn.earthquake.notifications.NotificationPermissionStatus
import com.mlhysrszn.earthquake.notifications.notificationPermissionStatus
import com.mlhysrszn.earthquake.notifications.shouldRequestNotificationPermission
import kotlin.math.roundToInt

@Composable
fun NotificationSettingsRoute(
    onBack: () -> Unit,
    viewModel: NotificationSettingsViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var permissionStatus by remember { mutableStateOf(notificationPermissionStatus(context)) }
    var permissionRequestInFlight by remember { mutableStateOf(false) }
    var returningFromSystemSettings by remember { mutableStateOf(false) }
    var lastRecordedPermissionStatus by remember { mutableStateOf(permissionStatus) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) {
        val result = notificationPermissionStatus(context)
        permissionStatus = result
        lastRecordedPermissionStatus = result
        permissionRequestInFlight = false
        viewModel.recordPermissionOutcome(result.name.lowercase(), "runtime_request")
    }

    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val currentStatus = notificationPermissionStatus(context)
                permissionStatus = currentStatus
                if (
                    returningFromSystemSettings &&
                    !permissionRequestInFlight &&
                    currentStatus != lastRecordedPermissionStatus
                ) {
                    viewModel.recordPermissionOutcome(
                        currentStatus.name.lowercase(),
                        "system_settings",
                    )
                }
                if (!permissionRequestInFlight) lastRecordedPermissionStatus = currentStatus
                returningFromSystemSettings = false
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    NotificationSettingsScreen(
        state = state,
        permissionStatus = permissionStatus,
        onBack = onBack,
        onEnabledChange = { enabled ->
            viewModel.setNotificationsEnabled(enabled)
            if (shouldRequestNotificationPermission(Build.VERSION.SDK_INT, enabled, permissionStatus)) {
                permissionRequestInFlight = true
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        },
        onThresholdChange = viewModel::setMagnitudeThreshold,
        onOpenSystemSettings = {
            returningFromSystemSettings = true
            context.startActivity(
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                    putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                },
            )
        },
        demoContent = { DemoScenarioSection() },
    )
}

@Composable
fun NotificationSettingsScreen(
    state: NotificationSettingsUiState,
    permissionStatus: NotificationPermissionStatus,
    onBack: () -> Unit,
    onEnabledChange: (Boolean) -> Unit,
    onThresholdChange: (Double) -> Unit,
    onOpenSystemSettings: () -> Unit,
    modifier: Modifier = Modifier,
    demoContent: @Composable () -> Unit = {},
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            Surface(tonalElevation = 2.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = onBack) {
                        Text(text = stringResource(R.string.back))
                    }
                    Text(
                        text = stringResource(R.string.notification_settings_title),
                        modifier = Modifier.padding(start = 8.dp),
                        style = MaterialTheme.typography.titleLarge,
                    )
                }
            }
        },
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.notifications_enabled),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                )
                Switch(
                    checked = state.preferences.notificationsEnabled,
                    onCheckedChange = onEnabledChange,
                    modifier = Modifier.testTag(NOTIFICATION_SWITCH_TEST_TAG),
                )
            }

            Column {
                Text(
                    text = stringResource(
                        R.string.magnitude_threshold,
                        formatThreshold(state.preferences.magnitudeThreshold),
                    ),
                    style = MaterialTheme.typography.titleMedium,
                )
                Slider(
                    modifier = Modifier.testTag(MAGNITUDE_SLIDER_TEST_TAG),
                    value = state.preferences.magnitudeThreshold.toFloat(),
                    onValueChange = { value ->
                        val steps = (value * 2).roundToInt()
                        onThresholdChange(steps / 2.0)
                    },
                    valueRange = NotificationPreferences.MIN_MAGNITUDE_THRESHOLD.toFloat()..
                        NotificationPreferences.MAX_MAGNITUDE_THRESHOLD.toFloat(),
                    steps = 18,
                )
                Text(
                    text = stringResource(R.string.magnitude_threshold_range),
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(permissionStatus.messageResource()),
                    style = MaterialTheme.typography.bodyMedium,
                )
                if (
                    permissionStatus == NotificationPermissionStatus.DENIED ||
                    permissionStatus == NotificationPermissionStatus.SYSTEM_DISABLED
                ) {
                    Button(onClick = onOpenSystemSettings) {
                        Text(text = stringResource(R.string.open_notification_settings))
                    }
                }
            }

            if (state.saveFailed) {
                Text(
                    text = stringResource(R.string.preferences_save_failed),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            demoContent()
        }
    }
}

@Composable
private fun NotificationPermissionStatus.messageResource(): Int = when (this) {
    NotificationPermissionStatus.GRANTED -> R.string.notification_permission_granted
    NotificationPermissionStatus.DENIED -> R.string.notification_permission_denied
    NotificationPermissionStatus.SYSTEM_DISABLED -> R.string.notification_permission_system_disabled
    NotificationPermissionStatus.NOT_REQUIRED -> R.string.notification_permission_not_required
}

private fun formatThreshold(value: Double): String =
    String.format(java.util.Locale.forLanguageTag("tr-TR"), "%.1f", value)

internal const val NOTIFICATION_SWITCH_TEST_TAG = "notifications-enabled-switch"
internal const val MAGNITUDE_SLIDER_TEST_TAG = "magnitude-threshold-slider"
