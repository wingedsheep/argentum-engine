package com.wingedsheep.sdk.scripting.effects

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.selfNounToken
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
}

/**
 * One-shot destruction-replacement shield: the next time the target permanent
 * would be destroyed this turn, remove all damage marked on it instead.
 *
 * Unlike [RegenerateEffect], this does NOT tap the permanent and does NOT remove
 * it from combat — it only swaps "destroyed" for "damage cleared". Used by
 * Pyramids (Arabian Nights), whose second mode protects a target land from the
 * next destruction this turn.
 *
 * Lasts until end of turn; the shield is consumed by the first destruction it
 * intercepts. Coexists with regeneration shields on the same entity — the engine
 * resolves regeneration first, then this shield, matching the order in which
 * existing shields are checked.
 */
@SerialName("RemoveDamageShield")
@Serializable
data class RemoveDamageShieldEffect(
    val target: EffectTarget
) : Effect {
    override val description: String =
        "The next time ${target.description} would be destroyed this turn, " +
            "remove all damage marked on it instead"
}

/**
 * Mark target as unable to regenerate.
 * "It can't be regenerated."
 *
 * Designed to be used BEFORE a destroy effect via `then` for cards like Smother.
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
 * Designed to be composed with damage/destroy effects via `then` for cards like Carbonize.
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
            any -> {
                append("any number of ")
                if (excludeSource) append("other ")
                append("${filter.description}s")
            }
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
}

/**
 * Sacrifice a specific permanent identified by target.
 * "Sacrifice it" — used in delayed triggers where the exact permanent to sacrifice
 * was determined at ability resolution time (e.g., Skirk Alarmist's delayed sacrifice).
 *
 * A permanent can only ever be sacrificed by its own controller, so this always sacrifices the
 * target via its controller; the flag below only selects which of two MTG templates is meant.
 *
 * @property target The specific permanent to sacrifice
 * @property sacrificedByItsController Picks the templating intent. False (default) = the resolving
 *   player is the actor ("you sacrifice it" / "sacrifice the creature you put onto the battlefield"):
 *   if control slipped away before resolution the sacrifice is skipped. True = the target's own
 *   controller is the actor regardless of who resolves the effect ("[that creature]'s controller
 *   sacrifices it", e.g. The Ring's Ring-bearer ability), so it is never skipped on a control
 *   mismatch.
 */
@SerialName("SacrificeTarget")
@Serializable
data class SacrificeTargetEffect(
    val target: EffectTarget = EffectTarget.ContextTarget(0),
    val sacrificedByItsController: Boolean = false
) : Effect, SelfReferentialDescription {
    // `EffectTarget.Self.description` is the legacy "this creature", which is wrong the moment a
    // land or artifact sacrifices itself (Safe Haven's upkeep trigger read "you may sacrifice this
    // creature"). Route Self through the self-noun token so the render layer can say "this land".
    override val descriptionTemplate: String = "sacrifice ${target.selfNounToken}"
    override val description: String get() = defaultResolvedDescription
}

/**
 * Emit an `ExploitedEvent` (CR 702.110b) for each creature the source just sacrificed as its exploit
 * ability resolved. Appended internally by [com.wingedsheep.sdk.dsl.exploit] immediately after the
 * exploit [SacrificeEffect] inside the reflexive's action, so `ExploitedEvent` triggers
 * ([com.wingedsheep.sdk.scripting.EventPattern.ExploitedEvent], e.g. Skull Skaab) fire once per
 * exploited creature.
 *
 * There is no explicit input field: the executor reads the sacrificed creatures' last-known info
 * (id, name, token-ness) from `EffectContext.sacrificedPermanents`, which the composite executor
 * threads in from the preceding [SacrificeEffect]. When the optional sacrifice is *declined*, no
 * creature was sacrificed, so `sacrificedPermanents` is empty and no event is emitted (satisfying
 * CR 702.110a's "may"). The exploiter is always the ability's source.
 *
 * Card authors should not use this directly; it is wired into the `exploit()` helper.
 */
