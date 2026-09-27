package com.wingedsheep.sdk.core

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

/**
 * A kind of counter (CR 122.1) — the one spelling of "which counter" across cards, effects, events
 * and state.
 *
 * Counter kinds are open: the game can name any word a card prints, so this is a value type over its
 * canonical id rather than a closed enum. The companion names every kind a card in the corpus uses
 * ([KNOWN]); a card that needs a new one adds a constant here (and to the web client's
 * `CounterType` mirror) — the corpus test in `mtg-sets` rejects a card that names a kind
 * [KNOWN] does not list, which is the typo safety a closed enum used to give.
 *
 * Where a position means "a counter of any kind" it is typed `CounterType?` and `null` is the
 * wildcard.
 *
 * @property name The canonical id: `PLUS_ONE_PLUS_ONE`, `FIRST_STRIKE`, `CHARGE`. This is what
 * serializes, what the client receives, and what equality compares.
 */
@Serializable(with = CounterTypeSerializer::class)
@JvmInline
value class CounterType(val name: String) {

    /**
     * The spelling a card prints: `+1/+1` and `-1/-0` for the stat counters (CR 122.1a), otherwise
     * the id in lower case with spaces (`first strike`, `stun`).
     */
    val printed: String
        get() = STAT_ID.matchEntire(name)?.destructured?.let { (sign1, n1, sign2, n2) ->
            "${SIGNS.getValue(sign1)}${NUMBERS.indexOf(n1)}/${SIGNS.getValue(sign2)}${NUMBERS.indexOf(n2)}"
        } ?: name.lowercase().replace('_', ' ')

    override fun toString(): String = name

    companion object {
        val PLUS_ONE_PLUS_ONE = CounterType("PLUS_ONE_PLUS_ONE")
        val MINUS_ONE_MINUS_ONE = CounterType("MINUS_ONE_MINUS_ONE")
        val PLUS_ONE_PLUS_ZERO = CounterType("PLUS_ONE_PLUS_ZERO")
        val PLUS_ZERO_PLUS_ONE = CounterType("PLUS_ZERO_PLUS_ONE")

        /**
         * +2/+0 and +0/+2 counters (CR 122.1a — a +X/+Y counter adds X to power and Y to toughness).
         * Distinct kinds from two +1/+0 counters, which matters for anything that counts counters.
         * Frankenstein's Monster enters with a +2/+0, +1/+1, or +0/+2 counter per creature card exiled.
         */
        val PLUS_TWO_PLUS_ZERO = CounterType("PLUS_TWO_PLUS_ZERO")
        val PLUS_ZERO_PLUS_TWO = CounterType("PLUS_ZERO_PLUS_TWO")
        val MINUS_ONE_MINUS_ZERO = CounterType("MINUS_ONE_MINUS_ZERO")
        val MINUS_ZERO_MINUS_ONE = CounterType("MINUS_ZERO_MINUS_ONE")

        /**
         * The Fallen Empires stat counters. CR 122.1a defines a +X/+Y counter generally, but the engine
         * enumerates the kinds it can sum, so each printed size needs its own constant:
         * `+1/+2` (Armor Thrull), `+2/+2` (Soul Exchange), `-2/-2` (Ebon Praetor).
         */
        val PLUS_ONE_PLUS_TWO = CounterType("PLUS_ONE_PLUS_TWO")

        /** +2/+2 counter (FEM — Soul Exchange). */
        val PLUS_TWO_PLUS_TWO = CounterType("PLUS_TWO_PLUS_TWO")

        /** -2/-2 counter (FEM — Ebon Praetor). */
        val MINUS_TWO_MINUS_TWO = CounterType("MINUS_TWO_MINUS_TWO")
        val LOYALTY = CounterType("LOYALTY")

        /**
         * Defense counter (CR 310.4). A battle's defense *is* its number of defense counters
         * (CR 310.4c): it enters with as many as its printed defense number, damage removes that
         * many (CR 120.3h), and a battle at 0 is put into its owner's graveyard (CR 704.5v/w). The
         * battle analogue of [LOYALTY].
         */
        val DEFENSE = CounterType("DEFENSE")
        val CHARGE = CounterType("CHARGE")
        val GEM = CounterType("GEM")
        val POISON = CounterType("POISON")
        val SILVER = CounterType("SILVER")
        val GOLD = CounterType("GOLD")
        val PLAGUE = CounterType("PLAGUE")
        val TRAP = CounterType("TRAP")
        val FATE = CounterType("FATE")
        val DEPLETION = CounterType("DEPLETION")
        val EGG = CounterType("EGG")
        val LORE = CounterType("LORE")
        val AIM = CounterType("AIM")
        val STUN = CounterType("STUN")

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
        val SHIELD = CounterType("SHIELD")
        val FINALITY = CounterType("FINALITY")
        val SUPPLY = CounterType("SUPPLY")
        val FLYING = CounterType("FLYING")
        val FIRST_STRIKE = CounterType("FIRST_STRIKE")
        val DOUBLE_STRIKE = CounterType("DOUBLE_STRIKE")
        val VIGILANCE = CounterType("VIGILANCE")
        val LIFELINK = CounterType("LIFELINK")
        val INDESTRUCTIBLE = CounterType("INDESTRUCTIBLE")
        val DEATHTOUCH = CounterType("DEATHTOUCH")
        val TRAMPLE = CounterType("TRAMPLE")
        val HEXPROOF = CounterType("HEXPROOF")
        val REACH = CounterType("REACH")

        /**
         * Haste counter (MSH — Super-Adaptoid). Keyword counter (CR 122.1b / 613.1f): the permanent
         * gains haste for as long as it has one. Wired through `StateProjector.KEYWORD_COUNTER_MAP`.
         */
        val HASTE = CounterType("HASTE")

        /**
         * Menace counter (MSH — Super-Adaptoid). Keyword counter (CR 122.1b / 613.1f): the permanent
         * gains menace for as long as it has one. Wired through `StateProjector.KEYWORD_COUNTER_MAP`.
         */
        val MENACE = CounterType("MENACE")
        val STASH = CounterType("STASH")

        /**
         * Croak counter (VOW — Grolnok, the Omnivore). A pure marker: it grants nothing on its own
         * and exists only so the card's play permission has something to filter exile by, the same
         * role [STASH] plays for Tinybones, Bauble Burglar.
         */
        val CROAK = CounterType("CROAK")
        val BLIGHT = CounterType("BLIGHT")
        val COIN = CounterType("COIN")
        val FLOOD = CounterType("FLOOD")
        val CHORUS = CounterType("CHORUS")
        val DREAM = CounterType("DREAM")
        val QUEST = CounterType("QUEST")
        val GROWTH = CounterType("GROWTH")
        val TIME = CounterType("TIME")
        val FEATHER = CounterType("FEATHER")
        val HOURGLASS = CounterType("HOURGLASS")

        /**
         * Decayed counter (Tarkir: Dragonstorm). A keyword-ability counter (CR 702.147a): a creature
         * with one or more decayed counters has Decayed — "This creature can't block" and "When this
         * creature attacks, sacrifice it at end of combat." Granted to *any* creature, independent of
         * its printed abilities (e.g. Rot-Curse Rakshasa's Renew). The behavior is realized by the
         * engine off the counter, mirroring how the printed [com.wingedsheep.sdk.dsl.card]`.decayed()`
         * helper composes the same static + triggered ability.
         */
        val DECAYED = CounterType("DECAYED")

        /**
         * Hope counter (LTR — Dawn of a New Age). Passive counter: no inherent rule, the card
         * referencing it reads the count via `DynamicAmounts.countersOnSelf(...)`.
         */
        val HOPE = CounterType("HOPE")

        /**
         * Verse counter (LTR — Lost Isle Calling). Passive counter accumulated on a Saga-like
         * permanent; the card itself reads the count.
         */
        val VERSE = CounterType("VERSE")

        /**
         * Influence counter (LTR — Palantír of Orthanc). Passive counter the card's own abilities
         * scale off of.
         */
        val INFLUENCE = CounterType("INFLUENCE")

        /**
         * Burden counter (LTR — The One Ring). Passive counter that the card's own legendary-rule
         * and damage trigger read; the engine has no inherent behavior tied to it.
         */
        val BURDEN = CounterType("BURDEN")

        /**
         * Loot counter (OTJ — Bandit's Haul). Passive storage counter with no inherent rule; the
         * card's own abilities accumulate it (commit-a-crime trigger) and spend it (remove two as an
         * activation cost to draw).
         */
        val LOOT = CounterType("LOOT")

        /**
         * Wind counter (ARN — Cyclone). Passive counter accumulated each upkeep; the card reads the
         * count to scale its pay-or-sacrifice cost and the damage it deals. No inherent rule.
         */
        val WIND = CounterType("WIND")

        /**
         * Nest counter (DSK — Twitching Doll). Passive storage counter with no inherent rule; the
         * card's own abilities accumulate it (a mana-ability adds one per activation) and read the
         * count to scale a token-creation payoff. No inherent rule.
         */
        val NEST = CounterType("NEST")

        /**
         * Page counter (SOS — Diary of Dreams). Passive storage counter with no inherent rule; the
         * card's own abilities accumulate it (an instant/sorcery-cast trigger adds one) and read the
         * count to reduce an activated ability's cost. No inherent rule.
         */
        val PAGE = CounterType("PAGE")

        /**
         * Hoofprint counter (LRW — Hoofprints of the Stag). Passive storage counter with no inherent
         * rule; the enchantment's draw trigger accumulates one and its activated ability spends four
         * to make a 4/4 flying Elemental. No inherent rule.
         */
        val HOOFPRINT = CounterType("HOOFPRINT")

        /**
         * Mannequin counter (LRW — Makeshift Mannequin). A pure marker with no inherent rule: it is
         * the thing the reanimated creature's granted "when this becomes the target of a spell or
         * ability, sacrifice it" ability is keyed to, via
         * [com.wingedsheep.sdk.scripting.Duration.WhileAffectedHasCounter]. Remove the counter and the
         * drawback goes with it.
         */
        val MANNEQUIN = CounterType("MANNEQUIN")

        /**
         * Rev counter (DSK — Chainsaw). Passive storage counter with no inherent rule; the card's own
         * abilities accumulate it (a "whenever one or more creatures die" trigger adds one) and read
         * the count to scale the equipped creature's power bonus (+X/+0). No inherent rule.
         */
        val REV = CounterType("REV")

        /**
         * Bloodstain counter (MKM — Blood Spatter Analysis). Passive storage counter with no inherent
         * rule; the enchantment's own "whenever one or more creatures die" trigger accumulates one and
         * the same trigger reads the count to decide whether to sacrifice itself at five or more. No
         * inherent rule.
         */
        val BLOODSTAIN = CounterType("BLOODSTAIN")

        /**
         * Blood counter (RAV — Bloodletter Quill). Passive storage counter with no inherent rule; the
         * artifact's draw ability puts one on as part of its activation cost and the same ability reads
         * the running count to size the life lost, while a second ability spends {U}{B} to take one
         * back off. No inherent rule.
         */
        val BLOOD = CounterType("BLOOD")

        /**
         * Soul counter (FDN — Ravenous Amulet). Passive storage counter with no inherent rule; the
         * card's own abilities accumulate it (a "sacrifice a creature: draw a card" activation adds
         * one) and its sacrifice ability reads the count to size the life each opponent loses. No
         * inherent rule.
         */
        val SOUL = CounterType("SOUL")

        /**
         * Divinity counter (CHK — Myojin cycle). Passive counter with no inherent rule; each Myojin's
         * own static and activated abilities check for or remove it.
         */
        val DIVINITY = CounterType("DIVINITY")

        /**
         * Doom counter (ATQ — Armageddon Clock). Passive counter accumulated one-per-upkeep; the card
         * reads the count to scale the damage it deals to each player in the draw step, and a {4}
         * activated ability removes one. No inherent rule.
         */
        val DOOM = CounterType("DOOM")

        /**
         * Possession counter (DSK — Unwilling Vessel). Passive storage counter with no inherent rule;
         * Eerie triggers accumulate it (an enchantment you control entering / fully unlocking a Room
         * each add one) and the card's dies trigger reads the count to size the X/X Spirit token it
         * leaves behind. No inherent rule.
         */
        val POSSESSION = CounterType("POSSESSION")

        /**
         * Fire counter (TLA — War Balloon; later Fated Firepower / "Fated" cards). Passive named
         * counter with no inherent rule of its own — the card referencing it reads the count (e.g.
         * "As long as this Vehicle has three or more fire counters on it, it's an artifact creature")
         * via `Conditions.SourceCounterCountAtLeast(...)` / `DynamicAmounts.countersOnSelf(...)`.
         * NOT a keyword counter, so it is intentionally absent from `StateProjector.KEYWORD_COUNTER_MAP`.
         */
        val FIRE = CounterType("FIRE")

        /**
         * Conqueror counter (TLA — Zhao, the Moon Slayer). Passive named counter with no inherent
         * rule of its own — the card referencing it reads the count (e.g. "As long as Zhao has a
         * conqueror counter on him, nonbasic lands are Mountains") via
         * `Conditions.SourceCounterCountAtLeast(...)` / `DynamicAmounts.countersOnSelf(...)`.
         * NOT a keyword counter, so it is intentionally absent from `StateProjector.KEYWORD_COUNTER_MAP`.
         */
        val CONQUEROR = CounterType("CONQUEROR")

        /**
         * Net counter (LCI — Braided Net). Passive named counter with no inherent rule of its
         * own — the card enters with three (an `EntersWithCounters` replacement with
         * [NET]) and removes one as an activation cost
         * (`Costs.RemoveCounterFromSelf(CounterType.NET, 1)`).
         * NOT a keyword counter, so it is intentionally absent from `StateProjector.KEYWORD_COUNTER_MAP`.
         */
        val NET = CounterType("NET")

        /**
         * Landmark counter (LCI — Treasure Map). Passive named counter with no inherent rule of its
         * own — Treasure Map's activated ability adds one per activation and reads the count (via
         * `Conditions.SourceCounterCountAtLeast(...)`) to remove three, transform into Treasure Cove,
         * and make three Treasures.
         * NOT a keyword counter, so it is intentionally absent from `StateProjector.KEYWORD_COUNTER_MAP`.
         */
        val LANDMARK = CounterType("LANDMARK")

        /**
         * Dread counter (LCI — Grasping Shadows). Passive named counter with no inherent rule of its
         * own — Grasping Shadows adds one whenever a creature you control attacks alone and reads the
         * count (via `Conditions.SourceCounterCountAtLeast(...)`) to transform into Shadows' Lair,
         * whose activated ability spends one (`Costs.RemoveCounterFromSelf(CounterType.DREAD, 1)`).
         * NOT a keyword counter, so it is intentionally absent from `StateProjector.KEYWORD_COUNTER_MAP`.
         */
        val DREAD = CounterType("DREAD")
        val SPORE = CounterType("SPORE")

        /**
         * Incubation counter (FDN — Drake Hatcher). Passive storage counter with no inherent rule; the
         * card's own abilities accumulate it (a combat-damage trigger adds one per point of damage) and
         * spend it (remove three as an activation cost to hatch a Drake token). NOT a keyword counter,
         * so it is intentionally absent from `StateProjector.KEYWORD_COUNTER_MAP`.
         * Not MTG's Incubate/incubator-token mechanic.
         */
        val INCUBATION = CounterType("INCUBATION")

        /**
         * Fellowship counter (FDN — Banner of Kinship). Passive storage counter with no inherent rule;
         * the Banner enters with one per creature of its chosen type and its static ability reads the
         * count to size the anthem. NOT a keyword counter, so it is intentionally absent from
         * `StateProjector.KEYWORD_COUNTER_MAP`.
         */
        val FELLOWSHIP = CounterType("FELLOWSHIP")

        /**
         * Bait counter (FDN — Fishing Pole). Passive storage counter with no inherent rule; the
         * Equipment's granted activated ability accumulates one and its "equipped creature becomes
         * untapped" trigger spends one to reel in a Fish token. No inherent rule.
         */
        val BAIT = CounterType("BAIT")

        /**
         * Bore counter (LCI — Brass's Tunnel-Grinder). Passive named counter with no inherent rule of
         * its own — Brass's Tunnel-Grinder adds one at its end step if you descended this turn and reads
         * the count (via `Conditions.SourceCounterCountAtLeast(...)`) to remove three and transform into
         * Tecutlan, the Searing Rift. NOT a keyword counter, so it is intentionally absent from
         * `StateProjector.KEYWORD_COUNTER_MAP`.
         */
        val BORE = CounterType("BORE")

        /**
         * Point counter (LCI — Contested Game Ball). Passive storage counter with no inherent rule of
         * its own — Contested Game Ball's activated ability adds one per activation and reads the count
         * (via `Conditions.SourceCounterCountAtLeast(...)`) to sacrifice itself and create a Treasure
         * once it has five or more. NOT a keyword counter, so it is intentionally absent from
         * `StateProjector.KEYWORD_COUNTER_MAP`.
         */
        val POINT = CounterType("POINT")

        /**
         * Wish counter (ELD — Wishclaw Talisman). Passive "uses left" counter with no inherent rule of
         * its own — the Talisman enters with three and each activation of its tutor ability removes one
         * as part of the cost, so the counters bound how many times it can be used before it is stuck on
         * the battlefield. NOT a keyword counter, so it is intentionally absent from
         * `StateProjector.KEYWORD_COUNTER_MAP`.
         */
        val WISH = CounterType("WISH")

        /**
         * Revival counter (FDN — Nine-Lives Familiar). Passive "lives left" counter with no inherent
         * rule of its own — the Familiar enters with eight if you cast it, and its dies trigger reads
         * the last-known count (via
         * `DynamicAmounts.lastKnownSourceCounters(CounterType.REVIVAL)`) to
         * return itself with one fewer. NOT a keyword counter, so it is intentionally absent from
         * `StateProjector.KEYWORD_COUNTER_MAP`.
         */
        val REVIVAL = CounterType("REVIVAL")

        /**
         * Ingenuity counter (SPM — Lady Octopus, Inspired Inventor). Passive storage counter with no
         * inherent rule of its own — Lady Octopus's first/second-draw triggers each add one and her
         * {T} ability reads the count (via `DynamicAmounts.countersOnSelf(
         * CounterType.INGENUITY)`) to cap the mana value of the artifact she can free-cast from hand.
         * NOT a keyword counter, so it is intentionally absent from `StateProjector.KEYWORD_COUNTER_MAP`.
         */
        val INGENUITY = CounterType("INGENUITY")

        /**
         * Film counter (SPM — Peter Parker's Camera). Passive "uses left" counter with no inherent rule
         * of its own — the Camera enters with three (`EntersWithCounters(
         * CounterType.FILM, count = 3, selfOnly = true)`) and each activation of its copy ability removes
         * one as part of the cost (`Costs.RemoveCounterFromSelf(CounterType.FILM, 1)`), bounding how many
         * times it can copy an ability before it sits inert. Same shape as [WISH] / [NET].
         * NOT a keyword counter, so it is intentionally absent from `StateProjector.KEYWORD_COUNTER_MAP`.
         */
        val FILM = CounterType("FILM")

        /**
         * Skewer counter (WOE — Rotisserie Elemental). A tally counter with no inherent rule: the
         * Elemental accumulates one per combat-damage hit, and the size of the impulse-exile it can
         * cash itself in for is read straight off the tally. Same shape as [FILM] /
         * [WISH]. NOT a keyword counter, so it is intentionally absent from
         * `StateProjector.KEYWORD_COUNTER_MAP`.
         */
        val SKEWER = CounterType("SKEWER")

        /**
         * Energy counter (Kaladesh block onward, CR 107.14). Unlike most kinds,
         * energy counters are placed on **players**, not permanents (CR 122.1 — "a marker placed on an
         * object or player"), the same way poison counters are (see [POISON] usage via
         * `CountersComponent` on a player entity). "You get {E}{E}{E}" places counters on the controller
         * (`Effects.AddCounters(CounterType.ENERGY, 3, EffectTarget.Controller)` — no new plumbing needed,
         * `AddCountersExecutor` already supports player-shaped targets for the same reason poison does).
         * "Pay {E}" (CR 107.14) removes one as part of a cost; "pay any amount of {E}" (Galvanic
         * Discharge) is the resolution-time variable form — see `Effects.PayCounters`. Reading a
         * player's current total: `DynamicAmount.PlayerCounterCount(CounterType.ENERGY, player)`.
         * NOT a keyword counter, so it is intentionally absent from `StateProjector.KEYWORD_COUNTER_MAP`.
         */
        val ENERGY = CounterType("ENERGY")

        /**
         * Ice counter (SOI — Thing in the Ice). Passive "thaw countdown" counter with no inherent rule
         * of its own — Thing in the Ice enters with four (`EntersWithCounters(
         * CounterType.ICE, count = 4, selfOnly = true)`) and its instant/sorcery cast trigger removes one,
         * then transforms the permanent once the tally reaches zero (a `Gate.WhenCondition` on
         * `Conditions.SourceCounterCountAtMost(CounterType.ICE, 0)`). Same countdown shape as
         * [WISH] / [FILM], but read down to zero rather than spent as a cost — and
         * per the printed ruling, removing the last counter *any other way* does not transform it.
         * NOT a keyword counter, so it is intentionally absent from `StateProjector.KEYWORD_COUNTER_MAP`.
         */
        val ICE = CounterType("ICE")

        /**
         * Omen counter (VOW — Soulcipher Board). Passive countdown counter with no inherent rule; the
         * artifact enters with three and its "whenever a creature card is put into your graveyard"
         * trigger removes one, transforming the artifact once the last one is gone.
         */
        val OMEN = CounterType("OMEN")

        /**
         * Suspect counter (VOW — Investigator's Journal). Passive storage counter with no inherent
         * rule; the artifact enters with one per creature the most-creatured player controls and its
         * activated ability removes one to draw. Unrelated to the *suspected* keyword action
         * (CR 701.58), which grants menace and can't-block and places no counter.
         */
        val SUSPECT = CounterType("SUSPECT")

        /**
         * Plan counter (MSH — the Plan enchantment cycle: Political Triumph, Rewrite History,
         * Construct a Cosmic Cube, Robot Domination, Death to Our Enemies, Claim the Kingdom).
         * Passive named counter with no inherent rule of its own — each Plan enchantment's own
         * "whenever …" trigger adds one, and a second ability gated on
         * `Conditions.SourceCounterCountAtLeast(CounterType.PLAN, N)` fires when the Nth one lands and
         * sacrifices the enchantment. Same accumulate-then-threshold shape as [POINT] /
         * [LANDMARK], but the payoff removes the source instead of transforming it.
         * NOT a keyword counter, so it is intentionally absent from `StateProjector.KEYWORD_COUNTER_MAP`.
         */
        val PLAN = CounterType("PLAN")

        /**
         * Invasion counter (MSH — Alien Invasion). Passive tally counter with no inherent rule of its
         * own — the enchantment's begin-combat trigger reads the count (via
         * `DynamicAmounts.countersOnSelf(CounterType.INVASION)`) to size the
         * +1/+1 counters on the Alien token it just made, then adds one more, so each combat's Alien
         * is one bigger than the last. Same tally shape as [SKEWER].
         * NOT a keyword counter, so it is intentionally absent from `StateProjector.KEYWORD_COUNTER_MAP`.
         */
        val INVASION = CounterType("INVASION")

        /**
         * Unlock counter (MKM — Cryptex). Passive accumulate-then-threshold counter with no inherent
         * rule of its own: Cryptex's mana ability adds one per activation, and its sacrifice ability is
         * gated on `Conditions.SourceCounterCountAtLeast(CounterType.UNLOCK, 5)`. Same shape as
         * [POINT] / [PLAN], and like them the payoff removes the source.
         * NOT a keyword counter, so it is intentionally absent from `StateProjector.KEYWORD_COUNTER_MAP`.
         */
        val UNLOCK = CounterType("UNLOCK")

        /**
         * Harness counter (Marvel's Spider-Man Infinity Stones). A binary "harnessed" marker: the Stone's
         * activated Harness ability places one, and its `∞` ability is gated on the Stone having a harness
         * counter (CR-style "as long as this permanent has a harness counter"). Not a resource — exactly
         * one is ever placed; it models the permanent "once harnessed" state that resets if the Stone
         * leaves the battlefield.
         */
        val HARNESS = CounterType("HARNESS")

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
        val HONE = CounterType("HONE")

        /**
         * Storage counter (The Dark — City of Shadows; the later storage-land cycles). A passive
         * counter with no inherent rule of its own, exactly like [LOOT] and [NEST]: the card that
         * places them is also the only thing that reads them. City of Shadows exiles a creature to add
         * one, then taps to add {C} for each.
         */
        val STORAGE = CounterType("STORAGE")

        /**
         * Hunger counter (Fasting). A pure bookkeeping counter: it modifies nothing on its own, and the
         * card that uses it reads its own count back through `Conditions.SourceCounterCountAtLeast`.
         */
        val HUNGER = CounterType("HUNGER")

        /**
         * Slime counter (VOW — Toxrill, the Corrosive). A pure marker with no inherent rule of its
         * own, like [STORAGE] and [HUNGER]: Toxrill's own static ability is what reads the tally
         * ("creatures you don't control get -1/-1 for each slime counter on them"), and his dies
         * trigger only asks whether one is present. Per the printed ruling, both of those apply to
         * *any* slime counter, including ones placed by another source — so the counter is deliberately
         * not modelled as an ability of the permanent carrying it.
         *
         * NOT a stat counter: a slime counter shrinks a creature only while a Toxrill is on the
         * battlefield, so it is not one of the stat kinds and stays out of the intrinsic P/T folding in
         * `EffectApplicator.applyCounters`.
         */
        val SLIME = CounterType("SLIME")

        /**
         * Javelin counter (FEM — Icatian Javelineers). A plain resource counter: the creature enters
         * with one and removing it is part of the cost of its ping. The counter does nothing of its
         * own — the card spends it.
         */
        val JAVELIN = CounterType("JAVELIN")

        /**
         * Credit counter (FEM — Icatian Moneychanger). Accrues one per upkeep and is cashed in for
         * life when the creature sacrifices itself. Purely a stored quantity, like [STORAGE].
         */
        val CREDIT = CounterType("CREDIT")

        /**
         * Cube counter (FEM — Delif's Cube). Charged one at a time by the artifact's first ability and
         * spent by its second — the same store-and-spend shape as [STORAGE].
         */
        val CUBE = CounterType("CUBE")

        /**
         * Tide counter (FEM — Homarid, Tidal Influence). Unlike the other stored counters, a tide
         * counter's *exact* count is what matters: the permanent's static effect switches on at
         * exactly one and again at exactly three, and it sheds all of them on reaching four.
         */
        val TIDE = CounterType("TIDE")

        /**
         * Judgment counter (VOW — Faithbound Judge // Sinner's Judgment). A passive
         * accumulate-then-threshold counter with no inherent rule of its own, the same shape as
         * [PLAN] and [UNLOCK]: each face's own upkeep trigger adds one, and a second ability gated on
         * `Conditions.SourceCounterCountAtLeast(CounterType.JUDGMENT, 3)` reads the tally back — the
         * creature face to shed its defender restriction, the Aura face to make the enchanted player
         * lose the game. The two faces do *not* share a tally: they are different objects (CR 400.7),
         * so a disturbed Sinner's Judgment starts at zero.
         * NOT a keyword counter, so it is intentionally absent from `StateProjector.KEYWORD_COUNTER_MAP`.
         */
        val JUDGMENT = CounterType("JUDGMENT")

        /**
         * Bloodline counter (VOW — Edgar, Charmed Groom // Edgar Markov's Coffin). A tally with no
         * inherent rule: the Coffin adds one each upkeep and transforms once it has three.
         */
        val BLOODLINE = CounterType("BLOODLINE")

        /**
         * Invitation counter (VOW — Wedding Announcement). A tally with no inherent rule: the
         * enchantment adds one each end step and transforms once it has three.
         */
        val INVITATION = CounterType("INVITATION")

        /**
         * Impostor counter (MKM — Illicit Masquerade). A pure marker: the enchantment's dies trigger
         * keys off any impostor counter, including ones another source placed.
         */
        val IMPOSTOR = CounterType("IMPOSTOR")

        /**
         * Devotion counter (CHK — Bloodthirsty Ogre). A tally with no inherent rule: one tap
         * ability adds one, another reads the count back as the size of a -X/-X.
         */
        val DEVOTION = CounterType("DEVOTION")

        /** Every counter kind the SDK names, in declaration order. */
        val KNOWN: List<CounterType> = listOf(
            PLUS_ONE_PLUS_ONE,
            MINUS_ONE_MINUS_ONE,
            PLUS_ONE_PLUS_ZERO,
            PLUS_ZERO_PLUS_ONE,
            PLUS_TWO_PLUS_ZERO,
            PLUS_ZERO_PLUS_TWO,
            MINUS_ONE_MINUS_ZERO,
            MINUS_ZERO_MINUS_ONE,
            PLUS_ONE_PLUS_TWO,
            PLUS_TWO_PLUS_TWO,
            MINUS_TWO_MINUS_TWO,
            LOYALTY,
            DEFENSE,
            CHARGE,
            GEM,
            POISON,
            SILVER,
            GOLD,
            PLAGUE,
            TRAP,
            FATE,
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
            CROAK,
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
            HOOFPRINT,
            MANNEQUIN,
            REV,
            BLOODSTAIN,
            BLOOD,
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
            SUSPECT,
            PLAN,
            INVASION,
            UNLOCK,
            HARNESS,
            HONE,
            STORAGE,
            HUNGER,
            SLIME,
            JAVELIN,
            CREDIT,
            CUBE,
            TIDE,
            JUDGMENT,
            BLOODLINE,
            INVITATION,
            IMPOSTOR,
            DEVOTION,
        )

        /**
         * The counter kind a [spelling] names — a canonical id (`PLUS_ONE_PLUS_ONE`, `FIRST_STRIKE`)
         * or a printed spelling (`+1/+1`, `first strike`), in any case. The one lenient normaliser:
         * every conversion from text goes through it. It never guesses — a name it does not know is
         * simply a new kind, never a fallback to `+1/+1`.
         */
        fun of(spelling: String): CounterType {
            val trimmed = spelling.trim()
            STAT_PRINTED.matchEntire(trimmed)?.destructured?.let { (sign1, n1, sign2, n2) ->
                val power = "${SIGN_WORDS.getValue(sign1)}_${NUMBERS[n1.toInt()]}"
                val toughness = "${SIGN_WORDS.getValue(sign2)}_${NUMBERS[n2.toInt()]}"
                return CounterType("${power}_$toughness")
            }
            return CounterType(trimmed.uppercase().replace(' ', '_'))
        }

        private val NUMBERS = listOf("ZERO", "ONE", "TWO", "THREE", "FOUR", "FIVE", "SIX", "SEVEN", "EIGHT", "NINE")
        private val SIGNS = mapOf("PLUS" to "+", "MINUS" to "-")
        private val SIGN_WORDS = SIGNS.entries.associate { (word, sign) -> sign to word }
        private val STAT_ID = NUMBERS.joinToString("|").let { n -> Regex("""(PLUS|MINUS)_($n)_(PLUS|MINUS)_($n)""") }
        private val STAT_PRINTED = Regex("""([+-])(\d)/([+-])(\d)""")
    }
}

