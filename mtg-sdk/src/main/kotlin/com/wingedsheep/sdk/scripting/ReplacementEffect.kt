package com.wingedsheep.sdk.scripting

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.scripting.conditions.Condition
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.events.CounterTypeFilter
import com.wingedsheep.sdk.scripting.events.RecipientFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.text.TextReplaceable
import com.wingedsheep.sdk.scripting.text.TextReplacer
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.values.DynamicAmount
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
 * a EventPattern filter with a modification/replacement behavior.
 *
 * Examples:
 * ```kotlin
 * // Doubling Season (tokens)
 * DoubleTokenCreation(
 *     appliesTo = EventPattern.TokenCreationEvent(controller = ControllerFilter.You)
 * )
 *
 * // Hardened Scales
 * ModifyCounterPlacement(
 *     modifier = 1,
 *     appliesTo = EventPattern.CounterPlacementEvent(
 *         counterType = CounterTypeFilter.PlusOnePlusOne,
 *         recipient = RecipientFilter.CreatureYouControl
 *     )
 * )
 *
 * // Rest in Peace
 * RedirectZoneChange(
 *     newDestination = Zone.Exile,
 *     appliesTo = EventPattern.ZoneChangeEvent(to = Zone.Graveyard)
 * )
 *
 * // Prevention shield (combat damage from red sources)
 * PreventDamage(
 *     appliesTo = EventPattern.DamageEvent(
 *         recipient = RecipientFilter.You,
 *         source = SourceFilter.HasColor(Color.RED),
 *         damageType = DamageType.Combat
 *     )
 * )
 * ```
 */
@Serializable
sealed interface ReplacementEffect : TextReplaceable<ReplacementEffect> {
    /** Human-readable description of the replacement effect */
    val description: String

    /** What type of event this replacement intercepts (compositional) */
    val appliesTo: EventPattern
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
    override val appliesTo: EventPattern = EventPattern.TokenCreationEvent()
) : ReplacementEffect {
    override val description: String =
        "If ${appliesTo.description}, create twice that many of those tokens instead"

    override fun applyTextReplacement(replacer: TextReplacer): ReplacementEffect {
        val newAppliesTo = appliesTo.applyTextReplacement(replacer)
        return if (newAppliesTo !== appliesTo) copy(appliesTo = newAppliesTo) else this
    }
}

/**
 * Modify the number of tokens created by a fixed amount.
 */
@SerialName("ModifyTokenCount")
@Serializable
data class ModifyTokenCount(
    val modifier: Int,
    override val appliesTo: EventPattern = EventPattern.TokenCreationEvent()
) : ReplacementEffect {
    override val description: String = buildString {
        append("If ${appliesTo.description}, create ")
        if (modifier > 0) append("$modifier more")
        else append("${-modifier} fewer")
        append(" of those tokens instead")
    }

    override fun applyTextReplacement(replacer: TextReplacer): ReplacementEffect {
        val newAppliesTo = appliesTo.applyTextReplacement(replacer)
        return if (newAppliesTo !== appliesTo) copy(appliesTo = newAppliesTo) else this
    }
}

// =============================================================================
// Counter Replacement Effects
// =============================================================================

/**
 * Double the number of counters placed.
 * Example: Doubling Season (counters), Corpsejack Menace, Innkeeper's Talent Level 3
 *
 * @param placedByYou When true, only applies when the controller of this effect is the
 *                    player putting the counters (e.g., Innkeeper's Talent: "If YOU would
 *                    put one or more counters..."). When false, applies regardless of who
 *                    is placing the counters — the recipient filter on [appliesTo] is the
 *                    sole "you control" gate (e.g., Doubling Season: "on a permanent you
 *                    control").
 */
@SerialName("DoubleCounterPlacement")
@Serializable
data class DoubleCounterPlacement(
    val placedByYou: Boolean = false,
    override val appliesTo: EventPattern = EventPattern.CounterPlacementEvent(
        counterType = CounterTypeFilter.PlusOnePlusOne,
        recipient = RecipientFilter.CreatureYouControl
    )
) : ReplacementEffect {
    override val description: String =
        "If ${appliesTo.description}, place twice that many counters instead"

    override fun applyTextReplacement(replacer: TextReplacer): ReplacementEffect {
        val newAppliesTo = appliesTo.applyTextReplacement(replacer)
        return if (newAppliesTo !== appliesTo) copy(appliesTo = newAppliesTo) else this
    }
}

/**
 * Add additional counters when counters are placed.
 * Example: Hardened Scales (+1), Winding Constrictor (+1), Branching Evolution (double)
 */
