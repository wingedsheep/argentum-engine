package com.wingedsheep.rulesengine.ability

import com.wingedsheep.rulesengine.core.CardId
import com.wingedsheep.rulesengine.core.Color
import com.wingedsheep.rulesengine.core.Keyword
import com.wingedsheep.rulesengine.ecs.EntityId
import com.wingedsheep.rulesengine.player.PlayerId
import kotlinx.serialization.Serializable

/**
 * Sealed hierarchy of effects.
 * Effects define WHAT happens when an ability resolves.
 */
@Serializable
sealed interface Effect {
    /** Human-readable description of the effect */
    val description: String
}

// =============================================================================
// Life Effects
// =============================================================================

/**
 * Gain life effect.
 * "You gain X life" or "Target player gains X life"
 */
@Serializable
data class GainLifeEffect(
    val amount: Int,
    val target: EffectTarget = EffectTarget.Controller
) : Effect {
    override val description: String = when (target) {
        EffectTarget.Controller -> "You gain $amount life"
        EffectTarget.Opponent -> "Target opponent gains $amount life"
        EffectTarget.AnyPlayer -> "Target player gains $amount life"
        else -> "Gain $amount life"
    }
}

/**
 * Lose life effect.
 * "You lose X life" or "Target player loses X life"
 */
@Serializable
data class LoseLifeEffect(
    val amount: Int,
    val target: EffectTarget = EffectTarget.Opponent
) : Effect {
    override val description: String = when (target) {
        EffectTarget.Controller -> "You lose $amount life"
        EffectTarget.Opponent -> "Target opponent loses $amount life"
        EffectTarget.AnyPlayer -> "Target player loses $amount life"
        else -> "Lose $amount life"
    }
}

// =============================================================================
// Damage Effects
// =============================================================================

/**
 * Deal damage effect.
 * "Deal X damage to target creature/player"
 */
@Serializable
data class DealDamageEffect(
    val amount: Int,
    val target: EffectTarget
) : Effect {
    override val description: String = "Deal $amount damage to ${target.description}"
}

// =============================================================================
// Card Drawing Effects
// =============================================================================

/**
 * Draw cards effect.
 * "Draw X cards" or "Target player draws X cards"
 */
@Serializable
data class DrawCardsEffect(
    val count: Int,
    val target: EffectTarget = EffectTarget.Controller
) : Effect {
    override val description: String = when (target) {
        EffectTarget.Controller -> "Draw ${if (count == 1) "a card" else "$count cards"}"
        EffectTarget.Opponent -> "Target opponent draws ${if (count == 1) "a card" else "$count cards"}"
        else -> "Target player draws ${if (count == 1) "a card" else "$count cards"}"
    }
}

/**
 * Discard cards effect.
 * "Discard X cards" or "Target player discards X cards"
 */
@Serializable
data class DiscardCardsEffect(
    val count: Int,
    val target: EffectTarget = EffectTarget.Opponent
) : Effect {
    override val description: String = when (target) {
        EffectTarget.Controller -> "Discard ${if (count == 1) "a card" else "$count cards"}"
        EffectTarget.Opponent -> "Target opponent discards ${if (count == 1) "a card" else "$count cards"}"
        else -> "Target player discards ${if (count == 1) "a card" else "$count cards"}"
    }
}

// =============================================================================
// Creature Effects
// =============================================================================

/**
 * Destroy target creature/permanent effect.
 * "Destroy target creature"
 */
@Serializable
data class DestroyEffect(
    val target: EffectTarget
) : Effect {
    override val description: String = "Destroy ${target.description}"
}

/**
 * Exile target effect.
 * "Exile target creature/permanent"
 */
@Serializable
data class ExileEffect(
    val target: EffectTarget
) : Effect {
    override val description: String = "Exile ${target.description}"
}

/**
 * Return to hand effect.
 * "Return target creature to its owner's hand"
 */
