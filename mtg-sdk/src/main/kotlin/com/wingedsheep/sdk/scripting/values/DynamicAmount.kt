package com.wingedsheep.sdk.scripting.values

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.events.CounterTypeFilter
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.conditions.Condition
import com.wingedsheep.sdk.scripting.text.TextReplaceable
import com.wingedsheep.sdk.scripting.text.TextReplacer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import com.wingedsheep.sdk.dsl.station

// Note: Player, Zone, and GameObjectFilter are in the same package (com.wingedsheep.sdk.scripting)

/**
 * Trackable per-player statistics that reset at end of turn.
 *
 * Used by [DynamicAmount.TurnTracking] to read turn-tracking counters from player components.
 */
@Serializable
enum class TurnTracker {
    /** Count of all creatures (including tokens) that died under a player's control this turn. */
    CREATURES_DIED,
    /** Count of nontoken creatures put into a player's graveyard from the battlefield this turn. */
    NONTOKEN_CREATURES_DIED,
    /**
     * Count of creatures (including tokens) that left the battlefield under a player's control this
     * turn — regardless of destination (death, exile, bounce, …). Distinct from [CREATURES_DIED]
     * (only battlefield→graveyard). Powers "for each creature that left the battlefield under your
     * control this turn" (Kutzil's Flanker).
     */
    CREATURES_LEFT_BATTLEFIELD,
    /** Count of creatures exiled from opponents' control this turn. */
    OPPONENT_CREATURES_EXILED,
    /** Count of opponents who lost life this turn. */
    OPPONENTS_WHO_LOST_LIFE,
    /** Total damage received by the player this turn. */
    DAMAGE_RECEIVED,
    /** Total damage dealt to the player this turn by artifact sources (Reverse Polarity). */
    DAMAGE_RECEIVED_FROM_ARTIFACTS,
    /** Total amount of life the player has gained this turn. */
    LIFE_GAINED,
    /**
     * Indicator (0 or 1) that the player has lost life at least once this turn. Backed by the
     * `LifeLostThisTurnComponent` marker; use `Compare(TurnTracking(player, LIFE_LOST), GTE,
     * Fixed(1))` for boolean checks and [LIFE_LOST_AMOUNT] when you need how much was lost.
     */
    LIFE_LOST,
    /**
     * Total amount of life the player has lost this turn — damage taken, life-loss effects and
     * life paid as a cost all count. The amount-carrying counterpart of [LIFE_LOST] and the mirror
     * of [LIFE_GAINED]; life gained never nets against it (Rowan, Scion of War: gaining 3 and
     * losing 3 in a turn leaves the amount lost at 3).
     */
    LIFE_LOST_AMOUNT,
    /** Indicator (0 or 1) that the player declared at least one attacker this turn. */
    PLAYER_ATTACKED,
    /** Indicator (0 or 1) that the player was dealt combat damage this turn. */
    DEALT_COMBAT_DAMAGE,
    /**
     * Indicator (0 or 1) that the player was dealt combat damage by a legendary creature this
     * turn. Backed by `WasDealtCombatDamageByLegendaryCreatureThisTurnComponent`, set when a
     * legendary creature deals combat damage to the player and cleared at end of turn. Powers
     * "if an opponent was dealt combat damage by a legendary creature this turn" (Blitzball).
     */
    DEALT_COMBAT_DAMAGE_BY_LEGENDARY_CREATURE,
    /** Indicator (0 or 1) that the player put one or more counters on a creature this turn. */
    COUNTERS_PUT_ON_CREATURE,
    /** Number of land cards the player played this turn (derived from `LandDropsComponent`). */
    LANDS_PLAYED,
    /**
     * Number of lands that entered the battlefield under the player's control this turn.
     * Unlike [LANDS_PLAYED] this counts *any* land ETB — land drops, Lander-token search,
     * Cultivate-style "put a land onto the battlefield" effects, opponent's
     * gift-a-land effects, etc. — so it matches "a land entered the battlefield under your
     * control this turn" wording (Bioengineered Future).
     */
    LANDS_ENTERED_UNDER_CONTROL,
    /**
     * Number of **nonland** permanents that entered the battlefield under the player's control
     * this turn — the complement of [LANDS_ENTERED_UNDER_CONTROL] over the same per-player entry
     * log. Tokens count (they're permanents); a permanent that is both a land and a creature does
     * not (it's a land). Entries are counted per *entry event*, so a permanent that leaves and
     * re-enters counts twice (CR 400.7 — it's a new object each time), and an entry stays counted
     * even after the permanent leaves the battlefield or changes controller.
     *
     * `Compare(TurnTracking(You, NONLAND_PERMANENTS_ENTERED), GTE, Fixed(2))` is the **Celebration**
     * ability word (Wilds of Eldraine, CR 207.2c — flavor only, no keyword): "two or more nonland
     * permanents entered the battlefield under your control this turn". Reach for it via
     * `Conditions.Celebration`.
     */
    NONLAND_PERMANENTS_ENTERED,
    /**
     * The number of creatures that entered the battlefield under the player's control this turn —
     * the creature-typed slice of the same per-player entry log behind [NONLAND_PERMANENTS_ENTERED]
     * (an entry counts if it was a creature at the moment it entered). Entries are counted per entry
     * event (a creature that leaves and re-enters counts twice, CR 400.7) and stay counted after the
     * creature later leaves. `Compare(TurnTracking(You, CREATURES_ENTERED_UNDER_CONTROL), GTE,
     * Fixed(2))` backs "two or more creatures entered the battlefield under your control this turn"
     * (Spider-UK). Reach for the threshold form via `Conditions.CreaturesEnteredThisTurn`.
     */
    CREATURES_ENTERED_UNDER_CONTROL,
    /** Indicator (0 or 1) that the player sacrificed at least one Food this turn. */
    FOOD_SACRIFICED,
    /**
     * Indicator (0 or 1) that the player sacrificed at least one artifact this turn — the
     * card-type sibling of [FOOD_SACRIFICED], recorded by the same central sacrifice hook and
     * read off the projected type line, so a permanent that was only an artifact through a
     * continuous effect still counts.
     *
     * Backs Murders at Karlov Manor's "if you've sacrificed an artifact this turn" wording
     * (Suspicious Detonation's cost reduction, Furtive Courier's evasion). Turn history, not a
     * board scan: it stays set for the rest of the turn even once the artifact has left the
     * graveyard.
     */
    ARTIFACT_SACRIFICED,
    /** Total cards that left the player's graveyard this turn (Bonecache Overseer). */
    CARDS_LEFT_GRAVEYARD,
    /**
     * Number of times the player descended this turn (CR 700.11) — count of nontoken
     * permanent cards put into the player's graveyard from any zone. Backs the descend
     * gate and the descend N / fathomless descent ability words.
     */
    DESCENDED,
    /**
     * Number of creature cards put into the player's graveyard from any zone this turn — the
     * creature-typed sibling of [DESCENDED], keyed on the card's owner and excluding tokens
     * (a token isn't a card, CR 111.6). Backed by
     * `CreatureCardsPutIntoGraveyardThisTurnComponent`, cleared at end of turn.
     *
     * Turn history, not a graveyard scan: the card need not still be there, so reanimating it
     * later in the turn doesn't clear the count. Reach for the threshold form via
     * `Conditions.CreatureCardPutIntoYourGraveyardThisTurn` — Murders at Karlov Manor's "if a
     * creature card was put into your graveyard from anywhere this turn" (Macabre Reconstruction).
     */
    CREATURE_CARDS_PUT_INTO_GRAVEYARD,
    /**
     * Number of cards the player has drawn this turn (CR 120). Backed by
     * `CardsDrawnThisTurnComponent`, reset to 0 for every player at the start of each turn.
     * Powers "equal to the number of cards you've drawn this turn" (Duelist of the Mind).
     */
    CARDS_DRAWN,
    /**
     * Number of cards the player has discarded this turn (CR 701.8). Backed by
     * `CardsDiscardedThisTurnComponent`, reset to an empty list for every player at the start of
     * each turn. Every discard site (cost, effect, cycling, CR 514.1 hand-size cleanup) records
     * into it. Powers "draw a card for each card you've discarded this turn" (Green Goblin,
     * Revenant) and, in threshold form, the Mayhem gate (CR 702.187 — via
     * `Conditions.YouDiscardedThisCardThisTurn`, a per-card membership check on the same component).
     */
    CARDS_DISCARDED,
    /**
     * Number of cards put into exile this turn, keyed on each card's owner. Summed across every
     * player (via `Player.Each`) it gives the game-wide count of cards put into exile this turn.
     * Backed by `CardsPutIntoExileThisTurnComponent`, reset to 0 for every player at the start of
     * each turn. Tokens are excluded — a token briefly placed in exile isn't a card. Powers
     * "if one or more cards were put into exile this turn" (Ennis, Debate Moderator).
     */
    CARDS_PUT_INTO_EXILE,
    /**
     * Number of permanents the player has sacrificed this turn (any permanent type,
     * controller-scoped). Backed by `PermanentsSacrificedThisTurnComponent`, reset to 0 for
     * every player at the start of each turn. Distinct from the game-wide
     * `GameState.permanentsSacrificedThisTurn` cost-reduction counter. Powers "if you sacrificed
     * one or more permanents this turn, ... deals that much damage" (Sawblade Skinripper).
     */
    PERMANENTS_SACRIFICED,
    /**
     * Total noncombat damage red sources this player controlled have dealt this turn
     * (controller-scoped). Backed by `RedNoncombatDamageDealtThisTurnComponent`, reset to 0 for
     * every player at end of turn. Powers Temple of Power's transform gate ("red sources you
     * controlled dealt 4 or more noncombat damage this turn", back of Ojer Axonil, Deepest Might).
     */
    RED_NONCOMBAT_DAMAGE_DEALT,
    /**
     * Number of distinct sources the player controlled that dealt damage this turn — Case of the
     * Burning Masks's "three or more sources you controlled dealt damage this turn". Backed by
     * `DamageSourcesThisTurnComponent`, recorded on the source's controller *at the time it dealt
     * the damage* and cleared at end of turn.
     *
     * Counts objects, not damage events: a permanent that deals damage several times in a turn is
     * one source, and one that left the battlefield and returned is two (CR 400.7). Abilities are
     * not sources — the source of an activated or triggered ability is the object it came from — so
     * a creature that pings twice with its own ability still counts once.
     */
    DAMAGE_SOURCES,
    /**
     * Number of *distinct* elemental bending keyword actions ([com.wingedsheep.sdk.core.BendType]:
     * waterbend, earthbend, firebend, airbend) the player has performed this turn — 0 through 4.
     * Backed by `BendsThisTurnComponent`, reset to empty for every player at the start of each
     * turn. `Compare(TurnTracking(You, DISTINCT_BENDS), GTE, Fixed(4))` powers "if you've done all
     * four this turn" (Avatar Aang, CR 701.65–701.67 / 702.189).
     */
    DISTINCT_BENDS,
    /**
     * Count of artifacts (including tokens) put into a graveyard from the battlefield under the
     * player's control this turn — the artifact-typed sibling of [CREATURES_DIED], recorded by the
     * same `ZoneTransitionService` battlefield→graveyard hook and credited to the *last-known
     * controller*, so a stolen artifact that is then destroyed counts for the thief.
     *
     * Read it with [Player.Each] for the **game-wide** total: "the number of artifacts that were
     * put into graveyards from the battlefield this turn" (Anzrag's Rampage) is
     * `TurnTracking(Player.Each, ARTIFACTS_DIED)`, which sums every player's tally. Every artifact
     * that hits a graveyard from the battlefield had exactly one controller, so the sum is the
     * game-wide count with no double-counting. There is deliberately no separate game-scoped
     * tracker: [Player.Each] already spans the table, and a second component would be a second
     * thing to keep in sync.
     *
     * Type is read off the *last-known* projected type line, so an artifact animated into a
     * creature still counts as an artifact, and a permanent that was only an artifact through a
     * continuous effect counts while that effect applied.
     */
    ARTIFACTS_DIED,
    /**
     * How many cards the player had in hand **at the beginning of this turn** — a snapshot taken
     * in the turn's untap step, before any draw, not a running count. Backed by
     * `CardsInHandAtTurnStartComponent`, rewritten for every player at each turn start.
     *
     * The one tracker in this enum that does not accumulate, and that is the point: an upkeep
     * ability asking "did you have no cards in hand at the beginning of this turn" cannot use a
     * live hand count, because by the upkeep the answer has already been changed by the very
     * things the card is measuring. `Compare(TurnTracking(You, CARDS_IN_HAND_AT_TURN_START), GTE,
     * Fixed(1))` powers Mindstorm Crown.
     */
    CARDS_IN_HAND_AT_TURN_START;

