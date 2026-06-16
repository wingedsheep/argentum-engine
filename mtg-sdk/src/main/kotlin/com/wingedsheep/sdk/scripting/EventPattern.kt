package com.wingedsheep.sdk.scripting

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.events.AmountFilter
import com.wingedsheep.sdk.scripting.events.ControllerFilter
import com.wingedsheep.sdk.scripting.events.CounterTypeFilter
import com.wingedsheep.sdk.scripting.events.DamageType
import com.wingedsheep.sdk.scripting.events.AttackPredicate
import com.wingedsheep.sdk.scripting.events.RecipientFilter
import com.wingedsheep.sdk.scripting.events.SourceFilter
import com.wingedsheep.sdk.scripting.events.SpellCastPredicate
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.text.TextReplaceable
import com.wingedsheep.sdk.scripting.text.TextReplacer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

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
        val excludeTo: Zone? = null
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
                }
                if (from != null && to != Zone.GRAVEYARD) {
                    append(" from ${from.displayName}")
                }
            } else if (from != null) {
                append(" would leave ${from.displayName}")
            }
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
        val exceptFirstInDrawStep: Boolean = false
    ) : EventPattern {
        override val description: String = buildString {
            append(player.description)
            append(" would draw a card")
            if (exceptFirstInDrawStep) append(" (except the first each draw step)")
        }
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
     */
    @SerialName("LifeGainEvent")
    @Serializable
    data class LifeGainEvent(
        val player: Player = Player.You
    ) : EventPattern {
        override val description: String = "${player.description} would gain life"
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
     * Whenever the Ring tempts a player (CR 701.54d). Fires after the temptee completes
     * the "the Ring tempts you" action, even if some or all of it was impossible.
     * Used by cards with "Whenever the Ring tempts you, ...".
     */
    @SerialName("RingTemptedEvent")
    @Serializable
    data class RingTemptedEvent(
        val player: Player = Player.You
    ) : EventPattern {
        override val description: String = "the Ring tempts ${player.description}"
    }

    /**
     * Whenever a player scries (CR 701.18). Fires once per scry, after every card chosen
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
        val cardFilter: GameObjectFilter? = null
    ) : EventPattern {
        override val description: String = buildString {
            append(player.description)
            append(" would discard ")
            if (cardFilter != null) {
                append(cardFilter.description)
            } else {
                append("a card")
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
     * When a player would search their library.
     */
    @SerialName("SearchLibraryEvent")
    @Serializable
    data class SearchLibraryEvent(
        val player: Player = Player.You
    ) : EventPattern {
        override val description: String = "${player.description} would search a library"
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
         * [AttackPredicate.AttackerCountAtLeast].
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
     * declared against the player themself.
     */
    @SerialName("CreaturesAttackYouEvent")
    @Serializable
    data class CreaturesAttackYouEvent(
        val minAttackers: Int = 1
    ) : EventPattern {
        override val description: String = if (minAttackers <= 1) {
            "one or more creatures attack you"
        } else {
            "$minAttackers or more creatures attack you"
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
        /**
         * When true, the trigger fires only on damage that exceeded what was needed to be
         * lethal (CR 120.4a). Combined with the existing damageType / recipient / sourceFilter
         * gates — e.g. Fall of Cair Andros uses
         * `DealsDamageEvent(damageType = NonCombat, recipient = Matching(creatureOpponentControls),
         *  requireExcess = true)` and reads the excess via
         * `ContextPropertyKey.TRIGGER_EXCESS_DAMAGE_AMOUNT`.
         */
        val requireExcess: Boolean = false
    ) : EventPattern {
        override val description: String = buildString {
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
     * Whenever a creature dealt damage by this permanent this turn dies.
     * Binding SELF = "whenever a creature dealt damage by Soul Collector this turn dies".
     *
     * Detection uses DamageDealtToCreaturesThisTurnComponent on the source entity
     * to check if it dealt damage to the dying creature this turn.
     */
    @SerialName("CreatureDealtDamageBySourceDiesEvent")
    @Serializable
    data object CreatureDealtDamageBySourceDiesEvent : EventPattern {
        override val description: String = "whenever a creature dealt damage by this creature this turn dies"
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
     * @param nthSpell The spell number that triggers this (e.g., 2 for "second spell")
     * @param player Which player's spell count to track
     */
    @SerialName("NthSpellCastEvent")
    @Serializable
    data class NthSpellCastEvent(
        val nthSpell: Int,
        val player: Player = Player.Each
    ) : EventPattern {
        override val description: String = buildString {
            append(player.description)
            append(" casts their ")
            append(when (nthSpell) {
                2 -> "second"
                3 -> "third"
                else -> "${nthSpell}th"
            })
            append(" spell each turn")
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
     * attach a `triggerCondition` to the triggered ability rather than encoding it here.
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
     * When a permanent (or, opt-in, a spell on the stack) becomes the target of a spell or ability.
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
     * [byYou] restricts to spells or abilities controlled by the trigger's controller.
     * [firstTimeEachTurn] restricts to the first time each turn (used by Valiant).
     */
    @SerialName("BecomesTargetEvent")
    @Serializable
    data class BecomesTargetEvent(
        val targetFilter: GameObjectFilter = GameObjectFilter.Any,
        val byYou: Boolean = false,
        val byOpponent: Boolean = false,
        val firstTimeEachTurn: Boolean = false,
        val includeSpellTargets: Boolean = false
    ) : EventPattern {
        override val description: String = buildString {
            append(describeObjectForEvent(targetFilter))
            append(" becomes the target of a spell or ability")
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
        val filter: GameObjectFilter? = null
    ) : EventPattern {
        override val description: String = buildString {
            append("a ")
            append(filter?.description ?: "permanent")
            append(" becomes tapped")
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
     */
    @SerialName("UntapEvent")
    @Serializable
    data object UntapEvent : EventPattern {
        override val description: String = "this permanent becomes untapped"
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
     */
    @SerialName("CreatureTurnedFaceUpEvent")
    @Serializable
    data class CreatureTurnedFaceUpEvent(
        val player: Player = Player.You
    ) : EventPattern {
        override val description: String = buildString {
            append("a creature ")
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
     * When control of a permanent changes.
     * Binding SELF = "when you gain control of this from another player".
     */
    @SerialName("ControlChangeEvent")
    @Serializable
    data object ControlChangeEvent : EventPattern {
        override val description: String = "control of this permanent changes"
    }

    // ---- Counter Triggers ----

    /**
     * When one or more counters of a specific type are placed on a permanent.
     *
     * Examples:
     * - "Whenever you put one or more +1/+1 counters on a creature you control"
     *   → CountersPlacedEvent(counterType = Counters.PLUS_ONE_PLUS_ONE, filter = GameObjectFilter.Creature.youControl())
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
        val firstTimeEachTurn: Boolean = false
    ) : EventPattern {
        override val description: String = buildString {
            val typeLabel = if (counterType == com.wingedsheep.sdk.core.Counters.ANY) "" else "$counterType "
            append("one or more ${typeLabel}counters are placed on ")
            append(describeObjectForEvent(filter))
            if (firstTimeEachTurn) append(" for the first time this turn")
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
     * When a player activates an activated ability that isn't a mana ability.
     *
     * Mana abilities resolve without using the stack (CR 605.3), so the engine only emits its
     * `AbilityActivatedEvent` for non-mana activated abilities — including planeswalker loyalty
     * abilities (CR 606), which are activated abilities. This template therefore matches exactly
     * "activates an ability that isn't a mana ability"; [player] scopes whose activations count
     * ([Player.EachOpponent] for "an opponent activates …", [Player.You] for your own, etc.).
     *
     * Used by Flamescroll Celebrant: "Whenever an opponent activates an ability that isn't a mana
     * ability, this creature deals 1 damage to that player."
     *
     * [targetMatch] optionally narrows the trigger to abilities that target a particular kind of
     * object or player. When non-null, the activated ability must have at least one chosen target
     * satisfying it — a non-targeting ability never fires. Ertha Jo, Frontier Mentor uses
     * [com.wingedsheep.sdk.scripting.events.AbilityTargetMatch.CreatureOrPlayer] for
     * "Whenever you activate an ability that targets a creature or player".
     */
    @SerialName("AbilityActivatedEvent")
    @Serializable
    data class AbilityActivatedEvent(
        val player: Player = Player.You,
        val targetMatch: com.wingedsheep.sdk.scripting.events.AbilityTargetMatch? = null
    ) : EventPattern {
        override val description: String = buildString {
            append(player.description)
            append(" activates an ability that ")
            if (targetMatch != null) {
                append("targets a ")
                append(targetMatch.description)
            } else {
                append("isn't a mana ability")
            }
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
     * `triggerCondition = Conditions.IsYourTurn` rather than baked into the event, and the
     * "this ability triggers only once each turn" restriction via `oncePerTurn = true`.
     *
     * Examples:
     * - "Whenever one or more cards leave your graveyard during your turn, …"
     *   → CardsLeftYourGraveyardEvent() + triggerCondition = Conditions.IsYourTurn
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
    // Sacrifice Triggers
    // =========================================================================

    /**
     * Whenever you sacrifice one or more permanents matching a filter.
     * Batching trigger — fires at most once per event batch regardless of how many
     * permanents were sacrificed.
     *
     * Examples:
     *   → PermanentsSacrificedEvent(filter = GameObjectFilter.Food)
     *     "Whenever you sacrifice one or more Foods"
     *   → PermanentsSacrificedEvent()
     *     "Whenever you sacrifice a permanent"
     */
    @SerialName("PermanentsSacrificedEvent")
    @Serializable
    data class PermanentsSacrificedEvent(
        val filter: GameObjectFilter = GameObjectFilter.Any
    ) : EventPattern {
        override val description: String = buildString {
            append("you sacrifice one or more ")
            if (filter != GameObjectFilter.Any) {
                append(filter.cardPredicates.joinToString(" ") { it.description })
                append("s")
            } else {
                append("permanents")
            }
        }

        override fun applyTextReplacement(replacer: TextReplacer): EventPattern {
            val newFilter = filter.applyTextReplacement(replacer)
            return if (newFilter !== filter) copy(filter = newFilter) else this
        }
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
            append(" you control die")
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
        val filter: GameObjectFilter = GameObjectFilter.Any
    ) : EventPattern {
        override val description: String = buildString {
            append("one or more ")
            append(describeObjectForEvent(filter))
            append(" you control enter the battlefield")
        }

        override fun applyTextReplacement(replacer: TextReplacer): EventPattern {
            val newFilter = filter.applyTextReplacement(replacer)
            return if (newFilter !== filter) copy(filter = newFilter) else this
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
