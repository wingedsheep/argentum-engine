package com.wingedsheep.rulesengine.ecs.components

import com.wingedsheep.rulesengine.ecs.Component
import com.wingedsheep.rulesengine.ecs.EntityId
import kotlinx.serialization.Serializable

/**
 * Components that represent pending player choices during state-based action checking.
 *
 * These components follow the ECS pattern: SBA checking detects conditions and creates
 * pending choice state, the game loop prompts for player input, and separate actions
 * resolve the choices.
 */

/**
 * Represents a pending Legend Rule choice that a player must make.
 *
 * When a player controls multiple legendary permanents with the same name,
 * they must choose which one to keep. This component stores the pending choice
 * until the player makes their decision.
 *
 * @property controllerId The player who must make the choice
 * @property legendaryName The name of the legendary permanent (for display)
 * @property duplicateIds All legendary permanents with this name controlled by this player
 */
@Serializable
data class PendingLegendRuleChoice(
    val controllerId: EntityId,
    val legendaryName: String,
    val duplicateIds: List<EntityId>
) {
    init {
        require(duplicateIds.size >= 2) { "Legend rule requires at least 2 duplicates" }
    }
}

/**
 * Component attached to the game state (stored in globalFlags or a dedicated field)
 * to track all pending legend rule choices that need resolution.
 *
 * This allows the game loop to detect when player input is needed.
 */
@Serializable
data class PendingLegendRuleChoicesComponent(
    val choices: List<PendingLegendRuleChoice> = emptyList()
) : Component {

    val hasPendingChoices: Boolean get() = choices.isNotEmpty()

    fun addChoice(choice: PendingLegendRuleChoice): PendingLegendRuleChoicesComponent =
        copy(choices = choices + choice)

    fun removeChoice(choice: PendingLegendRuleChoice): PendingLegendRuleChoicesComponent =
        copy(choices = choices - choice)

    fun getChoicesForPlayer(playerId: EntityId): List<PendingLegendRuleChoice> =
        choices.filter { it.controllerId == playerId }

    companion object {
        val EMPTY = PendingLegendRuleChoicesComponent()
    }
}