@SerialName("EmitExploitedEvent")
@Serializable
data object EmitExploitedEventEffect : Effect {
    // Intentionally blank: this is an internal reflexive-action tail with no player-facing text.
    override val description: String = ""
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
    val target: EffectTarget = EffectTarget.PlayerRef(Player.TargetOpponent),
    /**
     * When non-null, the number of permanents to sacrifice is evaluated at resolution from
     * this [DynamicAmount] instead of the fixed [count] (e.g. Rush of Dread's "sacrifices half
     * the creatures they control, rounded up" via `Divide(AggregateBattlefield(...), Fixed(2))`).
     * The fixed [count] is kept as the fallback for the common edict ("a creature") case so
     * existing cards and their compiled snapshots are untouched.
     */
    val dynamicCount: DynamicAmount? = null
) : Effect {
    override val description: String = buildString {
        append(target.description)
        // "you sacrifice", not "you sacrifices" — the controller form reads as second person.
        append(if (target.description.equals("you", ignoreCase = true)) " sacrifice " else " sacrifices ")
        if (dynamicCount != null) {
            append(dynamicCount.description)
            append(" ")
            append(filter.description)
            append("s")
        } else {
            if (count == 1) {
                append("a ")
            } else {
                append("$count ")
            }
            append(filter.description)
            if (count != 1) append("s")
        }
    }
    override fun applyTextReplacement(replacer: TextReplacer): Effect {
        val newFilter = filter.applyTextReplacement(replacer)
        val newDynamicCount = dynamicCount?.applyTextReplacement(replacer)
        return if (newFilter !== filter || newDynamicCount !== dynamicCount)
            copy(filter = newFilter, dynamicCount = newDynamicCount) else this
    }
}

/**
 * Exile [target] until this permanent leaves the battlefield, linking the exiled card to the source
 * so a leaves-the-battlefield trigger can return it (pair with
 * [com.wingedsheep.sdk.dsl.Effects.ReturnLinkedExileUnderOwnersControl] or a
 * [ReturnLinkedExileEffect] on the source's leaves trigger).
 *
 * The target is normally a battlefield permanent — O-Ring style (Liminal Hold, Driftgloom Coyote).
 * A **graveyard card** is also a legal target: Savior of Ollenbock exiles "up to one other target
 * creature from the battlefield or creature card from a graveyard", and the executor moves the
 * target to exile from whichever of those two zones it is in. Any other zone is not a legal
 * exile-until-leaves source and is ignored.
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
 * Exile [target] creature and all Auras attached to it, linking every exiled card to the source
 * permanent ([com.wingedsheep.engine.state.components.battlefield.LinkedExileComponent]) and
 * noting the number and kind of counters that were on the creature
 * ([com.wingedsheep.engine.state.components.battlefield.NotedExileComponent]).
 *
 * The state-preserving half of a blink that remembers counters and Auras (Tawnos's Coffin:
 * "Exile target creature and all Auras attached to it. Note the number and kind of counters that
 * were on that creature."). Pair with [ReturnNotedExileTappedWithAurasEffect] on the source's
 * leaves-the-battlefield and becomes-untapped triggers. No-op if the source has already left the
 * battlefield, or the target is gone, when this resolves.
 */
@SerialName("ExileWithAurasNotingCounters")
@Serializable
data class ExileWithAurasNotingCountersEffect(
    val target: EffectTarget = EffectTarget.ContextTarget(0)
) : Effect {
    override val description: String =
        "Exile ${target.description} and all Auras attached to it, noting its counters"
}

/**
 * Return the noted exiled creature (the principal of the source's
 * [com.wingedsheep.engine.state.components.battlefield.NotedExileComponent]) to the battlefield
 * **tapped under its owner's control** with its noted number and kind of counters restored, then
 * return the source's other linked-exiled cards (the creature's Auras) attached to that permanent.
 * Auras that can't legally re-attach are put into their owners' graveyards by the unattached-Aura
 * state-based action (CR 704.5m).
 *
 * The return half of [ExileWithAurasNotingCountersEffect] — Tawnos's Coffin's "When this artifact
 * leaves the battlefield or becomes untapped, return that exiled card …". A no-op when nothing is
 * noted / the principal is no longer in exile (so firing it from both an untap trigger and a leave
 * trigger is safe — whichever fires first returns the cards, the other finds nothing).
 */
