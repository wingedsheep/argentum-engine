package com.wingedsheep.engine.state.components.identity

import com.wingedsheep.engine.state.Component
import com.wingedsheep.sdk.core.Color
import kotlinx.serialization.Serializable

/**
 * Stores the color chosen when this permanent entered the battlefield.
 * Used by cards like Riptide Replicator ("As this artifact enters, choose a color").
 */
@Serializable
data class ChosenColorComponent(
    val color: Color
) : Component

/**
 * Stores the creature type chosen when this permanent entered the battlefield.
 * Used by cards like Doom Cannon ("As this artifact enters, choose a creature type").
 */
@Serializable
data class ChosenCreatureTypeComponent(
    val creatureType: String
) : Component
