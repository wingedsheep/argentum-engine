package com.wingedsheep.sdk.scripting

import kotlinx.serialization.Serializable

/**
 * Unified player reference type.
 *
 * Used for:
 * - Effect targets: "target player", "each opponent"
 * - Zone scoping: "your graveyard", "opponent's hand"
 * - Counting: "creatures you control"
 */
@Serializable
sealed interface Player {
    val description: String

    // =============================================================================
    // Static Player References
    // =============================================================================

    /** The controller of the ability/effect */
    @Serializable
    data object You : Player {
        override val description: String = "you"
    }

    /** Any single opponent (not targeted) */
    @Serializable
    data object Opponent : Player {
        override val description: String = "an opponent"
    }

    /** All opponents */
    @Serializable
    data object EachOpponent : Player {
        override val description: String = "each opponent"
    }

    /** All players */
    @Serializable
    data object Each : Player {
        override val description: String = "each player"
    }

    /** Any player (for matching/filtering) */
    @Serializable
    data object Any : Player {
        override val description: String = "a player"
    }

    // =============================================================================
    // Target-Bound Player References
    // =============================================================================

    /** A targeted player (resolved at effect execution) */
    @Serializable
    data object TargetPlayer : Player {
        override val description: String = "target player"
    }

    /** A targeted opponent (resolved at effect execution) */
    @Serializable
    data object TargetOpponent : Player {
        override val description: String = "target opponent"
    }

    /** A player from the context (for multi-target spells) */
    @Serializable
    data class ContextPlayer(val index: Int) : Player {
        override val description: String = "that player"
    }

    // =============================================================================
    // Relational Player References
    // =============================================================================

    /** Controller of a permanent (used with EffectTarget) */
    @Serializable
    data class ControllerOf(val targetDescription: String) : Player {
        override val description: String = "its controller"
    }

    /** Owner of a permanent (used with EffectTarget) */
    @Serializable
    data class OwnerOf(val targetDescription: String) : Player {
        override val description: String = "its owner"
    }

    // =============================================================================
    // Possessive Forms (for descriptions)
    // =============================================================================

    /** Get possessive form for zone descriptions like "your hand", "opponent's graveyard" */
    val possessive: String
        get() = when (this) {
            You -> "your"
            Opponent -> "opponent's"
            TargetOpponent -> "target opponent's"
            TargetPlayer -> "target player's"
            Each -> "each player's"
            EachOpponent -> "each opponent's"
            Any -> "a player's"
            is ContextPlayer -> "that player's"
            is ControllerOf -> "its controller's"
            is OwnerOf -> "its owner's"
        }
}