@SerialName("ModifyCounterPlacement")
@Serializable
data class ModifyCounterPlacement(
    val modifier: Int = 1,
    override val appliesTo: EventPattern = EventPattern.CounterPlacementEvent(
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

    override fun applyTextReplacement(replacer: TextReplacer): ReplacementEffect {
        val newAppliesTo = appliesTo.applyTextReplacement(replacer)
        return if (newAppliesTo !== appliesTo) copy(appliesTo = newAppliesTo) else this
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
    override val appliesTo: EventPattern
) : ReplacementEffect {
    override val description: String =
        "If ${appliesTo.description}, put it into ${newDestination.displayName} instead"

    override fun applyTextReplacement(replacer: TextReplacer): ReplacementEffect {
        val newAppliesTo = appliesTo.applyTextReplacement(replacer)
        return if (newAppliesTo !== appliesTo) copy(appliesTo = newAppliesTo) else this
    }
}

/**
 * Permanent enters the battlefield tapped.
 * Example: Glacial Fortress (conditional), tap lands, Thalia Heretic Cathar, Steam Vents (pay life)
 *
 * @param unlessCondition If non-null, the permanent only enters tapped when this condition is NOT met.
 *                        Used for "check lands" like Sulfur Falls ("enters tapped unless you control an Island or a Mountain").
 * @param payLifeCost If non-null, the player may pay this much life to have the permanent enter untapped.
 *                    Used for "shock lands" like Steam Vents ("you may pay 2 life. If you don't, it enters tapped").
 */
/**
 * Generic "as ~ enters the battlefield, run [effect]" replacement.
 *
 * The wrapped [effect] executes via the normal effect-executor pipeline at the
 * moment the source permanent enters, AFTER it has been placed on the battlefield
 * (so `EffectTarget.Self` resolves to the entering permanent) but BEFORE the
 * standard `EntersTapped` check runs. The effect may pause for player input
 * (continuations, target selection, sub-decisions) just like any other effect.
 *
 * Use this to compose ETB-time choices out of existing atoms — e.g. SOI shadow
 * lands wrap [com.wingedsheep.sdk.scripting.effects.MayRevealCardFromHandEffect]
 * with `otherwise = Effects.Tap(EffectTarget.Self)`; a future "as ~ enters,
 * sacrifice another creature" land could wrap `Effects.Sacrifice(filter)`.
 *
 * Distinct from a "when ~ enters" [com.wingedsheep.sdk.scripting.trigger.Trigger]:
 * triggers fire after entry resolves and use the stack, so they can't gate ETB-time
 * state like the tapped-on-entry flag. This replacement runs synchronously inline
 * with the entry event.
 */
@SerialName("OnEnterRunEffect")
@Serializable
data class OnEnterRunEffect(
    val effect: com.wingedsheep.sdk.scripting.effects.Effect,
    override val appliesTo: EventPattern = EventPattern.ZoneChangeEvent(
        filter = GameObjectFilter.Any,
        to = Zone.BATTLEFIELD
    )
) : ReplacementEffect {
    override val description: String = "As this permanent enters, ${effect.description.lowercase()}"

    override fun applyTextReplacement(replacer: TextReplacer): ReplacementEffect {
        val newEffect = effect.applyTextReplacement(replacer)
        val newAppliesTo = appliesTo.applyTextReplacement(replacer)
        return if (newEffect !== effect || newAppliesTo !== appliesTo)
            copy(effect = newEffect, appliesTo = newAppliesTo)
        else this
    }
}

@SerialName("EntersTapped")
@Serializable
data class EntersTapped(
    val unlessCondition: Condition? = null,
    val payLifeCost: Int? = null,
    override val appliesTo: EventPattern = EventPattern.ZoneChangeEvent(
        filter = GameObjectFilter.Any,
        to = Zone.BATTLEFIELD
    )
) : ReplacementEffect {
    override val description: String = when {
        payLifeCost != null -> "As this permanent enters, you may pay $payLifeCost life. If you don't, it enters tapped."
        unlessCondition != null -> "This permanent enters tapped unless ${unlessCondition.description}"
        else -> "If ${appliesTo.description}, it enters tapped"
    }

    override fun applyTextReplacement(replacer: TextReplacer): ReplacementEffect {
        val newAppliesTo = appliesTo.applyTextReplacement(replacer)
        return if (newAppliesTo !== appliesTo) copy(appliesTo = newAppliesTo) else this
    }
}

/**
 * Permanent/creature enters with counters.
 * Example: Master Biomancer, Metallic Mimic
 *
 * @param condition When non-null, the counters are only added if this condition evaluates true
 *                  at the moment the permanent enters the battlefield. Used for cards like
 *                  Frilled Sparkshooter ("This creature enters with a +1/+1 counter on it if
 *                  an opponent lost life this turn.").
 */
@SerialName("EntersWithCounters")
@Serializable
data class EntersWithCounters(
    val counterType: CounterTypeFilter = CounterTypeFilter.PlusOnePlusOne,
    val count: Int,
    val selfOnly: Boolean = false,
    val condition: Condition? = null,
    override val appliesTo: EventPattern = EventPattern.ZoneChangeEvent(
        filter = GameObjectFilter.Creature.youControl(),
        to = Zone.BATTLEFIELD
    )
) : ReplacementEffect {
    override val description: String = buildString {
        append("If ${appliesTo.description}, it enters with $count ${counterType.description} counters")
        if (condition != null) append(" if ${condition.description}")
    }

    override fun applyTextReplacement(replacer: TextReplacer): ReplacementEffect {
        val newAppliesTo = appliesTo.applyTextReplacement(replacer)
        val newCondition = condition?.applyTextReplacement(replacer)
        return if (newAppliesTo !== appliesTo || newCondition !== condition)
            copy(appliesTo = newAppliesTo, condition = newCondition)
        else this
    }
}

/**
 * Permanent/creature enters with a dynamic number of counters.
 * Example: Stag Beetle (enters with X +1/+1 counters where X = number of other creatures)
 *
 * @param otherOnly When true, this effect only applies to OTHER creatures entering
 *                  (not the permanent with this replacement effect). Used for
 *                  Gev, Scaled Scorch: "Other creatures you control enter with additional counters."
 */
@SerialName("EntersWithDynamicCounters")
@Serializable
data class EntersWithDynamicCounters(
    val counterType: CounterTypeFilter = CounterTypeFilter.PlusOnePlusOne,
    val count: DynamicAmount,
    val otherOnly: Boolean = false,
    override val appliesTo: EventPattern = EventPattern.ZoneChangeEvent(
        filter = GameObjectFilter.Creature.youControl(),
        to = Zone.BATTLEFIELD
    )
) : ReplacementEffect {
    override val description: String =
        "If ${appliesTo.description}, it enters with ${count.description} ${counterType.description} counters"

    override fun applyTextReplacement(replacer: TextReplacer): ReplacementEffect {
        val newAppliesTo = appliesTo.applyTextReplacement(replacer)
        val newCount = count.applyTextReplacement(replacer)
        return if (newAppliesTo !== appliesTo || newCount !== count) copy(appliesTo = newAppliesTo, count = newCount) else this
    }
}

/**
 * Undying - if creature dies without +1/+1 counters, return it with one.
 */
@SerialName("Undying")
@Serializable
data class UndyingEffect(
    override val appliesTo: EventPattern = EventPattern.ZoneChangeEvent(
        filter = GameObjectFilter.Creature.youControl(),
        from = Zone.BATTLEFIELD,
        to = Zone.GRAVEYARD
    )
) : ReplacementEffect {
    override val description: String =
        "When this creature dies, if it had no +1/+1 counters on it, return it to the battlefield with a +1/+1 counter"

    override fun applyTextReplacement(replacer: TextReplacer): ReplacementEffect {
        val newAppliesTo = appliesTo.applyTextReplacement(replacer)
        return if (newAppliesTo !== appliesTo) copy(appliesTo = newAppliesTo) else this
    }
}

/**
 * Persist - if creature dies without -1/-1 counters, return it with one.
 */
@SerialName("Persist")
@Serializable
data class PersistEffect(
    override val appliesTo: EventPattern = EventPattern.ZoneChangeEvent(
        filter = GameObjectFilter.Creature.youControl(),
        from = Zone.BATTLEFIELD,
        to = Zone.GRAVEYARD
    )
) : ReplacementEffect {
    override val description: String =
        "When this creature dies, if it had no -1/-1 counters on it, return it to the battlefield with a -1/-1 counter"

    override fun applyTextReplacement(replacer: TextReplacer): ReplacementEffect {
        val newAppliesTo = appliesTo.applyTextReplacement(replacer)
        return if (newAppliesTo !== appliesTo) copy(appliesTo = newAppliesTo) else this
    }
}

// =============================================================================
// Damage Replacement Effects
// =============================================================================

/**
 * Prevent damage.
 * Example: Fog effects, protection, damage shields
 *
 * The optional [restrictions] list lets a card gate the prevention on arbitrary
 * additional conditions (mirroring [ModifyLifeLoss.restrictions]). Each entry is a
 * [Condition] evaluated against the source permanent's controller; the prevention
 * only applies when *all* restrictions hold. This is how "as long as …, prevent …"
 * statics are expressed without a dedicated conditional-replacement wrapper — e.g.
 * Spirit of Resistance ("As long as you control a permanent of each color, prevent
 * all damage that would be dealt to you").
 */
@SerialName("PreventDamage")
@Serializable
data class PreventDamage(
    val amount: Int? = null,  // null = prevent all
    val restrictions: List<Condition> = emptyList(),
    override val appliesTo: EventPattern
) : ReplacementEffect {
    override val description: String = buildString {
        val restrictionDesc = restrictions.joinToString(" and ") { it.description.removePrefix("if ") }
        if (restrictionDesc.isNotEmpty()) {
            append(restrictionDesc.replaceFirstChar { it.uppercase() })
            append(", if ")
        } else {
            append("If ")
        }
        append(appliesTo.description)
        append(", prevent ")
        if (amount == null) {
            append("that damage")
        } else {
            append("$amount of that damage")
        }
    }

    override fun applyTextReplacement(replacer: TextReplacer): ReplacementEffect {
        val newAppliesTo = appliesTo.applyTextReplacement(replacer)
        val newRestrictions = restrictions.map { it.applyTextReplacement(replacer) }
        val anyChanged = newAppliesTo !== appliesTo ||
            newRestrictions.zip(restrictions).any { (n, o) -> n !== o }
        return if (anyChanged) copy(appliesTo = newAppliesTo, restrictions = newRestrictions) else this
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
    override val appliesTo: EventPattern
) : ReplacementEffect {
    override val description: String =
        "If ${appliesTo.description}, that damage is dealt to ${redirectTo.description} instead"

    override fun applyTextReplacement(replacer: TextReplacer): ReplacementEffect {
        val newAppliesTo = appliesTo.applyTextReplacement(replacer)
        return if (newAppliesTo !== appliesTo) copy(appliesTo = newAppliesTo) else this
    }
}

/**
 * Double damage dealt.
 * Example: Furnace of Rath, Insult // Injury
 */
@SerialName("DoubleDamage")
@Serializable
data class DoubleDamage(
    override val appliesTo: EventPattern
) : ReplacementEffect {
    override val description: String =
        "If ${appliesTo.description}, it deals double that damage instead"

    override fun applyTextReplacement(replacer: TextReplacer): ReplacementEffect {
        val newAppliesTo = appliesTo.applyTextReplacement(replacer)
        return if (newAppliesTo !== appliesTo) copy(appliesTo = newAppliesTo) else this
    }
}

/**
 * Modify damage dealt by a fixed amount.
 * Example: Valley Flamecaller ("If a Lizard, Mouse, Otter, or Raccoon you control would deal
 * damage to a permanent or player, it deals that much damage plus 1 instead.")
 */
@SerialName("ModifyDamageAmount")
@Serializable
data class ModifyDamageAmount(
    val modifier: Int,
    override val appliesTo: EventPattern
) : ReplacementEffect {
    override val description: String = buildString {
        append("If ${appliesTo.description}, it deals that much damage plus $modifier instead")
    }

    override fun applyTextReplacement(replacer: TextReplacer): ReplacementEffect {
        val newAppliesTo = appliesTo.applyTextReplacement(replacer)
        return if (newAppliesTo !== appliesTo) copy(appliesTo = newAppliesTo) else this
    }
}

/**
 * Cap damage at a maximum amount. If a matching source would deal more than
 * [maxAmount] damage, it deals exactly [maxAmount] instead (smaller amounts are
 * unchanged). Distinct from [PreventDamage] (which subtracts) and [ModifyDamageAmount]
 * (which adds): capping clamps to an upper bound.
 *
 * Example: Divine Presence ("If a source would deal 4 or more damage to a permanent
 * or player, that source deals 3 damage to that permanent or player instead.") →
 * `CapDamage(maxAmount = 3, appliesTo = DamageEvent(recipient = AnyPermanentOrPlayer))`.
 */
@SerialName("CapDamage")
@Serializable
data class CapDamage(
    val maxAmount: Int,
    override val appliesTo: EventPattern
) : ReplacementEffect {
    override val description: String =
        "If ${appliesTo.description}, it deals $maxAmount damage instead"

    override fun applyTextReplacement(replacer: TextReplacer): ReplacementEffect {
        val newAppliesTo = appliesTo.applyTextReplacement(replacer)
        return if (newAppliesTo !== appliesTo) copy(appliesTo = newAppliesTo) else this
    }
}

// =============================================================================
// Draw Replacement Effects
// =============================================================================

/**
 * Modify the number of cards a draw event draws by a fixed amount, optionally gated by
 * additional [restrictions]. Applied at the call site where the original draw count is
 * announced (spell/ability resolution and the draw step), so the modifier fires once per
 * draw instruction (CR 121.2a: "An instruction to draw multiple cards can be modified by
 * replacement effects that refer to the number of cards drawn. This modification occurs
 * before considering any of the individual card draws.") and is not re-applied when a
 * paused per-card draw loop resumes.
 *
 * Each entry in [restrictions] is a [Condition] evaluated against the drawing player as
 * the controller context; the modification only applies when ALL restrictions hold. This
 * mirrors [ModifyLifeLoss]'s shape — use it for cards whose extra-draw clause is gated by
 * arbitrary additional conditions. Note that "you" in restriction text reads as the drawing
 * player, not the source's controller — for `DrawEvent(player = Player.You)` they're the
 * same, but a future `DrawEvent(player = Player.Opponent)` card whose restriction means
 * "you" = source controller would need a source-relative condition instead.
 *
 * Examples:
 * - Quantum Riddler ("As long as you have one or fewer cards in hand, if you would draw
 *   one or more cards, you draw that many cards plus one instead"):
 *     `ModifyDrawAmount(modifier = 1,
 *                       restrictions = listOf(Conditions.CardsInHandAtMost(1)),
 *                       appliesTo = DrawEvent(player = Player.You))`
 *
 * @param modifier Flat amount added to the draw count when the event fires for a matching
 *        player. Negative values reduce the draw (clamped to ≥ 0 by the caller).
 * @param restrictions Additional [Condition]s gating when the modifier applies. Evaluated
 *        against the drawing player as controller; ALL must hold.
 */
@SerialName("ModifyDrawAmount")
@Serializable
data class ModifyDrawAmount(
    val modifier: Int,
    val restrictions: List<Condition> = emptyList(),
    override val appliesTo: EventPattern = EventPattern.DrawEvent()
) : ReplacementEffect {
    override val description: String = buildString {
        val restrictionDesc = restrictions.joinToString(" and ") { it.description.removePrefix("if ") }
        if (restrictionDesc.isNotEmpty()) {
            append(restrictionDesc.replaceFirstChar { it.uppercase() })
            append(", if ")
        } else {
            append("If ")
        }
        append(appliesTo.description)
        append(", they draw that many cards plus $modifier instead")
    }

    override fun applyTextReplacement(replacer: TextReplacer): ReplacementEffect {
        val newAppliesTo = appliesTo.applyTextReplacement(replacer)
        val newRestrictions = restrictions.map { it.applyTextReplacement(replacer) }
        val anyChanged = newAppliesTo !== appliesTo ||
            newRestrictions.zip(restrictions).any { (n, o) -> n !== o }
        return if (anyChanged) copy(appliesTo = newAppliesTo, restrictions = newRestrictions) else this
    }
}

/**
 * Replace drawing with another effect.
 * Example: Underrealm Lich (look at 3, put 1 in hand, rest in graveyard)
 */
@SerialName("ReplaceDrawWith")
@Serializable
data class ReplaceDrawWithEffect(
    val replacementEffect: Effect,
    val optional: Boolean = false,
    override val appliesTo: EventPattern = EventPattern.DrawEvent()
) : ReplacementEffect {
    override val description: String = buildString {
        append("If ${appliesTo.description}, ")
        if (optional) append("you may ")
        append("instead ${replacementEffect.description}")
    }

    override fun applyTextReplacement(replacer: TextReplacer): ReplacementEffect {
        val newAppliesTo = appliesTo.applyTextReplacement(replacer)
        val newReplacementEffect = replacementEffect.applyTextReplacement(replacer)
        return if (newAppliesTo !== appliesTo || newReplacementEffect !== replacementEffect)
            copy(appliesTo = newAppliesTo, replacementEffect = newReplacementEffect)
        else this
    }
}

/**
 * Prevent drawing (with optional replacement).
 * Example: Spirit of the Labyrinth (second draw), Narset Parter of Veils
 */
@SerialName("PreventDraw")
@Serializable
data class PreventDraw(
    override val appliesTo: EventPattern = EventPattern.DrawEvent()
) : ReplacementEffect {
    override val description: String =
        "If ${appliesTo.description}, that draw doesn't happen"

    override fun applyTextReplacement(replacer: TextReplacer): ReplacementEffect {
        val newAppliesTo = appliesTo.applyTextReplacement(replacer)
        return if (newAppliesTo !== appliesTo) copy(appliesTo = newAppliesTo) else this
    }
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
    override val appliesTo: EventPattern = EventPattern.LifeGainEvent()
) : ReplacementEffect {
    override val description: String =
        "If ${appliesTo.description}, that player gains no life instead"

    override fun applyTextReplacement(replacer: TextReplacer): ReplacementEffect {
        val newAppliesTo = appliesTo.applyTextReplacement(replacer)
        return if (newAppliesTo !== appliesTo) copy(appliesTo = newAppliesTo) else this
    }
}

/**
 * Damage can't be prevented.
 * Example: Sunspine Lynx, Leyline of Punishment
 *
 * While a permanent with this replacement effect is on the battlefield,
 * all damage is treated as though it can't be prevented (protection,
 * prevention shields, etc. are ignored).
 */
@SerialName("DamageCantBePrevented")
@Serializable
data class DamageCantBePrevented(
    override val appliesTo: EventPattern = EventPattern.DamageEvent()
) : ReplacementEffect {
    override val description: String = "Damage can't be prevented"

    override fun applyTextReplacement(replacer: TextReplacer): ReplacementEffect {
        val newAppliesTo = appliesTo.applyTextReplacement(replacer)
        return if (newAppliesTo !== appliesTo) copy(appliesTo = newAppliesTo) else this
    }
}

/**
 * Replace life gain with another effect.
 * Example: Tainted Remedy (life gain becomes life loss)
 */
@SerialName("ReplaceLifeGain")
@Serializable
data class ReplaceLifeGain(
    val replacementEffect: Effect,
    override val appliesTo: EventPattern = EventPattern.LifeGainEvent()
) : ReplacementEffect {
    override val description: String =
        "If ${appliesTo.description}, instead ${replacementEffect.description}"

    override fun applyTextReplacement(replacer: TextReplacer): ReplacementEffect {
        val newAppliesTo = appliesTo.applyTextReplacement(replacer)
        val newReplacementEffect = replacementEffect.applyTextReplacement(replacer)
        return if (newAppliesTo !== appliesTo || newReplacementEffect !== replacementEffect)
            copy(appliesTo = newAppliesTo, replacementEffect = newReplacementEffect)
        else this
    }
}

/**
 * Modify life gain amount. Combines multiplicative and additive modifications:
 * `newAmount = (originalAmount * multiplier) + modifier`, clamped to ≥ 0.
 *
 * Examples:
 * - Alhammarret's Archive — double life gain: `ModifyLifeGain(multiplier = 2)`
 * - Leyline of Hope — "you gain that much life plus 1 instead":
 *     `ModifyLifeGain(modifier = 1, appliesTo = LifeGainEvent(player = Player.You))`
 *
 * @param multiplier Multiplicative factor applied first (default 2 to preserve the
 *        historical Alhammarret's Archive default).
 * @param modifier Flat amount added after multiplication (default 0 = unchanged).
 */
@SerialName("ModifyLifeGain")
@Serializable
data class ModifyLifeGain(
    val multiplier: Int = 2,
    val modifier: Int = 0,
    override val appliesTo: EventPattern = EventPattern.LifeGainEvent()
) : ReplacementEffect {
    override val description: String = buildString {
        append("If ")
        append(appliesTo.description)
        append(", gain ")
        when {
            multiplier == 0 && modifier == 0 -> append("no life")
            multiplier == 1 && modifier > 0 -> append("that much life plus $modifier")
            multiplier == 1 && modifier < 0 -> append("${-modifier} less life")
            multiplier != 1 && modifier == 0 -> when (multiplier) {
                2 -> append("twice that much life")
                else -> append("$multiplier times that much life")
            }
            else -> {
                when (multiplier) {
                    2 -> append("twice that much life")
                    else -> append("$multiplier times that much life")
                }
                if (modifier > 0) append(" plus $modifier") else append(" minus ${-modifier}")
            }
        }
        append(" instead")
    }

    override fun applyTextReplacement(replacer: TextReplacer): ReplacementEffect {
        val newAppliesTo = appliesTo.applyTextReplacement(replacer)
        return if (newAppliesTo !== appliesTo) copy(appliesTo = newAppliesTo) else this
    }
}

/**
 * Modify life loss amount. Combines multiplicative and additive modifications:
 * `newAmount = (originalAmount * multiplier) + modifier`, clamped to ≥ 0.
 *
 * Per the printed reminder text "(Damage causes loss of life.)" on Bloodletter of
 * Aclazotz, this replacement applies to life loss caused by damage as well as direct
 * life-loss effects. Lifelink and other damage-based triggers still see the original
 * damage amount — only the life total reduction is modified.
 *
 * The [restrictions] list lets cards layer arbitrary additional gates (e.g., "during
 * your turn", "while you control a Vampire") onto the replacement; the engine
 * evaluates each entry with the source permanent's controller as the [Condition]
 * context and only applies the modification when *all* restrictions hold.
 *
 * Examples:
 * - Bloodletter of Aclazotz (loses twice as much during your turn):
 *     `ModifyLifeLoss(multiplier = 2, restrictions = listOf(IsYourTurn),
 *                    appliesTo = LifeLossEvent(player = Player.Opponent))`
 * - "Each opponent loses an additional 1 life":
 *     `ModifyLifeLoss(modifier = 1, appliesTo = LifeLossEvent(player = Player.Opponent))`
 * - "If you would lose life, you lose 1 less life instead" (with floor at 0):
 *     `ModifyLifeLoss(modifier = -1, appliesTo = LifeLossEvent(player = Player.You))`
 *
 * @param multiplier Multiplicative factor applied first (default 1 = unchanged).
 * @param modifier Flat amount added after multiplication (default 0 = unchanged).
 * @param restrictions Additional [Condition]s gating when this replacement applies.
 *        Evaluated against the source permanent's controller; ALL must hold.
 */
@SerialName("ModifyLifeLoss")
@Serializable
data class ModifyLifeLoss(
    val multiplier: Int = 1,
    val modifier: Int = 0,
    val restrictions: List<com.wingedsheep.sdk.scripting.conditions.Condition> = emptyList(),
    override val appliesTo: EventPattern = EventPattern.LifeLossEvent()
) : ReplacementEffect {
    override val description: String = buildString {
        val restrictionDesc = restrictions.joinToString(" and ") { it.description.removePrefix("if ") }
        if (restrictionDesc.isNotEmpty()) {
            append(restrictionDesc.replaceFirstChar { it.uppercase() })
            append(", if ")
        } else {
            append("If ")
        }
        append(appliesTo.description)
        append(", they lose ")
        when {
            multiplier == 0 && modifier == 0 -> append("no life")
            multiplier == 1 && modifier > 0 -> append("that much life plus $modifier")
            multiplier == 1 && modifier < 0 -> append("${-modifier} less life")
            multiplier != 1 && modifier == 0 -> when (multiplier) {
                2 -> append("twice that much life")
                else -> append("$multiplier times that much life")
            }
            else -> {
                when (multiplier) {
                    2 -> append("twice that much life")
                    else -> append("$multiplier times that much life")
                }
                if (modifier > 0) append(" plus $modifier") else append(" minus ${-modifier}")
            }
        }
        append(" instead")
    }

    override fun applyTextReplacement(replacer: TextReplacer): ReplacementEffect {
        val newAppliesTo = appliesTo.applyTextReplacement(replacer)
        val newRestrictions = restrictions.map { it.applyTextReplacement(replacer) }
        val anyChanged = newAppliesTo !== appliesTo ||
            newRestrictions.zip(restrictions).any { (n, o) -> n !== o }
        return if (anyChanged) copy(appliesTo = newAppliesTo, restrictions = newRestrictions) else this
    }
}

// =============================================================================
// Copy Replacement Effects
// =============================================================================

/**
 * Enter the battlefield as a copy of a card or permanent.
 * Example: Clone ("You may have this creature enter as a copy of any creature on the battlefield")
 * Example: Clever Impersonator ("You may have this creature enter as a copy of any nonland permanent on the battlefield")
 * Example: Superior Spider-Man ("You may have this creature enter as a copy of any creature card in a
 *          graveyard, except his name is Superior Spider-Man and he's a 4/4 Spider Human Hero ... When
 *          you do, exile that card.")
 *
 * When this permanent would enter the battlefield, the controller may choose an object in [copyFromZone]
 * matching [copyFilter]. If they do, the permanent enters as a copy of that object (with the overrides
 * below applied). If they don't (or can't), the permanent enters as itself (typically 0/0 and dies).
 *
 * @param copyFilter Filter for what can be copied. Defaults to creatures only (Clone).
 *                   Use [GameObjectFilter.Companion.NonlandPermanent] for Clever Impersonator.
 * @param copyFromZone Where to look for copy candidates. [Zone.BATTLEFIELD] (default) copies a permanent;
 *                   [Zone.GRAVEYARD] copies a creature *card* from any graveyard (Superior Spider-Man).
 * @param filterByTotalManaSpent When true, only creatures with mana value ≤ total mana spent
 *                                to cast this spell are valid copy targets. Used for Mockingbird.
 * @param additionalSubtypes Subtypes to add to the copy (e.g., "Bird" for Mockingbird; "Spider", "Human",
 *                   "Hero" for Superior Spider-Man — added "in addition to its other types").
 * @param additionalKeywords Keywords to grant to the copy (e.g., FLYING for Mockingbird).
 * @param nameOverride When non-null, the copy keeps this name instead of the copied object's name
 *                   ("except his name is Superior Spider-Man").
 * @param powerOverride When non-null, the copy's base power is set to this value ("he's a 4/4 ...").
 * @param toughnessOverride When non-null, the copy's base toughness is set to this value.
 * @param exileCopiedCard When true, the copied card is exiled after the copy is applied
 *                   ("When you do, exile that card"). Only meaningful with [copyFromZone] = graveyard.
 */
@SerialName("EntersAsCopy")
@Serializable
data class EntersAsCopy(
    val optional: Boolean = true,
    val copyFilter: GameObjectFilter = GameObjectFilter.Creature,
    val copyFromZone: Zone = Zone.BATTLEFIELD,
    val filterByTotalManaSpent: Boolean = false,
    val additionalSubtypes: List<String> = emptyList(),
    val additionalKeywords: List<Keyword> = emptyList(),
    val nameOverride: String? = null,
    val powerOverride: Int? = null,
    val toughnessOverride: Int? = null,
    val exileCopiedCard: Boolean = false,
    override val appliesTo: EventPattern = EventPattern.ZoneChangeEvent(
        filter = GameObjectFilter.Any,
        to = Zone.BATTLEFIELD
    )
) : ReplacementEffect {
    override val description: String = run {
        val filterDesc = copyFilter.description
        val where = if (copyFromZone == Zone.GRAVEYARD) "$filterDesc card in a graveyard" else "$filterDesc on the battlefield"
        val lead = if (optional) {
            "You may have this creature enter as a copy of any $where"
        } else {
            "This creature enters as a copy of any $where"
        }
        buildString {
            append(lead)
            val exceptions = buildList {
                if (nameOverride != null) add("its name is $nameOverride")
                if (powerOverride != null && toughnessOverride != null) {
                    add("it's $powerOverride/$toughnessOverride")
                }
                if (additionalSubtypes.isNotEmpty()) {
                    add("a ${additionalSubtypes.joinToString(" ")} in addition to its other types")
                }
                if (additionalKeywords.isNotEmpty()) {
                    add("it has ${additionalKeywords.joinToString(", ") { it.name.lowercase() }}")
                }
            }
            if (exceptions.isNotEmpty()) append(", except ${exceptions.joinToString(" and ")}")
            if (exileCopiedCard) append(". When you do, exile that card")
        }
    }

    override fun applyTextReplacement(replacer: TextReplacer): ReplacementEffect {
        val newAppliesTo = appliesTo.applyTextReplacement(replacer)
        return if (newAppliesTo !== appliesTo) copy(appliesTo = newAppliesTo) else this
    }
}

// =============================================================================
// Enter-With-Choice Replacement Effects
// =============================================================================

/**
 * What the player chooses as the permanent enters.
 */
@Serializable
enum class ChoiceType {
    /** Choose a color (e.g., Riptide Replicator, Ward Sliver) */
    COLOR,
    /** Choose a creature type (e.g., Doom Cannon, Cover of Darkness) */
    CREATURE_TYPE,
    /** Choose another creature you control (e.g., Dauntless Bodyguard) */
    CREATURE_ON_BATTLEFIELD,
    /**
     * Choose one of a card-defined set of named [ModeOption]s. Used for cards
     * whose entry choice gates which abilities are active — e.g., the Khans
     * cycle of Sieges ("As this enters, choose Khans or Dragons"). The chosen
     * mode is stored on the permanent and queryable via
     * [com.wingedsheep.sdk.scripting.conditions.SourceChosenModeIs].
     */
    MODE,
    /**
     * Choose a basic land type (Plains, Island, Swamp, Mountain, or Forest)
     * (e.g., Phantasmal Terrain: "As this Aura enters, choose a basic land type").
     * The chosen type is stored on the permanent in a
     * [com.wingedsheep.engine.state.components.battlefield.CastChoicesComponent]
     * and read by [com.wingedsheep.sdk.scripting.SetEnchantedLandTypeFromChosen].
     */
    BASIC_LAND_TYPE
}

/**
 * A single named option in an [EntersWithChoice] of type [ChoiceType.MODE].
 *
 * The [id] is the stable, machine-readable identifier referenced by
 * [com.wingedsheep.sdk.scripting.conditions.SourceChosenModeIs] and stored
 * on the resulting [com.wingedsheep.engine.state.components.battlefield.CastChoicesComponent].
 * The [label] is the human-readable display text shown in the prompt.
 *
 * [description] supplies optional rules text shown alongside the label
 * (e.g., the corresponding ability's reminder text). [iconKey] is an
 * optional asset identifier the frontend can map to an SVG icon — cards
 * that do not supply an icon get a purely textual choice.
 */
@Serializable
data class ModeOption(
    val id: String,
    val label: String,
    val description: String? = null,
    val iconKey: String? = null
)

/**
 * As this permanent enters, make a choice. The chosen value is stored on
 * the permanent for use by other abilities.
 *
 * Replaces the former EntersWithColorChoice, EntersWithCreatureTypeChoice,
 * and EntersWithCreatureChoice with a single parameterized type.
 *
 * @param choiceType What kind of choice to present
 * @param chooser Who makes the choice (default: controller)
 *
 * Examples:
 * - Riptide Replicator: `EntersWithChoice(ChoiceType.COLOR)`
 * - Callous Oppressor: `EntersWithChoice(ChoiceType.CREATURE_TYPE, chooser = Player.Opponent)`
 * - Dauntless Bodyguard: `EntersWithChoice(ChoiceType.CREATURE_ON_BATTLEFIELD)`
 */
@SerialName("EntersWithChoice")
@Serializable
data class EntersWithChoice(
    val choiceType: ChoiceType,
    val chooser: Player = Player.You,
    /**
     * When [choiceType] is [ChoiceType.CREATURE_TYPE], restrict the choosable
     * subtypes to this list. `null` means any creature type is allowed (the
     * default, matching cards like Three Tree City). Used by cards that
     * enumerate a specific tribal shortlist such as Eclipsed Realms.
     */
    val allowedCreatureTypes: List<String>? = null,
    /**
     * When [choiceType] is [ChoiceType.MODE], the card-defined list of named
     * options the player picks between. Required for MODE; ignored otherwise.
     */
    val modeOptions: List<ModeOption> = emptyList(),
    override val appliesTo: EventPattern = EventPattern.ZoneChangeEvent(
        filter = GameObjectFilter.Any,
        to = Zone.BATTLEFIELD
    )
) : ReplacementEffect {
    override val description: String = when (choiceType) {
        ChoiceType.COLOR -> if (chooser == Player.Opponent) {
            "As this permanent enters, an opponent chooses a color"
        } else {
            "As this permanent enters, choose a color"
        }
        ChoiceType.CREATURE_TYPE -> if (chooser == Player.Opponent) {
            "As this permanent enters, an opponent chooses a creature type"
        } else {
            "As this permanent enters, choose a creature type"
        }
        ChoiceType.CREATURE_ON_BATTLEFIELD -> "As this creature enters, choose another creature you control"
        ChoiceType.MODE -> {
            val labels = modeOptions.joinToString(" or ") { it.label }
            if (chooser == Player.Opponent) {
                "As this permanent enters, an opponent chooses $labels"
            } else {
                "As this permanent enters, choose $labels"
            }
        }
        ChoiceType.BASIC_LAND_TYPE -> if (chooser == Player.Opponent) {
            "As this permanent enters, an opponent chooses a basic land type"
        } else {
            "As this permanent enters, choose a basic land type"
        }
    }

    override fun applyTextReplacement(replacer: TextReplacer): ReplacementEffect {
        val newAppliesTo = appliesTo.applyTextReplacement(replacer)
        return if (newAppliesTo !== appliesTo) copy(appliesTo = newAppliesTo) else this
    }
}

// =============================================================================
// Enters-With-Reveal-Counters Replacement Effect
// =============================================================================

/**
 * As this creature enters, you may reveal any number of cards from a zone
 * that match a filter. For each card revealed, put N counters on this creature.
 *
 * Generalizes the Amplify mechanic — the default parameters reproduce Amplify
 * exactly (reveal creatures from hand sharing a type, +1/+1 counters).
 *
 * @param filter Which cards can be revealed (default: creatures sharing a creature type with this)
 * @param revealSource Which zone to reveal from (default: HAND)
 * @param counterType Counter type description (default: "+1/+1")
 * @param countersPerReveal How many counters per revealed card
 *
 * Examples:
 * - Embalmed Brawler (Amplify 1): `EntersWithRevealCounters(countersPerReveal = 1)`
 * - Kilnmouth Dragon (Amplify 3): `EntersWithRevealCounters(countersPerReveal = 3)`
 */
@SerialName("EntersWithRevealCounters")
@Serializable
data class EntersWithRevealCounters(
    val filter: GameObjectFilter = GameObjectFilter(
        cardPredicates = listOf(CardPredicate.IsCreature, CardPredicate.SharesCreatureTypeWithSource)
    ),
    val revealSource: Zone = Zone.HAND,
    val counterType: String = "+1/+1",
    val countersPerReveal: Int,
    override val appliesTo: EventPattern = EventPattern.ZoneChangeEvent(
        filter = GameObjectFilter.Creature.youControl(),
        to = Zone.BATTLEFIELD
    )
) : ReplacementEffect {
    override val description: String =
        "As this creature enters, you may reveal any number of cards from your ${revealSource.name.lowercase()} that match. For each card revealed this way, put $countersPerReveal $counterType counter${if (countersPerReveal > 1) "s" else ""} on it."

    override fun applyTextReplacement(replacer: TextReplacer): ReplacementEffect {
        val newFilter = filter.applyTextReplacement(replacer)
        val newAppliesTo = appliesTo.applyTextReplacement(replacer)
        return if (newFilter !== filter || newAppliesTo !== appliesTo) copy(filter = newFilter, appliesTo = newAppliesTo) else this
    }
}

// =============================================================================
// Enters-With-Devour Replacement Effect
// =============================================================================

/**
 * Devour (CR 702.82) and its variants. As this permanent enters, the controller
 * may sacrifice any number of permanents matching [sacrificeFilter]. The
 * permanent then enters with [multiplier] × (count sacrificed) counters of
 * [counterType] on it.
 *
 * Devour is the rules-precise replacement effect generated by
 * [com.wingedsheep.sdk.scripting.KeywordAbility.Devour]. Cards that print the
 * Devour keyword should declare both: the [KeywordAbility] surfaces the
 * keyword for rules-text rendering, and this replacement effect supplies the
 * mechanical behavior.
 *
 * Examples:
 * - Plain Devour 2 (creatures): `EntersWithDevour(multiplier = 2)`
 * - Famished Worldsire — Devour land 3:
 *     `EntersWithDevour(multiplier = 3,
 *                       sacrificeFilter = GameObjectFilter.Land,
 *                       variant = "land")`
 *
 * @param multiplier Counters placed per sacrificed permanent.
 * @param sacrificeFilter Which permanents the controller may sacrifice. The
 *        engine restricts to permanents the controller controls regardless
 *        (CR 701.21a "to sacrifice a permanent, its controller moves it…").
 * @param counterType The counter type granted (default: +1/+1).
 * @param variant Optional rules-text variant ("" or "land") used in the
 *        description string. Mechanically inert.
 */
@SerialName("EntersWithDevour")
@Serializable
data class EntersWithDevour(
    val multiplier: Int,
    val sacrificeFilter: GameObjectFilter = GameObjectFilter.Creature,
    val counterType: com.wingedsheep.sdk.scripting.events.CounterTypeFilter =
        com.wingedsheep.sdk.scripting.events.CounterTypeFilter.PlusOnePlusOne,
    val variant: String = "",
    override val appliesTo: EventPattern = EventPattern.ZoneChangeEvent(
        filter = GameObjectFilter.Any,
        to = Zone.BATTLEFIELD
    )
) : ReplacementEffect {
    override val description: String = buildString {
        append("Devour")
        if (variant.isNotBlank()) {
            append(" ")
            append(variant)
        }
        append(" ")
        append(multiplier)
        append(" (As this creature enters, you may sacrifice any number of ")
        append(sacrificeFilter.description)
        append("s. It enters with ")
        append(multiplier)
        append(" times that many ")
        append(counterType.description)
        append(" counters on it.)")
    }

    override fun applyTextReplacement(replacer: TextReplacer): ReplacementEffect {
        val newAppliesTo = appliesTo.applyTextReplacement(replacer)
        val newFilter = sacrificeFilter.applyTextReplacement(replacer)
        return if (newAppliesTo !== appliesTo || newFilter !== sacrificeFilter)
            copy(appliesTo = newAppliesTo, sacrificeFilter = newFilter)
        else this
    }
}

// =============================================================================
// Damage-to-Counters Replacement Effect
// =============================================================================

/**
 * Replace damage dealt to a player with counters on this permanent.
 * Example: Force Bubble — "If damage would be dealt to you, put that many
 * depletion counters on this enchantment instead."
 *
 * @param counterType The type of counter to add (e.g., "depletion")
 * @param sacrificeThreshold If non-null, sacrifice this permanent when it has
 *        this many or more counters of the specified type (state-triggered ability)
 */
@SerialName("ReplaceDamageWithCounters")
@Serializable
data class ReplaceDamageWithCounters(
    val counterType: String,
    val sacrificeThreshold: Int? = null,
    override val appliesTo: EventPattern = EventPattern.DamageEvent(
        recipient = RecipientFilter.You
    )
) : ReplacementEffect {
    override val description: String = buildString {
        append("If ${appliesTo.description}, put that many $counterType counters on this permanent instead")
        if (sacrificeThreshold != null) {
            append(". When there are $sacrificeThreshold or more $counterType counters on this permanent, sacrifice it")
        }
    }

    override fun applyTextReplacement(replacer: TextReplacer): ReplacementEffect {
        val newAppliesTo = appliesTo.applyTextReplacement(replacer)
        return if (newAppliesTo !== appliesTo) copy(appliesTo = newAppliesTo) else this
    }
}

// =============================================================================
// Extra Turn Replacement Effects
// =============================================================================

/**
 * Prevent extra turns from being taken.
 * Example: Ugin's Nexus — "If a player would begin an extra turn, that player
 * skips that turn instead."
 *
 * Checked by TakeExtraTurnExecutor before granting extra turns.
 */
@SerialName("PreventExtraTurns")
@Serializable
data class PreventExtraTurns(
    override val appliesTo: EventPattern = EventPattern.ExtraTurnEvent()
) : ReplacementEffect {
    override val description: String =
        "If a player would begin an extra turn, that player skips that turn instead"

    override fun applyTextReplacement(replacer: TextReplacer): ReplacementEffect {
        val newAppliesTo = appliesTo.applyTextReplacement(replacer)
        return if (newAppliesTo !== appliesTo) copy(appliesTo = newAppliesTo) else this
    }
}

/**
 * Redirect a zone change to a different destination AND execute an additional effect.
 * Example: Ugin's Nexus — "If Ugin's Nexus would be put into a graveyard from
 * the battlefield, instead exile it and take an extra turn after this one."
 *
 * Extends RedirectZoneChange with an additional effect that fires when the replacement applies.
 *
 * @param newDestination The zone to redirect to (e.g., Exile)
 * @param additionalEffect The effect to execute when replacement fires (e.g., TakeExtraTurnEffect)
 * @param selfOnly When true, only applies when the entity being moved IS this permanent
 * @param appliesTo The zone change event this replacement intercepts
 */
@SerialName("RedirectZoneChangeWithEffect")
@Serializable
data class RedirectZoneChangeWithEffect(
    val newDestination: Zone,
    val additionalEffect: Effect,
    val selfOnly: Boolean = false,
    override val appliesTo: EventPattern
) : ReplacementEffect {
    override val description: String =
        "If ${appliesTo.description}, instead put it into ${newDestination.displayName} and ${additionalEffect.description}"

    override fun applyTextReplacement(replacer: TextReplacer): ReplacementEffect {
        val newAppliesTo = appliesTo.applyTextReplacement(replacer)
        val newAdditionalEffect = additionalEffect.applyTextReplacement(replacer)
        return if (newAppliesTo !== appliesTo || newAdditionalEffect !== additionalEffect)
            copy(appliesTo = newAppliesTo, additionalEffect = newAdditionalEffect)
        else this
    }
}

// =============================================================================
// Token Creation Replacement Effects
// =============================================================================

/**
 * Replace token creation with creating token copies of the permanent this source is attached to.
 *
 * Works for both Equipment (attached creature) and Auras (enchanted artifact / creature / etc.).
 * The source must have an [com.wingedsheep.engine.state.components.battlefield.AttachedToComponent]
 * — the engine looks at that to find the permanent to copy.
 *
 * Examples:
 * - Mirrormind Crown — "As long as this Equipment is attached to a creature, the first time
 *   you would create one or more tokens each turn, you may instead create that many tokens
 *   that are copies of equipped creature."
 *     `ReplaceTokenCreationWithAttachedCopy(attachmentVerb = "equipped")`
 * - Moonlit Meditation — "Enchant artifact or creature you control. The first time you would
 *   create one or more tokens each turn, you may instead create that many tokens that are
 *   copies of enchanted permanent."
 *     `ReplaceTokenCreationWithAttachedCopy(attachmentVerb = "enchanted")`
 *
 * @param optional If true, the player may choose whether to apply the replacement ("you may")
 * @param oncePerTurn If true, only applies to the first token creation each turn
 * @param attachmentVerb Word used in the description for the attached permanent
 *        (e.g. "equipped", "enchanted", "fortified"). Display-only — behavior is driven
 *        entirely by the source's [com.wingedsheep.engine.state.components.battlefield.AttachedToComponent]
 *        and validated at cast / attach time by auraTarget / equipmentTarget.
 */
@SerialName("ReplaceTokenCreationWithAttachedCopy")
@Serializable
data class ReplaceTokenCreationWithAttachedCopy(
    val optional: Boolean = true,
    val oncePerTurn: Boolean = true,
    val attachmentVerb: String = "attached",
    override val appliesTo: EventPattern = EventPattern.TokenCreationEvent()
) : ReplacementEffect {
    override val description: String = buildString {
        if (oncePerTurn) append("The first time ")
        else append("If ")
        append("you would create one or more tokens")
        if (oncePerTurn) append(" each turn")
        append(", ")
        if (optional) append("you may instead ")
        else append("instead ")
        append("create that many tokens that are copies of ")
        append(attachmentVerb)
        append(" permanent")
    }

    override fun applyTextReplacement(replacer: TextReplacer): ReplacementEffect {
        val newAppliesTo = appliesTo.applyTextReplacement(replacer)
        return if (newAppliesTo !== appliesTo) copy(appliesTo = newAppliesTo) else this
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
    override val appliesTo: EventPattern,
    override val description: String
) : ReplacementEffect {
    override fun applyTextReplacement(replacer: TextReplacer): ReplacementEffect {
        val newAppliesTo = appliesTo.applyTextReplacement(replacer)
        val newReplacement = replacement?.applyTextReplacement(replacer)
        return if (newAppliesTo !== appliesTo || newReplacement !== replacement)
            copy(appliesTo = newAppliesTo, replacement = newReplacement)
        else this
    }
}
