package com.wingedsheep.sdk.scripting

import com.wingedsheep.sdk.core.BendType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.events.*
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.text.TextReplaceable
import com.wingedsheep.sdk.scripting.text.TextReplacer
import com.wingedsheep.sdk.scripting.util.numberToWord
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Which side of a control change an [EventPattern.ControlChangeEvent] ability watches, relative to
 * the ability's controller.
 */
@Serializable
enum class ControlChangeDirection {
    /** "When you **gain** control …": the ability's controller is the *new* controller. */
    GAINED,

    /** "When you **lose** control …": the ability's controller was the *old* controller. */
    LOST
}

/**
 * Which reveal outcome an [EventPattern.ExploredEvent] trigger requires (CR 701.44a).
 *
 * - [ANY] — fires on every explore, regardless of what was revealed (or if the library was empty,
 *   CR 701.44b: the permanent still explored). Backs "whenever a creature you control explores".
 * - [LAND] — fires only when the revealed card was a land (went to hand). Backs "explores a land card".
 * - [NONLAND] — fires only when the revealed card was a nonland (the +1/+1-counter branch). Backs
 *   "explores a nonland card".
 */
@Serializable
enum class ExploreReveal { ANY, LAND, NONLAND }

/**
 * Represents a game event type used by both replacement effects and triggered abilities.
 *
 * This is compositional - events are specified by combining an event type
 * with filters, rather than having pre-baked scenarios.
 *
 * Example:
 * ```kotlin
 * // "Combat damage from red sources to creatures you control"
 * EventPattern.DamageEvent(
 *     recipient = RecipientFilter.CreatureYouControl,
 *     source = SourceFilter.HasColor(Color.RED),
 *     damageType = DamageType.Combat
 * )
 * ```
 *
 * Supporting filter types are organized in the events/ subdirectory:
 * - EventFilters.kt - RecipientFilter, SourceFilter, DamageType,
 *                     CounterTypeFilter, ControllerFilter, Player
 * - Zone.kt - Zone enumeration
 */
@Serializable
sealed interface EventPattern : TextReplaceable<EventPattern> {
    val description: String

    /** Matches when any constituent event pattern matches. */
    @SerialName("AnyOfEvents")
    @Serializable
    data class AnyOf(val events: List<EventPattern>) : EventPattern {
        init {
            require(events.size >= 2) { "AnyOf requires at least two event patterns" }
        }

        override val description: String = events.joinToString(" or ") { it.description }

        override fun applyTextReplacement(replacer: TextReplacer): EventPattern {
            val replaced = events.map { it.applyTextReplacement(replacer) }
            return if (replaced == events) this else copy(events = replaced)
        }
    }

    // =========================================================================
    // Damage Events (Replacement Effect)
    // =========================================================================

    /**
     * When damage would be dealt (used by replacement effects).
     *
     * Examples:
     * - "damage would be dealt to you" → DamageEvent(recipient = RecipientFilter.You)
     * - "combat damage would be dealt" → DamageEvent(damageType = DamageType.Combat)
     * - "damage from red sources" → DamageEvent(source = SourceFilter.HasColor(RED))
     */
    @SerialName("DamageEvent")
    @Serializable
    data class DamageEvent(
        val recipient: RecipientFilter = RecipientFilter.Any,
        val source: SourceFilter = SourceFilter.Any,
        val damageType: DamageType = DamageType.Any,
        val amount: AmountFilter = AmountFilter.Any
    ) : EventPattern {
        override val description: String = buildString {
            if (amount != AmountFilter.Any) {
                append(amount.description)
                append(" ")
            }
            if (damageType != DamageType.Any) {
                append(damageType.description)
                append(" ")
            }
            append("damage would be dealt to ")
            append(recipient.description)
            if (source != SourceFilter.Any) {
                append(" from ")
                append(source.description)
            }
        }
    }

    // =========================================================================
    // Zone Change Events
    // =========================================================================

    /**
     * When an object changes zones.
     *
     * Used by both replacement effects ("would enter/die") and triggers ("enters/dies").
     * When [to] is null, matches any destination zone (e.g., "leaves the battlefield").
     *
     * Examples:
     * - "a card would be put into a graveyard" → ZoneChangeEvent(to = Zone.GRAVEYARD)
     * - "a creature would enter the battlefield" → ZoneChangeEvent(filter = GameObjectFilter.Creature, to = Zone.BATTLEFIELD)
     * - "a creature would die" → ZoneChangeEvent(filter = GameObjectFilter.Creature, from = Zone.BATTLEFIELD, to = Zone.GRAVEYARD)
     * - "leaves the battlefield" → ZoneChangeEvent(from = Zone.BATTLEFIELD, to = null)
     */
    @SerialName("ZoneChangeEvent")
    @Serializable
    data class ZoneChangeEvent(
        val filter: GameObjectFilter = GameObjectFilter.Any,
        val from: Zone? = null,
        val to: Zone? = null,
        val excludeTo: Zone? = null,
        /**
         * When true, the trigger fires only if the battlefield exit was **not** a sacrifice
         * (CR 701.21) — Urza's Miter: "...is put into a graveyard from the battlefield, if it
         * wasn't sacrificed...". Only meaningful for `from = BATTLEFIELD` patterns; the matcher
         * reads the triggering event's sacrifice flag.
         */
        val excludeSacrifice: Boolean = false,
        /**
         * When true, the trigger fires only if this object left the battlefield because it was
         * exiled as a material to pay a Craft cost (CR 702.167) — "When this creature is exiled
         * from the battlefield while you're activating a craft ability" (Market Gnome). Only
         * meaningful for `from = BATTLEFIELD, to = EXILE` patterns; the matcher reads the
         * triggering event's craft-material flag, which the Craft cost payment stamps onto the
         * exile of each chosen material (and only those — an unrelated exile leaves it `false`).
         */
        val requireCraftMaterial: Boolean = false
    ) : EventPattern {
        override val description: String = buildString {
            append(describeObjectForEvent(filter))
            if (excludeTo != null && from != null) {
                append(" would leave ${from.displayName} without ${if (excludeTo == Zone.GRAVEYARD) "dying" else "going to ${excludeTo.displayName}"}")
            } else if (to != null) {
                append(" would ")
                when (to) {
                    Zone.GRAVEYARD -> {
                        if (from == Zone.BATTLEFIELD) append("die")
                        else append("be put into a graveyard")
                    }
                    Zone.BATTLEFIELD -> append("enter the battlefield")
                    Zone.EXILE -> append("be exiled")
                    Zone.HAND -> append("be returned to a hand")
                    Zone.LIBRARY -> append("be put into a library")
                    Zone.STACK -> append("be put on the stack")
                    Zone.COMMAND -> append("be put into the command zone")
                    // No card is ever moved *to* the sideboard mid-game (it is "outside the game",
                    // CR 400.11); this branch only exists for `when` exhaustiveness.
                    Zone.SIDEBOARD -> append("be put into a sideboard")
                }
                if (from != null && to != Zone.GRAVEYARD) {
                    append(" from ${from.displayName}")
                }
            } else if (from != null) {
                append(" would leave ${from.displayName}")
            }
            if (requireCraftMaterial) append(" while you're activating a craft ability")
        }

        override fun applyTextReplacement(replacer: TextReplacer): EventPattern {
            val newFilter = filter.applyTextReplacement(replacer)
            return if (newFilter !== filter) copy(filter = newFilter) else this
        }
    }

    // =========================================================================
    // Counter Events
    // =========================================================================

    /**
     * When counters would be placed on a permanent.
     *
     * Examples:
     * - "counters would be placed" → CounterPlacementEvent()
     * - "+1/+1 counters on creatures you control" → CounterPlacementEvent(counterType = CounterTypeFilter.PlusOnePlusOne, recipient = RecipientFilter.CreatureYouControl)
     */
    @SerialName("CounterPlacementEvent")
    @Serializable
    data class CounterPlacementEvent(
        val counterType: CounterTypeFilter = CounterTypeFilter.Any,
        val recipient: RecipientFilter = RecipientFilter.Any
    ) : EventPattern {
        override val description: String = buildString {
            if (counterType != CounterTypeFilter.Any) {
                append(counterType.description)
                append(" ")
            }
            append("counters would be placed on ")
            append(recipient.description)
        }
    }

    // =========================================================================
    // Token Events
    // =========================================================================

    /**
     * When tokens would be created.
     *
     * Examples:
     * - "tokens under your control" → TokenCreationEvent(controller = ControllerFilter.You)
     * - "any tokens" → TokenCreationEvent(controller = ControllerFilter.Any)
     */
    @SerialName("TokenCreationEvent")
    @Serializable
    data class TokenCreationEvent(
        val controller: ControllerFilter = ControllerFilter.You,
        val tokenFilter: GameObjectFilter? = null
    ) : EventPattern {
        override val description: String = buildString {
            append("one or more ")
            if (tokenFilter != null) {
                append(tokenFilter.description)
                append(" ")
            }
            append("tokens would be created")
            if (controller != ControllerFilter.Any) {
                append(" ")
                append(controller.description)
            }
        }

        override fun applyTextReplacement(replacer: TextReplacer): EventPattern {
            val f = tokenFilter ?: return this
            val newFilter = f.applyTextReplacement(replacer)
            return if (newFilter !== f) copy(tokenFilter = newFilter) else this
        }
    }

    // =========================================================================
    // Draw Events
    // =========================================================================

    /**
     * When a player draws a card. Fires once per individual card drawn (CR 121.2),
     * even when several cards are drawn by one effect.
     *
     * Examples:
     * - "you would draw a card" → DrawEvent(player = Player.You)
     * - "an opponent would draw" → DrawEvent(player = Player.EachOpponent)
     *
     * When [exceptFirstInDrawStep] is set, the first card the drawing player draws in
     * each of their own draw steps (CR 504.1's turn-based draw, normally) does **not**
     * fire the trigger — every other draw they make does. This is the Orcish Bowmasters
     * clause "except the first card they draw in each of their draw steps".
     */
    @SerialName("DrawEvent")
    @Serializable
    data class DrawEvent(
        val player: Player = Player.You,
        val exceptFirstInDrawStep: Boolean = false,
    ) : EventPattern {
        override val description: String = buildString {
            append(player.description)
            append(" would draw a card")
            if (exceptFirstInDrawStep) append(" (except the first each draw step)")
        }
    }

    /**
     * When a player would draw one or more cards. Fires at the beginning of the
     * draw card cycle announcing the total cards drawn.
     *
     * The [amount] parameter is the threshold that triggers the event — "if an opponent
     * would draw N or more cards".
     */
    @SerialName("DrawCardsEvent")
    @Serializable
    data class DrawCardsEvent(
        val player: Player = Player.You,
        val amount: Int = 1
    ) : EventPattern {
        override val description: String = buildString {
            append(player.description)
            append(" would draw ")
            append(numberToWord(amount))
            append(" or more cards")
        }
    }

    /**
     * When a player would mill one or more cards (CR 701.13). Used by
     * [com.wingedsheep.sdk.scripting.ModifyMillAmount] to adjust the count at the mill
     * announcement (e.g. The Water Crystal: "If an opponent would mill one or more cards,
     * they mill that many cards plus four instead").
     */
    @SerialName("MillEvent")
    @Serializable
    data class MillEvent(
        val player: Player = Player.You
    ) : EventPattern {
        override val description: String = "${player.description} would mill one or more cards"
    }

    /**
     * Fires on a `CardsDrawnEvent` when the drawing player's per-turn draw count
     * crosses the specified threshold (CR 121.2 — each card drawn is an individual
     * draw, so a single multi-card draw fires at most once when the Nth card lands
     * inside that batch). Per-player count is tracked by
     * `CardsDrawnThisTurnComponent` and reset by `TurnManager` at the start of each turn.
     *
     * Used by cards like Knights of Dol Amroth: "Whenever you draw your second card
     * each turn, …".
     *
     * @param nthCard The card number that triggers this (e.g., 2 for "second card")
     * @param player Which player's draw count to track
     */
    @SerialName("NthCardDrawnEvent")
    @Serializable
    data class NthCardDrawnEvent(
        val nthCard: Int,
        val player: Player = Player.You
    ) : EventPattern {
        override val description: String = buildString {
            append(player.description)
            append(" draws ")
            append(when (player) {
                Player.You -> "your "
                else -> "their "
            })
            append(when (nthCard) {
                2 -> "second"
                3 -> "third"
                else -> "${nthCard}th"
            })
            append(" card each turn")
        }
    }

    // =========================================================================
    // Life Events
    // =========================================================================

    /**
     * When a player would gain life.
     *
     * [firstTimeEachTurn] restricts the match to the first life-gaining event for that player this
     * turn — "whenever you gain life for the first time each turn" (Leech Collector).
     */
    @SerialName("LifeGainEvent")
    @Serializable
    data class LifeGainEvent(
        val player: Player = Player.You,
        val firstTimeEachTurn: Boolean = false
    ) : EventPattern {
        override val description: String = "${player.description} would gain life" +
            if (firstTimeEachTurn) " for the first time each turn" else ""
    }

    /**
     * When a player would lose life.
     */
    @SerialName("LifeLossEvent")
    @Serializable
    data class LifeLossEvent(
        val player: Player = Player.You
    ) : EventPattern {
        override val description: String = "${player.description} would lose life"
    }

    /**
     * When a player would **pay** life — life spent to satisfy a cost (CR 118.8), as opposed to
     * life lost to damage or to a "loses N life" effect. Distinct from [LifeLossEvent]: every life
     * payment reduces a life total, but only a payment is a cost the player chose to pay, and only
     * payments are replaceable by effects worded "if you would pay life" (Ashiok, Wicked
     * Manipulator). Damage and unpayable costs are never this event.
     *
     * Replacement-effect only — a payment surfaces to triggered abilities as the `LifeChangedEvent`
     * it produces, so this pattern never matches as a trigger.
     */
    @SerialName("LifePaymentEvent")
    @Serializable
    data class LifePaymentEvent(
        val player: Player = Player.You
    ) : EventPattern {
        override val description: String = "${player.description} would pay life"
    }

    /**
     * When a player would gain or lose life.
     * Used for cards like Moonstone Harbinger: "Whenever you gain or lose life during your turn".
     */
    @SerialName("LifeGainOrLossEvent")
    @Serializable
    data class LifeGainOrLossEvent(
        val player: Player = Player.You
    ) : EventPattern {
        override val description: String = "${player.description} would gain or lose life"
    }

    /**
     * When a player loses the game (CR 104.3 — life 0, drawing from an empty library, poison,
     * commander damage, or a "loses the game" effect). Fires from the engine's `PlayerLostEvent`,
     * which is emitted by the state-based-action / turn-based checks when a player loses.
     *
     * Use [player] to filter which player's loss is relevant (default [Player.Each] — any player).
     * For "when the chosen player loses the game" (Shinryu, Transcendent Rival) pair
     * [Player.Each] with a `triggerRestriction` comparing the triggering player to the source's
     * chosen opponent, so `Player.TriggeringPlayer` inside the effect resolves to the loser.
     */
    @SerialName("PlayerLostGameEvent")
    @Serializable
    data class PlayerLostGameEvent(
        val player: Player = Player.Each
    ) : EventPattern {
        override val description: String = "${player.description} loses the game"
    }

