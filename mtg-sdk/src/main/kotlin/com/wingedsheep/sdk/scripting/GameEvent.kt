package com.wingedsheep.sdk.scripting

import com.wingedsheep.sdk.core.Zone
import kotlinx.serialization.Serializable

/**
 * Represents a game event type that replacement effects can intercept.
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
 *                     CounterTypeFilter, ControllerFilter, Player
 * - Zone.kt - Zone enumeration
 */
@Serializable
sealed interface GameEvent {
    val description: String

    // =========================================================================
    // Damage Events
    // =========================================================================

    /**
     * When damage would be dealt.
     *
     * Examples:
     * - "damage would be dealt to you" → DamageEvent(recipient = RecipientFilter.You)
     * - "combat damage would be dealt" → DamageEvent(damageType = DamageType.Combat)
     * - "damage from red sources" → DamageEvent(source = SourceFilter.HasColor(RED))
     */
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
    }

    // =========================================================================
    // Zone Change Events
    // =========================================================================

    /**
     * When an object would change zones.
     *
     * Examples:
     * - "a card would be put into a graveyard" → ZoneChangeEvent(to = Zone.GRAVEYARD)
     * - "a creature would enter the battlefield" → ZoneChangeEvent(filter = GameObjectFilter.Creature, to = Zone.BATTLEFIELD)
     * - "a creature would die" → ZoneChangeEvent(filter = GameObjectFilter.Creature, from = Zone.BATTLEFIELD, to = Zone.GRAVEYARD)
     */
    @Serializable
    data class ZoneChangeEvent(
        val filter: GameObjectFilter = GameObjectFilter.Any,
        val from: Zone? = null,
        val to: Zone
    ) : GameEvent {
        override val description: String = buildString {
            append(describeObjectForEvent(filter))
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
    }

    // =========================================================================
    // Draw Events
    // =========================================================================

    /**
     * When a player would draw a card.
     *
     * Examples:
     * - "you would draw a card" → DrawEvent(player = Player.You)
     * - "an opponent would draw" → DrawEvent(player = Player.Opponent)
     */
    @Serializable
    data class DrawEvent(
        val player: Player = Player.You
    ) : GameEvent {
        override val description: String = "${player.description} would draw a card"
    }

    // =========================================================================
    // Life Events
    // =========================================================================

    /**
     * When a player would gain life.
     */
    @Serializable
    data class LifeGainEvent(
        val player: Player = Player.You
    ) : GameEvent {
        override val description: String = "${player.description} would gain life"
    }

    /**
     * When a player would lose life.
     */
    @Serializable
    data class LifeLossEvent(
        val player: Player = Player.You
    ) : GameEvent {
        override val description: String = "${player.description} would lose life"
    }

    // =========================================================================
    // Discard Events
    // =========================================================================

    /**
     * When a player would discard a card.
     */
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
    }

    // =========================================================================
    // Search Events
    // =========================================================================

    /**
     * When a player would search their library.
     */
    @Serializable
    data class SearchLibraryEvent(
        val player: Player = Player.You
    ) : GameEvent {
        override val description: String = "${player.description} would search a library"
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
private fun describeObjectForEvent(filter: GameObjectFilter): String {
    if (filter.cardPredicates.isEmpty()
        && filter.statePredicates.isEmpty()
        && filter.controllerPredicate == null
    ) {
        return "a card or permanent"
    }

    val baseParts = buildString {
        filter.statePredicates.forEach { append(it.description); append(" ") }
        filter.cardPredicates.forEach { append(it.description); append(" ") }
    }.trim()

    val article = if (baseParts.first().lowercaseChar() in "aeiou") "an" else "a"

    val controllerSuffix = filter.controllerPredicate
        ?.description
        ?.takeIf { it.isNotEmpty() }
        ?.let { " $it" }
        ?: ""

    return "$article $baseParts$controllerSuffix"
}
