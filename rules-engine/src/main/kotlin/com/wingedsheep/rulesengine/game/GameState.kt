package com.wingedsheep.rulesengine.game

import com.wingedsheep.rulesengine.ability.PendingTrigger
import com.wingedsheep.rulesengine.ability.StackedTrigger
import com.wingedsheep.rulesengine.card.CardInstance
import com.wingedsheep.rulesengine.combat.CombatState
import com.wingedsheep.rulesengine.core.CardId
import com.wingedsheep.rulesengine.ecs.EntityId
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
    val combat: CombatState? = null,
    val isGameOver: Boolean = false,
    val winner: PlayerId? = null,
    val pendingTriggers: List<PendingTrigger> = emptyList(),
    val triggersOnStack: List<StackedTrigger> = emptyList()
) {
    init {
        require(players.isNotEmpty()) { "Game must have at least one player" }
        val playerEntityIds = players.keys.map { EntityId.fromPlayerId(it) }.toSet()
        require(playerEntityIds == turnState.playerOrder.toSet()) {
            "All players must be in turn order"
        }
    }

    val activePlayer: Player
        get() = players[turnState.activePlayer.toPlayerId()]
            ?: error("Active player not found: ${turnState.activePlayer}")

    val priorityPlayer: Player
        get() = players[turnState.priorityPlayer.toPlayerId()]
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
        updatePlayer(turnState.activePlayer.toPlayerId(), transform)

    fun updatePriorityPlayer(transform: (Player) -> Player): GameState =
        updatePlayer(turnState.priorityPlayer.toPlayerId(), transform)

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

    // Combat helpers
    val isInCombat: Boolean
        get() = combat != null && currentPhase == Phase.COMBAT

    val defendingPlayer: PlayerId?
        get() = combat?.defendingPlayer?.toPlayerId()

    fun updateCombat(transform: (CombatState) -> CombatState): GameState {
        val currentCombat = combat ?: return this
        return copy(combat = transform(currentCombat))
    }

    fun startCombat(defendingPlayer: PlayerId): GameState {
        return copy(combat = CombatState.create(
            turnState.activePlayer,
            EntityId.fromPlayerId(defendingPlayer)
        ))
    }

    fun endCombat(): GameState = copy(combat = null)

    fun endGame(winner: PlayerId?): GameState =
        copy(isGameOver = true, winner = winner)

    // Trigger helpers
    fun addPendingTriggers(triggers: List<PendingTrigger>): GameState =
        copy(pendingTriggers = pendingTriggers + triggers)

    fun clearPendingTriggers(): GameState =
        copy(pendingTriggers = emptyList())

    fun putTriggerOnStack(trigger: StackedTrigger): GameState =
        copy(triggersOnStack = triggersOnStack + trigger)

    fun removeTopTriggerFromStack(): Pair<StackedTrigger?, GameState> {
        if (triggersOnStack.isEmpty()) return null to this
        val top = triggersOnStack.last()
        return top to copy(triggersOnStack = triggersOnStack.dropLast(1))
    }

    val hasTriggersOnStack: Boolean
        get() = triggersOnStack.isNotEmpty()

    val hasPendingTriggers: Boolean
        get() = pendingTriggers.isNotEmpty()

    companion object {
        fun newGame(players: List<Player>): GameState {
            require(players.isNotEmpty()) { "Game must have at least one player" }
            require(players.size >= 2) { "Game requires at least 2 players" }

            val playerMap = players.associateBy { it.id }
            val playerOrder = players.map { EntityId.fromPlayerId(it.id) }

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