    /**
     * Whenever the Ring tempts a player (CR 701.54d). Fires after the temptee completes
     * the "the Ring tempts you" action, even if some or all of it was impossible.
     * Used by cards with "Whenever the Ring tempts you, ...".
     */
    @SerialName("RingTemptedEvent")
    @Serializable
    data class RingTemptedEvent(
        val player: Player = Player.You,
        /**
         * When true, only match temptations in which the player actually *chose* a creature as
         * their Ring-bearer (the event's `bearerId` is non-null). Models "Whenever you choose a
         * creature as your Ring-bearer" (Call of the Ring) as distinct from the plain
         * "Whenever the Ring tempts you" (which fires even when no creature could be chosen).
         */
        val requireBearerChosen: Boolean = false
    ) : EventPattern {
        override val description: String =
            if (requireBearerChosen) "${player.description} choose a creature as your Ring-bearer"
            else "the Ring tempts ${player.description}"
    }

    /**
     * Whenever a player scries (CR 701.22). Fires once per scry, after every card chosen
     * for the bottom/top has been moved. Carries the number of cards actually looked at,
     * which equals the scry N parameter unless the library had fewer cards available.
     * Read this count via [com.wingedsheep.sdk.scripting.values.ContextPropertyKey.TRIGGER_SCRY_COUNT]
     * for "for each card looked at" payoffs (e.g. Celeborn the Wise, Elrond Master of Healing).
     */
    @SerialName("ScriedEvent")
    @Serializable
    data class ScriedEvent(
        val player: Player = Player.You
    ) : EventPattern {
        override val description: String = "${player.description} scries"
    }

    /**
     * Whenever a player surveils (CR 701.25). Fires once per surveil, after the kept/graveyard
     * moves have all resolved. Carries the number of cards actually looked at (equals the surveil
     * N parameter unless the library had fewer cards). Read this count via
     * [com.wingedsheep.sdk.scripting.values.ContextPropertyKey.TRIGGER_SCRY_COUNT] ("the number of
     * cards looked at"). A literal "surveil 0" produces no event (CR 701.25c).
     */
    @SerialName("SurveiledEvent")
    @Serializable
    data class SurveiledEvent(
        val player: Player = Player.You
    ) : EventPattern {
        override val description: String = "${player.description} surveils"
    }

    /**
     * Whenever a player scries **or** surveils (CR 701.22 / 701.25) — the combined look-at-top
     * trigger used by "Whenever you scry or surveil, …" (Matoya, Archon Elder). Matches either a
     * scry or a surveil event from [player]; the cards-looked-at count is exposed the same way as
     * the individual triggers (TRIGGER_SCRY_COUNT).
     */
    @SerialName("ScriedOrSurveiledEvent")
    @Serializable
    data class ScriedOrSurveiledEvent(
        val player: Player = Player.You
    ) : EventPattern {
        override val description: String = "${player.description} scries or surveils"
    }

    /**
     * Whenever [player] discovers (CR 701.57). Fires once per discover, after the whole discover
     * process is complete — including the "cast for free or put into hand" decision (CR 701.57b: a
     * player has "discovered" only after the process finishes, "even if some or all of those
     * actions were impossible"). Carries the discover value N (the mana-value threshold used), read
     * via [com.wingedsheep.sdk.scripting.values.ContextPropertyKey.TRIGGER_DISCOVER_VALUE] so
     * "discover again for the same value" payoffs (Curator of Sun's Creation) can reuse it. The
     * event fires even when the library was empty or held no matching card.
     */
    @SerialName("DiscoveredEvent")
    @Serializable
    data class DiscoveredEvent(
        val player: Player = Player.You
    ) : EventPattern {
        override val description: String = "${player.description} discovers"
    }

    /**
     * Whenever [player] collects evidence (CR 701.59). Fires once per collection, after the chosen
     * cards have been exiled — and only then: a declined collection, and one that was impossible
     * under CR 701.59b, never fire it, so "whenever you collect evidence" payoffs (Surveillance
     * Monitor's Thopter, Evidence Examiner's Clue) can trust that evidence genuinely changed hands.
     *
     * Fires for **every** context the mechanic appears in — an activated-ability cost (Cryptex), a
     * cast-time additional cost (Extract a Confession), a ward cost (Axebane Ferox), and the
     * resolution-time [com.wingedsheep.sdk.scripting.effects.CollectEvidenceEffect] (Sample
     * Collector) — because all four share one payment implementation. That matters for the printed
     * cards: Surveillance Monitor's own ETB collection triggers its own payoff.
     *
     * Note that this is a *different* fact from the CR 701.59c linkage: this pattern observes any
     * collection by [player], while `Conditions.WasEvidenceCollected` asks only whether *this
     * object's own* optional cast cost was declared.
     */
    @SerialName("EvidenceCollectedEvent")
    @Serializable
    data class EvidenceCollectedEvent(
        val player: Player = Player.You
    ) : EventPattern {
        override val description: String = "${player.description} collects evidence"
    }

    /**
     * Whenever [player] solves a Case (CR 719.3a) — fires as that Case's "To solve" trigger
     * resolves and the designation is stamped, which is exactly what the printed ruling for Case
     * File Auditor says ("triggers whenever a 'to solve' ability you control resolves").
     *
     * The solving player is the Case's controller, carried on the engine event rather than looked
     * up afterwards: a Case whose Solved ability sacrifices it is already gone by the time this
     * payoff is matched.
     *
     * The designation is sticky and one-way (CR 719.3b), so this can only fire once per Case
     * object — a Case that leaves and returns is a new object and can be solved again.
     */
    @SerialName("CaseSolvedEvent")
    @Serializable
    data class CaseSolvedEvent(
        val player: Player = Player.You
    ) : EventPattern {
        override val description: String = "${player.description} solves a Case"
    }

    /**
     * Whenever a permanent matching [filter] explores (CR 701.44), optionally gated by whether the
     * revealed card was a land ([revealedType]). The exploring permanent is the event subject, so
     * "a creature you control explores" is `filter = GameObjectFilter.Creature.youControl()` with a
     * [com.wingedsheep.sdk.scripting.TriggerBinding.ANY] binding (the observer watches every
     * matching permanent, resolving "you" to the observer's controller). Fires once per explore,
     * including the empty-library case (CR 701.44b — the permanent still explored), where
     * [revealedType] `LAND`/`NONLAND` do not match but `ANY` does.
     */
    @SerialName("ExploredEvent")
    @Serializable
    data class ExploredEvent(
        val filter: GameObjectFilter? = null,
        val revealedType: ExploreReveal = ExploreReveal.ANY
    ) : EventPattern {
        override val description: String = when (revealedType) {
            ExploreReveal.ANY -> "a permanent explores"
            ExploreReveal.LAND -> "a permanent explores a land card"
            ExploreReveal.NONLAND -> "a permanent explores a nonland card"
        }

        override fun applyTextReplacement(replacer: TextReplacer): EventPattern {
            val newFilter = filter?.applyTextReplacement(replacer)
            return if (newFilter !== filter) copy(filter = newFilter) else this
        }
    }

    /**
     * Whenever a permanent matching [filter] connives (CR 701.50). The conniving permanent is the
     * event subject, so "a creature you control connives" is
     * `filter = GameObjectFilter.Creature.youControl()`, resolving "you" to the observing ability's
     * controller. Fires once per connive, after the draw/discard/counter process completes — CR
     * 701.50f, the permanent "connives" even if some or all of those actions were impossible (empty
     * hand, empty library).
     *
     * Only a *real* connive fires this. Connive-shaped looting that the printed card never calls
     * connive (Teo, Spirited Glider — "draw a card, then discard a card…", built from
     * [com.wingedsheep.sdk.dsl.Effects.ConniveTargeting]) is not a connive and emits nothing.
     *
     * Doubles as the `appliesTo` filter for [com.wingedsheep.sdk.scripting.ModifyKeywordAction]
     * ("if a creature you control would connive, instead …") — see that type.
     */
    @SerialName("ConnivedEvent")
    @Serializable
    data class ConnivedEvent(
        val filter: GameObjectFilter? = null
    ) : EventPattern {
        override val description: String = "a permanent connives"

        override fun applyTextReplacement(replacer: TextReplacer): EventPattern {
            val newFilter = filter?.applyTextReplacement(replacer)
            return if (newFilter !== filter) copy(filter = newFilter) else this
        }
    }

    /**
     * Whenever [player] performs one of the four elemental bending keyword actions in [types]
     * (CR 701.65b Airbend / 701.66b Earthbend / 701.67c Waterbend / 702.189b Firebending). Fires
     * once per bend. The default [types] set is all four, backing "Whenever you waterbend,
     * earthbend, firebend, or airbend, …" (Avatar Aang); pass a narrower set for a single-element
     * variant ("whenever you earthbend, …"). See [com.wingedsheep.sdk.dsl.Triggers.YouBend].
     */
    @SerialName("BendPerformedEvent")
    @Serializable
    data class BendPerformedEvent(
        val player: Player = Player.You,
        val types: Set<BendType> = BendType.ALL
    ) : EventPattern {
        override val description: String = "${player.description} " + when {
            types == BendType.ALL -> "waterbends, earthbends, firebends, or airbends"
            types.size == 1 -> "${types.first().oracleVerb}s"
            else -> types.joinToString(", ") { "${it.oracleVerb}s" }
        }
    }

    /**
     * Whenever a player manifests dread (CR 701.60). Fires once per manifest-dread, after the
     * chosen card has been manifested and the other card(s) put into the graveyard. Per CR
     * 701.60b the trigger fires "even if some or all of those actions were impossible" (an empty
     * or one-card library), exactly like scry/surveil.
     *
     * The card(s) put into the graveyard this way are exposed to the payoff as the pipeline
     * collection [com.wingedsheep.sdk.scripting.effects.IterationSpace.TRIGGER_CAPTURED_COLLECTION],
     * so a payoff that references "a card you put into your graveyard this way" (Paranormal
     * Analyst) can move it out of the graveyard. The collection is empty when the library held
     * fewer than two cards.
     */
    @SerialName("ManifestedDreadEvent")
    @Serializable
    data class ManifestedDreadEvent(
        val player: Player = Player.You
    ) : EventPattern {
        override val description: String = "${player.description} manifest dread"
    }

    // =========================================================================
    // Extra Turn Events
    // =========================================================================

    /**
     * When a player would take an extra turn.
     * Used by PreventExtraTurns replacement effect (Ugin's Nexus).
     */
    @SerialName("ExtraTurnEvent")
    @Serializable
    data class ExtraTurnEvent(
        val player: Player = Player.Each
    ) : EventPattern {
        override val description: String = "${player.description} would take an extra turn"
    }

    // =========================================================================
    // Discard Events
    // =========================================================================

    /**
     * When a player would discard a card.
     */
    @SerialName("DiscardEvent")
    @Serializable
    data class DiscardEvent(
        val player: Player = Player.You,
        val cardFilter: GameObjectFilter? = null,
        /**
         * Batch ("one or more") semantics — CR 603.2c: an ability triggers only once each time
         * its trigger event occurs. When true, discarding several matching cards in one discard
         * event fires the trigger once per event instead of once per card — "Whenever you
         * discard one or more cards" (Inti, Seneschal of the Sun) vs the per-card "Whenever
         * you discard a card" (Cool but Rude). Sequential discards ("discard a card, then
         * discard a card") are separate events and still fire separately.
         */
        val batch: Boolean = false
    ) : EventPattern {
        override val description: String = buildString {
            append(player.description)
            append(" would discard ")
            if (batch) append("one or more ")
            if (cardFilter != null) {
                append(cardFilter.description)
            } else {
                append(if (batch) "cards" else "a card")
            }
        }

        override fun applyTextReplacement(replacer: TextReplacer): EventPattern {
            val f = cardFilter ?: return this
            val newFilter = f.applyTextReplacement(replacer)
            return if (newFilter !== f) copy(cardFilter = newFilter) else this
        }
    }

    // =========================================================================
    // Search Events
    // =========================================================================

    /**
     * Whenever [player] searches their library (CR 701.23). Fires once per search, after the found
     * cards have moved and the library has shuffled. Searching is the act of looking through the
     * zone (CR 701.23a) and finding a card is not required (CR 701.23b), so the trigger fires even
     * when no card was found. Scope it with [Player.EachOpponent] for "Whenever an opponent searches
     * their library" (Wan Shi Tong, Librarian) or [Player.You] for "Whenever you search your library".
     *
     * Emitted automatically by the search primitives ([com.wingedsheep.sdk.dsl.LibraryPatterns
     * .searchLibrary] / `searchMultipleZones` / `eachPlayerSearchesLibrary`) — every tutor, fetch,
     * and basic-land search fires it; no card has to opt in.
     */
    @SerialName("SearchLibraryEvent")
    @Serializable
    data class SearchLibraryEvent(
        val player: Player = Player.You
    ) : EventPattern {
        override val description: String = "${player.description} searches their library"
    }

    /**
     * Whenever a spell or ability causes [player] to shuffle their library (CR 701.24). The
     * search twin of [SearchLibraryEvent], and the shape Psychogenic Probe keys on.
     *
     * Deliberately restricted to spell- and ability-caused shuffles, matching the only printed
     * wording: the game rules also shuffle each library while setting up (CR 103.2) and when a
     * player mulligans (CR 103.5), and neither may fire an ability. The engine tags those two
     * emission sites, so nothing here has to know about them.
     *
     * Every shuffle primitive emits it — `Effects.ShuffleLibrary`, the shuffle leg of every
     * search pipeline, `ZonePlacement.Shuffled` moves, and the shuffle-into-library replacement
     * effects (Darksteel Colossus). Three consequences of CR 701.24 come free from being one
     * event per shuffle: a search-then-shuffle still fires it even though the found cards are
     * held out of the randomization (701.24b), a library holding zero or one cards still fires
     * it (701.24e), and two effects shuffling one library simultaneously fire it twice (701.24f).
     */
    @SerialName("ShuffleLibraryEvent")
    @Serializable
    data class ShuffleLibraryEvent(
        val player: Player = Player.Any
    ) : EventPattern {
        override val description: String =
            "a spell or ability causes ${player.description} to shuffle their library"
    }

    // =========================================================================
    // Trigger-Only Events (below here — used only as trigger filters)
    // =========================================================================

    // ---- Combat Triggers ----

    /**
     * When a creature attacks.
     * Binding SELF = "when this creature attacks", ANY = "whenever a creature attacks".
     * Optional [filter] restricts which attacking creatures trigger this (e.g., nontoken creatures).
     * When filter is null and binding is ANY, triggers for creatures you control (default).
     * When filter is set, triggers for any creature matching the filter regardless of controller
     * (same pattern as [BecomesBlockedEvent]).
     */
    @SerialName("AttackEvent")
    @Serializable
    data class AttackEvent(
        val filter: GameObjectFilter? = null,
        /**
         * Extensible set of attack-time facts the trigger requires (conjunctive).
         * Each predicate is an [AttackPredicate] sealed-case — adding a new
         * attack-time mechanic (Battalion-style attacker-count gates,
         * with-another-matching-creature, …) is one new sealed-case + one
         * matcher branch, not a new field here.
         *
         * Current cases: [AttackPredicate.Alone],
         * [AttackPredicate.AttackerCountAtLeast],
         * [AttackPredicate.FirstTimeEachTurn],
         * [AttackPredicate.DefenderIsPlayer].
         */
        val requires: Set<AttackPredicate> = emptySet(),
    ) : EventPattern {
        override val description: String = buildString {
            if (filter != null) {
                append("a ${filter.description} attacks")
            } else {
                append("a creature attacks")
            }
            requires.forEach { append(" ").append(it.description) }
        }

        override fun applyTextReplacement(replacer: TextReplacer): EventPattern {
            val f = filter ?: return this
            val newFilter = f.applyTextReplacement(replacer)
            return if (newFilter !== f) copy(filter = newFilter) else this
        }
    }

