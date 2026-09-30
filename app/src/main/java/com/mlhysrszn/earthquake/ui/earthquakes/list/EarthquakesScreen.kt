package com.mlhysrszn.earthquake.ui.earthquakes.list

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mlhysrszn.earthquake.R
import com.mlhysrszn.earthquake.domain.model.Earthquake
import com.mlhysrszn.earthquake.domain.repository.RefreshResult
import com.mlhysrszn.earthquake.ui.analytics.ProductEventViewModel
import com.mlhysrszn.earthquake.ui.format.MagnitudeBadge
import com.mlhysrszn.earthquake.ui.format.formatMagnitude
import com.mlhysrszn.earthquake.ui.format.formatOccurrenceTime
import com.mlhysrszn.earthquake.ui.format.relativeAgeText
import java.time.Instant

@Composable
fun EarthquakesRoute(
    onEarthquakeClick: (String) -> Unit,
    onSettingsClick: () -> Unit,
    viewModel: EarthquakesViewModel = hiltViewModel(),
) {
    val productEventViewModel = hiltViewModel<ProductEventViewModel>()
    LaunchedEffect(Unit) {
        productEventViewModel.recordScreenView(screen = "list")
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.refreshIfStale()
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    EarthquakesScreen(
        state = state,
        onRefresh = viewModel::refresh,
        onRetry = viewModel::retry,
        onEarthquakeClick = onEarthquakeClick,
        onSettingsClick = onSettingsClick,
        onMinimumMagnitudeChange = viewModel::setMinimumMagnitude,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EarthquakesScreen(
    state: EarthquakesUiState,
    onRefresh: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    onEarthquakeClick: (String) -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onMinimumMagnitudeChange: (Double?) -> Unit = {},
) {
    val hasAnyEarthquakes = state.earthquakes.isNotEmpty() || state.totalEarthquakeCount > 0
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            Surface(tonalElevation = 2.dp) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(
                            WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
                        )
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(R.string.earthquakes_title),
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.headlineSmall,
                        )
                        TextButton(
                            onClick = onRefresh,
                            enabled = !state.isInitialLoading && !state.isRefreshing,
                        ) {
                            Text(text = stringResource(R.string.refresh))
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(R.string.earthquakes_window),
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        TextButton(onClick = onSettingsClick) {
                            Text(text = stringResource(R.string.notification_settings_short))
                        }
                    }
                    MagnitudeFilterRow(
                        selected = state.minimumMagnitude,
                        onSelect = onMinimumMagnitudeChange,
                    )
                }
            }
        },
    ) { contentPadding ->
        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding),
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (!hasAnyEarthquakes && (state.isInitialLoading || state.isRefreshing)) {
                    item(key = "initial-loading") {
                        LoadingContent(
                            modifier = Modifier.fillParentMaxSize(),
                            messageResource = if (state.isRefreshing) {
                                R.string.updating
                            } else {
                                R.string.loading_earthquakes
                            },
                        )
                    }
                } else if (state.earthquakes.isEmpty() && hasAnyEarthquakes) {
                    item(key = "filtered-empty-state") {
                        FilteredEmptyContent(
                            onClearFilter = { onMinimumMagnitudeChange(null) },
                            modifier = Modifier.fillParentMaxSize(),
                        )
                    }
                } else if (state.earthquakes.isEmpty()) {
                    item(key = "empty-state") {
                        EmptyContent(
                            error = state.refreshError,
                            onRetry = onRetry,
                            modifier = Modifier.fillParentMaxSize(),
                        )
                    }
                } else {
                    if (state.isInitialLoading || state.isRefreshing) {
                        item(key = "refresh-progress") {
                            RefreshProgressContent()
                        }
                    }
                    state.refreshError?.let { error ->
                        item(key = "refresh-error") {
                            RefreshErrorContent(error = error, onRetry = onRetry)
                        }
                    }
                    state.lastUpdatedAt?.let { updatedAt ->
                        item(key = "last-updated") {
                            Text(
                                text = stringResource(
                                    R.string.last_updated,
                                    formatOccurrenceTime(updatedAt),
                                ),
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                    items(state.earthquakes, key = Earthquake::id) { earthquake ->
                        EarthquakeCard(
                            earthquake = earthquake,
                            referenceTime = state.referenceTime,
                            onClick = { onEarthquakeClick(earthquake.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MagnitudeFilterRow(
    selected: Double?,
    onSelect: (Double?) -> Unit,
) {
    val filterLabel = stringResource(R.string.magnitude_filter_label)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .semantics { contentDescription = filterLabel },
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        EarthquakesViewModel.MAGNITUDE_FILTERS.forEach { minimum ->
            FilterChip(
                selected = selected == minimum,
                onClick = { onSelect(minimum) },
                label = {
                    Text(
                        text = minimum?.let {
                            stringResource(R.string.magnitude_filter_minimum, formatMagnitude(it))
                        } ?: stringResource(R.string.magnitude_filter_all),
                    )
                },
            )
        }
    }
}

@Composable
private fun FilteredEmptyContent(
    onClearFilter: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.padding(24.dp), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.empty_filtered_earthquakes),
                style = MaterialTheme.typography.titleMedium,
            )
            TextButton(onClick = onClearFilter) {
                Text(text = stringResource(R.string.clear_filter))
            }
        }
    }
}

@Composable
private fun LoadingContent(
    modifier: Modifier = Modifier,
    messageResource: Int = R.string.loading_earthquakes,
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.size(12.dp))
            Text(text = stringResource(messageResource))
        }
    }
}

