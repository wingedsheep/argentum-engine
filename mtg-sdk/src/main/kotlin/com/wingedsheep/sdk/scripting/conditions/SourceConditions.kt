package com.wingedsheep.sdk.scripting.conditions

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.events.CounterTypeFilter
import com.wingedsheep.sdk.scripting.text.TextReplacer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// =============================================================================
// Source Conditions
//
// "The source permanent matches [filter]" lives in the unified [EntityMatches]
// condition (EntityMatchesCondition.kt) as `EntityMatches(EffectTarget.Self, filter)`,
// reached through the `Conditions.SourceMatches` / `Conditions.SourceIs*` facade helpers.
// =============================================================================

/**
 * Condition: "If you control this permanent"
 * Checks whether the effect's controllerId matches the source's controller.
 * Used in contexts where controllerId is overridden (e.g., per-player iteration)
 * to gate effects that should only apply to the source's actual controller.
 */
@SerialName("YouControlSource")
@Serializable
data object YouControlSource : Condition {
    override val description: String = "if you control this permanent"
}

/**
 * Condition: "it's [threshold] or less of the controller's turn of the game".
 *
 * True when the controller (resolved through context) has taken at most
 * [threshold] turns so far — i.e. `PlayerTurnsTakenComponent.count <= threshold`.
 * Note the counter is incremented at turn start, so during the controller's
 * first turn it reads 1, not 0.
 *
 * Used by cards like Starting Town: "this land enters tapped unless it's your
 * first, second, or third turn of the game" — that's
 * `ControllerTurnsTakenAtMost(3)`.
 */
@SerialName("ControllerTurnsTakenAtMost")
@Serializable
data class ControllerTurnsTakenAtMost(val threshold: Int) : Condition {
    init {
        require(threshold >= 1) {
            "ControllerTurnsTakenAtMost threshold must be >= 1 (turns are 1-indexed)"
        }
    }
    override val description: String = "it's your ${turnsOrdinal(threshold)} turn of the game"

    companion object {
        private fun turnsOrdinal(n: Int): String = when (n) {
            1 -> "first"
            2 -> "first or second"
            3 -> "first, second, or third"
            4 -> "first, second, third, or fourth"
            5 -> "first, second, third, fourth, or fifth"
            // Above 5 the printed phrasings switch to "first through Nth" wording —
            // accept any int and fall back to that compact form.
            else -> "first through ${n}${ordinalSuffix(n)}"
        }

        private fun ordinalSuffix(n: Int): String {
            val mod100 = n % 100
            if (mod100 in 11..13) return "th"
            return when (n % 10) {
                1 -> "st"
                2 -> "nd"
                3 -> "rd"
                else -> "th"
            }
        }
    }
}

/**
 * Condition: "if this creature is your Ring-bearer" (CR 701.54e).
 *
 * True when the source permanent is on the battlefield under the ability's controller and has the
 * Ring-bearer designation for that player. Used by cards whose effects key off "your Ring-bearer".
 */
@SerialName("SourceIsRingBearer")
@Serializable
data object SourceIsRingBearer : Condition {
    override val description: String = "if this creature is your Ring-bearer"
}

/**
 * Condition: "if you chose a creature other than this as your Ring-bearer" (CR 701.54a).
 *
 * Pairs with `Triggers.RingTemptsYou` as an intervening-if on cards whose payoff fires only when
 * the player picked someone other than the source — Aragorn (Company Leader), Faramir (Field
 * Commander), Gandalf (Friend of the Shire), Galadriel of Lothlórien. True when the ability's
 * controller currently has a Ring-bearer AND that Ring-bearer isn't the source permanent. If the
 * controller had no creatures to choose from (no bearer exists), the condition is false — they
 * didn't choose any creature, so they didn't choose one other than the source.
 */
@SerialName("YouChoseOtherCreatureAsRingBearer")
@Serializable
data object YouChoseOtherCreatureAsRingBearer : Condition {
    override val description: String = "if you chose a creature other than this as your Ring-bearer"
}

/**
 * Condition: "As long as this permanent is modified"
 *
 * Per CR 700.4, a permanent is modified if it has one or more counters on it, one or more
 * Equipment attached, or is enchanted by one or more Auras its controller controls. Kept
 * as a dedicated condition because the controller-of-source / controller-of-Aura match
 * isn't expressible via the generic `SourceMatches` filter machinery.
 */
@SerialName("SourceIsModified")
@Serializable
data object SourceIsModified : Condition {
    override val description: String = "this permanent is modified"
}