@SerialName("ReturnNotedExileTappedWithAuras")
@Serializable
data object ReturnNotedExileTappedWithAurasEffect : Effect {
    override val description: String =
        "Return the exiled card to the battlefield tapped with the noted counters on it, then " +
            "return the exiled Aura cards attached to it"
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
    /**
     * When non-null and destination is BATTLEFIELD, the card enters face down as a 2/2 creature.
     * The [FaceDownMode] selects how it can later be turned face up (morph cost vs. mana cost).
     */
    val faceDown: FaceDownMode? = null,
    /** When true and destination is EXILE, the exiled card is linked to the source permanent via LinkedExileComponent. */
    val linkToSource: Boolean = false,
    /**
     * When set and destination is LIBRARY, places the card at this position from the top (0-indexed).
     * 0 = top, 1 = second from top, 2 = third from top, etc.
     * Takes precedence over [placement] when destination is LIBRARY.
     */
    val positionFromTop: Int? = null,
    /**
     * When non-null, the card enters the battlefield with one counter of this type, honoring
     * counter-placement replacements and prevention. In other destinations this annotates the
     * arriving card — "exile it with a stash counter on it" (Tinybones, Bauble Burglar), "with a dream
     * counter on it" (Goliath Daydreamer). The single-target counterpart of
     * [MoveCollectionEffect.addCounterType]; skipped along with the move when [fromZone] gates it out.
     */
    val addCounterType: CounterType? = null,
    /**
     * When non-null, [addCounterType] is placed only if the card landed in [destination] and matches
     * this filter there, read off *projected* state as it arrives — "if a creature enters this way, it
     * enters with an additional +1/+1 counter on it" (Recommission). Projected, so a noncreature
     * artifact that another effect makes a creature on the battlefield (March of the Machines) still
     * gets the counter, as Recommission's ruling requires. Null places the counter unconditionally.
     */
    val addCounterIf: GameObjectFilter? = null,
    /**
     * What a card put onto the battlefield [ZonePlacement.TappedAndAttacking] attacks. The default,
     * [EnteringAttackDefender.JOIN_SOURCE], is a defending player already in this combat (else the
     * first opponent); [EnteringAttackDefender.RETURNED_ATTACKER] is ninjutsu's "the same player,
     * planeswalker, or battle as the creature that was returned" (CR 702.49c).
     */
    val attackDefender: EnteringAttackDefender = EnteringAttackDefender.JOIN_SOURCE
) : Effect {
    init {
        require(attackDefender != EnteringAttackDefender.CONTROLLER_CHOOSES) {
            "MoveToZoneEffect has no per-card defender prompt; use JOIN_SOURCE or RETURNED_ATTACKER"
        }
        require(attackDefender == EnteringAttackDefender.JOIN_SOURCE || placement == ZonePlacement.TappedAndAttacking) {
            "attackDefender needs placement = TappedAndAttacking"
        }
    }

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
            destination == Zone.BATTLEFIELD && faceDown != null ->
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
}

/**
 * Exile a target and let its owner play it for as long as it remains exiled.
 * If the owner is an opponent of the effect controller, spells cast this way
 * can have an additional generic mana tax.
 *
 * Used for Soul Partition-style effects.
 *
 * @property target The permanent/card to exile
 * @property opponentCostIncrease Generic mana added when an opponent casts the exiled card
 */
@SerialName("ExileAndGrantOwnerPlayPermission")
@Serializable
data class ExileAndGrantOwnerPlayPermissionEffect(
    val target: EffectTarget,
    val opponentCostIncrease: Int = 0
) : Effect {
    override val description: String = buildString {
        append("Exile ${target.description}. For as long as that card remains exiled, its owner may play it")
        if (opponentCostIncrease > 0) {
            append(". A spell cast by an opponent this way costs {$opponentCostIncrease} more to cast")
        }
    }
}

/**
 * Returns the source permanent (typically an Aura) from its current zone to the battlefield
 * attached to the specified target. Used by the Dragon aura cycle (Dragon Shadow, Dragon Breath, etc.)
 * which return from the graveyard when a creature with high mana value enters the battlefield.
 *
 * The [target] specifies what the aura attaches to (typically [EffectTarget.TriggeringEntity]).
 * It may resolve to a **player** as well as a permanent — "attached to target opponent" for a
 * Curse (Radiant Grace). The two cases differ in who ends up controlling the returned Aura: a
 * permanent host hands control to *that permanent's* controller (the Dragon cycle's "attached to
 * that creature" reads as an ordinary Aura following its host), while a player host leaves it
 * under the **ability's** controller, which is the only reading "under your control attached to
 * target opponent" allows.
 *
 * @property transformed Return the card **transformed** — back face up (CR 712.8). A card in a
 *   non-battlefield zone is always front-face-up, so "transformed" can only mean the back face.
 *   Per the standing ruling a single-faced card told to enter transformed doesn't move at all, so
 *   this is a no-op rather than an error on a card with no back face.
 */
