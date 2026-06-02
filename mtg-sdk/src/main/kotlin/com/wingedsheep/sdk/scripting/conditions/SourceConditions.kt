package com.wingedsheep.sdk.scripting.conditions

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.events.CounterTypeFilter
import com.wingedsheep.sdk.scripting.text.TextReplacer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// =============================================================================
// Source Matching Primitive
// =============================================================================

/**
 * Condition: the source permanent matches [filter].
 *
 * Generic source-state primitive that subsumes the older singleton conditions
 * (`SourceIsAttacking`, `SourceIsTapped`, `SourceHasSubtype`, `SourceHasKeyword`,
 * `SourceHasCounter`, etc.). The engine evaluates by running [filter] against
 * the source entity via the standard predicate evaluator — works in both
 * resolution and static-ability (projection) contexts.
 *
 * Card authors should prefer the `Conditions.*` DSL helpers, which build the
 * appropriate filter for common cases (`Conditions.SourceIsAttacking`,
 * `Conditions.SourceHasSubtype(Subtype.WALL)`, etc.).
 */
@SerialName("SourceMatches")
@Serializable
data class SourceMatches(val filter: GameObjectFilter) : Condition {
    override val description: String =
        if (filter == GameObjectFilter.Any) "if this permanent matches"
        else "if this ${filter.description}"
    override fun applyTextReplacement(replacer: TextReplacer): Condition {
        val newFilter = filter.applyTextReplacement(replacer)
        return if (newFilter !== filter) copy(filter = newFilter) else this
    }
}

// =============================================================================
// Source Conditions
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
 * Condition: "if this creature is your Ring-bearer" (CR 701.52e).
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
 * Reads the `ChosenModeComponent` stored on the source permanent (set by an
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
