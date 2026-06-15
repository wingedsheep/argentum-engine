package com.wingedsheep.sdk.scripting.effects

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.events.CounterTypeFilter
import com.wingedsheep.sdk.scripting.events.RecipientFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.text.TextReplacer
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// =============================================================================
// Counter Manipulation Effects
// =============================================================================

/**
 * Add counters effect.
 * "Put X +1/+1 counters on target creature"
 */
@SerialName("AddCounters")
@Serializable
data class AddCountersEffect(
    val counterType: String,
    val count: Int,
    val target: EffectTarget
) : Effect {
    override val description: String =
        "Put $count $counterType counter${if (count != 1) "s" else ""} on ${target.description}"
}

/**
 * Add counters with a dynamic amount.
 * "Put N +1/+1 counters on target creature, where N is [amount]"
 */
@SerialName("AddDynamicCounters")
@Serializable
data class AddDynamicCountersEffect(
    val counterType: String,
    val amount: DynamicAmount,
    val target: EffectTarget
) : Effect {
    override val description: String =
        "Put ${amount.description} $counterType counters on ${target.description}"
}

/**
 * Put all counters that were on the triggering source onto a target.
 * "When this creature dies, put its counters on target creature you control."
 *
 * At resolution, the executor reads the last-known counter map captured when the
 * source left the battlefield (every counter type, not just +1/+1) and places one
 * of each kind on the target.
 */
@SerialName("MoveAllLastKnownCounters")
@Serializable
data class MoveAllLastKnownCountersEffect(
    val target: EffectTarget = EffectTarget.ContextTarget(0)
) : Effect {
    override val description: String =
        "Put its counters on ${target.description}"
}

/**
 * Double the number of counters of a given kind already on a target (one-shot).
 * "Double the number of +1/+1 counters on that creature."
 *
 * Distinct from the `DoubleCounterPlacement` replacement effect, which doubles
 * counters as they are *placed* in the future. This is an immediate doubling of
 * the counters present at resolution: the executor reads the current count of
 * [counterType] on the target and puts that many more on it (so the total
 * doubles). Putting those counters still triggers counter-placement replacement
 * effects (e.g., Hardened Scales), matching the rules treatment of doubling as
 * additional counter placement.
 *
 * No-op when the target has no counters of [counterType].
 */
@SerialName("DoubleCounters")
@Serializable
data class DoubleCountersEffect(
    val counterType: String = Counters.PLUS_ONE_PLUS_ONE,
    val target: EffectTarget = EffectTarget.ContextTarget(0)
) : Effect {
    override val description: String =
        "Double the number of $counterType counters on ${target.description}"
}

/**
 * Remove counters effect.
 * "Remove X -1/-1 counters from target creature"
 */
@SerialName("RemoveCounters")
@Serializable
data class RemoveCountersEffect(
    val counterType: String,
    val count: Int,
    val target: EffectTarget
) : Effect {
    override val description: String =
        "Remove $count $counterType counter${if (count != 1) "s" else ""} from ${target.description}"
}

/**
 * Remove any number of counters from a target permanent. The controller chooses
 * how many counters of each kind to remove (0 up to the current count of that kind).
 *
 * "Remove any number of counters from target creature you control."
 *
 * At resolution time, the executor enumerates each counter kind currently on the
 * target and presents a sequence of `ChooseNumberDecision`s — one per kind.
 */
@SerialName("RemoveAnyNumberOfCounters")
@Serializable
data class RemoveAnyNumberOfCountersEffect(
    val target: EffectTarget = EffectTarget.ContextTarget(0)
) : Effect {
    override val description: String =
        "Remove any number of counters from ${target.description}"
}

/**
 * Remove every counter (of any kind) from a target permanent.
 *
 * "Remove all counters from target creature."
 *
 * Mandatory and non-interactive — the executor clears every counter kind currently
 * on the target. No-op when the target has no counters.
 */
@SerialName("RemoveAllCounters")
@Serializable
data class RemoveAllCountersEffect(
    val target: EffectTarget = EffectTarget.ContextTarget(0)
) : Effect {
    override val description: String =
        "Remove all counters from ${target.description}"
}

/**
 * Add counters to all entities in a named collection.
 * Used for non-targeting "choose" effects that place counters on multiple permanents.
 * "Put an aim counter on each of them"
 */
