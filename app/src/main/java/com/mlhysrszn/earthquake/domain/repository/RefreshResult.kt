package com.mlhysrszn.earthquake.domain.repository

/** The outcome of a repository refresh, safe for presentation to interpret. */
sealed interface RefreshResult {
    /** A valid response was processed; this is the count of accepted earthquake records. */
    data class Success(val acceptedEventCount: Int) : RefreshResult {
        init {
            require(acceptedEventCount >= 0) { "Accepted event count cannot be negative" }
        }
    }

    /** A refresh failure classified without exposing transport or storage implementation types. */
    data class Failure(val reason: Reason) : RefreshResult

    enum class Reason {
        NETWORK,
        INVALID_RESPONSE,
        STORAGE,
        UNKNOWN,
    }
}
