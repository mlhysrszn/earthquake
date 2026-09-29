package com.mlhysrszn.earthquake.di

import com.mlhysrszn.earthquake.domain.model.ProductEventEnvironment

data class AppVariantConfiguration(
    val environment: ProductEventEnvironment,
    val databaseName: String,
    val preferencesFileName: String,
    val periodicWorkName: String,
)
