package com.wingedsheep.sdk.scripting

import kotlinx.serialization.SerialName
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
    @SerialName("InstantSpeed")
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
    @SerialName("SorcerySpeed")
    @Serializable
    data object SorcerySpeed : TimingRule

    /**
     * Mana ability: Special timing that does NOT use the stack.
     *
     * MTG Rules 605.1: A mana ability is either:
     * - An activated ability that could add mana to a player's mana pool, doesn't require a target,
     *   isn't a loyalty ability, and whose cost and effect move no card to or from a library
     *   (605.1a — the library clause arrived in the August 7, 2026 update, and is what makes
     *   Chromatic Sphere and Deranged Assistant ordinary activated abilities)
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
     *
     * A mana ability that reads "Activate only as a sorcery" (Grinning Ignus) keeps
     * [SorcerySpeed] as its timing instead, with `isManaAbility = true` still marking it a mana
     * ability: no stack, but sorcery timing (CR 602.5d) and so never usable mid-payment.
     */
    @SerialName("ManaAbility")
    @Serializable
    data object ManaAbility : TimingRule
}
