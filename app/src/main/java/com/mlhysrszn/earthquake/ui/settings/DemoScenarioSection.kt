package com.mlhysrszn.earthquake.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mlhysrszn.earthquake.R
import com.mlhysrszn.earthquake.domain.model.DemoScenarioStatus

@Composable
fun DemoScenarioSection(viewModel: DemoScenarioViewModel = hiltViewModel()) {
    val status by viewModel.status.collectAsStateWithLifecycle()
    DemoScenarioContent(
        status = status,
        onAddBelow = viewModel::addBelowThreshold,
        onAddAbove = viewModel::addAboveThreshold,
        onReplayDuplicate = viewModel::replayDuplicate,
        onReset = viewModel::reset,
    )
}

/** Renders nothing in live builds, where the controller reports itself unavailable. */
@Composable
fun DemoScenarioContent(
    status: DemoScenarioStatus,
    onAddBelow: () -> Unit,
    onAddAbove: () -> Unit,
    onReplayDuplicate: () -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!status.isAvailable) return
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.demo_scenario_title),
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = stringResource(R.string.demo_scenario_status, status.eventCount),
            style = MaterialTheme.typography.bodySmall,
        )
        Button(onClick = onAddBelow) { Text(stringResource(R.string.demo_add_below)) }
        Button(onClick = onAddAbove) { Text(stringResource(R.string.demo_add_above)) }
        OutlinedButton(onClick = onReplayDuplicate, enabled = status.canReplayDuplicate) {
            Text(stringResource(R.string.demo_replay_duplicate))
        }
        OutlinedButton(onClick = onReset) { Text(stringResource(R.string.demo_reset)) }
    }
}