@Serializable
data class ReturnToHandEffect(
    val target: EffectTarget
) : Effect {
    override val description: String = "Return ${target.description} to its owner's hand"
}

/**
 * Tap/Untap target effect.
 * "Tap target creature" or "Untap target creature"
 */
@Serializable
data class TapUntapEffect(
    val target: EffectTarget,
    val tap: Boolean = true
) : Effect {
    override val description: String = "${if (tap) "Tap" else "Untap"} ${target.description}"
}

// =============================================================================
// Stat Modification Effects
// =============================================================================

/**
 * Modify power/toughness effect.
 * "Target creature gets +X/+Y until end of turn"
 */
@Serializable
data class ModifyStatsEffect(
    val powerModifier: Int,
    val toughnessModifier: Int,
    val target: EffectTarget,
    val untilEndOfTurn: Boolean = true
) : Effect {
    override val description: String = buildString {
        append("${target.description} gets ")
        append(if (powerModifier >= 0) "+$powerModifier" else "$powerModifier")
        append("/")
        append(if (toughnessModifier >= 0) "+$toughnessModifier" else "$toughnessModifier")
        if (untilEndOfTurn) append(" until end of turn")
    }
}

/**
 * Add counters effect.
 * "Put X +1/+1 counters on target creature"
 */
@Serializable
data class AddCountersEffect(
    val counterType: String,
    val count: Int,
    val target: EffectTarget
) : Effect {
    override val description: String =
        "Put $count $counterType counter${if (count != 1) "s" else ""} on ${target.description}"
}

// =============================================================================
// Mana Effects
// =============================================================================

/**
 * Add mana effect.
 * "Add {G}" or "Add {R}{R}"
 */
@Serializable
data class AddManaEffect(
    val color: Color,
    val amount: Int = 1
) : Effect {
    override val description: String = "Add ${"{${color.symbol}}".repeat(amount)}"
}

/**
 * Add colorless mana effect.
 * "Add {C}{C}"
 */
@Serializable
data class AddColorlessManaEffect(
    val amount: Int
) : Effect {
    override val description: String = "Add ${"{C}".repeat(amount)}"
}

// =============================================================================
// Token Effects
// =============================================================================

/**
 * Create token effect.
 * "Create a 1/1 white Soldier creature token"
 */
@Serializable
data class CreateTokenEffect(
    val count: Int = 1,
    val power: Int,
    val toughness: Int,
    val colors: Set<Color>,
    val creatureTypes: Set<String>,
    val keywords: Set<Keyword> = emptySet()
) : Effect {
    override val description: String = buildString {
        append("Create ")
        append(if (count == 1) "a" else "$count")
        append(" $power/$toughness ")
        append(colors.joinToString(" and ") { it.displayName.lowercase() })
        append(" ")
        append(creatureTypes.joinToString(" "))
        append(" creature token")
        if (count != 1) append("s")
        if (keywords.isNotEmpty()) {
            append(" with ")
            append(keywords.joinToString(", ") { it.name.lowercase() })
        }
    }
}

// =============================================================================
// Composite Effects
// =============================================================================

/**
 * Multiple effects that happen together.
 */
@Serializable
data class CompositeEffect(
    val effects: List<Effect>
) : Effect {
    override val description: String = effects.joinToString(". ") { it.description }
}

// =============================================================================
// Library Effects
// =============================================================================

/**
 * Shuffle a card into its owner's library.
 * "Shuffle this card into its owner's library"
 */
@Serializable
data class ShuffleIntoLibraryEffect(
    val target: EffectTarget
) : Effect {
    override val description: String = "Shuffle ${target.description} into its owner's library"
}

/**
 * Put a card on top of its owner's library.
 * "Put this card on top of its owner's library"
 */
@Serializable
data class PutOnTopOfLibraryEffect(
    val target: EffectTarget
) : Effect {
    override val description: String = "Put ${target.description} on top of its owner's library"
}

/**
 * Look at the top N cards and choose some to keep.
 * "Look at the top N cards of your library. Put X of them into your hand and the rest into your graveyard."
 */
