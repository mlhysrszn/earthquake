package com.mlhysrszn.earthquake.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import com.mlhysrszn.earthquake.domain.model.ProductEventName
import com.mlhysrszn.earthquake.MainActivity
import com.mlhysrszn.earthquake.R
import com.mlhysrszn.earthquake.ui.format.formatMagnitude
import com.mlhysrszn.earthquake.ui.format.formatOccurrenceTime
import com.mlhysrszn.earthquake.domain.model.Earthquake
import com.mlhysrszn.earthquake.domain.repository.EarthquakeNotificationRequest
import com.mlhysrszn.earthquake.domain.repository.EarthquakeNotificationSender
import com.mlhysrszn.earthquake.domain.repository.NotificationDeliveryResult
import com.mlhysrszn.earthquake.domain.repository.ProductEventRecorder
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidEarthquakeNotificationSender @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val productEventRecorder: ProductEventRecorder,
) : EarthquakeNotificationSender {
    private val notificationManager = NotificationManagerCompat.from(context)

    override suspend fun send(
        request: EarthquakeNotificationRequest,
    ): NotificationDeliveryResult = when (notificationPermissionStatus(context)) {
        NotificationPermissionStatus.DENIED -> {
            recordSuppressed(request, "permission_denied")
            NotificationDeliveryResult.PermissionDenied
        }

        NotificationPermissionStatus.SYSTEM_DISABLED -> {
            recordSuppressed(request, "system_disabled")
            NotificationDeliveryResult.SystemDisabled
        }

        NotificationPermissionStatus.GRANTED,
        NotificationPermissionStatus.NOT_REQUIRED,
        -> post(request)
    }

    private suspend fun post(request: EarthquakeNotificationRequest): NotificationDeliveryResult {
        val result = try {
            createNotificationChannel()
            val earthquake = request.earthquake
            val title = earthquake.magnitude?.let { magnitude ->
                context.getString(R.string.notification_title, formatMagnitude(magnitude))
            } ?: context.getString(R.string.notification_title_unknown)
            val place = earthquake.place?.takeIf(String::isNotBlank)
                ?: context.getString(R.string.place_unknown)
            val body = context.getString(
                R.string.notification_body,
                place,
                formatOccurrenceTime(earthquake.occurredAt),
            )
            val notification = NotificationCompat.Builder(
                context,
                EarthquakeNotificationConstants.CHANNEL_ID,
            )
                .setSmallIcon(R.drawable.ic_earthquake_notification)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                .setCategory(NotificationCompat.CATEGORY_EVENT)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setOnlyAlertOnce(true)
                .setContentIntent(createContentIntent(earthquake))
                .build()

            // The event ID is the tag, so different IDs cannot collide on the numeric ID.
            notificationManager.notify(
                request.canonicalEventId,
                EarthquakeNotificationConstants.NOTIFICATION_ID,
                notification,
            )
            NotificationDeliveryResult.Posted
        } catch (_: SecurityException) {
            NotificationDeliveryResult.PermissionDenied
        } catch (_: Exception) {
            NotificationDeliveryResult.RetryableFailure
        }

        when (result) {
            NotificationDeliveryResult.Posted -> productEventRecorder.record(
                name = ProductEventName.NOTIFICATION_POSTED,
                properties = mapOf("event_id" to request.earthquake.id),
            )

            NotificationDeliveryResult.PermissionDenied -> recordSuppressed(request, "permission_denied")
            NotificationDeliveryResult.SystemDisabled -> recordSuppressed(request, "system_disabled")
            NotificationDeliveryResult.RetryableFailure -> productEventRecorder.record(
                name = ProductEventName.NOTIFICATION_DELIVERY_FAILED,
                properties = mapOf("event_id" to request.earthquake.id),
            )
        }
        return result
    }

    private suspend fun recordSuppressed(request: EarthquakeNotificationRequest, reason: String) {
        productEventRecorder.record(
            name = ProductEventName.NOTIFICATION_DELIVERY_SUPPRESSED,
            properties = mapOf("event_id" to request.earthquake.id, "reason" to reason),
        )
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = checkNotNull(context.getSystemService(NotificationManager::class.java))
        manager.createNotificationChannel(
            NotificationChannel(
                EarthquakeNotificationConstants.CHANNEL_ID,
                context.getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = context.getString(R.string.notification_channel_description)
            },
        )
    }

    private fun createContentIntent(earthquake: Earthquake): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            action = EarthquakeNotificationConstants.ACTION_OPEN_EARTHQUAKE
            data = Uri.parse("earthquake://event/${Uri.encode(earthquake.id)}")
            putExtra(MainActivity.EXTRA_EARTHQUAKE_ID, earthquake.id)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        return PendingIntent.getActivity(
            context,
            EarthquakeNotificationConstants.NOTIFICATION_ID,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
