package com.mlhysrszn.earthquake.ui.format

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import com.mlhysrszn.earthquake.R
import java.time.Instant

/** A magnitude chip colored by severity, so strong events stand out while scanning the list. */
@Composable
fun MagnitudeBadge(
    magnitude: Double?,
    modifier: Modifier = Modifier,
) {
    val (container, content) = severityColors(magnitudeSeverity(magnitude))
    val label = magnitude?.let(::formatMagnitude) ?: "?"
    val description = magnitude?.let { stringResource(R.string.magnitude, label) }
        ?: stringResource(R.string.magnitude_unknown)
    Box(
        modifier = modifier
            .size(56.dp)
            .background(container, RoundedCornerShape(12.dp))
            .clearAndSetSemantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Text(text = label, color = content, style = MaterialTheme.typography.titleLarge)
    }
}

@Composable
private fun severityColors(severity: MagnitudeSeverity): Pair<Color, Color> {
    val scheme = MaterialTheme.colorScheme
    return when (severity) {
        MagnitudeSeverity.UNKNOWN,
        MagnitudeSeverity.MINOR,
        -> scheme.surface to scheme.onSurfaceVariant
        MagnitudeSeverity.LIGHT -> scheme.secondaryContainer to scheme.onSecondaryContainer
        MagnitudeSeverity.MODERATE -> scheme.tertiaryContainer to scheme.onTertiaryContainer
        MagnitudeSeverity.STRONG -> scheme.error to scheme.onError
    }
}

@Composable
fun relativeAgeText(instant: Instant, reference: Instant): String =
    when (val age = relativeAge(instant, reference)) {
        RelativeAge.JustNow -> stringResource(R.string.relative_time_just_now)
        is RelativeAge.Minutes -> stringResource(R.string.relative_time_minutes, age.value)
        is RelativeAge.Hours -> stringResource(R.string.relative_time_hours, age.value)
    }
