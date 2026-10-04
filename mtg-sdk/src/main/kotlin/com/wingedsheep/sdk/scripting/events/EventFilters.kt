package com.wingedsheep.sdk.scripting.events

import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.GameObjectFilter
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// =============================================================================
// Recipient - a player or an object an event happens to
// =============================================================================

/**
 * Who or what an event happens to — the recipient of damage, the permanent a counter is put on, the
 * thing an activated ability targets. It can be a **player or an object**, which is exactly why it
 * isn't a [GameObjectFilter]: that type describes objects only. Rather than grow a third vocabulary,
 * [Recipient] names each half with the vocabulary that already exists for it — a [Player] reference
 * for players, a [GameObjectFilter] for objects — and [AnyOf] unions them ("a player or
 * planeswalker", "an opponent or a permanent an opponent controls").
 *
 * Every reading is relative to the observing ability: [Player] `You` / `EachOpponent` / `EnchantedPlayer`
 * and a filter's `youControl()` / `sourceItself()` / `attachedToBySource()` resolve against the
 * permanent that carries the trigger or replacement and its controller. An [Object] is matched against
 * the recipient's last-known information when the recipient has already left the battlefield — a
 * creature killed by the very damage that is being asked about is still "a creature you control",
 * because the trigger event is checked against the objects as they existed right after it (CR 603.10)
 * — and against its own characteristics while it is still on its way onto the battlefield (a counter
 * placed on an entering creature, CR 614.12).
 *
 * The companion constants spell the common recipients so card code stays readable:
 * `Recipient.AnyPlayer`, `Recipient.CreatureYouControl`, `Recipient.Self`, …
 */
@Serializable
sealed interface Recipient {
    val description: String

    /** A player named by a [com.wingedsheep.sdk.scripting.references.Player] reference. */
    @SerialName("RecipientPlayer")
    @Serializable
    data class Player(val player: com.wingedsheep.sdk.scripting.references.Player) : Recipient {
        override val description: String get() = when (player) {
            com.wingedsheep.sdk.scripting.references.Player.EachOpponent -> "an opponent"
            else -> player.description
        }
    }

    /** An object (a permanent, or a spell for an ability's target) matching [filter]. */
    @SerialName("RecipientObject")
    @Serializable
    data class Object(val filter: GameObjectFilter) : Recipient {
        override val description: String get() = filter.description
    }

    /**
     * "Another player" — any player other than the observing ability's controller. Wider than
     * [Opponent]: in a team game a teammate is another player but not an opponent (CR 102.3), so
     * Night Dealings' "a source you control deals damage to another player" counts damage to a
     * Two-Headed Giant teammate that [Opponent] would skip.
     */
    @SerialName("RecipientAnotherPlayer")
    @Serializable
    data object AnotherPlayer : Recipient {
        override val description: String = "another player"
    }

    /** Any of [options] — the heterogeneous "player or object" unions. */
    @SerialName("RecipientAnyOf")
    @Serializable
    data class AnyOf(val options: List<Recipient>) : Recipient {
        init {
            require(options.size >= 2) { "Recipient.AnyOf needs at least two options, got ${options.size}" }
        }

        override val description: String get() = options.joinToString(" or ") { it.description }
    }

