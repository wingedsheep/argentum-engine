package com.wingedsheep.sdk.scripting

import kotlinx.serialization.Serializable

/**
 * Reference to a player in zone counting and similar contexts.
 */
@Serializable
sealed interface PlayerReference {
    val description: String

    @Serializable
    data object You : PlayerReference {
        override val description: String = "your"
    }

    @Serializable
    data object Opponent : PlayerReference {
        override val description: String = "opponent"
    }

    @Serializable
    data object TargetOpponent : PlayerReference {
        override val description: String = "target opponent"
    }

    @Serializable
    data object TargetPlayer : PlayerReference {
        override val description: String = "target player"
    }

    @Serializable
    data object Each : PlayerReference {
        override val description: String = "each player"
    }
}
