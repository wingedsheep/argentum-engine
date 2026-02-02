package com.wingedsheep.sdk.scripting

import kotlinx.serialization.Serializable

/**
 * Reference to a game zone.
 *
 * @deprecated Use [Zone] enum instead. Zone is the canonical zone type that includes
 * all zones (Battlefield, Graveyard, Hand, Library, Exile, Stack, Command).
 *
 * Migration:
 * - ZoneReference.Hand -> Zone.Hand
 * - ZoneReference.Battlefield -> Zone.Battlefield
 * - ZoneReference.Graveyard -> Zone.Graveyard
 * - ZoneReference.Library -> Zone.Library
 * - ZoneReference.Exile -> Zone.Exile
 */
@Deprecated(
    message = "Use Zone enum instead",
    replaceWith = ReplaceWith("Zone", "com.wingedsheep.sdk.scripting.Zone")
)
@Serializable
sealed interface ZoneReference {
    val description: String

    @Serializable
    data object Hand : ZoneReference {
        override val description: String = "hand"
    }

    @Serializable
    data object Battlefield : ZoneReference {
        override val description: String = "battlefield"
    }

    @Serializable
    data object Graveyard : ZoneReference {
        override val description: String = "graveyard"
    }

    @Serializable
    data object Library : ZoneReference {
        override val description: String = "library"
    }

    @Serializable
    data object Exile : ZoneReference {
        override val description: String = "exile"
    }
}
