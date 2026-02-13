package com.wingedsheep.sdk.scripting

import com.wingedsheep.sdk.core.Zone
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Sealed interface for replacement effects.
 *
 * Replacement effects intercept game events BEFORE they happen and
 * modify or replace them entirely. Unlike triggered abilities, replacement
 * effects do not use the stack.
 *
 * The system is compositional - replacement effects are specified by combining
 * a GameEvent filter with a modification/replacement behavior.
 *
 * Examples:
 * ```kotlin
 * // Doubling Season (tokens)
 * DoubleTokenCreation(
 *     appliesTo = GameEvent.TokenCreationEvent(controller = ControllerFilter.You)
 * )
 *
 * // Hardened Scales
 * ModifyCounterPlacement(
 *     modifier = 1,
 *     appliesTo = GameEvent.CounterPlacementEvent(
 *         counterType = CounterTypeFilter.PlusOnePlusOne,
 *         recipient = RecipientFilter.CreatureYouControl
 *     )
 * )
 *
 * // Rest in Peace
 * RedirectZoneChange(
 *     newDestination = Zone.Exile,
 *     appliesTo = GameEvent.ZoneChangeEvent(to = Zone.Graveyard)
 * )
 *
 * // Prevention shield (combat damage from red sources)
 * PreventDamage(
 *     appliesTo = GameEvent.DamageEvent(
 *         recipient = RecipientFilter.You,
 *         source = SourceFilter.HasColor(Color.RED),
 *         damageType = DamageType.Combat
 *     )
 * )
 * ```
 */
@Serializable
sealed interface ReplacementEffect {
    /** Human-readable description of the replacement effect */
    val description: String

    /** What type of event this replacement intercepts (compositional) */
    val appliesTo: GameEvent
}

// =============================================================================
// Token Replacement Effects
// =============================================================================

/**
 * Double the number of tokens created.
 * Example: Doubling Season, Parallel Lives, Anointed Procession
 */
@SerialName("DoubleTokenCreation")
@Serializable
data class DoubleTokenCreation(
    override val appliesTo: GameEvent = GameEvent.TokenCreationEvent()
) : ReplacementEffect {
    override val description: String =
        "If ${appliesTo.description}, create twice that many of those tokens instead"
}

/**
 * Modify the number of tokens created by a fixed amount.
 */
@SerialName("ModifyTokenCount")
@Serializable
data class ModifyTokenCount(
    val modifier: Int,
    override val appliesTo: GameEvent = GameEvent.TokenCreationEvent()
) : ReplacementEffect {
    override val description: String = buildString {
        append("If ${appliesTo.description}, create ")
        if (modifier > 0) append("$modifier more")
        else append("${-modifier} fewer")
        append(" of those tokens instead")
    }
}

// =============================================================================
// Counter Replacement Effects
// =============================================================================

/**
 * Double the number of counters placed.
 * Example: Doubling Season (counters), Corpsejack Menace
 */
@SerialName("DoubleCounterPlacement")
@Serializable
data class DoubleCounterPlacement(
    override val appliesTo: GameEvent = GameEvent.CounterPlacementEvent(
        counterType = CounterTypeFilter.PlusOnePlusOne,
        recipient = RecipientFilter.CreatureYouControl
    )
) : ReplacementEffect {
    override val description: String =
        "If ${appliesTo.description}, place twice that many counters instead"
}

/**
 * Add additional counters when counters are placed.
 * Example: Hardened Scales (+1), Winding Constrictor (+1), Branching Evolution (double)
 */
@SerialName("ModifyCounterPlacement")
@Serializable
data class ModifyCounterPlacement(
    val modifier: Int = 1,
    override val appliesTo: GameEvent = GameEvent.CounterPlacementEvent(
        counterType = CounterTypeFilter.PlusOnePlusOne,
        recipient = RecipientFilter.CreatureYouControl
    )
) : ReplacementEffect {
    override val description: String = buildString {
        append("If ${appliesTo.description}, ")
        if (modifier > 0) {
            append("$modifier additional counter")
            if (modifier > 1) append("s")
            append(" is placed")
        } else {
            append("${-modifier} fewer counter")
            if (-modifier > 1) append("s")
            append(" is placed")
        }
    }
}

// =============================================================================
// Zone Change Replacement Effects
// =============================================================================

/**
 * Redirect a zone change to a different destination.
 * Example: Rest in Peace (graveyard → exile), Leyline of the Void
 */