    fun descriptionFor(player: Player): String = when (this) {
        CREATURES_DIED -> "the number of creatures that died under ${player.possessive} control this turn"
        NONTOKEN_CREATURES_DIED -> "the number of nontoken creatures put into ${player.possessive} graveyard from the battlefield this turn"
        CREATURES_LEFT_BATTLEFIELD -> "the number of creatures that left the battlefield under ${player.possessive} control this turn"
        OPPONENT_CREATURES_EXILED -> "the number of creatures that were exiled under your opponents' control this turn"
        OPPONENTS_WHO_LOST_LIFE -> "the number of opponents who lost life this turn"
        DAMAGE_RECEIVED -> "the damage already dealt to that player this turn"
        DAMAGE_RECEIVED_FROM_ARTIFACTS -> "the damage dealt to ${player.description} so far this turn by artifacts"
        LIFE_GAINED -> "the amount of life ${player.possessive} gained this turn"
        LIFE_LOST -> "whether ${player.description} lost life this turn"
        LIFE_LOST_AMOUNT -> "the amount of life ${player.possessive} lost this turn"
        PLAYER_ATTACKED -> "whether ${player.description} attacked this turn"
        DEALT_COMBAT_DAMAGE -> "whether ${player.description} were dealt combat damage this turn"
        DEALT_COMBAT_DAMAGE_BY_LEGENDARY_CREATURE ->
            "whether ${player.description} were dealt combat damage by a legendary creature this turn"
        COUNTERS_PUT_ON_CREATURE -> "whether ${player.description} put a counter on a creature this turn"
        LANDS_PLAYED -> "the number of lands ${player.description} played this turn"
        LANDS_ENTERED_UNDER_CONTROL -> "the number of lands that entered the battlefield under ${player.possessive} control this turn"
        NONLAND_PERMANENTS_ENTERED -> "the number of nonland permanents that entered the battlefield under ${player.possessive} control this turn"
        CREATURES_ENTERED_UNDER_CONTROL -> "the number of creatures that entered the battlefield under ${player.possessive} control this turn"
        FOOD_SACRIFICED -> "whether ${player.description} sacrificed a Food this turn"
        ARTIFACT_SACRIFICED -> "whether ${player.description} sacrificed an artifact this turn"
        CARDS_LEFT_GRAVEYARD -> "the number of cards that left ${player.possessive} graveyard this turn"
        DESCENDED -> "the number of times ${player.description} descended this turn"
        CREATURE_CARDS_PUT_INTO_GRAVEYARD ->
            "the number of creature cards put into ${player.possessive} graveyard this turn"
        CARDS_DRAWN -> "the number of cards ${player.description} have drawn this turn"
        CARDS_DISCARDED -> "the number of cards ${player.description} have discarded this turn"
        CARDS_PUT_INTO_EXILE -> "the number of cards put into exile this turn"
        PERMANENTS_SACRIFICED -> "the number of permanents ${player.description} sacrificed this turn"
        RED_NONCOMBAT_DAMAGE_DEALT -> "the noncombat damage red sources ${player.description} controlled dealt this turn"
        DAMAGE_SOURCES -> "the number of sources ${player.description} controlled that dealt damage this turn"
        DISTINCT_BENDS -> "the number of different ways ${player.description} bent this turn"
        ARTIFACTS_DIED -> if (player == Player.Each) {
            "the number of artifacts that were put into graveyards from the battlefield this turn"
        } else {
            "the number of artifacts put into graveyards from the battlefield under " +
                "${player.possessive} control this turn"
        }
        CARDS_IN_HAND_AT_TURN_START ->
            "the number of cards ${player.description} had in hand at the beginning of this turn"
    }
}

/**
 * Keys for [DynamicAmount.ContextProperty] — values that an effect reads from its
 * resolution context (trigger payload, additional-cost values, target list, linked-exile
 * pile attached to the source) rather than from any fixed entity property.
 *
 * Each key carries its own oracle-text [description] used in card text generation.
 */
