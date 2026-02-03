package com.wingedsheep.sdk.scripting

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.targeting.PermanentTargetFilter
import kotlinx.serialization.Serializable

// =============================================================================
// Removal Effects
// =============================================================================

/**
 * Destroy target creature/permanent effect.
 * "Destroy target creature"
 */
@Serializable
data class DestroyEffect(
    val target: EffectTarget
) : Effect {
    override val description: String = "Destroy ${target.description}"
}

/**
 * Mark target as unable to regenerate.
 * "It can't be regenerated."
 *
 * Designed to be used AFTER DestroyEffect via .then() for cards like Smother.
 * The target may be in the graveyard when this effect resolves.
 * When regeneration is implemented, this will mark the entity to prevent
 * regeneration effects from returning it to the battlefield.
 *
 * Example: DestroyEffect(target) then CantBeRegeneratedEffect(target)
 */
@Serializable
data class CantBeRegeneratedEffect(
    val target: EffectTarget
) : Effect {
    override val description: String = "${target.description} can't be regenerated"
}

/**
 * Exile target effect.
 * "Exile target creature/permanent"
 */
@Serializable
data class ExileEffect(
    val target: EffectTarget
) : Effect {
    override val description: String = "Exile ${target.description}"
}

/**
 * Return to hand effect.
 * "Return target creature to its owner's hand"
 */
@Serializable
data class ReturnToHandEffect(
    val target: EffectTarget
) : Effect {
    override val description: String = "Return ${target.description} to its owner's hand"
}

/**
 * Destroy all permanents matching a filter.
 * Unified effect that handles all "destroy all X" patterns.
 *
 * Examples:
 * - DestroyAllEffect(PermanentTargetFilter.Land) -> "Destroy all lands"
 * - DestroyAllEffect(PermanentTargetFilter.Creature, noRegenerate = true) -> Wrath of God
 * - DestroyAllEffect(PermanentTargetFilter.And(listOf(Creature, WithColor(WHITE)))) -> Virtue's Ruin
 * - DestroyAllEffect(PermanentTargetFilter.And(listOf(Land, WithSubtype(ISLAND)))) -> Boiling Seas
 *
 * @param filter Which permanents to destroy (defaults to Any = all permanents)
 * @param noRegenerate If true, destroyed permanents can't be regenerated (for future regeneration support)
 */
@Serializable
data class DestroyAllEffect(
    val filter: PermanentTargetFilter = PermanentTargetFilter.Any,
    val noRegenerate: Boolean = false
) : Effect {
    override val description: String = buildString {
        append("Destroy all ")
        when (filter) {
            is PermanentTargetFilter.Any -> append("permanents")
            is PermanentTargetFilter.Creature -> append("creatures")
            is PermanentTargetFilter.Land -> append("lands")
            is PermanentTargetFilter.CreatureOrLand -> append("creatures and lands")
            else -> append(filter.description).append("s")
        }
        if (noRegenerate) append(". They can't be regenerated")
    }
}

/**
 * Sacrifice permanents effect.
 * Can be used as a cost or standalone effect.
 *
 * @property filter Which permanents can be sacrificed
 * @property count How many to sacrifice
 * @property any If true, "any number" (for Scapeshift)
 */
@Serializable
data class SacrificeEffect(
    val filter: GameObjectFilter,
    val count: Int = 1,
    val any: Boolean = false
) : Effect {
    override val description: String = buildString {
        append("sacrifice ")
        when {
            any -> append("any number of ${filter.description}s")
            count == 1 -> append("a ${filter.description}")
            else -> append("$count ${filter.description}s")
        }
    }
}

/**
 * Sacrifice the source permanent (self).
 * "Sacrifice this creature" or "Sacrifice this permanent"
 *
 * Used primarily as the suffer effect in PayOrSufferEffect for punisher mechanics.
 */
@Serializable
data object SacrificeSelfEffect : Effect {
    override val description: String = "sacrifice this permanent"
}

/**
 * Force sacrifice effect: Target player sacrifices permanents matching a filter.
 * "Target player sacrifices a creature" (Edict effects)
 */
@Serializable
data class ForceSacrificeEffect(
    val filter: GameObjectFilter,
    val count: Int = 1,
    val target: EffectTarget = EffectTarget.Opponent
) : Effect {
    override val description: String = buildString {
        append(target.description)
        append(" sacrifices ")
        if (count == 1) {
            append("a ")
        } else {
            append("$count ")
        }
        append(filter.description)
        if (count != 1) append("s")
    }
}

/**
 * Separate permanents into piles effect.
 * "Separate all permanents target player controls into two piles.
 *  That player sacrifices all permanents in the pile of their choice."
 * Used for Liliana of the Veil's ultimate.
 */
@Serializable
data class SeparatePermanentsIntoPilesEffect(
    val target: EffectTarget = EffectTarget.AnyPlayer
) : Effect {
    override val description: String =
        "Separate all permanents ${target.description} controls into two piles. " +
                "That player sacrifices all permanents in the pile of their choice"
}

/**
 * Exile a permanent until this permanent leaves the battlefield.
 * Used for O-Ring style effects like Liminal Hold.
 */
@Serializable
data class ExileUntilLeavesEffect(
    val target: EffectTarget
) : Effect {
    override val description: String =
        "Exile ${target.description} until this permanent leaves the battlefield"
}

/**
 * Exile a creature and create a token for its controller.
 * Used for effects like Crib Swap: "Exile target creature. Its controller creates a 1/1 token."
 *
 * @property target The creature to exile
 * @property tokenPower Power of the replacement token
 * @property tokenToughness Toughness of the replacement token
 * @property tokenColors Colors of the token (empty for colorless)
 * @property tokenTypes Creature types of the token
 * @property tokenKeywords Keywords the token has
 */
@Serializable
data class ExileAndReplaceWithTokenEffect(
    val target: EffectTarget,
    val tokenPower: Int,
    val tokenToughness: Int,
    val tokenColors: Set<Color> = emptySet(),
    val tokenTypes: Set<String>,
    val tokenKeywords: Set<Keyword> = emptySet()
) : Effect {
    override val description: String = buildString {
        append("Exile ${target.description}. Its controller creates a ")
        append("$tokenPower/$tokenToughness ")
        if (tokenColors.isEmpty()) {
            append("colorless ")
        } else {
            append(tokenColors.joinToString(" and ") { it.displayName.lowercase() })
            append(" ")
        }
        append(tokenTypes.joinToString(" "))
        append(" creature token")
        if (tokenKeywords.isNotEmpty()) {
            append(" with ")
            append(tokenKeywords.joinToString(", ") { it.displayName.lowercase() })
        }
    }
}

/**
 * Return a card from graveyard to another zone.
 * "Return target creature card from your graveyard to your hand"
 */
@Serializable
data class ReturnFromGraveyardEffect(
    val filter: GameObjectFilter = GameObjectFilter.Any,
    val destination: SearchDestination = SearchDestination.HAND
) : Effect {
    override val description: String
        get() = "Return ${filter.description} from your graveyard ${destination.description}"
}
