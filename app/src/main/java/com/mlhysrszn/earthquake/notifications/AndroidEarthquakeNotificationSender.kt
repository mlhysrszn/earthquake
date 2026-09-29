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
import com.mlhysrszn.earthquake.MainActivity
import com.mlhysrszn.earthquake.R
import com.mlhysrszn.earthquake.domain.model.Earthquake
import com.mlhysrszn.earthquake.domain.repository.EarthquakeNotificationRequest
import com.mlhysrszn.earthquake.domain.repository.EarthquakeNotificationSender
import com.mlhysrszn.earthquake.domain.repository.NotificationDeliveryResult
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidEarthquakeNotificationSender @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : EarthquakeNotificationSender {
    private val notificationManager = NotificationManagerCompat.from(context)

    override suspend fun send(
        request: EarthquakeNotificationRequest,
    ): NotificationDeliveryResult = when (notificationPermissionStatus(context)) {
        NotificationPermissionStatus.DENIED -> NotificationDeliveryResult.PermissionDenied
        NotificationPermissionStatus.SYSTEM_DISABLED -> NotificationDeliveryResult.SystemDisabled
        NotificationPermissionStatus.GRANTED,
        NotificationPermissionStatus.NOT_REQUIRED,
        -> post(request)
    }

    private fun post(request: EarthquakeNotificationRequest): NotificationDeliveryResult = try {
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
            formatTime(earthquake.occurredAt),
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

    private fun formatMagnitude(magnitude: Double): String = DecimalFormat(
        "0.0",
        DecimalFormatSymbols.getInstance(Locale.forLanguageTag("tr-TR")),
    ).format(magnitude)

    private fun formatTime(instant: Instant): String = DateTimeFormatter
        .ofPattern("d MMM, HH:mm", Locale.forLanguageTag("tr-TR"))
        .withZone(ZoneId.systemDefault())
        .format(instant)
}