    /**
     * When you attack with at least [minAttackers] creatures.
     * Optional [attackerFilter] restricts which attackers count (e.g., Lizards only).
     * When set, the trigger fires only if at least [minAttackers] of the declared
     * attackers match the filter.
     */
    @SerialName("YouAttackEvent")
    @Serializable
    data class YouAttackEvent(
        val minAttackers: Int = 1,
        val attackerFilter: GameObjectFilter? = null
    ) : EventPattern {
        override val description: String = buildString {
            append("you attack with ")
            if (minAttackers <= 1) {
                append("one or more ")
            } else {
                append("$minAttackers or more ")
            }
            if (attackerFilter != null) {
                append(attackerFilter.description)
            } else {
                append("creatures")
            }
        }
    }

    /**
     * When one or more creatures attack the trigger's controller (the player).
     * Defender side of [YouAttackEvent]: fires once per [com.wingedsheep.engine.core.AttackersDeclaredEvent]
     * when at least [minAttackers] declared attackers have the trigger's controller as their defender.
     *
     * Per CR 509.1b and the Orim's Prayer ruling, creatures attacking a planeswalker controlled
     * by the trigger's controller do **not** count toward this trigger — only attackers
     * declared against the player themself. That is the default, and it is what "creatures attack
     * you" prints.
     *
     * [includePlaneswalkersYouControl] opts into the wider reading for the cards that spell it
     * out — Tomik, Wielder of Law: "two or more of those creatures are attacking you **and/or
     * planeswalkers you control**". It widens only which attackers *count*; the trigger is still
     * the defending player's. Battles are never included: "planeswalkers you control" is literal,
     * and a battle you protect is controlled by its caster, not by you.
     */
    @SerialName("CreaturesAttackYouEvent")
    @Serializable
    data class CreaturesAttackYouEvent(
        val minAttackers: Int = 1,
        val includePlaneswalkersYouControl: Boolean = false
    ) : EventPattern {
        override val description: String = buildString {
            append(if (minAttackers <= 1) "one or more creatures attack" else "$minAttackers or more creatures attack")
            append(if (includePlaneswalkersYouControl) " you and/or planeswalkers you control" else " you")
        }
    }

    /**
     * Whenever you play a land (CR 305.1 — the special land-play action). [fromZoneOtherThan], when
     * set, restricts to lands *played* from a zone other than that one: "whenever you play a land …
     * from anywhere other than your hand" (Shadow of the Goblin) is `fromZoneOtherThan = Zone.HAND`.
     *
     * Matches the engine's `LandPlayedEvent`, which is emitted only for the land-play action, never
     * for a land an effect *puts* onto the battlefield — so this does not over-trigger on fetches,
     * reanimation, or ramp (the gap that a plain `ZoneChangeEvent(→ BATTLEFIELD)` pattern can't close).
     */
    @SerialName("LandPlayedEvent")
    @Serializable
    data class LandPlayedEvent(
        val fromZoneOtherThan: Zone? = null
    ) : EventPattern {
        override val description: String = buildString {
            append("you play a land")
            if (fromZoneOtherThan != null) {
                append(" from anywhere other than your ${fromZoneOtherThan.name.lowercase()}")
            }
        }
    }

    /**
     * When one or more creatures attack a player who is an opponent of the trigger's controller.
     * The "your opponents are attacked" counterpart of [CreaturesAttackYouEvent]: fires once per
     * [com.wingedsheep.engine.core.AttackersDeclaredEvent] when at least [minAttackers] declared
     * attackers have one of the controller's opponents as their defender. As with the "you" side,
     * only attackers declared against an opponent *player* count (not against a planeswalker the
     * opponent controls). Party Dude level 3.
     */
    @SerialName("CreaturesAttackYourOpponentEvent")
    @Serializable
    data class CreaturesAttackYourOpponentEvent(
        val minAttackers: Int = 1
    ) : EventPattern {
        override val description: String = if (minAttackers <= 1) {
            "one or more of your opponents are attacked"
        } else {
            "$minAttackers or more creatures attack your opponents"
        }
    }

    /**
     * When a creature blocks.
     * Binding SELF = "when this creature blocks".
     * Binding ANY + filter = "whenever a [filter] blocks" — fires once per matching blocker.
     *
     * [attackerFilter] restricts the blocked attacker (e.g. "blocks a creature with flying").
     * When set with SELF binding, fires once per blocked attacker matching the filter and
     * sets TriggerContext.triggeringEntityId to that attacker. Skystinger pattern.
     */
    @SerialName("BlockEvent")
    @Serializable
    data class BlockEvent(
        val filter: GameObjectFilter? = null,
        val attackerFilter: GameObjectFilter? = null
    ) : EventPattern {
        override val description: String = buildString {
            append(if (filter != null) "a ${filter.description} blocks" else "a creature blocks")
            if (attackerFilter != null) append(" a ${attackerFilter.description}")
        }
        override fun applyTextReplacement(replacer: TextReplacer): EventPattern {
            val newFilter = filter?.applyTextReplacement(replacer)
            val newAttackerFilter = attackerFilter?.applyTextReplacement(replacer)
            val filterChanged = newFilter !== filter
            val attackerChanged = newAttackerFilter !== attackerFilter
            return if (filterChanged || attackerChanged) copy(filter = newFilter, attackerFilter = newAttackerFilter) else this
        }
    }

    /**
     * When a creature becomes blocked.
     * Binding SELF = "when this creature becomes blocked",
     * ANY = "whenever a creature you control becomes blocked" (filter=null),
     * ANY + filter = "whenever a [filter] becomes blocked" (any controller).
     */
    @SerialName("BecomesBlockedEvent")
    @Serializable
    data class BecomesBlockedEvent(
        val filter: GameObjectFilter? = null
    ) : EventPattern {
        override val description: String = buildString {
            if (filter != null) {
                append("a ${filter.description} becomes blocked")
            } else {
                append("a creature becomes blocked")
            }
        }

        override fun applyTextReplacement(replacer: TextReplacer): EventPattern {
            val f = filter ?: return this
            val newFilter = f.applyTextReplacement(replacer)
            return if (newFilter !== f) copy(filter = newFilter) else this
        }
    }

    /**
     * Synthetic "trigger" event used to wrap a [StateTriggeredAbility]'s effect into a
     * [TriggeredAbility] when the engine enqueues a state trigger onto the stack
     * (CR 603.8). This event is never matched against real game events — the engine
     * detects state-trigger transitions via the [com.wingedsheep.engine.event.StateTriggerPoller]
     * and produces a [com.wingedsheep.engine.event.PendingTrigger] directly.
     */
    @SerialName("StateConditionMetEvent")
    @Serializable
    data object StateConditionMetEvent : EventPattern {
        override val description: String = "the state condition is met"
    }

    /**
     * When this attacking creature reaches the end of the Declare Blockers step with
     * no blockers assigned to it (CR 509.3g — "attacks and isn't blocked").
     *
     * SELF only — "when this creature attacks and isn't blocked". An ANY-binding
     * filtered variant ("whenever a [filter] attacks and isn't blocked") is not yet
     * wired in [com.wingedsheep.engine.event.TriggerMatcher]; add the matcher/detector
     * branches and a filter field together when a card needs it.
     *
     * Mirrors [BecomesBlockedEvent]. Detected (not emitted) once per unblocked attacker
     * after blocker declaration is finalized for the current combat.
     */
    @SerialName("BecomesUnblockedEvent")
    @Serializable
    data object BecomesUnblockedEvent : EventPattern {
        override val description: String = "a creature attacks and isn't blocked"
    }

    /**
     * When this creature blocks or becomes blocked by a creature matching [partnerFilter].
     * Binding SELF = "when this creature blocks or becomes blocked by [filter]".
     *
     * TriggerContext.triggeringEntityId = the combat partner (the creature that matched the filter).
     * Used for Corrosive Ooze.
     */
    @SerialName("BlocksOrBecomesBlockedByEvent")
    @Serializable
    data class BlocksOrBecomesBlockedByEvent(
        val partnerFilter: GameObjectFilter? = null
    ) : EventPattern {
        override val description: String = buildString {
            append("this creature blocks or becomes blocked by ")
            if (partnerFilter != null) {
                append(describeObjectForEvent(partnerFilter))
            } else {
                append("a creature")
            }
        }

        override fun applyTextReplacement(replacer: TextReplacer): EventPattern {
            val f = partnerFilter ?: return this
            val newFilter = f.applyTextReplacement(replacer)
            return if (newFilter !== f) copy(partnerFilter = newFilter) else this
        }
    }

    // ---- Damage Triggers ----

    /**
     * When a source deals damage.
     * Binding SELF = "when this creature deals damage".
     *
     * Used for triggers on the SOURCE of damage (e.g., "whenever this creature deals combat damage to a player").
     * For triggers on the RECIPIENT ("whenever this creature is dealt damage"), use [DamageReceivedEvent].
     */
    @SerialName("DealsDamageEvent")
    @Serializable
    data class DealsDamageEvent(
        val damageType: DamageType = DamageType.Any,
        val recipient: RecipientFilter = RecipientFilter.Any,
        val sourceFilter: GameObjectFilter? = null,
        /** Extensible, conjunctive facts about the damage event and its source. */
        val requires: Set<com.wingedsheep.sdk.scripting.events.DamagePredicate> = emptySet(),
        /**
         * When true, the trigger fires only on damage that exceeded what was needed to be
         * lethal (CR 120.4a). Combined with the existing damageType / recipient / sourceFilter
         * gates — e.g. Fall of Cair Andros uses
         * `DealsDamageEvent(damageType = NonCombat, recipient = Matching(creatureOpponentControls),
         *  requireExcess = true)` and reads the excess via
         * `ContextPropertyKey.TRIGGER_EXCESS_DAMAGE_AMOUNT`.
         */
        val requireExcess: Boolean = false,
        /**
         * Batch ("one or more") semantics — CR 603.2c: an ability triggers only once each time
         * its trigger event occurs. When true, simultaneous damage to several matching
         * recipients (a sweeper, combat damage to multiple blockers) fires the trigger once per
         * event batch instead of once per damaged recipient — "Whenever one or more creatures
         * your opponents control are dealt excess noncombat damage" (Magmatic Galleon) vs the
         * per-recipient "Whenever a creature is dealt excess noncombat damage" (Fall of Cair
         * Andros). Only honored for `TriggerBinding.ANY` observer triggers; SELF/ATTACHED
         * damage triggers are inherently per-source-event.
         */
        val batch: Boolean = false
    ) : EventPattern {
        override val description: String = buildString {
            if (batch) {
                // Recipient-side batch wording: "one or more [recipients] are dealt … damage".
                append("one or more ")
                append(if (recipient != RecipientFilter.Any) recipient.description else "permanents or players")
                append(" are dealt ")
                if (requireExcess) append("excess ")
                if (damageType != DamageType.Any) {
                    append(damageType.description)
                    append(" ")
                }
                append("damage")
                if (sourceFilter != null) {
                    append(" by ")
                    append(describeObjectForEvent(sourceFilter))
                }
            } else {
                if (sourceFilter != null) {
                    append(describeObjectForEvent(sourceFilter))
                    append(" ")
                }
                append("deals ")
                if (requireExcess) append("excess ")
                if (damageType != DamageType.Any) {
                    append(damageType.description)
                    append(" ")
                }
                append("damage")
                if (recipient != RecipientFilter.Any) {
                    append(" to ")
                    append(recipient.description)
                }
            }
            requires.forEach { append(" ").append(it.description) }
        }

        override fun applyTextReplacement(replacer: TextReplacer): EventPattern {
            val f = sourceFilter ?: return this
            val newFilter = f.applyTextReplacement(replacer)
            return if (newFilter !== f) copy(sourceFilter = newFilter) else this
        }
    }

    /**
     * When this permanent is dealt damage.
     * Binding SELF = "whenever this creature is dealt damage".
     *
     * The [source] filter distinguishes "damaged by a creature" vs "damaged by a spell".
     */
    @SerialName("DamageReceivedEvent")
    @Serializable
    data class DamageReceivedEvent(
        val source: SourceFilter = SourceFilter.Any
    ) : EventPattern {
        override val description: String = buildString {
            append("this is dealt damage")
            if (source != SourceFilter.Any) {
                append(" by ")
                append(source.description)
            }
        }
    }

    /**
     * Whenever a creature dealt damage this turn by a matching source dies.
     *
     * - [sourceFilter] == null → the Soul Collector shape, bound SELF: "whenever a creature dealt
     *   damage by this creature this turn dies". Detection uses the
     *   DamageDealtToCreaturesThisTurnComponent on the source (this) entity.
     * - [sourceFilter] == null, bound ATTACHED → the Scythe of the Wretched shape: "whenever a creature
     *   dealt damage by *equipped* creature this turn dies". Same tracker, read off the attachment
     *   target instead of off the permanent bearing the trigger. The attachment is resolved when the
     *   creature dies, so the Equipment may have moved since the damage was dealt (its own ruling), and
     *   an unattached Equipment never fires.
     * - [sourceFilter] != null → an observer shape (binding ANY): "whenever a creature dealt damage
     *   this turn by [a source matching the filter] dies" (Shelob, Child of Ungoliant: "by a Spider
     *   you controlled"). The damaging sources are evaluated against the filter using last-known
     *   information from when the damage was dealt (CR 603.10a / 608.2h), so a Spider that died in
     *   the same combat still qualifies. The filter's controller predicate is resolved relative to
     *   the controller of the permanent bearing the trigger.
     */
    @SerialName("CreatureDealtDamageBySourceDiesEvent")
    @Serializable
    data class CreatureDealtDamageBySourceDiesEvent(
        val sourceFilter: GameObjectFilter? = null
    ) : EventPattern {
        override val description: String =
            if (sourceFilter == null) "whenever a creature dealt damage by this creature this turn dies"
            else "whenever a creature dealt damage this turn by ${sourceFilter.description} dies"
    }

    /**
     * When a "prevent the next damage from a chosen source" shield prevents damage this way
     * (Deflecting Palm, New Way Forward). Only used as the spec of an event-based delayed
     * triggered ability that the shield links to via id — the engine scopes it to the shield's
     * own prevention, exposing the prevented amount and the source's controller to the effect.
     */
    @SerialName("DamagePreventedEvent")
    @Serializable
    data object DamagePreventedEvent : EventPattern {
        override val description: String = "when damage is prevented this way"
    }

    /**
     * When a card is played (cast as a spell or played as a land) using a specific
     * "you may play this card" permission — i.e. an impulse-style "exile … you may play
     * that card this turn" grant. Used only as the spec of an event-based delayed
     * triggered ability that the granting permission links to via id, so the rider
     * ("When you play a card this way, …") fires on the stack. Mirrors the link-id
     * scoping of [DamagePreventedEvent]. (Fires of Mount Doom.)
     */
    @SerialName("CardPlayedFromPermissionEvent")
    @Serializable
    data object CardPlayedFromPermissionEvent : EventPattern {
        override val description: String = "when you play a card this way"
    }

    // ---- Phase/Step Triggers ----

