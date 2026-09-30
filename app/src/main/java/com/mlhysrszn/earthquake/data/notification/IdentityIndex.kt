package com.mlhysrszn.earthquake.data.notification

import com.mlhysrszn.earthquake.data.local.room.NotificationEventAliasEntity
import com.mlhysrszn.earthquake.domain.model.Earthquake

/** An event's canonical ID; [ambiguous] when its IDs point to more than one known event. */
internal data class ResolvedIdentity(
    val canonicalId: String,
    val tokens: Set<String>,
    val ambiguous: Boolean,
)

/**
 * Maps every known USGS ID and alias to one canonical event ID. The feature ID of a new event
 * becomes canonical; later records sharing any ID with it resolve to the same event.
 */
internal class IdentityIndex(storedAliases: List<NotificationEventAliasEntity>) {
    private val canonicalIdByAlias = storedAliases
        .associateTo(mutableMapOf()) { alias -> alias.aliasId to alias.canonicalEventId }
    private val changedAliases = linkedMapOf<String, NotificationEventAliasEntity>()

    fun resolve(earthquake: Earthquake): ResolvedIdentity {
        val tokens = (earthquake.aliases + earthquake.id)
            .map(String::trim)
            .filter(String::isNotEmpty)
            .toSortedSet()
        val knownCanonicalIds = tokens.mapNotNull(canonicalIdByAlias::get).toSet()
        return ResolvedIdentity(
            canonicalId = knownCanonicalIds.singleOrNull() ?: earthquake.id,
            tokens = tokens,
            ambiguous = knownCanonicalIds.size > 1,
        )
    }

    /** Links unclaimed tokens to the canonical ID; tokens owned by another event are left alone. */
    fun remember(identity: ResolvedIdentity, nowEpochMillis: Long) {
        identity.tokens.forEach { aliasId ->
            val known = canonicalIdByAlias[aliasId]
            if (known == null || known == identity.canonicalId) {
                canonicalIdByAlias[aliasId] = identity.canonicalId
                changedAliases[aliasId] = NotificationEventAliasEntity(
                    aliasId = aliasId,
                    canonicalEventId = identity.canonicalId,
                    lastSeenAtEpochMillis = nowEpochMillis,
                )
            }
        }
    }

    fun changedAliases(): List<NotificationEventAliasEntity> = changedAliases.values.toList()
}
