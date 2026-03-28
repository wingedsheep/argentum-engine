package com.wingedsheep.sdk.scripting.effects

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.text.TextReplacer
import com.wingedsheep.sdk.scripting.values.DynamicAmount
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
    val any: Boolean = false,
    /** When true, the source permanent is excluded from valid sacrifice choices. */
    val excludeSource: Boolean = false
) : Effect {
    override val description: String = buildString {
        append("sacrifice ")
        when {
            any -> append("any number of ${filter.description}s")
            count == 1 -> {
                if (excludeSource) append("another ") else append("a ")
                append(filter.description)
            }
            else -> {
                append("$count ")
                if (excludeSource) append("other ")
                append("${filter.description}s")
            }
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
 * The type of delayed action to perform at end of combat.
 */
@Serializable
enum class DelayedAction {
    DESTROY,
    SACRIFICE
}

/**
 * Mark a permanent for a delayed action at end of combat.
 *
 * When executed, adds a marker component to the target permanent. The TurnManager
 * processes these markers when the END_COMBAT step begins:
 * - [DelayedAction.DESTROY] adds MarkedForDestructionAtEndOfCombatComponent
 * - [DelayedAction.SACRIFICE] adds MarkedForSacrificeAtEndOfCombatComponent
 *
 * Used by Serpentine Basilisk ("destroy that creature at end of combat")
 * and Mardu Blazebringer ("sacrifice it at end of combat").
 *
 * @property target The permanent to mark
 * @property action Whether to destroy or sacrifice the permanent
 */
@SerialName("MarkForDelayedAction")
@Serializable
data class MarkForDelayedActionEffect(
    val target: EffectTarget,
    val action: DelayedAction
) : Effect {
    override val description: String = when (action) {
        DelayedAction.DESTROY -> "Destroy ${target.description} at end of combat"
        DelayedAction.SACRIFICE -> "Sacrifice ${target.description} at end of combat"
    }
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
/**
 * Destroy all Equipment attached to the target permanent.
 * Used for Corrosive Ooze's delayed trigger at end of combat.
 */
@SerialName("DestroyAllEquipmentOnTarget")
@Serializable
data class DestroyAllEquipmentOnTargetEffect(
    val target: EffectTarget
) : Effect {
    override val description: String = "Destroy all Equipment attached to ${target.description}"
    override fun applyTextReplacement(replacer: TextReplacer): Effect = this
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
    val fromZone: Zone? = null,
    /** When true and destination is BATTLEFIELD, the card enters face down (as a 2/2 morph creature). */
    val faceDown: Boolean = false,
    /** When true and destination is EXILE, the exiled card is linked to the source permanent via LinkedExileComponent. */
    val linkToSource: Boolean = false,
    /**
     * When set and destination is LIBRARY, places the card at this position from the top (0-indexed).
     * 0 = top, 1 = second from top, 2 = third from top, etc.
     * Takes precedence over [placement] when destination is LIBRARY.
     */
    val positionFromTop: Int? = null
) : Effect {
    override val description: String = buildString {
        when {
            byDestruction -> append("Destroy ${target.description}")
            destination == Zone.HAND -> append("Return ${target.description} to its owner's hand")
            destination == Zone.EXILE -> append("Exile ${target.description}")
            destination == Zone.LIBRARY && positionFromTop != null -> {
                val ordinal = when (positionFromTop) {
                    0 -> "top"
                    1 -> "second from the top"
                    2 -> "third from the top"
                    else -> "${positionFromTop + 1}th from the top"
                }
                append("Put ${target.description} into its owner's library $ordinal")
            }
            destination == Zone.LIBRARY && placement == ZonePlacement.Shuffled ->
                append("Shuffle ${target.description} into its owner's library")
            destination == Zone.LIBRARY && placement == ZonePlacement.Top ->
                append("Put ${target.description} on top of its owner's library")
            destination == Zone.BATTLEFIELD && faceDown ->
                append("Put ${target.description} onto the battlefield face down")
            destination == Zone.BATTLEFIELD && controllerOverride != null ->
                append("Put ${target.description} onto the battlefield under your control")
            destination == Zone.BATTLEFIELD && placement == ZonePlacement.TappedAndAttacking ->
                append("Put ${target.description} onto the battlefield tapped and attacking")
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

/**
 * Return to owner's hand all creature cards in a player's graveyard that were put there
 * from anywhere this turn.
 *
 * Uses GraveyardEntryTurnComponent (stamped by GameState.addToZone) to determine which
 * cards entered the graveyard during the current turn.
 *
 * Reusable for any "return creatures put into your graveyard this turn" effect.
 *
 * @property player Which player's graveyard to check (defaults to Controller)
 */
@SerialName("ReturnCreaturesPutInGraveyardThisTurn")
@Serializable
data class ReturnCreaturesPutInGraveyardThisTurnEffect(
    val player: Player = Player.You
) : Effect {
    override val description: String =
        "Return to your hand all creature cards in your graveyard that were put there from anywhere this turn"
    override fun applyTextReplacement(replacer: TextReplacer): Effect = this
}

/**
 * Exile all cards in each opponent's graveyard.
 * Used for Phyrexian Scriptures Chapter III and similar graveyard hate effects.
 */
@SerialName("ExileOpponentsGraveyards")
@Serializable
data object ExileOpponentsGraveyardsEffect : Effect {
    override val description: String = "Exile all opponents' graveyards"
    override fun applyTextReplacement(replacer: TextReplacer): Effect = this
}

/**
 * Force a player to exile cards from multiple zones (battlefield, hand, graveyard).
 * The player chooses which to exile from any combination of those zones.
 *
 * Used for Lich's Mastery: "for each 1 life you lost, exile a permanent you control
 * or a card from your hand or graveyard."
 *
 * If the total available is less than [count], the player exiles everything they can.
 *
 * @property count Number of things to exile (can be dynamic, e.g., life lost amount)
 * @property target The player who must exile (defaults to controller)
 */
@SerialName("ForceExileMultiZone")
@Serializable
data class ForceExileMultiZoneEffect(
    val count: DynamicAmount,
    val target: EffectTarget = EffectTarget.Controller
) : Effect {
    override val description: String =
        "Exile ${count.description} permanents you control or cards from your hand or graveyard"
    override fun applyTextReplacement(replacer: TextReplacer): Effect = this
}

/**
 * Exile a warped permanent and mark it as re-castable via warp from exile.
 * Used by the warp mechanic's delayed trigger: "At the beginning of the next end step,
 * exile this permanent." The exiled card retains its warp ability and can be cast
 * from exile for its warp cost on a later turn.
 *
 * @property target The permanent to exile (resolved to SpecificEntity by delayed trigger creation)
 */
@SerialName("WarpExile")
@Serializable
data class WarpExileEffect(
    val target: EffectTarget
) : Effect {
    override val description: String = "Exile ${target.description} (warp)"
    override fun applyTextReplacement(replacer: TextReplacer): Effect = this
}
