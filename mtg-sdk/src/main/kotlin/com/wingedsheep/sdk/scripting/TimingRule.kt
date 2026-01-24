package com.wingedsheep.sdk.scripting

import kotlinx.serialization.Serializable

/**
 * Defines timing rules for when spells can be cast or abilities can be activated.
 *
 * This is the single source of truth for timing concepts like "sorcery speed" and "instant speed",
 * shared between spell cast restrictions and activated ability timing.
 *
 * MTG Rules Reference:
 * - "Any time you could cast an instant" = whenever you have priority
 * - "Any time you could cast a sorcery" = during your main phase, stack empty, you have priority
 */
@Serializable
sealed interface TimingRule {

    /**
     * Instant speed: Can be used whenever you have priority.
     * This is the default for most instants and activated abilities.
     */
    @Serializable
    data object InstantSpeed : TimingRule

    /**
     * Sorcery speed: Can only be used during your main phase,
     * when the stack is empty, and you have priority.
     *
     * This is the default timing for:
     * - Sorceries
     * - Creatures, artifacts, enchantments, planeswalkers
     * - Activated abilities with "Activate only as a sorcery" text
     */
    @Serializable
    data object SorcerySpeed : TimingRule

    /**
     * Mana ability: Special timing that does NOT use the stack.
     *
     * MTG Rules 605.1: A mana ability is either:
     * - An activated ability without a target that could add mana to a player's mana pool
     * - A triggered ability without a target that triggers from a mana ability
     *
     * Key characteristics:
     * - Does NOT go on the stack (Rule 605.3a)
     * - Resolves immediately when activated
     * - Cannot be responded to or countered (no Stifle, Disallow, etc.)
     * - Can be activated during mana payment even without priority
     * - Can be activated any time the player has priority
     *
     * Examples:
     * - Tapping a basic land for mana
     * - Llanowar Elves' "{T}: Add {G}"
     * - Black Lotus' "{T}, Sacrifice: Add three mana of any one color"
     */
    @Serializable
    data object ManaAbility : TimingRule
}
