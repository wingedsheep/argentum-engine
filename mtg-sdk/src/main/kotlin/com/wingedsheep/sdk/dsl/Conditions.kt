package com.wingedsheep.sdk.dsl

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.scripting.*
import com.wingedsheep.sdk.scripting.Condition as ConditionInterface

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
        com.wingedsheep.sdk.scripting.OpponentControlsMoreLands

    /**
     * If an opponent controls more creatures than you.
     */
    val OpponentControlsMoreCreatures: ConditionInterface =
        com.wingedsheep.sdk.scripting.OpponentControlsMoreCreatures

    /**
     * If an opponent controls a creature.
     */
    val OpponentControlsCreature: ConditionInterface =
        com.wingedsheep.sdk.scripting.OpponentControlsCreature

    /**
     * If you control a creature.
     */
    val ControlCreature: ConditionInterface =
        com.wingedsheep.sdk.scripting.ControlCreature

    /**
     * If you control an enchantment.
     */
    val ControlEnchantment: ConditionInterface =
        com.wingedsheep.sdk.scripting.ControlEnchantment

    /**
     * If you control an artifact.
     */
    val ControlArtifact: ConditionInterface =
        com.wingedsheep.sdk.scripting.ControlArtifact

    /**
     * If you control N or more creatures.
     */
    fun ControlCreaturesAtLeast(count: Int): ConditionInterface =
        com.wingedsheep.sdk.scripting.ControlCreaturesAtLeast(count)

    /**
     * If you control a creature with a specific keyword.
     */
    fun ControlCreatureWithKeyword(keyword: Keyword): ConditionInterface =
        com.wingedsheep.sdk.scripting.ControlCreatureWithKeyword(keyword)

    /**
     * If you control a creature of a specific type.
     */
    fun ControlCreatureOfType(subtype: Subtype): ConditionInterface =
        com.wingedsheep.sdk.scripting.ControlCreatureOfType(subtype)

    /**
     * If a player controls more creatures of the given subtype than each other player.
     */
    fun APlayerControlsMostOfSubtype(subtype: Subtype): ConditionInterface =
        com.wingedsheep.sdk.scripting.APlayerControlsMostOfSubtype(subtype)

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
        com.wingedsheep.sdk.scripting.MoreLifeThanOpponent

    /**
     * If you have less life than an opponent.
     */
    val LessLifeThanOpponent: ConditionInterface =
        com.wingedsheep.sdk.scripting.LessLifeThanOpponent

    // =========================================================================
    // Hand Conditions
    // =========================================================================

    /**
     * If you have no cards in hand.
     */
    val EmptyHand: ConditionInterface =
        com.wingedsheep.sdk.scripting.EmptyHand

    /**
     * If you have N or more cards in hand.
     */
    fun CardsInHandAtLeast(count: Int): ConditionInterface =
        com.wingedsheep.sdk.scripting.CardsInHandAtLeast(count)

    /**
     * If you have N or fewer cards in hand.
     */
    fun CardsInHandAtMost(count: Int): ConditionInterface =
        com.wingedsheep.sdk.scripting.CardsInHandAtMost(count)

    // =========================================================================
    // Graveyard Conditions
    // =========================================================================

    /**
     * If there are N or more creature cards in your graveyard.
     */
    fun CreatureCardsInGraveyardAtLeast(count: Int): ConditionInterface =
        com.wingedsheep.sdk.scripting.CreatureCardsInGraveyardAtLeast(count)

    /**
     * If there are N or more cards in your graveyard.
     */
    fun CardsInGraveyardAtLeast(count: Int): ConditionInterface =
        com.wingedsheep.sdk.scripting.CardsInGraveyardAtLeast(count)

    /**
     * If there is a card of a specific subtype in your graveyard.
     */
    fun GraveyardContainsSubtype(subtype: Subtype): ConditionInterface =
        com.wingedsheep.sdk.scripting.GraveyardContainsSubtype(subtype)

    // =========================================================================
    // Source Conditions
    // =========================================================================

    /**
     * If this creature is attacking.
     */
    val SourceIsAttacking: ConditionInterface =
        com.wingedsheep.sdk.scripting.SourceIsAttacking

    /**
     * If this creature is blocking.
     */
    val SourceIsBlocking: ConditionInterface =
        com.wingedsheep.sdk.scripting.SourceIsBlocking

    /**
     * If this permanent is tapped.
     */
    val SourceIsTapped: ConditionInterface =
        com.wingedsheep.sdk.scripting.SourceIsTapped

    /**
     * If this permanent is untapped.
     */
    val SourceIsUntapped: ConditionInterface =
        com.wingedsheep.sdk.scripting.SourceIsUntapped

    // =========================================================================
    // Turn Conditions
    // =========================================================================

    /**
     * If it's your turn.
     */
    val IsYourTurn: ConditionInterface =
        com.wingedsheep.sdk.scripting.IsYourTurn

    /**
     * If it's not your turn.
     */
    val IsNotYourTurn: ConditionInterface =
        com.wingedsheep.sdk.scripting.IsNotYourTurn

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
