package com.mlhysrszn.earthquake.ui.earthquakes.detail

import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mlhysrszn.earthquake.R
import com.mlhysrszn.earthquake.domain.model.Earthquake
import com.mlhysrszn.earthquake.ui.analytics.ProductEventViewModel
import com.mlhysrszn.earthquake.domain.repository.RefreshResult
import com.mlhysrszn.earthquake.ui.format.MagnitudeBadge
import com.mlhysrszn.earthquake.ui.format.formatCoordinate
import com.mlhysrszn.earthquake.ui.format.formatMagnitude
import com.mlhysrszn.earthquake.ui.format.formatOccurrenceTime

@Composable
fun EarthquakeDetailRoute(
    viewModel: EarthquakeDetailViewModel,
    entrySource: String,
    onBack: () -> Unit,
) {
    val productEventViewModel = hiltViewModel<ProductEventViewModel>()
    LaunchedEffect(viewModel, entrySource) {
        productEventViewModel.recordScreenView(
            screen = "detail",
            properties = mapOf(
                "event_id" to viewModel.earthquakeId,
                "entry_source" to entrySource,
            ),
        )
        if (entrySource == "notification") {
            productEventViewModel.recordNotificationOpened(viewModel.earthquakeId)
        }
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    EarthquakeDetailScreen(
        state = state,
        onBack = onBack,
        onRetry = viewModel::retry,
    )
}

@Composable
fun EarthquakeDetailScreen(
    state: EarthquakeDetailUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            Surface(tonalElevation = 2.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(
                            WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
                        )
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = onBack) {
                        Text(text = stringResource(R.string.back))
                    }
                    Text(
                        text = stringResource(R.string.earthquake_detail_title),
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 8.dp),
                        style = MaterialTheme.typography.titleLarge,
                    )
                }
            }
        },
    ) { contentPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .padding(16.dp),
            contentAlignment = Alignment.TopCenter,
        ) {
            when (state) {
                EarthquakeDetailUiState.Loading -> DetailLoading()
                is EarthquakeDetailUiState.Content -> EarthquakeDetailContent(state.earthquake)
                EarthquakeDetailUiState.Unavailable -> DetailUnavailable(onRetry)
                is EarthquakeDetailUiState.Failure -> DetailFailure(state.reason, onRetry)
            }
        }
    }
}

@Composable
private fun DetailLoading() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator()
        Spacer(modifier = Modifier.size(12.dp))
        Text(text = stringResource(R.string.loading_earthquake_detail))
    }
}

@Composable
private fun EarthquakeDetailContent(earthquake: Earthquake) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MagnitudeBadge(magnitude = earthquake.magnitude)
                Text(
                    text = earthquake.magnitude?.let {
                        stringResource(R.string.magnitude, formatMagnitude(it))
                    } ?: stringResource(R.string.magnitude_unknown),
                    style = MaterialTheme.typography.headlineSmall,
                )
            }
            Text(
                text = earthquake.place?.takeIf(String::isNotBlank)
                    ?: stringResource(R.string.place_unknown),
                style = MaterialTheme.typography.titleMedium,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = stringResource(
                    R.string.occurred_at,
                    formatOccurrenceTime(earthquake.occurredAt),
                ),
                style = MaterialTheme.typography.bodyLarge,
            )
            earthquake.magnitudeType?.let { magnitudeType ->
                Text(
                    text = stringResource(R.string.magnitude_type, magnitudeType),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
            earthquake.depthKm?.let { depth ->
                Text(
                    text = stringResource(R.string.depth, formatMagnitude(depth)),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
            earthquake.updatedAt?.let { updatedAt ->
                Text(
                    text = stringResource(
                        R.string.event_updated,
                        formatOccurrenceTime(updatedAt),
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            val latitude = earthquake.latitude
            val longitude = earthquake.longitude
            if (latitude != null && longitude != null) {
                Text(
                    text = stringResource(
                        R.string.coordinates,
                        formatCoordinate(latitude),
                        formatCoordinate(longitude),
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            DetailActions(earthquake)
        }
    }
}

@Composable
private fun DetailActions(earthquake: Earthquake) {
    val context = LocalContext.current
    val latitude = earthquake.latitude
    val longitude = earthquake.longitude
    val sourceUrl = earthquake.sourceUrl
    if ((latitude == null || longitude == null) && sourceUrl == null) return
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (latitude != null && longitude != null) {
            Button(onClick = { context.openUri(mapUri(latitude, longitude, earthquake.place)) }) {
                Text(text = stringResource(R.string.open_in_map))
            }
        }
        if (sourceUrl != null) {
            OutlinedButton(onClick = { context.openUri(sourceUrl.toUri()) }) {
                Text(text = stringResource(R.string.open_source_page))
            }
        }
    }
}

/** A geo URI with a labeled pin; map apps center on the coordinates. */
internal fun mapUri(latitude: Double, longitude: Double, label: String?): Uri {
    val point = "$latitude,$longitude"
    val query = label?.takeIf(String::isNotBlank)?.let { "$point($it)" } ?: point
    return "geo:$point?q=${Uri.encode(query)}".toUri()
}

private fun Context.openUri(uri: Uri) {
    try {
        startActivity(Intent(Intent.ACTION_VIEW, uri))
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(this, R.string.no_app_to_open, Toast.LENGTH_SHORT).show()
    }
}

@Composable
private fun DetailUnavailable(onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(R.string.earthquake_unavailable),
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(modifier = Modifier.size(12.dp))
        Button(onClick = onRetry) {
            Text(text = stringResource(R.string.retry))
        }
    }
}

@Composable
private fun DetailFailure(
    reason: RefreshResult.Reason,
    onRetry: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(R.string.detail_load_failed),
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(modifier = Modifier.size(8.dp))
        Text(
            text = stringResource(reason.messageResource()),
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(modifier = Modifier.size(12.dp))
        Button(onClick = onRetry) {
            Text(text = stringResource(R.string.retry))
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
