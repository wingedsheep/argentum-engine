package com.wingedsheep.sdk.scripting.effects

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.text.TextReplacer
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
    override fun applyTextReplacement(replacer: TextReplacer): Effect = this
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
    override fun applyTextReplacement(replacer: TextReplacer): Effect = this
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
    override fun applyTextReplacement(replacer: TextReplacer): Effect = this
}

/**
 * Mark target creature so that when it dies this turn, its controller's graveyard is exiled.
 * "When that creature dies this turn, exile its controller's graveyard."
 *
 * Creates a floating effect marker on the target. When the creature dies (via SBA or destruction),
 * all cards in the controller's graveyard (including the dying creature itself) are exiled.
 * If the creature doesn't die this turn, the marker expires at end of turn.
 *
 * Only applies to creatures — if the target is a player, this effect does nothing.
 */
@SerialName("MarkExileControllerGraveyardOnDeath")
@Serializable
data class MarkExileControllerGraveyardOnDeathEffect(
    val target: EffectTarget
) : Effect {
    override val description: String = "When ${target.description} dies this turn, exile its controller's graveyard"
    override fun applyTextReplacement(replacer: TextReplacer): Effect = this
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
    override fun applyTextReplacement(replacer: TextReplacer): Effect = this
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
    override fun applyTextReplacement(replacer: TextReplacer): Effect {
        val newFilter = filter.applyTextReplacement(replacer)
        return if (newFilter !== filter) copy(filter = newFilter) else this
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
    override fun applyTextReplacement(replacer: TextReplacer): Effect = this
}

/**
 * Sacrifice a specific permanent identified by target.
 * "Sacrifice it" — used in delayed triggers where the exact permanent to sacrifice
 * was determined at ability resolution time (e.g., Skirk Alarmist's delayed sacrifice).
 *
 * @property target The specific permanent to sacrifice
 */
@SerialName("SacrificeTarget")
@Serializable
data class SacrificeTargetEffect(
    val target: EffectTarget = EffectTarget.ContextTarget(0)
) : Effect {
    override val description: String = "sacrifice ${target.description}"
    override fun applyTextReplacement(replacer: TextReplacer): Effect = this
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
    override fun applyTextReplacement(replacer: TextReplacer): Effect {
        val newFilter = filter.applyTextReplacement(replacer)
        return if (newFilter !== filter) copy(filter = newFilter) else this
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
    override fun applyTextReplacement(replacer: TextReplacer): Effect = this
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
    override fun applyTextReplacement(replacer: TextReplacer): Effect = this
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
    override fun applyTextReplacement(replacer: TextReplacer): Effect = this
}

/**
 * Sacrifice target permanent at end of combat.
 * Marks the permanent for sacrifice when the end of combat step begins.
 * Used by Mardu Blazebringer and similar creatures that sacrifice themselves
 * after attacking or blocking.
 */
@SerialName("SacrificeAtEndOfCombat")
@Serializable
data class SacrificeAtEndOfCombatEffect(
    val target: EffectTarget
) : Effect {
    override val description: String = "Sacrifice ${target.description} at end of combat"
    override fun applyTextReplacement(replacer: TextReplacer): Effect = this
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
@Deprecated("Use Effects.DestroyAll() or EffectPatterns.destroyAllPipeline() instead")
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
    override fun applyTextReplacement(replacer: TextReplacer): Effect {
        val newFilter = filter.applyTextReplacement(replacer)
        return if (newFilter !== filter) copy(filter = newFilter) else this
    }
}

/**
 * Return all permanents matching the filter to their owners' hands.
 * Uses GroupFilter to support excludeSelf and any GameObjectFilter predicates.
 */
@Deprecated("Use Effects.ReturnAllToHand() or EffectPatterns.returnAllToHand() instead")
@SerialName("ReturnAllToHand")
@Serializable
data class ReturnAllToHandEffect(
    val filter: GroupFilter
) : Effect {
    override val description: String = "Return ${filter.description} to their owners' hands"
    override fun applyTextReplacement(replacer: TextReplacer): Effect {
        val newFilter = filter.applyTextReplacement(replacer)
        return if (newFilter !== filter) copy(filter = newFilter) else this
    }
}

@SerialName("MoveToZone")
@Serializable
data class MoveToZoneEffect(
    val target: EffectTarget,
    val destination: Zone,
    val placement: ZonePlacement = ZonePlacement.Default,
    val byDestruction: Boolean = false,
    /** When set, the card enters the battlefield under this player's control instead of the owner's. */
    val controllerOverride: EffectTarget? = null,
    /** When set, the move is skipped if the target is not currently in this zone. */
    val fromZone: Zone? = null
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
    override fun applyTextReplacement(replacer: TextReplacer): Effect = this
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
    override fun applyTextReplacement(replacer: TextReplacer): Effect = this
}

/**
 * Exile all permanents matching a filter that the controller controls, and link them
 * to the source permanent. The exiled entity IDs are stored on the source as a
 * LinkedExileComponent, and the count is stored in the effect context as a collection
 * named [storeAs] (use VariableReference("{storeAs}_count") for the count).
 *
 * Used for Day of the Dragons-style effects where permanents are exiled and later
 * returned when the source leaves the battlefield.
 *
 * @property filter Which permanents to exile (matched against projected state)
 * @property storeAs Collection name for storing exiled IDs (default "linked_exile")
 */
@SerialName("ExileGroupAndLink")
@Serializable
data class ExileGroupAndLinkEffect(
    val filter: GroupFilter,
    val storeAs: String = "linked_exile"
) : Effect {
    override val description: String =
        "Exile all ${filter.description} and link them to this permanent"
    override fun applyTextReplacement(replacer: TextReplacer): Effect {
        val newFilter = filter.applyTextReplacement(replacer)
        return if (newFilter !== filter) copy(filter = newFilter) else this
    }
}

/**
 * Return all cards linked to the source permanent (via LinkedExileComponent) to the
 * battlefield under the controller's control.
 *
 * Used for the leaves-the-battlefield half of Day of the Dragons-style effects.
 * Reads the LinkedExileComponent from the source entity (which may be in the graveyard
 * or exile at this point) and moves each linked card from exile to the battlefield.
 */
@SerialName("ReturnLinkedExile")
@Serializable
data class ReturnLinkedExileEffect(
    val underOwnersControl: Boolean = false
) : Effect {
    override val description: String =
        if (underOwnersControl) "Return the exiled cards to the battlefield under their owners' control"
        else "Return the exiled cards to the battlefield under your control"
    override fun applyTextReplacement(replacer: TextReplacer): Effect = this
}

/**
 * Return one card from the source's linked exile (LinkedExileComponent) to the
 * battlefield. The active player (whose upkeep it is) chooses one of their owned
 * cards from the linked exile and returns it to the battlefield.
 *
 * If no eligible cards remain for this player, does nothing.
 * If no cards remain in the linked exile at all, removes the global triggered ability.
 *
 * Reusable for any effect that exiles cards, links them to a source, and gradually
 * returns them.
 */
@SerialName("ReturnOneFromLinkedExile")
@Serializable
data object ReturnOneFromLinkedExileEffect : Effect {
    override val description: String =
        "Return one of the exiled cards you own to the battlefield"
    override fun applyTextReplacement(replacer: TextReplacer): Effect = this
}
