package com.wingedsheep.rulesengine.targeting

import com.wingedsheep.rulesengine.ability.ChosenTarget
import com.wingedsheep.rulesengine.core.CardId
import com.wingedsheep.rulesengine.ecs.EntityId
import com.wingedsheep.rulesengine.player.PlayerId
import kotlinx.serialization.Serializable

/**
 * Represents a potential or selected target for a spell or ability.
 */
@Serializable
sealed interface Target {
    /**
     * A player as a target.
     *
     * Uses EntityId for ECS compatibility. The playerId property provides
     * backward compatibility with the old PlayerId-based system.
     */
    @Serializable
    data class PlayerTarget(val entityId: EntityId) : Target {
        /**
         * Get the PlayerId for backward compatibility with old system.
         */
        val playerId: PlayerId get() = entityId.toPlayerId()

        companion object {
            /**
             * Create from PlayerId (backward compatibility).
             */
            @Deprecated(
                "Use PlayerTarget(EntityId) instead",
                ReplaceWith("PlayerTarget(EntityId.fromPlayerId(playerId))")
            )
            fun fromPlayerId(playerId: PlayerId): PlayerTarget =
                PlayerTarget(EntityId.fromPlayerId(playerId))
        }
    }

    /**
     * A card (on battlefield, stack, graveyard, etc.) as a target.
     */
    @Serializable
    data class CardTarget(val cardId: CardId) : Target

    companion object {
        /**
         * Create a player target from an EntityId (preferred).
         */
        fun player(entityId: EntityId): Target = PlayerTarget(entityId)

        /**
         * Create a player target from a PlayerId (backward compatibility).
         */
        @Deprecated(
            "Use player(EntityId) instead",
            ReplaceWith("player(EntityId.fromPlayerId(playerId))")
        )
        fun player(playerId: PlayerId): Target = PlayerTarget(EntityId.fromPlayerId(playerId))

        /**
         * Create a player target from a string ID.
         */
        fun player(playerId: String): Target = PlayerTarget(EntityId.of(playerId))

        fun card(cardId: CardId): Target = CardTarget(cardId)
        fun card(cardId: String): Target = CardTarget(CardId(cardId))
    }
}

/**
 * Convert a Target to a ChosenTarget (for compatibility with existing effect system).
 */
fun Target.toChosenTarget(): ChosenTarget = when (this) {
    is Target.PlayerTarget -> ChosenTarget.PlayerTarget(entityId)
    is Target.CardTarget -> ChosenTarget.CardTarget(cardId)
}

/**
 * Convert a ChosenTarget to a Target.
 */
fun ChosenTarget.toTarget(): Target = when (this) {
    is ChosenTarget.PlayerTarget -> Target.PlayerTarget(entityId)
    is ChosenTarget.CardTarget -> Target.CardTarget(cardId)
}

/**
 * Represents the targets selected for a spell or ability.
 * Includes information about requirements and validation state.
 */
@Serializable
data class TargetSelection(
    val requirements: List<TargetRequirement>,
    val selectedTargets: Map<Int, List<Target>> = emptyMap()  // Index -> targets for that requirement
) {
    /**
     * Check if all required targets have been selected.
     */
    val isComplete: Boolean
        get() = requirements.indices.all { index ->
            val requirement = requirements[index]
            val targets = selectedTargets[index] ?: emptyList()
            if (requirement.optional) {
                targets.size <= requirement.count
            } else {
                targets.size == requirement.count
            }
        }

    /**
     * Get all selected targets as a flat list.
     */
    val allTargets: List<Target>
        get() = selectedTargets.values.flatten()

    /**
     * Get all selected targets as ChosenTargets.
     */
    val asChosenTargets: List<ChosenTarget>
        get() = allTargets.map { it.toChosenTarget() }

    /**
     * Add a target for a specific requirement.
     */
    fun addTarget(requirementIndex: Int, target: Target): TargetSelection {
        val currentTargets = selectedTargets[requirementIndex] ?: emptyList()
        return copy(selectedTargets = selectedTargets + (requirementIndex to currentTargets + target))
    }

    /**
     * Remove a target from a specific requirement.
     */
    fun removeTarget(requirementIndex: Int, target: Target): TargetSelection {
        val currentTargets = selectedTargets[requirementIndex] ?: emptyList()
        return copy(selectedTargets = selectedTargets + (requirementIndex to currentTargets - target))
    }

    /**
     * Clear all targets for a specific requirement.
     */
    fun clearTargets(requirementIndex: Int): TargetSelection {
        return copy(selectedTargets = selectedTargets - requirementIndex)
    }

    /**
     * Clear all targets.
     */
    fun clearAllTargets(): TargetSelection {
        return copy(selectedTargets = emptyMap())
    }

    companion object {
        /**
         * Create an empty target selection with the given requirements.
         */
        fun forRequirements(requirements: List<TargetRequirement>): TargetSelection =
            TargetSelection(requirements)

        /**
         * Create a target selection with a single requirement.
         */
        fun forRequirement(requirement: TargetRequirement): TargetSelection =
            TargetSelection(listOf(requirement))

        /**
         * Create a target selection with no requirements (for spells that don't target).
         */
        fun none(): TargetSelection = TargetSelection(emptyList())
    }
}

/**
 * Represents a spell or ability with its target requirements and current selections.
 */
@Serializable
data class TargetingInfo(
    val sourceId: CardId,
    val sourceName: String,
    val controllerId: PlayerId,
    val requirements: List<TargetRequirement>,
    val selection: TargetSelection = TargetSelection.forRequirements(requirements)
) {
    val isComplete: Boolean get() = selection.isComplete

    val allSelectedTargets: List<Target> get() = selection.allTargets

    fun withTarget(requirementIndex: Int, target: Target): TargetingInfo =
        copy(selection = selection.addTarget(requirementIndex, target))

    fun withoutTarget(requirementIndex: Int, target: Target): TargetingInfo =
        copy(selection = selection.removeTarget(requirementIndex, target))
}
