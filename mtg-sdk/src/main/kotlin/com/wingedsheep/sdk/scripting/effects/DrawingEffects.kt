package com.wingedsheep.sdk.scripting.effects

import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount
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
 * Each player returns a permanent they control to its owner's hand.
 * Used as a replacement effect for Words of Wind:
 * "each player returns a permanent they control to its owner's hand instead"
 */
@SerialName("EachPlayerReturnsPermanentToHand")
@Serializable
data object EachPlayerReturnsPermanentToHandEffect : Effect {
    override val description: String =
        "each player returns a permanent they control to its owner's hand"
}

/**
 * Generic draw-replacement effect for the "Words of" cycle.
 *
 * Wraps any atomic effect as a draw replacement shield:
 * "The next time you would draw a card this turn, [replacementEffect] instead."
 *
 * Examples:
 * ```kotlin
 * // Words of Worship: gain 5 life
 * ReplaceNextDrawWithEffect(replacementEffect = Effects.GainLife(5))
 *
 * // Words of Wind: each player returns a permanent to hand
 * ReplaceNextDrawWithEffect(replacementEffect = Effects.EachPlayerReturnPermanentToHand())
 *
 * // Words of Waste: each opponent discards a card
 * ReplaceNextDrawWithEffect(replacementEffect = Effects.EachOpponentDiscards(1))
 *
 * // Words of War: deal 2 damage to any target
 * ReplaceNextDrawWithEffect(replacementEffect = Effects.DealDamage(2, EffectTarget.ContextTarget(0)))
 *
 * // Words of Wilding: create a 2/2 green Bear token
 * ReplaceNextDrawWithEffect(replacementEffect = Effects.CreateToken(2, 2, setOf(Color.GREEN), setOf("Bear")))
 * ```
 *
 * @property replacementEffect The effect executed instead of the draw
 */
@SerialName("ReplaceNextDrawWith")
@Serializable
data class ReplaceNextDrawWithEffect(
    val replacementEffect: Effect
) : Effect {
    override val description: String =
        "The next time you would draw a card this turn, ${replacementEffect.description} instead"
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
 * Trade Secrets effect.
 * "Target opponent draws two cards, then you draw up to four cards.
 * That opponent may repeat this process as many times as they choose."
 */
@SerialName("TradeSecrets")
@Serializable
data object TradeSecretsEffect : Effect {
    override val description: String =
        "Target opponent draws two cards, then you draw up to four cards. That opponent may repeat this process as many times as they choose."
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
