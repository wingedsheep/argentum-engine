package com.wingedsheep.engine.state.components.identity

import com.wingedsheep.engine.state.Component
import com.wingedsheep.sdk.model.EntityId
import kotlinx.serialization.Serializable

/**
 * Who owns this entity (original owner, doesn't change).
 */
@Serializable
data class OwnerComponent(
    val playerId: EntityId
) : Component

/**
 * Who controls this entity (can change via effects).
 */
@Serializable
data class ControllerComponent(
    val playerId: EntityId
) : Component