@Serializable
data class LookAtTopCardsEffect(
    val count: Int,
    val keepCount: Int,
    val restToGraveyard: Boolean = true
) : Effect {
    override val description: String = buildString {
        append("Look at the top $count cards of your library. ")
        append("Put $keepCount of them into your hand and the rest into your ")
        append(if (restToGraveyard) "graveyard" else "library in any order")
    }
}

/**
 * Shuffle a player's library.
 * "Shuffle your library" or "Target player shuffles their library"
 */
@Serializable
data class ShuffleLibraryEffect(
    val target: EffectTarget = EffectTarget.Controller
) : Effect {
    override val description: String = when (target) {
        EffectTarget.Controller -> "Shuffle your library"
        EffectTarget.Opponent -> "Target opponent shuffles their library"
        else -> "Target player shuffles their library"
    }
}

/**
 * Search library for cards matching a filter.
 * "Search your library for a Forest card and put it onto the battlefield"
 *
 * @param selectedCardIds Optional explicit card IDs to select. When provided,
 *        the handler will select these specific cards (if they match the filter)
 *        instead of auto-selecting. This enables player choice in the decision
 *        system and deterministic testing.
 */
@Serializable
data class SearchLibraryEffect(
    val filter: CardFilter,
    val count: Int = 1,
    val destination: SearchDestination = SearchDestination.HAND,
    val entersTapped: Boolean = false,
    val shuffleAfter: Boolean = true,
    val reveal: Boolean = false,
    val selectedCardIds: List<EntityId>? = null
) : Effect {
    override val description: String = buildString {
        append("Search your library for ")
        append(if (count == 1) "a" else "up to $count")
        append(" ${filter.description}")
        if (count != 1) append("s")
        if (reveal) append(", reveal ${if (count == 1) "it" else "them"},")
        append(" and put ${if (count == 1) "it" else "them"} ")
        append(destination.description)
        if (entersTapped && destination == SearchDestination.BATTLEFIELD) {
            append(" tapped")
        }
        if (shuffleAfter) append(". Then shuffle your library")
    }
}

/**
 * Destination for searched cards.
 */
@Serializable
enum class SearchDestination(val description: String) {
    HAND("into your hand"),
    BATTLEFIELD("onto the battlefield"),
    GRAVEYARD("into your graveyard"),
    TOP_OF_LIBRARY("on top of your library")
}

// =============================================================================
// Card Filters
// =============================================================================

/**
 * Filter for matching cards during search effects.
 */
@Serializable
sealed interface CardFilter {
    val description: String

    /** Match any card */
    @Serializable
    data object AnyCard : CardFilter {
        override val description: String = "card"
    }

    /** Match creature cards */
    @Serializable
    data object CreatureCard : CardFilter {
        override val description: String = "creature card"
    }

    /** Match land cards */
    @Serializable
    data object LandCard : CardFilter {
        override val description: String = "land card"
    }

    /** Match basic land cards */
    @Serializable
    data object BasicLandCard : CardFilter {
        override val description: String = "basic land card"
    }

    /** Match sorcery cards */
    @Serializable
    data object SorceryCard : CardFilter {
        override val description: String = "sorcery card"
    }

    /** Match instant cards */
    @Serializable
    data object InstantCard : CardFilter {
        override val description: String = "instant card"
    }

    /** Match cards with a specific subtype (e.g., "Forest", "Elf") */
    @Serializable
    data class HasSubtype(val subtype: String) : CardFilter {
        override val description: String = subtype
    }

    /** Match cards with a specific color */
    @Serializable
    data class HasColor(val color: Color) : CardFilter {
        override val description: String = "${color.displayName.lowercase()} card"
    }

    /** Match cards that are both a type and have a specific property */
    @Serializable
    data class And(val filters: List<CardFilter>) : CardFilter {
        override val description: String = filters.joinToString(" ") { it.description }
    }

