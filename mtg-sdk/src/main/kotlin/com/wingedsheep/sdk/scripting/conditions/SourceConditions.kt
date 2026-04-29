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
    override fun applyTextReplacement(replacer: TextReplacer): Condition = this
}

/**
 * Condition: "If this creature is attacking"
 */
@SerialName("SourceIsAttacking")
@Serializable
data object SourceIsAttacking : Condition {
    override val description: String = "if this creature is attacking"
    override fun applyTextReplacement(replacer: TextReplacer): Condition = this
}

/**
 * Condition: "If this creature is blocking"
 */
@SerialName("SourceIsBlocking")
@Serializable
data object SourceIsBlocking : Condition {
    override val description: String = "if this creature is blocking"
    override fun applyTextReplacement(replacer: TextReplacer): Condition = this
}

/**
 * Condition: "If this creature is tapped"
 */
@SerialName("SourceIsTapped")
@Serializable
data object SourceIsTapped : Condition {
    override val description: String = "if this creature is tapped"
    override fun applyTextReplacement(replacer: TextReplacer): Condition = this
}

/**
 * Condition: "If this creature is untapped"
 */
@SerialName("SourceIsUntapped")
@Serializable
data object SourceIsUntapped : Condition {
    override val description: String = "if this creature is untapped"
    override fun applyTextReplacement(replacer: TextReplacer): Condition = this
}

/**
 * Condition: "If this creature has dealt damage"
 * Used for cards like Karakyk Guardian: "has hexproof if it hasn't dealt damage yet"
 *
 * The engine tracks damage dealt history per-object since entering the battlefield.
 */
@SerialName("SourceHasDealtDamage")
@Serializable
data object SourceHasDealtDamage : Condition {
    override val description: String = "this creature has dealt damage"
    override fun applyTextReplacement(replacer: TextReplacer): Condition = this
}

/**
 * Condition: "As long as this permanent is modified"
 * A permanent is modified if it has one or more counters on it, has one or more
 * Equipment attached to it, or is enchanted by one or more Auras its controller controls.
 * Used for Neon Dynasty-style cards like Skyward Spider.
 */
@SerialName("SourceIsModified")
@Serializable
data object SourceIsModified : Condition {
    override val description: String = "this permanent is modified"
    override fun applyTextReplacement(replacer: TextReplacer): Condition = this
}

/**
 * Condition: "If this creature has dealt combat damage to a player"
 * Used for Saboteur abilities and similar effects.
 */
@SerialName("SourceHasDealtCombatDamageToPlayer")
@Serializable
data object SourceHasDealtCombatDamageToPlayer : Condition {
    override val description: String = "this creature has dealt combat damage to a player"
    override fun applyTextReplacement(replacer: TextReplacer): Condition = this
}

/**
 * Condition: "If this creature entered the battlefield this turn"
 * Used for summoning sickness checks and ETB-sensitive abilities.
 */
@SerialName("SourceEnteredThisTurn")
@Serializable
data object SourceEnteredThisTurn : Condition {
    override val description: String = "this creature entered the battlefield this turn"
    override fun applyTextReplacement(replacer: TextReplacer): Condition = this
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
    override fun applyTextReplacement(replacer: TextReplacer): Condition = this
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
    override fun applyTextReplacement(replacer: TextReplacer): Condition = this
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
    override fun applyTextReplacement(replacer: TextReplacer): Condition = this
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
    override fun applyTextReplacement(replacer: TextReplacer): Condition = this
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
    override fun applyTextReplacement(replacer: TextReplacer): Condition = this
}

/**
 * Condition: "As long as this creature is a [subtype]"
 * Used for cards like Mistform Wall: "This creature has defender as long as it's a Wall."
 *
 * Evaluated during state projection against projected subtypes, so type-changing
 * effects in Layer 4 are properly accounted for when checking conditions in Layer 6.
 */
@SerialName("SourceHasSubtype")
@Serializable
data class SourceHasSubtype(val subtype: Subtype) : Condition {
    override val description: String = "as long as this creature is a ${subtype.value}"
    override fun applyTextReplacement(replacer: TextReplacer): Condition {
        val new = replacer.replaceSubtype(subtype)
        return if (new == subtype) this else SourceHasSubtype(new)
    }
}

/**
 * Condition: "As long as this creature has [keyword]"
 * Used for cards with conditional effects based on keywords, e.g.,
 * "If this creature has flying, it gets +1/+1."
 *
 * Evaluated during state projection against projected keywords, so ability-granting
 * effects in Layer 6 are properly accounted for.
 */
@SerialName("SourceHasKeyword")
@Serializable
data class SourceHasKeyword(val keyword: Keyword) : Condition {
    override val description: String = "as long as this creature has ${keyword.name.lowercase()}"
    override fun applyTextReplacement(replacer: TextReplacer): Condition = this
}

/**
 * Condition: "While this creature has a [counter type] counter on it"
 * Used for intervening-if triggers like Moonshadow that only fire while a specific
 * counter is present on the source permanent.
 */
@SerialName("SourceHasCounter")
@Serializable
data class SourceHasCounter(val counterType: CounterTypeFilter) : Condition {
    override val description: String = "while this creature has a ${counterType.description} counter on it"
    override fun applyTextReplacement(replacer: TextReplacer): Condition = this
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