    /**
     * At the beginning of a step.
     * [player] controls whose turn it fires on:
     * - Player.You = your upkeep/end step (controllerOnly)
     * - Player.Each = each upkeep/end step
     */
    @SerialName("StepEvent")
    @Serializable
    data class StepEvent(
        val step: Step,
        val player: Player = Player.You
    ) : EventPattern {
        override val description: String = buildString {
            append("at the beginning of ")
            when (player) {
                Player.You -> append("your ")
                Player.Each -> append("each ")
                else -> append("${player.description}'s ")
            }
            append(step.displayName)
        }
    }

    // ---- Spell/Card Triggers ----

    /**
     * When a spell is cast.
     */
    @SerialName("SpellCastEvent")
    @Serializable
    data class SpellCastEvent(
        val spellFilter: GameObjectFilter = GameObjectFilter.Any,
        val player: Player = Player.You,
        /**
         * Extensible set of cast-time facts the trigger requires (conjunctive).
         * Each predicate is a [SpellCastPredicate] sealed-case — adding a new
         * cast-time mechanic (was-copied, was-overloaded, paid-life-cost, …)
         * is one new sealed-case + one matcher branch, not a new field here.
         *
         * Current cases: [SpellCastPredicate.CastFromZone],
         * [SpellCastPredicate.WasKicked],
         * [SpellCastPredicate.PaidWithManaFromSubtype].
         */
        val requires: Set<SpellCastPredicate> = emptySet(),
    ) : EventPattern {
        override val description: String = buildString {
            append(player.description)
            append(" casts ")
            val wasKicked = SpellCastPredicate.WasKicked in requires
            val isModal = SpellCastPredicate.IsModal in requires
            val prefixedQualifiers = listOfNotNull(
                "kicked".takeIf { wasKicked },
                "modal".takeIf { isModal }
            )
            val filterDesc = spellFilter.description
            val anyPrefix = prefixedQualifiers.isNotEmpty()
            if (anyPrefix) {
                append("a ")
                append(prefixedQualifiers.joinToString(" "))
                append(" ")
                if (filterDesc == "card" || filterDesc.isBlank()) append("spell") else append("$filterDesc spell")
            } else {
                if (filterDesc == "card" || filterDesc.isBlank()) {
                    append("a spell")
                } else {
                    append("a $filterDesc spell")
                }
            }
            // Suffix qualifiers (cast-from-zone, mana-source, …) in registration order.
            requires
                .filter { it !is SpellCastPredicate.WasKicked && it !is SpellCastPredicate.IsModal }
                .forEach { append(" ").append(it.description) }
        }
    }

    /**
     * When a player casts their Nth spell in a turn.
     * Fires on SpellCastEvent when the casting player's per-turn spell count
     * crosses the specified threshold.
     *
     * Used by cards like Hearthborn Battler: "Whenever a player casts their second spell each turn"
     *
     * [spellFilter] narrows *which* spells the count runs over, so the ordinal is per-kind rather
     * than over every spell: The Queen of Dale's "casts their first **noncreature** spell each turn"
     * is `nthSpell = 1, spellFilter = GameObjectFilter.Noncreature`. The count reads the caster's
     * cast history, so it tracks casts and not resolutions — a matching spell already cast this turn
     * closes the window even if it was countered, exactly as `nthOfTypePerTurn` does for cost and
     * flash gates. `null` (the default) counts every spell, the Hearthborn Battler shape.
     *
     * @param nthSpell The spell number that triggers this (e.g., 2 for "second spell")
     * @param player Which player's spell count to track
     * @param spellFilter Restricts the count to matching spells; null counts all of them
     */
    @SerialName("NthSpellCastEvent")
    @Serializable
    data class NthSpellCastEvent(
        val nthSpell: Int,
        val player: Player = Player.Each,
        val spellFilter: GameObjectFilter? = null
    ) : EventPattern {
        override val description: String = buildString {
            append(player.description)
            append(" casts their ")
            append(when (nthSpell) {
                1 -> "first"
                2 -> "second"
                3 -> "third"
                else -> "${nthSpell}th"
            })
            append(" ")
            spellFilter?.let { append(it.description).append(" ") }
            append("spell each turn")
        }

        override fun applyTextReplacement(replacer: TextReplacer): EventPattern {
            val newFilter = spellFilter?.applyTextReplacement(replacer)
            return if (newFilter !== spellFilter) copy(spellFilter = newFilter) else this
        }
    }

    /**
     * When you cast this spell — a "cast trigger" (CR 603.2) that fires on the spell's *own*
     * cast while it is on the stack; the ability travels with the spell. This is distinct from
     * [SpellCastEvent], which observes *other* spells being cast from a permanent on the
     * battlefield. A [CastThisSpellEvent] trigger is detected only via the dedicated self-cast
     * path in the engine's TriggerDetector and is never indexed against battlefield permanents,
     * so it cannot fire once the spell has resolved into a permanent.
     *
     * The controller is always the caster, so there is no player parameter. For an intervening
     * "if" (CR 603.4) such as Sage of the Skies' "if you've cast another spell this turn",
     * attach a `triggerRestriction` to the triggered ability rather than encoding it here.
     */
    @SerialName("CastThisSpellEvent")
    @Serializable
    data object CastThisSpellEvent : EventPattern {
        override val description: String = "you cast this spell"
    }

    /**
     * When you expend N — i.e., you spend your Nth total mana to cast spells
     * during a turn. Triggers at most once per turn per threshold.
     *
     * Used by Bloomburrow cards like Junkblade Bruiser.
     *
     * @param threshold The mana threshold that triggers this (e.g., 4 for "expend 4")
     * @param player Which player's spending to track
     */
    @SerialName("ExpendEvent")
    @Serializable
    data class ExpendEvent(
        val threshold: Int,
        val player: Player = Player.You
    ) : EventPattern {
        override val description: String = "${player.description} expends $threshold"
    }

    /**
     * When a player commits a crime (Outlaws of Thunder Junction).
     *
     * Fires when [player] casts a spell, activates an ability, or puts a triggered ability
     * on the stack with at least one initial target that is an opponent, a permanent / spell /
     * ability an opponent controls, or a card in an opponent's graveyard. Fires at most once
     * per spell or ability regardless of how many qualifying targets it has.
     *
     * Used by cards like Forsaken Miner: "Whenever you commit a crime, you may pay {B}…".
     */
    @SerialName("CommitCrimeEvent")
    @Serializable
    data class CommitCrimeEvent(
        val player: Player = Player.You
    ) : EventPattern {
        override val description: String = "${player.description} commit a crime"
    }

    /**
     * When this card becomes plotted (Outlaws of Thunder Junction). A Plot card's controller
     * pays the plot cost and exiles it from hand face up as a special action — the card is then
     * marked plotted (CR 718). This is a SELF-scoped trigger: it fires only for the very card
     * that became plotted, while it sits in exile.
     *
     * Used by cards like Aloe Alchemist: "When this card becomes plotted, target creature gets
     * +3/+2 and gains trample until end of turn."
     */
    @SerialName("BecomesPlottedEvent")
    @Serializable
    data object BecomesPlottedEvent : EventPattern {
        override val description: String = "this card becomes plotted"
    }

    /**
     * When a permanent becomes saddled (CR 702.171b) — a Saddle ability resolved on it.
     *
     * Binding SELF = "whenever this creature becomes saddled" (the Mount itself, Stubborn
     * Burrowfiend); ANY = "whenever a [filter] becomes saddled". The Mount stays on the battlefield
     * while saddled, so this matches in the regular battlefield trigger loop (unlike the
     * exile-resident plotted/door designations).
     *
     * [firstTimeEachTurn] restricts to the first time the permanent became saddled this turn —
     * Saddle may be activated again while already saddled, but the "first time each turn"
     * intervening-if only fires on the first resolution per turn.
     */
    @SerialName("BecameSaddledEvent")
    @Serializable
    data class BecameSaddledEvent(
        val filter: GameObjectFilter = GameObjectFilter.Any,
        val firstTimeEachTurn: Boolean = false
    ) : EventPattern {
        override val description: String = buildString {
            append(describeObjectForEvent(filter))
            append(" becomes saddled")
            if (firstTimeEachTurn) append(" for the first time each turn")
        }

        override fun applyTextReplacement(replacer: TextReplacer): EventPattern {
            val newFilter = filter.applyTextReplacement(replacer)
            return if (newFilter !== filter) copy(filter = newFilter) else this
        }
    }

    /**
     * When an Aura, Equipment, or Fortification becomes attached to a permanent or player
     * (CR 603.2e — "becomes" triggers fire only at the moment of attaching, not on a state that
     * already exists, and not on phasing in/out per CR 702.26j).
     *
     * The triggering entity is the *attachment* (the aura/equipment that became attached); the
     * permanent it attached to is carried as the attached-to entity in the trigger context
     * ([com.wingedsheep.sdk.scripting.targets.EffectTarget.AttachedToTriggeringPermanent]).
     *
     * Binding SELF = "whenever this Equipment/Aura becomes attached to a creature" (Assimilation
     * Aegis). Binding ANY with [attachmentFilter] / [attachmentController] = "whenever a [filter]
     * you control becomes attached to …" (Eriette, the Beguiler).
     *
     * @property attachmentFilter restricts which attachment qualifies (e.g. Aura, Equipment).
     * @property attachmentController restricts who must control the attachment (e.g. [Player.You]).
     * @property attachedToFilter restricts what it must attach to (e.g. a nonland permanent an
     *   opponent controls). The attached-to permanent is matched against this filter, with the
     *   triggering attachment available as the comparison reference for relative predicates
     *   (e.g. mana value at most the Aura's mana value).
     */
    @SerialName("BecomesAttachedEvent")
    @Serializable
    data class BecomesAttachedEvent(
        val attachmentFilter: GameObjectFilter = GameObjectFilter.Any,
        val attachmentController: Player = Player.Any,
        val attachedToFilter: GameObjectFilter = GameObjectFilter.Any,
    ) : EventPattern {
        override val description: String = buildString {
            append(describeObjectForEvent(attachmentFilter))
            if (attachmentController == Player.You) append(" you control")
            append(" becomes attached")
            if (attachedToFilter != GameObjectFilter.Any) {
                append(" to ${describeObjectForEvent(attachedToFilter)}")
            }
        }

        override fun applyTextReplacement(replacer: TextReplacer): EventPattern {
            val newAttachment = attachmentFilter.applyTextReplacement(replacer)
            val newAttachedTo = attachedToFilter.applyTextReplacement(replacer)
            return if (newAttachment !== attachmentFilter || newAttachedTo !== attachedToFilter) {
                copy(attachmentFilter = newAttachment, attachedToFilter = newAttachedTo)
            } else this
        }
    }

    /**
     * When an Aura, Equipment, or Fortification becomes **unattached** from a permanent — the
     * mirror of [BecomesAttachedEvent].
     *
     * Fires on every way an attachment stops being attached to its host while that attachment
     * existed attached a moment earlier (CR 701.3d): an explicit "unattach it" effect, equipping it
     * to a *different* permanent, the attachment leaving the battlefield, the host leaving the
     * battlefield, and the CR 704.5n state-based unattach when the pairing becomes illegal (the
     * host stops being a creature, the Equipment itself becomes a creature, protection). It does
     * **not** fire for an attachment that was never attached.
     *
     * The triggering entity is the *attachment*; the permanent it came off is carried as the
     * attached-to entity in the trigger context
     * ([com.wingedsheep.sdk.scripting.targets.EffectTarget.AttachedToTriggeringPermanent]) —
     * "that permanent" in the payoff.
     *
     * Because the unattach can be *caused* by the attachment leaving the battlefield, the ability
     * fires from the attachment's last-known existence (CR 603.6e/603.10), exactly like a
     * leaves-the-battlefield trigger. The former host may likewise be gone by resolution, in which
     * case a payoff that acts on it simply does nothing — Stitcher's Graft's ruling spells this out:
     * "It also becomes unattached if the equipped creature leaves the battlefield, but the triggered
     * ability won't do anything in that case."
     *
     * Binding SELF = "whenever this Equipment becomes unattached from a permanent" (Stitcher's
     * Graft). Binding ANY with [attachmentFilter] / [attachmentController] = "whenever an Aura you
     * control becomes unattached …".
     *
     * @property attachmentFilter restricts which attachment qualifies (e.g. Aura, Equipment).
     * @property attachmentController restricts who must control the attachment (e.g. [Player.You]).
     * @property unattachedFromFilter restricts what it must have come off. Matched against the
     *   former host with the triggering attachment exposed as the comparison reference, mirroring
     *   [BecomesAttachedEvent.attachedToFilter]. Evaluated against the host's *current* state, so a
     *   host that left the battlefield matches only [GameObjectFilter.Any].
     */
    @SerialName("BecomesUnattachedEvent")
    @Serializable
    data class BecomesUnattachedEvent(
        val attachmentFilter: GameObjectFilter = GameObjectFilter.Any,
        val attachmentController: Player = Player.Any,
        val unattachedFromFilter: GameObjectFilter = GameObjectFilter.Any,
    ) : EventPattern {
        override val description: String = buildString {
            append(describeObjectForEvent(attachmentFilter))
            if (attachmentController == Player.You) append(" you control")
            append(" becomes unattached")
            if (unattachedFromFilter != GameObjectFilter.Any) {
                append(" from ${describeObjectForEvent(unattachedFromFilter)}")
            }
        }

        override fun applyTextReplacement(replacer: TextReplacer): EventPattern {
            val newAttachment = attachmentFilter.applyTextReplacement(replacer)
            val newUnattachedFrom = unattachedFromFilter.applyTextReplacement(replacer)
            return if (newAttachment !== attachmentFilter || newUnattachedFrom !== unattachedFromFilter) {
                copy(attachmentFilter = newAttachment, unattachedFromFilter = newUnattachedFrom)
            } else this
        }
    }

    /**
     * When a player chooses one or more targets.
     *
     * Fires when [player] casts a spell, activates an ability, or puts a triggered ability
     * on the stack with at least one chosen target. Fires at most once per spell or ability
     * regardless of how many targets it has. The triggering entity is the spell/ability on
     * the stack, so an effect resolving from this trigger can read and change those targets
     * (Psychic Battle).
     */
    @SerialName("TargetsChosenEvent")
    @Serializable
    data class TargetsChosenEvent(
        val player: Player = Player.Each
    ) : EventPattern {
        override val description: String = "${player.description} chooses one or more targets"
    }

    /**
     * When a card is cycled.
     */
    @SerialName("CycleEvent")
    @Serializable
    data class CycleEvent(
        val player: Player = Player.You
    ) : EventPattern {
        override val description: String = "${player.description} cycles a card"
    }

    // ---- Gift Triggers ----

    /**
     * When a player gives a gift (Bloomburrow gift mechanic).
     * Used by cards like Jolly Gerbils: "Whenever you give a gift, draw a card."
     */
    @SerialName("GiftGivenEvent")
    @Serializable
    data class GiftGivenEvent(
        val player: Player = Player.You
    ) : EventPattern {
        override val description: String = "${player.description} gives a gift"
    }

    // ---- Room Triggers (DSK) ----

    /**
     * Whenever a player fully unlocks a Room (Duskmourn mechanic).
     * A Room is fully unlocked when both of its doors have been unlocked.
     *
     * Used by Eerie abilities: "Whenever an enchantment you control enters and
     * whenever you fully unlock a Room, [effect]."
     *
     * [player] filters whose Room is unlocked (default: you).
     */
    @SerialName("RoomFullyUnlockedEvent")
    @Serializable
    data class RoomFullyUnlockedEvent(
        val player: Player = Player.You
    ) : EventPattern {
        override val description: String = "${player.description} fully unlock a Room"
    }