    /** Match cards that match any of the filters */
    @Serializable
    data class Or(val filters: List<CardFilter>) : CardFilter {
        override val description: String = filters.joinToString(" or ") { it.description }
    }
}

// =============================================================================
// Combat Effects
// =============================================================================

/**
 * All creatures that can block target creature must do so.
 * "All creatures able to block target creature this turn do so."
 */
@Serializable
data class MustBeBlockedEffect(
    val target: EffectTarget
) : Effect {
    override val description: String = "All creatures able to block ${target.description} this turn do so"
}

// =============================================================================
// Keyword Grant Effects
// =============================================================================

/**
 * Grant a keyword to a target until end of turn.
 * "Target creature gains flying until end of turn."
 */
@Serializable
data class GrantKeywordUntilEndOfTurnEffect(
    val keyword: Keyword,
    val target: EffectTarget
) : Effect {
    override val description: String = "${target.description} gains ${keyword.displayName.lowercase()} until end of turn"
}

// =============================================================================
// Mass Destruction Effects
// =============================================================================

/**
 * Destroy all lands.
 * "Destroy all lands."
 */
@Serializable
data object DestroyAllLandsEffect : Effect {
    override val description: String = "Destroy all lands"
}

/**
 * Destroy all creatures.
 * "Destroy all creatures."
 */
@Serializable
data object DestroyAllCreaturesEffect : Effect {
    override val description: String = "Destroy all creatures"
}

/**
 * Destroy all lands of a specific type.
 * "Destroy all Plains." / "Destroy all Islands."
 */
@Serializable
data class DestroyAllLandsOfTypeEffect(
    val landType: String
) : Effect {
    override val description: String = "Destroy all ${landType}s"
}

/**
 * Wheel effect - each affected player shuffles their hand into their library, then draws that many cards.
 * Used for Winds of Change, Wheel of Fortune-style effects.
 */
@Serializable
data class WheelEffect(
    val target: EffectTarget = EffectTarget.EachPlayer
) : Effect {
    override val description: String = when (target) {
        EffectTarget.Controller -> "Shuffle your hand into your library, then draw that many cards"
        EffectTarget.EachPlayer -> "Each player shuffles their hand into their library, then draws that many cards"
        else -> "Shuffle hand into library, then draw that many cards"
    }
}

/**
 * Deal damage to all creatures.
 * "Deal X damage to each creature."
 */
@Serializable
data class DealDamageToAllCreaturesEffect(
    val amount: Int,
    val onlyFlying: Boolean = false,
    val onlyNonFlying: Boolean = false
) : Effect {
    override val description: String = buildString {
        append("Deal $amount damage to each ")
        when {
            onlyFlying -> append("creature with flying")
            onlyNonFlying -> append("creature without flying")
            else -> append("creature")
        }
    }
}

/**
 * Deal damage to each creature and each player.
 * Used for effects like Earthquake, Dry Spell, Fire Tempest.
 */
@Serializable
data class DealDamageToAllEffect(
    val amount: Int,
    val onlyFlyingCreatures: Boolean = false,
    val onlyNonFlyingCreatures: Boolean = false
) : Effect {
    override val description: String = buildString {
        append("Deal $amount damage to each ")
        when {
            onlyFlyingCreatures -> append("creature with flying")
            onlyNonFlyingCreatures -> append("creature without flying")
            else -> append("creature")
        }
        append(" and each player")
    }
}

/**
 * Drain effect - deal damage and gain that much life.
 * "Deal X damage to target and you gain X life."
 */
@Serializable
data class DrainEffect(
    val amount: Int,
    val target: EffectTarget
) : Effect {
    override val description: String = "Deal $amount damage to ${target.description} and you gain $amount life"
}

// =============================================================================
// Effect Targets
// =============================================================================

/**
 * Defines who/what an effect targets.
 */
@Serializable
sealed interface EffectTarget {
    val description: String

    /** The controller of the source ability */
    @Serializable
    data object Controller : EffectTarget {
        override val description: String = "you"
    }

