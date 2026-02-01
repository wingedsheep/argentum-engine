package com.wingedsheep.sdk.scripting

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.targeting.PermanentTargetFilter
import com.wingedsheep.sdk.targeting.TargetRequirement
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
    val amount: DynamicAmount,
    val target: EffectTarget = EffectTarget.Controller
) : Effect {
    /** Convenience constructor for fixed amounts */
    constructor(amount: Int, target: EffectTarget = EffectTarget.Controller) : this(DynamicAmount.Fixed(amount), target)

    override val description: String = when (target) {
        EffectTarget.Controller -> "You gain ${amount.description} life"
        EffectTarget.Opponent -> "Target opponent gains ${amount.description} life"
        EffectTarget.AnyPlayer -> "Target player gains ${amount.description} life"
        else -> "Gain ${amount.description} life"
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

/**
 * Deal dynamic damage to a target.
 * "Deal damage equal to X to target"
 * Used for effects like Final Strike where damage depends on a dynamic value.
 */
@Serializable
data class DealDynamicDamageEffect(
    val amount: DynamicAmount,
    val target: EffectTarget
) : Effect {
    override val description: String = "Deal damage equal to ${amount.description} to ${target.description}"
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
    val count: DynamicAmount,
    val target: EffectTarget = EffectTarget.Controller
) : Effect {
    /** Convenience constructor for fixed amounts */
    constructor(count: Int, target: EffectTarget = EffectTarget.Controller) : this(DynamicAmount.Fixed(count), target)

    override val description: String = when (target) {
        EffectTarget.Controller -> when (val c = count) {
            is DynamicAmount.Fixed -> "Draw ${if (c.amount == 1) "a card" else "${c.amount} cards"}"
            else -> "Draw cards equal to ${c.description}"
        }
        EffectTarget.Opponent -> "Target opponent draws cards equal to ${count.description}"
        else -> "Target player draws cards equal to ${count.description}"
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

/**
 * Discard cards at random effect.
 * "Target opponent discards a card at random"
 * Used for cards like Mind Knives.
 */
@Serializable
data class DiscardRandomEffect(
    val count: Int = 1,
    val target: EffectTarget = EffectTarget.Opponent
) : Effect {
    override val description: String = when (target) {
        EffectTarget.Controller -> "Discard ${if (count == 1) "a card" else "$count cards"} at random"
        EffectTarget.Opponent -> "Target opponent discards ${if (count == 1) "a card" else "$count cards"} at random"
        else -> "Target player discards ${if (count == 1) "a card" else "$count cards"} at random"
    }
}

/**
 * Each opponent discards cards.
 * "Each opponent discards a card."
 * Used for cards like Noxious Toad.
 */
@Serializable
data class EachOpponentDiscardsEffect(
    val count: Int = 1
) : Effect {
    override val description: String = "Each opponent discards ${if (count == 1) "a card" else "$count cards"}"
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
    val duration: Duration = Duration.EndOfTurn
) : Effect {
    override val description: String = buildString {
        append("${target.description} gets ")
        append(if (powerModifier >= 0) "+$powerModifier" else "$powerModifier")
        append("/")
        append(if (toughnessModifier >= 0) "+$toughnessModifier" else "$toughnessModifier")
        if (duration.description.isNotEmpty()) append(" ${duration.description}")
    }
}

/**
 * Remove all creature types from a target creature.
 * "Target creature loses all creature types until end of turn"
 */
@Serializable
data class LoseAllCreatureTypesEffect(
    val target: EffectTarget,
    val duration: Duration = Duration.EndOfTurn
) : Effect {
    override val description: String = buildString {
        append("${target.description} loses all creature types")
        if (duration.description.isNotEmpty()) append(" ${duration.description}")
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
 * Add one mana of any color effect.
 * "{T}: Add one mana of any color."
 *
 * The player chooses the color when this ability resolves.
 */
@Serializable
data class AddAnyColorManaEffect(
    val amount: Int = 1
) : Effect {
    override val description: String = if (amount == 1) {
        "Add one mana of any color"
    } else {
        "Add $amount mana of any color"
    }
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
     * Count of other creatures with a specific subtype you control.
     * "the number of other Goblins you control"
     */
    @Serializable
    data class OtherCreaturesWithSubtypeYouControl(val subtype: Subtype) : DynamicAmount {
        override val description: String = "the number of other ${subtype.value}s you control"
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
     * Count of all creatures with a specific subtype on the battlefield.
     * Used for tribal effects like Wellwisher ("for each Elf on the battlefield").
     */
    @Serializable
    data class CreaturesWithSubtypeOnBattlefield(val subtype: Subtype) : DynamicAmount {
        override val description: String = "the number of ${subtype.value}s on the battlefield"
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

    // =========================================================================
    // Graveyard-based DynamicAmounts (for Tarmogoyf, etc.)
    // =========================================================================

    /**
     * Count of card types among cards in all graveyards.
     * Used for Tarmogoyf's characteristic-defining ability.
     */
    @Serializable
    data object CardTypesInAllGraveyards : DynamicAmount {
        override val description: String = "the number of card types among cards in all graveyards"
    }

    /**
     * Count of cards in your graveyard.
     * Used for creatures like Lhurgoyf.
     */
    @Serializable
    data object CardsInYourGraveyard : DynamicAmount {
        override val description: String = "the number of cards in your graveyard"
    }

    /**
     * Count of creature cards in your graveyard.
     */
    @Serializable
    data object CreatureCardsInYourGraveyard : DynamicAmount {
        override val description: String = "the number of creature cards in your graveyard"
    }

    /**
     * Count of lands you control.
     */
    @Serializable
    data object LandsYouControl : DynamicAmount {
        override val description: String = "the number of lands you control"
    }

    /**
     * Count of lands with a specific subtype you control.
     * "the number of Mountains you control"
     */
    @Serializable
    data class LandsWithSubtypeYouControl(val subtype: Subtype) : DynamicAmount {
        override val description: String = "the number of ${subtype.value}s you control"
    }

    // =========================================================================
    // X Value and Variable References
    // =========================================================================

    /**
     * The X value of the spell (from mana cost).
     * Used for X spells like Fireball.
     */
    @Serializable
    data object XValue : DynamicAmount {
        override val description: String = "X"
    }

    /**
     * Reference to a stored variable by name.
     * Used for effects that need to reference a previously computed/stored value.
     * Example: Scapeshift stores "sacrificedCount" and SearchLibrary reads it.
     */
    @Serializable
    data class VariableReference(val variableName: String) : DynamicAmount {
        override val description: String = "the stored $variableName"
    }

    // =========================================================================
    // Opponent-relative DynamicAmounts
    // =========================================================================

    /**
     * Count of creatures attacking you, optionally with a multiplier.
     * Used for effects like Blessed Reversal ("gain 3 life for each creature attacking you").
     */
    @Serializable
    data class CreaturesAttackingYou(val multiplier: Int = 1) : DynamicAmount {
        override val description: String = if (multiplier == 1) {
            "the number of creatures attacking you"
        } else {
            "$multiplier for each creature attacking you"
        }
    }

    /**
     * Count of lands of a specific type target opponent controls.
     * Used for color-hosers like Renewing Dawn.
     */
    @Serializable
    data class LandsOfTypeTargetOpponentControls(
        val landType: String,
        val multiplier: Int = 1
    ) : DynamicAmount {
        override val description: String = if (multiplier == 1) {
            "the number of ${landType}s target opponent controls"
        } else {
            "$multiplier for each $landType target opponent controls"
        }
    }

    /**
     * Count of creatures of a specific color target opponent controls.
     * Used for color-hosers like Starlight.
     */
    @Serializable
    data class CreaturesOfColorTargetOpponentControls(
        val color: Color,
        val multiplier: Int = 1
    ) : DynamicAmount {
        override val description: String = if (multiplier == 1) {
            "the number of ${color.displayName.lowercase()} creatures target opponent controls"
        } else {
            "$multiplier for each ${color.displayName.lowercase()} creature target opponent controls"
        }
    }

    /**
     * Difference in hand sizes between target opponent and you (if positive).
     * Used for effects like Balance of Power.
     */
    @Serializable
    data object HandSizeDifferenceFromTargetOpponent : DynamicAmount {
        override val description: String = "the difference if target opponent has more cards in hand than you"
    }

    /**
     * Number of tapped creatures target opponent controls.
     * Used for Theft of Dreams.
     * @deprecated Use CountInZone with appropriate filter instead
     */
    @Serializable
    data object TappedCreaturesTargetOpponentControls : DynamicAmount {
        override val description: String = "the number of tapped creatures target opponent controls"
    }

    // =========================================================================
    // Math Operations - Composable arithmetic on DynamicAmounts
    // =========================================================================

    /**
     * Add two dynamic amounts.
     * Example: Add(Fixed(2), CreaturesYouControl) = "2 + creatures you control"
     */
    @Serializable
    data class Add(val left: DynamicAmount, val right: DynamicAmount) : DynamicAmount {
        override val description: String = "(${left.description} + ${right.description})"
    }

    /**
     * Subtract one dynamic amount from another.
     * Example: Subtract(CardsInHand(Opponent), CardsInHand(Controller))
     * Replaces HandSizeDifferenceFromTargetOpponent
     */
    @Serializable
    data class Subtract(val left: DynamicAmount, val right: DynamicAmount) : DynamicAmount {
        override val description: String = "(${left.description} - ${right.description})"
    }

    /**
     * Multiply a dynamic amount by a fixed multiplier.
     * Example: Multiply(CreaturesAttackingYou, 3) for Blessed Reversal
     */
    @Serializable
    data class Multiply(val amount: DynamicAmount, val multiplier: Int) : DynamicAmount {
        override val description: String = "$multiplier × ${amount.description}"
    }

    /**
     * Take the maximum of zero and the amount (clamp negative to zero).
     * Useful for difference calculations that should not go negative.
     * Example: IfPositive(Subtract(opponentHand, yourHand))
     */
    @Serializable
    data class IfPositive(val amount: DynamicAmount) : DynamicAmount {
        override val description: String = "${amount.description} (if positive)"
    }

    /**
     * Maximum of two amounts.
     */
    @Serializable
    data class Max(val left: DynamicAmount, val right: DynamicAmount) : DynamicAmount {
        override val description: String = "max(${left.description}, ${right.description})"
    }

    /**
     * Minimum of two amounts.
     */
    @Serializable
    data class Min(val left: DynamicAmount, val right: DynamicAmount) : DynamicAmount {
        override val description: String = "min(${left.description}, ${right.description})"
    }

    // =========================================================================
    // Context-based Values - Values from cost payment or trigger context
    // =========================================================================

    /**
     * Power of a creature that was sacrificed as an additional cost.
     * Used for effects like Final Strike: "Deal damage equal to that creature's power"
     */
    @Serializable
    data object SacrificedPermanentPower : DynamicAmount {
        override val description: String = "the sacrificed creature's power"
    }

    /**
     * Toughness of a creature that was sacrificed as an additional cost.
     */
    @Serializable
    data object SacrificedPermanentToughness : DynamicAmount {
        override val description: String = "the sacrificed creature's toughness"
    }

    // =========================================================================
    // Zone-based Counting - General counting with filters
    // =========================================================================

    /**
     * Count cards in a zone matching a filter.
     * This is the general-purpose counting primitive that replaces many specific amounts.
     *
     * Examples:
     * - CountInZone(Controller, Hand) = cards in your hand
     * - CountInZone(Opponent, Hand) = cards in opponent's hand
     * - CountInZone(Controller, Battlefield, PermanentFilter.Creature) = creatures you control
     * - CountInZone(Opponent, Battlefield, PermanentFilter.TappedCreature) = tapped creatures opponent controls
     *
     * @param player Whose zone to count in
     * @param zone Which zone to count
     * @param filter Optional filter for what to count
     */
    @Serializable
    data class CountInZone(
        val player: PlayerReference,
        val zone: ZoneReference,
        val filter: CountFilter = CountFilter.Any
    ) : DynamicAmount {
        override val description: String = buildString {
            append("the number of ")
            if (filter != CountFilter.Any) {
                append(filter.description)
                append(" ")
            }
            append("cards in ")
            append(player.description)
            append("'s ")
            append(zone.description)
        }
    }

    /**
     * Count permanents on battlefield matching a filter.
     * Convenience wrapper for CountInZone with battlefield.
     */
    @Serializable
    data class CountPermanents(
        val controller: PlayerReference,
        val filter: CountFilter = CountFilter.Any
    ) : DynamicAmount {
        override val description: String = buildString {
            append("the number of ")
            if (filter != CountFilter.Any) {
                append(filter.description)
                append(" ")
            }
            append(controller.description)
            append(" controls")
        }
    }
}

// =============================================================================
// Player Reference - Who we're referring to
// =============================================================================

/**
 * Reference to a player in zone counting and similar contexts.
 */
@Serializable
sealed interface PlayerReference {
    val description: String

    @Serializable
    data object You : PlayerReference {
        override val description: String = "your"
    }

    @Serializable
    data object Opponent : PlayerReference {
        override val description: String = "opponent"
    }

    @Serializable
    data object TargetOpponent : PlayerReference {
        override val description: String = "target opponent"
    }

    @Serializable
    data object TargetPlayer : PlayerReference {
        override val description: String = "target player"
    }

    @Serializable
    data object Each : PlayerReference {
        override val description: String = "each player"
    }
}

// =============================================================================
// Zone Reference - Which zone we're referring to
// =============================================================================

/**
 * Reference to a game zone.
 */
@Serializable
sealed interface ZoneReference {
    val description: String

    @Serializable
    data object Hand : ZoneReference {
        override val description: String = "hand"
    }

    @Serializable
    data object Battlefield : ZoneReference {
        override val description: String = "battlefield"
    }

    @Serializable
    data object Graveyard : ZoneReference {
        override val description: String = "graveyard"
    }

    @Serializable
    data object Library : ZoneReference {
        override val description: String = "library"
    }

    @Serializable
    data object Exile : ZoneReference {
        override val description: String = "exile"
    }
}

// =============================================================================
// Count Filter - What to count
// =============================================================================

/**
 * Filter for counting cards/permanents.
 * This is the universal filter used in CountInZone and similar.
 */
@Serializable
sealed interface CountFilter {
    val description: String

    @Serializable
    data object Any : CountFilter {
        override val description: String = ""
    }

    @Serializable
    data object Creatures : CountFilter {
        override val description: String = "creature"
    }

    @Serializable
    data object TappedCreatures : CountFilter {
        override val description: String = "tapped creature"
    }

    @Serializable
    data object UntappedCreatures : CountFilter {
        override val description: String = "untapped creature"
    }

    @Serializable
    data object Lands : CountFilter {
        override val description: String = "land"
    }

    @Serializable
    data class LandType(val landType: String) : CountFilter {
        override val description: String = landType
    }

    @Serializable
    data class CreatureColor(val color: Color) : CountFilter {
        override val description: String = "${color.displayName.lowercase()} creature"
    }

    @Serializable
    data class CardColor(val color: Color) : CountFilter {
        override val description: String = "${color.displayName.lowercase()} card"
    }

    @Serializable
    data class HasSubtype(val subtype: String) : CountFilter {
        override val description: String = subtype
    }

    @Serializable
    data object AttackingCreatures : CountFilter {
        override val description: String = "attacking creature"
    }

    /**
     * Combine multiple filters with AND logic.
     */
    @Serializable
    data class And(val filters: List<CountFilter>) : CountFilter {
        override val description: String = filters.joinToString(" ") { it.description }
    }

    /**
     * Combine multiple filters with OR logic.
     */
    @Serializable
    data class Or(val filters: List<CountFilter>) : CountFilter {
        override val description: String = filters.joinToString(" or ") { it.description }
    }
}

// =============================================================================
// Effect Variables (for context binding between effects)
// =============================================================================

/**
 * Represents a variable that can store values during effect execution.
 *
 * This enables effects to:
 * 1. Store the result of an action (e.g., which card was exiled)
 * 2. Store a count (e.g., how many lands were sacrificed)
 * 3. Reference these stored values in subsequent effects
 *
 * Usage:
 * ```kotlin
 * // Store the exiled card reference
 * StoreResultEffect(
 *     effect = ExileEffect(EffectTarget.ContextTarget(0)),
 *     storeAs = EffectVariable.EntityRef("exiledCard")
 * )
 *
 * // Later, return it from exile
 * ReturnFromExileEffect(StoredEntityTarget("exiledCard"))
 * ```
 */
@Serializable
sealed interface EffectVariable {
    /** Name used to reference this variable */
    val name: String

    /** Human-readable description */
    val description: String

    /**
     * Stores a reference to an entity (card, permanent, player).
     * Used for: "exile target creature... return the exiled card"
     */
    @Serializable
    data class EntityRef(override val name: String) : EffectVariable {
        override val description: String = "the $name"
    }

    /**
     * Stores a count/number.
     * Used for: "sacrifice any number of lands... search for that many lands"
     */
    @Serializable
    data class Count(override val name: String) : EffectVariable {
        override val description: String = "the number of $name"
    }

    /**
     * Stores an amount (damage dealt, life gained, etc.).
     * Used for: "deal damage equal to the damage dealt this way"
     */
    @Serializable
    data class Amount(override val name: String) : EffectVariable {
        override val description: String = "the $name amount"
    }
}

/**
 * Effect that stores the result of executing an inner effect.
 *
 * This enables Oblivion Ring-style effects where the first trigger
 * needs to remember which card it exiled so the second trigger
 * can return it.
 *
 * @param effect The effect to execute
 * @param storeAs The variable to store the result in
 */
@Serializable
data class StoreResultEffect(
    val effect: Effect,
    val storeAs: EffectVariable
) : Effect {
    override val description: String = "${effect.description} (stored as ${storeAs.name})"
}

/**
 * Effect that stores a count from the result of executing an effect.
 *
 * Used for variable-count effects like Scapeshift:
 * "Sacrifice any number of lands. Search for that many land cards."
 *
 * @param effect The effect to execute (typically a sacrifice or similar)
 * @param storeAs The count variable to store the number in
 */
@Serializable
data class StoreCountEffect(
    val effect: Effect,
    val storeAs: EffectVariable.Count
) : Effect {
    override val description: String = "${effect.description} (count stored as ${storeAs.name})"
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

/**
 * Create Treasure artifact tokens.
 * Treasure tokens have "{T}, Sacrifice this artifact: Add one mana of any color."
 */
@Serializable
data class CreateTreasureTokensEffect(
    val count: Int = 1
) : Effect {
    override val description: String = if (count == 1) {
        "Create a Treasure token"
    } else {
        "Create $count Treasure tokens"
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
// Atomic / Compositional Effects
// =============================================================================

/**
 * Optional effect wrapper - the player may choose to perform or skip this effect.
 * "You may draw a card" or "You may shuffle your library"
 *
 * Use this to compose optional parts of abilities rather than creating
 * specific optional variants of each effect.
 */
@Serializable
data class MayEffect(
    val effect: Effect,
    val description_override: String? = null
) : Effect {
    override val description: String = description_override ?: "You may ${effect.description.lowercase()}"
}

/**
 * Reveal a player's hand (publicly visible to all players).
 * This is an atomic effect that just reveals - use with CompositeEffect for
 * "reveal and do something based on what's revealed" patterns.
 *
 * Example: Baleful Stare = CompositeEffect(RevealHandEffect, DrawCardsEffect(count))
 */
@Serializable
data class RevealHandEffect(
    val target: EffectTarget = EffectTarget.ContextTarget(0)
) : Effect {
    override val description: String = when (target) {
        is EffectTarget.ContextTarget -> "Target opponent reveals their hand"
        EffectTarget.Opponent -> "Target opponent reveals their hand"
        EffectTarget.Controller -> "Reveal your hand"
        else -> "Target player reveals their hand"
    }
}

/**
 * Look at the top N cards of a library and put them back in any order.
 * This is the atomic "scry-like" or "look and reorder" primitive.
 *
 * Use with CompositeEffect for patterns like Omen:
 * CompositeEffect(LookAtTopAndReorderEffect(3), MayEffect(ShuffleLibraryEffect()), DrawCardsEffect(1))
 */
@Serializable
data class LookAtTopAndReorderEffect(
    val count: Int,
    val target: EffectTarget = EffectTarget.Controller
) : Effect {
    override val description: String = "Look at the top $count cards of your library and put them back in any order"
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
 * Mill N - Put the top N cards of a player's library into their graveyard.
 * "Mill 3" or "Target player mills 3 cards"
 */
@Serializable
data class MillEffect(
    val count: Int,
    val target: EffectTarget = EffectTarget.Controller
) : Effect {
    override val description: String = when (target) {
        is EffectTarget.Controller -> "Mill $count"
        is EffectTarget.AnyPlayer -> "Target player mills $count"
        is EffectTarget.EachOpponent -> "Each opponent mills $count"
        is EffectTarget.Opponent -> "Target opponent mills $count"
        else -> "${target.description} mills $count"
    }
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
 * Destroy all permanents matching a filter.
 * Unified effect that handles all "destroy all X" patterns.
 *
 * Examples:
 * - DestroyAllEffect(PermanentTargetFilter.Land) -> "Destroy all lands"
 * - DestroyAllEffect(PermanentTargetFilter.Creature, noRegenerate = true) -> Wrath of God
 * - DestroyAllEffect(PermanentTargetFilter.And(listOf(Creature, WithColor(WHITE)))) -> Virtue's Ruin
 * - DestroyAllEffect(PermanentTargetFilter.And(listOf(Land, WithSubtype(ISLAND)))) -> Boiling Seas
 *
 * @param filter Which permanents to destroy (defaults to Any = all permanents)
 * @param noRegenerate If true, destroyed permanents can't be regenerated (for future regeneration support)
 */
@Serializable
data class DestroyAllEffect(
    val filter: PermanentTargetFilter = PermanentTargetFilter.Any,
    val noRegenerate: Boolean = false
) : Effect {
    override val description: String = buildString {
        append("Destroy all ")
        when (filter) {
            is PermanentTargetFilter.Any -> append("permanents")
            is PermanentTargetFilter.Creature -> append("creatures")
            is PermanentTargetFilter.Land -> append("lands")
            is PermanentTargetFilter.CreatureOrLand -> append("creatures and lands")
            else -> append(filter.description).append("s")
        }
        if (noRegenerate) append(". They can't be regenerated")
    }
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
 * Deal damage to a group of creatures matching a filter.
 * Use with .then(DealDamageToPlayersEffect) for effects that also damage players.
 *
 * Examples:
 * - Pyroclasm: DealDamageToGroupEffect(2)
 * - Needle Storm: DealDamageToGroupEffect(4, CreatureDamageFilter.WithKeyword(Keyword.FLYING))
 * - Earthquake: DealDamageToGroupEffect(DynamicAmount.XValue, CreatureDamageFilter.WithoutKeyword(Keyword.FLYING))
 *                  .then(DealDamageToPlayersEffect(DynamicAmount.XValue))
 *
 * @param amount The amount of damage to deal (can be fixed or dynamic like X)
 * @param filter Which creatures are damaged (All = all creatures)
 */
@Serializable
data class DealDamageToGroupEffect(
    val amount: DynamicAmount,
    val filter: CreatureDamageFilter = CreatureDamageFilter.All
) : Effect {
    constructor(
        amount: Int,
        filter: CreatureDamageFilter = CreatureDamageFilter.All
    ) : this(DynamicAmount.Fixed(amount), filter)

    override val description: String = "Deal ${amount.description} damage to ${filter.description}"
}

/**
 * Deal damage to players.
 * Can target each player, controller only, opponent only, or each opponent.
 * Often composed with DealDamageToGroupEffect for effects like Earthquake.
 *
 * Examples:
 * - Earthquake: DealDamageToGroupEffect(...).then(DealDamageToPlayersEffect(DynamicAmount.XValue))
 * - Fire Tempest: DealDamageToGroupEffect(6).then(DealDamageToPlayersEffect(6))
 * - Flame Rift: DealDamageToPlayersEffect(4) (just players, no creatures)
 *
 * @param amount The amount of damage to deal (can be fixed or dynamic like X)
 * @param target Which players to damage (EachPlayer, Controller, Opponent, EachOpponent)
 */
@Serializable
data class DealDamageToPlayersEffect(
    val amount: DynamicAmount,
    val target: EffectTarget = EffectTarget.EachPlayer
) : Effect {
    constructor(amount: Int, target: EffectTarget = EffectTarget.EachPlayer) : this(DynamicAmount.Fixed(amount), target)

    override val description: String = when (target) {
        EffectTarget.EachPlayer -> "Deal ${amount.description} damage to each player"
        EffectTarget.Controller -> "Deal ${amount.description} damage to you"
        EffectTarget.Opponent -> "Deal ${amount.description} damage to target opponent"
        EffectTarget.EachOpponent -> "Deal ${amount.description} damage to each opponent"
        else -> "Deal ${amount.description} damage to ${target.description}"
    }
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

    /** The creature enchanted by this aura */
    @Serializable
    data object EnchantedCreature : EffectTarget {
        override val description: String = "enchanted creature"
    }

    /** The controller of the target (used for effects like "its controller gains 4 life") */
    @Serializable
    data object TargetController : EffectTarget {
        override val description: String = "its controller"
    }

    /** Target creature with flying */
    @Serializable
    data object TargetCreatureWithFlying : EffectTarget {
        override val description: String = "target creature with flying"
    }

    /**
     * TARGET BINDING: Refers to a specific target selection from the declaration phase.
     * This solves the ambiguity of which target applies to which effect.
     * @property index The index of the TargetRequirement in the CardScript.
     */
    @Serializable
    data class ContextTarget(val index: Int) : EffectTarget {
        override val description: String = "target"
    }

    /**
     * VARIABLE BINDING: Refers to an entity stored in a variable during effect execution.
     *
     * This enables Oblivion Ring-style effects:
     * - First trigger exiles a creature and stores it: `StoreResultEffect(exile, "exiledCard")`
     * - Second trigger returns it: `ReturnFromExileEffect(StoredEntityTarget("exiledCard"))`
     *
     * @property variableName The name of the variable holding the entity reference.
     */
    @Serializable
    data class StoredEntityTarget(val variableName: String) : EffectTarget {
        override val description: String = "the stored $variableName"
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
 * Represents a single mode in a modal spell.
 *
 * Each mode can have its own targeting requirements, allowing cards like
 * Cryptic Command where different modes need different targets.
 *
 * @property effect The effect when this mode is chosen
 * @property targetRequirements Targets required for this specific mode
 * @property description Human-readable description of the mode
 */
@Serializable
data class Mode(
    val effect: Effect,
    val targetRequirements: List<TargetRequirement> = emptyList(),
    val description: String = effect.description
) {
    companion object {
        /**
         * Create a mode with no targeting.
         */
        fun noTarget(effect: Effect, description: String = effect.description): Mode =
            Mode(effect, emptyList(), description)

        /**
         * Create a mode with a single target.
         */
        fun withTarget(effect: Effect, target: TargetRequirement, description: String = effect.description): Mode =
            Mode(effect, listOf(target), description)
    }
}

/**
 * Modal spell effect - choose one or more of several modes.
 * "Choose one — [Mode A] or [Mode B]"
 * "Choose two — [Mode A], [Mode B], [Mode C], or [Mode D]"
 *
 * Each mode can have its own targeting requirements, which are combined
 * based on which modes are chosen when the spell is cast.
 *
 * Example (Cryptic Command):
 * ```kotlin
 * ModalEffect(
 *     modes = listOf(
 *         Mode.withTarget(CounterSpellEffect, TargetSpell(), "Counter target spell"),
 *         Mode.withTarget(ReturnToHandEffect(EffectTarget.ContextTarget(0)), TargetPermanent(), "Return target permanent to its owner's hand"),
 *         Mode.noTarget(TapAllCreaturesEffect(CreatureGroupFilter.OpponentsControl), "Tap all creatures your opponents control"),
 *         Mode.noTarget(DrawCardsEffect(1), "Draw a card")
 *     ),
 *     chooseCount = 2
 * )
 * ```
 *
 * @property modes List of possible modes to choose from
 * @property chooseCount How many modes to choose (default 1)
 */
@Serializable
data class ModalEffect(
    val modes: List<Mode>,
    val chooseCount: Int = 1
) : Effect {
    override val description: String = buildString {
        append("Choose ")
        when (chooseCount) {
            1 -> append("one")
            2 -> append("two")
            3 -> append("three")
            else -> append(chooseCount)
        }
        append(" —\n")
        modes.forEachIndexed { index, mode ->
            append("• ")
            append(mode.description)
            if (index < modes.lastIndex) append("\n")
        }
    }

    companion object {
        /**
         * Create a simple modal effect with effects that have no targeting.
         * Backwards compatible with the old List<Effect> pattern.
         */
        fun simple(effects: List<Effect>, chooseCount: Int = 1): ModalEffect =
            ModalEffect(effects.map { Mode.noTarget(it) }, chooseCount)

        /**
         * Create a choose-one modal effect.
         */
        fun chooseOne(vararg modes: Mode): ModalEffect =
            ModalEffect(modes.toList(), 1)

        /**
         * Create a choose-two modal effect (Cryptic Command style).
         */
        fun chooseTwo(vararg modes: Mode): ModalEffect =
            ModalEffect(modes.toList(), 2)
    }
}

// =============================================================================
// Optional Cost Effects
// =============================================================================

/**
 * Effect with an optional cost - "You may [cost]. If you do, [ifPaid]."
 *
 * This is the fundamental building block for optional effects like:
 * - "You may pay {2}. If you do, draw a card."
 * - "You may sacrifice a creature. If you do, deal 3 damage to any target."
 * - "You may discard a card. If you do, draw two cards."
 *
 * @property cost The optional cost the player may pay (e.g., PayLifeEffect, SacrificeEffect)
 * @property ifPaid The effect that happens if the player pays the cost
 * @property ifNotPaid Optional effect if the player doesn't pay (usually null)
 */
@Serializable
data class OptionalCostEffect(
    val cost: Effect,
    val ifPaid: Effect,
    val ifNotPaid: Effect? = null
) : Effect {
    override val description: String = buildString {
        append("You may ${cost.description.replaceFirstChar { it.lowercase() }}. ")
        append("If you do, ${ifPaid.description.replaceFirstChar { it.lowercase() }}")
        if (ifNotPaid != null) {
            append(". Otherwise, ${ifNotPaid.description.replaceFirstChar { it.lowercase() }}")
        }
    }
}

/**
 * Pay life cost effect.
 * Used as a cost in OptionalCostEffect.
 */
@Serializable
data class PayLifeEffect(
    val amount: Int
) : Effect {
    override val description: String = "pay $amount life"
}

/**
 * Sacrifice permanents effect.
 * Can be used as a cost or standalone effect.
 *
 * @property filter Which permanents can be sacrificed
 * @property count How many to sacrifice
 * @property any If true, "any number" (for Scapeshift)
 */
@Serializable
data class SacrificeEffect(
    val filter: CardFilter,
    val count: Int = 1,
    val any: Boolean = false
) : Effect {
    override val description: String = buildString {
        append("sacrifice ")
        when {
            any -> append("any number of ${filter.description}s")
            count == 1 -> append("a ${filter.description}")
            else -> append("$count ${filter.description}s")
        }
    }
}

/**
 * Sacrifice the source permanent (self).
 * "Sacrifice this creature" or "Sacrifice this permanent"
 *
 * Used primarily as the suffer effect in PayOrSufferEffect for punisher mechanics.
 */
@Serializable
data object SacrificeSelfEffect : Effect {
    override val description: String = "sacrifice this permanent"
}

/**
 * Reflexive trigger - "When you do, [effect]."
 *
 * Used for abilities that trigger from the resolution of another effect.
 * Example: Heart-Piercer Manticore - "You may sacrifice another creature.
 *          When you do, deal damage equal to that creature's power."
 *
 * @property action The optional action (sacrifice, discard, etc.)
 * @property optional Whether the action is optional
 * @property reflexiveEffect The effect that happens "when you do"
 */
@Serializable
data class ReflexiveTriggerEffect(
    val action: Effect,
    val optional: Boolean = true,
    val reflexiveEffect: Effect
) : Effect {
    override val description: String = buildString {
        if (optional) append("You may ")
        append(action.description.replaceFirstChar { it.lowercase() })
        append(". When you do, ")
        append(reflexiveEffect.description.replaceFirstChar { it.lowercase() })
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

// =============================================================================
// Blight Effects (Lorwyn Eclipsed)
// =============================================================================

/**
 * Blight effect - "may blight N. If you do, [effect]"
 * Blight N means "put N -1/-1 counters on a creature you control".
 * This is an optional cost-gated effect used in triggered abilities.
 *
 * The player may choose a creature they control to blight. If they do,
 * the inner effect happens. If they don't (or can't), nothing happens.
 *
 * @property blightAmount Number of -1/-1 counters to place
 * @property innerEffect The effect that happens if the player blights
 * @property targetId The creature chosen to receive the counters (filled in during resolution)
 */
@Serializable
data class BlightEffect(
    val blightAmount: Int,
    val innerEffect: Effect,
    val targetId: EntityId? = null
) : Effect {
    override val description: String = buildString {
        append("You may blight $blightAmount. If you do, ")
        append(innerEffect.description.replaceFirstChar { it.lowercase() })
    }
}

/**
 * Put -1/-1 counters on a creature.
 * Used for blight effects and wither-style damage.
 *
 * @property count Number of -1/-1 counters to place
 * @property target The creature to receive the counters
 */
@Serializable
data class AddMinusCountersEffect(
    val count: Int,
    val target: EffectTarget
) : Effect {
    override val description: String =
        "Put $count -1/-1 counter${if (count != 1) "s" else ""} on ${target.description}"
}

// =============================================================================
// Optional Cost-Gated Effects
// =============================================================================

/**
 * "May tap another untapped creature you control. If you do, [effect]."
 * This is an optional cost-gated effect - the player may pay the cost to get the effect.
 *
 * @property innerEffect The effect that happens if the player pays the tap cost
 * @property targetId The creature chosen to tap (filled in during resolution)
 */
@Serializable
data class TapCreatureForEffectEffect(
    val innerEffect: Effect,
    val targetId: EntityId? = null
) : Effect {
    override val description: String = buildString {
        append("You may tap another untapped creature you control. If you do, ")
        append(innerEffect.description.replaceFirstChar { it.lowercase() })
    }
}

/**
 * Deal damage with a replacement effect: if the creature would die this turn, exile it instead.
 * Used for cards like Feed the Flames.
 *
 * This combines damage dealing with a death-replacement effect that lasts until end of turn.
 *
 * @property amount The amount of damage to deal
 * @property target The creature to target
 */
@Serializable
data class DealDamageExileOnDeathEffect(
    val amount: Int,
    val target: EffectTarget
) : Effect {
    override val description: String = buildString {
        append("Deal $amount damage to ${target.description}. ")
        append("If that creature would die this turn, exile it instead")
    }
}

// =============================================================================
// Life Payment Effects
// =============================================================================

/**
 * Lose half your life, rounded up.
 * Used for cards like Cruel Bargain.
 */
@Serializable
data class LoseHalfLifeEffect(
    val roundUp: Boolean = true,
    val target: EffectTarget = EffectTarget.Controller
) : Effect {
    override val description: String = when (target) {
        EffectTarget.Controller -> "You lose half your life${if (roundUp) ", rounded up" else ", rounded down"}"
        else -> "${target.description} loses half their life${if (roundUp) ", rounded up" else ", rounded down"}"
    }
}

/**
 * Target player's owner gains life equal to a fixed amount.
 * Used for effects like "Its owner gains 4 life" (Path of Peace).
 * This targets the owner of the previously targeted permanent.
 */
@Serializable
data class OwnerGainsLifeEffect(
    val amount: Int
) : Effect {
    override val description: String = "Its owner gains $amount life"
}

// =============================================================================
// Phase Skip Effects
// =============================================================================

/**
 * Target player skips their combat phases during their next turn.
 * Used for cards like False Peace.
 */
@Serializable
data class SkipCombatPhasesEffect(
    val target: EffectTarget = EffectTarget.AnyPlayer
) : Effect {
    override val description: String = "${target.description} skips all combat phases of their next turn"
}

/**
 * Target player's creatures and lands don't untap during their next untap step.
 * Used for cards like Exhaustion.
 */
@Serializable
data class SkipUntapEffect(
    val target: EffectTarget = EffectTarget.Opponent,
    val affectsCreatures: Boolean = true,
    val affectsLands: Boolean = true
) : Effect {
    override val description: String = buildString {
        val affectedTypes = listOfNotNull(
            if (affectsCreatures) "Creatures" else null,
            if (affectsLands) "lands" else null
        ).joinToString(" and ")
        append("$affectedTypes ${target.description} controls don't untap during their next untap step")
    }
}

// =============================================================================
// X Spell Effects (Dynamic Amount)
// =============================================================================

/**
 * Deal X damage where X is determined by the spell's X value.
 * Used for cards like Blaze.
 */
@Serializable
data class DealXDamageEffect(
    val target: EffectTarget
) : Effect {
    override val description: String = "Deal X damage to ${target.description}"
}

/**
 * Filter for mass damage effects targeting creatures.
 * Used with DealDamageToGroupEffect to specify which creatures are affected.
 */
@Serializable
sealed interface CreatureDamageFilter {
    val description: String

    /** All creatures */
    @Serializable
    data object All : CreatureDamageFilter {
        override val description: String = "each creature"
    }

    /** Creatures with a specific keyword */
    @Serializable
    data class WithKeyword(val keyword: Keyword) : CreatureDamageFilter {
        override val description: String = "each creature with ${keyword.displayName.lowercase()}"
    }

    /** Creatures without a specific keyword */
    @Serializable
    data class WithoutKeyword(val keyword: Keyword) : CreatureDamageFilter {
        override val description: String = "each creature without ${keyword.displayName.lowercase()}"
    }

    /** Creatures of a specific color */
    @Serializable
    data class OfColor(val color: Color) : CreatureDamageFilter {
        override val description: String = "each ${color.displayName.lowercase()} creature"
    }

    /** Creatures not of a specific color */
    @Serializable
    data class NotOfColor(val color: Color) : CreatureDamageFilter {
        override val description: String = "each non${color.displayName.lowercase()} creature"
    }

    /** Attacking creatures only */
    @Serializable
    data object Attacking : CreatureDamageFilter {
        override val description: String = "each attacking creature"
    }
}

/**
 * Deal damage to multiple targets, dividing the total as you choose.
 * Used for cards like Forked Lightning ("4 damage divided among 1-3 targets").
 */
@Serializable
data class DividedDamageEffect(
    val totalDamage: Int,
    val minTargets: Int = 1,
    val maxTargets: Int = 3
) : Effect {
    override val description: String = "Deal $totalDamage damage divided as you choose among $minTargets to $maxTargets target creatures"
}

/**
 * Each player draws X cards where X is determined by the spell's X value.
 * Used for cards like Prosperity.
 */
@Serializable
data class EachPlayerDrawsXEffect(
    val includeController: Boolean = true,
    val includeOpponents: Boolean = true
) : Effect {
    override val description: String = when {
        includeController && includeOpponents -> "Each player draws X cards"
        includeController -> "You draw X cards"
        includeOpponents -> "Each opponent draws X cards"
        else -> "Draw X cards"
    }
}

/**
 * Each player may draw up to a number of cards, gaining life for each card not drawn.
 * "Each player may draw up to two cards. For each card less than two a player draws
 * this way, that player gains 2 life."
 * Used for Temporary Truce.
 *
 * @property maxCards Maximum number of cards each player may draw
 * @property lifePerCardNotDrawn Life gained for each card fewer than maxCards drawn (0 to disable)
 */
@Serializable
data class EachPlayerMayDrawEffect(
    val maxCards: Int,
    val lifePerCardNotDrawn: Int = 0
) : Effect {
    override val description: String = buildString {
        append("Each player may draw up to $maxCards cards")
        if (lifePerCardNotDrawn > 0) {
            append(". For each card less than $maxCards a player draws this way, that player gains $lifePerCardNotDrawn life")
        }
    }
}


/**
 * Look at top cards of target opponent's library, put some in graveyard, rest on top.
 * "Look at the top five cards of target opponent's library. Put one of them into that
 * player's graveyard and the rest back on top of their library in any order."
 * Used for Cruel Fate.
 *
 * @property count Number of cards to look at
 * @property toGraveyard Number of cards to put in graveyard
 */
@Serializable
data class LookAtOpponentLibraryEffect(
    val count: Int,
    val toGraveyard: Int = 1
) : Effect {
    override val description: String = buildString {
        append("Look at the top $count cards of target opponent's library. ")
        append("Put ${if (toGraveyard == 1) "one of them" else "$toGraveyard of them"} ")
        append("into that player's graveyard and the rest on top of their library in any order")
    }
}

// =============================================================================
// Grant Keyword to Group Effects
// =============================================================================

/**
 * Grant a keyword to multiple creatures until end of turn.
 * Used for cards like Nature's Cloak: "Green creatures you control gain forestwalk until end of turn."
 *
 * @property keyword The keyword to grant
 * @property filter Which creatures are affected
 */
@Serializable
data class GrantKeywordToGroupEffect(
    val keyword: Keyword,
    val filter: CreatureGroupFilter,
    val duration: Duration = Duration.EndOfTurn
) : Effect {
    override val description: String = buildString {
        append("${filter.description} gain ${keyword.displayName.lowercase()}")
        if (duration.description.isNotEmpty()) append(" ${duration.description}")
    }
}

/**
 * Grant evasion to a group of creatures until end of turn.
 * "Black creatures you control can't be blocked this turn except by black creatures."
 * Used for Dread Charge.
 *
 * @property filter Which creatures gain the evasion
 * @property canOnlyBeBlockedByColor The color of creatures that can block them
 */
@Serializable
data class GrantCantBeBlockedExceptByColorEffect(
    val filter: CreatureGroupFilter,
    val canOnlyBeBlockedByColor: Color,
    val duration: Duration = Duration.EndOfTurn
) : Effect {
    override val description: String = buildString {
        append("${filter.description} can't be blocked")
        if (duration.description.isNotEmpty()) append(" ${duration.description}")
        append(" except by ${canOnlyBeBlockedByColor.displayName.lowercase()} creatures")
    }
}

/**
 * Filter for groups of creatures affected by mass effects.
 */
@Serializable
sealed interface CreatureGroupFilter {
    val description: String

    /** All creatures you control */
    @Serializable
    data object AllYouControl : CreatureGroupFilter {
        override val description: String = "Creatures you control"
    }

    /** All creatures opponents control */
    @Serializable
    data object AllOpponentsControl : CreatureGroupFilter {
        override val description: String = "Creatures your opponents control"
    }

    /** All creatures */
    @Serializable
    data object All : CreatureGroupFilter {
        override val description: String = "All creatures"
    }

    /** All other creatures (except the source) */
    @Serializable
    data object AllOther : CreatureGroupFilter {
        override val description: String = "All other creatures"
    }

    /** Creatures you control with a specific color */
    @Serializable
    data class ColorYouControl(val color: Color) : CreatureGroupFilter {
        override val description: String = "${color.displayName} creatures you control"
    }

    /** Creatures you control with a specific keyword */
    @Serializable
    data class WithKeywordYouControl(val keyword: Keyword) : CreatureGroupFilter {
        override val description: String = "Creatures you control with ${keyword.displayName.lowercase()}"
    }

    /** All nonwhite creatures */
    @Serializable
    data object NonWhite : CreatureGroupFilter {
        override val description: String = "All nonwhite creatures"
    }

    /** Creatures that are not a specific color */
    @Serializable
    data class NotColor(val excludedColor: Color) : CreatureGroupFilter {
        override val description: String = "All non${excludedColor.displayName.lowercase()} creatures"
    }
}

/**
 * Modify power/toughness for a group of creatures until end of turn.
 * Used for cards like Warrior's Charge: "Creatures you control get +1/+1 until end of turn."
 *
 * @property powerModifier Power bonus (can be negative)
 * @property toughnessModifier Toughness bonus (can be negative)
 * @property filter Which creatures are affected
 */
@Serializable
data class ModifyStatsForGroupEffect(
    val powerModifier: Int,
    val toughnessModifier: Int,
    val filter: CreatureGroupFilter,
    val duration: Duration = Duration.EndOfTurn
) : Effect {
    override val description: String = buildString {
        append("${filter.description} get ")
        val powerStr = if (powerModifier >= 0) "+$powerModifier" else "$powerModifier"
        val toughStr = if (toughnessModifier >= 0) "+$toughnessModifier" else "$toughnessModifier"
        append("$powerStr/$toughStr")
        if (duration.description.isNotEmpty()) append(" ${duration.description}")
    }
}

/**
 * Deal damage to each attacking creature.
 * Used for Scorching Winds: "Deal 1 damage to each attacking creature."
 */
@Serializable
data class DealDamageToAttackingCreaturesEffect(
    val amount: Int
) : Effect {
    override val description: String = "Deal $amount damage to each attacking creature"
}

/**
 * Force sacrifice effect: Target player sacrifices permanents matching a filter.
 * "Target player sacrifices a creature" (Edict effects)
 */
@Serializable
data class ForceSacrificeEffect(
    val filter: CardFilter,
    val count: Int = 1,
    val target: EffectTarget = EffectTarget.Opponent
) : Effect {
    override val description: String = buildString {
        append(target.description)
        append(" sacrifices ")
        if (count == 1) {
            append("a ")
        } else {
            append("$count ")
        }
        append(filter.description)
        if (count != 1) append("s")
    }
}

/**
 * Separate permanents into piles effect.
 * "Separate all permanents target player controls into two piles.
 *  That player sacrifices all permanents in the pile of their choice."
 * Used for Liliana of the Veil's ultimate.
 */
@Serializable
data class SeparatePermanentsIntoPilesEffect(
    val target: EffectTarget = EffectTarget.AnyPlayer
) : Effect {
    override val description: String =
        "Separate all permanents ${target.description} controls into two piles. " +
                "That player sacrifices all permanents in the pile of their choice"
}

// =============================================================================
// Life Gain Based on Game State
// =============================================================================

// =============================================================================
// Mass Tap Effects
// =============================================================================

/**
 * Tap all creatures matching a filter.
 * "Tap all nonwhite creatures."
 * Used for Blinding Light.
 *
 * @property filter Which creatures are affected
 */
@Serializable
data class TapAllCreaturesEffect(
    val filter: CreatureGroupFilter = CreatureGroupFilter.All
) : Effect {
    override val description: String = "Tap ${filter.description.replaceFirstChar { it.lowercase() }}"
}

/**
 * Untap all creatures you control.
 * Used for Mobilize: "Untap all creatures you control."
 */
@Serializable
data object UntapAllCreaturesYouControlEffect : Effect {
    override val description: String = "Untap all creatures you control"
}

// =============================================================================
// Combat Damage Effects
// =============================================================================

/**
 * Creates a delayed trigger for the rest of the turn that reflects combat damage.
 * "This turn, whenever an attacking creature deals combat damage to you,
 *  it deals that much damage to its controller."
 * Used for Harsh Justice.
 *
 * The engine implements this by creating a temporary triggered ability that
 * listens for combat damage events and applies reflection.
 */
@Serializable
data class ReflectCombatDamageEffect(
    val target: EffectTarget = EffectTarget.Controller
) : Effect {
    override val description: String =
        "This turn, whenever an attacking creature deals combat damage to you, " +
                "it deals that much damage to its controller"
}

/**
 * Prevents combat damage that would be dealt by specified creatures.
 * "Prevent all combat damage that would be dealt by creatures you don't control."
 * Used for Fog-type effects with creature restrictions.
 */
@Serializable
data class PreventCombatDamageFromEffect(
    val source: CreatureGroupFilter,
    val duration: Duration = Duration.EndOfTurn
) : Effect {
    override val description: String =
        "Prevent all combat damage that would be dealt by ${source.description.replaceFirstChar { it.lowercase() }}"
}

// =============================================================================
// Flux/Wheel Variant Effects
// =============================================================================

/**
 * Each player discards cards, then draws that many cards.
 * Used for Flux, Windfall, and similar effects.
 *
 * @param minDiscard Minimum cards to discard (0 for "any number")
 * @param maxDiscard Maximum cards to discard (null means up to hand size)
 * @param discardEntireHand If true, each player must discard their entire hand
 * @param controllerBonusDraw Extra cards the controller draws after the effect
 */
@Serializable
data class EachPlayerDiscardsDrawsEffect(
    val minDiscard: Int = 0,
    val maxDiscard: Int? = null,
    val discardEntireHand: Boolean = false,
    val controllerBonusDraw: Int = 0
) : Effect {
    override val description: String = buildString {
        if (discardEntireHand) {
            append("Each player discards their hand, then draws that many cards")
        } else if (minDiscard == 0 && maxDiscard == null) {
            append("Each player discards any number of cards, then draws that many cards")
        } else {
            append("Each player discards ")
            if (minDiscard == maxDiscard) {
                append("$minDiscard card${if (minDiscard != 1) "s" else ""}")
            } else {
                append("$minDiscard to ${maxDiscard ?: "any number of"} cards")
            }
            append(", then draws that many cards")
        }
        if (controllerBonusDraw > 0) {
            append(". Draw ${if (controllerBonusDraw == 1) "a card" else "$controllerBonusDraw cards"}")
        }
    }
}


/**
 * Look at target player's hand.
 * Used for Ingenious Thief and similar "peek" effects.
 */
@Serializable
data class LookAtTargetHandEffect(
    val target: EffectTarget = EffectTarget.AnyPlayer
) : Effect {
    override val description: String = "Look at ${target.description}'s hand"
}

// =============================================================================
// Counter Spell Effects
// =============================================================================

/**
 * Counter target spell that matches a filter.
 * Used for Mystic Denial: "Counter target creature or sorcery spell."
 */
@Serializable
data class CounterSpellWithFilterEffect(
    val filter: SpellFilter
) : Effect {
    override val description: String = "Counter target ${filter.description} spell"
}

/**
 * Filter for spell types that can be countered.
 */
@Serializable
sealed interface SpellFilter {
    val description: String

    @Serializable
    data object AnySpell : SpellFilter {
        override val description: String = ""
    }

    @Serializable
    data object CreatureSpell : SpellFilter {
        override val description: String = "creature"
    }

    @Serializable
    data object NonCreatureSpell : SpellFilter {
        override val description: String = "noncreature"
    }

    @Serializable
    data object SorcerySpell : SpellFilter {
        override val description: String = "sorcery"
    }

    @Serializable
    data object InstantSpell : SpellFilter {
        override val description: String = "instant"
    }

    @Serializable
    data class CreatureOrSorcery(val dummy: Unit = Unit) : SpellFilter {
        override val description: String = "creature or sorcery"
    }
}

// =============================================================================
// Library Manipulation Effects
// =============================================================================

/**
 * Search library for a card type and put it on top.
 * Used for Personal Tutor: "Search your library for a sorcery card, reveal it,
 * then shuffle and put that card on top."
 */
@Serializable
data class SearchLibraryToTopEffect(
    val filter: CardFilter,
    val reveal: Boolean = false
) : Effect {
    override val description: String =
        "Search your library for ${filter.description}${if (reveal) ", reveal it," else ","} then shuffle and put that card on top"
}

// =============================================================================
// Combat Manipulation Effects
// =============================================================================

/**
 * Force creatures to attack during target player's next turn.
 * Used for Taunt: "During target player's next turn, creatures that player controls attack you if able."
 */
@Serializable
data class TauntEffect(
    val target: EffectTarget = EffectTarget.AnyPlayer
) : Effect {
    override val description: String =
        "During ${target.description}'s next turn, creatures they control attack you if able"
}

/**
 * Tap up to X target creatures with a filter.
 * Used for Tidal Surge: "Tap up to three target creatures without flying."
 */
@Serializable
data class TapTargetCreaturesEffect(
    val maxTargets: Int,
    val filter: CreatureTargetFilter = CreatureTargetFilter.Any
) : Effect {
    override val description: String = buildString {
        append("Tap up to $maxTargets target ")
        if (filter != CreatureTargetFilter.Any) {
            append("${filter.description} ")
        }
        append("creature${if (maxTargets > 1) "s" else ""}")
    }
}

/**
 * Filter for creature targeting.
 */
@Serializable
sealed interface CreatureTargetFilter {
    val description: String

    @Serializable
    data object Any : CreatureTargetFilter {
        override val description: String = ""
    }

    @Serializable
    data object WithoutFlying : CreatureTargetFilter {
        override val description: String = "without flying"
    }

    @Serializable
    data object WithFlying : CreatureTargetFilter {
        override val description: String = "with flying"
    }

    @Serializable
    data object Tapped : CreatureTargetFilter {
        override val description: String = "tapped"
    }

    @Serializable
    data object Untapped : CreatureTargetFilter {
        override val description: String = "untapped"
    }

    @Serializable
    data object Attacking : CreatureTargetFilter {
        override val description: String = "attacking"
    }

    @Serializable
    data object Nonblack : CreatureTargetFilter {
        override val description: String = "nonblack"
    }
}

/**
 * You may play additional lands this turn.
 * Used for Summer Bloom: "You may play up to three additional lands this turn."
 *
 * @param count The number of additional lands you may play
 */
@Serializable
data class PlayAdditionalLandsEffect(
    val count: Int
) : Effect {
    override val description: String = "You may play up to $count additional land${if (count != 1) "s" else ""} this turn"
}

// =============================================================================
// Turn Manipulation Effects
// =============================================================================

/**
 * Take an extra turn after this one, with a consequence at end of turn.
 * Used for Last Chance: "Take an extra turn after this one. At the beginning of that turn's end step, you lose the game."
 *
 * @param loseAtEndStep If true, you lose the game at the beginning of that turn's end step
 */
@Serializable
data class TakeExtraTurnEffect(
    val loseAtEndStep: Boolean = false
) : Effect {
    override val description: String = buildString {
        append("Take an extra turn after this one")
        if (loseAtEndStep) {
            append(". At the beginning of that turn's end step, you lose the game")
        }
    }
}

// =============================================================================
// Damage Prevention Effects
// =============================================================================

/**
 * Prevent all damage that would be dealt to you this turn by attacking creatures.
 * Used for Deep Wood: "Prevent all damage that would be dealt to you this turn by attacking creatures."
 */
@Serializable
data object PreventDamageFromAttackingCreaturesThisTurnEffect : Effect {
    override val description: String = "Prevent all damage that would be dealt to you this turn by attacking creatures"
}

// =============================================================================
// Land-Based Life Gain Effects
// =============================================================================

/**
 * Gain life equal to the number of lands of a specific type on the battlefield.
 * Used for Fruition: "You gain 1 life for each Forest on the battlefield."
 *
 * @property landType The type of land to count (e.g., "Forest")
 * @property lifePerLand The amount of life gained per land
 */
@Serializable
data class GainLifeForEachLandOnBattlefieldEffect(
    val landType: String,
    val lifePerLand: Int = 1
) : Effect {
    override val description: String = "You gain $lifePerLand life for each $landType on the battlefield"
}

// =============================================================================
// Unless Effects (Punisher Mechanics)
// =============================================================================

/**
 * Represents a cost that can be paid to avoid a consequence.
 * Used by PayOrSufferEffect to unify "unless" mechanics.
 */
@Serializable
sealed interface PayCost {
    val description: String

    /**
     * Discard one or more cards matching a filter to avoid the consequence.
     * "...unless you discard a land card"
     *
     * @property filter What type of card must be discarded
     * @property count How many cards must be discarded (default 1)
     * @property random If true, the discard is random (e.g., Pillaging Horde)
     */
    @Serializable
    data class Discard(
        val filter: CardFilter = CardFilter.AnyCard,
        val count: Int = 1,
        val random: Boolean = false
    ) : PayCost {
        override val description: String = buildString {
            append("discard ")
            if (count == 1) {
                when (filter) {
                    CardFilter.AnyCard -> append("a card")
                    CardFilter.LandCard -> append("a land card")
                    CardFilter.CreatureCard -> append("a creature card")
                    else -> append("a ${filter.description}")
                }
            } else {
                append("$count ")
                when (filter) {
                    CardFilter.AnyCard -> append("cards")
                    CardFilter.LandCard -> append("land cards")
                    CardFilter.CreatureCard -> append("creature cards")
                    else -> append("${filter.description}s")
                }
            }
            if (random) append(" at random")
        }
    }

    /**
     * Sacrifice one or more permanents matching a filter to avoid the consequence.
     * "...unless you sacrifice three Forests"
     *
     * @property filter What type of permanent must be sacrificed
     * @property count How many permanents must be sacrificed (default 1)
     */
    @Serializable
    data class Sacrifice(
        val filter: CardFilter,
        val count: Int = 1
    ) : PayCost {
        override val description: String = buildString {
            append("sacrifice ")
            if (count == 1) {
                val desc = filter.description
                append(if (desc.first().lowercaseChar() in "aeiou") "an" else "a")
                append(" $desc")
            } else {
                append(numberToWord(count))
                append(" ${filter.description}s")
            }
        }

        private fun numberToWord(n: Int): String = when (n) {
            1 -> "one"
            2 -> "two"
            3 -> "three"
            4 -> "four"
            5 -> "five"
            else -> n.toString()
        }
    }

    /**
     * Pay life to avoid the consequence.
     * "...unless you pay 3 life"
     *
     * @property amount How much life to pay
     */
    @Serializable
    data class PayLife(
        val amount: Int
    ) : PayCost {
        override val description: String = "pay $amount life"
    }
}

/**
 * Generic "unless" effect for punisher mechanics.
 * "Do [suffer], unless you [cost]."
 *
 * This is a unified effect that handles:
 * - "Sacrifice this unless you discard a land card" (Thundering Wurm)
 * - "Sacrifice this unless you sacrifice three Forests" (Primeval Force)
 * - Similar punisher-style effects
 *
 * @property cost The cost that can be paid to avoid the consequence
 * @property suffer The consequence if the cost is not paid
 * @property player Who must make the choice (defaults to controller)
 */
@Serializable
data class PayOrSufferEffect(
    val cost: PayCost,
    val suffer: Effect,
    val player: EffectTarget = EffectTarget.Controller
) : Effect {
    override val description: String = "${suffer.description} unless you ${cost.description}"
}
