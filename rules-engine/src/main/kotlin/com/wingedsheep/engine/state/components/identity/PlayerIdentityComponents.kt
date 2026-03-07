package com.wingedsheep.engine.state.components.identity

import com.wingedsheep.engine.state.Component
import kotlinx.serialization.Serializable

/**
 * Marks an entity as a player.
 */
@Serializable
data class PlayerComponent(
    val name: String,
    val startingLifeTotal: Int = 20
) : Component

/**
 * Player's current life total.
 */
@Serializable
data class LifeTotalComponent(
    val life: Int
) : Component
