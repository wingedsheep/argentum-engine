package com.wingedsheep.sdk.dsl

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.events.CounterTypeFilter
import com.wingedsheep.sdk.scripting.conditions.AllConditions
import com.wingedsheep.sdk.scripting.conditions.AnyCondition
import com.wingedsheep.sdk.scripting.conditions.Compare
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.conditions.Exists
import com.wingedsheep.sdk.scripting.conditions.NotCondition
import com.wingedsheep.sdk.scripting.conditions.SourceIsAttacking as SourceIsAttackingCondition
import com.wingedsheep.sdk.scripting.conditions.SourceIsBlocking as SourceIsBlockingCondition
import com.wingedsheep.sdk.scripting.conditions.SourceIsTapped as SourceIsTappedCondition
import com.wingedsheep.sdk.scripting.conditions.SourceIsUntapped as SourceIsUntappedCondition
import com.wingedsheep.sdk.scripting.conditions.IsYourTurn as IsYourTurnCondition
import com.wingedsheep.sdk.scripting.conditions.IsNotYourTurn as IsNotYourTurnCondition
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.scripting.conditions.Condition as ConditionInterface

/**
 * Facade object providing convenient access to Condition types.
 *
 * Usage:
 * ```kotlin
 * Conditions.OpponentControlsMoreLands
 * Conditions.LifeAtMost(5)
 * Conditions.ControlCreature
 * ```
 */
object Conditions {

    // =========================================================================
    // Battlefield Conditions (via Exists / Compare)
    // =========================================================================

    /**
     * If an opponent controls more lands than you.
     */
    val OpponentControlsMoreLands: ConditionInterface =
        Compare(
            DynamicAmount.AggregateBattlefield(Player.Opponent, GameObjectFilter.Land),
            ComparisonOperator.GT,
            DynamicAmount.AggregateBattlefield(Player.You, GameObjectFilter.Land)
        )

    /**
     * If an opponent controls more creatures than you.
     */
    val OpponentControlsMoreCreatures: ConditionInterface =
        Compare(
            DynamicAmount.AggregateBattlefield(Player.Opponent, GameObjectFilter.Creature),
            ComparisonOperator.GT,
            DynamicAmount.AggregateBattlefield(Player.You, GameObjectFilter.Creature)
        )

    /**
     * If an opponent controls a creature.
     */
    val OpponentControlsCreature: ConditionInterface =
        Exists(Player.Opponent, Zone.BATTLEFIELD, GameObjectFilter.Creature)

    /**
     * If you control a creature.
     */
    val ControlCreature: ConditionInterface =
        Exists(Player.You, Zone.BATTLEFIELD, GameObjectFilter.Creature)

    /**
     * If you control an enchantment.
     */
    val ControlEnchantment: ConditionInterface =
        Exists(Player.You, Zone.BATTLEFIELD, GameObjectFilter.Enchantment)

    /**
     * If you control an artifact.
     */
    val ControlArtifact: ConditionInterface =
        Exists(Player.You, Zone.BATTLEFIELD, GameObjectFilter.Artifact)

    /**
     * If you control N or more lands.
     */
    fun ControlLandsAtLeast(count: Int): ConditionInterface =
        Compare(
            DynamicAmount.AggregateBattlefield(Player.You, GameObjectFilter.Land),
            ComparisonOperator.GTE,
            DynamicAmount.Fixed(count)
        )

    /**
     * If you control N or more creatures.
     */
    fun ControlCreaturesAtLeast(count: Int): ConditionInterface =
        Compare(
            DynamicAmount.AggregateBattlefield(Player.You, GameObjectFilter.Creature),
            ComparisonOperator.GTE,
            DynamicAmount.Fixed(count)
        )

    /**
     * If you control a creature with a specific keyword.
     */
    fun ControlCreatureWithKeyword(keyword: Keyword): ConditionInterface =
        Exists(Player.You, Zone.BATTLEFIELD, GameObjectFilter.Creature.withKeyword(keyword))

    /**
     * If you control a creature of a specific type.
     */
    fun ControlCreatureOfType(subtype: Subtype): ConditionInterface =
        Exists(Player.You, Zone.BATTLEFIELD, GameObjectFilter.Creature.withSubtype(subtype))

    /**
     * If a player controls more creatures of the given subtype than each other player.
     */
    fun APlayerControlsMostOfSubtype(subtype: Subtype): ConditionInterface =
        com.wingedsheep.sdk.scripting.conditions.APlayerControlsMostOfSubtype(subtype)

    /**
     * If the target creature's power is at most the given dynamic amount.
     * Used for cards like Unified Strike.
     */
    fun TargetPowerAtMost(amount: DynamicAmount, targetIndex: Int = 0): ConditionInterface =
        Compare(DynamicAmount.TargetPower(targetIndex), ComparisonOperator.LTE, amount)

    /**
     * If the target spell's mana value is at most the given dynamic amount.
     * Used for conditional counterspells like Dispersal Shield.
     */
    fun TargetSpellManaValueAtMost(amount: DynamicAmount, targetIndex: Int = 0): ConditionInterface =
        Compare(DynamicAmount.TargetManaValue(targetIndex), ComparisonOperator.LTE, amount)