/**
 * Condition: "if you put a counter on this creature this turn".
 *
 * True iff one or more counters have been placed on the source permanent during the
 * current turn — tracked by the per-permanent `ReceivedCountersThisTurnComponent`, which
 * the counter-placement path stamps and the cleanup step clears each turn. Used by
 * Secrets of Strixhaven's Fractal Tender end-step trigger ("if you put a counter on this
 * creature this turn, …"), and reusable by any "if a counter was put on this permanent
 * this turn" intervening-if.
 */
@SerialName("SourceReceivedCounterThisTurn")
@Serializable
data object SourceReceivedCounterThisTurn : Condition {
    override val description: String = "you put a counter on this creature this turn"
}

/**
 * Condition: "If you cast this spell from your hand"
 * Used for Phage the Untouchable's ETB trigger condition.
 * Checks whether the source permanent was cast from the hand (as opposed to
 * being put onto the battlefield by another effect).
 */
@SerialName("WasCastFromHand")
@Serializable
data object WasCastFromHand : Condition {
    override val description: String = "you cast it from your hand"
}

/**
 * Condition: "If you cast this spell" (from any zone).
 * Used as an intervening-if condition on ETB triggers like Sunderflock's
 * ("if you cast it"). True iff the source permanent entered the battlefield via
 * stack resolution of a cast — i.e., it has a CastFromHand or CastFromGraveyard
 * marker. False if the permanent was put onto the battlefield by another effect
 * (e.g., reanimation, Show and Tell, token creation).
 */
@SerialName("WasCast")
@Serializable
data object WasCast : Condition {
    override val description: String = "you cast it"
}

/**
 * Condition: "if it wasn't cast or no mana was spent to cast it" — the standard
 * free-cast payoff clause (Freestrider Commando, Satoru, the Infiltrator, etc.).
 *
 * True iff **no mana at all** was spent to put the source onto the battlefield:
 * - it was put onto the battlefield without being cast (reanimation, token, "put onto
 *   the battlefield"), **or**
 * - it was cast but its total mana payment was zero (e.g. a plotted card cast for free,
 *   or a {0}-cost spell).
 *
 * False if any mana was spent — including mana paid for additional costs or cost
 * increases on an otherwise-free cast (per the Freestrider Commando ruling: a plotted
 * spell taxed by Aven Interrupter had mana spent, so it does *not* qualify).
 *
 * Implementation reads the source's cast-mana record: the engine only stamps that record
 * when the total mana spent to cast was greater than zero, so its absence (or a zero
 * total) is exactly "no mana was spent." This single condition covers the whole oracle
 * clause; compose `All(WasCast, NoManaSpentToCast)` for the narrower "cast for free"
 * sense that excludes uncast permanents.
 */
@SerialName("NoManaSpentToCast")
@Serializable
data object NoManaSpentToCast : Condition {
    override val description: String = "it wasn't cast or no mana was spent to cast it"
}

/**
 * Condition: "if none of them were cast or no mana was spent to cast them" — the batch-enters
 * variant of [NoManaSpentToCast], evaluated over the permanents a batch trigger captured
 * (the `PermanentsEnteredEvent` batch that caused the trigger, exposed at resolution as the
 * `trigger.captured` pipeline collection) rather than the ability's own source.
 *
 * True iff **every** captured permanent satisfies [NoManaSpentToCast] (was put onto the
 * battlefield without being cast, or was cast with zero total mana spent). An empty capture is
 * vacuously true. Use as a resolution-time gate ([ConditionalEffect]) on a
 * [com.wingedsheep.sdk.dsl.Triggers.OneOrMorePermanentsEnter] payoff — Satoru, the Infiltrator
 * ("Whenever Satoru and/or one or more other nontoken creatures you control enter, if none of
 * them were cast or no mana was spent to cast them, draw a card.").
 */
@SerialName("NoManaSpentToCastEntered")
@Serializable
data object NoManaSpentToCastEntered : Condition {
    override val description: String = "none of them were cast or no mana was spent to cast them"
}

/**
 * Condition: "If this spell was cast from [zone]"
 * Used for flashback spells and other zone-dependent effects.
 * Checks whether the spell was cast from the specified zone.
 */
@SerialName("WasCastFromZone")
@Serializable
data class WasCastFromZone(val zone: Zone) : Condition {
    override val description: String = "this spell was cast from a ${zone.displayName.lowercase()}"
}

/**
 * Condition: "If this spell was kicked"
 * Used for kicker spells like Shivan Fire where the effect changes based on
 * whether the kicker cost was paid.
 */
@SerialName("WasKicked")
@Serializable
data object WasKicked : Condition {
    override val description: String = "this spell was kicked"
}