@SerialName("RedirectZoneChange")
@Serializable
data class RedirectZoneChange(
    val newDestination: Zone,
    override val appliesTo: GameEvent
) : ReplacementEffect {
    override val description: String =
        "If ${appliesTo.description}, put it into ${newDestination.displayName} instead"
}

/**
 * Permanent enters the battlefield tapped.
 * Example: Glacial Fortress (conditional), tap lands, Thalia Heretic Cathar
 */
@SerialName("EntersTapped")
@Serializable
data class EntersTapped(
    override val appliesTo: GameEvent = GameEvent.ZoneChangeEvent(
        filter = GameObjectFilter.Any,
        to = Zone.BATTLEFIELD
    )
) : ReplacementEffect {
    override val description: String = "If ${appliesTo.description}, it enters tapped"
}

/**
 * Permanent/creature enters with counters.
 * Example: Master Biomancer, Metallic Mimic
 */
@SerialName("EntersWithCounters")
@Serializable
data class EntersWithCounters(
    val counterType: CounterTypeFilter = CounterTypeFilter.PlusOnePlusOne,
    val count: Int,
    override val appliesTo: GameEvent = GameEvent.ZoneChangeEvent(
        filter = GameObjectFilter.Creature.youControl(),
        to = Zone.BATTLEFIELD
    )
) : ReplacementEffect {
    override val description: String =
        "If ${appliesTo.description}, it enters with $count ${counterType.description} counters"
}

/**
 * Permanent/creature enters with a dynamic number of counters.
 * Example: Stag Beetle (enters with X +1/+1 counters where X = number of other creatures)
 */
@SerialName("EntersWithDynamicCounters")
@Serializable
data class EntersWithDynamicCounters(
    val counterType: CounterTypeFilter = CounterTypeFilter.PlusOnePlusOne,
    val count: DynamicAmount,
    override val appliesTo: GameEvent = GameEvent.ZoneChangeEvent(
        filter = GameObjectFilter.Creature.youControl(),
        to = Zone.BATTLEFIELD
    )
) : ReplacementEffect {
    override val description: String =
        "If ${appliesTo.description}, it enters with ${count.description} ${counterType.description} counters"
}

/**
 * Undying - if creature dies without +1/+1 counters, return it with one.
 */
@SerialName("Undying")
@Serializable
data class UndyingEffect(
    override val appliesTo: GameEvent = GameEvent.ZoneChangeEvent(
        filter = GameObjectFilter.Creature.youControl(),
        from = Zone.BATTLEFIELD,
        to = Zone.GRAVEYARD
    )
) : ReplacementEffect {
    override val description: String =
        "When this creature dies, if it had no +1/+1 counters on it, return it to the battlefield with a +1/+1 counter"
}

/**
 * Persist - if creature dies without -1/-1 counters, return it with one.
 */
@SerialName("Persist")
@Serializable
data class PersistEffect(
    override val appliesTo: GameEvent = GameEvent.ZoneChangeEvent(
        filter = GameObjectFilter.Creature.youControl(),
        from = Zone.BATTLEFIELD,
        to = Zone.GRAVEYARD
    )
) : ReplacementEffect {
    override val description: String =
        "When this creature dies, if it had no -1/-1 counters on it, return it to the battlefield with a -1/-1 counter"
}

// =============================================================================
// Damage Replacement Effects
// =============================================================================

/**
 * Prevent damage.
 * Example: Fog effects, protection, damage shields
 */
@SerialName("PreventDamage")
@Serializable
data class PreventDamage(
    val amount: Int? = null,  // null = prevent all
    override val appliesTo: GameEvent
) : ReplacementEffect {
    override val description: String = buildString {
        append("If ${appliesTo.description}, prevent ")
        if (amount == null) {
            append("that damage")
        } else {
            append("$amount of that damage")
        }
    }
}

/**
 * Redirect damage to another target.
 * Example: Pariah, Stuffy Doll, Boros Reckoner
 */
@SerialName("RedirectDamage")
@Serializable
data class RedirectDamage(
    val redirectTo: EffectTarget,
    override val appliesTo: GameEvent
) : ReplacementEffect {
    override val description: String =
        "If ${appliesTo.description}, that damage is dealt to ${redirectTo.description} instead"
}

/**
 * Double damage dealt.
 * Example: Furnace of Rath, Insult // Injury
 */
@SerialName("DoubleDamage")
@Serializable
data class DoubleDamage(
    override val appliesTo: GameEvent
) : ReplacementEffect {
    override val description: String =
        "If ${appliesTo.description}, it deals double that damage instead"
}

// =============================================================================
// Draw Replacement Effects
// =============================================================================

