package com.wingedsheep.sdk.scripting

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.events.ControllerFilter
import com.wingedsheep.sdk.scripting.events.CounterTypeFilter
import com.wingedsheep.sdk.scripting.events.DamageType
import com.wingedsheep.sdk.scripting.events.RecipientFilter
import com.wingedsheep.sdk.scripting.events.SourceFilter
import com.wingedsheep.sdk.scripting.events.SpellTypeFilter
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
 * GameEvent.DamageEvent(
 *     recipient = RecipientFilter.CreatureYouControl,
 *     source = SourceFilter.HasColor(Color.RED),
 *     damageType = DamageType.Combat
 * )
 * ```
 *
 * Supporting filter types are organized in the events/ subdirectory:
 * - EventFilters.kt - RecipientFilter, SourceFilter, DamageType,
 *                     CounterTypeFilter, ControllerFilter, Player, SpellTypeFilter
 * - Zone.kt - Zone enumeration
 */
@Serializable
sealed interface GameEvent : TextReplaceable<GameEvent> {
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
        val damageType: DamageType = DamageType.Any
    ) : GameEvent {
        override val description: String = buildString {
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

        override fun applyTextReplacement(replacer: TextReplacer): GameEvent = this
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
    ) : GameEvent {
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

        override fun applyTextReplacement(replacer: TextReplacer): GameEvent {
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
    ) : GameEvent {
        override val description: String = buildString {
            if (counterType != CounterTypeFilter.Any) {
                append(counterType.description)
                append(" ")
            }
            append("counters would be placed on ")
            append(recipient.description)
        }

        override fun applyTextReplacement(replacer: TextReplacer): GameEvent = this
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
    ) : GameEvent {
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

        override fun applyTextReplacement(replacer: TextReplacer): GameEvent {
            val f = tokenFilter ?: return this
            val newFilter = f.applyTextReplacement(replacer)
            return if (newFilter !== f) copy(tokenFilter = newFilter) else this
        }
    }

    // =========================================================================
    // Draw Events
    // =========================================================================

    /**
     * When a player draws a card.
     *
     * Examples:
     * - "you would draw a card" → DrawEvent(player = Player.You)
     * - "an opponent would draw" → DrawEvent(player = Player.Opponent)
     */
    @SerialName("DrawEvent")
    @Serializable
    data class DrawEvent(
        val player: Player = Player.You
    ) : GameEvent {
        override val description: String = "${player.description} would draw a card"

        override fun applyTextReplacement(replacer: TextReplacer): GameEvent = this
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
    ) : GameEvent {
        override val description: String = "${player.description} would gain life"

        override fun applyTextReplacement(replacer: TextReplacer): GameEvent = this
    }

    /**
     * When a player would lose life.
     */
    @SerialName("LifeLossEvent")
    @Serializable
    data class LifeLossEvent(
        val player: Player = Player.You
    ) : GameEvent {
        override val description: String = "${player.description} would lose life"

        override fun applyTextReplacement(replacer: TextReplacer): GameEvent = this
    }

    /**
     * When a player would gain or lose life.
     * Used for cards like Moonstone Harbinger: "Whenever you gain or lose life during your turn".
     */
    @SerialName("LifeGainOrLossEvent")
    @Serializable
    data class LifeGainOrLossEvent(
        val player: Player = Player.You
    ) : GameEvent {
        override val description: String = "${player.description} would gain or lose life"

        override fun applyTextReplacement(replacer: TextReplacer): GameEvent = this
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
    ) : GameEvent {
        override val description: String = "${player.description} would take an extra turn"

        override fun applyTextReplacement(replacer: TextReplacer): GameEvent = this
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
    ) : GameEvent {
        override val description: String = buildString {
            append(player.description)
            append(" would discard ")
            if (cardFilter != null) {
                append(cardFilter.description)
            } else {
                append("a card")
            }
        }

        override fun applyTextReplacement(replacer: TextReplacer): GameEvent {
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
    ) : GameEvent {
        override val description: String = "${player.description} would search a library"

        override fun applyTextReplacement(replacer: TextReplacer): GameEvent = this
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
        val alone: Boolean = false
    ) : GameEvent {
        override val description: String = buildString {
            if (filter != null) {
                append("a ${filter.description} attacks")
            } else {
                append("a creature attacks")
            }
            if (alone) append(" alone")
        }

        override fun applyTextReplacement(replacer: TextReplacer): GameEvent {
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
    ) : GameEvent {
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

        override fun applyTextReplacement(replacer: TextReplacer): GameEvent = this
    }

    /**
     * When a creature blocks.
     * Binding SELF = "when this creature blocks".
     */
    @SerialName("BlockEvent")
    @Serializable
    data object BlockEvent : GameEvent {
        override val description: String = "a creature blocks"
        override fun applyTextReplacement(replacer: TextReplacer): GameEvent = this
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
    ) : GameEvent {
        override val description: String = buildString {
            if (filter != null) {
                append("a ${filter.description} becomes blocked")
            } else {
                append("a creature becomes blocked")
            }
        }

        override fun applyTextReplacement(replacer: TextReplacer): GameEvent {
            val f = filter ?: return this
            val newFilter = f.applyTextReplacement(replacer)
            return if (newFilter !== f) copy(filter = newFilter) else this
        }
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
    ) : GameEvent {
        override val description: String = buildString {
            append("this creature blocks or becomes blocked by ")
            if (partnerFilter != null) {
                append(describeObjectForEvent(partnerFilter))
            } else {
                append("a creature")
            }
        }

        override fun applyTextReplacement(replacer: TextReplacer): GameEvent {
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
        val sourceFilter: GameObjectFilter? = null
    ) : GameEvent {
        override val description: String = buildString {
            if (sourceFilter != null) {
                append(describeObjectForEvent(sourceFilter))
                append(" ")
            }
            append("deals ")
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

        override fun applyTextReplacement(replacer: TextReplacer): GameEvent {
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
    ) : GameEvent {
        override val description: String = buildString {
            append("this is dealt damage")
            if (source != SourceFilter.Any) {
                append(" by ")
                append(source.description)
            }
        }

        override fun applyTextReplacement(replacer: TextReplacer): GameEvent = this
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
    data object CreatureDealtDamageBySourceDiesEvent : GameEvent {
        override val description: String = "whenever a creature dealt damage by this creature this turn dies"
        override fun applyTextReplacement(replacer: TextReplacer): GameEvent = this
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
    ) : GameEvent {
        override val description: String = buildString {
            append("at the beginning of ")
            when (player) {
                Player.You -> append("your ")
                Player.Each -> append("each ")
                else -> append("${player.description}'s ")
            }
            append(step.displayName)
        }

        override fun applyTextReplacement(replacer: TextReplacer): GameEvent = this
    }

    // ---- Spell/Card Triggers ----

    /**
     * When a spell is cast.
     */
    @SerialName("SpellCastEvent")
    @Serializable
    data class SpellCastEvent(
        val spellType: SpellTypeFilter = SpellTypeFilter.ANY,
        val manaValueAtLeast: Int? = null,
        val manaValueAtMost: Int? = null,
        val manaValueEquals: Int? = null,
        val kicked: Boolean? = null,
        val player: Player = Player.You,
        val subtype: Subtype? = null
    ) : GameEvent {
        override val description: String = buildString {
            append(player.description)
            append(" casts ")
            if (kicked == true) append("a kicked ")
            if (subtype != null) {
                append("a ${subtype.value} spell")
            } else {
                when (spellType) {
                    SpellTypeFilter.ANY -> if (kicked != true) append("a spell") else append("spell")
                    SpellTypeFilter.CREATURE -> if (kicked != true) append("a creature spell") else append("creature spell")
                    SpellTypeFilter.NONCREATURE -> if (kicked != true) append("a noncreature spell") else append("noncreature spell")
                    SpellTypeFilter.INSTANT_OR_SORCERY -> if (kicked != true) append("an instant or sorcery spell") else append("instant or sorcery spell")
                    SpellTypeFilter.ENCHANTMENT -> if (kicked != true) append("an enchantment spell") else append("enchantment spell")
                    SpellTypeFilter.HISTORIC -> if (kicked != true) append("a historic spell") else append("historic spell")
                }
            }
            if (manaValueAtLeast != null) append(" with mana value $manaValueAtLeast or greater")
            if (manaValueAtMost != null) append(" with mana value $manaValueAtMost or less")
            if (manaValueEquals != null) append(" with mana value $manaValueEquals")
        }

        override fun applyTextReplacement(replacer: TextReplacer): GameEvent = this
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
    ) : GameEvent {
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

        override fun applyTextReplacement(replacer: TextReplacer): GameEvent = this
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
    ) : GameEvent {
        override val description: String = "${player.description} expends $threshold"

        override fun applyTextReplacement(replacer: TextReplacer): GameEvent = this
    }

    /**
     * When a card is cycled.
     */
    @SerialName("CycleEvent")
    @Serializable
    data class CycleEvent(
        val player: Player = Player.You
    ) : GameEvent {
        override val description: String = "${player.description} cycles a card"

        override fun applyTextReplacement(replacer: TextReplacer): GameEvent = this
    }

    // ---- Targeting Triggers ----

    /**
     * When a permanent becomes the target of a spell or ability.
     * Binding SELF = "when this creature becomes the target",
     * ANY = "whenever a creature you control becomes the target".
     *
     * The [targetFilter] restricts what type of permanent can trigger this
     * (e.g., Cleric creatures you control).
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
        val firstTimeEachTurn: Boolean = false
    ) : GameEvent {
        override val description: String = buildString {
            append(describeObjectForEvent(targetFilter))
            append(" becomes the target of a spell or ability")
            if (byYou) append(" you control")
            if (byOpponent) append(" an opponent controls")
            if (firstTimeEachTurn) append(" for the first time each turn")
        }

        override fun applyTextReplacement(replacer: TextReplacer): GameEvent {
            val newFilter = targetFilter.applyTextReplacement(replacer)
            return if (newFilter !== targetFilter) copy(targetFilter = newFilter) else this
        }
    }

    // ---- State Change Triggers ----

    /**
     * When a permanent becomes tapped.
     * Binding SELF = "whenever this becomes tapped".
     */
    @SerialName("TapEvent")
    @Serializable
    data object TapEvent : GameEvent {
        override val description: String = "this permanent becomes tapped"
        override fun applyTextReplacement(replacer: TextReplacer): GameEvent = this
    }

    /**
     * When a permanent becomes untapped.
     * Binding SELF = "whenever this becomes untapped".
     */
    @SerialName("UntapEvent")
    @Serializable
    data object UntapEvent : GameEvent {
        override val description: String = "this permanent becomes untapped"
        override fun applyTextReplacement(replacer: TextReplacer): GameEvent = this
    }

    /**
     * When a permanent is turned face up.
     * Binding SELF = "when this is turned face up".
     */
    @SerialName("TurnFaceUpEvent")
    @Serializable
    data object TurnFaceUpEvent : GameEvent {
        override val description: String = "this is turned face up"
        override fun applyTextReplacement(replacer: TextReplacer): GameEvent = this
    }

    /**
     * When a creature is turned face up.
     * [player] filters whose creature: [Player.You] (default), [Player.Any], etc.
     */
    @SerialName("CreatureTurnedFaceUpEvent")
    @Serializable
    data class CreatureTurnedFaceUpEvent(
        val player: Player = Player.You
    ) : GameEvent {
        override val description: String = buildString {
            append("a creature ")
            when (player) {
                is Player.You -> append("you control ")
                is Player.Any -> {}
                else -> append("${player.description} controls ")
            }
            append("is turned face up")
        }
        override fun applyTextReplacement(replacer: TextReplacer): GameEvent = this
    }

    /**
     * When a permanent transforms.
     * [intoBackFace] filters direction: true = to back, false = to front, null = either.
     */
    @SerialName("TransformEvent")
    @Serializable
    data class TransformEvent(
        val intoBackFace: Boolean? = null
    ) : GameEvent {
        override val description: String = buildString {
            append("this transforms")
            when (intoBackFace) {
                true -> append(" into its back face")
                false -> append(" into its front face")
                null -> {}
            }
        }

        override fun applyTextReplacement(replacer: TextReplacer): GameEvent = this
    }

    /**
     * When control of a permanent changes.
     * Binding SELF = "when you gain control of this from another player".
     */
    @SerialName("ControlChangeEvent")
    @Serializable
    data object ControlChangeEvent : GameEvent {
        override val description: String = "control of this permanent changes"
        override fun applyTextReplacement(replacer: TextReplacer): GameEvent = this
    }

    // ---- Counter Triggers ----

    /**
     * When one or more counters of a specific type are placed on a permanent.
     *
     * Examples:
     * - "Whenever you put one or more +1/+1 counters on a creature you control"
     *   → CountersPlacedEvent(counterType = "+1/+1", filter = GameObjectFilter.Creature.youControl())
     *
     * @property counterType The counter type to match (e.g., "+1/+1", "LORE")
     * @property filter Filter for the permanent receiving counters
     */
    @SerialName("CountersPlacedEvent")
    @Serializable
    data class CountersPlacedEvent(
        val counterType: String,
        val filter: GameObjectFilter = GameObjectFilter.Any
    ) : GameEvent {
        override val description: String = buildString {
            append("one or more $counterType counters are placed on ")
            append(describeObjectForEvent(filter))
        }

        override fun applyTextReplacement(replacer: TextReplacer): GameEvent = this
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
    data object SpellOrAbilityOnStackEvent : GameEvent {
        override val description: String = "a spell or ability is put onto the stack"
        override fun applyTextReplacement(replacer: TextReplacer): GameEvent = this
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
    ) : GameEvent {
        override val description: String = buildString {
            append("you reveal ")
            if (cardFilter != null) {
                append(describeObjectForEvent(cardFilter))
            } else {
                append("a card")
            }
            append(" this way")
        }

        override fun applyTextReplacement(replacer: TextReplacer): GameEvent {
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
    ) : GameEvent {
        override val description: String = buildString {
            append("one or more ")
            if (filter != GameObjectFilter.Any) {
                append(filter.cardPredicates.joinToString(" ") { it.description })
                append(" ")
            }
            append("cards are put into your graveyard from your library")
        }

        override fun applyTextReplacement(replacer: TextReplacer): GameEvent {
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
    ) : GameEvent {
        override val description: String = buildString {
            append("you sacrifice one or more ")
            if (filter != GameObjectFilter.Any) {
                append(filter.cardPredicates.joinToString(" ") { it.description })
                append("s")
            } else {
                append("permanents")
            }
        }

        override fun applyTextReplacement(replacer: TextReplacer): GameEvent {
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
    ) : GameEvent {
        override val description: String = buildString {
            append("one or more ")
            append(describeObjectForEvent(sourceFilter))
            append(" you control deal combat damage to a player")
        }

        override fun applyTextReplacement(replacer: TextReplacer): GameEvent {
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
    ) : GameEvent {
        override val description: String = buildString {
            append("one or more ")
            if (excludeSelf) append("other ")
            append(describeObjectForEvent(filter))
            append(" you control leave the battlefield without dying")
        }

        override fun applyTextReplacement(replacer: TextReplacer): GameEvent {
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
    ) : GameEvent {
        override val description: String = buildString {
            append("one or more ")
            append(describeObjectForEvent(filter))
            append(" you control enter the battlefield")
        }

        override fun applyTextReplacement(replacer: TextReplacer): GameEvent {
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
