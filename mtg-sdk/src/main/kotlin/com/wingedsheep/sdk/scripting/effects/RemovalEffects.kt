package com.wingedsheep.sdk.scripting

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import kotlinx.serialization.Serializable

// =============================================================================
// Removal Effects
// =============================================================================

/**
 * Mark target as unable to regenerate.
 * "It can't be regenerated."
 *
 * Designed to be used AFTER a destroy effect via .then() for cards like Smother.
 * The target may be in the graveyard when this effect resolves.
 * When regeneration is implemented, this will mark the entity to prevent
 * regeneration effects from returning it to the battlefield.
 *
 * Example: MoveToZoneEffect(target, Zone.Graveyard, byDestruction = true) then CantBeRegeneratedEffect(target)
 */
@Serializable
data class CantBeRegeneratedEffect(
    val target: EffectTarget
) : Effect {
    override val description: String = "${target.description} can't be regenerated"
}

/**
 * Destroy all permanents matching a filter.
 * Unified effect that handles all "destroy all X" patterns.
 *
 * Examples:
 * - DestroyAllEffect(GroupFilter.AllLands) -> "Destroy all lands"
 * - DestroyAllEffect(GroupFilter.AllCreatures, noRegenerate = true) -> Wrath of God
 * - DestroyAllEffect(GroupFilter(GOF.Creature.withColor(Color.WHITE))) -> Virtue's Ruin
 * - DestroyAllEffect(GroupFilter(GOF.Land.withSubtype(Subtype.ISLAND))) -> Boiling Seas
 *
 * @param filter Which permanents to destroy (defaults to AllPermanents)
 * @param noRegenerate If true, destroyed permanents can't be regenerated (for future regeneration support)
 */
@Serializable
data class DestroyAllEffect(
    val filter: GroupFilter = GroupFilter.AllPermanents,
    val noRegenerate: Boolean = false
) : Effect {
    override val description: String = buildString {
        append("Destroy ")
        append(filter.description)
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
    val target: EffectTarget = EffectTarget.PlayerRef(Player.TargetOpponent)
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
    val target: EffectTarget = EffectTarget.PlayerRef(Player.TargetPlayer)
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
 * Unified zone-moving effect.
 * Consolidates destroy, exile, bounce, shuffle-into-library, put-on-top, etc.
 *
 * @property target The entity to move
 * @property destination The destination zone
 * @property placement How to place the card in the destination zone
 * @property byDestruction If true, use destruction semantics (indestructible check)
 */
@Serializable
data class MoveToZoneEffect(
    val target: EffectTarget,
    val destination: Zone,
    val placement: ZonePlacement = ZonePlacement.Default,
    val byDestruction: Boolean = false
) : Effect {
    override val description: String = buildString {
        when {
            byDestruction -> append("Destroy ${target.description}")
            destination == Zone.Hand -> append("Return ${target.description} to its owner's hand")
            destination == Zone.Exile -> append("Exile ${target.description}")
            destination == Zone.Library && placement == ZonePlacement.Shuffled ->
                append("Shuffle ${target.description} into its owner's library")
            destination == Zone.Library && placement == ZonePlacement.Top ->
                append("Put ${target.description} on top of its owner's library")
            destination == Zone.Battlefield && placement == ZonePlacement.Tapped ->
                append("Put ${target.description} onto the battlefield tapped")
            destination == Zone.Battlefield ->
                append("Put ${target.description} onto the battlefield")
            else -> append("Put ${target.description} into ${destination.description}")
        }
    }
}