@Serializable
enum class ContextPropertyKey(val description: String) {
    /** The amount of damage in the current trigger payload (Tephraderm, Wall of Hope, …). */
    TRIGGER_DAMAGE_AMOUNT("the damage dealt"),
    /**
     * The damage recipient creature's toughness at the instant the triggering damage was dealt
     * (last-known information — the creature may have died from the same damage). Read by payoffs
     * keyed on "damage equal to that creature's toughness" (Taii Wakeen, Perfect Shot). `null`/0
     * when the recipient was not a creature.
     */
    TRIGGER_RECIPIENT_TOUGHNESS("that creature's toughness"),
    /**
     * The amount of damage prevented by a prevention shield's `onPrevented` reaction context
     * (New Way Forward, Deflecting Palm) — "that much" / "that many". Shares the trigger-amount
     * slot in [com.wingedsheep.engine.handlers.EffectContext].
     */
    PREVENTED_DAMAGE_AMOUNT("the prevented damage"),
    /** The amount of life gained in the current trigger payload (False Cure, Lich's Mastery). */
    TRIGGER_LIFE_GAINED("the life gained"),
    /** The amount of life lost in the current trigger payload (Lich's Mastery). */
    TRIGGER_LIFE_LOST("the life lost"),
    /** Number of cards exiled as an additional cost (Chill Haunting). */
    ADDITIONAL_COST_EXILED_COUNT("the number of cards exiled"),
    /** Number of (still-legal) targets in the current effect context. */
    TARGET_COUNT("the number of targets"),
    /** Number of +1/+1 counters on the source as it last existed on the battlefield (Hooded Hydra). */
    LAST_KNOWN_PLUS_ONE_COUNTER_COUNT("the number of +1/+1 counters on it"),
    /**
     * Number of counters placed in the triggering [CountersPlacedEvent] payload —
     * used by abilities like Simic Ascendancy: "Whenever one or more +1/+1 counters
     * are put on a creature you control, put **that many** growth counters on this
     * enchantment."
     */
    TRIGGER_COUNTERS_PLACED_AMOUNT("that many"),
    /** Total counters of any kind on the source as it last existed on the battlefield (Shadow Urchin). */
    LAST_KNOWN_TOTAL_COUNTER_COUNT("the number of counters on it"),
    /** Total cards exiled and linked to the source permanent (Veteran Survivor). */
    LINKED_EXILE_CARD_COUNT("the number of cards exiled with this creature"),
    /** Distinct card types among cards exiled and linked to the source permanent (Keen-Eyed Curator). */
    LINKED_EXILE_DISTINCT_CARD_TYPE_COUNT("the number of card types among cards exiled with this creature"),
    /**
     * Number of times a mode was chosen for the modal spell that fired this trigger.
     * Counts mode selections, not distinct modes (Spree-style cards may pick the same
     * mode several times — see Riku of Many Paths: "X is the number of times you chose
     * a mode for that spell, not the number of distinct modes").
     */
    MODES_CHOSEN_ON_TRIGGERING_SPELL("the number of times you chose a mode for that spell"),
    /**
     * Total mana spent to cast the spell that fired this trigger. Distinct from
     * [DynamicAmount.TotalManaSpent], which reads the *current resolving object's* own cast —
     * this reads the **triggering** spell's cast (a "Whenever you cast an instant or sorcery
     * spell, …, where X is the amount of mana spent to cast that spell" payoff that lives on a
     * separate permanent). Populated from `SpellCastEvent.totalManaSpent`. `0` when the trigger
     * was not driven by a spell cast. Used by Aberrant Manawurm and Expressive Firedancer.
     */
    MANA_SPENT_ON_TRIGGERING_SPELL("the amount of mana spent to cast that spell"),
    /**
     * Number of distinct *colors* of mana spent to cast the spell that fired this trigger (0–5).
     * Distinct from [DynamicAmount.DistinctColorsManaSpent], which reads the *current resolving
     * object's* own cast (Converge) — this reads the **triggering** spell's payment (a "Whenever
     * you cast an instant or sorcery spell, … for each color of mana spent to cast that spell"
     * payoff living on a separate permanent). Colorless is not a color (CR 105.1) and never
     * contributes. Populated from `SpellCastEvent.distinctColorsSpent`; `0` when the trigger was
     * not driven by a spell cast. Used by Magmablood Archaic.
     */
    COLORS_SPENT_ON_TRIGGERING_SPELL("the number of colors of mana spent to cast that spell"),
    /**
     * Mana value (CR 202.3) of the spell that fired this trigger. Distinct from
     * [MANA_SPENT_ON_TRIGGERING_SPELL] (mana actually paid) — this reads the spell's printed
     * mana value, unaffected by cost reductions / alternative costs. Populated from
     * `SpellCastEvent.manaValue`; `0` when the trigger was not driven by a spell cast. Used by
     * Kellan, the Kid — "a permanent spell with equal or lesser mana value."
     */
    TRIGGERING_SPELL_MANA_VALUE("the mana value of that spell"),
    /**
     * The value chosen for `{X}` on the spell that fired this trigger (CR 601.2b). Distinct from
     * [MANA_SPENT_ON_TRIGGERING_SPELL] (total mana paid) and [TRIGGERING_SPELL_MANA_VALUE] (printed
     * mana value, which counts each {X} as 0): this reads the actual X the caster announced. `0`
     * when the trigger was not driven by a spell cast or the spell had no {X}. Populated from
     * `SpellCastEvent.xValue`. Pair with `SpellCastPredicate.HasXInCost`. Used by Geometer's
     * Arthropod — "look at the top X cards of your library."
     */
    X_VALUE_OF_TRIGGERING_SPELL("X"),
    /**
     * Number of cards actually looked at by the scry that fired this trigger. Equals the
     * scry N parameter unless the library held fewer cards. Read by "Whenever you scry,
     * ... for each card looked at" payoffs (Celeborn the Wise, Elrond Master of Healing).
     */
    TRIGGER_SCRY_COUNT("the number of cards looked at"),
    /**
     * Damage in excess of lethal dealt to the creature target in the trigger payload
     * (CR 120.4a). Set from `DamageDealtEvent.excessAmount`; non-zero only when the trigger
     * is a `DealsDamageEvent(requireExcess = true)`. Used by Fall of Cair Andros — "amass
     * Orcs X, where X is the excess damage."
     */
    TRIGGER_EXCESS_DAMAGE_AMOUNT("the excess damage"),
    /**
     * The value N of the discover that fired this trigger (CR 701.57) — the mana-value threshold
     * used, not the number of cards exiled. Read by "Whenever you discover, discover again for the
     * same value" payoffs (Curator of Sun's Creation). `0` when the trigger was not driven by a
     * discover.
     */
    TRIGGER_DISCOVER_VALUE("the same value"),
    /**
     * The total **last-known power** of the creatures that died in the batch that fired this
     * trigger (CR 603.2c / 603.10). Summed from each dying creature's power the instant it left
     * the battlefield — the graveyard card's printed power would drop counters and buffs, so the
     * value is captured at detection time. Read by "one or more creatures you control die"
     * batch payoffs keyed on "the total power of those creatures" (The Skullspore Nexus).
     * `0` when the trigger was not a creatures-died batch.
     */
    DIED_BATCH_TOTAL_POWER("the total power of those creatures"),
    /**
     * Number of cards discarded in the batch that fired this trigger (CR 603.2c). Read by
     * "Whenever you discard one or more cards, ... that much / that many" payoffs (Magmakin
     * Artillerist). Populated from `CardsDiscardedEvent.cardIds.size`, so a single discard
     * event of three cards reports `3` while three sequential one-card discards fire three
     * separate triggers reporting `1`. `0` when the trigger was not a discard.
     */
    TRIGGER_DISCARD_COUNT("that much"),
}

/**
 * Sources for dynamic values in effects.
 */
@Serializable
sealed interface DynamicAmount : TextReplaceable<DynamicAmount> {
    val description: String

    companion object {
        /**
         * Pluralize the last word of a filter description for use in counting phrases.
         * Examples: "creature" → "creatures", "land" → "lands", "sorcery" → "sorceries",
         * "Wolf" → "Wolves", "Leech" → "Leeches"
         *
         * Creature types drive most of these, and Magic has plenty that a bare "+s" gets wrong:
         * Wolf/Elf/Dwarf take -ves, and the sibilant endings (Leech, Fish, Sphinx) take -es.
         * Irregular plurals (Ox → Oxen) aren't worth a lookup table — they read acceptably as
         * "Oxes" and cost more in maintenance than they return.
         */
        internal fun pluralize(filterDesc: String): String {
            if (filterDesc.isEmpty()) return "cards"
            val words = filterDesc.split(" ")
            val lastWord = words.last()
            val lower = lastWord.lowercase()
            val plural = when {
                lower.endsWith("s") -> lastWord
                // Wolf → Wolves, Knife → Knives. "Dwarf" is the one Magic spells "Dwarves".
                lower.endsWith("f") -> lastWord.dropLast(1) + "ves"
                lower.endsWith("fe") -> lastWord.dropLast(2) + "ves"
                // Sibilants need the extra syllable: Leech → Leeches, Sphinx → Sphinxes.
                lower.endsWith("ch") || lower.endsWith("sh") ||
                    lower.endsWith("x") || lower.endsWith("z") -> lastWord + "es"
                lower.endsWith("y") && !lower.endsWith("ey") -> lastWord.dropLast(1) + "ies"
                else -> lastWord + "s"
            }
            return (words.dropLast(1) + plural).joinToString(" ")
        }

        /**
         * Strip article from zone displayName for use with possessives.
         * "a graveyard" → "graveyard", "the battlefield" → "battlefield"
         */
        internal fun zoneSimpleName(zone: Zone): String =
            zone.displayName.removePrefix("a ").removePrefix("the ")
    }

    /**
     * Your current life total.
     */
    @SerialName("YourLifeTotal")
    @Serializable
    data object YourLifeTotal : DynamicAmount {
        override val description: String = "your life total"
    }

    /**
     * Life total of a specific player.
     * Generalizes [YourLifeTotal] to support opponent comparisons.
     *
     * Examples:
     * ```kotlin
     * LifeTotal(Player.You)       // your life total
     * LifeTotal(Player.TargetOpponent)  // target opponent's life total
     * ```
     */
    @SerialName("LifeTotal")
    @Serializable
    data class LifeTotal(val player: Player) : DynamicAmount {
        override val description: String = "${player.possessive} life total"
    }

    /**
     * A player's **speed** (Aetherdrift, CR 702.179) — the 0–4 designation that
     * "Start your engines!" begins and the inherent speed trigger raises.
     *
     * A player who has no speed reads as 0 (CR 702.179f), so this is always a plain number and
     * needs no "has speed" guard at the use site.
     *
     * Examples:
     * ```kotlin
     * Speed(Player.You)  // "your speed" — Point the Way's X, The Speed Demon's X
     * ```
     * Comparisons build the max-speed gate: `Compare(Speed(Player.You), EQ, Fixed(Speed.MAX))`,
     * exposed as [com.wingedsheep.sdk.dsl.Conditions.YouHaveMaxSpeed].
     */
    @SerialName("Speed")
    @Serializable
    data class Speed(val player: Player = Player.You) : DynamicAmount {
        override val description: String = "${player.possessive} speed"
    }

    /**
     * How many counters of [counterType] a player currently has — the player-scoped sibling of
     * [EntityProperty]'s [com.wingedsheep.sdk.scripting.values.EntityNumericProperty.CounterCount]
     * (which reads a permanent/object; `EntityReference` has no case for "a player" since players
     * aren't targeted the way permanents are). Counters placed directly on a player rather than a
     * permanent (CR 122.1) — poison ([com.wingedsheep.sdk.core.Counters.POISON]), energy
     * ([com.wingedsheep.sdk.core.Counters.ENERGY], CR 107.14), and rad counters all live here.
     *
     * Examples:
     * ```kotlin
     * PlayerCounterCount(Counters.ENERGY, Player.You)  // "your energy counters" — Longtusk Cub
     * ```
     */
    @SerialName("PlayerCounterCount")
    @Serializable
    data class PlayerCounterCount(val counterType: String, val player: Player = Player.You) : DynamicAmount {
        override val description: String = "${player.possessive} $counterType counters"
    }

    /**
     * The starting life total of a player (e.g., 20 in standard, 40 in commander).
     * Used for conditions like "life total ≤ half your starting life total".
     */
    @SerialName("StartingLifeTotal")
    @Serializable
    data class StartingLifeTotal(val player: Player) : DynamicAmount {
        override val description: String = "${player.possessive} starting life total"
    }

