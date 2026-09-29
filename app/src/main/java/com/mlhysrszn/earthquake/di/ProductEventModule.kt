package com.mlhysrszn.earthquake.di

import com.mlhysrszn.earthquake.data.analytics.LocalProductEventRecorder
import com.mlhysrszn.earthquake.data.repository.RoomProductEventRepository
import com.mlhysrszn.earthquake.domain.repository.ProductEventRecorder
import com.mlhysrszn.earthquake.domain.repository.ProductEventRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ProductEventModule {
    @Binds
    @Singleton
    abstract fun bindProductEventRepository(
        implementation: RoomProductEventRepository,
    ): ProductEventRepository

    @Binds
    @Singleton
    abstract fun bindProductEventRecorder(
        implementation: LocalProductEventRecorder,
    ): ProductEventRecorder
}
