package com.wingedsheep.sdk.scripting

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// =============================================================================
// Card Drawing Effects
// =============================================================================

/**
 * Draw cards effect.
 * "Draw X cards" or "Target player draws X cards"
 */
@SerialName("DrawCards")
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
@SerialName("DiscardCards")
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
@SerialName("DiscardRandom")
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
@SerialName("EachOpponentDiscards")
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
@SerialName("Wheel")
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
@SerialName("EachPlayerDrawsX")
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
@SerialName("EachPlayerMayDraw")
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
@SerialName("EachPlayerDiscardsDraws")
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
@SerialName("LookAtTargetHand")
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
@SerialName("LookAtFaceDownCreature")
@Serializable
data class LookAtFaceDownCreatureEffect(
    val target: EffectTarget = EffectTarget.ContextTarget(0)
) : Effect {
    override val description: String = "Look at target face-down creature"
}

/**
 * Look at all face-down creatures a player controls.
 * Used for Spy Network and similar effects that reveal all face-down creatures.
 * Marks each face-down creature as revealed to the controller of the ability.
 *
 * @property target The player whose face-down creatures to look at
 */
@SerialName("LookAtAllFaceDownCreatures")
@Serializable
data class LookAtAllFaceDownCreaturesEffect(
    val target: EffectTarget = EffectTarget.PlayerRef(Player.TargetPlayer)
) : Effect {
    override val description: String = "Look at any face-down creatures ${target.description} controls"
}

/**
 * Blackmail effect - target player reveals three cards from their hand
 * and the controller chooses one of them. That player discards that card.
 * If the target player has three or fewer cards, they reveal all of them.
 *
 * @property revealCount How many cards the target player reveals (default 3)
 * @property target The player whose hand is being targeted
 */
@SerialName("Blackmail")
@Serializable
data class BlackmailEffect(
    val revealCount: Int = 3,
    val target: EffectTarget = EffectTarget.ContextTarget(0)
) : Effect {
    override val description: String =
        "Target player reveals $revealCount cards from their hand and you choose one of them. That player discards that card."
}

/**
 * Each player discards a card, then each player who didn't discard a creature card loses life.
 * Used for Strongarm Tactics.
 *
 * @property lifeLoss Life lost by each player who didn't discard a creature card
 */
@SerialName("EachPlayerDiscardsOrLoseLife")
@Serializable
data class EachPlayerDiscardsOrLoseLifeEffect(
    val lifeLoss: Int = 4
) : Effect {
    override val description: String =
        "Each player discards a card. Then each player who didn't discard a creature card this way loses $lifeLoss life."
}

/**
 * Target player discards cards, then that player may copy this spell
 * and may choose a new target for that copy.
 * Used for Chain of Smog and similar "chain discard" mechanics.
 *
 * @property count Number of cards to discard
 * @property target The player who discards
 * @property spellName The name of the spell (for the copy's description on the stack)
 */
@SerialName("DiscardAndChainCopy")
@Serializable
data class DiscardAndChainCopyEffect(
    val count: Int,
    val target: EffectTarget = EffectTarget.PlayerRef(Player.TargetPlayer),
    val spellName: String
) : Effect {
    override val description: String = buildString {
        append("Target player discards ${if (count == 1) "a card" else "$count cards"}. ")
        append("That player may copy this spell and may choose a new target for that copy")
    }
}

/**
 * Target player discards their entire hand.
 * "Target opponent discards their hand"
 * Used for Wheel and Deal and similar effects.
 */
@SerialName("DiscardHand")
@Serializable
data class DiscardHandEffect(
    val target: EffectTarget = EffectTarget.ContextTarget(0)
) : Effect {
    override val description: String = when (target) {
        EffectTarget.Controller -> "Discard your hand"
        else -> "${target.description.replaceFirstChar { it.uppercase() }} discards their hand"
    }
}

/**
 * Replace next draw with life gain effect.
 * "{1}: The next time you would draw a card this turn, you gain X life instead."
 * Used for Words of Worship and similar "Words of" cycle cards.
 *
 * This creates a floating replacement effect shield that intercepts the next card draw
 * for the controller this turn, replacing it with life gain.
 *
 * @property lifeAmount The amount of life gained instead of drawing
 */
