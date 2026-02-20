package com.wingedsheep.sdk.dsl

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.scripting.conditions.AllConditions
import com.wingedsheep.sdk.scripting.conditions.AnyCondition
import com.wingedsheep.sdk.scripting.conditions.LifeTotalAtLeast
import com.wingedsheep.sdk.scripting.conditions.LifeTotalAtMost
import com.wingedsheep.sdk.scripting.conditions.NotCondition
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
        OpponentControlsMoreLands

    /**
     * If an opponent controls more creatures than you.
     */
    val OpponentControlsMoreCreatures: ConditionInterface =
        OpponentControlsMoreCreatures

    /**
     * If an opponent controls a creature.
     */
    val OpponentControlsCreature: ConditionInterface =
        OpponentControlsCreature

    /**
     * If you control a creature.
     */
    val ControlCreature: ConditionInterface =
        ControlCreature

    /**
     * If you control an enchantment.
     */
    val ControlEnchantment: ConditionInterface =
        ControlEnchantment

    /**
     * If you control an artifact.
     */
    val ControlArtifact: ConditionInterface =
        ControlArtifact

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
        MoreLifeThanOpponent

    /**
     * If you have less life than an opponent.
     */
    val LessLifeThanOpponent: ConditionInterface =
        LessLifeThanOpponent

    // =========================================================================
    // Hand Conditions
    // =========================================================================

    /**
     * If you have no cards in hand.
     */
    val EmptyHand: ConditionInterface =
        EmptyHand

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
        SourceIsAttacking

    /**
     * If this creature is blocking.
     */
    val SourceIsBlocking: ConditionInterface =
        SourceIsBlocking

    /**
     * If this permanent is tapped.
     */
    val SourceIsTapped: ConditionInterface =
        SourceIsTapped

    /**
     * If this permanent is untapped.
     */
    val SourceIsUntapped: ConditionInterface =
        SourceIsUntapped

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
        IsYourTurn

    /**
     * If it's not your turn.
     */
    val IsNotYourTurn: ConditionInterface =
        IsNotYourTurn

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
