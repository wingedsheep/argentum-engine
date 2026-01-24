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
}
