package com.mlhysrszn.earthquake.ui.settings

import androidx.lifecycle.ViewModelStore
import com.mlhysrszn.earthquake.domain.model.NotificationPreferences
import com.mlhysrszn.earthquake.domain.model.ProductEventName
import com.mlhysrszn.earthquake.domain.repository.NotificationPreferencesRepository
import com.mlhysrszn.earthquake.domain.repository.ProductEventRecorder
import com.mlhysrszn.earthquake.notifications.NotificationPermissionStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NotificationSettingsViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setMainDispatcher() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun resetMainDispatcher() {
        Dispatchers.resetMain()
    }

    @Test
    fun enablingWithPermissionRecordsCorrelatedSetupAttemptAndCompletion() = runTest(dispatcher) {
        val repository = FakePreferencesRepository()
        val events = mutableListOf<Pair<ProductEventName, Map<String, String>>>()
        val viewModel = NotificationSettingsViewModel(repository) { name, properties ->
            events += name to properties
        }
        val store = ViewModelStore().apply { put("settings", viewModel) }

        try {
            viewModel.setNotificationsEnabled(enabled = true, permissionReady = true)
            advanceUntilIdle()

            assertEquals(true, repository.preferences.value.notificationsEnabled)
            assertEquals(
                listOf(
                    ProductEventName.NOTIFICATION_SETUP_STARTED,
                    ProductEventName.NOTIFICATION_PREFERENCE_SAVED,
                    ProductEventName.NOTIFICATION_SETUP_COMPLETED,
                ),
                events.map { it.first },
            )
            assertEquals(
                events[0].second["attempt_id"],
                events[2].second["attempt_id"],
            )
            assertFalse(viewModel.uiState.value.saveFailed)
        } finally {
            store.clear()
        }
    }

    @Test
    fun permissionOutcomeIsTrackedIndependentlyFromPreference() = runTest(dispatcher) {
        val repository = FakePreferencesRepository()
        val events = mutableListOf<ProductEventName>()
        val viewModel = NotificationSettingsViewModel(repository) { name, _ -> events += name }
        val store = ViewModelStore().apply { put("settings", viewModel) }

        try {
            viewModel.recordPermissionOutcome(NotificationPermissionStatus.DENIED, "runtime_request")
            advanceUntilIdle()

            assertFalse(repository.preferences.value.notificationsEnabled)
            assertEquals(listOf(ProductEventName.PERMISSION_OUTCOME), events)
        } finally {
            store.clear()
        }
    }

    @Test
    fun failedPreferenceWriteCountsAsSetupAttemptButNotCompletion() = runTest(dispatcher) {
        val repository = FakePreferencesRepository().apply { failEnabledWrite = true }
        val events = mutableListOf<ProductEventName>()
        val viewModel = NotificationSettingsViewModel(repository) { name, _ -> events += name }
        val store = ViewModelStore().apply { put("settings", viewModel) }

        try {
            viewModel.setNotificationsEnabled(enabled = true, permissionReady = true)
            advanceUntilIdle()

            assertEquals(listOf(ProductEventName.NOTIFICATION_SETUP_STARTED), events)
            assertFalse(repository.preferences.value.notificationsEnabled)
        } finally {
            store.clear()
        }
    }

    @Test
    fun setupCompletesOnlyAfterPermissionIsGranted() = runTest(dispatcher) {
        val repository = FakePreferencesRepository()
        val events = mutableListOf<Pair<ProductEventName, Map<String, String>>>()
        val viewModel = NotificationSettingsViewModel(repository) { name, properties ->
            events += name to properties
        }
        val store = ViewModelStore().apply { put("settings", viewModel) }

        try {
            viewModel.setNotificationsEnabled(enabled = true, permissionReady = false)
            advanceUntilIdle()
            assertFalse(events.any { it.first == ProductEventName.NOTIFICATION_SETUP_COMPLETED })

            viewModel.recordPermissionOutcome(NotificationPermissionStatus.DENIED, "runtime_request")
            advanceUntilIdle()
            assertFalse(events.any { it.first == ProductEventName.NOTIFICATION_SETUP_COMPLETED })

            viewModel.recordPermissionOutcome(NotificationPermissionStatus.GRANTED, "system_settings")
            viewModel.recordPermissionOutcome(NotificationPermissionStatus.GRANTED, "system_settings")
            advanceUntilIdle()

            val completions = events.filter { it.first == ProductEventName.NOTIFICATION_SETUP_COMPLETED }
            assertEquals(1, completions.size)
            assertEquals(
                events.first { it.first == ProductEventName.NOTIFICATION_SETUP_STARTED }.second["attempt_id"],
                completions.single().second["attempt_id"],
            )
        } finally {
            store.clear()
        }
    }

    private class FakePreferencesRepository : NotificationPreferencesRepository {
        val preferences = MutableStateFlow(NotificationPreferences())
        var failEnabledWrite = false

        override fun observePreferences(): Flow<NotificationPreferences> = preferences

        override suspend fun setNotificationsEnabled(enabled: Boolean) {
            if (failEnabledWrite) error("preference store failed")
            preferences.value = preferences.value.copy(notificationsEnabled = enabled)
        }

        override suspend fun setMagnitudeThreshold(threshold: Double) {
            preferences.value = preferences.value.copy(magnitudeThreshold = threshold)
        }
    }
}
