package com.mlhysrszn.earthquake.notifications

import com.mlhysrszn.earthquake.domain.repository.EarthquakeNotificationSender
import com.mlhysrszn.earthquake.domain.usecase.DispatchPendingNotifications
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class NotificationDeliveryModule {
    @Binds
    @Singleton
    abstract fun bindEarthquakeNotificationSender(
        implementation: AndroidEarthquakeNotificationSender,
    ): EarthquakeNotificationSender

    @Binds
    @Singleton
    abstract fun bindDispatchPendingNotifications(
        implementation: PendingNotificationDispatcher,
    ): DispatchPendingNotifications
}
