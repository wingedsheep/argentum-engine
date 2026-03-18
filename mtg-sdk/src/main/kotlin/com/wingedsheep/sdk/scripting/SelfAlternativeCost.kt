package com.wingedsheep.sdk.scripting

import kotlinx.serialization.Serializable

/**
 * Represents an alternative cost that a spell can define for itself.
 * Unlike [GrantAlternativeCastingCost] (which is a static ability on a permanent
 * that grants alternative costs to other spells), this is declared on the spell's
 * own CardScript.
 *
 * The player may choose to pay the alternative mana cost (plus any additional costs)
 * instead of the spell's normal mana cost.
 *
 * Example: Zahid, Djinn of the Lamp — "You may pay {3}{U} and tap an untapped
 * artifact you control rather than pay this spell's mana cost."
 *
 * @property manaCost The alternative mana cost (e.g., "{3}{U}")
 * @property additionalCosts Non-mana costs that must be paid alongside the alternative
 *           mana cost (e.g., tapping an artifact)
 */
@Serializable
data class SelfAlternativeCost(
    val manaCost: String,
    val additionalCosts: List<AdditionalCost> = emptyList()
)