/**
 * Replace drawing with another effect.
 * Example: Underrealm Lich (look at 3, put 1 in hand, rest in graveyard)
 */
@SerialName("ReplaceDrawWith")
@Serializable
data class ReplaceDrawWithEffect(
    val replacementEffect: Effect,
    override val appliesTo: GameEvent = GameEvent.DrawEvent()
) : ReplacementEffect {
    override val description: String =
        "If ${appliesTo.description}, instead ${replacementEffect.description}"
}

/**
 * Prevent drawing (with optional replacement).
 * Example: Spirit of the Labyrinth (second draw), Narset Parter of Veils
 */
@SerialName("PreventDraw")
@Serializable
data class PreventDraw(
    override val appliesTo: GameEvent = GameEvent.DrawEvent()
) : ReplacementEffect {
    override val description: String =
        "If ${appliesTo.description}, that draw doesn't happen"
}

// =============================================================================
// Life Replacement Effects
// =============================================================================

/**
 * Prevent life gain.
 * Example: Erebos, Sulfuric Vortex
 */
@SerialName("PreventLifeGain")
@Serializable
data class PreventLifeGain(
    override val appliesTo: GameEvent = GameEvent.LifeGainEvent()
) : ReplacementEffect {
    override val description: String =
        "If ${appliesTo.description}, that player gains no life instead"
}

/**
 * Replace life gain with another effect.
 * Example: Tainted Remedy (life gain becomes life loss)
 */
@SerialName("ReplaceLifeGain")
@Serializable
data class ReplaceLifeGain(
    val replacementEffect: Effect,
    override val appliesTo: GameEvent = GameEvent.LifeGainEvent()
) : ReplacementEffect {
    override val description: String =
        "If ${appliesTo.description}, instead ${replacementEffect.description}"
}

/**
 * Modify life gain amount.
 * Example: Alhammarret's Archive (double life gain)
 */
@SerialName("ModifyLifeGain")
@Serializable
data class ModifyLifeGain(
    val multiplier: Int = 2,
    override val appliesTo: GameEvent = GameEvent.LifeGainEvent()
) : ReplacementEffect {
    override val description: String = when (multiplier) {
        2 -> "If ${appliesTo.description}, gain twice that much life instead"
        0 -> "If ${appliesTo.description}, gain no life instead"
        else -> "If ${appliesTo.description}, gain $multiplier times that much life instead"
    }
}

// =============================================================================
// Copy Replacement Effects
// =============================================================================

/**
 * Enter the battlefield as a copy of a creature.
 * Example: Clone ("You may have this creature enter as a copy of any creature on the battlefield")
 *
 * When this permanent would enter the battlefield, the controller may choose a creature
 * on the battlefield. If they do, the permanent enters as a copy of that creature.
 * If they don't (or can't), the permanent enters as itself (typically 0/0 and dies).
 */
@SerialName("EntersAsCopy")
@Serializable
data class EntersAsCopy(
    val optional: Boolean = true,
    override val appliesTo: GameEvent = GameEvent.ZoneChangeEvent(
        filter = GameObjectFilter.Any,
        to = Zone.BATTLEFIELD
    )
) : ReplacementEffect {
    override val description: String = if (optional) {
        "You may have this creature enter as a copy of any creature on the battlefield"
    } else {
        "This creature enters as a copy of any creature on the battlefield"
    }
}

// =============================================================================
// Creature Type Choice Replacement Effects
// =============================================================================

/**
 * As this permanent enters, choose a creature type.
 * The chosen type is stored on the permanent for use by other abilities.
 * Example: Doom Cannon, Cover of Darkness, Steely Resolve
 */
@SerialName("EntersWithCreatureTypeChoice")
@Serializable
data class EntersWithCreatureTypeChoice(
    val opponentChooses: Boolean = false,
    override val appliesTo: GameEvent = GameEvent.ZoneChangeEvent(
        filter = GameObjectFilter.Any,
        to = Zone.BATTLEFIELD
    )
) : ReplacementEffect {
    override val description: String = if (opponentChooses) {
        "As this permanent enters, an opponent chooses a creature type"
    } else {
        "As this permanent enters, choose a creature type"
    }
}

// =============================================================================
// Generic Replacement Effect
// =============================================================================

/**
 * Generic replacement effect for complex scenarios.
 * Use when no specific replacement effect type fits.
 */
@SerialName("GenericReplacement")
@Serializable
data class GenericReplacementEffect(
    val replacement: Effect?,  // null = prevent entirely
    override val appliesTo: GameEvent,
    override val description: String
) : ReplacementEffect