/**
 * Condition: "If this spell's sneak cost was paid" (CR 702.190).
 *
 * True when the source spell/permanent was cast for its [Sneak][com.wingedsheep.sdk.scripting.KeywordAbility.Sneak]
 * cost (mana + returning an unblocked attacker). Pairs with the durable
 * [com.wingedsheep.sdk.scripting.ChoiceSlot.SNEAK] flag the engine stamps on a resolved
 * permanent and the `wasSneaked` flag carried in the resolution context for a non-permanent
 * spell. Used by riders such as Leonardo, Leader in Blue and The Last Ronin's Technique that
 * change behavior when the sneak cost was paid.
 */
@SerialName("SneakCostWasPaid")
@Serializable
data object SneakCostWasPaid : Condition {
    override val description: String = "its sneak cost was paid"
}

/**
 * Condition: "If this spell's blight additional cost was paid"
 * Used for Lorwyn Eclipsed cards (e.g., Cinder Strike) where the effect changes
 * based on whether the optional Blight additional cost was actually paid.
 *
 * Pairs with [com.wingedsheep.sdk.scripting.AdditionalCost.BlightOrPay] — true when
 * the blight path was chosen during casting (a creature was selected and given
 * -1/-1 counters as part of the cost), false when the spell was cast without
 * paying blight.
 */
@SerialName("BlightWasPaid")
@Serializable
data object BlightWasPaid : Condition {
    override val description: String = "this spell's additional cost was paid"
}

/**
 * Condition: "If {W}{W} was spent to cast it" (mana-spent gating)
 * Used for Lorwyn Incarnation cycle (Catharsis, Deceit, Emptiness, etc.)
 * where ETB triggers are gated on specific mana colors spent to cast.
 *
 * Checks the CastRecordComponent on the permanent for per-color mana spent.
 * Each pip in [requiredWhite], [requiredBlue], etc. must have been spent.
 */
@SerialName("ManaSpentToCastIncludes")
@Serializable
data class ManaSpentToCastIncludes(
    val requiredWhite: Int = 0,
    val requiredBlue: Int = 0,
    val requiredBlack: Int = 0,
    val requiredRed: Int = 0,
    val requiredGreen: Int = 0
) : Condition {
    override val description: String = buildString {
        append("if ")
        val parts = mutableListOf<String>()
        repeat(requiredWhite) { parts.add("{W}") }
        repeat(requiredBlue) { parts.add("{U}") }
        repeat(requiredBlack) { parts.add("{B}") }
        repeat(requiredRed) { parts.add("{R}") }
        repeat(requiredGreen) { parts.add("{G}") }
        append(parts.joinToString(""))
        append(" was spent to cast it")
    }
}

/**
 * Condition: "If this permanent's impending cost was paid" (CR 702.176a).
 *
 * Reads the `CastForImpendingComponent` marker stamped on the permanent when it
 * resolved from an impending cast. Dual-mode: works in both static-ability
 * projection (gating the "isn't a creature" type-removing static) and resolution
 * (gating the end-step time-counter-removal trigger). Pair with
 * `SourceHasCounter(TIME)` via `AllConditions` to recover the printed
 * "impending cost was paid AND has a time counter" gate.
 */
@SerialName("SourceCastForImpending")
@Serializable
data object SourceCastForImpending : Condition {
    override val description: String = "this permanent's impending cost was paid"
}

/**
 * Condition: "If the chosen mode is [modeId]".
 *
 * Reads the `CastChoicesComponent` stored on the source permanent (set by an
 * `EntersWithChoice(ChoiceType.MODE,...)` replacement effect). Used to gate
 * triggered or static abilities on a card-defined named choice — e.g., a
 * Siege whose upkeep trigger only fires when "Khans" was chosen.
 *
 * @property modeId The mode id to match against the stored chosen mode.
 */
@SerialName("SourceChosenModeIs")
@Serializable
data class SourceChosenModeIs(val modeId: String) : Condition {
    override val description: String = "if the chosen mode is \"$modeId\""
}

/**
 * Condition: a value was locked in for [slot] when the source was cast / as it entered.
 *
 * The generic "was this choice made" guard (mtgish's `AColorWasChosen`), reading the durable
 * cast-choices bag on the source. Works at both resolution and projection. Use for any
 * [com.wingedsheep.sdk.scripting.ChoiceSlot] — e.g. `CastChoiceMade(ChoiceSlot.COLOR)` gates an
 * effect on "a color was chosen", `CastChoiceMade(ChoiceSlot.KICKED)` is "this spell was kicked".
 *
 * @property slot Which cast-choice slot must hold a value.
 */
@SerialName("CastChoiceMade")
@Serializable
data class CastChoiceMade(val slot: com.wingedsheep.sdk.scripting.ChoiceSlot) : Condition {
    override val description: String = "if a ${slot.name.lowercase().replace('_', ' ')} was chosen"
}

