package com.wingedsheep.sdk.scripting

import kotlinx.serialization.Serializable

// =============================================================================
// Card Drawing Effects
// =============================================================================

/**
 * Draw cards effect.
 * "Draw X cards" or "Target player draws X cards"
 */
@Serializable
data class DrawCardsEffect(
    val count: DynamicAmount,
    val target: EffectTarget = EffectTarget.Controller
) : Effect {
    /** Convenience constructor for fixed amounts */
    constructor(count: Int, target: EffectTarget = EffectTarget.Controller) : this(DynamicAmount.Fixed(count), target)

    override val description: String = when (target) {
        EffectTarget.Controller -> when (val c = count) {
            is DynamicAmount.Fixed -> "Draw ${if (c.amount == 1) "a card" else "${c.amount} cards"}"
            else -> "Draw cards equal to ${c.description}"
        }
        else -> "${target.description.replaceFirstChar { it.uppercase() }} draws cards equal to ${count.description}"
    }
}

/**
 * Discard cards effect.
 * "Discard X cards" or "Target player discards X cards"
 */
@Serializable
data class DiscardCardsEffect(
    val count: Int,
    val target: EffectTarget = EffectTarget.PlayerRef(Player.TargetOpponent)
) : Effect {
    override val description: String = when (target) {
        EffectTarget.Controller -> "Discard ${if (count == 1) "a card" else "$count cards"}"
        else -> "${target.description.replaceFirstChar { it.uppercase() }} discards ${if (count == 1) "a card" else "$count cards"}"
    }
}

/**
 * Discard cards at random effect.
 * "Target opponent discards a card at random"
 * Used for cards like Mind Knives.
 */
@Serializable
data class DiscardRandomEffect(
    val count: Int = 1,
    val target: EffectTarget = EffectTarget.PlayerRef(Player.TargetOpponent)
) : Effect {
    override val description: String = when (target) {
        EffectTarget.Controller -> "Discard ${if (count == 1) "a card" else "$count cards"} at random"
        else -> "${target.description.replaceFirstChar { it.uppercase() }} discards ${if (count == 1) "a card" else "$count cards"} at random"
    }
}

/**
 * Each opponent discards cards.
 * "Each opponent discards a card."
 * Used for cards like Noxious Toad.
 *
 * @param count Number of cards each opponent discards
 * @param controllerDrawsPerDiscard If > 0, the controller draws this many cards
 *   for each card actually discarded by opponents. Used for Syphon Mind.
 */
@Serializable
data class EachOpponentDiscardsEffect(
    val count: Int = 1,
    val controllerDrawsPerDiscard: Int = 0
) : Effect {
    override val description: String = buildString {
        append("Each opponent discards ${if (count == 1) "a card" else "$count cards"}")
        if (controllerDrawsPerDiscard > 0) {
            append(". You draw a card for each card discarded this way")
        }
    }
}

/**
 * Wheel effect - each affected player shuffles their hand into their library, then draws that many cards.
 * Used for Winds of Change, Wheel of Fortune-style effects.
 */
@Serializable
data class WheelEffect(
    val target: EffectTarget = EffectTarget.PlayerRef(Player.Each)
) : Effect {
    override val description: String = when (target) {
        EffectTarget.Controller -> "Shuffle your hand into your library, then draw that many cards"
        else -> "${target.description.replaceFirstChar { it.uppercase() }} shuffles their hand into their library, then draws that many cards"
    }
}

/**
 * Each player draws X cards where X is determined by the spell's X value.
 * Used for cards like Prosperity.
 */
@Serializable
data class EachPlayerDrawsXEffect(
    val includeController: Boolean = true,
    val includeOpponents: Boolean = true
) : Effect {
    override val description: String = when {
        includeController && includeOpponents -> "Each player draws X cards"
        includeController -> "You draw X cards"
        includeOpponents -> "Each opponent draws X cards"
        else -> "Draw X cards"
    }
}

