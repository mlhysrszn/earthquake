package com.mlhysrszn.earthquake.data.local.room

sealed interface SnapshotWriteResult {
    data class Applied(val storedEventCount: Int) : SnapshotWriteResult

    data class IgnoredStale(
        val latestSourceGeneratedAtEpochMillis: Long,
    ) : SnapshotWriteResult
}