    /**
     * The total amount of unspent mana currently in a player's mana pool (all colours plus
     * colorless plus any restricted-mana entries — the pool's `total`). Used for
     * "as long as you have six or more unspent mana" (Ozai, the Phoenix King).
     */
    @SerialName("UnspentMana")
    @Serializable
    data class UnspentMana(val player: Player) : DynamicAmount {
        override val description: String = "${player.possessive} unspent mana"
    }

    /**
     * Fixed amount (for consistency in the type system).
     */
    @SerialName("Fixed")
    @Serializable
    data class Fixed(val amount: Int) : DynamicAmount {
        override val description: String = "$amount"
    }

    /**
     * A value pulled from the current resolution context — trigger payload, additional-cost
     * accumulator, target list, or a linked-exile pile attached to the source permanent.
     *
     * The key uniquely identifies which contextual quantity to read; the evaluator dispatches
     * on the key. Replaces the per-context one-off cases (TriggerDamageAmount,
     * TriggerLifeGainAmount, TriggerLifeLossAmount, AdditionalCostExiledCount,
     * AdditionalCostBlightAmount, TargetCount, LastKnownCounterCount,
     * LastKnownTotalCounterCount, CardsInLinkedExile, CardTypesInLinkedExile).
     *
     * Examples:
     * ```kotlin
     * ContextProperty(ContextPropertyKey.TRIGGER_DAMAGE_AMOUNT)         // Tephraderm
     * ContextProperty(ContextPropertyKey.LAST_KNOWN_PLUS_ONE_COUNTER_COUNT) // Hooded Hydra
     * ```
     */
    @SerialName("ContextProperty")
    @Serializable
    data class ContextProperty(val key: ContextPropertyKey) : DynamicAmount {
        override val description: String = key.description
    }

    // =========================================================================
    // X Value and Variable References
    // =========================================================================

    /**
     * The X value of the spell (from mana cost).
     * Used for X spells like Fireball.
     */
    @SerialName("XValue")
    @Serializable
    data object XValue : DynamicAmount {
        override val description: String = "X"
    }

    /**
     * The number of counters matching [counterType] the *source* of the current ability had as it
     * last existed on the battlefield (CR 113.7a / 608.2h last-known information). Counters cease
     * to exist on a zone change (CR 122.2), so the count comes from whichever snapshot the
     * resolution context carries:
     *
     *  - the **cost-payment** snapshot, when an activated ability's cost exiles or sacrifices its
     *    own source — Lost Isle Calling: "{4}{U}{U}, Exile this enchantment: Draw a card for each
     *    verse counter on this enchantment. If it had seven or more verse counters on it, take an
     *    extra turn after this one." Both the draw amount and the seven-or-more test read
     *    `LastKnownSourceCounters(CounterTypeFilter.Named(Counters.VERSE))`.
     *  - the **leaves-the-battlefield trigger** snapshot, for a dies/leaves ability reading the
     *    counters its source had as it died — Nine-Lives Familiar: "When this creature dies, if it
     *    had a revival counter on it, return it … with one fewer revival counter on it."
     *
     * The parameterized sibling of [ContextPropertyKey.LAST_KNOWN_PLUS_ONE_COUNTER_COUNT] (fixed to
     * +1/+1) and [ContextPropertyKey.LAST_KNOWN_TOTAL_COUNTER_COUNT] (sums every kind): naming one
     * kind means an unrelated +1/+1 counter can't satisfy "if it had a revival counter on it".
     * Evaluates to `0` when neither snapshot is present; a permanent still on the battlefield
     * should be read with [com.wingedsheep.sdk.dsl.DynamicAmounts.countersOnSelf] instead.
     */
    @SerialName("LastKnownSourceCounters")
    @Serializable
    data class LastKnownSourceCounters(
        val counterType: com.wingedsheep.sdk.scripting.events.CounterTypeFilter
    ) : DynamicAmount {
        override val description: String =
            "the number of ${counterType.description} counters on it".replace("  ", " ")
    }

    /**
     * The total damage dealt to the ability's source this turn, read as last-known information —
     * "where X is the amount of damage dealt to it this turn" (Tangled Colony).
     *
     * Backed by the same per-turn tally the engine already keeps on every permanent that is dealt
     * damage (summed per source-controller and captured onto the `ZoneChangeEvent` when the
     * permanent leaves the battlefield), so it reads correctly from a dies/leaves trigger where
     * the entity itself is already gone. Summing that map gives the total from *all* sources; the
     * per-player split is what
     * [com.wingedsheep.sdk.scripting.effects.EachPlayerDrawsForDamageDealtToSourceEffect]
     * (Grothama, All-Devouring) uses instead.
     *
     * Evaluates to `0` when no snapshot is present — including for a source still on the
     * battlefield, since the tally is only captured on the way out.
     */
    @SerialName("LastKnownDamageDealtToSource")
    @Serializable
    data object LastKnownDamageDealtToSource : DynamicAmount {
        override val description: String = "the amount of damage dealt to it this turn"
    }

    /**
     * The value of `{X}` this object was cast with, read off the *current object* regardless of
     * what zone it is in.
     *
     * Contrast [XValue], which resolves from the transient resolution context
     * (`EffectContext.xValue`) and is therefore only populated while the spell itself is
     * resolving. [CastX] is the durable, object-scoped reading: it survives onto the permanent
     * after the spell resolves, so a "when you cast this spell" trigger, an enters-the-battlefield
     * trigger, the enters-with-counters replacement, and a later activated ability all read the
     * *same* X — the immutable-ECS analogue of mtgish's `ValueX` / `Trigger_ValueXOfThatSpell`.
     *
     * It is backed by a durable component that rides the spell's stable entity onto the
     * battlefield; a card that changes zones becomes a new object (CR 400.7) and stops carrying
     * it, but the value is preserved as last-known information for dies/leaves triggers. A copy of
     * a *permanent* (Clone) does not inherit it (X is not a copiable value, CR 707.2), while a copy
     * of a *spell* on the stack does (it copies the value of X).
     *
     * Example — Hydroid Krasis "enters with X +1/+1 counters" and "when you cast this spell, draw
     * half X cards" both read `DynamicAmount.CastX`.
     */
    @SerialName("CastX")
    @Serializable
    data object CastX : DynamicAmount {
        override val description: String = "X"
    }

    /**
     * A *numeric* value locked in for a [com.wingedsheep.sdk.scripting.ChoiceSlot] as this object
     * was cast, read off the *current object* regardless of zone — the sibling of [CastX] for the
     * other cast-choice slots that carry a number.
     *
     * Currently the only numeric slot is [com.wingedsheep.sdk.scripting.ChoiceSlot.BLIGHT_AMOUNT]
     * (the X declared for a `blight X` additional cost, e.g. Soul Immolation). It reads the durable
     * [com.wingedsheep.sdk.scripting.ChoiceSlot] value off the cast-choices bag, falling back to the
     * resolution context so a spell that never becomes a permanent (an instant/sorcery) still
     * resolves it from the value paid at cast.
     *
     * The non-numeric slots (color, creature type, mode) are read by conditions
     * ([com.wingedsheep.sdk.scripting.conditions.CastChoiceMade] /
     * [com.wingedsheep.sdk.scripting.conditions.CastChoiceIs]) or consumed directly by effects
     * (e.g. token creation), not by [DynamicAmount].
     */
    @SerialName("CastChoice")
    @Serializable
    data class CastChoice(val slot: com.wingedsheep.sdk.scripting.ChoiceSlot) : DynamicAmount {
        override val description: String = "X"
    }

    /**
     * The total amount of mana paid from the pool to cast the current spell.
     *
     * Sums every per-color and colorless bucket recorded on the spell's stack object.
     * For `{X}` spells the X portion is already included (the mana solver routes those
     * payments through the same buckets), so this is **not** the same as [XValue]:
     *  - `XValue` is the chosen value of X (e.g. 3 for Blaze cast with X=3).
     *  - `TotalManaSpent` is the full mana paid (e.g. 4 for `{X}{R}` Blaze with X=3).
     *
     * Used for effects like Memory Deluge: "where X is the amount of mana spent to cast this spell."
     */
    @SerialName("TotalManaSpent")
    @Serializable
    data object TotalManaSpent : DynamicAmount {
        override val description: String = "the total mana spent to cast this spell"
    }

    /**
     * The amount of mana of a specific [color] that was spent on the `{X}` portion of the
     * current spell or activated ability.
     *
     * Distinct from [TotalManaSpent] (which sums every color across the whole cost): this
     * counts only mana paid toward the variable `{X}` symbols, broken down by color. Used
     * by cards whose payoff scales with how much of a color was spent on X — e.g. Soul Burn
     * ("You gain life equal to the amount of {B} spent on X"). Typically paired with an
     * `xManaRestriction` on the spell/ability so the X portion can only be paid with the
     * relevant colors.
     */
    @SerialName("ManaSpentOnX")
    @Serializable
    data class ManaSpentOnX(val color: Color) : DynamicAmount {
        override val description: String = "the amount of {${color.symbol}} spent on X"
    }

    /**
     * The number of mana units produced by a source with [subtype] that were spent to cast the
     * current spell — e.g. Bat Colony's "create a 1/1 black Bat with flying for each mana from a
     * Cave spent to cast it" is `ManaSpentFromSubtype(Subtype.CAVE)`.
     *
     * Evaluated against the source entity's recorded payment (via `ManaSpentReader`), so it resolves
     * correctly whether read while the spell is still on the stack or as the permanent enters and its
     * enters-the-battlefield ability resolves. A permanent put onto the battlefield without being
     * cast spent no mana, so this is 0 for it. The subtype is snapshotted at production, so a
     * Treasure sacrificed for its own mana still counts.
     */
    @SerialName("ManaSpentFromSubtype")
    @Serializable
    data class ManaSpentFromSubtype(val subtype: com.wingedsheep.sdk.core.Subtype) : DynamicAmount {
        override val description: String = "the amount of mana from a ${subtype.value} spent to cast this"
    }

