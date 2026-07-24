package com.wingedsheep.sdk.core

import kotlinx.serialization.Serializable

/**
 * Types of counters that can be placed on permanents.
 */
@Serializable
enum class CounterType {
    PLUS_ONE_PLUS_ONE,
    MINUS_ONE_MINUS_ONE,
    PLUS_ONE_PLUS_ZERO,
    PLUS_ZERO_PLUS_ONE,
    MINUS_ONE_MINUS_ZERO,
    MINUS_ZERO_MINUS_ONE,
    LOYALTY,
    CHARGE,
    GEM,
    POISON,
    SILVER,
    GOLD,
    PLAGUE,
    TRAP,
    DEPLETION,
    EGG,
    LORE,
    AIM,
    STUN,
    FINALITY,
    SUPPLY,
    FLYING,
    FIRST_STRIKE,
    DOUBLE_STRIKE,
    VIGILANCE,
    LIFELINK,
    INDESTRUCTIBLE,
    DEATHTOUCH,
    TRAMPLE,
    HEXPROOF,
    REACH,
    STASH,
    BLIGHT,
    COIN,
    FLOOD,
    CHORUS,
    DREAM,
    QUEST,
    GROWTH,
    TIME,
    FEATHER,
    HOURGLASS,
    DECAYED,
    HOPE,
    VERSE,
    INFLUENCE,
    BURDEN,
    LOOT,
    WIND,
    NEST,
    PAGE,
    REV,
    SOUL,
    DIVINITY,
    DOOM,
    POSSESSION,
    FIRE,
    CONQUEROR,
    NET,
    LANDMARK,
    DREAD,
    SPORE,
    INCUBATION,
    FELLOWSHIP,
    BAIT,
    BORE;

    companion object {
        /**
         * Maps a counter-type *name* — as stored on effects/durations, i.e. a `Counters.*`
         * string constant (e.g. `"blight"`, `"+1/+1"`) — to its [CounterType], or `null` if
         * it doesn't correspond to a known counter. Handles the two symbolic names (`+1/+1`,
         * `-1/-1`) and otherwise upper-cases and swaps spaces for underscores to match the
         * enum constant. Mirrors the inline parse used by `StatePredicate.HasCounter`.
         */
        fun fromName(name: String): CounterType? = when (name) {
            "+1/+1" -> PLUS_ONE_PLUS_ONE
            "-1/-1" -> MINUS_ONE_MINUS_ONE
            else -> try {
                valueOf(name.uppercase().replace(' ', '_'))
            } catch (_: IllegalArgumentException) {
                null
            }
        }
    }
}

/**
 * String constants for counter types used in card definitions and effects.
 * Use these instead of raw string literals for consistency and refactorability.
 */
object Counters {
    const val PLUS_ONE_PLUS_ONE = "+1/+1"
    const val MINUS_ONE_MINUS_ONE = "-1/-1"
    const val PLUS_ONE_PLUS_ZERO = "+1/+0"
    const val PLUS_ZERO_PLUS_ONE = "+0/+1"
    const val MINUS_ONE_MINUS_ZERO = "-1/-0"
    const val MINUS_ZERO_MINUS_ONE = "-0/-1"
    const val LOYALTY = "loyalty"
    const val CHARGE = "charge"
    const val GEM = "gem"
    const val POISON = "poison"
    const val SILVER = "silver"
    const val GOLD = "gold"
    const val PLAGUE = "plague"
    const val TRAP = "trap"
    const val DEPLETION = "depletion"
    const val EGG = "egg"
    const val LORE = "lore"
    const val AIM = "aim"
    const val STUN = "stun"
    const val FINALITY = "finality"
    const val SUPPLY = "supply"
    const val FLYING = "flying"
    const val FIRST_STRIKE = "first strike"
    const val DOUBLE_STRIKE = "double strike"
    const val VIGILANCE = "vigilance"
    const val LIFELINK = "lifelink"
    const val INDESTRUCTIBLE = "indestructible"
    const val DEATHTOUCH = "deathtouch"
    const val TRAMPLE = "trample"
    const val HEXPROOF = "hexproof"
    const val REACH = "reach"
    const val STASH = "stash"
    const val BLIGHT = "blight"
    const val COIN = "coin"
    const val FLOOD = "flood"
    const val CHORUS = "chorus"
    const val DREAM = "dream"
    const val QUEST = "quest"
    const val GROWTH = "growth"
    const val TIME = "time"
    const val FEATHER = "feather"
    const val HOURGLASS = "hourglass"
    const val SPORE = "spore"