    /**
     * A door of a Room becomes unlocked (CR 709.5h). Fires whenever a face is given the
     * "unlocked" designation, either at ETB (the cast face) or via the unlock special action.
     *
     * Used by face-scoped triggered abilities authored as "When you unlock this door, …".
     * The matcher is face-aware: a SELF-bound trigger matches only when the unlocked face
     * is the face the ability was authored on.
     */
    @SerialName("DoorUnlockedEvent")
    @Serializable
    data class DoorUnlockedEvent(
        val player: Player = Player.You
    ) : EventPattern {
        override val description: String = "${player.description} unlock a door"
    }

    // ---- Targeting Triggers ----

    /**
     * When a permanent (or, opt-in, a spell on the stack or a player) becomes the target of a spell
     * or ability.
     * Binding SELF = "when this creature becomes the target",
     * ANY = "whenever a creature you control becomes the target".
     *
     * The [targetFilter] restricts what type of object can trigger this
     * (e.g., Cleric creatures you control).
     *
     * By default this matches **permanent** targets only — "a creature you control" means a creature
     * on the battlefield (Pawpatch Recruit, Daru Spiritualist), not a creature spell on the stack.
     * Set [includeSpellTargets] for the rarer "... or a creature spell you control" wording, which
     * also fires when a spell on the stack is targeted; the [targetFilter] is then matched against
     * the spell's card data, so a creature spell matches a `Creature` filter (Surrak, Elusive Hunter:
     * "a creature you control or a creature spell you control becomes the target").
     *
     * Players are the third target kind and are likewise opt-in, via [includePlayerTargets] — "a
     * player or permanent becomes the target" (Loki, God of Mischief). Without it a targeted player
     * never matches, so the many permanent-only triggers already in the pool stay unaffected by the
     * engine emitting target events for players. [includePlayerTargets] requires [targetFilter] to
     * be `Any` and throws otherwise: a player carries no card data for a filter to read, so the pair
     * would fire on permanents only while [description] still promised the player half. A future "a
     * player or *creature*" wording needs the object half and the player half kept apart.
     *
     * [byYou] restricts to spells or abilities controlled by the trigger's controller.
     * [firstTimeEachTurn] restricts to the first time each turn (used by Valiant).
     * [spellsOnly] restricts to "becomes the target of a **spell**" wording (King of the
     * Oathbreakers), ignoring abilities; [abilitiesOnly] is its mirror — "becomes the target of an
     * **ability**" (Loki, God of Mischief), ignoring spells. The default (neither) matches both.
     * They are mutually exclusive: asking for both would match nothing, and throws — as does the
     * equally contradictory [byYou] + [byOpponent] pair.
     *
     * Note that [spellsOnly] / [abilitiesOnly] narrow *what did the targeting*, while
     * [includeSpellTargets] / [includePlayerTargets] widen *what got targeted*; the two axes are
     * independent.
     */
    @SerialName("BecomesTargetEvent")
    @Serializable
    data class BecomesTargetEvent(
        val targetFilter: GameObjectFilter = GameObjectFilter.Any,
        val byYou: Boolean = false,
        val byOpponent: Boolean = false,
        val firstTimeEachTurn: Boolean = false,
        val includeSpellTargets: Boolean = false,
        val spellsOnly: Boolean = false,
        val includePlayerTargets: Boolean = false,
        val abilitiesOnly: Boolean = false
    ) : EventPattern {
        init {
            require(!(spellsOnly && abilitiesOnly)) {
                "BecomesTargetEvent cannot be both spellsOnly and abilitiesOnly — nothing would match"
            }
            require(!(byYou && byOpponent)) {
                "BecomesTargetEvent cannot be both byYou and byOpponent — nothing would match"
            }
            require(!includePlayerTargets || targetFilter == GameObjectFilter.Any) {
                "BecomesTargetEvent.includePlayerTargets only fires for players when targetFilter is Any — " +
                    "a player has no card data for a filter to read; split the object and player halves first"
            }
        }

        override val description: String = buildString {
            if (includePlayerTargets) {
                // The player half is only reachable with filter `Any` (see the require above), and
                // the one printed wording that uses it says "a player or permanent" — deliberately
                // narrower than the generic `describeObjectForEvent(Any)` phrasing ("a card or
                // permanent"), which is what the object-only branch below still renders.
                append("a player or ")
                if (targetFilter == GameObjectFilter.Any) append("permanent")
                else append(describeObjectForEvent(targetFilter))
            } else {
                append(describeObjectForEvent(targetFilter))
            }
            when {
                spellsOnly -> append(" becomes the target of a spell")
                abilitiesOnly -> append(" becomes the target of an ability")
                else -> append(" becomes the target of a spell or ability")
            }
            if (byYou) append(" you control")
            if (byOpponent) append(" an opponent controls")
            if (firstTimeEachTurn) append(" for the first time each turn")
        }

        override fun applyTextReplacement(replacer: TextReplacer): EventPattern {
            val newFilter = targetFilter.applyTextReplacement(replacer)
            return if (newFilter !== targetFilter) copy(targetFilter = newFilter) else this
        }
    }

    // ---- State Change Triggers ----

    /**
     * When a permanent becomes tapped.
     * Binding SELF = "whenever this becomes tapped".
     * [filter] optionally restricts which permanents count (e.g. only creatures or
     * lands) — used with [TriggerBinding.ANY] for "whenever a creature or land becomes
     * tapped" effects (Temporal Distortion). Null = any permanent.
     */
    @SerialName("TapEvent")
    @Serializable
    data class TapEvent(
        val filter: GameObjectFilter? = null,
        /**
         * Batch ("one or more … become tapped") semantics (CR 603.2c). When true the trigger fires
         * at most **once** per simultaneous tap batch regardless of how many matching permanents
         * were tapped together (Deeproot Pilgrimage), instead of once per tapped permanent. Handled
         * by the dedicated batch pass; the per-event path skips it. ANY-binding only.
         */
        val batch: Boolean = false,
        /**
         * Who must have done the tapping, relative to the trigger's controller — the difference
         * between the passive "**a** creature becomes tapped" (null, any tap from any cause) and
         * the active "whenever **you tap** a creature an opponent controls"
         * ([com.wingedsheep.sdk.scripting.references.Player.You], Wilds of Eldraine's Hylda of the
         * Icy Crown / Icewrought Sentry / Solitary Sanctuary / Sharae of Numbing Depths).
         *
         * The tapper is the controller of the spell, ability, or cost payment that caused the
         * permanent to become tapped; a permanent tapped as a turn-based action or to pay its own
         * controller's cost is tapped by its controller. Per the Hylda ruling, a spell *you* control
         * that instructs an **opponent** to tap a creature they control makes *them* the tapper, so
         * a `You` pattern does not fire (Tangle Wire).
         *
         * "An **untapped** creature" needs no separate axis: tapping is a transition (CR 701.26a —
         * "only untapped permanents can be tapped"), so an already-tapped permanent emits no tap
         * event at all.
         */
        val tapper: Player? = null,
        /**
         * *Why* the permanent had to become tapped — the difference between the cause-agnostic
         * "whenever this becomes tapped" (null, the default, any cause) and "whenever this becomes
         * tapped **to pay a teamwork cost**" ([TapReason.TEAMWORK], Agent Maria Hill).
         *
         * Orthogonal to [tapper], which says *who* caused the tap: a teamwork tap and an attack tap
         * are both performed by the permanent's own controller, so only the reason separates them.
         * Only the causes the engine classifies can be asked for — everything else reports
         * [TapReason.UNSPECIFIED] and is matched only by a null here. See [TapReason].
         *
         * **Use `null`, never [TapReason.UNSPECIFIED], for "any cause".** Asking for `UNSPECIFIED`
         * is a legal but meaningless predicate: it matches only the taps the engine has *not*
         * classified, so its meaning would quietly shrink the day a new cause is named, and it
         * renders as no clause at all in [description]. No printed card wants it.
         */
        val reason: TapReason? = null,
        /**
         * Restrict to the **first time each permanent became tapped this turn** — the per-permanent
         * "if it's the first time that creature has become tapped this turn" rider (Captain America,
         * Living Legend).
         *
         * Per-*permanent*, not per-*ability*: with several creatures tapping in the same turn the
         * trigger fires once for each of them, and a creature that untaps and taps again that turn
         * fires it no second time. That is exactly what `oncePerTurn` on the ability cannot express —
         * `oncePerTurn` caps the *ability* at one firing per turn, so it would answer only the first
         * creature. The two are composable and mean different things; reach for this one whenever the
         * printed "first time" clause names the object rather than the ability.
         *
         * The window is a *becomes tapped* window, not a *was tapped* one: a permanent that **entered
         * the battlefield tapped** never became tapped (CR 701.26a — only untapped permanents can be
         * tapped), so tapping it later that turn is still its first time.
         *
         * **Per-permanent only — cannot be combined with [batch].** No printed card pairs the "one or
         * more … become tapped" wording with a first-time clause, and the two plausible readings of
         * that combination (narrow the batch to its first-time taps, versus fire only on the turn's
         * first tap *batch*) are not obviously distinguishable without one. Rather than ship a guess
         * that a future card would silently inherit, the combination is rejected outright; the first
         * real card decides the reading, and this `require` is where that decision gets recorded.
         *
         * This clause is a printed intervening "if" (CR 603.4), so a card using it wants **both**
         * checks: this rider for the check made when the trigger event occurs, and
         * `interveningIf = Conditions.TriggeringPermanentBecameTappedOnlyOnceThisTurn` for the check
         * made again at resolution. This one alone leaves the second check unimplemented.
         */
        val firstTimeEachTurn: Boolean = false
    ) : EventPattern {
        init {
            require(!(batch && firstTimeEachTurn)) {
                "TapEvent: firstTimeEachTurn is per-permanent and has no settled batch reading; " +
                    "see the field's documentation"
            }
        }

        override val description: String = buildString {
            if (tapper != null) {
                append(tapper.description.replaceFirstChar { it.uppercase() })
                append(if (batch) " tap one or more " else " tap a ")
                append(filter?.description ?: "permanent")
            } else {
                append(if (batch) "one or more " else "a ")
                append(filter?.description ?: "permanent")
                append(if (batch) " become tapped" else " becomes tapped")
            }
            if (reason != null && reason != TapReason.UNSPECIFIED) {
                append(" ")
                append(reason.description)
            }
            if (firstTimeEachTurn) append(" for the first time each turn")
        }
        override fun applyTextReplacement(replacer: TextReplacer): EventPattern {
            val newFilter = filter?.applyTextReplacement(replacer)
            return if (newFilter !== filter) copy(filter = newFilter) else this
        }
    }

    /**
     * When a player taps a land for mana (a land's mana ability resolves).
     *
     * Models the "Whenever a player taps a land for mana" family (Overabundance, Mana Flare,
     * Pulse of Llanowar, Heartbeat of Spring). [player] restricts whose tap fires the trigger
     * (relative to the trigger's controller — `Each` for any player, `Opponent`, `You`).
     * [landFilter] optionally restricts which lands count (e.g. only basic lands).
     *
     * Fires on the manual mana-ability activation path; automatic cost payment adds mana via the
     * solver and does not emit this event, matching how the engine handles mana-ability side
     * effects during auto-payment.
     */
    @SerialName("LandTappedForMana")
    @Serializable
    data class LandTappedForMana(
        val player: Player = Player.Each,
        val landFilter: GameObjectFilter? = null
    ) : EventPattern {
        override val description: String = buildString {
            append(player.description.replaceFirstChar { it.uppercase() })
            append(" taps a ")
            append(landFilter?.description ?: "land")
            append(" for mana")
        }
        override fun applyTextReplacement(replacer: TextReplacer): EventPattern {
            val newFilter = landFilter?.applyTextReplacement(replacer)
            return if (newFilter !== landFilter) copy(landFilter = newFilter) else this
        }
    }

    /**
     * When a permanent becomes untapped.
     * Binding SELF = "whenever this becomes untapped".
     *
     * [batch] = true is the untap analogue of [TapEvent] batch semantics (CR 603.2c): the trigger
     * fires at most **once** per simultaneous untap batch regardless of how many matching permanents
     * were untapped together — the untap step untaps all of a player's permanents at once — instead
     * of once per untapped permanent. Handled by the dedicated batch pass
     * (`TriggerDetector.detectUntapBatchTriggers`); the per-event path skips it. ANY-binding only.
     * The untapped permanents are exposed to the payoff as the trigger's captured collection, so a
     * "put that many counters" effect can read the count. [filter] optionally restricts which
     * untapped permanents count (e.g. "permanents you control" — The Millennium Calendar).
     */
    @SerialName("UntapEvent")
    @Serializable
    data class UntapEvent(
        val filter: GameObjectFilter? = null,
        val batch: Boolean = false
    ) : EventPattern {
        override val description: String = buildString {
            append(if (batch) "one or more " else "a ")
            append(filter?.description ?: "permanent")
            append(if (batch) " become untapped" else " becomes untapped")
        }
        override fun applyTextReplacement(replacer: TextReplacer): EventPattern {
            val newFilter = filter?.applyTextReplacement(replacer)
            return if (newFilter !== filter) copy(filter = newFilter) else this
        }
    }

    /**
     * When a permanent phases in (Rule 702.26).
     * Binding SELF = "whenever this permanent phases in",
     * ANY = "whenever a permanent matching [filter] phases in".
     *
     * [filter] optionally restricts which permanents count (e.g. "a Spirit you control" —
     * King of the Oathbreakers). Null = any permanent. A permanent phases in during its
     * controller's untap step; the phase-in trigger then fires with the permanent back on
     * the battlefield (same object, with its counters and attachments preserved).
     */
    @SerialName("PhasesInEvent")
    @Serializable
    data class PhasesInEvent(
        val filter: GameObjectFilter? = null
    ) : EventPattern {
        override val description: String = buildString {
            append(filter?.let { describeObjectForEvent(it) } ?: "a permanent")
            append(" phases in")
        }
        override fun applyTextReplacement(replacer: TextReplacer): EventPattern {
            val newFilter = filter?.applyTextReplacement(replacer)
            return if (newFilter !== filter) copy(filter = newFilter) else this
        }
    }

    /**
     * When a permanent is turned face up.
     * Binding SELF = "when this is turned face up".
     */
    @SerialName("TurnFaceUpEvent")
    @Serializable
    data object TurnFaceUpEvent : EventPattern {
        override val description: String = "this is turned face up"
    }

    /**
     * When a creature is turned face up.
     * [player] filters whose creature: [Player.You] (default), [Player.Any], etc.
     * [filter] narrows *which* creature, evaluated against the permanent's post-flip
     * (face-up) characteristics — "whenever a Detective you control is turned face up"
     * (Perimeter Enforcer) is `filter = Creature.withSubtype(Subtype.DETECTIVE)`.
     * Defaults to [GameObjectFilter.Any], i.e. any creature turned face up.
     */
    @SerialName("CreatureTurnedFaceUpEvent")
    @Serializable
    data class CreatureTurnedFaceUpEvent(
        val player: Player = Player.You,
        val filter: GameObjectFilter = GameObjectFilter.Any
    ) : EventPattern {
        override val description: String = buildString {
            append(if (filter == GameObjectFilter.Any) "a creature " else "a ${filter.description} ")
            when (player) {
                is Player.You -> append("you control ")
                is Player.Any -> {}
                else -> append("${player.description} controls ")
            }
            append("is turned face up")
        }
    }