    companion object {
        /** Any player. */
        val AnyPlayer: Recipient = Player(com.wingedsheep.sdk.scripting.references.Player.Any)

        /** You — the controller of the observing ability. */
        val You: Recipient = Player(com.wingedsheep.sdk.scripting.references.Player.You)

        /** An opponent of the observing ability's controller. */
        val Opponent: Recipient = Player(com.wingedsheep.sdk.scripting.references.Player.EachOpponent)

        /**
         * The player the observing Aura is attached to — "deals combat damage to enchanted player"
         * (Curse of Hospitality). Scoped by the source's *attachment*, not its controller, so a
         * curse on one opponent doesn't fire off damage dealt to another; matches nothing when the
         * source isn't attached to a player.
         */
        val EnchantedPlayer: Recipient = Player(com.wingedsheep.sdk.scripting.references.Player.EnchantedPlayer)

        /** Any object at all — every permanent a damage or counter event can reach. */
        val AnyObject: Recipient = Object(GameObjectFilter.Any)

        /** Anything: any player or any object — the unrestricted default. */
        val Any: Recipient = AnyOf(listOf(AnyPlayer, AnyObject))

        val AnyCreature: Recipient = Object(GameObjectFilter.Creature)
        val AnyPermanent: Recipient = Object(GameObjectFilter.Permanent)
        val CreatureYouControl: Recipient = Object(GameObjectFilter.Creature.youControl())
        val CreatureOpponentControls: Recipient = Object(GameObjectFilter.Creature.opponentControls())
        val PermanentYouControl: Recipient = Object(GameObjectFilter.Permanent.youControl())

        /** The permanent carrying the observing ability ("this creature"). */
        val Self: Recipient = Object(GameObjectFilter.Any.sourceItself())

        /**
         * The permanent the observing Aura is attached to ("enchanted creature"). The same object as
         * [EquippedCreature] — both read the source's attachment — named twice so an Aura and an
         * Equipment each read in their own vocabulary.
         */
        val EnchantedCreature: Recipient = Object(GameObjectFilter.Any.attachedToBySource())

        /** The permanent the observing Equipment is attached to ("equipped creature"); see [EnchantedCreature]. */
        val EquippedCreature: Recipient = EnchantedCreature

        /** "A player or planeswalker". */
        val AnyPlayerOrPlaneswalker: Recipient = AnyOf(listOf(AnyPlayer, Object(GameObjectFilter.Planeswalker)))

        /** "A player or battle" — the March of the Machine combat-damage wording (Archpriest of Shadows). */
        val AnyPlayerOrBattle: Recipient = AnyOf(listOf(AnyPlayer, Object(GameObjectFilter.Battle)))

        /** "An opponent or battle" — Bloodfeather Phoenix's "deals damage to an opponent or battle". */
        val OpponentOrBattle: Recipient = AnyOf(listOf(Opponent, Object(GameObjectFilter.Battle)))

        /** "A creature or player" — Ertha Jo, Frontier Mentor's "an ability that targets a creature or player". */
        val CreatureOrPlayer: Recipient = AnyOf(listOf(Object(GameObjectFilter.Creature), AnyPlayer))

        /**
         * "An opponent or a permanent an opponent controls" (Fated Firepower, Twinflame Tyrant) —
         * the opponent player and every permanent type they control.
         */
        val OpponentOrPermanentTheyControl: Recipient =
            AnyOf(listOf(Opponent, Object(GameObjectFilter.Permanent.opponentControls())))
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
// Damage Predicates - extensible facts about a damage event the trigger requires
// =============================================================================

/** Additional facts a damage trigger can require beyond source and recipient filters. */
@Serializable
sealed interface DamagePredicate {
    val description: String

    /**
     * The damage source has exactly one chosen target, and that target is this damage recipient.
     * Collateral damage to another permanent or player does not satisfy this relationship.
     */
    @SerialName("DamageSourceSoleTargetIsRecipient")
    @Serializable
    data object SourceSoleTargetIsRecipient : DamagePredicate {
        override val description = "and its only target is the damage recipient"
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
     * Mana produced by a permanent with this subtype was spent on the cast — Treasure
     * (Alchemist's Talent, Rain of Riches), Cave, or any other producing-source subtype. The
     * subtype is snapshotted when the mana is produced, so a Treasure sacrificed to tap for its own
     * mana still counts.
     */
    @SerialName("SpellPaidWithManaFromSubtype")
    @Serializable
    data class PaidWithManaFromSubtype(val subtype: Subtype) : SpellCastPredicate {
        override val description = "using mana from a ${subtype.value}"
    }

    /**
     * At least [atLeast] mana produced by a source of card type [cardType] was spent on the cast —
     * Inga and Esika's "if three or more mana from creatures was spent to cast it" is
     * `PaidWithManaFromCardType(CardType.CREATURE, atLeast = 3)`. The source's card types are
     * snapshotted when the mana is produced (projected, so an animated land counts as a creature),
     * and restricted mana ("spend this mana only to cast a creature spell") carries the tag too.
     * The spell's payment never changes, so as a trigger condition this reads the same when the
     * trigger is put on the stack and when it resolves.
     */
    @SerialName("SpellPaidWithManaFromCardType")
    @Serializable
    data class PaidWithManaFromCardType(val cardType: CardType, val atLeast: Int = 1) : SpellCastPredicate {
        override val description = if (atLeast <= 1) "using mana from a ${cardType.displayName.lowercase()}"
            else "with $atLeast or more mana from ${cardType.displayName.lowercase()}s spent to cast it"
    }

    /**
     * Mana produced by the trigger's own source permanent was spent on the cast — "Whenever you cast
     * a … spell using mana produced by [this]" (Tecutlan, the Searing Rift / Barracks of the Thousand
     * / The Myriad Pools). Matched against the source that produced the mana, not a subtype, so it
     * fires only for the specific land whose ability made the mana.
     */
    @SerialName("SpellPaidWithManaFromSource")
    @Serializable
    data object PaidWithManaFromSource : SpellCastPredicate {
        override val description = "using mana produced by this"
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
     * The spell was cast targeting the trigger's own source permanent and **nothing else** — every
     * instance of the word "target" on the spell points at the source ("a spell that targets only
     * [this creature]" — Zada, Hedron Grinder; Mirrorwing Dragon).
     *
     * Strictly narrower than [TargetsSource], which is satisfied as soon as the source is among the
     * chosen targets: a spell targeting both the source and another permanent satisfies
     * [TargetsSource] but not this. A spell with **no** targets never satisfies it either. A spell
     * with several instances of "target" all pointed at the source does — the copies made by
     * [com.wingedsheep.sdk.scripting.effects.CopyForEachOtherPossibleTargetEffect] then have to
     * be legal for each of those instances (CR 707.10d).
     */
    @SerialName("SpellTargetsOnlySource")
    @Serializable
    data object TargetsOnlySource : SpellCastPredicate {
        override val description = "that targets only this"
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
     * The just-cast spell is **owned by a player other than the one who cast it** — the card's
     * owner (CR 108.3, fixed at game start) differs from its caster. This is true when a player
     * casts a spell that isn't theirs: a card exiled from an opponent's graveyard/hand that they
     * may cast (Nita, Forum Conciliator; Gonti, Lord of Luxury), a spell stolen with control of
     * the stack object, etc. A spell cast from the caster's own zones (owner == caster) does not
     * satisfy it.
     *
     * Resolved against the spell entity's owner record vs. the *caster*, not the trigger's
     * controller, so it reads the same under both wordings: "whenever **you** cast a spell you
     * don't own" (where the [EventPattern.SpellCastEvent.player] gate has already pinned the
     * caster to the trigger's controller) and "whenever **a player** casts a spell they don't
     * own" (Gonti, Night Minister), which observes every seat.
     */
    @SerialName("SpellNotOwnedByController")
    @Serializable
    data object NotOwnedByController : SpellCastPredicate {
        override val description = "you don't own"
    }

    /**
     * The spell was cast **as an Adventure** (CR 715.3) — "Whenever you cast an Adventure spell"
     * (Chancellor of Tales). This is about how the card was cast, not what the card is: the same
     * adventurer card cast as its creature half does *not* satisfy it, and per the 2023-09-01
     * rulings an "Adventure spell" is never found among instants/sorceries that merely have an
     * Adventure printed on them.
     *
     * Contrast [com.wingedsheep.sdk.scripting.predicates.CardPredicate.HasAdventure], which is a
     * zone-independent characteristic of the *card* (Frantic Firebolt tallying adventurer cards in
     * a graveyard) and is true regardless of which half was cast.
     */
    @SerialName("SpellCastAsAdventure")
    @Serializable
    data object CastAsAdventure : SpellCastPredicate {
        override val description = "as an Adventure"
    }

    /**
     * The spell was cast **as a prepare spell** (CR 722.3c) — "Whenever you cast a prepared spell"
     * (Codie, Ravenous Codex). A preparation card's prepare spell is never cast directly (CR 722.3);
     * it is the copy a prepared permanent leaves in exile that gets cast, so this is true exactly
     * for that cast copy. The same preparation card cast from hand as its creature half does not
     * match, and neither does an unrelated instant or sorcery.
     *
     * A cast-time fact like [CastAsAdventure], not a characteristic: contrast
     * [com.wingedsheep.sdk.scripting.predicates.StatePredicate.IsPrepared], which is the
     * *permanent's* "prepared" designation.
     */
    @SerialName("SpellCastAsPrepareSpell")
    @Serializable
    data object CastAsPrepareSpell : SpellCastPredicate {
        override val description = "prepared"
    }

    /**
     * The spell was cast with at least one chosen target that is an **opponent** of the trigger's
     * controller — "a spell that targets an opponent" (Danitha, Spear of Agony). The player half of
     * [TargetsMatching], which only sees objects.
     */
    @SerialName("SpellTargetsOpponent")
    @Serializable
    data object TargetsOpponent : SpellCastPredicate {
        override val description = "that targets an opponent"
    }

    /**
     * The spell cast is a **card**, not a copy — "whenever a player casts an instant or sorcery
     * *card*" (Eye of the Storm). Casting a copy of a card (CR 707.12 — the "copy it, then you may
     * cast the copy" pattern) is still casting a spell and still fires "casts a spell" triggers,
     * but a copy of a card is not a card, so it never satisfies this. Without it a payoff that
     * casts copies of what it saw would re-trigger itself on every copy it casts.
     */
    @SerialName("SpellIsCard")
    @Serializable
    data object IsCard : SpellCastPredicate {
        override val description = "card"
    }

    /**
     * The spell itself matches [filter] — the same test as
     * [com.wingedsheep.sdk.scripting.EventPattern.SpellCastEvent.spellFilter], as a predicate so it
     * can sit inside [AnyOf]: "an Equipment spell **or** a spell that targets a creature you
     * control" (Danitha, Sword of Hope). Standing alone, prefer `spellFilter`.
     */
    @SerialName("SpellMatches")
    @Serializable
    data class SpellMatches(val filter: GameObjectFilter) : SpellCastPredicate {
        override val description = "that is ${filter.description}"
    }

    /**
     * At least one of [options] holds — the disjunction `requires` (a conjunctive set) can't say on
     * its own. "A spell that targets an opponent or a creature an opponent controls" (Danitha,
     * Spear of Agony) and "an Equipment spell or a spell that targets a creature you control"
     * (Danitha, Sword of Hope).
     *
     * Only the gate is disjunctive: the "those creatures" capture behind [TargetsMatching] reads
     * top-level predicates alone, so a payoff that acts on the matched targets must not hide its
     * [TargetsMatching] inside an [AnyOf].
     */
    @SerialName("SpellAnyOf")
    @Serializable
    data class AnyOf(val options: List<SpellCastPredicate>) : SpellCastPredicate {
        init {
            require(options.size >= 2) { "SpellCastPredicate.AnyOf needs at least two options, got ${options.size}" }
        }

        override val description = options.joinToString(" or ") { it.description }
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
     * The attacker was declared as attacking a **battle** — the defender-kind sibling of
     * [DefenderIsPlayer] (CR 508.1: an attacker is declared as attacking a player, planeswalker,
     * or battle). "Whenever this creature attacks a battle" (Thrashing Frontliner, War-Trained
     * Slasher). Like [DefenderIsPlayer] the defender kind is fixed at declaration, so the matcher
     * reads the stamped `AttackersDeclaredEvent.attackersAgainstBattle` set.
     *
     * Per-attacker by design: use it with a `SELF` binding.
     */
    @SerialName("AttacksDefenderIsBattle")
    @Serializable
    data object DefenderIsBattle : AttackPredicate {
        override val description = "a battle"
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
