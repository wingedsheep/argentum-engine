package com.wingedsheep.sdk.scripting.references

import kotlinx.serialization.SerialName
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
    @SerialName("You")
    @Serializable
    data object You : Player {
        override val description: String = "you"
    }

    /**
     * A genuinely non-targeted "an opponent" — used only where the printed text has a
     * single opponent act without targeting them (a chooser: "an opponent chooses a
     * creature type", "choose ... an opponent"). The engine currently resolves this to
     * the controller's first opponent in turn order; the proper multiplayer flow (the
     * controller picks which opponent) is tracked in `backlog/multiplayer.md`.
     *
     * Do NOT use this for:
     * - "target opponent" → [TargetOpponent]
     * - "each opponent" / "your opponents" / "an opponent controls" (exists/aggregation) → [EachOpponent]
     * - "defending player" / combat-damage-to-a-player triggers → [DefendingPlayer]
     * - "that player" in a per-opponent trigger ("at the beginning of each opponent's
     *   upkeep, that player ...") → [TriggeringPlayer]
     */
    @SerialName("AnOpponent")
    @Serializable
    data object AnOpponent : Player {
        override val description: String = "an opponent"
    }

    /**
     * The defending player, per CR 802.2a: the specific player the ability's source is
     * attacking, determined per attacking creature — never "the opponent" via turn order.
     * Resolves through the source's attack assignment (a creature attacking a planeswalker
     * defends against that planeswalker's controller); for "deals combat damage to a
     * player" triggers whose source has left combat before resolution, the damaged player
     * is read from the trigger context as last-known information.
     */
    @SerialName("DefendingPlayer")
    @Serializable
    data object DefendingPlayer : Player {
        override val description: String = "defending player"
    }

    /** All opponents */
    @SerialName("EachOpponent")
    @Serializable
    data object EachOpponent : Player {
        override val description: String = "each opponent"
    }

    /** All players */
    @SerialName("Each")
    @Serializable
    data object Each : Player {
        override val description: String = "each player"
    }

    /** All players in APNAP order (active player first, then turn order) */
    @SerialName("ActivePlayerFirst")
    @Serializable
    data object ActivePlayerFirst : Player {
        override val description: String = "each player"
    }

    /** Any player (for matching/filtering) */
    @SerialName("Any")
    @Serializable
    data object Any : Player {
        override val description: String = "a player"
    }

    // =============================================================================
    // Target-Bound Player References
    // =============================================================================

    /** A targeted player (resolved at effect execution) */
    @SerialName("TargetPlayer")
    @Serializable
    data object TargetPlayer : Player {
        override val description: String = "target player"
    }

    /** A targeted opponent (resolved at effect execution) */
    @SerialName("TargetOpponent")
    @Serializable
    data object TargetOpponent : Player {
        override val description: String = "target opponent"
    }

    /** A player from the context (for multi-target spells) */
    @SerialName("ContextPlayer")
    @Serializable
    data class ContextPlayer(val index: Int) : Player {
        override val description: String = "that player"
    }

    /**
     * The player currently being considered as a target (CR 115). Bound by the engine's
     * target enumerator/validator to each candidate player in turn while evaluating a
     * [com.wingedsheep.sdk.scripting.targets.TargetPlayer.restriction] /
     * [com.wingedsheep.sdk.scripting.targets.TargetOpponent.restriction]. It only resolves
     * inside that restriction-evaluation context (where `EffectContext.candidatePlayerId`
     * is set) — at effect-execution time there is no candidate, so it resolves to nothing.
     * Reach it through the `Conditions.candidate*` facade rather than constructing it by hand.
     */
    @SerialName("Candidate")
    @Serializable
    data object Candidate : Player {
        override val description: String = "that player"
    }

    /** The player from the trigger context (e.g., player dealt combat damage) */
    @SerialName("TriggeringPlayer")
    @Serializable
    data object TriggeringPlayer : Player {
        override val description: String = "that player"
    }

    // =============================================================================
    // Relational Player References
    // =============================================================================

    /**
     * The opponent locked into the source's [com.wingedsheep.sdk.scripting.ChoiceSlot.OPPONENT]
     * slot (set by an `EntersWithChoice(ChoiceType.OPPONENT, …)` replacement effect).
     * Resolves to that stored player entity id; null if no opponent has been chosen on
     * the source.
     *
     * Used by cards like Jihad ("White creatures get +2/+1 as long as the chosen player
     * controls a nontoken permanent of the chosen color") — `Exists(Player.ChosenOpponent,
     * Zone.BATTLEFIELD, …)` reads the chosen opponent from the source's
     * [com.wingedsheep.engine.state.components.battlefield.CastChoicesComponent].
     */
    @SerialName("ChosenOpponent")
    @Serializable
    data object ChosenOpponent : Player {
        override val description: String = "the chosen player"
    }

    /** Controller of a permanent (used with EffectTarget) */
    @SerialName("ControllerOf")
    @Serializable
    data class ControllerOf(val targetDescription: String) : Player {
        override val description: String = "its controller"
    }

    /** Owner of a permanent (used with EffectTarget) */
    @SerialName("OwnerOf")
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
            AnOpponent -> "an opponent's"
            DefendingPlayer -> "defending player's"
            TargetOpponent -> "target opponent's"
            TargetPlayer -> "target player's"
            Each -> "each player's"
            ActivePlayerFirst -> "each player's"
            EachOpponent -> "each opponent's"
            Any -> "a player's"
            is ContextPlayer -> "that player's"
            Candidate -> "that player's"
            TriggeringPlayer -> "that player's"
            ChosenOpponent -> "the chosen player's"
            is ControllerOf -> "its controller's"
            is OwnerOf -> "its owner's"
        }
}
