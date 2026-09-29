package com.mlhysrszn.earthquake.data.local.room

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object EarthquakeDatabaseMigrations {
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "ALTER TABLE earthquakes ADD COLUMN aliasIdsJson TEXT NOT NULL DEFAULT '[]'",
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS notification_processing (
                    canonicalEventId TEXT NOT NULL,
                    latestEventId TEXT NOT NULL,
                    eventOccurredAtEpochMillis INTEGER NOT NULL,
                    outcome TEXT NOT NULL,
                    reason TEXT,
                    lastSeenAtEpochMillis INTEGER NOT NULL,
                    lastDecisionAtEpochMillis INTEGER NOT NULL,
                    PRIMARY KEY(canonicalEventId)
                )
                """.trimIndent(),
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS index_notification_processing_outcome " +
                    "ON notification_processing (outcome)",
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS index_notification_processing_lastSeenAtEpochMillis " +
                    "ON notification_processing (lastSeenAtEpochMillis)",
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS notification_event_aliases (
                    aliasId TEXT NOT NULL,
                    canonicalEventId TEXT NOT NULL,
                    lastSeenAtEpochMillis INTEGER NOT NULL,
                    PRIMARY KEY(aliasId)
                )
                """.trimIndent(),
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS index_notification_event_aliases_canonicalEventId " +
                    "ON notification_event_aliases (canonicalEventId)",
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS notification_processing_metadata (
                    id INTEGER NOT NULL,
                    baselineEstablished INTEGER NOT NULL,
                    lastProcessedFeedCursorEpochMillis INTEGER,
                    lastEvaluatedAtEpochMillis INTEGER NOT NULL,
                    PRIMARY KEY(id)
                )
                """.trimIndent(),
            )
        }
    }
}