    /**
     * The number of distinct *colors* of mana spent to cast the source spell (0–5).
     *
     * Backs the **Converge** ability word — "Converge — … for each color of mana spent to
     * cast it" — and the classic **Sunburst** counter rule. Counts how many of the five
     * colored buckets (W/U/B/R/G) recorded on the spell's stack object are non-zero; colorless
     * is not a color (CR 105.1) and never contributes. As with [TotalManaSpent], mana spent on
     * the `{X}` portion is already folded into those buckets, so it counts here too.
     *
     * Evaluated against the source entity's recorded payment, so it resolves correctly whether
     * read while the spell is still on the stack or as the permanent enters (the dominant use:
     * feeding `ReplacementEffect.EntersWithDynamicCounters`). A permanent put onto the
     * battlefield without being cast spent no mana, so this is 0 for it.
     */
    @SerialName("DistinctColorsManaSpent")
    @Serializable
    data object DistinctColorsManaSpent : DynamicAmount {
        override val description: String = "the number of colors of mana spent to cast this spell"
    }

    /**
     * A player's **devotion** to one or more colors (CR 700.5): the number of mana symbols of
     * [colors] among the mana costs of permanents that [player] controls. A single-element list
     * is devotion to one color ("your devotion to red"); several colors give devotion to that
     * combination ("your devotion to white and black"), where a symbol that is any of the listed
     * colors is counted once.
     *
     * Every kind of mana symbol that carries a color contributes: a plain colored symbol, both
     * halves of a two-color hybrid ({W/U} adds to both white and blue devotion), a monocolored
     * "twobrid" ({2/B} is a black symbol), and a Phyrexian symbol ({B/P} is a black symbol).
     * Generic, colorless ({C}), and {X} symbols never contribute. Face-down permanents have no
     * mana cost and contribute 0 (CR 711.4). Reads the controller via projected state so
     * control-changing effects are honored.
     *
     * Used by "draw cards equal to your devotion to red" / "gain life equal to your devotion to
     * white" payoffs (Clive, Ifrit's Dominant).
     */
    @SerialName("DevotionTo")
    @Serializable
    data class DevotionTo(
        val colors: List<Color>,
        val player: Player = Player.You
    ) : DynamicAmount {
        override val description: String = buildString {
            append(if (player == Player.You) "your" else player.possessive)
            append(" devotion to ")
            append(colors.joinToString(" and ") { it.displayName.lowercase() })
        }
    }

    /**
     * Reference to a stored variable by name.
     * Used for effects that need to reference a previously computed/stored value.
     * Example: Scapeshift stores "sacrificedCount" and SearchLibrary reads it.
     */
    @SerialName("VariableReference")
    @Serializable
    data class VariableReference(val variableName: String) : DynamicAmount {
        override val description: String = "the stored $variableName"
    }

    /**
     * Mana value of a card stored in a named collection.
     * Reads the first card from the stored collection and returns its mana value.
     * Used for effects like Erratic Explosion: "damage equal to that card's mana value".
     */
    @SerialName("StoredCardManaValue")
    @Serializable
    data class StoredCardManaValue(val collectionName: String) : DynamicAmount {
        override val description: String = "the mana value of that card"
    }

    /**
     * Number of *distinct* entities across several named pipeline collections.
     *
     * Each listed collection is unioned and de-duplicated by entity id, then the size of the
     * union is returned. Use this for "did you affect N *different* objects" payoffs where the
     * same object may have been selected in more than one pipeline step — e.g. Call the Spirit
     * Dragons puts a +1/+1 counter on a Dragon you control of each color (one selection per
     * color) and wins if five *different* Dragons received counters, even though a multicolored
     * Dragon could have been chosen for two colors.
     */
    @SerialName("DistinctEntitiesInCollections")
    @Serializable
    data class DistinctEntitiesInCollections(val collections: List<String>) : DynamicAmount {
        override val description: String = "the number of distinct selected permanents"
    }

    /**
     * Number of distinct *card types* among the cards in the named pipeline collections (the
     * union of every collection, de-duplicated by card type). An artifact creature contributes
     * both "artifact" and "creature". Cards are read by entity id, so the value is correct even
     * after the collection has been moved to another zone — e.g. after being discarded into a
     * graveyard.
     *
     * Used for "draw a card for each card type among cards discarded this way" (Kefka, Court Mage):
     * multiple players each discard into a separate collection and the payoff counts distinct card
     * types across all of them. This is the collection-scoped sibling of
     * [ContextPropertyKey.LINKED_EXILE_DISTINCT_CARD_TYPE_COUNT] (which reads linked-exile cards)
     * and of [SpellsCastThisTurn] with `countDistinctCardTypes = true` (which reads cast history).
     */
    @SerialName("DistinctCardTypesInCollections")
    @Serializable
    data class DistinctCardTypesInCollections(val collections: List<String>) : DynamicAmount {
        override val description: String = "the number of card types among those cards"
    }

    /**
     * Sum of the mana values of *every* card in a named pipeline collection.
     *
     * Unlike [StoredCardManaValue] (which reads only the first card), this totals the mana value
     * of all cards stored under [collectionName]. Cards are read by entity id, so the value is
     * correct even after the collection has been moved to another zone (e.g. milled into the
     * graveyard). Used for "you mill X cards… that player loses life equal to the total mana value
     * of those cards" (Palantír of Orthanc).
     */
    @SerialName("ManaValueSumOfCollection")
    @Serializable
    data class ManaValueSumOfCollection(val collectionName: String) : DynamicAmount {
        override val description: String = "the total mana value of the $collectionName cards"
    }

    // =========================================================================
    // Math Operations - Composable arithmetic on DynamicAmounts
    // =========================================================================

    /**
     * Add two dynamic amounts.
     * Example: Add(Fixed(2), CreaturesYouControl) = "2 + creatures you control"
     */
    @SerialName("Add")
    @Serializable
    data class Add(val left: DynamicAmount, val right: DynamicAmount) : DynamicAmount {
        override val description: String = "(${left.description} + ${right.description})"
        override fun applyTextReplacement(replacer: TextReplacer): DynamicAmount {
            val newLeft = left.applyTextReplacement(replacer)
            val newRight = right.applyTextReplacement(replacer)
            return if (newLeft !== left || newRight !== right) copy(left = newLeft, right = newRight) else this
        }
    }

    /**
     * Subtract one dynamic amount from another.
     */
    @SerialName("Subtract")
    @Serializable
    data class Subtract(val left: DynamicAmount, val right: DynamicAmount) : DynamicAmount {
        override val description: String = "(${left.description} - ${right.description})"
        override fun applyTextReplacement(replacer: TextReplacer): DynamicAmount {
            val newLeft = left.applyTextReplacement(replacer)
            val newRight = right.applyTextReplacement(replacer)
            return if (newLeft !== left || newRight !== right) copy(left = newLeft, right = newRight) else this
        }
    }

    /**
     * Multiply a dynamic amount by a fixed multiplier.
     * Example: Multiply(AggregateBattlefield(Player.EachOpponent, GameObjectFilter.Creature.attacking()), 3)
     */
    @SerialName("Multiply")
    @Serializable
    data class Multiply(val amount: DynamicAmount, val multiplier: Int) : DynamicAmount {
        override val description: String = "$multiplier × ${amount.description}"
        override fun applyTextReplacement(replacer: TextReplacer): DynamicAmount {
            val newAmount = amount.applyTextReplacement(replacer)
            return if (newAmount !== amount) copy(amount = newAmount) else this
        }
    }

    /**
     * Raise a fixed [base] to the power of a dynamic [exponent] (`base^exponent`).
     * Example: `Power(base = 2, exponent = CastX)` — "draws 2ˣ cards" (Mathemagics).
     * A non-positive exponent yields `base^0 = 1` (since `2⁰ = 1`).
     */
    @SerialName("Power")
    @Serializable
    data class Power(val base: Int, val exponent: DynamicAmount) : DynamicAmount {
        override val description: String = "$base^${exponent.description}"
        override fun applyTextReplacement(replacer: TextReplacer): DynamicAmount {
            val newExponent = exponent.applyTextReplacement(replacer)
            return if (newExponent !== exponent) copy(exponent = newExponent) else this
        }
    }

    /**
     * Take the maximum of zero and the amount (clamp negative to zero).
     * Useful for difference calculations that should not go negative.
     * Example: IfPositive(Subtract(Count(Player.TargetOpponent, Zone.HAND), Count(Player.You, Zone.HAND)))
     */
    @SerialName("IfPositive")
    @Serializable
    data class IfPositive(val amount: DynamicAmount) : DynamicAmount {
        override val description: String = "${amount.description} (if positive)"
        override fun applyTextReplacement(replacer: TextReplacer): DynamicAmount {
            val newAmount = amount.applyTextReplacement(replacer)
            return if (newAmount !== amount) copy(amount = newAmount) else this
        }
    }

    /**
     * Maximum of two amounts.
     */
    @SerialName("Max")
    @Serializable
    data class Max(val left: DynamicAmount, val right: DynamicAmount) : DynamicAmount {
        override val description: String = "max(${left.description}, ${right.description})"
        override fun applyTextReplacement(replacer: TextReplacer): DynamicAmount {
            val newLeft = left.applyTextReplacement(replacer)
            val newRight = right.applyTextReplacement(replacer)
            return if (newLeft !== left || newRight !== right) copy(left = newLeft, right = newRight) else this
        }
    }

    /**
     * Minimum of two amounts.
     */
    @SerialName("Min")
    @Serializable
    data class Min(val left: DynamicAmount, val right: DynamicAmount) : DynamicAmount {
        override val description: String = "min(${left.description}, ${right.description})"
        override fun applyTextReplacement(replacer: TextReplacer): DynamicAmount {
            val newLeft = left.applyTextReplacement(replacer)
            val newRight = right.applyTextReplacement(replacer)
            return if (newLeft !== left || newRight !== right) copy(left = newLeft, right = newRight) else this
        }
    }

