package com.wingedsheep.sdk.scripting

import kotlinx.serialization.Serializable

/**
 * Reference to a game zone.
 */
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
