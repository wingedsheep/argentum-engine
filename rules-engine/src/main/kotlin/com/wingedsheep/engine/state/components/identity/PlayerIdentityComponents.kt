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

/**
 * Team membership for a player entity (Two-Headed Giant and other team variants — CR 810).
 *
 * Players sharing a [teamIndex] are on the same team. The index is assigned at game init from
 * [com.wingedsheep.engine.core.GameConfig.teams]. In non-team formats no player carries this
 * component and every player is implicitly a team of one — see [com.wingedsheep.engine.state.GameState.teamOf]
 * / [com.wingedsheep.engine.state.GameState.teams], so all team-aware helpers degrade to per-player
 * behaviour with no change to existing games.
 */
@Serializable
data class TeamComponent(
    val teamIndex: Int
) : Component
