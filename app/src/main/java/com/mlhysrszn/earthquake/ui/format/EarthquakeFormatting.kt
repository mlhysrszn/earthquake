package com.mlhysrszn.earthquake.ui.format

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Shared Turkish formatting for the list, details, settings, and notifications. */
private val turkish: Locale = Locale.forLanguageTag("tr-TR")

fun formatMagnitude(magnitude: Double): String =
    DecimalFormat("0.0", DecimalFormatSymbols.getInstance(turkish)).format(magnitude)

fun formatCoordinate(value: Double): String =
    DecimalFormat("0.000", DecimalFormatSymbols.getInstance(turkish)).format(value)

fun formatOccurrenceTime(instant: Instant, zone: ZoneId = ZoneId.systemDefault()): String =
    DateTimeFormatter.ofPattern("d MMM, HH:mm", turkish).withZone(zone).format(instant)

/** How long ago an event happened, rounded down; future times count as "just now". */
sealed interface RelativeAge {
    data object JustNow : RelativeAge
    data class Minutes(val value: Long) : RelativeAge
    data class Hours(val value: Long) : RelativeAge
}

fun relativeAge(instant: Instant, reference: Instant): RelativeAge {
    val elapsed = Duration.between(instant, reference)
    return when {
        elapsed < Duration.ofMinutes(1) -> RelativeAge.JustNow
        elapsed < Duration.ofHours(1) -> RelativeAge.Minutes(elapsed.toMinutes())
        else -> RelativeAge.Hours(elapsed.toHours())
    }
}

/** Visual severity bands used to color magnitudes consistently across screens. */
enum class MagnitudeSeverity {
    UNKNOWN,
    MINOR,
    LIGHT,
    MODERATE,
    STRONG,
}

fun magnitudeSeverity(magnitude: Double?): MagnitudeSeverity = when {
    magnitude == null -> MagnitudeSeverity.UNKNOWN
    magnitude < 3.0 -> MagnitudeSeverity.MINOR
    magnitude < 4.0 -> MagnitudeSeverity.LIGHT
    magnitude < 5.0 -> MagnitudeSeverity.MODERATE
    else -> MagnitudeSeverity.STRONG
}