/**
 * Condition: the value locked in for [slot] equals [value] (compared as text).
 *
 * The generic slot reader that subsumes per-slot conditions like [SourceChosenModeIs] for new
 * cards — `CastChoiceIs(ChoiceSlot.MODE, "Khans")`, `CastChoiceIs(ChoiceSlot.COLOR, "RED")`,
 * `CastChoiceIs(ChoiceSlot.CREATURE_TYPE, "Goblin")`. Color values compare against the color's
 * enum name. Works at both resolution and projection.
 *
 * @property slot Which cast-choice slot to read.
 * @property value The expected value, as text (mode id, creature/land type, or color enum name).
 */
@SerialName("CastChoiceIs")
@Serializable
data class CastChoiceIs(
    val slot: com.wingedsheep.sdk.scripting.ChoiceSlot,
    val value: String
) : Condition {
    override val description: String = "if the chosen ${slot.name.lowercase().replace('_', ' ')} is \"$value\""
}

/**
 * Condition: the named cast-time capture [flag] was true *as the source spell was cast* (CR 601.2i).
 *
 * The reader half of the cast-time condition-capture mechanic
 * ([com.wingedsheep.sdk.scripting.CastTimeCapture]). A spell declares, via the `captureAtCast`
 * DSL, one or more named conditions evaluated the moment it finishes being cast; the engine records
 * the names that were true onto the spell on the stack. At resolution the spell's own effect reads
 * the recorded flag through this condition, so "deals 4 damage instead if you controlled a Mount as
 * you cast this spell" (Steer Clear) stays true even if the Mount has since left — the read is of
 * the frozen cast-time answer, not the current board.
 *
 * Distinct from the player-choice slot conditions [CastChoiceMade] / [CastChoiceIs]: those read a
 * value the player *chose* (a color, a mode); this reads whether an automatically-evaluated game
 * condition *held* at cast time.
 *
 * @property flag The capture name to read (matches the name given to `captureAtCast`).
 */
@SerialName("CastTimeFlagSet")
@Serializable
data class CastTimeFlagSet(val flag: String) : Condition {
    override val description: String = "if \"$flag\" was true as this spell was cast"
}

/**
 * Condition: "If a [subtype] was sacrificed this way"
 * Checks whether any permanent sacrificed as part of the cost had the given subtype
 * (using projected subtypes snapshotted at time of sacrifice).
 *
 * Used for cards like Thallid Omnivore: "If a Saproling was sacrificed this way, you gain 2 life."
 */
@SerialName("SacrificedPermanentHadSubtype")
@Serializable
data class SacrificedPermanentHadSubtype(val subtype: String) : Condition {
    override val description: String = "if a $subtype was sacrificed this way"
    override fun applyTextReplacement(replacer: TextReplacer): Condition {
        val newSubtype = replacer.replaceSubtype(Subtype(subtype))
        return if (newSubtype.value == subtype) this else SacrificedPermanentHadSubtype(newSubtype.value)
    }
}

/**
 * Condition: "If the sacrificed permanent was legendary."
 *
 * Reads `EffectContext.sacrificedPermanents` (snapshots captured at cost-payment time
 * or by a same-spell sacrifice effect like a symmetric edict) and matches when at
 * least one snapshot's projected supertypes contain `LEGENDARY`. Snapshotting the
 * supertype set at sacrifice time means a "became legendary" continuous effect that
 * wore off between sacrifice and resolution still counts — the legendary fact is
 * frozen at the moment of payment.
 *
 * Used by LTR's sacrifice-rider cards:
 *  - Nasty End ("If the sacrificed creature was legendary, draw three cards instead.")
 *  - Gríma Wormtongue ("If the sacrificed creature was legendary, amass Orcs 2.")
 */
@SerialName("SacrificedPermanentWasLegendary")
@Serializable
data object SacrificedPermanentWasLegendary : Condition {
    override val description: String = "if the sacrificed creature was legendary"
}

/**
 * Condition: "If you sacrificed a permanent this way."
 *
 * Reads `EffectContext.sacrificedPermanents` and matches when at least one snapshot
 * was controlled by the source's controller at the moment of sacrifice. Used for
 * symmetric edict riders where each player sacrifices and the controller's branch
 * has a follow-up effect.
 *
 * Used by LTR's Rise of the Witch-king: "Each player sacrifices a creature of their
 * choice. If you sacrificed a creature this way, you may return another permanent
 * card from your graveyard to the battlefield."
 */
@SerialName("YouSacrificedPermanentThisWay")
@Serializable
data object YouSacrificedPermanentThisWay : Condition {
    override val description: String = "if you sacrificed a creature this way"
}
