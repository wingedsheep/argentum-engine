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
     * No cost ({0}) — the ability is free to activate.
     */
    val Free: AbilityCost = AbilityCost.Free

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

    /**
     * Sacrifice another permanent matching the filter (excludes the source permanent).
     */
    fun SacrificeAnother(filter: GameObjectFilter = GameObjectFilter.Any): AbilityCost =
        AbilityCost.Sacrifice(filter, excludeSelf = true)

    /**
     * Sacrifice multiple permanents matching the filter.
     */
    fun SacrificeMultiple(count: Int, filter: GameObjectFilter = GameObjectFilter.Any): AbilityCost =
        AbilityCost.Sacrifice(filter, count = count)

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
     * Exile this creature (for abilities that exile themselves as cost).
     */
    val ExileSelf: AbilityCost = AbilityCost.ExileSelf

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

    /**
     * Exile X cards from graveyard, where X is the ability's X value.
     */
    fun ExileXFromGraveyard(filter: GameObjectFilter = GameObjectFilter.Any): AbilityCost =
        AbilityCost.ExileXFromGraveyard(filter)

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

    /**
     * Tap X permanents you control, where X is the ability's chosen X value.
     * Example: "Tap X untapped Knights you control" for Aryel, Knight of Windgrace.
     */
    fun TapXPermanents(filter: GameObjectFilter = GameObjectFilter.Creature): AbilityCost =
        AbilityCost.TapXPermanents(filter)

    // =========================================================================
    // Return to Hand Costs
    // =========================================================================

    /**
     * Return a permanent you control matching the filter to its owner's hand.
     */
    fun ReturnToHand(filter: GameObjectFilter = GameObjectFilter.Any, count: Int = 1): AbilityCost =
        AbilityCost.ReturnToHand(filter, count)

    // =========================================================================
    // Counter Removal Costs
    // =========================================================================

    /**
     * Remove X +1/+1 counters from among creatures you control.
     * X is chosen by the player; the engine auto-distributes counter removal.
     */
    val RemoveXPlusOnePlusOneCounters: AbilityCost = AbilityCost.RemoveXPlusOnePlusOneCounters

    /**
     * Remove one or more counters of the specified type from this permanent.
     * Used for artifacts with charge/gem counters as activation costs.
     */
    fun RemoveCounterFromSelf(counterType: String, count: Int = 1): AbilityCost =
        AbilityCost.RemoveCounterFromSelf(counterType, count)

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

    /**
     * Forage: exile three cards from your graveyard or sacrifice a Food.
     */
    fun Forage(): AbilityCost = AbilityCost.Forage
}
