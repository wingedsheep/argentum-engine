package com.wingedsheep.sdk.scripting.effects

import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.text.TextReplacer
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

    override fun runtimeDescription(resolver: (DynamicAmount) -> Int): String {
        val resolved = resolver(count)
        val cardText = if (resolved == 1) "a card" else "$resolved cards"
        return when (target) {
            EffectTarget.Controller -> "Draw $cardText"
            else -> "${target.description.replaceFirstChar { it.uppercase() }} draws $cardText"
        }
    }

    override fun applyTextReplacement(replacer: TextReplacer): Effect {
        val newCount = count.applyTextReplacement(replacer)
        return if (newCount !== count) copy(count = newCount) else this
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

    override fun applyTextReplacement(replacer: TextReplacer): Effect = this
}

/**
 * Scope for looking at face-down creatures.
 */
@Serializable
enum class FaceDownLookScope {
    /** Look at a single target face-down creature. */
    SINGLE_TARGET,
    /** Look at all face-down creatures controlled by the target player. */
    ALL_CONTROLLED_BY_TARGET_PLAYER
}

/**
 * Look at face-down creature(s).
 *
 * When [scope] is [FaceDownLookScope.SINGLE_TARGET], looks at the single face-down
 * creature identified by [target]. When [scope] is [FaceDownLookScope.ALL_CONTROLLED_BY_TARGET_PLAYER],
 * looks at all face-down creatures controlled by the player identified by [target].
 *
 * In both cases, marks the face-down creature(s) as revealed to the controller of the ability.
 *
 * @property target The creature (SINGLE_TARGET) or player (ALL_CONTROLLED_BY_TARGET_PLAYER)
 * @property scope Whether to look at one creature or all of a player's face-down creatures
 */
@SerialName("LookAtFaceDown")
@Serializable
data class LookAtFaceDownEffect(
    val target: EffectTarget,
    val scope: FaceDownLookScope
) : Effect {
    override val description: String = when (scope) {
        FaceDownLookScope.SINGLE_TARGET -> "Look at target face-down creature"
        FaceDownLookScope.ALL_CONTROLLED_BY_TARGET_PLAYER ->
            "Look at any face-down creatures ${target.description} controls"
    }

    override fun applyTextReplacement(replacer: TextReplacer): Effect = this
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

    override fun applyTextReplacement(replacer: TextReplacer): Effect = this
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

    override fun applyTextReplacement(replacer: TextReplacer): Effect = this
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

    override fun applyTextReplacement(replacer: TextReplacer): Effect {
        val newReplacement = replacementEffect.applyTextReplacement(replacer)
        return if (newReplacement !== replacementEffect) copy(replacementEffect = newReplacement) else this
    }
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

    override fun applyTextReplacement(replacer: TextReplacer): Effect = this
}

/**
 * Draw up to N cards effect.
 * "Draw up to N cards" - the player chooses how many (0 to maxCards).
 *
 * @property maxCards Maximum number of cards the player may draw
 * @property target The player who draws
 * @property storeNotDrawnAs If set, stores the number of cards NOT drawn (maxCards - chosen)
 *   as a named numeric variable in the effect context for subsequent pipeline effects
 */
@SerialName("DrawUpTo")
@Serializable
data class DrawUpToEffect(
    val maxCards: Int,
    val target: EffectTarget = EffectTarget.Controller,
    val storeNotDrawnAs: String? = null
) : Effect {
    override val description: String = buildString {
        when (target) {
            EffectTarget.Controller -> append("Draw up to $maxCards cards")
            else -> append("${target.description.replaceFirstChar { it.uppercase() }} draws up to $maxCards cards")
        }
    }

    override fun applyTextReplacement(replacer: TextReplacer): Effect = this
}

/**
 * Each player draws cards equal to the damage they dealt this turn (via sources they
 * controlled) to the trigger's source. Reads the per-player damage map captured on the
 * trigger context at leave-battlefield time. Used for Grothama, All-Devouring's
 * LTB ability.
 */
@SerialName("EachPlayerDrawsForDamageDealtToSource")
@Serializable
data object EachPlayerDrawsForDamageDealtToSourceEffect : Effect {
    override val description: String =
        "Each player draws cards equal to the amount of damage dealt to this creature this turn by sources they controlled"

    override fun applyTextReplacement(replacer: TextReplacer): Effect = this
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

    override fun applyTextReplacement(replacer: TextReplacer): Effect = this
}