@SerialName("ReturnSelfToBattlefieldAttached")
@Serializable
data class ReturnSelfToBattlefieldAttachedEffect(
    val target: EffectTarget = EffectTarget.TriggeringEntity,
    val transformed: Boolean = false,
    /**
     * When set, the Aura enters under this player's control ("under your control") instead of the
     * default: a permanent host's controller, or the ability's controller for a player host.
     */
    val controllerOverride: EffectTarget? = null,
) : Effect {
    override val description: String = buildString {
        append("Return this card from your graveyard to the battlefield")
        if (transformed) append(" transformed")
        if (controllerOverride != null) append(" under your control")
        append(" attached to ${target.description}")
    }
}

/**
 * Put a card onto the battlefield **as an Aura** it isn't printed as: "return it to the
 * battlefield. It's an Aura enchantment with enchant [enchant] and \"…\", and it loses all other
 * abilities" (Harold and Bob, First Numens; Bronzehide Lion).
 *
 * The card enters under the effect controller's control already an Aura — layer 4 sets its card
 * types to enchantment and its subtypes to Aura (supertypes, name and colors stay), layer 6 strips
 * every printed ability, and it has exactly [enchant] as its enchant ability plus the granted
 * abilities. Being an Aura from the moment it enters, it is never a creature on the battlefield:
 * "a creature entered" observers don't see it, and its controller chooses what it enchants as it
 * enters, among the permanents [enchant] admits (CR 303.4f — not targeted, so hexproof doesn't
 * stop it, protection does). With nothing it could legally enchant it doesn't move (CR 303.4g;
 * the Harold and Bob ruling: "it remains in its owner's graveyard"). The changes last for as long
 * as the permanent stays on the battlefield.
 *
 * No neighbour takes this: [MoveToZoneEffect] moves a card with its printed characteristics and
 * offers an Aura host only by the card's printed enchant ability; `PutOntoBattlefieldAttachedToChosenEffect`'s
 * host filter only narrows a printed Aura's enchant ability and never becomes it; and
 * [BecomeArtifactEffect] rewrites a permanent already on the battlefield — applied after the move,
 * the card would enter as a creature first and pick no host at all.
 *
 * @property target The card to return — [EffectTarget.Self] for a dies trigger's own card.
 * @property enchant The enchant ability it gains ("enchant Forest you control"), read with "you"
 *   as the Aura's controller.
 * @property grantedStaticAbilities Static abilities it has as that Aura ("Enchanted Forest has
 *   '{T}: …'").
 * @property grantedAbilities Activated abilities it has as that Aura (Bronzehide Lion's
 *   "{G}{W}: Enchanted creature gains indestructible until end of turn").
 * @property fromZone The zone the card must still be in; a card that has left it doesn't move.
 */
@SerialName("PutOntoBattlefieldAsAura")
@Serializable
data class PutOntoBattlefieldAsAuraEffect(
    val enchant: GameObjectFilter,
    val grantedStaticAbilities: List<com.wingedsheep.sdk.scripting.StaticAbility> = emptyList(),
    val grantedAbilities: List<com.wingedsheep.sdk.scripting.ActivatedAbility> = emptyList(),
    val target: EffectTarget = EffectTarget.Self,
    val fromZone: Zone? = Zone.GRAVEYARD,
) : Effect {
    override val description: String = buildString {
        append("Return ${target.description} to the battlefield. It's an Aura enchantment with enchant ")
        append(enchant.description)
        val gained = grantedStaticAbilities.map { it.description } + grantedAbilities.map { it.description }
        if (gained.isNotEmpty()) append(" and ${gained.joinToString(" and ") { "\"$it\"" }}")
        append(", and it loses all other abilities")
    }

    override fun applyTextReplacement(replacer: TextReplacer): Effect {
        val newEnchant = enchant.applyTextReplacement(replacer)
        val newStatics = grantedStaticAbilities.map { it.applyTextReplacement(replacer) }
        val newAbilities = grantedAbilities.map { it.applyTextReplacement(replacer) }
        return if (newEnchant != enchant || newStatics != grantedStaticAbilities || newAbilities != grantedAbilities) {
            copy(enchant = newEnchant, grantedStaticAbilities = newStatics, grantedAbilities = newAbilities)
        } else this
    }
}

