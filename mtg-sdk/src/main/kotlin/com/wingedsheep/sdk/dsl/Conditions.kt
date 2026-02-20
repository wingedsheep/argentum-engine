package com.wingedsheep.sdk.dsl

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.scripting.conditions.AllConditions
import com.wingedsheep.sdk.scripting.conditions.AnyCondition
import com.wingedsheep.sdk.scripting.conditions.LifeTotalAtLeast
import com.wingedsheep.sdk.scripting.conditions.LifeTotalAtMost
import com.wingedsheep.sdk.scripting.conditions.NotCondition
import com.wingedsheep.sdk.scripting.conditions.OpponentControlsMoreLands as OpponentControlsMoreLandsCondition
import com.wingedsheep.sdk.scripting.conditions.OpponentControlsMoreCreatures as OpponentControlsMoreCreaturesCondition
import com.wingedsheep.sdk.scripting.conditions.OpponentControlsCreature as OpponentControlsCreatureCondition
import com.wingedsheep.sdk.scripting.conditions.ControlCreature as ControlCreatureCondition
import com.wingedsheep.sdk.scripting.conditions.ControlEnchantment as ControlEnchantmentCondition
import com.wingedsheep.sdk.scripting.conditions.ControlArtifact as ControlArtifactCondition
import com.wingedsheep.sdk.scripting.conditions.MoreLifeThanOpponent as MoreLifeThanOpponentCondition
import com.wingedsheep.sdk.scripting.conditions.LessLifeThanOpponent as LessLifeThanOpponentCondition
import com.wingedsheep.sdk.scripting.conditions.EmptyHand as EmptyHandCondition
import com.wingedsheep.sdk.scripting.conditions.SourceIsAttacking as SourceIsAttackingCondition
import com.wingedsheep.sdk.scripting.conditions.SourceIsBlocking as SourceIsBlockingCondition
import com.wingedsheep.sdk.scripting.conditions.SourceIsTapped as SourceIsTappedCondition
import com.wingedsheep.sdk.scripting.conditions.SourceIsUntapped as SourceIsUntappedCondition
import com.wingedsheep.sdk.scripting.conditions.IsYourTurn as IsYourTurnCondition
import com.wingedsheep.sdk.scripting.conditions.IsNotYourTurn as IsNotYourTurnCondition
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
    // Battlefield Conditions
    // =========================================================================

    /**
     * If an opponent controls more lands than you.
     */
    val OpponentControlsMoreLands: ConditionInterface =
        OpponentControlsMoreLandsCondition

    /**
     * If an opponent controls more creatures than you.
     */
    val OpponentControlsMoreCreatures: ConditionInterface =
        OpponentControlsMoreCreaturesCondition

    /**
     * If an opponent controls a creature.
     */
    val OpponentControlsCreature: ConditionInterface =
        OpponentControlsCreatureCondition

    /**
     * If you control a creature.
     */
    val ControlCreature: ConditionInterface =
        ControlCreatureCondition

    /**
     * If you control an enchantment.
     */
    val ControlEnchantment: ConditionInterface =
        ControlEnchantmentCondition

    /**
     * If you control an artifact.
     */
    val ControlArtifact: ConditionInterface =
        ControlArtifactCondition

    /**
     * If you control N or more creatures.
     */
    fun ControlCreaturesAtLeast(count: Int): ConditionInterface =
        com.wingedsheep.sdk.scripting.conditions.ControlCreaturesAtLeast(count)

    /**
     * If you control a creature with a specific keyword.
     */
    fun ControlCreatureWithKeyword(keyword: Keyword): ConditionInterface =
        com.wingedsheep.sdk.scripting.conditions.ControlCreatureWithKeyword(keyword)

    /**
     * If you control a creature of a specific type.
     */
    fun ControlCreatureOfType(subtype: Subtype): ConditionInterface =
        com.wingedsheep.sdk.scripting.conditions.ControlCreatureOfType(subtype)

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
        com.wingedsheep.sdk.scripting.conditions.TargetPowerAtMost(amount, targetIndex)

    /**
     * If the target spell's mana value is at most the given dynamic amount.
     * Used for conditional counterspells like Dispersal Shield.
     */
    fun TargetSpellManaValueAtMost(amount: DynamicAmount, targetIndex: Int = 0): ConditionInterface =
        com.wingedsheep.sdk.scripting.conditions.TargetSpellManaValueAtMost(amount, targetIndex)

    // =========================================================================
    // Life Total Conditions
    // =========================================================================

    /**
     * If your life total is N or less.
     */
    fun LifeAtMost(threshold: Int): ConditionInterface =
        LifeTotalAtMost(threshold)

    /**
     * If your life total is N or more.
     */
    fun LifeAtLeast(threshold: Int): ConditionInterface =
        LifeTotalAtLeast(threshold)

    /**
     * If you have more life than an opponent.
     */
    val MoreLifeThanOpponent: ConditionInterface =
        MoreLifeThanOpponentCondition

    /**
     * If you have less life than an opponent.
     */
    val LessLifeThanOpponent: ConditionInterface =
        LessLifeThanOpponentCondition

    // =========================================================================
    // Hand Conditions
    // =========================================================================

    /**
     * If you have no cards in hand.
     */
    val EmptyHand: ConditionInterface =
        EmptyHandCondition

    /**
     * If you have N or more cards in hand.
     */
    fun CardsInHandAtLeast(count: Int): ConditionInterface =
        com.wingedsheep.sdk.scripting.conditions.CardsInHandAtLeast(count)

    /**
     * If you have N or fewer cards in hand.
     */
    fun CardsInHandAtMost(count: Int): ConditionInterface =
        com.wingedsheep.sdk.scripting.conditions.CardsInHandAtMost(count)

    // =========================================================================
    // Graveyard Conditions
    // =========================================================================

    /**
     * If there are N or more creature cards in your graveyard.
     */
    fun CreatureCardsInGraveyardAtLeast(count: Int): ConditionInterface =
        com.wingedsheep.sdk.scripting.conditions.CreatureCardsInGraveyardAtLeast(count)

    /**
     * If there are N or more cards in your graveyard.
     */
    fun CardsInGraveyardAtLeast(count: Int): ConditionInterface =
        com.wingedsheep.sdk.scripting.conditions.CardsInGraveyardAtLeast(count)

    /**
     * If there is a card of a specific subtype in your graveyard.
     */
    fun GraveyardContainsSubtype(subtype: Subtype): ConditionInterface =
        com.wingedsheep.sdk.scripting.conditions.GraveyardContainsSubtype(subtype)

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
