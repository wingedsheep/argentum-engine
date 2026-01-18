package com.wingedsheep.rulesengine.ecs.components

import com.wingedsheep.rulesengine.core.Keyword
import com.wingedsheep.rulesengine.ecs.Component
import kotlinx.serialization.Serializable

/**
 * Creature-specific components for power/toughness and keywords.
 */

/**
 * Power and toughness for creatures.
 *
 * Tracks both base values (from card definition) and modifiers (from effects).
 * Does NOT include counters - those are handled separately by CountersComponent.
 *
 * The final P/T calculation is:
 * - Power = basePower + powerModifier + plusOnePlusOneCounters
 * - Toughness = baseToughness + toughnessModifier + plusOnePlusOneCounters - minusOneMinusOneCounters
 *
 * @property basePower Power from the card definition
 * @property baseToughness Toughness from the card definition
 * @property powerModifier Temporary power modifiers from effects (e.g., Giant Growth)
 * @property toughnessModifier Temporary toughness modifiers from effects
 */
@Serializable
data class PTComponent(
    val basePower: Int,
    val baseToughness: Int,
    val powerModifier: Int = 0,
    val toughnessModifier: Int = 0
) : Component {
    /**
     * Current power (base + modifier, not including counters).
     */
    val currentPower: Int get() = basePower + powerModifier

    /**
     * Current toughness (base + modifier, not including counters).
     */
    val currentToughness: Int get() = baseToughness + toughnessModifier

    /**
     * Apply a power/toughness modification.
     */
    fun modify(power: Int = 0, toughness: Int = 0): PTComponent =
        copy(
            powerModifier = powerModifier + power,
            toughnessModifier = toughnessModifier + toughness
        )

    /**
     * Clear all modifiers (for set-to effects).
     */
    fun clearModifiers(): PTComponent =
        copy(powerModifier = 0, toughnessModifier = 0)

    companion object {
        /**
         * Create a PTComponent from base stats.
         */
        fun of(power: Int, toughness: Int): PTComponent =
            PTComponent(basePower = power, baseToughness = toughness)
    }
}

/**
 * Keyword modifications for a card.
 *
 * Tracks keywords that have been added or removed from the card's base keywords
 * (defined in CardDefinition). Also tracks temporary keywords that last until
 * end of turn.
 *
 * The effective keywords are:
 * (baseKeywords + added + temporary) - removed
 *
 * @property added Keywords permanently added to this card
 * @property removed Keywords permanently removed from this card
 * @property temporary Keywords added until end of turn
 */
@Serializable
data class KeywordsComponent(
    val added: Set<Keyword> = emptySet(),
    val removed: Set<Keyword> = emptySet(),
    val temporary: Set<Keyword> = emptySet()
) : Component {
    /**
     * Calculate effective keywords given base keywords from definition.
     */
    fun effectiveKeywords(baseKeywords: Set<Keyword>): Set<Keyword> =
        (baseKeywords + added + temporary) - removed

    /**
     * Add a permanent keyword.
     */
    fun addKeyword(keyword: Keyword): KeywordsComponent =
        copy(added = added + keyword)

    /**
     * Remove a keyword permanently.
     */
    fun removeKeyword(keyword: Keyword): KeywordsComponent =
        copy(removed = removed + keyword)

    /**
     * Add a temporary keyword (until end of turn).
     */
    fun addTemporaryKeyword(keyword: Keyword): KeywordsComponent =
        copy(temporary = temporary + keyword)

    /**
     * Clear all temporary keywords (at end of turn).
     */
    fun clearTemporary(): KeywordsComponent =
        copy(temporary = emptySet())

    /**
     * Check if any modifications exist.
     */
    val hasModifications: Boolean
        get() = added.isNotEmpty() || removed.isNotEmpty() || temporary.isNotEmpty()

    companion object {
        val EMPTY = KeywordsComponent()
    }
}