    /**
     * Conditional amount: evaluates to one of two amounts based on a condition.
     * Example: "2 if enchanted creature is a Wizard, otherwise 1"
     */
    @SerialName("Conditional")
    @Serializable
    data class Conditional(
        val condition: Condition,
        val ifTrue: DynamicAmount,
        val ifFalse: DynamicAmount
    ) : DynamicAmount {
        override val description: String = "${ifTrue.description} ${condition.description}, otherwise ${ifFalse.description}"
        override fun applyTextReplacement(replacer: TextReplacer): DynamicAmount {
            val newCondition = (condition as? TextReplaceable<*>)?.applyTextReplacement(replacer) as? Condition ?: condition
            val newIfTrue = ifTrue.applyTextReplacement(replacer)
            val newIfFalse = ifFalse.applyTextReplacement(replacer)
            return if (newCondition !== condition || newIfTrue !== ifTrue || newIfFalse !== ifFalse)
                copy(condition = newCondition, ifTrue = newIfTrue, ifFalse = newIfFalse) else this
        }
    }

    /**
     * Count of players in [scope] for whom [condition] evaluates to true.
     *
     * The condition is evaluated with the context's controllerId rebound to each candidate
     * player in turn, so `Player.You` inside the condition refers to the player being tested.
     * Used for effects like "draw an additional card for each opponent who has one or fewer
     * cards in hand" (Bandit's Talent, level 3).
     */
    @SerialName("CountPlayersWith")
    @Serializable
    data class CountPlayersWith(
        val scope: Player,
        val condition: Condition
    ) : DynamicAmount {
        override val description: String = "the number of ${scope.description} for whom ${condition.description}"
        override fun applyTextReplacement(replacer: TextReplacer): DynamicAmount {
            val newCondition = (condition as? TextReplaceable<*>)?.applyTextReplacement(replacer) as? Condition ?: condition
            return if (newCondition !== condition) copy(condition = newCondition) else this
        }
    }

    // =========================================================================
    // Zone-based Counting — generic counting primitives
    // =========================================================================

    /**
     * Count game objects in a zone matching a unified filter.
     * This is the preferred counting primitive using the new unified filter system.
     *
     * Examples:
     * ```kotlin
     * // Cards in your graveyard
     * Count(Player.You, Zone.GRAVEYARD)
     *
     * // Creature cards in your graveyard
     * Count(Player.You, Zone.GRAVEYARD, GameObjectFilter.Creature)
     *
     * // Cards in target opponent's hand
     * Count(Player.TargetOpponent, Zone.HAND)
     * ```
     *
     * @param player Whose zone to count in
     * @param zone Which zone to count
     * @param filter Filter for what to count (default: any)
     */
    @SerialName("Count")
    @Serializable
    data class Count(
        val player: Player,
        val zone: Zone,
        val filter: GameObjectFilter = GameObjectFilter.Companion.Any
    ) : DynamicAmount {
        override fun applyTextReplacement(replacer: TextReplacer): DynamicAmount {
            val newFilter = filter.applyTextReplacement(replacer)
            return if (newFilter !== filter) copy(filter = newFilter) else this
        }
        override val description: String = buildString {
            append("the number of ")
            append(pluralize(filter.description))
            append(" ")
            when (zone) {
                Zone.BATTLEFIELD -> {
                    when (player) {
                        Player.You -> append("you control")
                        Player.TargetOpponent -> append("${player.description} controls")
                        Player.Each -> append("on the battlefield")
                        else -> append("${player.description} controls")
                    }
                }
                else -> {
                    append("in ")
                    append(player.possessive)
                    append(" ")
                    append(zoneSimpleName(zone))
                }
            }
        }
    }

    /**
     * The number of unlocked doors among Rooms controlled by [player] (CR 709.5). A Room
     * permanent contributes one per unlocked door face, so a Room with both doors unlocked
     * counts as two.
     *
     * With [distinctNames] = true, counts the distinct printed names among those unlocked
     * door faces instead of the raw total — the "different names among unlocked doors" shape
     * (Promising Stairs). This reads per-face door state, which no entity-level aggregate
     * ([AggregateBattlefield]/[Count]) can see, since a single Room entity carries two faces.
     *
     * Feeds the standard [Compare] machinery: pair with `ComparisonOperator.GTE` for
     * "two or more unlocked doors" gating (Rampaging Soulrager) or a [WinGameEffect]-gated
     * "eight or more different names" (Promising Stairs), and use it directly as a count
     * (Misty Salon's X/X token).
     */
    @SerialName("UnlockedDoors")
    @Serializable
    data class UnlockedDoors(
        val player: Player = Player.You,
        val distinctNames: Boolean = false,
    ) : DynamicAmount {
        override val description: String = buildString {
            append("the number of ")
            append(if (distinctNames) "different names among unlocked doors of Rooms " else "unlocked doors among Rooms ")
            append(if (player == Player.You) "you control" else "${player.description} controls")
        }
    }

    /**
     * Generic battlefield aggregation primitive.
     * Queries permanents on the battlefield, filters them, optionally maps to a numeric
     * property, and applies an aggregation function.
     *
     * This unifies counting, max, min, and sum operations over battlefield entities:
     *
     * ```kotlin
     * // Count creatures you control
     * AggregateBattlefield(Player.You, GameObjectFilter.Creature)
     *
     * // Greatest mana value among permanents you control (Rush of Knowledge)
     * AggregateBattlefield(Player.You, aggregation = Aggregation.MAX, property = CardNumericProperty.MANA_VALUE)
     *
     * // Greatest power among creatures you control
     * AggregateBattlefield(Player.You, GameObjectFilter.Creature, Aggregation.MAX, CardNumericProperty.POWER)
     * ```
     *
     * Prefer using the fluent DSL via [DynamicAmounts.battlefield] rather than constructing directly.
     *
     * @param player Whose permanents to query
     * @param filter Filter for which permanents to include
     * @param aggregation How to aggregate (COUNT, MAX, MIN, SUM)
     * @param property Which numeric property to aggregate (ignored for COUNT)
     * @param counterType When set together with [Aggregation.SUM] (or MAX/MIN), the value aggregated
     *   per matched permanent is the count of this kind of counter on it, rather than [property].
     *   This expresses "the total number of <kind> counters among <filter> you control" — e.g. Tom
     *   Bombadil's "four or more lore counters among Sagas you control". Takes precedence over
     *   [property] when both are present.
     */
    @SerialName("AggregateBattlefield")
    @Serializable
    data class AggregateBattlefield(
        val player: Player,
        val filter: GameObjectFilter = GameObjectFilter.Companion.Any,
        val aggregation: Aggregation = Aggregation.COUNT,
        val property: CardNumericProperty? = null,
        val excludeSelf: Boolean = false,
        val counterType: CounterTypeFilter? = null
    ) : DynamicAmount {
        override fun applyTextReplacement(replacer: TextReplacer): DynamicAmount {
            val newFilter = filter.applyTextReplacement(replacer)
            return if (newFilter !== filter) copy(filter = newFilter) else this
        }
        override val description: String = buildString {
            when (aggregation) {
                Aggregation.COUNT -> {
                    append("the number of ")
                    if (excludeSelf) append("other ")
                    append(pluralize(filter.description))
                }
                Aggregation.MAX -> {
                    append("the greatest ${property?.description ?: "value"} among ")
                    if (excludeSelf) append("other ")
                    append(pluralize(filter.description))
                }
                Aggregation.MIN -> {
                    append("the least ${property?.description ?: "value"} among ")
                    if (excludeSelf) append("other ")
                    append(pluralize(filter.description))
                }
                Aggregation.SUM -> {
                    val what = counterType?.let { "${it.description} counters" } ?: (property?.description ?: "value")
                    append("the total $what ")
                    append(if (counterType != null) "among " else "of ")
                    if (excludeSelf) append("other ")
                    append(pluralize(filter.description))
                }
                Aggregation.DISTINCT_TYPES -> {
                    append("the number of card types among ")
                    if (excludeSelf) append("other ")
                    append(pluralize(filter.description))
                }
                Aggregation.DISTINCT_PERMANENT_TYPES -> {
                    append("the number of permanent types among ")
                    if (excludeSelf) append("other ")
                    append(pluralize(filter.description))
                }
                Aggregation.DISTINCT_COLORS -> {
                    append("the number of colors among ")
                    if (excludeSelf) append("other ")
                    append(pluralize(filter.description))
                }
                Aggregation.DISTINCT_COLOR_PAIRS -> {
                    append("the number of different color pairs among ")
                    if (excludeSelf) append("other ")
                    append(pluralize(filter.description))
                }
                Aggregation.DISTINCT_NAMES -> {
                    append("the number of differently named ")
                    if (excludeSelf) append("other ")
                    append(pluralize(filter.description))
                }
                Aggregation.DISTINCT_BASIC_LAND_SUBTYPES -> {
                    append("the number of basic land types among ")
                    if (excludeSelf) append("other ")
                    append(pluralize(filter.description))
                }
                Aggregation.DISTINCT_COUNTER_TYPES -> {
                    append("the number of different kinds of counters among ")
                    if (excludeSelf) append("other ")
                    append(pluralize(filter.description))
                }
                Aggregation.DISTINCT_VALUES -> {
                    append("the number of different ${property?.description ?: "value"} among ")
                    if (excludeSelf) append("other ")
                    append(pluralize(filter.description))
                }
            }
            append(" ")
            when (player) {
                Player.You -> append("you control")
                Player.TargetOpponent -> append("${player.description} controls")
                Player.Each -> append("on the battlefield")
                else -> append("${player.description} controls")
            }
        }
    }

