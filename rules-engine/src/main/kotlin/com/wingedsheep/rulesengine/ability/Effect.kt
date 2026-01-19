package com.wingedsheep.rulesengine.ability

import com.wingedsheep.rulesengine.core.Color
import com.wingedsheep.rulesengine.core.Keyword
import com.wingedsheep.rulesengine.ecs.EntityId
import kotlinx.serialization.Serializable

/**
 * Sealed hierarchy of effects.
 * Effects define WHAT happens when an ability resolves.
 */
@Serializable
sealed interface Effect {
    /** Human-readable description of the effect */
    val description: String

    /**
     * Operator to chain effects.
     * Allows syntax like: EffectA then EffectB
     */
    infix fun then(next: Effect): CompositeEffect {
        return if (this is CompositeEffect) {
            CompositeEffect(this.effects + next)
        } else {
            CompositeEffect(listOf(this, next))
        }
    }
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

/**
 * Remove counters effect.
 * "Remove X -1/-1 counters from target creature"
 */
@Serializable
data class RemoveCountersEffect(
    val counterType: String,
    val count: Int,
    val target: EffectTarget
) : Effect {
    override val description: String =
        "Remove $count $counterType counter${if (count != 1) "s" else ""} from ${target.description}"
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

/**
 * Add dynamic mana effect where the amount is determined at resolution time.
 * "Add X mana in any combination of {G} and/or {W}, where X is the number of other creatures you control."
 *
 * @property amountSource What determines the amount of mana to add
 * @property allowedColors The colors of mana that can be produced (player chooses distribution)
 */
@Serializable
data class AddDynamicManaEffect(
    val amountSource: DynamicAmount,
    val allowedColors: Set<Color>
) : Effect {
    override val description: String = buildString {
        append("Add X mana in any combination of ")
        append(allowedColors.joinToString(" and/or ") { "{${it.symbol}}" })
        append(", where X is ${amountSource.description}")
    }
}

/**
 * Sources for dynamic values in effects.
 */
@Serializable
sealed interface DynamicAmount {
    val description: String

    /**
     * Count of other creatures you control.
     */
    @Serializable
    data object OtherCreaturesYouControl : DynamicAmount {
        override val description: String = "the number of other creatures you control"
    }

    /**
     * Count of creatures you control (including self).
     */
    @Serializable
    data object CreaturesYouControl : DynamicAmount {
        override val description: String = "the number of creatures you control"
    }

    /**
     * Count of all creatures on the battlefield.
     */
    @Serializable
    data object AllCreatures : DynamicAmount {
        override val description: String = "the number of creatures on the battlefield"
    }

    /**
     * Your current life total.
     */
    @Serializable
    data object YourLifeTotal : DynamicAmount {
        override val description: String = "your life total"
    }

    /**
     * Fixed amount (for consistency in the type system).
     */
    @Serializable
    data class Fixed(val amount: Int) : DynamicAmount {
        override val description: String = "$amount"
    }

    /**
     * Count of creatures that entered the battlefield under your control this turn.
     */
    @Serializable
    data object CreaturesEnteredThisTurn : DynamicAmount {
        override val description: String = "the number of creatures that entered the battlefield under your control this turn"
    }

    /**
     * Count of attacking creatures you control.
     */
    @Serializable
    data object AttackingCreaturesYouControl : DynamicAmount {
        override val description: String = "the number of attacking creatures you control"
    }

    /**
     * Count of colors among permanents you control.
     */
    @Serializable
    data object ColorsAmongPermanentsYouControl : DynamicAmount {
        override val description: String = "the number of colors among permanents you control"
    }
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
 * Surveil N - Look at the top N cards of your library, then put any number of them
 * into your graveyard and the rest on top of your library in any order.
 * "Surveil 2"
 */
@Serializable
data class SurveilEffect(
    val count: Int
) : Effect {
    override val description: String = "Surveil $count"
}

/**
 * Scry N - Look at the top N cards of your library, then put any number of them
 * on the bottom of your library and the rest on top in any order.
 * "Scry 2"
 */
@Serializable
data class ScryEffect(
    val count: Int
) : Effect {
    override val description: String = "Scry $count"
}

/**
 * Look at the top X cards of your library where X is determined dynamically,
 * and put any number of cards matching a filter onto the battlefield.
 * Then shuffle.
 *
 * Used for effects like Ajani's ultimate:
 * "Look at the top X cards of your library, where X is your life total.
 *  You may put any number of nonland permanent cards with mana value 3 or less
 *  from among them onto the battlefield. Then shuffle."
 *
 * @property countSource What determines how many cards to look at
 * @property filter Cards matching this filter may be put onto the battlefield
 * @property shuffleAfter Whether to shuffle after (typically true)
 */
@Serializable
data class LookAtTopXPutOntoBattlefieldEffect(
    val countSource: DynamicAmount,
    val filter: CardFilter,
    val shuffleAfter: Boolean = true
) : Effect {
    override val description: String = buildString {
        append("Look at the top X cards of your library, where X is ${countSource.description}. ")
        append("You may put any number of ${filter.description}s from among them onto the battlefield")
        if (shuffleAfter) append(". Then shuffle")
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

    /** Match permanent cards (creature, artifact, enchantment, planeswalker) */
    @Serializable
    data object PermanentCard : CardFilter {
        override val description: String = "permanent card"
    }

    /** Match nonland permanent cards */
    @Serializable
    data object NonlandPermanentCard : CardFilter {
        override val description: String = "nonland permanent card"
    }

    /** Match cards with mana value at most X */
    @Serializable
    data class ManaValueAtMost(val maxManaValue: Int) : CardFilter {
        override val description: String = "card with mana value $maxManaValue or less"
    }

    /** Negation filter - match cards that don't match the inner filter */
    @Serializable
    data class Not(val filter: CardFilter) : CardFilter {
        override val description: String = "non${filter.description}"
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
// Stack Effects
// =============================================================================

/**
 * Counter target spell.
 * "Counter target spell."
 *
 * The countered spell is removed from the stack and placed in its owner's
 * graveyard without resolving (no effects happen).
 */
@Serializable
data object CounterSpellEffect : Effect {
    override val description: String = "Counter target spell"
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

    /** Target card in a graveyard */
    @Serializable
    data object TargetCardInGraveyard : EffectTarget {
        override val description: String = "target card in a graveyard"
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

    /** Target enchantment */
    @Serializable
    data object TargetEnchantment : EffectTarget {
        override val description: String = "target enchantment"
    }

    /** Target artifact */
    @Serializable
    data object TargetArtifact : EffectTarget {
        override val description: String = "target artifact"
    }

    /** Target nonland permanent an opponent controls */
    @Serializable
    data object TargetOpponentNonlandPermanent : EffectTarget {
        override val description: String = "target nonland permanent an opponent controls"
    }

    /** The controller of the target (used for effects like "its controller gains 4 life") */
    @Serializable
    data object TargetController : EffectTarget {
        override val description: String = "its controller"
    }

    /**
     * TARGET BINDING: Refers to a specific target selection from the declaration phase.
     * This solves the ambiguity of which target applies to which effect.
     * * @property index The index of the TargetRequirement in the CardScript.
     */
    @Serializable
    data class ContextTarget(val index: Int) : EffectTarget {
        override val description: String = "target"
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
// Transform Effects
// =============================================================================

/**
 * Transform a double-faced permanent.
 * Toggles between front and back face.
 * "Transform this creature"
 */
@Serializable
data class TransformEffect(
    val target: EffectTarget = EffectTarget.Self
) : Effect {
    override val description: String = "Transform ${target.description}"
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
 */
@Serializable
data class SacrificeUnlessEffect(
    val permanentToSacrifice: EffectTarget,
    val cost: SacrificeCost
) : Effect {
    override val description:
            String = "Sacrifice ${permanentToSacrifice.description} unless you ${cost.description}"
}

// =============================================================================
// Exile and Replace Effects
// =============================================================================

/**
 * Exile a creature and create a token for its controller.
 * Used for effects like Crib Swap: "Exile target creature. Its controller creates a 1/1 token."
 *
 * @property target The creature to exile
 * @property tokenPower Power of the replacement token
 * @property tokenToughness Toughness of the replacement token
 * @property tokenColors Colors of the token (empty for colorless)
 * @property tokenTypes Creature types of the token
 * @property tokenKeywords Keywords the token has
 */
@Serializable
data class ExileAndReplaceWithTokenEffect(
    val target: EffectTarget,
    val tokenPower: Int,
    val tokenToughness: Int,
    val tokenColors: Set<Color> = emptySet(),
    val tokenTypes: Set<String>,
    val tokenKeywords: Set<Keyword> = emptySet()
) : Effect {
    override val description: String = buildString {
        append("Exile ${target.description}. Its controller creates a ")
        append("$tokenPower/$tokenToughness ")
        if (tokenColors.isEmpty()) {
            append("colorless ")
        } else {
            append(tokenColors.joinToString(" and ") { it.displayName.lowercase() })
            append(" ")
        }
        append(tokenTypes.joinToString(" "))
        append(" creature token")
        if (tokenKeywords.isNotEmpty()) {
            append(" with ")
            append(tokenKeywords.joinToString(", ") { it.displayName.lowercase() })
        }
    }
}

// =============================================================================
// Modal/Choice Effects
// =============================================================================

/**
 * Modal spell effect - choose one of several modes.
 * "Choose one — [Mode A] or [Mode B]"
 *
 * @property modes List of possible effects to choose from
 * @property chooseCount How many modes to choose (default 1)
 */
@Serializable
data class ModalEffect(
    val modes: List<Effect>,
    val chooseCount: Int = 1
) : Effect {
    override val description: String = buildString {
        append("Choose ")
        if (chooseCount == 1) {
            append("one")
        } else {
            append(chooseCount)
        }
        append(" —\n")
        modes.forEachIndexed { index, effect ->
            append("• ")
            append(effect.description)
            if (index < modes.lastIndex) append("\n")
        }
    }
}

// =============================================================================
// Graveyard Activation Effects
// =============================================================================

/**
 * Effect that can be activated from the graveyard.
 * Used for cards like Goldmeadow Nomad with graveyard abilities.
 * Note: This is typically handled as an activated ability, not a spell effect.
 */
@Serializable
data class CreateTokenFromGraveyardEffect(
    val power: Int,
    val toughness: Int,
    val colors: Set<Color>,
    val creatureTypes: Set<String>
) : Effect {
    override val description: String = buildString {
        append("Create a $power/$toughness ")
        append(colors.joinToString(" and ") { it.displayName.lowercase() })
        append(" ")
        append(creatureTypes.joinToString(" "))
        append(" creature token")
    }
}

// =============================================================================
// Mass Transformation Effects
// =============================================================================

/**
 * Effect that transforms multiple creatures at once.
 * Used for effects like Curious Colossus: "each creature target opponent controls
 * loses all abilities, becomes a Coward, and has base P/T 1/1."
 *
 * @property target Which creatures are affected
 * @property loseAllAbilities If true, creatures lose all abilities
 * @property addCreatureType Type to add (if any)
 * @property setBasePower New base power (if set)
 * @property setBaseToughness New base toughness (if set)
 */
@Serializable
data class TransformAllCreaturesEffect(
    val target: EffectTarget,
    val loseAllAbilities: Boolean = false,
    val addCreatureType: String? = null,
    val setBasePower: Int? = null,
    val setBaseToughness: Int? = null
) : Effect {
    override val description: String = buildString {
        append("Each creature ${target.description}")
        val effects = mutableListOf<String>()
        if (loseAllAbilities) effects.add("loses all abilities")
        if (addCreatureType != null) effects.add("becomes a $addCreatureType in addition to its other types")
        if (setBasePower != null && setBaseToughness != null) {
            effects.add("has base power and toughness $setBasePower/$setBaseToughness")
        }
        append(" ")
        append(effects.joinToString(", "))
    }
}

// =============================================================================
// Exile Until Leaves Effects
// =============================================================================

/**
 * Exile a permanent until this permanent leaves the battlefield.
 * Used for O-Ring style effects like Liminal Hold.
 */
@Serializable
data class ExileUntilLeavesEffect(
    val target: EffectTarget
) : Effect {
    override val description: String =
        "Exile ${target.description} until this permanent leaves the battlefield"
}

// =============================================================================
// Put From Hand Effects
// =============================================================================

/**
 * Put a creature card from your hand onto the battlefield.
 * Used for effects like Kinscaer Sentry.
 */
@Serializable
data class PutCreatureFromHandOntoBattlefieldEffect(
    val maxManaValueSource: DynamicAmount,
    val entersTapped: Boolean = false,
    val entersAttacking: Boolean = false
) : Effect {
    override val description: String = buildString {
        append("Put a creature card with mana value ${maxManaValueSource.description} or less ")
        append("from your hand onto the battlefield")
        if (entersTapped) append(" tapped")
        if (entersAttacking) append(" and attacking")
    }
}