    /**
     * Decayed counter (Tarkir: Dragonstorm). A keyword-ability counter (CR 702.147a): a creature
     * with one or more decayed counters has Decayed — "This creature can't block" and "When this
     * creature attacks, sacrifice it at end of combat." Granted to *any* creature, independent of
     * its printed abilities (e.g. Rot-Curse Rakshasa's Renew). The behavior is realized by the
     * engine off the counter, mirroring how the printed [com.wingedsheep.sdk.dsl.card]`.decayed()`
     * helper composes the same static + triggered ability.
     */
    const val DECAYED = "decayed"

    /**
     * Hope counter (LTR — Dawn of a New Age). Passive counter: no inherent rule, the card
     * referencing it reads the count via `DynamicAmounts.countersOnSelf(...)`.
     */
    const val HOPE = "hope"

    /**
     * Verse counter (LTR — Lost Isle Calling). Passive counter accumulated on a Saga-like
     * permanent; the card itself reads the count.
     */
    const val VERSE = "verse"

    /**
     * Influence counter (LTR — Palantír of Orthanc). Passive counter the card's own abilities
     * scale off of.
     */
    const val INFLUENCE = "influence"

    /**
     * Burden counter (LTR — The One Ring). Passive counter that the card's own legendary-rule
     * and damage trigger read; the engine has no inherent behavior tied to it.
     */
    const val BURDEN = "burden"

    /**
     * Loot counter (OTJ — Bandit's Haul). Passive storage counter with no inherent rule; the
     * card's own abilities accumulate it (commit-a-crime trigger) and spend it (remove two as an
     * activation cost to draw).
     */
    const val LOOT = "loot"

    /**
     * Wind counter (ARN — Cyclone). Passive counter accumulated each upkeep; the card reads the
     * count to scale its pay-or-sacrifice cost and the damage it deals. No inherent rule.
     */
    const val WIND = "wind"

    /**
     * Nest counter (DSK — Twitching Doll). Passive storage counter with no inherent rule; the
     * card's own abilities accumulate it (a mana-ability adds one per activation) and read the
     * count to scale a token-creation payoff. No inherent rule.
     */
    const val NEST = "nest"

    /**
     * Page counter (SOS — Diary of Dreams). Passive storage counter with no inherent rule; the
     * card's own abilities accumulate it (an instant/sorcery-cast trigger adds one) and read the
     * count to reduce an activated ability's cost. No inherent rule.
     */
    const val PAGE = "page"

    /**
     * Bait counter (FDN — Fishing Pole). Passive storage counter with no inherent rule; the
     * Equipment's granted activated ability accumulates one and its "equipped creature becomes
     * untapped" trigger spends one to reel in a Fish token. No inherent rule.
     */
    const val BAIT = "bait"

    /**
     * Rev counter (DSK — Chainsaw). Passive storage counter with no inherent rule; the card's own
     * abilities accumulate it (a "whenever one or more creatures die" trigger adds one) and read
     * the count to scale the equipped creature's power bonus (+X/+0). No inherent rule.
     */
    const val REV = "rev"

    /**
     * Soul counter (FDN — Ravenous Amulet). Passive storage counter with no inherent rule; the
     * card's own abilities accumulate it (a "sacrifice a creature: draw a card" activation adds
     * one) and its sacrifice ability reads the count to size the life each opponent loses. No
     * inherent rule.
     */
    const val SOUL = "soul"

    /**
     * Divinity counter (CHK — Myojin cycle). Passive counter with no inherent rule; each Myojin's
     * own static and activated abilities check for or remove it.
     */
    const val DIVINITY = "divinity"

    /**
     * Doom counter (ATQ — Armageddon Clock). Passive counter accumulated one-per-upkeep; the card
     * reads the count to scale the damage it deals to each player in the draw step, and a {4}
     * activated ability removes one. No inherent rule.
     */
    const val DOOM = "doom"

    /**
     * Possession counter (DSK — Unwilling Vessel). Passive storage counter with no inherent rule;
     * Eerie triggers accumulate it (an enchantment you control entering / fully unlocking a Room
     * each add one) and the card's dies trigger reads the count to size the X/X Spirit token it
     * leaves behind. No inherent rule.
     */
    const val POSSESSION = "possession"

