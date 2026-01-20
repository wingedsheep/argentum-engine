package com.wingedsheep.rulesengine.ecs.components

import com.wingedsheep.rulesengine.core.CardType
import com.wingedsheep.rulesengine.core.Color
import com.wingedsheep.rulesengine.core.Keyword
import com.wingedsheep.rulesengine.core.Subtype
import com.wingedsheep.rulesengine.ecs.Component
import com.wingedsheep.rulesengine.ecs.EntityId
import kotlinx.serialization.Serializable

/**
 * Projected components represent the result of projection through Rule 613 layers.
 *
 * These components are created during state projection and reflect the "current"
 * state of an entity after all continuous effects have been applied. They are
 * distinct from base components which store the original values from card definitions.
 *
 * The projection pipeline:
 * 1. Read base components (or card definition)
 * 2. Apply modifiers in layer order
 * 3. Store results in projected components
 *
 * Consumers can then read projected components for the effective game state.
 */

/**
 * Projected controller after Layer 2 effects.
 * Only present if control has been modified from the default.
 */
@Serializable
data class ProjectedControlComponent(val controllerId: EntityId) : Component

/**
 * Projected types and subtypes after Layer 4 effects.
 */
@Serializable
data class ProjectedTypesComponent(
    val types: Set<CardType>,
    val subtypes: Set<Subtype>
) : Component

/**
 * Projected colors after Layer 5 effects.
 */
@Serializable
data class ProjectedColorsComponent(val colors: Set<Color>) : Component

/**
 * Projected abilities after Layer 6 effects.
 *
 * @property keywords The effective keywords after all modifications
 * @property hasAbilities Whether the object retains its printed abilities (false if Humility-style effect)
 * @property cantBlock Whether this creature is prevented from blocking
 * @property assignsDamageEqualToToughness Whether this creature assigns combat damage equal to toughness (Doran effect)
 */
@Serializable
data class ProjectedAbilitiesComponent(
    val keywords: Set<Keyword>,
    val hasAbilities: Boolean = true,
    val cantBlock: Boolean = false,
    val assignsDamageEqualToToughness: Boolean = false
) : Component

/**
 * Projected power and toughness after Layer 7 effects.
 * Null values indicate non-creature or removed P/T.
 */
@Serializable
data class ProjectedPTComponent(
    val power: Int?,
    val toughness: Int?
) : Component
