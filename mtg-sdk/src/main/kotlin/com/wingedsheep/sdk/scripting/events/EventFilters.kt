package com.wingedsheep.sdk.scripting.events

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.GameObjectFilter
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// =============================================================================
// Recipient Filters - Who/what receives damage or effects
// =============================================================================

/**
 * Filter for damage/effect recipients.
 */
@Serializable
sealed interface RecipientFilter {
    val description: String

    @SerialName("RecipientAny")
    @Serializable
    data object Any : RecipientFilter {
        override val description = "any target"
    }

    @SerialName("You")
    @Serializable
    data object You : RecipientFilter {
        override val description = "you"
    }

    @SerialName("RecipientOpponent")
    @Serializable
    data object Opponent : RecipientFilter {
        override val description = "an opponent"
    }

    @SerialName("AnyPlayer")
    @Serializable
    data object AnyPlayer : RecipientFilter {
        override val description = "a player"
    }

    @SerialName("AnyPlayerOrPlaneswalker")
    @Serializable
    data object AnyPlayerOrPlaneswalker : RecipientFilter {
        override val description = "a player or planeswalker"
    }

    @SerialName("CreatureYouControl")
    @Serializable
    data object CreatureYouControl : RecipientFilter {
        override val description = "a creature you control"
    }

    @SerialName("CreatureOpponentControls")
    @Serializable
    data object CreatureOpponentControls : RecipientFilter {
        override val description = "a creature an opponent controls"
    }

    @SerialName("AnyCreature")
    @Serializable
    data object AnyCreature : RecipientFilter {
        override val description = "a creature"
    }

    @SerialName("PermanentYouControl")
    @Serializable
    data object PermanentYouControl : RecipientFilter {
        override val description = "a permanent you control"
    }

    @SerialName("AnyPermanent")
    @Serializable
    data object AnyPermanent : RecipientFilter {
        override val description = "a permanent"
    }

    @SerialName("RecipientSelf")
    @Serializable
    data object Self : RecipientFilter {
        override val description = "this permanent"
    }

    @SerialName("RecipientEnchantedCreature")
    @Serializable
    data object EnchantedCreature : RecipientFilter {
        override val description = "enchanted creature"
    }

    @SerialName("RecipientEquippedCreature")
    @Serializable
    data object EquippedCreature : RecipientFilter {
        override val description = "equipped creature"
    }

    @SerialName("RecipientMatching")
    @Serializable
    data class Matching(val filter: GameObjectFilter) : RecipientFilter {
        override val description = filter.description
    }
}

// =============================================================================
// Source Filters - Where damage or effects come from
// =============================================================================

/**
 * Filter for damage/effect sources.
 */
@Serializable
sealed interface SourceFilter {
    val description: String

    @SerialName("SourceAny")
    @Serializable
    data object Any : SourceFilter {
        override val description = "any source"
    }

    @SerialName("SourceCombat")
    @Serializable
    data object Combat : SourceFilter {
        override val description = "combat"
    }

    @SerialName("SourceNonCombat")
    @Serializable
    data object NonCombat : SourceFilter {
        override val description = "a non-combat source"
    }

    @SerialName("Spell")
    @Serializable
    data object Spell : SourceFilter {
        override val description = "a spell"
    }

    @SerialName("Ability")
    @Serializable
    data object Ability : SourceFilter {
        override val description = "an ability"
    }

    @SerialName("HasColor")
    @Serializable
    data class HasColor(val color: Color) : SourceFilter {
        override val description = "a ${color.name.lowercase()} source"
    }

    @SerialName("HasType")
    @Serializable
    data class HasType(val type: String) : SourceFilter {
        override val description = "a $type"
    }

    @SerialName("SourceEnchantedCreature")
    @Serializable
    data object EnchantedCreature : SourceFilter {
        override val description = "enchanted creature"
    }

    @SerialName("SourceCreature")
    @Serializable
    data object Creature : SourceFilter {
        override val description = "a creature"
    }

    @SerialName("SourceMatching")
    @Serializable
    data class Matching(val filter: GameObjectFilter) : SourceFilter {
        override val description = filter.description
    }
}

// =============================================================================
// Damage Type - Classification of damage
// =============================================================================

/**
 * Damage type classification.
 */
@Serializable
sealed interface DamageType {
    val description: String

    @SerialName("DamageAny")
    @Serializable
    data object Any : DamageType {
        override val description = ""
    }

    @SerialName("DamageCombat")
    @Serializable
    data object Combat : DamageType {
        override val description = "combat"
    }

    @SerialName("DamageNonCombat")
    @Serializable
    data object NonCombat : DamageType {
        override val description = "noncombat"
    }
}

// =============================================================================
// Amount Filters - Threshold on the amount of a quantitative event (e.g. damage)
// =============================================================================

/**
 * Filter on the *amount* of a quantitative game event — currently the amount of
 * damage a [EventPattern.DamageEvent] would deal. Lets a replacement effect apply only
 * when the would-be amount crosses a threshold, without baking the threshold into the
 * replacement type itself.
 *
 * Used by Callous Giant ("If a source would deal 3 or less damage to this creature,
 * prevent that damage") via [AtMost]. Reusable by any future amount-gated prevention
 * or modification (e.g. "if it would deal 5 or more damage").
 */
@Serializable
sealed interface AmountFilter {
    val description: String

    /** Returns true when [amount] satisfies this filter. */
    fun matches(amount: Int): Boolean

    @SerialName("AmountAny")
    @Serializable
    data object Any : AmountFilter {
        override val description = ""
        override fun matches(amount: Int) = true
    }

