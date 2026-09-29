package com.mlhysrszn.earthquake.domain.model

enum class DemoScenarioKind {
    BELOW_THRESHOLD,
    ABOVE_THRESHOLD,
}

data class DemoScenarioStatus(
    val isAvailable: Boolean,
    val eventCount: Int = 0,
    val hasBelowThreshold: Boolean = false,
    val hasAboveThreshold: Boolean = false,
    val canReplayDuplicate: Boolean = false,
)