@SerialName("AddCountersToCollection")
@Serializable
data class AddCountersToCollectionEffect(
    val collectionName: String,
    val counterType: String,
    val count: Int = 1
) : Effect {
    override val description: String =
        "Put $count $counterType counter${if (count != 1) "s" else ""} on each of those permanents"
}

/**
 * Move one counter of each kind present on a [source] permanent that is *not* already
 * on the [destination] permanent, from the source onto the destination.
 *
 * "Move a counter of each kind not on Goldberry from another target permanent you control
 * onto Goldberry." (Goldberry, River-Daughter — ability A.)
 *
 * Deterministic, no player decision: the executor inspects every counter kind on the
 * source; for each kind the destination does *not* currently have, it removes one of that
 * kind from the source and adds one to the destination (emitting proper counter events and
 * honoring counter-placement replacement effects). Kinds the destination already has are
 * left untouched. No-op when source/destination is missing or the destination can't receive
 * counters.
 *
 * @property source The permanent counters are moved *from*
 * @property destination The permanent counters are moved *onto*
 */
@SerialName("MoveCountersEachKindMissing")
@Serializable
data class MoveCountersEachKindMissingEffect(
    val source: EffectTarget,
    val destination: EffectTarget
) : Effect {
    override val description: String =
        "Move a counter of each kind not on ${destination.description} from ${source.description} onto ${destination.description}"
}

/**
 * Move a player-chosen set (one or more) of counters from a [source] permanent onto a
 * [destination] permanent, then optionally draw a card if any counters were moved.
 *
 * "Move one or more counters from Goldberry onto another target permanent you control.
 * If you do, draw a card." (Goldberry, River-Daughter — ability B.)
 *
 * At resolution the executor enumerates each counter kind on the source and prompts the
 * controller (one [ChooseNumberDecision][com.wingedsheep.engine.core.ChooseNumberDecision]
 * per kind, 0..count) for how many of that kind to move; chosen counters are removed from
 * the source and added to the destination (honoring counter-placement replacement effects).
 * If [drawCardOnMove] is set and at least one counter was moved, the controller draws a card.
 *
 * @property source The permanent counters are moved *from*
 * @property destination The permanent counters are moved *onto*
 * @property drawCardOnMove When true, the controller draws a card if any counter was moved
 */
@SerialName("MoveChosenCountersToTarget")
@Serializable
data class MoveChosenCountersToTargetEffect(
    val source: EffectTarget,
    val destination: EffectTarget,
    val drawCardOnMove: Boolean = false
) : Effect {
    override val description: String = buildString {
        append("Move one or more counters from ${source.description} onto ${destination.description}")
        if (drawCardOnMove) append(". If you do, draw a card")
    }
}

/**
 * Distribute any number of counters from this creature onto other creatures.
 * "At the beginning of your upkeep, you may move any number of +1/+1 counters
 * from Forgotten Ancient onto other creatures."
 *
 * At resolution time, the executor:
 * 1. Checks how many counters of the given type are on the source creature
 * 2. Finds all other creatures on the battlefield
 * 3. If 0 counters or no other creatures, does nothing
 * 4. Presents a DistributeDecision with total = counter count, targets = other creatures
 * 5. On response, removes distributed counters from self and adds them per the distribution
 *
 * Does not target — the recipient creatures are chosen at resolution time.
 *
 * @property counterType The type of counter to move (e.g., "+1/+1")
 */
@SerialName("DistributeCountersFromSelf")
@Serializable
data class DistributeCountersFromSelfEffect(
    val counterType: String = Counters.PLUS_ONE_PLUS_ONE
) : Effect {
    override val description: String =
        "Move any number of $counterType counters from this creature onto other creatures"
}

/**
 * Proliferate — choose any number of permanents and/or players that have a counter.
 * For each, give it another counter of each kind already there.
 *
 * Pure data; the engine resolves at execution time by:
 * 1. Gathering all permanents on the battlefield AND all players that have at least
 *    one counter of any kind.
 * 2. Asking the controller to pick a subset (any number, including zero).
 * 3. For each chosen entity, adding one counter of every kind it already has.
 */
