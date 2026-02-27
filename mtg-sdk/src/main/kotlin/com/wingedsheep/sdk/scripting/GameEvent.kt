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
sealed interface GameEvent {
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
        val to: Zone? = null
    ) : GameEvent {
        override val description: String = buildString {
            append(describeObjectForEvent(filter))
            if (to != null) {
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
    }

    // =========================================================================
    // Trigger-Only Events (below here — used only as trigger filters)
    // =========================================================================

    // ---- Combat Triggers ----

    /**
     * When a creature attacks.
     * Binding SELF = "when this creature attacks", ANY = "whenever a creature attacks".
     */
    @SerialName("AttackEvent")
    @Serializable
    data object AttackEvent : GameEvent {
        override val description: String = "a creature attacks"
    }

    /**
     * When you attack with at least [minAttackers] creatures.
     */
    @SerialName("YouAttackEvent")
    @Serializable
    data class YouAttackEvent(
        val minAttackers: Int = 1
    ) : GameEvent {
        override val description: String = if (minAttackers <= 1) {
            "you attack"
        } else {
            "you attack with $minAttackers or more creatures"
        }
    }

    /**
     * When a creature blocks.
     * Binding SELF = "when this creature blocks".
     */
    @SerialName("BlockEvent")
    @Serializable
    data object BlockEvent : GameEvent {
        override val description: String = "a creature blocks"
    }

    /**
     * When a creature becomes blocked.
     * Binding SELF = "when this creature becomes blocked",
     * ANY = "whenever a creature you control becomes blocked".
     */
    @SerialName("BecomesBlockedEvent")
    @Serializable
    data object BecomesBlockedEvent : GameEvent {
        override val description: String = "a creature becomes blocked"
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
    }

    /**
     * At the beginning of enchanted creature's controller's upkeep.
     * Special case for auras like Custody Battle.
     */
    @SerialName("EnchantedCreatureControllerStepEvent")
    @Serializable
    data class EnchantedCreatureControllerStepEvent(
        val step: Step = Step.UPKEEP
    ) : GameEvent {
        override val description: String =
            "at the beginning of enchanted creature's controller's ${step.displayName}"
    }

    /**
     * When the enchanted creature is dealt damage.
     * Special case for auras like Frozen Solid.
     */
    @SerialName("EnchantedCreatureDamageReceivedEvent")
    @Serializable
    data object EnchantedCreatureDamageReceivedEvent : GameEvent {
        override val description: String = "when enchanted creature is dealt damage"
    }

    /**
     * When the enchanted creature deals combat damage to a player.
     * Special case for auras like One with Nature.
     */
    @SerialName("EnchantedCreatureDealsCombatDamageToPlayerEvent")
    @Serializable
    data object EnchantedCreatureDealsCombatDamageToPlayerEvent : GameEvent {
        override val description: String = "when enchanted creature deals combat damage to a player"
    }

    /**
     * When the enchanted creature deals damage (any type).
     * Special case for auras like Guilty Conscience.
     */
    @SerialName("EnchantedCreatureDealsDamageEvent")
    @Serializable
    data object EnchantedCreatureDealsDamageEvent : GameEvent {
        override val description: String = "when enchanted creature deals damage"
    }

    /**
     * When the enchanted creature is turned face up.
     * Special case for auras like Fatal Mutation.
     */
    @SerialName("EnchantedCreatureTurnedFaceUpEvent")
    @Serializable
    data object EnchantedCreatureTurnedFaceUpEvent : GameEvent {
        override val description: String = "when enchanted creature is turned face up"
    }

    /**
     * When the enchanted creature attacks.
     * Special case for auras like Extra Arms.
     */
    @SerialName("EnchantedCreatureAttacksEvent")
    @Serializable
    data object EnchantedCreatureAttacksEvent : GameEvent {
        override val description: String = "when enchanted creature attacks"
    }

    /**
     * When the enchanted permanent becomes tapped.
     * Special case for auras like Uncontrolled Infestation.
     */
    @SerialName("EnchantedPermanentBecomesTappedEvent")
    @Serializable
    data object EnchantedPermanentBecomesTappedEvent : GameEvent {
        override val description: String = "when enchanted permanent becomes tapped"
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
        val player: Player = Player.You
    ) : GameEvent {
        override val description: String = buildString {
            append(player.description)
            append(" casts ")
            when (spellType) {
                SpellTypeFilter.ANY -> append("a spell")
                SpellTypeFilter.CREATURE -> append("a creature spell")
                SpellTypeFilter.NONCREATURE -> append("a noncreature spell")
                SpellTypeFilter.INSTANT_OR_SORCERY -> append("an instant or sorcery spell")
                SpellTypeFilter.ENCHANTMENT -> append("an enchantment spell")
            }
            if (manaValueAtLeast != null) append(" with mana value $manaValueAtLeast or greater")
            if (manaValueAtMost != null) append(" with mana value $manaValueAtMost or less")
            if (manaValueEquals != null) append(" with mana value $manaValueEquals")
        }
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
    }

    // ---- Targeting Triggers ----

    /**
     * When a permanent becomes the target of a spell or ability.
     * Binding SELF = "when this creature becomes the target",
     * ANY = "whenever a creature you control becomes the target".
     *
     * The [targetFilter] restricts what type of permanent can trigger this
     * (e.g., Cleric creatures you control).
     */
    @SerialName("BecomesTargetEvent")
    @Serializable
    data class BecomesTargetEvent(
        val targetFilter: GameObjectFilter = GameObjectFilter.Any
    ) : GameEvent {
        override val description: String = buildString {
            append(describeObjectForEvent(targetFilter))
            append(" becomes the target of a spell or ability")
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
    }

    /**
     * When a permanent becomes untapped.
     * Binding SELF = "whenever this becomes untapped".
     */
    @SerialName("UntapEvent")
    @Serializable
    data object UntapEvent : GameEvent {
        override val description: String = "this permanent becomes untapped"
    }

    /**
     * When a permanent is turned face up.
     * Binding SELF = "when this is turned face up".
     */
    @SerialName("TurnFaceUpEvent")
    @Serializable
    data object TurnFaceUpEvent : GameEvent {
        override val description: String = "this is turned face up"
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
    }

    /**
     * When control of a permanent changes.
     * Binding SELF = "when you gain control of this from another player".
     */
    @SerialName("ControlChangeEvent")
    @Serializable
    data object ControlChangeEvent : GameEvent {
        override val description: String = "control of this permanent changes"
    }

    // ---- Draw/Reveal Triggers ----

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
