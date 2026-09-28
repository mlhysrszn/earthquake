package com.mlhysrszn.earthquake.ui.earthquakes.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mlhysrszn.earthquake.R
import com.mlhysrszn.earthquake.domain.model.Earthquake
import com.mlhysrszn.earthquake.domain.repository.RefreshResult
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun EarthquakeDetailRoute(
    viewModel: EarthquakeDetailViewModel,
    onBack: () -> Unit,
) {
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
            Text(
                text = earthquake.magnitude?.let {
                    stringResource(R.string.magnitude, formatMagnitude(it))
                } ?: stringResource(R.string.magnitude_unknown),
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.headlineSmall,
            )
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
            earthquake.sourceUrl?.let { sourceUrl ->
                Text(
                    text = stringResource(R.string.source_url, sourceUrl),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
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

private fun formatMagnitude(magnitude: Double): String = DecimalFormat(
    "0.0",
    DecimalFormatSymbols.getInstance(Locale.forLanguageTag("tr-TR")),
).format(magnitude)

private fun formatOccurrenceTime(instant: Instant): String = DateTimeFormatter
    .ofPattern("d MMM, HH:mm", Locale.forLanguageTag("tr-TR"))
    .withZone(ZoneId.systemDefault())
    .format(instant)