    /**
     * When a permanent transforms.
     * [intoBackFace] filters direction: true = to back, false = to front, null = either.
     */
    @SerialName("TransformEvent")
    @Serializable
    data class TransformEvent(
        val intoBackFace: Boolean? = null
    ) : EventPattern {
        override val description: String = buildString {
            append("this transforms")
            when (intoBackFace) {
                true -> append(" into its back face")
                false -> append(" into its front face")
                null -> {}
            }
        }
    }

    /**
     * When control of a permanent changes, in a given [direction] relative to the ability's
     * controller (CR 800.4 / 720). The [direction] selects which side of the control change the
     * ability watches:
     *  - [ControlChangeDirection.GAINED] (default): "when you **gain** control of this from another
     *    player" — the new controller is the ability's controller. With [TriggerBinding.SELF] this
     *    is the resident self-trigger (Risky Move). For an event-based **delayed** trigger scoped to
     *    a watched permanent, it fires when that permanent's controller becomes the trigger's
     *    controller.
     *  - [ControlChangeDirection.LOST]: "when you **lose** control of [the watched permanent]" — the
     *    *old* controller was the ability's controller. Used as the reflexive delayed trigger on
     *    Stolen Uniform ("When you lose control of that Equipment this turn …"). A delayed trigger
     *    of this shape fires on any mid-turn control change away from you (e.g. another player steals
     *    the permanent).
     *
     * [requireOpponent] (LOST only) additionally requires the *new* controller to be an opponent of
     * the ability's controller, modelling "whenever an **opponent** gains control of a permanent from
     * you". Combined with [TriggerBinding.ANY] this is a resident, battlefield-wide watcher: it fires
     * once for every permanent the ability's controller loses to an opponent — including the source of
     * the ability itself when that source is the permanent being stolen (look-back-in-time, CR 603.10),
     * so the *old* controller is still the one who receives the trigger (Zidane, Tantalus Thief).
     *
     * The default is [ControlChangeDirection.GAINED] so existing GAIN-control self-triggers keep
     * their meaning.
     */
    @SerialName("ControlChangeEvent")
    @Serializable
    data class ControlChangeEvent(
        val direction: ControlChangeDirection = ControlChangeDirection.GAINED,
        val requireOpponent: Boolean = false
    ) : EventPattern {
        override val description: String = when (direction) {
            ControlChangeDirection.GAINED -> "you gain control of this permanent"
            ControlChangeDirection.LOST ->
                if (requireOpponent) "an opponent gains control of a permanent from you"
                else "you lose control of this permanent"
        }
    }

    // ---- Counter Triggers ----

    /**
     * When one or more counters of a specific type are placed on a permanent.
     *
     * Examples:
     * - "Whenever you put one or more +1/+1 counters on a creature you control"
     *   → CountersPlacedEvent(counterType = Counters.PLUS_ONE_PLUS_ONE, filter = GameObjectFilter.Creature.youControl())
     * - "Whenever you put one or more +1/+1 counters on one or more other Heroes you control"
     *   → CountersPlacedEvent(counterType = Counters.PLUS_ONE_PLUS_ONE, placedBy = Player.You,
     *     filter = GameObjectFilter.Creature.youControl().withSubtype(Subtype.HERO), batch = true)
     *     with [TriggerBinding.OTHER]
     *
     * @property counterType The counter type to match (e.g., "+1/+1", "LORE")
     * @property filter Filter for the permanent receiving counters
     */
    @SerialName("CountersPlacedEvent")
    @Serializable
    data class CountersPlacedEvent(
        val counterType: String,
        val filter: GameObjectFilter = GameObjectFilter.Any,
        /**
         * When true, the trigger fires only the first time counters are put on the affected
         * permanent this turn (per CR intervening-if "if it's the first time counters have been
         * put on that creature this turn", e.g. Stalwart Successor). Matched against the engine
         * event's own "first counters this turn" flag, mirroring how Valiant uses
         * `firstTimeEachTurn` on [BecomesTargetEvent].
         */
        val firstTimeEachTurn: Boolean = false,
        /**
         * Which player must have *put* the counters, per CR 122.6/122.6a — distinct from [filter],
         * which constrains the permanent *receiving* them. `null` (default) matches any placer, so
         * "counters put on a creature you control" is expressed purely via the recipient filter.
         * Set to [Player.You] for "Whenever **you** put one or more counters on a creature" where
         * the recipient is unrestricted (Earth Kingdom General) — a placement by an opponent then
         * doesn't fire it. The placer is the controller of the effect that put the counters, or
         * (for a permanent entering with counters) that permanent's controller (CR 122.6a).
         * A placement the engine can't attribute to a placer never matches a non-null selector.
         */
        val placedBy: Player? = null,
        /**
         * Batch ("one or more counters on **one or more** permanents") semantics — CR 603.2c: an
         * ability triggers only once each time its trigger event occurs, and a single effect that
         * puts counters on several permanents at once is one such event.
         *
         * The two multiplicity shapes this flag selects, both of which the printed text writes as
         * "one or more counters" (the counters on a *single* permanent are always one event, so
         * that half needs no flag):
         *  - `false` (default): the *per-permanent* template "Whenever one or more counters are put
         *    on **a** creature you control" (Stalwart Successor, Exemplar of Light) — fires once for
         *    EACH permanent that received counters, so an effect hitting three creatures fires it
         *    three times.
         *  - `true`: the *batch* template "Whenever you put one or more counters on **one or more**
         *    creatures you control" (Invisible Woman, Sue Storm) — fires at most **once** per
         *    placement batch no matter how many permanents received counters or how many counters
         *    each got.
         *
         * Handled by the dedicated batch pass (`TriggerDetector.detectCountersPlacedBatchTriggers`);
         * the per-event path skips it. Every other axis ([counterType], [placedBy],
         * [firstTimeEachTurn], [filter], and the ability's [TriggerBinding]) *narrows* the batch
         * rather than discarding it: a batch that also placed counters on non-matching permanents
         * still fires, once, on its matching ones alone. Separate resolutions are separate batches,
         * so two effects each placing counters this turn fire it twice. The matching recipients are
         * exposed as the trigger's captured collection, as [UntapEvent.batch] does.
         *
         * Note that [firstTimeEachTurn] defaults to `false` here but to `!batch` in the
         * `Triggers.countersPlacedOn(...)` facade: a batch template essentially never carries a
         * printed "for the first time this turn" rider, so the facade stops handing one to it. The
         * combination remains expressible on both — it just has to be asked for.
         *
         * Mirrors [TapEvent.batch] / [UntapEvent.batch] / [DealsDamageEvent.batch], except that
         * those three are ANY-binding only while this one also honors [TriggerBinding.SELF]
         * ("counters put on this permanent") and [TriggerBinding.OTHER] ("on one or more **other**
         * Heroes you control").
         */
        val batch: Boolean = false
    ) : EventPattern {
        override val description: String = buildString {
            val typeLabel = if (counterType == com.wingedsheep.sdk.core.Counters.ANY) "" else "$counterType "
            if (placedBy != null) {
                append("${placedBy.description} put one or more ${typeLabel}counters on ")
            } else {
                append("one or more ${typeLabel}counters are placed on ")
            }
            // Batch wording names the recipients in the plural ("… on one or more Heroes you
            // control"); the per-permanent wording names a single one ("… on a creature you
            // control"). Both keep the controller as a suffix rather than the prefix
            // `filter.description` would put it in ("… on one or more you control Hero creature").
            if (batch) {
                append("one or more ")
                append(describeObjectsForEvent(filter))
            } else {
                append(describeObjectForEvent(filter))
            }
            if (firstTimeEachTurn) append(" for the first time this turn")
        }
    }

    /**
     * When one or more counters of a specific type are **removed** from a permanent — the mirror of
     * [CountersPlacedEvent].
     *
     * Set [lastRemoved] for the "when the **last** counter is removed" templating that the
     * counter-countdown mechanics share (CR 310.12b's Siege defeat trigger, and the vanishing /
     * suspend family's "when the last time counter is removed from this permanent"). It's a
     * property of the removal, not a separate event: the trigger fires only when the removal left
     * the permanent with none of that counter type on it, so removing 2 of 5 defense counters is
     * silent while the removal that takes the count to 0 fires exactly once. A removal of 0
     * counters never fires it.
     *
     * Examples:
     * - "When the last defense counter is removed from this permanent"
     *   → CountersRemovedEvent(counterType = Counters.DEFENSE, lastRemoved = true) with
     *     [TriggerBinding.SELF]
     * - "Whenever one or more +1/+1 counters are removed from a creature you control"
     *   → CountersRemovedEvent(counterType = Counters.PLUS_ONE_PLUS_ONE, filter =
     *     GameObjectFilter.Creature.youControl())
     *
     * @property counterType The counter type to match, or [com.wingedsheep.sdk.core.Counters.ANY]
     *   for counters of any kind.
     * @property filter Filter for the permanent the counters were removed from.
     * @property lastRemoved When true, fires only for the removal that takes the permanent's count
     *   of [counterType] to zero.
     */
    @SerialName("CountersRemovedEvent")
    @Serializable
    data class CountersRemovedEvent(
        val counterType: String,
        val filter: GameObjectFilter = GameObjectFilter.Any,
        val lastRemoved: Boolean = false,
    ) : EventPattern {
        override val description: String = buildString {
            val typeLabel = if (counterType == com.wingedsheep.sdk.core.Counters.ANY) "" else "$counterType "
            if (lastRemoved) {
                append("the last ${typeLabel}counter is removed from ")
            } else {
                append("one or more ${typeLabel}counters are removed from ")
            }
            append(describeObjectForEvent(filter))
        }
    }

    // ---- Draw/Reveal Triggers ----

    /**
     * When a spell or ability is put onto the stack.
     * Used for cards like Grip of Chaos: "Whenever a spell or ability is put onto the stack,
     * if it has a single target, reselect its target at random."
     *
     * Matches engine events: SpellCastEvent, AbilityActivatedEvent, AbilityTriggeredEvent.
     */
    @SerialName("SpellOrAbilityOnStackEvent")
    @Serializable
    data object SpellOrAbilityOnStackEvent : EventPattern {
        override val description: String = "a spell or ability is put onto the stack"
    }

    /**
     * When a player activates an activated ability.
     *
     * By default this matches "activates an ability that isn't a mana ability": mana abilities
     * resolve without using the stack (CR 605.3), so the engine emits its `AbilityActivatedEvent`
     * for non-mana activated abilities — including planeswalker loyalty abilities (CR 606), which
     * are activated abilities. [player] scopes whose activations count ([Player.EachOpponent] for
     * "an opponent activates …", [Player.You] for your own, etc.).
     *
     * Used by Flamescroll Celebrant: "Whenever an opponent activates an ability that isn't a mana
     * ability, this creature deals 1 damage to that player."
     *
     * [targetMatch] optionally narrows the trigger to abilities that target a particular kind of
     * object or player. When non-null, the activated ability must have at least one chosen target
     * satisfying it — a non-targeting ability never fires. Ertha Jo, Frontier Mentor uses
     * [com.wingedsheep.sdk.scripting.events.AbilityTargetMatch.CreatureOrPlayer] for
     * "Whenever you activate an ability that targets a creature or player".
     *
     * [sourceFilter] optionally restricts which permanent the activated ability must belong to
     * (e.g. [GameObjectFilter.Artifact], or `Artifact.opponentControls()`). Null = any source.
     *
     * [requireNoTapInCost] switches the trigger from the "isn't a mana ability" semantic to the
     * Antiquities "without {T} in its activation cost" semantic (Haunting Wind, Powerleech,
     * Artifact Possession). When true, the ability matches iff its activation cost does **not**
     * include the {T} symbol — and mana abilities without {T} *do* count, unlike the default
     * semantic. The engine emits an `AbilityActivatedEvent` for every activated ability whose cost
     * lacks {T} (mana or not); this flag is what tells the matcher to accept the mana-ability ones.
     *
     * [requireExhaust] narrows to exhaust abilities (CR 702.177). On its own it is the plain
     * Aetherdrift wording — "Whenever you activate an exhaust ability" (Adrenaline Jockey, Rangers'
     * Refueler) — which counts an exhaust *mana* ability too. [excludeManaAbilities] adds back the
     * "isn't a mana ability" clause for Pit Automaton, whose Oracle text was updated to
     * "an exhaust ability that isn't a mana ability" so its copy payoff can't grab a mana ability.
     */
    @SerialName("AbilityActivatedEvent")
    @Serializable
    data class AbilityActivatedEvent(
        val player: Player = Player.You,
        val targetMatch: com.wingedsheep.sdk.scripting.events.AbilityTargetMatch? = null,
        val sourceFilter: GameObjectFilter? = null,
        val requireNoTapInCost: Boolean = false,
        val requireExhaust: Boolean = false,
        val excludeManaAbilities: Boolean = false,
    ) : EventPattern {
        override val description: String = buildString {
            append(player.description)
            append(" activates ")
            if (sourceFilter != null) {
                append("a ")
                append(sourceFilter.description)
                append("'s ability that ")
            } else {
                append("an ability that ")
            }
            when {
                targetMatch != null -> {
                    append("targets a ")
                    append(targetMatch.description)
                }
                requireExhaust && excludeManaAbilities ->
                    append("is an exhaust ability that isn't a mana ability")
                requireExhaust -> append("is an exhaust ability")
                requireNoTapInCost -> append("doesn't have {T} in its activation cost")
                else -> append("isn't a mana ability")
            }
        }

        override fun applyTextReplacement(replacer: TextReplacer): EventPattern {
            val newFilter = sourceFilter?.applyTextReplacement(replacer)
            return if (newFilter !== sourceFilter) copy(sourceFilter = newFilter) else this
        }
    }

    /**
     * When a triggered ability is put onto the stack (CR 603.3), scoped by whose ability it is and
     * — optionally — by what caused it to trigger.
     *
     * [player] scopes whose triggered ability counts (its controller): [Player.You] for your own,
     * [Player.EachOpponent] for an opponent's, etc.
     *
     * [requireAttackCause] narrows to the Firebender Ascension clause "a creature you control
     * attacking causes a triggered ability of that creature to trigger": the ability must be a
     * per-attacker "whenever this creature attacks" ability (a SELF-bound [AttackEvent]) whose own
     * source creature was just declared as an attacker. The engine stamps `causedByAttack` on the
     * emitted `AbilityTriggeredEvent` when it puts such an ability on the stack; a non-attack
     * trigger (an ETB, a dies, a "deals combat damage") never matches. The triggering ability is
     * exposed as the triggering entity, so a copy effect can target it via
     * [com.wingedsheep.sdk.scripting.targets.EffectTarget.TriggeringEntity].
     *
     * [sourceFilter] optionally restricts which permanent the triggered ability must belong to.
     * Null = any source (the [player] scope alone applies).
     */
    @SerialName("AbilityTriggeredEvent")
    @Serializable
    data class AbilityTriggeredEvent(
        val player: Player = Player.You,
        val requireAttackCause: Boolean = false,
        val sourceFilter: GameObjectFilter? = null
    ) : EventPattern {
        override val description: String = buildString {
            if (requireAttackCause) {
                append("a creature ")
                append(player.description)
                append(" control attacking causes a triggered ability of that creature to trigger")
            } else {
                append(player.description)
                append(" put a triggered ability ")
                if (sourceFilter != null) {
                    append("of a ")
                    append(sourceFilter.description)
                    append(" ")
                }
                append("onto the stack")
            }
        }

        override fun applyTextReplacement(replacer: TextReplacer): EventPattern {
            val newFilter = sourceFilter?.applyTextReplacement(replacer)
            return if (newFilter !== sourceFilter) copy(sourceFilter = newFilter) else this
        }
    }

