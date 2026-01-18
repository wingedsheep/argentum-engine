package com.wingedsheep.rulesengine.game

import com.wingedsheep.rulesengine.ecs.EntityId
import com.wingedsheep.rulesengine.player.PlayerId
import kotlinx.serialization.Serializable

/**
 * Tracks the current turn, phase, step, and priority.
 *
 * Uses EntityId for player identification (ECS-compatible).
 */
@Serializable
data class TurnState(
    val turnNumber: Int = 1,
    val activePlayer: EntityId,
    val priorityPlayer: EntityId,
    val phase: Phase = Phase.BEGINNING,
    val step: Step = Step.UNTAP,
    val isFirstTurn: Boolean = true,
    val playerOrder: List<EntityId>
) {
    init {
        require(playerOrder.isNotEmpty()) { "Player order cannot be empty" }
        require(activePlayer in playerOrder) { "Active player must be in player order" }
        require(priorityPlayer in playerOrder) { "Priority player must be in player order" }
    }

    val isMainPhase: Boolean
        get() = step.isMainPhase

    val canPlaySorcerySpeed: Boolean
        get() = step.allowsSorcerySpeed && priorityPlayer == activePlayer

    val activePlayerIndex: Int
        get() = playerOrder.indexOf(activePlayer)

    // ==========================================================================
    // Backward Compatibility (PlayerId-based accessors)
    // ==========================================================================

    /**
     * Backward compatibility: get active player as PlayerId.
     */
    @Deprecated("Use activePlayer (EntityId) instead", ReplaceWith("activePlayer"))
    val activePlayerId: PlayerId
        get() = activePlayer.toPlayerId()

    /**
     * Backward compatibility: get priority player as PlayerId.
     */
    @Deprecated("Use priorityPlayer (EntityId) instead", ReplaceWith("priorityPlayer"))
    val priorityPlayerId: PlayerId
        get() = priorityPlayer.toPlayerId()

    /**
     * Backward compatibility: check if a PlayerId is the active player.
     */
    @Deprecated("Use activePlayer == entityId instead", ReplaceWith("activePlayer == EntityId.fromPlayerId(playerId)"))
    fun isActivePlayer(playerId: PlayerId): Boolean =
        activePlayer == EntityId.fromPlayerId(playerId)

    /**
     * Backward compatibility: check if a PlayerId is the priority player.
     */
    @Deprecated("Use priorityPlayer == entityId instead", ReplaceWith("priorityPlayer == EntityId.fromPlayerId(playerId)"))
    fun isPriorityPlayer(playerId: PlayerId): Boolean =
        priorityPlayer == EntityId.fromPlayerId(playerId)

    fun advanceStep(): TurnState {
        val nextStep = step.next()
        val wrapsToNextTurn = step == Step.CLEANUP

        return if (wrapsToNextTurn) {
            advanceToNextTurn()
        } else {
            copy(
                step = nextStep,
                phase = nextStep.phase,
                priorityPlayer = activePlayer
            )
        }
    }

    fun advanceToNextTurn(): TurnState {
        val nextPlayerIndex = (activePlayerIndex + 1) % playerOrder.size
        val nextPlayer = playerOrder[nextPlayerIndex]

        return copy(
            turnNumber = turnNumber + 1,
            activePlayer = nextPlayer,
            priorityPlayer = nextPlayer,
            phase = Phase.BEGINNING,
            step = Step.UNTAP,
            isFirstTurn = false
        )
    }

    fun advanceToPhase(targetPhase: Phase): TurnState {
        val targetStep = Step.firstStepOf(targetPhase)
        return copy(
            phase = targetPhase,
            step = targetStep,
            priorityPlayer = activePlayer
        )
    }

    fun advanceToStep(targetStep: Step): TurnState {
        return copy(
            phase = targetStep.phase,
            step = targetStep,
            priorityPlayer = activePlayer
        )
    }

    fun passPriority(): TurnState {
        val nextPlayerIndex = (playerOrder.indexOf(priorityPlayer) + 1) % playerOrder.size
        return copy(priorityPlayer = playerOrder[nextPlayerIndex])
    }

    fun resetPriorityToActivePlayer(): TurnState =
        copy(priorityPlayer = activePlayer)

    fun priorityPassedByAllPlayers(startingFrom: EntityId): Boolean =
        priorityPlayer == startingFrom

    companion object {
        /**
         * Create a new game turn state with EntityId player order (preferred).
         */
        fun newGame(playerOrder: List<EntityId>, startingPlayer: EntityId): TurnState {
            require(startingPlayer in playerOrder) { "Starting player must be in player order" }
            return TurnState(
                turnNumber = 1,
                activePlayer = startingPlayer,
                priorityPlayer = startingPlayer,
                phase = Phase.BEGINNING,
                step = Step.UNTAP,
                isFirstTurn = true,
                playerOrder = playerOrder
            )
        }

        /**
         * Create a new game turn state with first player starting.
         */
        fun newGame(playerOrder: List<EntityId>): TurnState {
            require(playerOrder.isNotEmpty()) { "Player order cannot be empty" }
            return newGame(playerOrder, playerOrder.first())
        }

        /**
         * Create from PlayerId list (backward compatibility).
         */
        @Deprecated(
            "Use newGame(List<EntityId>) instead",
            ReplaceWith("newGame(playerOrder.map { EntityId.fromPlayerId(it) })")
        )
        fun fromPlayerIds(playerOrder: List<PlayerId>, startingPlayer: PlayerId? = null): TurnState {
            val entityOrder = playerOrder.map { EntityId.fromPlayerId(it) }
            val starting = startingPlayer?.let { EntityId.fromPlayerId(it) } ?: entityOrder.first()
            return newGame(entityOrder, starting)
        }
    }
}