@SerialName("ReplaceNextDrawWithLifeGain")
@Serializable
data class ReplaceNextDrawWithLifeGainEffect(
    val lifeAmount: Int
) : Effect {
    override val description: String =
        "The next time you would draw a card this turn, you gain $lifeAmount life instead"
}

/**
 * Replace next draw with each-player-bounces effect.
 * "{1}: The next time you would draw a card this turn, each player returns a
 * permanent they control to its owner's hand instead."
 * Used for Words of Wind and similar "Words of" cycle cards.
 *
 * This creates a floating replacement effect shield that intercepts the next card draw
 * for the controller this turn, replacing it with each player bouncing a permanent.
 */
@SerialName("ReplaceNextDrawWithBounce")
@Serializable
data object ReplaceNextDrawWithBounceEffect : Effect {
    override val description: String =
        "The next time you would draw a card this turn, each player returns a permanent they control to its owner's hand instead"
}

/**
 * Replace next draw with each-opponent-discards effect.
 * "{1}: The next time you would draw a card this turn, each opponent discards a card instead."
 * Used for Words of Waste and similar "Words of" cycle cards.
 *
 * This creates a floating replacement effect shield that intercepts the next card draw
 * for the controller this turn, replacing it with each opponent discarding a card.
 */
@SerialName("ReplaceNextDrawWithDiscard")
@Serializable
data object ReplaceNextDrawWithDiscardEffect : Effect {
    override val description: String =
        "The next time you would draw a card this turn, each opponent discards a card instead"
}

/**
 * Replace next draw with damage effect.
 * "{1}: The next time you would draw a card this turn, this enchantment deals 2 damage to any target instead."
 * Used for Words of War and similar "Words of" cycle cards.
 *
 * This creates a floating replacement effect shield that intercepts the next card draw
 * for the controller this turn, replacing it with dealing damage to the chosen target.
 * The target is selected at activation time and stored in the shield.
 *
 * @property damageAmount The amount of damage dealt instead of drawing
 */
@SerialName("ReplaceNextDrawWithDamage")
@Serializable
data class ReplaceNextDrawWithDamageEffect(
    val damageAmount: Int
) : Effect {
    override val description: String =
        "The next time you would draw a card this turn, deal $damageAmount damage to any target instead"
}

/**
 * Replace next draw with token creation effect.
 * "{1}: The next time you would draw a card this turn, create a 2/2 green Bear creature token instead."
 * Used for Words of Wilding and similar "Words of" cycle cards.
 *
 * This creates a floating replacement effect shield that intercepts the next card draw
 * for the controller this turn, replacing it with token creation.
 */
@SerialName("ReplaceNextDrawWithBearToken")
@Serializable
data object ReplaceNextDrawWithBearTokenEffect : Effect {
    override val description: String =
        "The next time you would draw a card this turn, create a 2/2 green Bear creature token instead"
}

/**
 * Draw X cards, then for each card drawn this way, discard a card unless you sacrifice a permanent.
 * Used for Read the Runes.
 */
@SerialName("ReadTheRunes")
@Serializable
data object ReadTheRunesEffect : Effect {
    override val description: String =
        "Draw X cards. For each card drawn this way, discard a card unless you sacrifice a permanent."
}

/**
 * Reveal a player's hand (publicly visible to all players).
 * This is an atomic effect that just reveals - use with CompositeEffect for
 * "reveal and do something based on what's revealed" patterns.
 *
 * Example: Baleful Stare = CompositeEffect(RevealHandEffect, DrawCardsEffect(count))
 */
@SerialName("RevealHand")
@Serializable
data class RevealHandEffect(
    val target: EffectTarget = EffectTarget.ContextTarget(0)
) : Effect {
    override val description: String = when (target) {
        EffectTarget.Controller -> "Reveal your hand"
        else -> "${target.description.replaceFirstChar { it.uppercase() }} reveals their hand"
    }
}
