package com.mlhysrszn.earthquake.ui.activitylog

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mlhysrszn.earthquake.R
import com.mlhysrszn.earthquake.domain.model.NotificationDecision
import com.mlhysrszn.earthquake.domain.model.NotificationDecisionOutcome
import com.mlhysrszn.earthquake.domain.model.ProductEvent
import com.mlhysrszn.earthquake.domain.usecase.ProductMetrics
import com.mlhysrszn.earthquake.domain.usecase.Ratio
import com.mlhysrszn.earthquake.ui.format.MagnitudeBadge
import com.mlhysrszn.earthquake.ui.format.formatLogTime
import com.mlhysrszn.earthquake.ui.format.formatOccurrenceTime

@Composable
fun ActivityLogRoute(
    onBack: () -> Unit,
    viewModel: ActivityLogViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ActivityLogScreen(state = state, onBack = onBack)
}

@Composable
fun ActivityLogScreen(
    state: ActivityLogUiState,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            Surface(tonalElevation = 2.dp) {
                Column(
                    modifier = Modifier.windowInsetsPadding(
                        WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
                    ),
                ) {
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
                            text = stringResource(R.string.activity_log_title),
                            modifier = Modifier.padding(start = 8.dp),
                            style = MaterialTheme.typography.titleLarge,
                        )
                    }
                    PrimaryTabRow(selectedTabIndex = selectedTab) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = { Text(stringResource(R.string.activity_log_decisions_tab)) },
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = { Text(stringResource(R.string.activity_log_events_tab)) },
                        )
                    }
                }
            }
        },
    ) { contentPadding ->
        val contentModifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
        when {
            state.isLoading -> Box(contentModifier, contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            selectedTab == 0 -> DecisionList(state.decisions, contentModifier)
            else -> EventList(state.events, state.metrics, contentModifier)
        }
    }
}

@Composable
private fun DecisionList(decisions: List<NotificationDecision>, modifier: Modifier) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item(key = "decisions-explanation") {
            Text(
                text = stringResource(R.string.activity_log_decisions_explanation),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        if (decisions.isEmpty()) {
            item(key = "decisions-empty") { EmptyLog(R.string.activity_log_decisions_empty) }
        }
        items(decisions, key = NotificationDecision::canonicalEventId) { decision ->
            DecisionCard(decision)
        }
    }
}

@Composable
private fun DecisionCard(decision: NotificationDecision) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MagnitudeBadge(magnitude = decision.magnitude)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(decision.outcome.labelResource()),
                    color = if (decision.outcome.isNotified()) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    text = decision.place?.takeIf(String::isNotBlank) ?: decision.eventId,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = stringResource(
                        R.string.activity_log_decision_times,
                        formatOccurrenceTime(decision.occurredAt),
                        formatLogTime(decision.decidedAt),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun EventList(events: List<ProductEvent>, metrics: ProductMetrics?, modifier: Modifier) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        metrics?.let { item(key = "metrics") { MetricsCard(it) } }
        if (events.isEmpty()) {
            item(key = "events-empty") { EmptyLog(R.string.activity_log_events_empty) }
        }
        items(events, key = ProductEvent::id) { event -> EventRow(event) }
    }
}

@Composable
private fun MetricsCard(metrics: ProductMetrics) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = stringResource(R.string.activity_log_metrics_title),
                style = MaterialTheme.typography.titleSmall,
            )
            MetricLine(R.string.activity_log_metric_setup, metrics.setupCompletion)
            MetricLine(R.string.activity_log_metric_opening, metrics.notificationOpening)
            MetricLine(R.string.activity_log_metric_refresh_failure, metrics.refreshFailure)
            Text(
                text = stringResource(
                    R.string.activity_log_metrics_scope,
                    ActivityLogViewModel.EVENT_LIMIT,
                ),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun MetricLine(@StringRes label: Int, ratio: Ratio) {
    val value = ratio.percent?.let {
        stringResource(R.string.activity_log_metric_value, ratio.numerator, ratio.denominator, it)
    } ?: stringResource(R.string.activity_log_metric_no_data)
    Text(
        text = stringResource(R.string.activity_log_metric_line, stringResource(label), value),
        style = MaterialTheme.typography.bodyMedium,
    )
}

@Composable
private fun EventRow(event: ProductEvent) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(
            text = stringResource(
                R.string.activity_log_event_header,
                formatLogTime(event.occurredAt),
                event.name.name,
            ),
            style = MaterialTheme.typography.titleSmall,
        )
        if (event.properties.isNotEmpty()) {
            Text(
                text = event.properties.entries.joinToString(" · ") { (key, value) -> "$key=$value" },
                style = MaterialTheme.typography.bodySmall,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun EmptyLog(@StringRes message: Int) {
    Text(
        text = stringResource(message),
        modifier = Modifier.padding(vertical = 24.dp),
        style = MaterialTheme.typography.bodyLarge,
    )
}

private fun NotificationDecisionOutcome.isNotified(): Boolean =
    this == NotificationDecisionOutcome.POSTED

@StringRes
private fun NotificationDecisionOutcome.labelResource(): Int = when (this) {
    NotificationDecisionOutcome.POSTED -> R.string.decision_posted
    NotificationDecisionOutcome.PENDING -> R.string.decision_pending
    NotificationDecisionOutcome.RETRYABLE -> R.string.decision_retryable
    NotificationDecisionOutcome.EXPIRED -> R.string.decision_expired
    NotificationDecisionOutcome.BASELINED -> R.string.decision_baselined
    NotificationDecisionOutcome.SUPPRESSED_BELOW_THRESHOLD -> R.string.decision_below_threshold
    NotificationDecisionOutcome.SUPPRESSED_AT_THRESHOLD -> R.string.decision_at_threshold
    NotificationDecisionOutcome.SUPPRESSED_MISSING_MAGNITUDE -> R.string.decision_missing_magnitude
    NotificationDecisionOutcome.SUPPRESSED_DISABLED -> R.string.decision_disabled
    NotificationDecisionOutcome.SUPPRESSED_PERMISSION -> R.string.decision_permission
    NotificationDecisionOutcome.SUPPRESSED_OUTSIDE_WINDOW -> R.string.decision_outside_window
    NotificationDecisionOutcome.SUPPRESSED_ALREADY_PROCESSED -> R.string.decision_already_processed
    NotificationDecisionOutcome.AMBIGUOUS_IDENTITY -> R.string.decision_ambiguous
    NotificationDecisionOutcome.UNKNOWN -> R.string.decision_unknown
}
