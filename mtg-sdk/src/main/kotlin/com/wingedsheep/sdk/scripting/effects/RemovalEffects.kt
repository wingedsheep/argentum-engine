package com.wingedsheep.sdk.scripting.effects

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// =============================================================================
// Removal Effects
// =============================================================================

/**
 * Regenerate target creature.
 * "Regenerate [permanent]" creates a one-shot shield that expires at end of turn.
 * The next time that permanent would be destroyed this turn, instead:
 * tap it, remove all damage from it, and remove it from combat.
 */
@SerialName("Regenerate")
@Serializable
data class RegenerateEffect(
    val target: EffectTarget
) : Effect {
    override val description: String = "Regenerate ${target.description}"
}

/**
 * Mark target as unable to regenerate.
 * "It can't be regenerated."
 *
 * Designed to be used BEFORE a destroy effect via .then() for cards like Smother.
 * Places a floating effect that prevents regeneration shields from being used.
 */
@SerialName("CantBeRegenerated")
@Serializable
data class CantBeRegeneratedEffect(
    val target: EffectTarget
) : Effect {
    override val description: String = "${target.description} can't be regenerated"
}

/**
 * Mark target creature so that if it would die this turn, it goes to exile instead of graveyard.
 * "If it would die this turn, exile it instead."
 *
 * Designed to be composed with damage/destroy effects via .then() for cards like Carbonize.
 * Only applies to creatures — if the target is a player, this effect does nothing.
 */
@SerialName("MarkExileOnDeath")
@Serializable
data class MarkExileOnDeathEffect(
    val target: EffectTarget
) : Effect {
    override val description: String = "If ${target.description} would die this turn, exile it instead"
}

/**
 * Destroy all creatures that share a creature type with the sacrificed creature.
 * Used for Endemic Plague and similar effects.
 *
 * Requires that a creature was sacrificed as an additional cost (via context.sacrificedPermanents).
 * Looks up the sacrificed creature's subtypes, then destroys all creatures on the battlefield
 * that share at least one creature type.
 *
 * @param noRegenerate If true, destroyed creatures can't be regenerated
 */
@SerialName("DestroyAllSharingTypeWithSacrificed")
@Serializable
data class DestroyAllSharingTypeWithSacrificedEffect(
    val noRegenerate: Boolean = true
) : Effect {
    override val description: String = buildString {
        append("Destroy all creatures that share a creature type with the sacrificed creature")
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
@SerialName("Sacrifice")
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
@SerialName("SacrificeSelf")
@Serializable
data object SacrificeSelfEffect : Effect {
    override val description: String = "sacrifice this permanent"
}

/**
 * Force sacrifice effect: Target player sacrifices permanents matching a filter.
 * "Target player sacrifices a creature" (Edict effects)
 */
@SerialName("ForceSacrifice")
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
@SerialName("SeparatePermanentsIntoPiles")
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
@SerialName("ExileUntilLeaves")
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
@SerialName("ExileAndReplaceWithToken")
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
 * Destroy target creature at end of combat.
 * Creates a delayed destruction that happens when the end of combat step begins.
 * Used by Serpentine Basilisk and similar "basilisk" abilities.
 *
 * The target is typically the creature that was dealt combat damage,
 * resolved from the trigger context (triggeringEntityId).
 */
@SerialName("DestroyAtEndOfCombat")
@Serializable
data class DestroyAtEndOfCombatEffect(
    val target: EffectTarget
) : Effect {
    override val description: String = "Destroy ${target.description} at end of combat"
}

/**
 * Destroy all permanents matching a filter.
 *
 * Optionally excludes permanents that have any subtype matching a stored list of strings
 * (from storedStringLists in the effect context).
 *
 * @property filter Which permanents to destroy (e.g., GameObjectFilter.Creature)
 * @property canRegenerate Whether destroyed permanents can be regenerated
 * @property exceptSubtypesFromStored Optional key into storedStringLists; if set, skip
 *   permanents that have any subtype matching any string in that stored list
 * @property storeDestroyedAs If set, stores the IDs of actually destroyed permanents in
 *   updatedCollections under this key. Useful for "draw a card for each creature destroyed
 *   this way" patterns — compose with DrawCardsEffect(VariableReference("<key>_count")).
 */
@SerialName("DestroyAll")
@Serializable
data class DestroyAllEffect(
    val filter: GameObjectFilter,
    val canRegenerate: Boolean = true,
    val exceptSubtypesFromStored: String? = null,
    val storeDestroyedAs: String? = null
) : Effect {
    override val description: String = buildString {
        append("Destroy all ${filter.description}")
        if (!canRegenerate) append(". They can't be regenerated")
    }
}

/**
 * Each player chooses a creature type. Each player returns all creature cards of a type chosen
 * this way from their graveyard to the battlefield.
 *
 * Used for Patriarch's Bidding. Each player (in APNAP order) chooses a creature type.
 * After all choices, all creature cards matching any chosen type are returned from all graveyards
 * to the battlefield under their owner's control.
 */
@SerialName("PatriarchsBidding")
@Serializable
data object PatriarchsBiddingEffect : Effect {
    override val description: String =
        "Each player chooses a creature type. Each player returns all creature cards of a type chosen this way from their graveyard to the battlefield"
}

@SerialName("MoveToZone")
@Serializable
data class MoveToZoneEffect(
    val target: EffectTarget,
    val destination: Zone,
    val placement: ZonePlacement = ZonePlacement.Default,
    val byDestruction: Boolean = false,
    /** When set, the card enters the battlefield under this player's control instead of the owner's. */
    val controllerOverride: EffectTarget? = null
) : Effect {
    override val description: String = buildString {
        when {
            byDestruction -> append("Destroy ${target.description}")
            destination == Zone.HAND -> append("Return ${target.description} to its owner's hand")
            destination == Zone.EXILE -> append("Exile ${target.description}")
            destination == Zone.LIBRARY && placement == ZonePlacement.Shuffled ->
                append("Shuffle ${target.description} into its owner's library")
            destination == Zone.LIBRARY && placement == ZonePlacement.Top ->
                append("Put ${target.description} on top of its owner's library")
            destination == Zone.BATTLEFIELD && controllerOverride != null ->
                append("Put ${target.description} onto the battlefield under your control")
            destination == Zone.BATTLEFIELD && placement == ZonePlacement.Tapped ->
                append("Put ${target.description} onto the battlefield tapped")
            destination == Zone.BATTLEFIELD ->
                append("Put ${target.description} onto the battlefield")
            else -> append("Put ${target.description} into ${destination.displayName}")
        }
    }
}

/**
 * Returns the source permanent (typically an Aura) from its current zone to the battlefield
 * attached to the specified target. Used by the Dragon aura cycle (Dragon Shadow, Dragon Breath, etc.)
 * which return from the graveyard when a creature with high mana value enters the battlefield.
 *
 * The [target] specifies what the aura attaches to (typically [EffectTarget.TriggeringEntity]).
 */
@SerialName("ReturnSelfToBattlefieldAttached")
@Serializable
data class ReturnSelfToBattlefieldAttachedEffect(
    val target: EffectTarget = EffectTarget.TriggeringEntity
) : Effect {
    override val description: String =
        "Return this card from your graveyard to the battlefield attached to ${target.description}"
}
