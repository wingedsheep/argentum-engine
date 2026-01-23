package com.wingedsheep.sdk.scripting

import com.wingedsheep.sdk.core.Color
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
     * - "a card would be put into a graveyard" → ZoneChangeEvent(to = Zone.Graveyard)
     * - "a creature would enter the battlefield" → ZoneChangeEvent(object = ObjectFilter.Creature, to = Zone.Battlefield)
     * - "a creature would die" → ZoneChangeEvent(object = ObjectFilter.Creature, from = Zone.Battlefield, to = Zone.Graveyard)
     */
    @Serializable
    data class ZoneChangeEvent(
        val objectFilter: ObjectFilter = ObjectFilter.Any,
        val from: Zone? = null,
        val to: Zone
    ) : GameEvent {
        override val description: String = buildString {
            append(objectFilter.description)
            append(" would ")
            when (to) {
                Zone.Graveyard -> {
                    if (from == Zone.Battlefield) append("die")
                    else append("be put into a graveyard")
                }
                Zone.Battlefield -> append("enter the battlefield")
                Zone.Exile -> append("be exiled")
                Zone.Hand -> append("be returned to a hand")
                Zone.Library -> append("be put into a library")
                Zone.Stack -> append("be put on the stack")
                Zone.Command -> append("be put into the command zone")
            }
            if (from != null && to != Zone.Graveyard) {
                append(" from ${from.description}")
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
        val tokenFilter: CardFilter? = null
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
     * - "you would draw a card" → DrawEvent(player = PlayerFilter.You)
     * - "an opponent would draw" → DrawEvent(player = PlayerFilter.Opponent)
     */
    @Serializable
    data class DrawEvent(
        val player: PlayerFilter = PlayerFilter.You
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
        val player: PlayerFilter = PlayerFilter.You
    ) : GameEvent {
        override val description: String = "${player.description} would gain life"
    }

    /**
     * When a player would lose life.
     */
    @Serializable
    data class LifeLossEvent(
        val player: PlayerFilter = PlayerFilter.You
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
        val player: PlayerFilter = PlayerFilter.You,
        val cardFilter: CardFilter? = null
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
        val player: PlayerFilter = PlayerFilter.You
    ) : GameEvent {
        override val description: String = "${player.description} would search a library"
    }
}

// =============================================================================
// Supporting Filter Types
// =============================================================================

/**
 * Filter for damage/effect recipients.
 */
@Serializable
sealed interface RecipientFilter {
    val description: String

    @Serializable
    data object Any : RecipientFilter {
        override val description = "any target"
    }

    @Serializable
    data object You : RecipientFilter {
        override val description = "you"
    }

    @Serializable
    data object Opponent : RecipientFilter {
        override val description = "an opponent"
    }

    @Serializable
    data object AnyPlayer : RecipientFilter {
        override val description = "a player"
    }

    @Serializable
    data object CreatureYouControl : RecipientFilter {
        override val description = "a creature you control"
    }

    @Serializable
    data object CreatureOpponentControls : RecipientFilter {
        override val description = "a creature an opponent controls"
    }

    @Serializable
    data object AnyCreature : RecipientFilter {
        override val description = "a creature"
    }

    @Serializable
    data object PermanentYouControl : RecipientFilter {
        override val description = "a permanent you control"
    }

    @Serializable
    data object AnyPermanent : RecipientFilter {
        override val description = "a permanent"
    }

    @Serializable
    data class Matching(val filter: CardFilter) : RecipientFilter {
        override val description = filter.description
    }
}

/**
 * Filter for damage/effect sources.
 */
@Serializable
sealed interface SourceFilter {
    val description: String

    @Serializable
    data object Any : SourceFilter {
        override val description = "any source"
    }

    @Serializable
    data object Combat : SourceFilter {
        override val description = "combat"
    }

    @Serializable
    data object NonCombat : SourceFilter {
        override val description = "a non-combat source"
    }

    @Serializable
    data object Spell : SourceFilter {
        override val description = "a spell"
    }

    @Serializable
    data object Ability : SourceFilter {
        override val description = "an ability"
    }

    @Serializable
    data class HasColor(val color: Color) : SourceFilter {
        override val description = "a ${color.name.lowercase()} source"
    }

    @Serializable
    data class HasType(val type: String) : SourceFilter {
        override val description = "a $type"
    }

    @Serializable
    data class Matching(val filter: CardFilter) : SourceFilter {
        override val description = filter.description
    }
}

/**
 * Damage type classification.
 */
@Serializable
sealed interface DamageType {
    val description: String

    @Serializable
    data object Any : DamageType {
        override val description = ""
    }

    @Serializable
    data object Combat : DamageType {
        override val description = "combat"
    }

    @Serializable
    data object NonCombat : DamageType {
        override val description = "noncombat"
    }
}

/**
 * Filter for objects in zone change events.
 */
@Serializable
sealed interface ObjectFilter {
    val description: String

    @Serializable
    data object Any : ObjectFilter {
        override val description = "a card or permanent"
    }

    @Serializable
    data object AnyCard : ObjectFilter {
        override val description = "a card"
    }

    @Serializable
    data object Creature : ObjectFilter {
        override val description = "a creature"
    }

    @Serializable
    data object CreatureYouControl : ObjectFilter {
        override val description = "a creature you control"
    }

    @Serializable
    data object PermanentYouControl : ObjectFilter {
        override val description = "a permanent you control"
    }

    @Serializable
    data object NonlandPermanent : ObjectFilter {
        override val description = "a nonland permanent"
    }

    @Serializable
    data object Token : ObjectFilter {
        override val description = "a token"
    }

    @Serializable
    data class Matching(val filter: CardFilter) : ObjectFilter {
        override val description = filter.description
    }
}

/**
 * Counter type specification.
 */
@Serializable
sealed interface CounterTypeFilter {
    val description: String

    @Serializable
    data object Any : CounterTypeFilter {
        override val description = ""
    }

    @Serializable
    data object PlusOnePlusOne : CounterTypeFilter {
        override val description = "+1/+1"
    }

    @Serializable
    data object MinusOneMinusOne : CounterTypeFilter {
        override val description = "-1/-1"
    }

    @Serializable
    data object Loyalty : CounterTypeFilter {
        override val description = "loyalty"
    }

    @Serializable
    data class Named(val name: String) : CounterTypeFilter {
        override val description = name
    }
}

/**
 * Zone enumeration.
 */
@Serializable
enum class Zone(val description: String) {
    Battlefield("the battlefield"),
    Graveyard("a graveyard"),
    Hand("a hand"),
    Library("a library"),
    Exile("exile"),
    Stack("the stack"),
    Command("the command zone")
}

/**
 * Controller/owner filters.
 */
@Serializable
sealed interface ControllerFilter {
    val description: String

    @Serializable
    data object You : ControllerFilter {
        override val description = "under your control"
    }

    @Serializable
    data object Opponent : ControllerFilter {
        override val description = "under an opponent's control"
    }

    @Serializable
    data object Any : ControllerFilter {
        override val description = ""
    }
}

/**
 * Player filters.
 */
@Serializable
sealed interface PlayerFilter {
    val description: String

    @Serializable
    data object You : PlayerFilter {
        override val description = "you"
    }

    @Serializable
    data object Opponent : PlayerFilter {
        override val description = "an opponent"
    }

    @Serializable
    data object Any : PlayerFilter {
        override val description = "a player"
    }

    @Serializable
    data object Each : PlayerFilter {
        override val description = "each player"
    }
}
