package com.mlhysrszn.earthquake.data.notification

import com.mlhysrszn.earthquake.data.local.room.NotificationEventAliasEntity
import com.mlhysrszn.earthquake.domain.model.Earthquake
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class IdentityIndexTest {
    @Test
    fun `new event uses its own ID and trimmed aliases`() {
        val identity = IdentityIndex(emptyList()).resolve(event("us1", aliases = setOf(" ak9 ", "")))

        assertEquals("us1", identity.canonicalId)
        assertEquals(setOf("ak9", "us1"), identity.tokens)
        assertFalse(identity.ambiguous)
    }

    @Test
    fun `record sharing an alias resolves to the remembered event`() {
        val index = IdentityIndex(emptyList())
        index.remember(index.resolve(event("us1", aliases = setOf("ak9"))), nowEpochMillis = 1)

        val later = index.resolve(event("ak9"))

        assertEquals("us1", later.canonicalId)
        assertEquals(
            listOf("ak9" to "us1", "us1" to "us1"),
            index.changedAliases().map { it.aliasId to it.canonicalEventId },
        )
    }

    @Test
    fun `IDs pointing to two known events are ambiguous`() {
        val index = IdentityIndex(
            listOf(
                NotificationEventAliasEntity("us1", "us1", 0),
                NotificationEventAliasEntity("ak9", "ak9", 0),
            ),
        )

        val identity = index.resolve(event("nc5", aliases = setOf("us1", "ak9")))

        assertTrue(identity.ambiguous)
        assertEquals("nc5", identity.canonicalId)
    }

    @Test
    fun `remember never moves an alias owned by another event`() {
        val index = IdentityIndex(listOf(NotificationEventAliasEntity("ak9", "ak9", 0)))
        val identity = ResolvedIdentity(canonicalId = "us1", tokens = setOf("us1", "ak9"), ambiguous = false)

        index.remember(identity, nowEpochMillis = 5)

        assertEquals(listOf("us1"), index.changedAliases().map { it.aliasId })
        assertEquals("ak9", index.resolve(event("ak9")).canonicalId)
    }

    private fun event(id: String, aliases: Set<String> = emptySet()) = Earthquake(
        id = id,
        magnitude = 4.5,
        magnitudeType = "ml",
        place = null,
        occurredAt = Instant.parse("2026-09-30T12:00:00Z"),
        updatedAt = null,
        longitude = null,
        latitude = null,
        depthKm = null,
        sourceUrl = null,
        aliases = aliases,
    )
}
