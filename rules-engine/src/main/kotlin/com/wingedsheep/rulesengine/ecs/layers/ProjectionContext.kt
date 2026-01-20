package com.wingedsheep.rulesengine.ecs.layers

import com.wingedsheep.rulesengine.ecs.EntityId
import com.wingedsheep.rulesengine.ecs.GameState

/**
 * Context provided during projection for evaluating dynamic effects.
 *
 * This context allows modifications to access game state information when
 * they need to compute dynamic values (CDAs, "X where X is...", etc.).
 *
 * @property state The current game state (read-only access for evaluation)
 * @property entityId The entity being projected
 * @property sourceId The source of the current modifier being applied
 * @property controllerId The controller of the entity being projected
 */
data class ProjectionContext(
    val state: GameState,
    val entityId: EntityId,
    val sourceId: EntityId,
    val controllerId: EntityId
)
