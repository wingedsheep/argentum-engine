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

    /**
     * An opponent (player) of the effect's controller, or a permanent that an opponent
     * controls. Models the common damage-recipient template "an opponent or a permanent
     * an opponent controls" (Fated Firepower). Unlike [Opponent] (player only) or
     * [CreatureOpponentControls] (creature only), this matches both the opponent player
     * and every permanent type they control.
     */
    @SerialName("OpponentOrPermanentTheyControl")
    @Serializable
    data object OpponentOrPermanentTheyControl : RecipientFilter {
        override val description = "an opponent or a permanent an opponent controls"
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

    /**
     * The permanent that owns the effect — i.e. damage dealt *by this permanent*. Mirror of
     * [RecipientFilter.Self] for the source side. Used by static foggers like Fog Bank
     * ("prevent all combat damage that would be dealt to and dealt by this creature").
     */
    @SerialName("SourceSelf")
    @Serializable
    data object Self : SourceFilter {
        override val description = "this permanent"
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

    /**
     * A source (of any kind — permanent, spell, or ability) controlled by the effect's
     * controller. Models "a source you control" (Fated Firepower). The source's controller
     * is compared against the replacement's controller, so it covers attacking creatures,
     * burn spells on the stack, and ability sources alike.
     */
    @SerialName("SourceYouControl")
    @Serializable
    data object YouControl : SourceFilter {
        override val description = "a source you control"
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

    @SerialName("PlusOnePlusZero")
    @Serializable
    data object PlusOnePlusZero : CounterTypeFilter {
        override val description = "+1/+0"
    }

    @SerialName("PlusZeroPlusOne")
    @Serializable
    data object PlusZeroPlusOne : CounterTypeFilter {
        override val description = "+0/+1"
    }

    @SerialName("MinusOneMinusZero")
    @Serializable
    data object MinusOneMinusZero : CounterTypeFilter {
        override val description = "-1/-0"
    }

    @SerialName("MinusZeroMinusOne")
    @Serializable
    data object MinusZeroMinusOne : CounterTypeFilter {
        override val description = "-0/-1"
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

    /**
     * The spell was cast from a zone *other than* [zone] — the negation of [CastFromZone].
     * Used for "whenever you cast a spell from anywhere other than your hand" (Kellan, the Kid):
     * `CastFromZoneOtherThan(Zone.HAND)`. A spell with no recorded cast zone (synthetic / put
     * directly on the stack) does not satisfy this — only an actual cast from a different known
     * zone counts.
     */
    @SerialName("SpellCastFromZoneOtherThan")
    @Serializable
    data class CastFromZoneOtherThan(val zone: Zone) : SpellCastPredicate {
        override val description = when (zone) {
            Zone.HAND -> "from anywhere other than your hand"
            else -> "from anywhere other than your ${zone.displayName.lowercase()}"
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

    /**
     * The spell has `{X}` in its printed mana cost (CR 107.3) — "Whenever you cast a spell with
     * {X} in its mana cost, …" (Geometer's Arthropod). This is a property of the cost, not the
     * value chosen: a spell cast with X=0 still satisfies it. Pair with
     * [com.wingedsheep.sdk.scripting.values.ContextPropertyKey.X_VALUE_OF_TRIGGERING_SPELL] to
     * read the value X was set to.
     */
    @SerialName("SpellHasXInCost")
    @Serializable
    data object HasXInCost : SpellCastPredicate {
        override val description = "with {X} in its mana cost"
    }

    /**
     * The spell was cast targeting the trigger's own source permanent
     * ("a spell that targets [this creature]" — Legolas, Master Archer).
     */
    @SerialName("SpellTargetsSource")
    @Serializable
    data object TargetsSource : SpellCastPredicate {
        override val description = "that targets this"
    }

    /**
     * The spell was cast with at least one chosen target matching [filter]
     * ("a spell that targets a creature you don't control" — Legolas, Master Archer).
     * The filter is evaluated against each chosen target relative to the trigger
     * controller (so `youControl()` / opponent-controlled predicates resolve correctly).
     */
    @SerialName("SpellTargetsMatching")
    @Serializable
    data class TargetsMatching(val filter: GameObjectFilter) : SpellCastPredicate {
        override val description = "that targets ${filter.description}"
    }

    /**
     * The just-cast spell is **owned by a player other than the trigger's controller** — the
     * card's owner (CR 108.3, fixed at game start) differs from who cast it. This is true when
     * you cast a spell that isn't yours: a card exiled from an opponent's graveyard/hand that you
     * may cast (Nita, Forum Conciliator; Gonti, Lord of Luxury), a spell stolen with control of
     * the stack object, etc. A spell you cast from your own zones (owner == controller) does not
     * satisfy it. Resolved against the spell entity's owner record vs. the trigger controller.
     */
    @SerialName("SpellNotOwnedByController")
    @Serializable
    data object NotOwnedByController : SpellCastPredicate {
        override val description = "you don't own"
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

    /**
     * The attacker is attacking *for the first time this turn* — it had not been
     * declared as an attacker in any earlier combat phase this turn. The matcher
     * consults the per-turn attacker set (the same union that backs raid / "you
     * attacked with N creatures this turn"), so a creature that attacks again in a
     * second combat phase (extra-combat effects like Fear of Missing Out) does not
     * re-fire. The window resets at the start of each turn.
     *
     * Per-attacker by design: it gates against the trigger's own source, so use it
     * with a `SELF` binding — "Whenever this creature attacks for the first time
     * each turn, …".
     */
    @SerialName("AttacksFirstTimeEachTurn")
    @Serializable
    data object FirstTimeEachTurn : AttackPredicate {
        override val description = "for the first time each turn"
    }

    /**
     * The attacker was declared as attacking a **player** — not a planeswalker or a battle.
     * (CR 508.1: an attacker is declared as attacking a player, planeswalker, or battle.)
     *
     * A creature can only attack a player who is its controller's opponent, so on a `SELF`
     * binding this is exactly "attacks an opponent" (Kaalia of the Vast — whose 2024 ruling
     * clarifies the ability "doesn't trigger if it attacks a planeswalker or battle"). The
     * defender kind is fixed at declaration, so the matcher reads it from the stamped
     * `AttackersDeclaredEvent.attackersAgainstPlayer` set rather than post-declaration state.
     *
     * Per-attacker by design: it gates against the trigger's own source, so use it with a
     * `SELF` binding (or an ANY-binding attacker filter that already scopes to one creature).
     */
    @SerialName("AttacksDefenderIsPlayer")
    @Serializable
    data object DefenderIsPlayer : AttackPredicate {
        override val description = "a player"
    }

    /**
     * The attacker was declared as attacking **and** at least one *other* declared attacker has
     * strictly greater **projected** power than the attacker's own projected power. This is the
     * Training trigger condition (CR 702.149a: "Whenever this creature and at least one other
     * creature with power greater than this creature's power attack, put a +1/+1 counter on this
     * creature").
     *
     * Power is compared through **projected** state (Rule 613 layers), so anthems, auras, and
     * counters on the attacking band are reflected — a lord that pumps the *other* attacker can
     * flip this from false to true. Both powers are read at declaration time (when the trigger
     * condition is checked); the comparison is strict (`>`), so an equal-power partner does not
     * satisfy it.
     *
     * Per-attacker by design: it gates against the trigger's own source, so use it with a `SELF`
     * binding — "Whenever this creature trains, …".
     */
    @SerialName("AttacksAlongsideGreaterPower")
    @Serializable
    data object AttackedAlongsideGreaterPower : AttackPredicate {
        override val description = "with another creature with greater power"
    }
}

// =============================================================================
// Ability Target Match - constrains an activated ability by its chosen targets
// =============================================================================

/**
 * A predicate over the set of targets an activated ability on the stack was given.
 *
 * Used by [com.wingedsheep.sdk.scripting.EventPattern.AbilityActivatedEvent.targetMatch] to express
 * "Whenever you activate an ability that targets X" (Ertha Jo, Frontier Mentor — "...that targets a
 * creature or player"). A constraint is satisfied when **at least one** of the ability's chosen
 * targets matches it; a non-targeting ability (e.g. a tap-for-mana) never matches, so it doesn't
 * fire the trigger.
 *
 * The match space is wider than [GameObjectFilter] because an ability can target a *player* as well
 * as an object, and `GameObjectFilter` only describes objects. [AnyPlayer] covers the player half;
 * [ObjectMatching] covers the object half; [AnyOf] composes them into heterogeneous unions such as
 * "creature or player".
 */
@Serializable
sealed interface AbilityTargetMatch {
    val description: String

    /** At least one chosen target is a player. */
    @SerialName("AbilityTargetAnyPlayer")
    @Serializable
    data object AnyPlayer : AbilityTargetMatch {
        override val description = "player"
    }

    /** At least one chosen target is an object matching [filter] (creature, permanent, …). */
    @SerialName("AbilityTargetObjectMatching")
    @Serializable
    data class ObjectMatching(val filter: GameObjectFilter) : AbilityTargetMatch {
        override val description = filter.description
    }

    /** At least one chosen target matches any of [options] (heterogeneous OR). */
    @SerialName("AbilityTargetAnyOf")
    @Serializable
    data class AnyOf(val options: List<AbilityTargetMatch>) : AbilityTargetMatch {
        override val description = options.joinToString(" or ") { it.description }
    }

    companion object {
        /** "...that targets a creature or player." */
        val CreatureOrPlayer: AbilityTargetMatch =
            AnyOf(listOf(ObjectMatching(GameObjectFilter.Creature), AnyPlayer))
    }
}