    /** An opponent of the controller */
    @Serializable
    data object Opponent : EffectTarget {
        override val description: String = "target opponent"
    }

    /** Any player */
    @Serializable
    data object AnyPlayer : EffectTarget {
        override val description: String = "target player"
    }

    /** The source permanent itself */
    @Serializable
    data object Self : EffectTarget {
        override val description: String = "this creature"
    }

    /** Target creature */
    @Serializable
    data object TargetCreature : EffectTarget {
        override val description: String = "target creature"
    }

    /** Target creature an opponent controls */
    @Serializable
    data object TargetOpponentCreature : EffectTarget {
        override val description: String = "target creature an opponent controls"
    }

    /** Target creature you control */
    @Serializable
    data object TargetControlledCreature : EffectTarget {
        override val description: String = "target creature you control"
    }

    /** Target permanent */
    @Serializable
    data object TargetPermanent : EffectTarget {
        override val description: String = "target permanent"
    }

    /** Target nonland permanent */
    @Serializable
    data object TargetNonlandPermanent : EffectTarget {
        override val description: String = "target nonland permanent"
    }

    /** Target land */
    @Serializable
    data object TargetLand : EffectTarget {
        override val description: String = "target land"
    }

    /** Target nonblack creature */
    @Serializable
    data object TargetNonblackCreature : EffectTarget {
        override val description: String = "target nonblack creature"
    }

    /** Any target (creature or player) */
    @Serializable
    data object AnyTarget : EffectTarget {
        override val description: String = "any target"
    }

    /** All creatures */
    @Serializable
    data object AllCreatures : EffectTarget {
        override val description: String = "all creatures"
    }

    /** All creatures you control */
    @Serializable
    data object AllControlledCreatures : EffectTarget {
        override val description: String = "creatures you control"
    }

    /** All creatures opponents control */
    @Serializable
    data object AllOpponentCreatures : EffectTarget {
        override val description: String = "creatures your opponents control"
    }

    /** Each opponent */
    @Serializable
    data object EachOpponent : EffectTarget {
        override val description: String = "each opponent"
    }

    /** Each player */
    @Serializable
    data object EachPlayer : EffectTarget {
        override val description: String = "each player"
    }

    /** Target tapped creature */
    @Serializable
    data object TargetTappedCreature : EffectTarget {
        override val description: String = "target tapped creature"
    }

    /** The controller of the target (used for effects like "its controller gains 4 life") */
    @Serializable
    data object TargetController : EffectTarget {
        override val description: String = "its controller"
    }
}

// =============================================================================
// Graveyard Effects
// =============================================================================

/**
 * Return a card from graveyard to another zone.
 * "Return target creature card from your graveyard to your hand"
 */
@Serializable
data class ReturnFromGraveyardEffect(
    val filter: CardFilter,
    val destination: SearchDestination = SearchDestination.HAND
) : Effect {
    override val description: String =
        "Return ${filter.description} from your graveyard ${destination.description}"
}

// =============================================================================
// Sacrifice Effects
// =============================================================================

/**
 * Represents a sacrifice cost - a number of permanents matching a filter.
 * "sacrifice three Forests"
 */
@Serializable
data class SacrificeCost(
    val filter: CardFilter,
    val count: Int = 1
) {
    val description: String = if (count == 1) {
        "sacrifice a ${filter.description}"
    } else {
        "sacrifice $count ${filter.description}s"
    }
}

/**
 * Sacrifice a permanent unless a cost is paid.
 * "Sacrifice this creature unless you sacrifice three Forests."
 *
 * Used for cards like Primeval Force that require a sacrifice decision on ETB.
 */
@Serializable
data class SacrificeUnlessEffect(
    val permanentToSacrifice: EffectTarget,
    val cost: SacrificeCost
) : Effect {
    override val description: String = "Sacrifice ${permanentToSacrifice.description} unless you ${cost.description}"
}
