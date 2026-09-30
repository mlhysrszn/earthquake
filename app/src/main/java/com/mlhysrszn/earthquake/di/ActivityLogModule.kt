package com.mlhysrszn.earthquake.di

import com.mlhysrszn.earthquake.data.repository.RoomNotificationDecisionRepository
import com.mlhysrszn.earthquake.domain.repository.NotificationDecisionRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ActivityLogModule {
    @Binds
    @Singleton
    abstract fun bindNotificationDecisionRepository(
        implementation: RoomNotificationDecisionRepository,
    ): NotificationDecisionRepository
}