    /**
     * Fire counter (TLA — War Balloon; later Fated Firepower / "Fated" cards). Passive named
     * counter with no inherent rule of its own — the card referencing it reads the count (e.g.
     * "As long as this Vehicle has three or more fire counters on it, it's an artifact creature")
     * via `Conditions.SourceCounterCountAtLeast(...)` / `DynamicAmounts.countersOnSelf(...)`.
     * NOT a keyword counter, so it is intentionally absent from `StateProjector.KEYWORD_COUNTER_MAP`.
     */
    const val FIRE = "fire"

    /**
     * Conqueror counter (TLA — Zhao, the Moon Slayer). Passive named counter with no inherent
     * rule of its own — the card referencing it reads the count (e.g. "As long as Zhao has a
     * conqueror counter on him, nonbasic lands are Mountains") via
     * `Conditions.SourceCounterCountAtLeast(...)` / `DynamicAmounts.countersOnSelf(...)`.
     * NOT a keyword counter, so it is intentionally absent from `StateProjector.KEYWORD_COUNTER_MAP`.
     */
    const val CONQUEROR = "conqueror"

    /**
     * Net counter (LCI — Braided Net). Passive named counter with no inherent rule of its
     * own — the card enters with three (an `EntersWithCounters` replacement with
     * `CounterTypeFilter.Named(Counters.NET)`) and removes one as an activation cost
     * (`Costs.RemoveCounterFromSelf(Counters.NET, 1)`).
     * NOT a keyword counter, so it is intentionally absent from `StateProjector.KEYWORD_COUNTER_MAP`.
     */
    const val NET = "net"

    /**
     * Landmark counter (LCI — Treasure Map). Passive named counter with no inherent rule of its
     * own — Treasure Map's activated ability adds one per activation and reads the count (via
     * `Conditions.SourceCounterCountAtLeast(...)`) to remove three, transform into Treasure Cove,
     * and make three Treasures.
     * NOT a keyword counter, so it is intentionally absent from `StateProjector.KEYWORD_COUNTER_MAP`.
     */
    const val LANDMARK = "landmark"

    /**
     * Dread counter (LCI — Grasping Shadows). Passive named counter with no inherent rule of its
     * own — Grasping Shadows adds one whenever a creature you control attacks alone and reads the
     * count (via `Conditions.SourceCounterCountAtLeast(...)`) to transform into Shadows' Lair,
     * whose activated ability spends one (`Costs.RemoveCounterFromSelf(Counters.DREAD, 1)`).
     * NOT a keyword counter, so it is intentionally absent from `StateProjector.KEYWORD_COUNTER_MAP`.
     */
    const val DREAD = "dread"

    /**
     * Incubation counter (FDN — Drake Hatcher). Passive storage counter with no inherent rule; the
     * card's own abilities accumulate it (a combat-damage trigger adds one per point of damage) and
     * spend it (remove three as an activation cost to hatch a Drake token). NOT a keyword counter,
     * so it is intentionally absent from `StateProjector.KEYWORD_COUNTER_MAP`.
     * Not MTG's Incubate/incubator-token mechanic.
     */
    const val INCUBATION = "incubation"

    /**
     * Fellowship counter (FDN — Banner of Kinship). Passive storage counter with no inherent rule;
     * the Banner enters with one per creature of its chosen type and its static ability reads the
     * count to size the anthem. NOT a keyword counter, so it is intentionally absent from
     * `StateProjector.KEYWORD_COUNTER_MAP`.
     */
    const val FELLOWSHIP = "fellowship"

    /**
     * Bore counter (LCI — Brass's Tunnel-Grinder). Passive named counter with no inherent rule of
     * its own — Brass's Tunnel-Grinder adds one at its end step if you descended this turn and reads
     * the count (via `Conditions.SourceCounterCountAtLeast(...)`) to remove three and transform into
     * Tecutlan, the Searing Rift. NOT a keyword counter, so it is intentionally absent from
     * `StateProjector.KEYWORD_COUNTER_MAP`.
     */
    const val BORE = "bore"

    /**
     * Wildcard sentinel for triggers/events that fire on counters of *any* type, e.g.
     * "whenever one or more counters are put on a creature you control" (Stalwart Successor).
     * A [com.wingedsheep.sdk.scripting.EventPattern.CountersPlacedEvent] with this `counterType`
     * matches every counter-placement event regardless of the counter kind.
     */
    const val ANY = "any"
}