@Composable
private fun RefreshProgressContent() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        Text(text = stringResource(R.string.updating), style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun EmptyContent(
    error: RefreshResult.Reason?,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.padding(24.dp), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(
                    if (error == null) R.string.empty_earthquakes else R.string.earthquakes_load_failed,
                ),
                style = MaterialTheme.typography.titleMedium,
            )
            if (error != null) {
                Text(
                    text = stringResource(error.messageResource()),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Button(onClick = onRetry) {
                    Text(text = stringResource(R.string.retry))
                }
            }
        }
    }
}

@Composable
private fun RefreshErrorContent(
    error: RefreshResult.Reason,
    onRetry: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.refresh_failed),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                text = stringResource(error.messageResource()),
                style = MaterialTheme.typography.bodyMedium,
            )
            TextButton(onClick = onRetry) {
                Text(text = stringResource(R.string.retry))
            }
        }
    }
}

@Composable
private fun EarthquakeCard(
    earthquake: Earthquake,
    referenceTime: Instant?,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable(role = Role.Button, onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MagnitudeBadge(magnitude = earthquake.magnitude)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = earthquake.place?.takeIf(String::isNotBlank)
                        ?: stringResource(R.string.place_unknown),
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
                val occurredAt = stringResource(
                    R.string.occurred_at,
                    formatOccurrenceTime(earthquake.occurredAt),
                )
                Text(
                    text = referenceTime?.let { reference ->
                        stringResource(
                            R.string.occurred_at_with_age,
                            occurredAt,
                            relativeAgeText(earthquake.occurredAt, reference),
                        )
                    } ?: occurredAt,
                    style = MaterialTheme.typography.bodyMedium,
                )
                earthquake.depthKm?.let { depth ->
                    Text(
                        text = stringResource(R.string.depth, formatMagnitude(depth)),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}

@Composable
private fun RefreshResult.Reason.messageResource(): Int = when (this) {
    RefreshResult.Reason.NETWORK -> R.string.network_error
    RefreshResult.Reason.INVALID_RESPONSE -> R.string.invalid_response_error
    RefreshResult.Reason.STORAGE -> R.string.storage_error
    RefreshResult.Reason.UNKNOWN -> R.string.unknown_error
}