@SerialName("Proliferate")
@Serializable
data object ProliferateEffect : Effect {
    override val description: String =
        "Proliferate. (Choose any number of permanents and/or players, then give each another counter of each kind already there.)"
}

/**
 * Distribute a fixed number of counters among the targets from context.
 * "Distribute N counters among one or more target creatures you control."
 *
 * Distribution is deterministic when totalCounters equals number of targets * minPerTarget.
 * With 1 target, all counters go on it. With multiple targets, counters are divided evenly
 * (remainder goes to the first target).
 *
 * @property totalCounters Total number of counters to distribute
 * @property counterType The type of counter (e.g., "+1/+1")
 * @property minPerTarget Minimum counters each target must receive (per MTG rules, typically 1)
 */
@SerialName("DistributeCountersAmongTargets")
@Serializable
data class DistributeCountersAmongTargetsEffect(
    val totalCounters: Int,
    val counterType: String = Counters.PLUS_ONE_PLUS_ONE,
    val minPerTarget: Int = 1
) : Effect {
    override val description: String =
        "Distribute $totalCounters $counterType counter${if (totalCounters != 1) "s" else ""} among targets"
}

/**
 * Install a temporary, duration-scoped counter-placement-*modifier* replacement effect,
 * controlled by the resolving ability's controller.
 *
 * This is the activated/spell-granted, time-bounded analogue of the static
 * [com.wingedsheep.sdk.scripting.ModifyCounterPlacement] replacement (Hardened Scales,
 * Winding Constrictor). Where `ModifyCounterPlacement` lives on a permanent for as long as
 * that permanent is on the battlefield, this effect records a modifier in a turn-scoped store
 * on the game state, controller-scoped exactly like the static version: while it is active, if
 * the *controller* would put counters matching [counterType] on a recipient matching [recipient]
 * (resolved relative to that controller — e.g. "a creature you control" means a creature the
 * controller controls), [modifier] additional counters are placed instead.
 *
 * The store is consulted from the single counter-placement chokepoint, so every counter-adding
 * effect (AddCounters, AddDynamicCounters, Explore, enters-with-counters, proliferate-driven,
 * move-counters, …) honors it automatically. The modifier expires per [duration]
 * (typically [Duration.EndOfTurn]) via the normal end-of-turn cleanup.
 *
 * General by construction — parameterized over [modifier] (positive or negative), [duration],
 * [counterType] and [recipient]. The defaults reproduce the most common case ("+1/+1 counters
 * on a creature you control, until end of turn"), which is what Prairie Dog's
 * "{4}{W}: Until end of turn, if you would put one or more +1/+1 counters on a creature you
 * control, put that many plus one +1/+1 counters on it instead" needs.
 *
 * Examples:
 * - Prairie Dog (OTJ): `GrantCounterPlacementModifierEffect()` — +1, EndOfTurn, +1/+1, creature you control.
 * - A hypothetical "until end of turn, put one fewer counter…": `modifier = -1`.
 *
 * @property modifier Additional counters placed (negative reduces; the chokepoint floors the
 *        resulting count at 0).
 * @property duration How long the modifier stays active (default [Duration.EndOfTurn]).
 * @property counterType Which counter kind the modifier applies to (default +1/+1).
 * @property recipient Which recipients the modifier applies to, resolved relative to the
 *        controller (default "a creature you control").
 */
@SerialName("GrantCounterPlacementModifier")
@Serializable
data class GrantCounterPlacementModifierEffect(
    val modifier: Int = 1,
    val duration: Duration = Duration.EndOfTurn,
    val counterType: CounterTypeFilter = CounterTypeFilter.PlusOnePlusOne,
    val recipient: RecipientFilter = RecipientFilter.CreatureYouControl
) : Effect {
    override val description: String = buildString {
        val durationText = if (duration == Duration.Permanent) "" else "${duration.description.replaceFirstChar { it.uppercase() }}, "
        append(durationText)
        append("if you would put one or more ${counterType.description} counters on ${recipient.description}, ")
        if (modifier >= 0) {
            append("put that many plus $modifier ${counterType.description} counter${if (modifier != 1) "s" else ""} on it instead")
        } else {
            append("put that many minus ${-modifier} ${counterType.description} counter${if (-modifier != 1) "s" else ""} on it instead")
        }
    }
}