/**
 * Each player may draw up to a number of cards, gaining life for each card not drawn.
 * "Each player may draw up to two cards. For each card less than two a player draws
 * this way, that player gains 2 life."
 * Used for Temporary Truce.
 *
 * @property maxCards Maximum number of cards each player may draw
 * @property lifePerCardNotDrawn Life gained for each card fewer than maxCards drawn (0 to disable)
 */
@Serializable
data class EachPlayerMayDrawEffect(
    val maxCards: Int,
    val lifePerCardNotDrawn: Int = 0
) : Effect {
    override val description: String = buildString {
        append("Each player may draw up to $maxCards cards")
        if (lifePerCardNotDrawn > 0) {
            append(". For each card less than $maxCards a player draws this way, that player gains $lifePerCardNotDrawn life")
        }
    }
}

/**
 * Each player discards cards, then draws that many cards.
 * Used for Flux, Windfall, and similar effects.
 *
 * @param minDiscard Minimum cards to discard (0 for "any number")
 * @param maxDiscard Maximum cards to discard (null means up to hand size)
 * @param discardEntireHand If true, each player must discard their entire hand
 * @param controllerBonusDraw Extra cards the controller draws after the effect
 */
@Serializable
data class EachPlayerDiscardsDrawsEffect(
    val minDiscard: Int = 0,
    val maxDiscard: Int? = null,
    val discardEntireHand: Boolean = false,
    val controllerBonusDraw: Int = 0
) : Effect {
    override val description: String = buildString {
        if (discardEntireHand) {
            append("Each player discards their hand, then draws that many cards")
        } else if (minDiscard == 0 && maxDiscard == null) {
            append("Each player discards any number of cards, then draws that many cards")
        } else {
            append("Each player discards ")
            if (minDiscard == maxDiscard) {
                append("$minDiscard card${if (minDiscard != 1) "s" else ""}")
            } else {
                append("$minDiscard to ${maxDiscard ?: "any number of"} cards")
            }
            append(", then draws that many cards")
        }
        if (controllerBonusDraw > 0) {
            append(". Draw ${if (controllerBonusDraw == 1) "a card" else "$controllerBonusDraw cards"}")
        }
    }
}

/**
 * Look at target player's hand.
 * Used for Ingenious Thief and similar "peek" effects.
 */
@Serializable
data class LookAtTargetHandEffect(
    val target: EffectTarget = EffectTarget.PlayerRef(Player.TargetPlayer)
) : Effect {
    override val description: String = "Look at ${target.description}'s hand"
}

/**
 * Look at target face-down creature.
 * Used for Aven Soulgazer and similar morph-interaction effects.
 * Marks the face-down creature as revealed to the controller of the ability.
 */
@Serializable
data class LookAtFaceDownCreatureEffect(
    val target: EffectTarget = EffectTarget.ContextTarget(0)
) : Effect {
    override val description: String = "Look at target face-down creature"
}

/**
 * Blackmail effect - target player reveals three cards from their hand
 * and the controller chooses one of them. That player discards that card.
 * If the target player has three or fewer cards, they reveal all of them.
 *
 * @property revealCount How many cards the target player reveals (default 3)
 * @property target The player whose hand is being targeted
 */
@Serializable
data class BlackmailEffect(
    val revealCount: Int = 3,
    val target: EffectTarget = EffectTarget.ContextTarget(0)
) : Effect {
    override val description: String =
        "Target player reveals $revealCount cards from their hand and you choose one of them. That player discards that card."
}

/**
 * Reveal a player's hand (publicly visible to all players).
 * This is an atomic effect that just reveals - use with CompositeEffect for
 * "reveal and do something based on what's revealed" patterns.
 *
 * Example: Baleful Stare = CompositeEffect(RevealHandEffect, DrawCardsEffect(count))
 */
@Serializable
data class RevealHandEffect(
    val target: EffectTarget = EffectTarget.ContextTarget(0)
) : Effect {
    override val description: String = when (target) {
        EffectTarget.Controller -> "Reveal your hand"
        else -> "${target.description.replaceFirstChar { it.uppercase() }} reveals their hand"
    }
}
