package com.wingedsheep.rulesengine.game

import com.wingedsheep.rulesengine.player.PlayerId
import kotlinx.serialization.Serializable

@Serializable
data class TurnState(
    val turnNumber: Int = 1,
    val activePlayer: PlayerId,
    val priorityPlayer: PlayerId,
    val phase: Phase = Phase.BEGINNING,
    val step: Step = Step.UNTAP,
    val isFirstTurn: Boolean = true,
    val playerOrder: List<PlayerId>
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

    fun priorityPassedByAllPlayers(startingFrom: PlayerId): Boolean =
        priorityPlayer == startingFrom

    companion object {
        fun newGame(playerOrder: List<PlayerId>, startingPlayer: PlayerId): TurnState {
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

        fun newGame(playerOrder: List<PlayerId>): TurnState {
            require(playerOrder.isNotEmpty()) { "Player order cannot be empty" }
            return newGame(playerOrder, playerOrder.first())
        }
    }
}