    /**
     * Generic zone aggregation primitive.
     * Queries cards in a player's zone, filters them, optionally maps to a numeric
     * property, and applies an aggregation function.
     *
     * This is the zone-generic equivalent of [AggregateBattlefield], for non-battlefield zones
     * like graveyard, hand, library, and exile.
     *
     * ```kotlin
     * // Greatest mana value among cards in your graveyard (Wick's Patrol)
     * AggregateZone(Player.You, Zone.GRAVEYARD, aggregation = Aggregation.MAX, property = CardNumericProperty.MANA_VALUE)
     *
     * // Count creature cards in your graveyard
     * AggregateZone(Player.You, Zone.GRAVEYARD, GameObjectFilter.Creature)
     * ```
     *
     * Prefer using the fluent DSL via [DynamicAmounts.zone] rather than constructing directly.
     *
     * @param player Whose zone to query
     * @param zone Which zone to query (should not be BATTLEFIELD — use AggregateBattlefield for that)
     * @param filter Filter for which cards to include
     * @param aggregation How to aggregate (COUNT, MAX, MIN, SUM)
     * @param property Which numeric property to aggregate (ignored for COUNT)
     */
    @SerialName("AggregateZone")
    @Serializable
    data class AggregateZone(
        val player: Player,
        val zone: Zone,
        val filter: GameObjectFilter = GameObjectFilter.Companion.Any,
        val aggregation: Aggregation = Aggregation.COUNT,
        val property: CardNumericProperty? = null
    ) : DynamicAmount {
        override fun applyTextReplacement(replacer: TextReplacer): DynamicAmount {
            val newFilter = filter.applyTextReplacement(replacer)
            return if (newFilter !== filter) copy(filter = newFilter) else this
        }
        override val description: String = buildString {
            when (aggregation) {
                Aggregation.COUNT -> {
                    append("the number of ")
                    append(pluralize(filter.description))
                }
                Aggregation.MAX -> {
                    append("the greatest ${property?.description ?: "value"} among ")
                    append(pluralize(filter.description))
                }
                Aggregation.MIN -> {
                    append("the least ${property?.description ?: "value"} among ")
                    append(pluralize(filter.description))
                }
                Aggregation.SUM -> {
                    append("the total ${property?.description ?: "value"} of ")
                    append(pluralize(filter.description))
                }
                Aggregation.DISTINCT_TYPES -> {
                    append("the number of card types among ")
                    append(pluralize(filter.description))
                }
                Aggregation.DISTINCT_PERMANENT_TYPES -> {
                    append("the number of permanent types among ")
                    append(pluralize(filter.description))
                }
                Aggregation.DISTINCT_COLORS -> {
                    append("the number of colors among ")
                    append(pluralize(filter.description))
                }
                Aggregation.DISTINCT_COLOR_PAIRS -> {
                    append("the number of different color pairs among ")
                    append(pluralize(filter.description))
                }
                Aggregation.DISTINCT_NAMES -> {
                    append("the number of differently named ")
                    append(pluralize(filter.description))
                }
                Aggregation.DISTINCT_BASIC_LAND_SUBTYPES -> {
                    append("the number of basic land types among ")
                    append(pluralize(filter.description))
                }
                Aggregation.DISTINCT_COUNTER_TYPES -> {
                    append("the number of different kinds of counters among ")
                    append(pluralize(filter.description))
                }
                Aggregation.DISTINCT_VALUES -> {
                    append("the number of different ${property?.description ?: "value"} among ")
                    append(pluralize(filter.description))
                }
            }
            append(" in ")
            append(player.possessive)
            append(" ")
            append(zoneSimpleName(zone))
        }
    }

    // =========================================================================
    // Entity Property — unified access to any entity's numeric property
    // =========================================================================

    /**
     * Read a numeric property from a referenced entity.
     * This is the unified replacement for TargetPower, TargetManaValue, CountersOnTarget,
     * SourcePower, SacrificedPermanentPower, CountersOnSelf, etc.
     *
     * Examples:
     * ```kotlin
     * EntityProperty(EntityReference.Source, EntityNumericProperty.Power)        // SourcePower
     * EntityProperty(EntityReference.Target(0), EntityNumericProperty.ManaValue) // TargetManaValue
     * EntityProperty(EntityReference.Sacrificed(), EntityNumericProperty.Power)  // SacrificedPermanentPower
     * EntityProperty(EntityReference.Source, EntityNumericProperty.CounterCount(CounterTypeFilter.PlusOnePlusOne)) // CountersOnSelf
     * ```
     */
    @SerialName("EntityProperty")
    @Serializable
    data class EntityProperty(
        val entity: EntityReference,
        val numericProperty: EntityNumericProperty
    ) : DynamicAmount {
        override val description: String = "${entity.description}'s ${numericProperty.description}"
    }

    // =========================================================================
    // Station (CR 702.184)
    // =========================================================================

    /**
     * The number of charge counters a Station ability puts on its permanent: the power of the
     * creature tapped to pay the station cost (CR 702.184a — "equal to the tapped creature's
     * power").
     *
     * This is *not* a plain `EntityProperty(TappedAsCost(0), Power)` because Station carries a
     * rules twist that read does not: CR 702.184c lets a static ability change *which*
     * characteristic is counted. Tapestry Warden ("Each creature you control with toughness
     * greater than its power stations permanents using its toughness rather than its power")
     * substitutes toughness for power. Modelling the station amount as its own node keeps that
     * substitution scoped to station abilities — an unrelated "tap a creature: do X equal to its
     * power" ability that uses `EntityProperty(TappedAsCost(0), Power)` is left untouched.
     *
     * Like the generic tapped-as-cost read, it resolves with last-known information: if the tapped
     * creature has left the battlefield between cost payment and resolution it uses the snapshot
     * captured at cost-pay time (CR 113.7a). Reads the first creature tapped for the cost.
     *
     * Emitted by the `station()` card-DSL helper; do not hand-author.
     */
    @SerialName("StationCharge")
    @Serializable
    data object StationCharge : DynamicAmount {
        override val description: String = "the tapped creature's power"
    }

    // =========================================================================
    // Division
    // =========================================================================

    /**
     * Divide one dynamic amount by another, with configurable rounding.
     * Used for "lose half your life, rounded up" (Divide(LifeTotal, Fixed(2), roundUp=true)).
     */
    @SerialName("Divide")
    @Serializable
    data class Divide(
        val numerator: DynamicAmount,
        val denominator: DynamicAmount,
        val roundUp: Boolean = true
    ) : DynamicAmount {
        override val description: String = "${numerator.description} / ${denominator.description}"
        override fun applyTextReplacement(replacer: TextReplacer): DynamicAmount {
            val newNumerator = numerator.applyTextReplacement(replacer)
            val newDenominator = denominator.applyTextReplacement(replacer)
            return if (newNumerator !== numerator || newDenominator !== denominator)
                copy(numerator = newNumerator, denominator = newDenominator) else this
        }
    }

    /**
     * Reads a per-player turn-tracking counter.
     *
     * Unifies CreaturesDiedThisTurn, NonTokenCreaturesDiedThisTurn, OpponentCreaturesExiledThisTurn,
     * OpponentsWhoLostLifeThisTurn, and DamageDealtToTargetPlayerThisTurn into a single parameterized variant.
     *
     * @param player Which player(s) to read the counter from. Use [Player.ContextPlayer] with a target
     *   index when the player comes from a targeting context (e.g., "damage dealt to target player").
     * @param tracker Which turn-tracking stat to read
     */
    @SerialName("TurnTracking")
    @Serializable
    data class TurnTracking(val player: Player, val tracker: TurnTracker) : DynamicAmount {
        override val description: String = tracker.descriptionFor(player)
    }

    /**
     * Counts the spells a player has cast this turn, optionally filtered and optionally
     * excluding the currently-resolving spell itself.
     *
     * Reads the per-player `spellsCastThisTurnByPlayer` history (a list of [CastSpellRecord]
     * snapshots taken at cast time), so it counts spells regardless of where they are now —
     * the spell that triggered an ability is already recorded and counts unless [excludeSelf].
     *
     * - [filter] narrows to a spell characteristic captured at cast time (type, color, mana
     *   value). Face-down (Morph/Disguise) casts never match a non-empty filter. With
     *   [GameObjectFilter.Any] every cast counts.
     * - [excludeSelf] drops the resolving spell's own record (matched by its stack entity id
     *   against the evaluation context's source), for "the number of *other* spells you've
     *   cast this turn". It only has an effect when the source is itself the resolving spell.
     *
     * ```kotlin
     * // Thunder Salvo: "2 plus the number of other spells you've cast this turn"
     * DynamicAmount.Add(DynamicAmount.Fixed(2),
     *     DynamicAmount.SpellsCastThisTurn(Player.You, excludeSelf = true))
     *
     * // Magebane Lizard: "the number of noncreature spells they've cast this turn"
     * DynamicAmount.SpellsCastThisTurn(Player.TriggeringPlayer, GameObjectFilter.Noncreature)
     * ```
     *
     * @param player Whose cast history to count (summed when the ref resolves to several players)
     * @param filter Spell characteristics to match (default [GameObjectFilter.Any])
     * @param excludeSelf Exclude the resolving spell's own cast record (default false)
     * @param fromZone Restrict to spells cast from this zone, independently of [filter] (default any zone)
     * @param countDistinctCardTypes When true, instead of counting matching spells, count the
     *   *distinct card types* among them (April O'Neil, Hacktivist: "for each card type among
     *   spells you've cast this turn"). An artifact creature spell contributes both Artifact and
     *   Creature. Card types are unioned across every player resolved by [player].
     * @param beforeTriggeringSpell Count only the casts recorded *before* the triggering spell's own
     *   cast record — the "each other spell you've cast **before it** this turn" clause that Storm
     *   (CR 702.40a) and Thousand-Year Storm share. The triggering spell itself, and anything cast in
     *   response to the trigger while it waits on the stack, are both excluded, so the count is the
     *   spell's position in the turn's cast history rather than a resolution-time total. Unlike
     *   [excludeSelf] (which keys off the resolving *source*, so it is inert for a permanent's
     *   triggered ability) this keys off the triggering entity. A player whose history
     *   holds no record for the triggering spell contributes nothing.
     */
    @SerialName("SpellsCastThisTurn")
    @Serializable
    data class SpellsCastThisTurn(
        val player: Player = Player.You,
        val filter: GameObjectFilter = GameObjectFilter.Any,
        val excludeSelf: Boolean = false,
        val fromZone: Zone? = null,
        val countDistinctCardTypes: Boolean = false,
        val beforeTriggeringSpell: Boolean = false
    ) : DynamicAmount {
        override fun applyTextReplacement(replacer: TextReplacer): DynamicAmount {
            val newFilter = filter.applyTextReplacement(replacer)
            return if (newFilter !== filter) copy(filter = newFilter) else this
        }
        override val description: String = buildString {
            append("the number of ")
            if (countDistinctCardTypes) {
                append("card types among ")
                if (excludeSelf || beforeTriggeringSpell) append("other ")
                if (filter != GameObjectFilter.Any) append("${filter.description} ")
                append("spells")
            } else {
                if (excludeSelf || beforeTriggeringSpell) append("other ")
                if (filter == GameObjectFilter.Any) append("spells")
                else append("${filter.description} spells")
            }
            append(" ")
            when (player) {
                Player.You -> append("you've cast")
                else -> append("${player.description} has cast")
            }
            if (fromZone != null) append(" from ${fromZone.name.lowercase()}")
            if (beforeTriggeringSpell) append(" before it")
            append(" this turn")
        }
    }

