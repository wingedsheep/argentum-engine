package com.wingedsheep.sdk.dsl

import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Facade object providing convenient access to AbilityCost types.
 *
 * Usage:
 * ```kotlin
 * Costs.Tap
 * Costs.Mana("{2}")
 * Costs.Sacrifice(Filters.Creature)
 * Costs.Composite(Costs.Tap, Costs.Sacrifice(Filters.Self))
 * ```
 */
object Costs {

    // =========================================================================
    // Simple Costs
    // =========================================================================

    /**
     * Tap this permanent ({T}).
     */
    val Tap: AbilityCost = AbilityCost.Tap

    /**
     * Untap this permanent ({Q}).
     */
    val Untap: AbilityCost = AbilityCost.Untap

    // =========================================================================
    // Mana Costs
    // =========================================================================

    /**
     * Pay mana cost from string (e.g., "{2}{B}").
     */
    fun Mana(cost: String): AbilityCost =
        AbilityCost.Mana(ManaCost.parse(cost))

    /**
     * Pay mana cost from ManaCost object.
     */
    fun Mana(cost: ManaCost): AbilityCost =
        AbilityCost.Mana(cost)

    // =========================================================================
    // Life Costs
    // =========================================================================

    /**
     * Pay life.
     */
    fun PayLife(amount: Int): AbilityCost =
        AbilityCost.PayLife(amount)

    // =========================================================================
    // Sacrifice Costs
    // =========================================================================

    /**
     * Sacrifice a permanent matching the filter.
     */
    fun Sacrifice(filter: GameObjectFilter = GameObjectFilter.Any): AbilityCost =
        AbilityCost.Sacrifice(filter)

    // =========================================================================
    // Discard Costs
    // =========================================================================

    /**
     * Discard a card (any card).
     */
    val DiscardCard: AbilityCost = AbilityCost.Discard()

    /**
     * Discard a card matching the filter.
     */
    fun Discard(filter: GameObjectFilter = GameObjectFilter.Any): AbilityCost =
        AbilityCost.Discard(filter)

    /**
     * Discard your entire hand.
     */
    val DiscardHand: AbilityCost = AbilityCost.DiscardHand

    /**
     * Discard this card (for cycling and similar abilities).
     */
    val DiscardSelf: AbilityCost = AbilityCost.DiscardSelf

    /**
     * Sacrifice this permanent (for abilities that sacrifice themselves as cost).
     */
    val SacrificeSelf: AbilityCost = AbilityCost.SacrificeSelf

    /**
     * Sacrifice a creature of the type chosen when this permanent entered the battlefield.
     * Used by cards like Doom Cannon.
     */
    val SacrificeChosenCreatureType: AbilityCost = AbilityCost.SacrificeChosenCreatureType

    /**
     * Tap the creature this aura is attached to.
     * Used by auras that grant activated abilities to enchanted creature (e.g., Lavamancer's Skill).
     */
    val TapAttachedCreature: AbilityCost = AbilityCost.TapAttachedCreature

    // =========================================================================
    // Exile Costs
    // =========================================================================

    /**
     * Exile cards from graveyard.
     */
    fun ExileFromGraveyard(count: Int, filter: GameObjectFilter = GameObjectFilter.Any): AbilityCost =
        AbilityCost.ExileFromGraveyard(count, filter)

    // =========================================================================
    // Loyalty Costs
    // =========================================================================

    /**
     * Add loyalty counters (positive) or remove them (negative).
     */
    fun Loyalty(change: Int): AbilityCost =
        AbilityCost.Loyalty(change)

    // =========================================================================
    // Tap Permanents Costs
    // =========================================================================

    /**
     * Tap permanents you control (e.g., "Tap five untapped Clerics you control").
     */
    fun TapPermanents(count: Int, filter: GameObjectFilter = GameObjectFilter.Creature): AbilityCost =
        AbilityCost.TapPermanents(count, filter)

    // =========================================================================
    // Return to Hand Costs
    // =========================================================================

    /**
     * Return a permanent you control matching the filter to its owner's hand.
     */
    fun ReturnToHand(filter: GameObjectFilter = GameObjectFilter.Any): AbilityCost =
        AbilityCost.ReturnToHand(filter)

    // =========================================================================
    // Composite Costs
    // =========================================================================

    /**
     * Combine multiple costs.
     */
    fun Composite(vararg costs: AbilityCost): AbilityCost =
        AbilityCost.Composite(costs.toList())

    /**
     * Combine multiple costs from a list.
     */
    fun Composite(costs: List<AbilityCost>): AbilityCost =
        AbilityCost.Composite(costs)
}