/**
 * Serializes a [CounterType] as its canonical id — a plain JSON string, so it also works as a map
 * key (`{"PLUS_ONE_PLUS_ONE": 2}`).
 *
 * Decoding is lenient through [CounterType.of], and a JSON decoder also accepts the shapes of the
 * retired `CounterTypeFilter` (`"PlusOnePlusOne"`, `"Loyalty"`, `{"type": "Named", "name": "stun"}`)
 * so card definitions pinned into stored replays still load. Its any-kind wildcard (`"CounterAny"`)
 * has no [CounterType] — that position is `null` now — so it fails to decode, and the replay falls
 * back to the live card rather than reading "any counter" as one particular kind.
 */
object CounterTypeSerializer : KSerializer<CounterType> {

    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("com.wingedsheep.sdk.core.CounterType", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: CounterType) = encoder.encodeString(value.name)

    override fun deserialize(decoder: Decoder): CounterType {
        if (decoder !is JsonDecoder) return CounterType.of(decoder.decodeString())
        return when (val element = decoder.decodeJsonElement()) {
            is JsonPrimitive -> fromText(element.content)
            is JsonObject -> {
                val type = (element["type"] as? JsonPrimitive)?.contentOrNull
                    ?: throw SerializationException("Unrecognised counter type: $element")
                if (type == "Named") {
                    val name = (element["name"] as? JsonPrimitive)?.contentOrNull
                        ?: throw SerializationException("Named counter type without a name: $element")
                    CounterType.of(name)
                } else {
                    fromText(type)
                }
            }
            else -> throw SerializationException("Unrecognised counter type: $element")
        }
    }

    private fun fromText(text: String): CounterType {
        if (text == "CounterAny") {
            throw SerializationException("\"CounterAny\" is no longer a counter type; an any-kind position is null")
        }
        return LEGACY_FILTER_NAMES[text] ?: CounterType.of(text)
    }

    /** The retired `CounterTypeFilter` singletons, by serial name. */
    private val LEGACY_FILTER_NAMES = mapOf(
        "PlusOnePlusOne" to CounterType.PLUS_ONE_PLUS_ONE,
        "MinusOneMinusOne" to CounterType.MINUS_ONE_MINUS_ONE,
        "PlusOnePlusZero" to CounterType.PLUS_ONE_PLUS_ZERO,
        "PlusZeroPlusOne" to CounterType.PLUS_ZERO_PLUS_ONE,
        "MinusOneMinusZero" to CounterType.MINUS_ONE_MINUS_ZERO,
        "MinusZeroMinusOne" to CounterType.MINUS_ZERO_MINUS_ONE,
    )
}
