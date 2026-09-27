package com.mlhysrszn.earthquake.data.local.room

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sync_metadata")
data class SyncMetadataEntity(
    @PrimaryKey val id: Int = SINGLETON_ID,
    val sourceGeneratedAtEpochMillis: Long?,
    val lastSuccessfulFetchAtEpochMillis: Long,
) {
    companion object {
        const val SINGLETON_ID = 0
    }
}
