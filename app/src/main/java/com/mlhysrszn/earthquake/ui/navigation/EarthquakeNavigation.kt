package com.mlhysrszn.earthquake.ui.navigation

import androidx.compose.runtime.Composable
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.mlhysrszn.earthquake.ui.earthquakes.detail.EarthquakeDetailRoute
import com.mlhysrszn.earthquake.ui.earthquakes.detail.EarthquakeDetailViewModel
import com.mlhysrszn.earthquake.ui.earthquakes.list.EarthquakesRoute
import com.mlhysrszn.earthquake.ui.settings.NotificationSettingsRoute
import kotlinx.serialization.Serializable

@Serializable
private data object EarthquakeListKey : NavKey

@Serializable
private data class EarthquakeDetailKey(
    val earthquakeId: String,
    val entrySource: String,
) : NavKey

@Composable
fun EarthquakeNavigation(
    initialEarthquakeId: String? = null,
    initialEarthquakeSource: String = DETAIL_ENTRY_SOURCE_EXTERNAL,
) {
    val initialKeys: Array<NavKey> = if (initialEarthquakeId == null) {
        arrayOf(EarthquakeListKey)
    } else {
        arrayOf(
            EarthquakeListKey,
            EarthquakeDetailKey(initialEarthquakeId, initialEarthquakeSource),
        )
    }
    val backStack = rememberNavBackStack(*initialKeys)

    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
        ),
        entryProvider = entryProvider {
            entry<EarthquakeListKey> {
                EarthquakesRoute(
                    onEarthquakeClick = { earthquakeId ->
                        backStack.add(
                            EarthquakeDetailKey(earthquakeId, DETAIL_ENTRY_SOURCE_LIST),
                        )
                    },
                    onSettingsClick = { backStack.add(NotificationSettingsKey) },
                )
            }
            entry<EarthquakeDetailKey> { key ->
                val viewModel = hiltViewModel<
                    EarthquakeDetailViewModel,
                    EarthquakeDetailViewModel.Factory,
                >(
                    creationCallback = { factory -> factory.create(key.earthquakeId) },
                )
                EarthquakeDetailRoute(
                    viewModel = viewModel,
                    entrySource = key.entrySource,
                    onBack = { backStack.removeLastOrNull() },
                )
            }
            entry<NotificationSettingsKey> {
                NotificationSettingsRoute(
                    onBack = { backStack.removeLastOrNull() },
                )
            }
        },
    )
}

const val DETAIL_ENTRY_SOURCE_NOTIFICATION = "notification"
private const val DETAIL_ENTRY_SOURCE_EXTERNAL = "external"
private const val DETAIL_ENTRY_SOURCE_LIST = "list"