    /**
     * When a card is revealed from the first draw of a turn.
     * Triggered by the RevealFirstDrawEachTurn static ability.
     *
     * [cardFilter] restricts what type of card triggers this:
     * - null = any card revealed triggers this
     * - GameObjectFilter.Creature = only creature cards trigger this
     *
     * Used for Primitive Etchings: "Whenever you reveal a creature card this way, draw a card."
     */
    @SerialName("CardRevealedFromDrawEvent")
    @Serializable
    data class CardRevealedFromDrawEvent(
        val cardFilter: GameObjectFilter? = null
    ) : EventPattern {
        override val description: String = buildString {
            append("you reveal ")
            if (cardFilter != null) {
                append(describeObjectForEvent(cardFilter))
            } else {
                append("a card")
            }
            append(" this way")
        }

        override fun applyTextReplacement(replacer: TextReplacer): EventPattern {
            val f = cardFilter ?: return this
            val newFilter = f.applyTextReplacement(replacer)
            return if (newFilter !== f) copy(cardFilter = newFilter) else this
        }
    }

    /** Whenever this creature crews a Vehicle. */
    @SerialName("CrewsEvent")
    @Serializable
    data object CrewsEvent : EventPattern {
        override val description: String = "this creature crews a Vehicle"
    }

    /** Whenever this creature saddles a Mount. */
    @SerialName("SaddlesEvent")
    @Serializable
    data object SaddlesEvent : EventPattern {
        override val description: String = "this creature saddles a Mount"
    }

    // =========================================================================
    // Batched Zone Change Events (Triggers)
    // =========================================================================

    /**
     * Whenever one or more cards matching [filter] are put into your graveyard from your library.
     *
     * This is a **batching trigger** — it fires at most once per event batch, regardless of
     * how many matching cards were moved. Used by Sidisi, Brood Tyrant and similar cards.
     *
     * Detection is handled specially by TriggerDetector: after processing individual events,
     * it groups all library-to-graveyard ZoneChangeEvents, checks if any match the filter,
     * and fires the trigger once per qualifying controller.
     *
     * Examples:
     * - "Whenever one or more creature cards are put into your graveyard from your library"
     *   → CardsPutIntoGraveyardFromLibraryEvent(filter = GameObjectFilter.Creature)
     */
    @SerialName("CardsPutIntoGraveyardFromLibraryEvent")
    @Serializable
    data class CardsPutIntoGraveyardFromLibraryEvent(
        val filter: GameObjectFilter = GameObjectFilter.Any
    ) : EventPattern {
        override val description: String = buildString {
            append("one or more ")
            if (filter != GameObjectFilter.Any) {
                append(filter.cardPredicates.joinToString(" ") { it.description })
                append(" ")
            }
            append("cards are put into your graveyard from your library")
        }

        override fun applyTextReplacement(replacer: TextReplacer): EventPattern {
            val newFilter = filter.applyTextReplacement(replacer)
            return if (newFilter !== filter) copy(filter = newFilter) else this
        }
    }

    /**
     * Whenever one or more cards matching [filter] are put into your graveyard from anywhere.
     *
     * This is a **batching trigger** — it fires at most once per event batch, regardless of
     * how many matching cards entered the graveyard, and regardless of which zone they came
     * from. Used by Moonshadow and similar cards.
     *
     * Detection is handled specially by TriggerDetector: after processing individual events,
     * it groups all to-graveyard ZoneChangeEvents by owner, checks if any match the filter,
     * and fires the trigger once per qualifying controller.
     *
     * Examples:
     * - "Whenever one or more permanent cards are put into your graveyard from anywhere"
     *   → CardsPutIntoYourGraveyardEvent(filter = GameObjectFilter.Permanent)
     */
    @SerialName("CardsPutIntoYourGraveyardEvent")
    @Serializable
    data class CardsPutIntoYourGraveyardEvent(
        val filter: GameObjectFilter = GameObjectFilter.Any
    ) : EventPattern {
        override val description: String = buildString {
            append("one or more ")
            if (filter != GameObjectFilter.Any) {
                append(filter.cardPredicates.joinToString(" ") { it.description })
                append(" ")
            }
            append("cards are put into your graveyard from anywhere")
        }

        override fun applyTextReplacement(replacer: TextReplacer): EventPattern {
            val newFilter = filter.applyTextReplacement(replacer)
            return if (newFilter !== filter) copy(filter = newFilter) else this
        }
    }

    /**
     * Whenever one or more cards matching [filter] leave your graveyard.
     *
     * This is a **batching trigger** — it fires at most once per event batch, regardless of
     * how many matching cards left the graveyard. "Leave your graveyard" covers any move out
     * of the graveyard: cast/played from it, exiled (e.g. Renew, Delve, Escape), reanimated to
     * the battlefield, returned to hand or library, etc.
     *
     * Detection is handled specially by TriggerDetector: after processing individual events,
     * it groups all from-graveyard zone changes by the owner of that graveyard and fires the
     * trigger at most once per qualifying controller.
     *
     * The common "during your turn" timing restriction is expressed on the card via
     * `triggerRestriction = Conditions.IsYourTurn` rather than baked into the event, and the
     * "this ability triggers only once each turn" restriction via `oncePerTurn = true`.
     *
     * Examples:
     * - "Whenever one or more cards leave your graveyard during your turn, …"
     *   → CardsLeftYourGraveyardEvent() + triggerRestriction = Conditions.IsYourTurn
     *   (Attuned Hunter, Kishla Skimmer, Kheru Goldkeeper)
     */
    @SerialName("CardsLeftYourGraveyardEvent")
    @Serializable
    data class CardsLeftYourGraveyardEvent(
        val filter: GameObjectFilter = GameObjectFilter.Any
    ) : EventPattern {
        override val description: String = buildString {
            append("one or more ")
            if (filter != GameObjectFilter.Any) {
                append(filter.cardPredicates.joinToString(" ") { it.description })
                append(" ")
            }
            append("cards leave your graveyard")
        }

        override fun applyTextReplacement(replacer: TextReplacer): EventPattern {
            val newFilter = filter.applyTextReplacement(replacer)
            return if (newFilter !== filter) copy(filter = newFilter) else this
        }
    }

    // =========================================================================
    // Exile Batching Triggers
    // =========================================================================

    /**
     * Whenever one or more cards matching [filter] are put into exile from any of [fromZones].
     *
     * This is a **batching trigger** (CR 603.2c) — it fires at most once per event batch,
     * regardless of how many cards were exiled or which of the watched zones each came from.
     * Unlike [CardsPutIntoYourGraveyardEvent] / [CardsLeftYourGraveyardEvent] it is **not** scoped
     * to a single player's zone: "from graveyards and/or the battlefield" means *any* graveyard and
     * *any* player's permanents, so every controller with this trigger sees the same batch.
     *
     * Only cards fire it — tokens are not cards (CR 111.6), so a token exiled from the battlefield
     * never satisfies it even though it briefly occupies the exile zone before CR 111.7 sweeps it.
     *
     * The common "during your turn" timing restriction is expressed on the card via
     * `triggerRestriction = Conditions.IsYourTurn` rather than baked into the event.
     *
     * Examples:
     * - "Whenever one or more cards are put into exile from graveyards and/or the battlefield
     *   during your turn, …" → `CardsPutIntoExileEvent()` + `triggerRestriction = Conditions.IsYourTurn`
     *   (Ketramose, the New Dawn)
     */
    @SerialName("CardsPutIntoExileEvent")
    @Serializable
    data class CardsPutIntoExileEvent(
        val fromZones: Set<Zone> = setOf(Zone.GRAVEYARD, Zone.BATTLEFIELD),
        val filter: GameObjectFilter = GameObjectFilter.Any
    ) : EventPattern {
        override val description: String = buildString {
            append("one or more ")
            if (filter != GameObjectFilter.Any) {
                append(filter.cardPredicates.joinToString(" ") { it.description })
                append(" ")
            }
            append("cards are put into exile from ")
            append(
                fromZones.sortedBy { it.ordinal }.joinToString(" and/or ") { zone ->
                    when (zone) {
                        Zone.GRAVEYARD -> "graveyards"
                        Zone.BATTLEFIELD -> "the battlefield"
                        Zone.HAND -> "hands"
                        Zone.LIBRARY -> "libraries"
                        Zone.STACK -> "the stack"
                        else -> zone.displayName
                    }
                }
            )
        }

        override fun applyTextReplacement(replacer: TextReplacer): EventPattern {
            val newFilter = filter.applyTextReplacement(replacer)
            return if (newFilter !== filter) copy(filter = newFilter) else this
        }
    }

    // =========================================================================
    // Sacrifice Triggers
    // =========================================================================

    /**
     * Whenever you sacrifice one or more permanents matching a filter.
     *
     * Two multiplicity shapes, selected by [perPermanent] — the templating axis matters (CR 603.2c:
     * "an ability… can trigger repeatedly if one event contains multiple occurrences"):
     *  - [perPermanent] = false (default): the *batch* template "Whenever you sacrifice **one or
     *    more** permanents" — fires at most once per event batch regardless of how many permanents
     *    were sacrificed (Scavenger's Talent, Zodiark).
     *  - [perPermanent] = true: the *per-permanent* template "Whenever you sacrifice **a/another**
     *    permanent" — fires once for EACH matching permanent sacrificed, even when several are
     *    sacrificed simultaneously (Mazirek, Savra, Zhao). Combine with [TriggerBinding.OTHER] for
     *    "another" (excludes the source) or [TriggerBinding.ANY] for "a" (includes the source).
     *
     * [sacrificedBy] scopes *whose* sacrifices are watched, independently of who controls the
     * source. Only three values are meaningful, and the detector reads exactly these:
     *  - [Player.You] (default): the controller's own sacrifices ("Whenever *you* sacrifice…").
     *  - [Player.Each]: every player's ("Whenever *a player* sacrifices…", Zodiark, Umbral God).
     *  - [Player.EachOpponent]: only the controller's opponents' ("Whenever *an opponent*
     *    sacrifices…", Vengeful Tracker).
     *
     * For the two multi-player scopes the trigger fires once per watched player (batch) or once per
     * that player's matching permanent (per-permanent). The sacrificing player is bound as the
     * trigger's [Player.TriggeringPlayer], so payoffs can hit "them" specifically.
     *
     * Examples:
     *   → PermanentsSacrificedEvent(filter = GameObjectFilter.Food)
     *     "Whenever you sacrifice one or more Foods"
     *   → PermanentsSacrificedEvent(perPermanent = true)  // with OTHER binding
     *     "Whenever you sacrifice another permanent"
     *   → PermanentsSacrificedEvent(filter = GameObjectFilter.Creature, sacrificedBy = Player.Each)
     *     "Whenever a player sacrifices one or more creatures"
     *   → PermanentsSacrificedEvent(filter = GameObjectFilter.Artifact, perPermanent = true,
     *                               sacrificedBy = Player.EachOpponent)
     *     "Whenever an opponent sacrifices an artifact"
     */
    @SerialName("PermanentsSacrificedEvent")
    @Serializable
    data class PermanentsSacrificedEvent(
        val filter: GameObjectFilter = GameObjectFilter.Any,
        val sacrificedBy: Player = Player.You,
        val perPermanent: Boolean = false
    ) : EventPattern {
        override val description: String = buildString {
            val who = when (sacrificedBy) {
                Player.Each -> "a player sacrifices "
                Player.EachOpponent -> "an opponent sacrifices "
                else -> "you sacrifice "
            }
            append(who)
            // "another" is relative to the source permanent, so it only reads right when the
            // sacrificing player is the source's own controller; other scopes take a plain article.
            val article = if (sacrificedBy == Player.You) "another " else "a "
            append(if (perPermanent) article else "one or more ")
            if (filter != GameObjectFilter.Any) {
                append(filter.cardPredicates.joinToString(" ") { it.description })
                if (!perPermanent) append("s")
            } else {
                append(if (perPermanent) "permanent" else "permanents")
            }
        }

        override fun applyTextReplacement(replacer: TextReplacer): EventPattern {
            val newFilter = filter.applyTextReplacement(replacer)
            return if (newFilter !== filter) copy(filter = newFilter) else this
        }
    }

    /**
     * Whenever a creature exploits a creature (CR 702.110b) — a creature "exploits a creature"
     * when the controller of its exploit ability sacrifices a creature as that ability resolves.
     * Fires once per exploit (one reflexive resolution ⇒ at most one sacrifice ⇒ one event);
     * declining the optional sacrifice produces no event.
     *
     * The *exploiter* identity is selected by the ability's [TriggerBinding] against the event's
     * exploiter, exactly like other per-object triggers:
     *  - [TriggerBinding.SELF] — "when **this** creature exploits a creature" (Stitched Assistant,
     *    Fell Stinger). Note these cards bake their self-payoff into the exploit reflexive itself so
     *    it survives self-sacrifice; the SELF-bound pattern exists for the general case.
     *  - [TriggerBinding.ANY] — "whenever **a creature you control** exploits a creature" (Skull
     *    Skaab); includes the source's own exploit.
     *  - [TriggerBinding.OTHER] — "whenever **another** creature you control exploits a creature".
     *
     * [player] scopes the exploiter's *controller* (default [Player.You] = "a creature you control
     * exploits …"). [requireNontokenExploited] gates on the sacrificed creature being a **nontoken**
     * (Skull Skaab's "exploits a nontoken creature"). This is a boolean rather than a full
     * [GameObjectFilter] because the exploited creature is gone by the time the event is observed —
     * only its last-known token-ness is available (mirrors [RingTemptedEvent.requireBearerChosen]).
     */
    @SerialName("ExploitedEvent")
    @Serializable
    data class ExploitedEvent(
        val player: Player = Player.You,
        val requireNontokenExploited: Boolean = false
    ) : EventPattern {
        override val description: String = buildString {
            val who = if (player == Player.You) "a creature you control" else "a creature"
            append(who)
            append(" exploits ")
            append(if (requireNontokenExploited) "a nontoken creature" else "a creature")
        }
    }

    /**
     * When a creature trains (CR 702.149c) — "'When this creature trains' means 'When a resolving
     * training ability puts one or more +1/+1 counters on this creature.'" The training payoff of
     * Savior of Ollenbock ("Whenever this creature trains, exile up to one other target creature …").
     *
     * Emitted by the training ability's own resolution (the tail of the `training()` composite,
     * `com.wingedsheep.sdk.dsl.training`), **only when the +1/+1 counter actually lands** — a
     * Solemnity-type "can't have counters put on it" prohibition means the ability trains nothing
     * and no event fires, faithful to "puts one or more +1/+1 counters." Distinct from the raw
     * [CountersPlacedEvent]: that fires for a +1/+1 counter from *any* source; this fires only for
     * the counter a *resolving training ability* placed (the Option A distinction — see
     * `EmitTrainedEventEffect`).
     *
     * A parameterless [EventPattern] whose subject is selected by the ability's [TriggerBinding]
     * against the trained creature, exactly like [CountersPlacedEvent] / [ExploitedEvent]:
     *  - [com.wingedsheep.sdk.scripting.TriggerBinding.SELF] — "when **this** creature trains"
     *    (Savior of Ollenbock). The trained creature is the ability's own source.
     *  - [com.wingedsheep.sdk.scripting.TriggerBinding.OTHER] — "when **another** creature you
     *    control trains" (none printed yet; supported for the next card).
     *  - [com.wingedsheep.sdk.scripting.TriggerBinding.ANY] — no subject restriction.
     *
     * CR 702.149c defines only the SELF form, so this carries no player field; a controller-scoped
     * "a creature you control trains" variant would add one when a card needs it (mirroring the
     * deliberately-narrow [BecomesUnblockedEvent]).
     */
    @SerialName("TrainedEvent")
    @Serializable
    data object TrainedEvent : EventPattern {
        override val description: String = "this creature trains"
    }

