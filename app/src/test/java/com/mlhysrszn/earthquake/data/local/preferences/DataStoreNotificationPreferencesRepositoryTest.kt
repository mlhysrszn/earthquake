package com.mlhysrszn.earthquake.data.local.preferences

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import com.mlhysrszn.earthquake.domain.model.NotificationPreferences
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class DataStoreNotificationPreferencesRepositoryTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun `defaults are used and saved preferences survive reopening the store`() = runBlocking {
        val file = File(temporaryFolder.root, "notification_preferences.preferences_pb")
        val firstScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val firstRepository = repository(file, firstScope)

        assertEquals(NotificationPreferences(), firstRepository.observePreferences().first())
        firstRepository.setNotificationsEnabled(true)
        firstRepository.setMagnitudeThreshold(6.5)
        val firstJob = firstScope.coroutineContext[Job]
        firstJob?.cancelAndJoin()

        val secondScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        try {
            val reopenedRepository = repository(file, secondScope)
            assertEquals(
                NotificationPreferences(notificationsEnabled = true, magnitudeThreshold = 6.5),
                reopenedRepository.observePreferences().first(),
            )
        } finally {
            secondScope.coroutineContext[Job]?.cancelAndJoin()
        }
    }

    @Test
    fun `invalid threshold writes are rejected without changing saved settings`() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        try {
            val repository = repository(
                File(temporaryFolder.root, "invalid_write.preferences_pb"),
                scope,
            )
            repository.setNotificationsEnabled(true)
            val before = repository.observePreferences().first()

            var rejected = false
            try {
                repository.setMagnitudeThreshold(4.25)
            } catch (_: IllegalArgumentException) {
                rejected = true
            }

            assertTrue(rejected)
            assertEquals(before, repository.observePreferences().first())
        } finally {
            scope.coroutineContext[Job]?.cancelAndJoin()
        }
    }

    @Test
    fun `invalid stored threshold falls back to default without losing enabled preference`() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        try {
            val file = File(temporaryFolder.root, "corrupt_value.preferences_pb")
            val dataStore = PreferenceDataStoreFactory.create(scope = scope) { file }
            dataStore.edit { values ->
                values[booleanPreferencesKey("notifications_enabled")] = true
                values[intPreferencesKey("magnitude_threshold_tenths")] = 43
            }

            val preferences = DataStoreNotificationPreferencesRepository(dataStore)
                .observePreferences()
                .first()

            assertTrue(preferences.notificationsEnabled)
            assertEquals(4.0, preferences.magnitudeThreshold, 0.0)
        } finally {
            scope.coroutineContext[Job]?.cancelAndJoin()
        }
    }

    private fun repository(file: File, scope: CoroutineScope) =
        DataStoreNotificationPreferencesRepository(
            PreferenceDataStoreFactory.create(scope = scope) { file },
        )
}