/**
 * Put an Aura card onto the battlefield **as a non-Aura enchantment** — the inverse of
 * [PutOntoBattlefieldAsAuraEffect]: "return this card to the battlefield under your control as a
 * non-Aura enchantment. It loses \"enchant creature\" and gains \"At the beginning of that
 * player's upkeep, …\"" (Takklemaggot).
 *
 * The card enters under the effect controller's control and attached to nothing, already without
 * its Aura subtype (layer 4; its card types and other subtypes stay) and so without an enchant
 * ability to obey — the unattached-Aura state-based action (CR 704.5m) never sees an Aura. Its other
 * printed abilities stay; the [grantedTriggeredAbilities] are added for as long as it remains on
 * the battlefield. [chosenPlayer], when set, is resolved now and recorded on the permanent as its
 * [com.wingedsheep.sdk.scripting.ChoiceSlot.PLAYER] choice, so the granted abilities can name
 * "that player" as [Player.ChosenPlayer].
 *
 * No neighbour takes this: [PutOntoBattlefieldAsAuraEffect] makes a card an Aura and strips every
 * other ability, the opposite direction on both axes; [MoveToZoneEffect] moves an Aura card with
 * its printed type line, so it would need a host and die to CR 704.5m without one.
 *
 * @property target The card to return — [EffectTarget.Self] for a trigger's own card.
 * @property grantedTriggeredAbilities Triggered abilities it gains as that enchantment.
 * @property chosenPlayer The player recorded as its chosen player, or null for none.
 * @property fromZone The zone the card must still be in; a card that has left it doesn't move.
 */
@SerialName("PutOntoBattlefieldAsNonAura")
@Serializable
data class PutOntoBattlefieldAsNonAuraEffect(
    val grantedTriggeredAbilities: List<com.wingedsheep.sdk.scripting.TriggeredAbility> = emptyList(),
    val chosenPlayer: EffectTarget? = null,
    val target: EffectTarget = EffectTarget.Self,
    val fromZone: Zone? = Zone.GRAVEYARD,
) : Effect {
    override val description: String = buildString {
        append("Return ${target.description} to the battlefield as a non-Aura enchantment. It loses its enchant ability")
        if (grantedTriggeredAbilities.isNotEmpty()) {
            append(" and gains ${grantedTriggeredAbilities.joinToString(" and ") { "\"${it.description}\"" }}")
        }
    }

    override fun applyTextReplacement(replacer: TextReplacer): Effect {
        val newAbilities = grantedTriggeredAbilities.map { it.applyTextReplacement(replacer) }
        return if (newAbilities != grantedTriggeredAbilities) copy(grantedTriggeredAbilities = newAbilities) else this
    }
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
}

/**
 * Exile all cards in each opponent's graveyard.
 * Used for Phyrexian Scriptures Chapter III and similar graveyard hate effects.
 */
@SerialName("ExileOpponentsGraveyards")
@Serializable
data object ExileOpponentsGraveyardsEffect : Effect {
    override val description: String = "Exile all opponents' graveyards"
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
}

/**
 * Library positions a target's owner can choose between when an effect lets them
 * place the card into their library at one of several positions.
 */
@Serializable
enum class LibraryChoicePosition {
    /** Top of library. */
    Top,

    /** Second from top — beneath the topmost card. */
    SecondFromTop,

    /** Bottom of library. */
    Bottom;

    /** Human-readable label for the option list shown to the choosing player. */
    val label: String
        get() = when (this) {
            Top -> "Top of library"
            SecondFromTop -> "Second from top of library"
            Bottom -> "Bottom of library"
        }

    /** Lower-case phrase for rules text — "the top of its owner's library". */
    val phrase: String
        get() = when (this) {
            Top -> "top"
            SecondFromTop -> "second from the top"
            Bottom -> "bottom"
        }
}

