package com.mlhysrszn.earthquake.domain.usecase

/** Delivers persisted notification decisions and returns the number posted. */
fun interface DispatchPendingNotifications {
    suspend fun dispatch(): Int
}