    @SerialName("AmountAtMost")
    @Serializable
    data class AtMost(val value: Int) : AmountFilter {
        override val description = "$value or less"
        override fun matches(amount: Int) = amount <= value
    }

    @SerialName("AmountAtLeast")
    @Serializable
    data class AtLeast(val value: Int) : AmountFilter {
        override val description = "$value or more"
        override fun matches(amount: Int) = amount >= value
    }

    @SerialName("AmountExactly")
    @Serializable
    data class Exactly(val value: Int) : AmountFilter {
        override val description = "exactly $value"
        override fun matches(amount: Int) = amount == value
    }
}

// =============================================================================
// Counter Type Filters
// =============================================================================

/**
 * Counter type specification.
 */
@Serializable
sealed interface CounterTypeFilter {
    val description: String

    @SerialName("CounterAny")
    @Serializable
    data object Any : CounterTypeFilter {
        override val description = ""
    }

    @SerialName("PlusOnePlusOne")
    @Serializable
    data object PlusOnePlusOne : CounterTypeFilter {
        override val description = "+1/+1"
    }

    @SerialName("MinusOneMinusOne")
    @Serializable
    data object MinusOneMinusOne : CounterTypeFilter {
        override val description = "-1/-1"
    }

    @SerialName("Loyalty")
    @Serializable
    data object Loyalty : CounterTypeFilter {
        override val description = "loyalty"
    }

    @SerialName("Named")
    @Serializable
    data class Named(val name: String) : CounterTypeFilter {
        override val description = name
    }
}

// =============================================================================
// Controller Filters
// =============================================================================

/**
 * Controller/owner filters.
 */
@Serializable
sealed interface ControllerFilter {
    val description: String

    @SerialName("ControllerYou")
    @Serializable
    data object You : ControllerFilter {
        override val description = "under your control"
    }

    @SerialName("ControllerOpponent")
    @Serializable
    data object Opponent : ControllerFilter {
        override val description = "under an opponent's control"
    }

    @SerialName("ControllerAny")
    @Serializable
    data object Any : ControllerFilter {
        override val description = ""
    }
}

// =============================================================================
// Spell-Cast Predicates - extensible "facts about a cast" the trigger requires
// =============================================================================

/**
 * One required fact about a spell cast, used by `SpellCastEvent.requires` to
 * gate the trigger. The set is conjunctive: every predicate must hold.
 *
 * Each new "the cast had X property" mechanic adds a new sealed-case here
 * (and one branch in the engine matcher). The shape avoids growing
 * `SpellCastEvent` with a new boolean / optional field every time a new
 * cast-time fact becomes triggerable (kicker, treasure mana, future:
 * was-copied, was-overloaded, paid-additional-life, etc.).
 */
@Serializable
sealed interface SpellCastPredicate {
    val description: String

    /** The spell was cast from this zone (e.g. HAND for "from your hand"). */
    @SerialName("SpellCastFromZone")
    @Serializable
    data class CastFromZone(val zone: Zone) : SpellCastPredicate {
        override val description = when (zone) {
            Zone.HAND -> "from your hand"
            Zone.GRAVEYARD -> "from your graveyard"
            Zone.EXILE -> "from exile"
            else -> "from your ${zone.displayName.lowercase()}"
        }
    }

    /** The spell was cast with kicker (CR 702.32). */
    @SerialName("SpellWasKicked")
    @Serializable
    data object WasKicked : SpellCastPredicate {
        override val description = "kicked"
    }

    /**
     * Mana produced by a permanent with this subtype was spent on the cast.
     * Covers Treasure today; the engine matcher will resolve other token
     * subtypes (Food / Clue / Blood / Powerstone / Map) once the mana-pool
     * tracker generalizes beyond the current Treasure-only boolean.
     */
    @SerialName("SpellPaidWithManaFromSubtype")
    @Serializable
    data class PaidWithManaFromSubtype(val subtype: Subtype) : SpellCastPredicate {
        override val description = "using mana from a ${subtype.value}"
    }

    /**
     * The spell was modal — at least one mode was chosen at cast time (rules 700.2).
     * Used by triggers that fire only when a modal spell is cast (e.g., Riku of Many
     * Paths: "Whenever you cast a modal spell, …").
     */
    @SerialName("SpellIsModal")
    @Serializable
    data object IsModal : SpellCastPredicate {
        override val description = "modal"
    }
}

// =============================================================================
// Attack Predicates - extensible "facts about an attack declaration" the
// trigger requires
// =============================================================================

/**
 * One required fact about a creature attacking, used by `AttackEvent.requires`
 * to gate the trigger. The set is conjunctive: every predicate must hold.
 *
 * Each new attack-time mechanic (Battalion-style "with N+ attackers",
 * "with another matching creature", etc.) adds a new sealed-case here +
 * one branch in the engine matcher. The shape avoids growing `AttackEvent`
 * with a new boolean / optional field per axis (`alone`, `withAtLeastN`, …).
 */
@Serializable
sealed interface AttackPredicate {
    val description: String

    /**
     * The attacker is the only declared attacker this combat.
     * Equivalent to "attacker count == 1." Used for "attacks alone" cards.
     */
    @SerialName("AttacksAlone")
    @Serializable
    data object Alone : AttackPredicate {
        override val description = "alone"
    }

    /**
     * At least [n] creatures total were declared as attackers this combat
     * (counting the attacker the trigger fires for). Battalion shape, where
     * a creature triggers when it attacks together with two or more others
     * — `AttackerCountAtLeast(3)` on a `SELF` binding.
     */
    @SerialName("AttackerCountAtLeast")
    @Serializable
    data class AttackerCountAtLeast(val n: Int) : AttackPredicate {
        override val description = "with $n or more attackers"
    }
}