/**
 * The target's owner puts it into their library at one of the offered positions.
 *
 * Pauses for the owner to make a ChooseOptionDecision over [positions], then moves
 * the card accordingly. The target may be either a permanent on the battlefield
 * (e.g., Dire Downdraft) or a spell on the stack (e.g., Swat Away's "target spell
 * or creature") — the executor handles each case.
 *
 * Common configurations:
 * - `[Top, Bottom]` (default) — Dire Downdraft / Swat Away style (for *countering* a spell
 *   into its library — Hinder — use `CounterDestination.Library` instead)
 * - `[SecondFromTop, Bottom]` — Temporal Cleansing style
 *
 * @property target The entity to put into its owner's library
 * @property positions The library positions the owner may choose between
 */
@SerialName("PutOnLibraryPositionOfChoice")
@Serializable
data class PutOnLibraryPositionOfChoiceEffect(
    val target: EffectTarget,
    val positions: List<LibraryChoicePosition> = listOf(LibraryChoicePosition.Top, LibraryChoicePosition.Bottom)
) : Effect {
    override val description: String
        get() {
            val phrase = when (positions) {
                listOf(LibraryChoicePosition.Top, LibraryChoicePosition.Bottom) ->
                    "the top or bottom of their library"
                listOf(LibraryChoicePosition.SecondFromTop, LibraryChoicePosition.Bottom) ->
                    "their library second from the top or on the bottom"
                else -> positions.joinToString(" or ") { it.label.replaceFirstChar { c -> c.lowercase() } }
            }
            return "${target.description}'s owner puts it on $phrase"
        }
}

/**
 * Exile a warped permanent and mark it as re-castable via warp from exile.
 * Used by the warp mechanic's delayed trigger: "At the beginning of the next end step,
 * exile this permanent." The exiled card retains its warp ability and can be cast
 * from exile for its warp cost on a later turn.
 *
 * @property target The permanent to exile (resolved to SpecificEntity by delayed trigger creation)
 * @property enteredBattlefieldTimestamp The tracked permanent's battlefield-entry timestamp,
 *   snapshotted when the delayed trigger is created. At resolution the executor exiles the
 *   permanent only if its current entry timestamp still matches — if it left the battlefield
 *   and returned in between (blink), it's a new object the delayed trigger no longer tracks
 *   (CR 603.7c / 400.7) and the exile does nothing. Null skips the check (pre-existing
 *   serialized states, or callers that resolve the target at fire time).
 */
@SerialName("WarpExile")
@Serializable
data class WarpExileEffect(
    val target: EffectTarget,
    val enteredBattlefieldTimestamp: Long? = null
) : Effect {
    override val description: String = "Exile ${target.description} (warp)"
}

/**
 * Move one specifically tracked battlefield object to a zone.
 *
 * The optional [enteredBattlefieldTimestamp] identifies the object represented by [target], not
 * merely its entity ID. At resolution, the move is skipped unless the target is still on the
 * battlefield with that same entry timestamp. This is the reusable delayed-movement primitive for
 * effects such as dash's return-to-hand clause: a permanent that left and returned is a new object
 * and must not be moved by the old delayed trigger (CR 603.7c / 400.7).
 *
 * When nested in [CreateDelayedTriggerEffect], the delayed-trigger executor resolves [target] and
 * snapshots its entry timestamp when the trigger is created.
 */
@SerialName("MoveTrackedBattlefieldObject")
@Serializable
data class MoveTrackedBattlefieldObjectEffect(
    val target: EffectTarget,
    val destination: Zone,
    val enteredBattlefieldTimestamp: Long? = null
) : Effect {
    override val description: String =
        "Move ${target.description} to its owner's ${destination.noun}"
}

/**
 * Move an object out of its zone until this source leaves the battlefield. The return is a
 * one-shot effect, not a triggered ability: it happens immediately, even during resolution.
 * Both the source's battlefield visit and the moved object's destination visit are remembered.
 */
@SerialName("MoveUntilSourceLeaves")
@Serializable
data class MoveUntilSourceLeavesEffect(
    val target: EffectTarget,
    val destination: Zone
) : Effect {
    init {
        require(destination != Zone.BATTLEFIELD && destination != Zone.STACK)
    }
    override val description: String =
        "Move ${target.description} to ${destination.name.lowercase()} until this permanent leaves the battlefield"
}