    /**
     * The total number of spells cast during the immediately preceding turn.
     *
     * This reads the snapshot captured at the turn boundary before the current-turn spell
     * counters are cleared. It deliberately totals the previous active player's shared-turn team,
     * matching old Innistrad werewolf wording such as "if no spells were cast last turn" and
     * "if a player cast two or more spells last turn."
     */
    @SerialName("SpellsCastLastTurn")
    @Serializable
    data object SpellsCastLastTurn : DynamicAmount {
        override val description: String = "the number of spells cast last turn"
    }

    /**
     * Total printed power of the cards exiled to craft the source permanent (CR 702.167c).
     *
     * Reads the source's
     * `com.wingedsheep.engine.state.components.battlefield.CraftedFromExiledComponent` —
     * attached on battlefield entry by
     * [com.wingedsheep.sdk.scripting.effects.ReturnSelfFromExileTransformedEffect] when the
     * Craft activated ability resolves — and sums the printed power of those exiled cards.
     * Used by the Mastercraft Raptor back face of Saheeli's Lattice.
     *
     * Evaluates to zero when the source was not crafted (no component) — the back face has
     * no other way of legitimately existing, but a benign zero keeps the projector total order.
     */
    @SerialName("CraftedMaterialsTotalPower")
    @Serializable
    data object CraftedMaterialsTotalPower : DynamicAmount {
        override val description: String = "the total power of the exiled cards used to craft it"
    }

    /**
     * Total mana value of the cards exiled to craft the source permanent.
     *
     * The mana-value sibling of [CraftedMaterialsTotalPower] — same
     * `CraftedFromExiledComponent` read, summing printed mana value instead of power. Exact-one
     * crafts read it as the single material's mana value (Jadeheart Attendant: "you gain life
     * equal to the mana value of the exiled card used to craft it"). Zero when not crafted.
     */
    @SerialName("CraftedMaterialsTotalManaValue")
    @Serializable
    data object CraftedMaterialsTotalManaValue : DynamicAmount {
        override val description: String = "the total mana value of the exiled cards used to craft it"
    }

    /**
     * Number of colors (0–5) among the cards exiled to craft the source permanent.
     *
     * The color-counting sibling of [CraftedMaterialsTotalPower] — same
     * `CraftedFromExiledComponent` read, counting distinct printed colors across all exiled
     * materials (Sunbird Effigy's P/T CDA and its "add one mana of each of those colors"
     * mana ability). Zero when not crafted or all materials are colorless.
     */
    @SerialName("CraftedMaterialsColorCount")
    @Serializable
    data object CraftedMaterialsColorCount : DynamicAmount {
        override val description: String = "the number of colors among the exiled cards used to craft it"
    }

    /**
     * Number of distinct creatures that crewed (CR 702.122) or saddled (CR 702.171) the source
     * permanent this turn.
     *
     * Source-relative: reads the source's
     * `com.wingedsheep.engine.state.components.battlefield.CrewSaddleContributorsComponent` and
     * returns the size of the recorded set. The set retains contributors that have since left the
     * battlefield, so this counts every creature that crewed/saddled it this turn even if some are
     * no longer present as the ability resolves (per the Luxurious Locomotive ruling). A plain
     * battlefield-filter count ([Count] over [CrewedOrSaddledSourceThisTurn]) cannot express that,
     * which is why this reads the record directly. Evaluates to zero with no source / no component.
     *
     * Used by "for each creature that crewed it this turn" (Luxurious Locomotive).
     */
    @SerialName("CreaturesThatCrewedOrSaddledThisTurn")
    @Serializable
    data object CreaturesThatCrewedOrSaddledThisTurn : DynamicAmount {
        override val description: String = "the number of creatures that crewed or saddled it this turn"
    }

    /**
     * The number of permanents with **any** of [subtypes] that entered the battlefield under
     * [player]'s control this turn (counting even those that have since left or changed type —
     * the entry event is what's tracked). When [excludeTriggeringEntity] is true, the permanent
     * whose entry triggered the ability is not counted, giving "each *other* [subtype]" wording
     * (Geralf, the Fleshwright). Simultaneous entries each see the others (2024-04-12 ruling).
     *
     * [subtypes] is a set with any-of semantics rather than a single subtype so the printed
     * "Mounts and/or Vehicles" wording (Cloudspire Coordinator) counts each qualifying entry
     * exactly **once** — summing two single-subtype amounts would double-count a permanent that is
     * both. A one-element set is the ordinary single-tribe case.
     *
     * Backed by `PermanentsEnteredUnderControlThisTurnComponent`; the triggering entity is read
     * from the resolution context's triggering-entity id.
     */
    @SerialName("SubtypeEnteredUnderControlThisTurn")
    @Serializable
    data class SubtypeEnteredUnderControlThisTurn(
        val player: Player,
        val subtypes: Set<com.wingedsheep.sdk.core.Subtype>,
        val excludeTriggeringEntity: Boolean = false
    ) : DynamicAmount {
        override fun applyTextReplacement(replacer: TextReplacer): DynamicAmount {
            val new = subtypes.map { replacer.replaceSubtype(it) }.toSet()
            return if (new == subtypes) this else copy(subtypes = new)
        }
        override val description: String = buildString {
            append("the number of ")
            if (excludeTriggeringEntity) append("other ")
            append(subtypes.joinToString(" and/or ") { "${it.value}s" })
            append(" that entered the battlefield under ")
            append(player.possessive)
            append(" control this turn")
        }
    }

    /**
     * Number of permanents sacrificed by the current resolving effect ("this way"). Reads the
     * effect context's `sacrificedPermanents` snapshot list, populated when an edict (e.g. "each
     * opponent sacrifices a creature") resolves earlier in the same composite. Used by "Create a
     * Food token for each creature sacrificed this way" (Voracious Fell Beast).
     */
    @SerialName("PermanentsSacrificedThisWay")
    @Serializable
    data object PermanentsSacrificedThisWay : DynamicAmount {
        override val description: String = "the number of permanents sacrificed this way"
    }

    /**
     * Total power of the permanents sacrificed by the current resolving effect ("their total
     * power"). The sibling of [PermanentsSacrificedThisWay] over the same
     * `EffectContext.sacrificedPermanents` snapshot list, summing each snapshot's power instead of
     * counting the entries.
     *
     * The snapshots are last-known information taken as each permanent was sacrificed (Rule
     * 608.2h), which is what "their total power" has to mean — the permanents are already in the
     * graveyard by the time a later sibling effect reads them. Kylox, Visionary Inventor's ruling
     * of 2024-02-02 says so explicitly: "Use the power of the sacrificed creatures as they last
     * existed on the battlefield to determine the value of X."
     *
     * Negative power counts as written; the sum is floored at 0 by the effects that consume it
     * (you can't exile a negative number of cards), not here.
     */
    @SerialName("TotalPowerSacrificedThisWay")
    @Serializable
    data object TotalPowerSacrificedThisWay : DynamicAmount {
        override val description: String = "their total power"
    }

    /**
     * The size of the largest creature-type tribe among the creatures [player] controls — i.e.
     * "the greatest number of creatures you control that have a creature type in common."
     *
     * For every creature type present, count how many of the player's creatures have that type,
     * then take the maximum. A creature with several creature types counts toward each of its
     * tribes (a Bird Soldier contributes to both the Bird tally and the Soldier tally), and a
     * Changeling counts toward every tribe (it has all creature types). Evaluated against
     * projected state so type-changing effects are honored. Zero when the player controls no
     * creatures with a creature type.
     *
     * Used by White Lotus Tile — "{T}: Add X mana of any one color, where X is the greatest number
     * of creatures you control that have a creature type in common."
     */
    @SerialName("LargestSharedCreatureTypeCount")
    @Serializable
    data class LargestSharedCreatureTypeCount(
        val player: Player = Player.You
    ) : DynamicAmount {
        override val description: String =
            "the greatest number of creatures ${if (player == Player.You) "you control" else "${player.description} controls"} that have a creature type in common"
    }

}
