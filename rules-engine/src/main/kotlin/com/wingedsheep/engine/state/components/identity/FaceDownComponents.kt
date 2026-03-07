package com.wingedsheep.engine.state.components.identity

import com.wingedsheep.engine.state.Component
import com.wingedsheep.sdk.scripting.costs.PayCost
import kotlinx.serialization.Serializable

/**
 * Face-down status for morph/manifest.
 */
@Serializable
data object FaceDownComponent : Component

/**
 * Stores the morph cost and original card identity for face-down creatures.
 * This allows the creature to be turned face up by paying the morph cost.
 */
@Serializable
data class MorphDataComponent(
    val morphCost: PayCost,
    val originalCardDefinitionId: String
) : Component

/**
 * Marks a card as having a morph keyword ability.
 * Present on the entity regardless of zone, so filters can check
 * whether a card in hand/library/graveyard has morph.
 */
@Serializable
data object HasMorphAbilityComponent : Component
