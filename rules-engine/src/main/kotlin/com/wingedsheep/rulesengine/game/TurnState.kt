package com.wingedsheep.rulesengine.game

import com.wingedsheep.rulesengine.ecs.EntityId
import kotlinx.serialization.Serializable

/**
 * Tracks the current turn, phase, step, and priority.
 *
 * Uses EntityId for player identification.
 */
@Serializable
data class TurnState(
    val turnNumber: Int = 1,
    val activePlayer: EntityId,
    val priorityPlayer: EntityId,
    val phase: Phase = Phase.BEGINNING,
    val step: Step = Step.UNTAP,
    val isFirstTurn: Boolean = true,
    val playerOrder: List<EntityId>,
    val consecutivePasses: Int = 0
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

    fun advanceStep(): TurnState {
        val nextStep = step.next()
        val wrapsToNextTurn = step == Step.CLEANUP

        return if (wrapsToNextTurn) {
            advanceToNextTurn()
        } else {
            copy(
                step = nextStep,
                phase = nextStep.phase,
                priorityPlayer = activePlayer,
                consecutivePasses = 0
            )
        }
    }

    /**
     * Internal: Advance to the next turn. Only called from advanceStep() when at CLEANUP.
     */
    private fun advanceToNextTurn(): TurnState {
        val nextPlayerIndex = (activePlayerIndex + 1) % playerOrder.size
        val nextPlayer = playerOrder[nextPlayerIndex]

        return copy(
            turnNumber = turnNumber + 1,
            activePlayer = nextPlayer,
            priorityPlayer = nextPlayer,
            phase = Phase.BEGINNING,
            step = Step.UNTAP,
            isFirstTurn = false,
            consecutivePasses = 0
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

    // =========================================================================
    // Consecutive Passes Tracking (for Priority State Machine)
    // =========================================================================

    /**
     * Increment the consecutive passes counter.
     * Called when a player passes priority.
     */
    fun incrementConsecutivePasses(): TurnState =
        copy(consecutivePasses = consecutivePasses + 1)

    /**
     * Reset consecutive passes to zero.
     * Called when a player takes an action or the stack resolves.
     */
    fun resetConsecutivePasses(): TurnState =
        copy(consecutivePasses = 0)

    /**
     * Check if all players have passed priority in succession.
     * When true, the top of stack resolves (if not empty) or the step/phase advances.
     */
    fun allPlayersPassed(): Boolean =
        consecutivePasses >= playerOrder.size

    companion object {
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

        fun newGame(playerOrder: List<EntityId>): TurnState {
            require(playerOrder.isNotEmpty()) { "Player order cannot be empty" }
            return newGame(playerOrder, playerOrder.first())
        }
    }
}
