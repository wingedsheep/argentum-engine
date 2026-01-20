package com.wingedsheep.rulesengine.ecs.components

import com.wingedsheep.rulesengine.core.CardType
import com.wingedsheep.rulesengine.core.Color
import com.wingedsheep.rulesengine.core.Keyword
import com.wingedsheep.rulesengine.core.Subtype
import com.wingedsheep.rulesengine.ecs.Component
import kotlinx.serialization.Serializable

/**
 * Base components store the original values from a card's definition.
 *
 * These are "baked" onto an entity when it enters the battlefield, preserving
 * the printed characteristics from the CardDefinition. During projection,
 * modifiers transform these base values into projected values.
 *
 * Base components serve as the starting point for Layer 4+ projections.
 * They are distinct from projected components which store the results.
 *
 * Example flow:
 * 1. Grizzly Bears enters battlefield
 * 2. BaseStatsComponent(2, 2), BaseTypesComponent({CREATURE}, {Bear}) added
 * 3. Giant Growth cast targeting it
 * 4. Projection reads BaseStatsComponent, applies ModifyPT(+3, +3)
 * 5. Result: ProjectedPTComponent(5, 5)
 */

/**
 * Base power and toughness from card definition.
 * Only added to creatures.
 */
@Serializable
data class BaseStatsComponent(
    val power: Int?,
    val toughness: Int?
) : Component {
    /**
     * Convert to a ProjectedPTComponent for initialization.
     */
    fun toProjected(): ProjectedPTComponent = ProjectedPTComponent(power, toughness)
}

/**
 * Base types and subtypes from card definition.
 */
@Serializable
data class BaseTypesComponent(
    val types: Set<CardType>,
    val subtypes: Set<Subtype>
) : Component {
    /**
     * Convert to a ProjectedTypesComponent for initialization.
     */
    fun toProjected(): ProjectedTypesComponent = ProjectedTypesComponent(types, subtypes)
}

/**
 * Base colors from card definition (derived from mana cost or color indicator).
 */
@Serializable
data class BaseColorsComponent(val colors: Set<Color>) : Component {
    /**
     * Convert to a ProjectedColorsComponent for initialization.
     */
    fun toProjected(): ProjectedColorsComponent = ProjectedColorsComponent(colors)
}

/**
 * Base keywords from card definition.
 */
@Serializable
data class BaseKeywordsComponent(val keywords: Set<Keyword>) : Component {
    /**
     * Convert to a ProjectedAbilitiesComponent for initialization.
     */
    fun toProjected(): ProjectedAbilitiesComponent = ProjectedAbilitiesComponent(
        keywords = keywords,
        hasAbilities = true,
        cantBlock = false,
        assignsDamageEqualToToughness = false
    )
}
