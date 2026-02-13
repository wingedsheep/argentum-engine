package com.wingedsheep.sdk.scripting

import com.wingedsheep.sdk.core.Subtype
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// =============================================================================
// Hand/Library Conditions
// =============================================================================

/**
 * Condition: "If you have no cards in hand"
 */
@SerialName("EmptyHand")
@Serializable
data object EmptyHand : Condition {
    override val description: String = "if you have no cards in hand"
}

/**
 * Condition: "If you have X or more cards in hand"
 */
@SerialName("CardsInHandAtLeast")
@Serializable
data class CardsInHandAtLeast(val count: Int) : Condition {
    override val description: String = "if you have $count or more cards in hand"
}

/**
 * Condition: "If you have X or fewer cards in hand"
 */
@SerialName("CardsInHandAtMost")
@Serializable
data class CardsInHandAtMost(val count: Int) : Condition {
    override val description: String = "if you have $count or fewer cards in hand"
}

// =============================================================================
// Graveyard Conditions
// =============================================================================

/**
 * Condition: "If there are X or more creature cards in your graveyard"
 */
@SerialName("CreatureCardsInGraveyardAtLeast")
@Serializable
data class CreatureCardsInGraveyardAtLeast(val count: Int) : Condition {
    override val description: String = "if there are $count or more creature cards in your graveyard"
}

/**
 * Condition: "If there are X or more cards in your graveyard"
 */
@SerialName("CardsInGraveyardAtLeast")
@Serializable
data class CardsInGraveyardAtLeast(val count: Int) : Condition {
    override val description: String = "if there are $count or more cards in your graveyard"
}

/**
 * Condition: "If there is a [subtype] card in your graveyard"
 * Used for tribal synergies like Dawnhand Eulogist.
 */
@SerialName("GraveyardContainsSubtype")
@Serializable
data class GraveyardContainsSubtype(val subtype: Subtype) : Condition {
    override val description: String = "if there is a ${subtype.value} card in your graveyard"
}