    // =========================================================================
    // Combat Damage Batch Triggers
    // =========================================================================

    /**
     * Whenever one or more creatures matching [sourceFilter] you control deal combat damage
     * to a player. Batching trigger — fires at most once per event batch regardless of how
     * many matching creatures connected.
     *
     * Examples:
     *   → OneOrMoreDealCombatDamageToPlayerEvent(sourceFilter = GameObjectFilter.Creature.withSubtype("Bird"))
     *     "Whenever one or more Birds you control deal combat damage to a player"
     */
    @SerialName("OneOrMoreDealCombatDamageToPlayerEvent")
    @Serializable
    data class OneOrMoreDealCombatDamageToPlayerEvent(
        val sourceFilter: GameObjectFilter = GameObjectFilter.Companion.Creature
    ) : EventPattern {
        override val description: String = buildString {
            append("one or more ")
            append(describeObjectForEvent(sourceFilter))
            append(" you control deal combat damage to a player")
        }

        override fun applyTextReplacement(replacer: TextReplacer): EventPattern {
            val newFilter = sourceFilter.applyTextReplacement(replacer)
            return if (newFilter !== sourceFilter) copy(sourceFilter = newFilter) else this
        }
    }

    /**
     * Whenever one or more creatures matching [sourceFilter] deal combat damage to *you* (the
     * trigger's controller). Defensive batching counterpart of
     * [OneOrMoreDealCombatDamageToPlayerEvent] — fires at most once per combat-damage batch
     * regardless of how many creatures connected with you.
     *
     * Examples:
     *   → OneOrMoreDealCombatDamageToYouEvent()
     *     "Whenever one or more creatures deal combat damage to you" (Witch-king of Angmar)
     */
    @SerialName("OneOrMoreDealCombatDamageToYouEvent")
    @Serializable
    data class OneOrMoreDealCombatDamageToYouEvent(
        val sourceFilter: GameObjectFilter = GameObjectFilter.Companion.Creature
    ) : EventPattern {
        override val description: String = buildString {
            append("one or more ")
            append(describeObjectForEvent(sourceFilter))
            append(" deal combat damage to you")
        }

        override fun applyTextReplacement(replacer: TextReplacer): EventPattern {
            val newFilter = sourceFilter.applyTextReplacement(replacer)
            return if (newFilter !== sourceFilter) copy(sourceFilter = newFilter) else this
        }
    }

    // =========================================================================
    // Leave Battlefield Without Dying Batch Triggers
    // =========================================================================

    /**
     * Whenever one or more creatures you control leave the battlefield without dying.
     * Batching trigger — fires at most once per event batch regardless of how many
     * creatures left. "Without dying" means moving to any zone other than graveyard
     * (e.g., exiled, returned to hand, put on library).
     *
     * Examples:
     *   → LeaveBattlefieldWithoutDyingEvent()
     *     "Whenever one or more creatures you control leave the battlefield without dying"
     *   → LeaveBattlefieldWithoutDyingEvent(filter = GameObjectFilter.Creature)
     *     Same, with explicit creature filter
     */
    @SerialName("LeaveBattlefieldWithoutDyingEvent")
    @Serializable
    data class LeaveBattlefieldWithoutDyingEvent(
        val filter: GameObjectFilter = GameObjectFilter.Companion.Creature,
        val excludeSelf: Boolean = false
    ) : EventPattern {
        override val description: String = buildString {
            append("one or more ")
            if (excludeSelf) append("other ")
            append(describeObjectForEvent(filter))
            append(" you control leave the battlefield without dying")
        }

        override fun applyTextReplacement(replacer: TextReplacer): EventPattern {
            val newFilter = filter.applyTextReplacement(replacer)
            return if (newFilter !== filter) copy(filter = newFilter) else this
        }
    }

    // =========================================================================
    // Death Batch Triggers
    // =========================================================================

    /**
     * Whenever one or more creatures you control die.
     *
     * "Die" means a creature is put into a graveyard from the battlefield (CR 700.4).
     * This is a **batching trigger** — it fires at most once per event batch, regardless
     * of how many matching creatures died simultaneously. A board wipe that destroys
     * several of your creatures at once fires this once, not once per creature — the key
     * difference from the per-creature [ZoneChangeEvent] death shape (one event each),
     * which over-counts on mass removal.
     *
     * [excludeSelf] models the "other" in "one or more *other* creatures you control die":
     * the trigger's own source death does not count toward the batch.
     *
     * Detection is handled specially by TriggerDetector: after processing individual events,
     * it groups battlefield→graveyard zone changes by each creature's last-known controller,
     * checks the creature filter, and fires the trigger at most once per qualifying controller.
     *
     * Examples:
     * - "Whenever one or more other creatures you control die, put a +1/+1 counter on this creature."
     *   → CreaturesYouControlDiedEvent(excludeSelf = true)   (Vengeful Townsfolk)
     */
    @SerialName("CreaturesYouControlDiedEvent")
    @Serializable
    data class CreaturesYouControlDiedEvent(
        val filter: GameObjectFilter = GameObjectFilter.Companion.Creature,
        val excludeSelf: Boolean = false
    ) : EventPattern {
        override val description: String = buildString {
            append("one or more ")
            if (excludeSelf) append("other ")
            append(describeObjectForEvent(filter))
            // The filter's controller predicate scopes the trigger (you control / an opponent
            // controls); a null predicate keeps the historical "you control" wording.
            append(
                when (filter.controllerPredicate) {
                    com.wingedsheep.sdk.scripting.predicates.ControllerPredicate.ControlledByOpponent ->
                        " an opponent controls die"
                    com.wingedsheep.sdk.scripting.predicates.ControllerPredicate.ControlledByAny ->
                        " die"
                    else -> " you control die"
                }
            )
        }

        override fun applyTextReplacement(replacer: TextReplacer): EventPattern {
            val newFilter = filter.applyTextReplacement(replacer)
            return if (newFilter !== filter) copy(filter = newFilter) else this
        }
    }

    // =========================================================================
    // Enter Battlefield Batch Triggers
    // =========================================================================

    /**
     * Whenever one or more permanents matching a filter you control enter the battlefield.
     * Batching trigger — fires at most once per event batch regardless of how many
     * permanents entered.
     *
     * Examples:
     *   → PermanentsEnteredEvent(filter = GameObjectFilter.Noncreature and GameObjectFilter.Nonland)
     *     "Whenever one or more noncreature, nonland permanents you control enter"
     *   → PermanentsEnteredEvent()
     *     "Whenever one or more permanents you control enter"
     */
    @SerialName("PermanentsEnteredEvent")
    @Serializable
    data class PermanentsEnteredEvent(
        val filter: GameObjectFilter = GameObjectFilter.Any,
        /**
         * "One or more OTHER …" — the trigger's own source never counts toward the batch,
         * so the ability doesn't fire off its source's own entry (Valley Questcaller).
         * Contrast Satoru, the Infiltrator ("Satoru and/or one or more other creatures"),
         * which deliberately counts itself and leaves this false.
         */
        val excludeSource: Boolean = false
    ) : EventPattern {
        override val description: String = buildString {
            append("one or more ")
            if (excludeSource) append("other ")
            append(describeObjectForEvent(filter))
            append(" you control enter the battlefield")
        }

        override fun applyTextReplacement(replacer: TextReplacer): EventPattern {
            val newFilter = filter.applyTextReplacement(replacer)
            return if (newFilter !== filter) copy(filter = newFilter) else this
        }
    }

    // =========================================================================
    // Saga Chapter Resolution
    // =========================================================================

    /**
     * Whenever a Saga chapter ability resolves. With [finalChapterOnly] = true (the default),
     * only the Saga's *final* chapter ability matches — "Whenever the final chapter ability of a
     * Saga you control resolves" (Tom Bombadil). With it false, any chapter ability matches.
     *
     * The Saga must be controlled by the trigger source's controller ([player] = Player.You).
     * Saga chapter abilities are detected from lore-counter additions and put on the stack by the
     * engine; when one resolves it emits a SagaChapterResolvedEvent that this pattern matches.
     *
     * Pair with `oncePerTurn = true` on the triggered ability for "This ability triggers only once
     * each turn."
     */
    @SerialName("SagaChapterResolvedEvent")
    @Serializable
    data class SagaChapterResolvedEvent(
        val player: Player = Player.You,
        val finalChapterOnly: Boolean = true
    ) : EventPattern {
        override val description: String = buildString {
            append("the ")
            if (finalChapterOnly) append("final chapter ") else append("chapter ")
            append("ability of a Saga ")
            append(if (player == Player.You) "you control" else "${player.description} controls")
            append(" resolves")
        }
    }
}

/**
 * Builds a natural-language description of a [GameObjectFilter] for use in
 * event descriptions. Adds an article and places controller after the noun.
 *
 * Examples:
 *   GameObjectFilter.Any                        -> "a card or permanent"
 *   GameObjectFilter.Creature                   -> "a creature"
 *   GameObjectFilter.Creature.youControl()      -> "a creature you control"
 *   GameObjectFilter.NonlandPermanent            -> "a nonland permanent"
 *   GameObjectFilter.Token                       -> "a token"
 */
internal fun describeObjectForEvent(filter: GameObjectFilter): String {
    val baseParts = buildString {
        filter.statePredicates.forEach { append(it.description); append(" ") }
        filter.cardPredicates.forEach { append(it.description); append(" ") }
    }.trim()

    val controllerSuffix = filter.controllerPredicate
        ?.description
        ?.takeIf { it.isNotEmpty() }
        ?.let { " $it" }
        ?: ""

    if (baseParts.isEmpty()) {
        return "a card or permanent$controllerSuffix"
    }

    val article = if (baseParts.first().lowercaseChar() in "aeiou") "an" else "a"

    return "$article $baseParts$controllerSuffix"
}

/**
 * Head nouns whose MTG plural the regular rules in [pluralizeHeadNoun] get wrong. Curated from the
 * `Subtype` catalog rather than derived: English pluralization can't be inferred from spelling
 * (`Rhino` → `Rhinos` but `Hero` → `Heroes`; `Fox` → `Foxes` but `Fish` → `Fish`), and a wrong
 * guess ships as player-visible text.
 *
 * **Only entries that differ from what the regular rules already produce are listed** — `Cyclops`,
 * `Fungus`, `Pegasus` and `Plains` are absent because the "already ends in -s" rule leaves them
 * alone, which is right for all four. Subtypes WotC writes as invariant plurals (`Fish`, `Merfolk`,
 * `Myr`, `Eldrazi`, …) map to themselves.
 *
 * This list is deliberately incomplete: it covers the head nouns reachable from today's `Subtype`
 * catalog. Extend it when a new wording needs one, and add the case to
 * `DescribeObjectsForEventTest`.
 */
private val IRREGULAR_PLURAL_HEAD_NOUNS: Map<String, String> = mapOf(
    // -f / -fe → -ves
    "Elf" to "Elves",
    "Wolf" to "Wolves",
    "Werewolf" to "Werewolves",
    "Dwarf" to "Dwarves",
    // genuinely irregular
    "Hero" to "Heroes",
    "Mouse" to "Mice",
    "Class" to "Classes",
    "Homunculus" to "Homunculi",
    "Octopus" to "Octopuses",
    // invariant plurals
    "Fish" to "Fish",
    "Jellyfish" to "Jellyfish",
    "Merfolk" to "Merfolk",
    "Treefolk" to "Treefolk",
    "Moonfolk" to "Moonfolk",
    "Kithkin" to "Kithkin",
    "Kor" to "Kor",
    "Myr" to "Myr",
    "Eldrazi" to "Eldrazi",
    "Kavu" to "Kavu",
    "Kree" to "Kree",
    "Efreet" to "Efreet",
    "Djinn" to "Djinn",
)

/**
 * The plural of a single head noun — [IRREGULAR_PLURAL_HEAD_NOUNS] first, then the regular English
 * rules. Split out from [describeObjectsForEvent] so it can be tested directly against the subtype
 * catalog.
 */
internal fun pluralizeHeadNoun(head: String): String {
    IRREGULAR_PLURAL_HEAD_NOUNS[head]?.let { return it }
    return when {
        // Already plural ("creatures", "permanents") or an invariant -s noun ("Plains").
        head.endsWith("s", ignoreCase = true) -> head
        head.endsWith("ch", ignoreCase = true) || head.endsWith("sh", ignoreCase = true) ||
            head.endsWith("x", ignoreCase = true) || head.endsWith("z", ignoreCase = true) -> "${head}es"
        else -> "${head}s"
    }
}

/**
 * The plural counterpart of [describeObjectForEvent], for the "one or more <things>" batch event
 * wordings: no article, head noun pluralized, controller still a suffix rather than the prefix
 * `GameObjectFilter.description` renders it as.
 *
 * Examples (each prefixed with "one or more " by the caller):
 *   GameObjectFilter.Creature                                 -> "creatures"
 *   GameObjectFilter.Creature.youControl()                    -> "creatures you control"
 *   GameObjectFilter.Creature.youControl().withSubtype(HERO)  -> "creature Heroes you control"
 *   GameObjectFilter.Creature.youControl().withSubtype(ELF)   -> "creature Elves you control"
 *
 * Note the word order in the last two: predicates render in the order they were added to the
 * filter, and `withSubtype` *appends*, so the subtype trails the type ("creature Heroes", not
 * "Hero creatures"). That is [describeObjectForEvent]'s existing convention — it renders the
 * singular as "a creature Hero you control" — and is kept here on purpose so the two describers
 * agree; changing it would move generated text for the whole corpus, not just batch triggers.
 * It also means the head noun is the **subtype** whenever there is one, which is why
 * [pluralizeHeadNoun] needs the irregular table.
 */
internal fun describeObjectsForEvent(filter: GameObjectFilter): String {
    val baseParts = buildString {
        filter.statePredicates.forEach { append(it.description); append(" ") }
        filter.cardPredicates.forEach { append(it.description); append(" ") }
    }.trim()

    val controllerSuffix = filter.controllerPredicate
        ?.description
        ?.takeIf { it.isNotEmpty() }
        ?.let { " $it" }
        ?: ""

    if (baseParts.isEmpty()) {
        return "cards or permanents$controllerSuffix"
    }

    // Only the head noun (the last word) pluralizes: "creature Hero" -> "creature Heroes".
    val head = baseParts.substringAfterLast(' ')
    val plural = pluralizeHeadNoun(head)
    val pluralized = if (baseParts.contains(' ')) {
        "${baseParts.substringBeforeLast(' ')} $plural"
    } else {
        plural
    }

    return "$pluralized$controllerSuffix"
}
