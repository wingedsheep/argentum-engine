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

    /**
     * Defense counter (CR 310.4). A battle's defense *is* its number of defense counters
     * (CR 310.4c): it enters with as many as its printed defense number, damage removes that
     * many (CR 120.3h), and a battle at 0 is put into its owner's graveyard (CR 704.5v). The
     * battle analogue of [LOYALTY].
     */
    DEFENSE,
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
    SHIELD,
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
    HASTE,
    MENACE,
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
    BORE,
    POINT,
    WISH,
    REVIVAL,
    INGENUITY,
    FILM,
    SKEWER,
    ENERGY,
    ICE,
    OMEN,
    PLAN,
    INVASION,

    /**
     * Harness counter (Marvel's Spider-Man Infinity Stones). A binary "harnessed" marker: the Stone's
     * activated Harness ability places one, and its `∞` ability is gated on the Stone having a harness
     * counter (CR-style "as long as this permanent has a harness counter"). Not a resource — exactly
     * one is ever placed; it models the permanent "once harnessed" state that resets if the Stone
     * leaves the battlefield.
     */
    HARNESS,

    /**
     * Hone counter (The Hobbit). CR 122.1j: "A hone counter on an Equipment gives +1/+0 to any
     * creature that Equipment is attached to."
     *
     * Like [SHIELD] and [STUN], the behavior is inherent to the *counter*, not an ability of the
     * permanent carrying it — a hone counter placed on an Equipment that never mentions hone still
     * pumps that Equipment's equipped creature. That is exactly what Dwalin, Weaponmaster relies on
     * when he puts a counter on *each* Equipment you control, so it cannot be modelled as a static
     * ability printed on the two cards that happen to grant hone counters.
     *
     * Realized in `StateProjector.collectContinuousEffects`, which synthesizes one Layer 7c P/T
     * modification (CR 613.4c — "effects **and counters** that modify power and/or toughness") per
     * honed Equipment, aimed at whatever it is attached to. Deliberately **not** a keyword counter,
     * so it is absent from `StateProjector.KEYWORD_COUNTER_MAP`: it grants the Equipment nothing and
     * modifies a *different* object than the one it sits on.
     */
    HONE;

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
    const val DEFENSE = "defense"
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

    /**
     * Shield counter (SNC onward; MSH — Captain America, Super-Soldier). CR 122.1c: one *or more*
     * shield counters on a permanent create a **single** replacement effect and a **single**
     * prevention effect — "if this permanent would be destroyed as the result of an effect, instead
     * remove a shield counter from it" and "if damage would be dealt to this permanent, prevent that
     * damage and remove a shield counter from it". Both consume exactly one counter per event, so a
     * permanent with three shield counters survives three separate damage/destroy events, not one
     * event three times over.
     *
     * Inherent to the counter, not an ability of the permanent — a creature that loses all abilities
     * is still protected. Deliberately **not** a keyword counter, so it is absent from
     * `StateProjector.KEYWORD_COUNTER_MAP`. Realized by the engine at the four chokepoints that can
     * consume it: `DamageUtils.dealDamageToTarget` and `CombatDamageManager` (prevention — combat
     * damage marks itself rather than routing through `dealDamageToTarget`), plus
     * `ZoneMovementUtils.destroyPermanent` and `MoveCollectionExecutor`'s destroy branch
     * (replacement). Notably it does **not** stop sacrifice, the lethal-damage state-based action,
     * or 0-toughness death, and it is not regeneration.
     */
    const val SHIELD = "shield"

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

    /**
     * Haste counter (MSH — Super-Adaptoid). Keyword counter (CR 122.1b / 613.1f): the permanent
     * gains haste for as long as it has one. Wired through `StateProjector.KEYWORD_COUNTER_MAP`.
     */
    const val HASTE = "haste"

    /**
     * Menace counter (MSH — Super-Adaptoid). Keyword counter (CR 122.1b / 613.1f): the permanent
     * gains menace for as long as it has one. Wired through `StateProjector.KEYWORD_COUNTER_MAP`.
     */
    const val MENACE = "menace"
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
     * Omen counter (VOW — Soulcipher Board). Passive countdown counter with no inherent rule; the
     * artifact enters with three and its "whenever a creature card is put into your graveyard"
     * trigger removes one, transforming the artifact once the last one is gone.
     */
    const val OMEN = "omen"

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
     * Point counter (LCI — Contested Game Ball). Passive storage counter with no inherent rule of
     * its own — Contested Game Ball's activated ability adds one per activation and reads the count
     * (via `Conditions.SourceCounterCountAtLeast(...)`) to sacrifice itself and create a Treasure
     * once it has five or more. NOT a keyword counter, so it is intentionally absent from
     * `StateProjector.KEYWORD_COUNTER_MAP`.
     */
    const val POINT = "point"

    /**
     * Wish counter (ELD — Wishclaw Talisman). Passive "uses left" counter with no inherent rule of
     * its own — the Talisman enters with three and each activation of its tutor ability removes one
     * as part of the cost, so the counters bound how many times it can be used before it is stuck on
     * the battlefield. NOT a keyword counter, so it is intentionally absent from
     * `StateProjector.KEYWORD_COUNTER_MAP`.
     */
    const val WISH = "wish"

    /**
     * Revival counter (FDN — Nine-Lives Familiar). Passive "lives left" counter with no inherent
     * rule of its own — the Familiar enters with eight if you cast it, and its dies trigger reads
     * the last-known count (via
     * `DynamicAmounts.lastKnownSourceCounters(CounterTypeFilter.Named(Counters.REVIVAL))`) to
     * return itself with one fewer. NOT a keyword counter, so it is intentionally absent from
     * `StateProjector.KEYWORD_COUNTER_MAP`.
     */
    const val REVIVAL = "revival"

    /**
     * Ingenuity counter (SPM — Lady Octopus, Inspired Inventor). Passive storage counter with no
     * inherent rule of its own — Lady Octopus's first/second-draw triggers each add one and her
     * {T} ability reads the count (via `DynamicAmounts.countersOnSelf(CounterTypeFilter.Named(
     * Counters.INGENUITY))`) to cap the mana value of the artifact she can free-cast from hand.
     * NOT a keyword counter, so it is intentionally absent from `StateProjector.KEYWORD_COUNTER_MAP`.
     */
    const val INGENUITY = "ingenuity"

    /**
     * Film counter (SPM — Peter Parker's Camera). Passive "uses left" counter with no inherent rule
     * of its own — the Camera enters with three (`EntersWithCounters(CounterTypeFilter.Named(
     * Counters.FILM), count = 3, selfOnly = true)`) and each activation of its copy ability removes
     * one as part of the cost (`Costs.RemoveCounterFromSelf(Counters.FILM, 1)`), bounding how many
     * times it can copy an ability before it sits inert. Same shape as `Counters.WISH` / `Counters.NET`.
     * NOT a keyword counter, so it is intentionally absent from `StateProjector.KEYWORD_COUNTER_MAP`.
     */
    const val FILM = "film"

    /** Harness marker counter — the Infinity Stones' "once harnessed" binary state. */
    const val HARNESS = "harness"

    /**
     * Hone counter (The Hobbit). CR 122.1j: "A hone counter on an Equipment gives +1/+0 to any
     * creature that Equipment is attached to." The bonus belongs to the counter rather than to the
     * permanent holding it, so it applies to Equipment that never mention hone — see
     * [CounterType.HONE] for the full contract and where the engine realizes it.
     */
    const val HONE = "hone"

    /**
     * Skewer counter (WOE — Rotisserie Elemental). A tally counter with no inherent rule: the
     * Elemental accumulates one per combat-damage hit, and the size of the impulse-exile it can
     * cash itself in for is read straight off the tally. Same shape as `Counters.FILM` /
     * `Counters.WISH`. NOT a keyword counter, so it is intentionally absent from
     * `StateProjector.KEYWORD_COUNTER_MAP`.
     */
    const val SKEWER = "skewer"

    /**
     * Energy counter (Kaladesh block onward, CR 107.14). Unlike every other entry in this object,
     * energy counters are placed on **players**, not permanents (CR 122.1 — "a marker placed on an
     * object or player"), the same way poison counters are (see [CounterType.POISON] usage via
     * `CountersComponent` on a player entity). "You get {E}{E}{E}" places counters on the controller
     * (`Effects.AddCounters(Counters.ENERGY, 3, EffectTarget.Controller)` — no new plumbing needed,
     * `AddCountersExecutor` already supports player-shaped targets for the same reason poison does).
     * "Pay {E}" (CR 107.14) removes one as part of a cost; "pay any amount of {E}" (Galvanic
     * Discharge) is the resolution-time variable form — see `Effects.PayCounters`. Reading a
     * player's current total: `DynamicAmount.PlayerCounterCount(Counters.ENERGY, player)`.
     * NOT a keyword counter, so it is intentionally absent from `StateProjector.KEYWORD_COUNTER_MAP`.
     */
    const val ENERGY = "energy"

    /**
     * Ice counter (SOI — Thing in the Ice). Passive "thaw countdown" counter with no inherent rule
     * of its own — Thing in the Ice enters with four (`EntersWithCounters(CounterTypeFilter.Named(
     * Counters.ICE), count = 4, selfOnly = true)`) and its instant/sorcery cast trigger removes one,
     * then transforms the permanent once the tally reaches zero (a `Gate.WhenCondition` on
     * `Conditions.SourceCounterCountAtMost(Counters.ICE, 0)`). Same countdown shape as
     * `Counters.WISH` / `Counters.FILM`, but read down to zero rather than spent as a cost — and
     * per the printed ruling, removing the last counter *any other way* does not transform it.
     * NOT a keyword counter, so it is intentionally absent from `StateProjector.KEYWORD_COUNTER_MAP`.
     */
    const val ICE = "ice"

    /**
     * Plan counter (MSH — the Plan enchantment cycle: Political Triumph, Rewrite History,
     * Construct a Cosmic Cube, Robot Domination, Death to Our Enemies, Claim the Kingdom).
     * Passive named counter with no inherent rule of its own — each Plan enchantment's own
     * "whenever …" trigger adds one, and a second ability gated on
     * `Conditions.SourceCounterCountAtLeast(Counters.PLAN, N)` fires when the Nth one lands and
     * sacrifices the enchantment. Same accumulate-then-threshold shape as `Counters.POINT` /
     * `Counters.LANDMARK`, but the payoff removes the source instead of transforming it.
     * NOT a keyword counter, so it is intentionally absent from `StateProjector.KEYWORD_COUNTER_MAP`.
     */
    const val PLAN = "plan"

    /**
     * Invasion counter (MSH — Alien Invasion). Passive tally counter with no inherent rule of its
     * own — the enchantment's begin-combat trigger reads the count (via
     * `DynamicAmounts.countersOnSelf(CounterTypeFilter.Named(Counters.INVASION))`) to size the
     * +1/+1 counters on the Alien token it just made, then adds one more, so each combat's Alien
     * is one bigger than the last. Same tally shape as `Counters.SKEWER`.
     * NOT a keyword counter, so it is intentionally absent from `StateProjector.KEYWORD_COUNTER_MAP`.
     */
    const val INVASION = "invasion"

    /**
     * Wildcard sentinel for triggers/events that fire on counters of *any* type, e.g.
     * "whenever one or more counters are put on a creature you control" (Stalwart Successor).
     * A [com.wingedsheep.sdk.scripting.EventPattern.CountersPlacedEvent] with this `counterType`
     * matches every counter-placement event regardless of the counter kind.
     */
    const val ANY = "any"
}
