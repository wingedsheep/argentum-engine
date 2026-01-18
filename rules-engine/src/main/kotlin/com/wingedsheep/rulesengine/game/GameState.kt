package com.wingedsheep.rulesengine.game

import com.wingedsheep.rulesengine.card.CardInstance
import com.wingedsheep.rulesengine.core.CardId
import com.wingedsheep.rulesengine.player.Player
import com.wingedsheep.rulesengine.player.PlayerId
import com.wingedsheep.rulesengine.zone.Zone
import com.wingedsheep.rulesengine.zone.ZoneType
import kotlinx.serialization.Serializable

@Serializable
data class GameState(
    val players: Map<PlayerId, Player>,
    val battlefield: Zone,
    val stack: Zone,
    val exile: Zone,
    val turnState: TurnState,
    val isGameOver: Boolean = false,
    val winner: PlayerId? = null
) {
    init {
        require(players.isNotEmpty()) { "Game must have at least one player" }
        require(players.keys.toSet() == turnState.playerOrder.toSet()) {
            "All players must be in turn order"
        }
    }

    val activePlayer: Player
        get() = players[turnState.activePlayer]
            ?: error("Active player not found: ${turnState.activePlayer}")

    val priorityPlayer: Player
        get() = players[turnState.priorityPlayer]
            ?: error("Priority player not found: ${turnState.priorityPlayer}")

    val currentPhase: Phase
        get() = turnState.phase

    val currentStep: Step
        get() = turnState.step

    val turnNumber: Int
        get() = turnState.turnNumber

    val isMainPhase: Boolean
        get() = turnState.isMainPhase

    val stackIsEmpty: Boolean
        get() = stack.isEmpty

    fun getPlayer(playerId: PlayerId): Player =
        players[playerId] ?: error("Player not found: $playerId")

    fun updatePlayer(playerId: PlayerId, transform: (Player) -> Player): GameState {
        val player = getPlayer(playerId)
        return copy(players = players + (playerId to transform(player)))
    }

    fun updateActivePlayer(transform: (Player) -> Player): GameState =
        updatePlayer(turnState.activePlayer, transform)

    fun updatePriorityPlayer(transform: (Player) -> Player): GameState =
        updatePlayer(turnState.priorityPlayer, transform)

    fun updateAllPlayers(transform: (Player) -> Player): GameState =
        copy(players = players.mapValues { (_, player) -> transform(player) })

    fun updateBattlefield(transform: (Zone) -> Zone): GameState =
        copy(battlefield = transform(battlefield))

    fun updateStack(transform: (Zone) -> Zone): GameState =
        copy(stack = transform(stack))

    fun updateExile(transform: (Zone) -> Zone): GameState =
        copy(exile = transform(exile))

    fun updateTurnState(transform: (TurnState) -> TurnState): GameState =
        copy(turnState = transform(turnState))

    fun advanceStep(): GameState =
        copy(turnState = turnState.advanceStep())

    fun advanceToPhase(phase: Phase): GameState =
        copy(turnState = turnState.advanceToPhase(phase))

    fun advanceToStep(step: Step): GameState =
        copy(turnState = turnState.advanceToStep(step))

    fun passPriority(): GameState =
        copy(turnState = turnState.passPriority())

    // Card location helpers
    fun findCard(cardId: CardId): CardLocation? {
        // Check battlefield
        battlefield.getCard(cardId)?.let {
            return CardLocation(it, ZoneType.BATTLEFIELD, null)
        }

        // Check stack
        stack.getCard(cardId)?.let {
            return CardLocation(it, ZoneType.STACK, null)
        }

        // Check exile
        exile.getCard(cardId)?.let {
            return CardLocation(it, ZoneType.EXILE, null)
        }

        // Check player zones
        for ((playerId, player) in players) {
            player.library.getCard(cardId)?.let {
                return CardLocation(it, ZoneType.LIBRARY, playerId)
            }
            player.hand.getCard(cardId)?.let {
                return CardLocation(it, ZoneType.HAND, playerId)
            }
            player.graveyard.getCard(cardId)?.let {
                return CardLocation(it, ZoneType.GRAVEYARD, playerId)
            }
        }

        return null
    }

    fun getCardsOnBattlefield(): List<CardInstance> = battlefield.cards

    fun getCreaturesOnBattlefield(): List<CardInstance> =
        battlefield.cards.filter { it.isCreature }

    fun getCreaturesControlledBy(playerId: PlayerId): List<CardInstance> =
        battlefield.cards.filter { it.isCreature && it.controllerId == playerId.value }

    fun getPermanentsControlledBy(playerId: PlayerId): List<CardInstance> =
        battlefield.cards.filter { it.controllerId == playerId.value }

    fun endGame(winner: PlayerId?): GameState =
        copy(isGameOver = true, winner = winner)

    companion object {
        fun newGame(players: List<Player>): GameState {
            require(players.isNotEmpty()) { "Game must have at least one player" }
            require(players.size >= 2) { "Game requires at least 2 players" }

            val playerMap = players.associateBy { it.id }
            val playerOrder = players.map { it.id }

            return GameState(
                players = playerMap,
                battlefield = Zone.battlefield(),
                stack = Zone.stack(),
                exile = Zone.exile(),
                turnState = TurnState.newGame(playerOrder)
            )
        }

        fun newGame(player1: Player, player2: Player): GameState =
            newGame(listOf(player1, player2))
    }
}

@Serializable
data class CardLocation(
    val card: CardInstance,
    val zone: ZoneType,
    val owner: PlayerId?
)