    /**
     * If the target permanent has at least one counter of the given type.
     * Used for cards like Bring Low: "If that creature has a +1/+1 counter on it"
     */
    fun TargetHasCounter(counterType: CounterTypeFilter, targetIndex: Int = 0): ConditionInterface =
        Compare(DynamicAmount.CountersOnTarget(counterType, targetIndex), ComparisonOperator.GTE, DynamicAmount.Fixed(1))

    // =========================================================================
    // Life Total Conditions (via Compare)
    // =========================================================================

    /**
     * If your life total is N or less.
     */
    fun LifeAtMost(threshold: Int): ConditionInterface =
        Compare(DynamicAmount.LifeTotal(Player.You), ComparisonOperator.LTE, DynamicAmount.Fixed(threshold))

    /**
     * If your life total is N or more.
     */
    fun LifeAtLeast(threshold: Int): ConditionInterface =
        Compare(DynamicAmount.LifeTotal(Player.You), ComparisonOperator.GTE, DynamicAmount.Fixed(threshold))

    /**
     * If you have more life than an opponent.
     */
    val MoreLifeThanOpponent: ConditionInterface =
        Compare(DynamicAmount.LifeTotal(Player.You), ComparisonOperator.GT, DynamicAmount.LifeTotal(Player.Opponent))

    /**
     * If you have less life than an opponent.
     */
    val LessLifeThanOpponent: ConditionInterface =
        Compare(DynamicAmount.LifeTotal(Player.You), ComparisonOperator.LT, DynamicAmount.LifeTotal(Player.Opponent))

    // =========================================================================
    // Hand Conditions (via Compare / Exists)
    // =========================================================================

    /**
     * If you have no cards in hand.
     */
    val EmptyHand: ConditionInterface =
        Exists(Player.You, Zone.HAND, negate = true)

    /**
     * If you have N or more cards in hand.
     */
    fun CardsInHandAtLeast(count: Int): ConditionInterface =
        Compare(DynamicAmount.Count(Player.You, Zone.HAND), ComparisonOperator.GTE, DynamicAmount.Fixed(count))

    /**
     * If you have N or fewer cards in hand.
     */
    fun CardsInHandAtMost(count: Int): ConditionInterface =
        Compare(DynamicAmount.Count(Player.You, Zone.HAND), ComparisonOperator.LTE, DynamicAmount.Fixed(count))

    // =========================================================================
    // Graveyard Conditions (via Compare / Exists)
    // =========================================================================

    /**
     * If there are N or more creature cards in your graveyard.
     */
    fun CreatureCardsInGraveyardAtLeast(count: Int): ConditionInterface =
        Compare(
            DynamicAmount.Count(Player.You, Zone.GRAVEYARD, GameObjectFilter.Creature),
            ComparisonOperator.GTE,
            DynamicAmount.Fixed(count)
        )

    /**
     * If there are N or more cards in your graveyard.
     */
    fun CardsInGraveyardAtLeast(count: Int): ConditionInterface =
        Compare(DynamicAmount.Count(Player.You, Zone.GRAVEYARD), ComparisonOperator.GTE, DynamicAmount.Fixed(count))

    /**
     * If there is a card of a specific subtype in your graveyard.
     */
    fun GraveyardContainsSubtype(subtype: Subtype): ConditionInterface =
        Exists(Player.You, Zone.GRAVEYARD, GameObjectFilter.Any.withSubtype(subtype))

    // =========================================================================
    // Source Conditions
    // =========================================================================

    /**
     * If this creature is attacking.
     */
    val SourceIsAttacking: ConditionInterface =
        SourceIsAttackingCondition

    /**
     * If this creature is blocking.
     */
    val SourceIsBlocking: ConditionInterface =
        SourceIsBlockingCondition

    /**
     * If this permanent is tapped.
     */
    val SourceIsTapped: ConditionInterface =
        SourceIsTappedCondition

    /**
     * If this permanent is untapped.
     */
    val SourceIsUntapped: ConditionInterface =
        SourceIsUntappedCondition

    /**
     * As long as this creature is a specific subtype.
     * Used for conditional static abilities like "has defender as long as it's a Wall."
     */
    fun SourceHasSubtype(subtype: Subtype): ConditionInterface =
        com.wingedsheep.sdk.scripting.conditions.SourceHasSubtype(subtype)

    // =========================================================================
    // Turn Conditions
    // =========================================================================

    /**
     * If it's your turn.
     */
    val IsYourTurn: ConditionInterface =
        IsYourTurnCondition

    /**
     * If it's not your turn.
     */
    val IsNotYourTurn: ConditionInterface =
        IsNotYourTurnCondition

    // =========================================================================
    // Composite Conditions
    // =========================================================================

    /**
     * All conditions must be true (AND).
     */
    fun All(vararg conditions: ConditionInterface): ConditionInterface =
        AllConditions(conditions.toList())

    /**
     * Any condition must be true (OR).
     */
    fun Any(vararg conditions: ConditionInterface): ConditionInterface =
        AnyCondition(conditions.toList())

    /**
     * Condition must NOT be true.
     */
    fun Not(condition: ConditionInterface): ConditionInterface =
        NotCondition(condition)
}
