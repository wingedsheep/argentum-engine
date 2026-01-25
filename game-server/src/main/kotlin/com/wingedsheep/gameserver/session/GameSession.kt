package com.wingedsheep.gameserver.session

import com.wingedsheep.gameserver.dto.ClientGameState
import com.wingedsheep.gameserver.dto.ClientStateTransformer
import com.wingedsheep.gameserver.protocol.GameOverReason
import com.wingedsheep.gameserver.protocol.LegalActionInfo
import com.wingedsheep.gameserver.protocol.ServerMessage
import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.mechanics.mana.ManaSolver
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.player.LandDropsComponent
import com.wingedsheep.engine.state.components.player.MulliganStateComponent
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.ZoneType
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.targeting.*
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import org.slf4j.LoggerFactory
import java.util.UUID

private val logger = LoggerFactory.getLogger(GameSession::class.java)

/**
 * Represents an active game session between two players.
 *
 * This session acts as a thin wrapper around the engine's ActionProcessor.
 * The engine handles all game logic including mulligan state tracking.
 */
class GameSession(
    val sessionId: String = UUID.randomUUID().toString(),
    private val cardRegistry: CardRegistry,
    private val stateTransformer: ClientStateTransformer = ClientStateTransformer()
) {
    private var gameState: GameState? = null
    private val players = mutableMapOf<EntityId, PlayerSession>()
    private val deckLists = mutableMapOf<EntityId, List<String>>()

    private val actionProcessor = ActionProcessor(cardRegistry)
    private val gameInitializer = GameInitializer(cardRegistry)
    private val manaSolver = ManaSolver(cardRegistry)

    val player1: PlayerSession? get() = players.values.firstOrNull()
    val player2: PlayerSession? get() = players.values.drop(1).firstOrNull()

    val isFull: Boolean get() = players.size >= 2
    val isReady: Boolean get() = players.size == 2 && deckLists.size == 2
    val isStarted: Boolean get() = gameState != null

    /**
     * Check if we're in mulligan phase by looking at engine's mulligan state.
     */
    val isMulliganPhase: Boolean
        get() {
            val state = gameState ?: return false
            return state.turnOrder.any { playerId ->
                val mullState = state.getEntity(playerId)?.get<MulliganStateComponent>()
                mullState != null && !mullState.hasKept
            }
        }

    /**
     * Check if all mulligans are complete.
     */
    val allMulligansComplete: Boolean
        get() {
            val state = gameState ?: return false
            return state.turnOrder.all { playerId ->
                val mullState = state.getEntity(playerId)?.get<MulliganStateComponent>()
                mullState?.hasKept == true && mullState.cardsToBottom == 0
            }
        }

    /**
     * Add a player to this game session.
     * Returns the assigned EntityId for this player.
     */
    fun addPlayer(playerSession: PlayerSession, deckList: Map<String, Int>): EntityId {
        require(!isFull) { "Game session is full" }

        val playerId = playerSession.playerId
        players[playerId] = playerSession

        // Convert deck list map to flat list of card names
        val cards = deckList.flatMap { (cardName, count) ->
            List(count) { cardName }
        }
        deckLists[playerId] = cards
        playerSession.currentGameSessionId = sessionId

        return playerId
    }

    /**
     * Remove a player from the session.
     */
    fun removePlayer(playerId: EntityId) {
        players[playerId]?.currentGameSessionId = null
        players.remove(playerId)
        deckLists.remove(playerId)
    }

    /**
     * Get the opponent's player ID.
     */
    fun getOpponentId(playerId: EntityId): EntityId? {
        return players.keys.firstOrNull { it != playerId }
    }

    /**
     * Get the player session for a player ID.
     */
    fun getPlayerSession(playerId: EntityId): PlayerSession? = players[playerId]

    /**
     * Start the game. Both players must have joined with deck lists.
     * Initializes the game with the new engine - mulligan phase is handled by the engine.
     */
    fun startGame(): GameState {
        require(isReady) { "Game session not ready - need 2 players with deck lists" }

        val playerConfigs = players.map { (playerId, session) ->
            PlayerConfig(
                name = session.playerName,
                deck = Deck(deckLists[playerId]!!),
                playerId = playerId  // Pass existing player ID to the engine
            )
        }

        val config = GameConfig(
            players = playerConfigs
        )

        val result = gameInitializer.initializeGame(config)
        gameState = result.state
        return result.state
    }

    /**
     * Get the mulligan count for a player.
     */
    fun getMulliganCount(playerId: EntityId): Int {
        val state = gameState ?: return 0
        val mullState = state.getEntity(playerId)?.get<MulliganStateComponent>()
        return mullState?.mulligansTaken ?: 0
    }

    /**
     * Check if a player has completed their mulligan.
     */
    fun hasMulliganComplete(playerId: EntityId): Boolean {
        val state = gameState ?: return false
        val mullState = state.getEntity(playerId)?.get<MulliganStateComponent>()
        return mullState?.hasKept == true && mullState.cardsToBottom == 0
    }

    /**
     * Check if a player is awaiting bottom card selection.
     */
    fun isAwaitingBottomCards(playerId: EntityId): Boolean {
        val state = gameState ?: return false
        val mullState = state.getEntity(playerId)?.get<MulliganStateComponent>()
        return mullState?.hasKept == true && mullState.cardsToBottom > 0
    }

    /**
     * Get the number of cards player needs to put on bottom.
     */
    fun getCardsToBottom(playerId: EntityId): Int {
        val state = gameState ?: return 0
        val mullState = state.getEntity(playerId)?.get<MulliganStateComponent>()
        return if (mullState?.hasKept == true) mullState.cardsToBottom else 0
    }

    /**
     * Get the player's current hand for mulligan decisions.
     */
    fun getHand(playerId: EntityId): List<EntityId> {
        val state = gameState ?: return emptyList()
        return state.getHand(playerId)
    }

    /**
     * Player chooses to keep their current hand.
     * Routes through the engine's action processor.
     */
    fun keepHand(playerId: EntityId): MulliganActionResult {
        val state = gameState ?: return MulliganActionResult.Failure("Game not started")

        val action = KeepHand(playerId)
        val result = actionProcessor.process(state, action)

        val error = result.error
        return if (error != null) {
            MulliganActionResult.Failure(error)
        } else {
            gameState = result.state
            val mullState = result.state.getEntity(playerId)?.get<MulliganStateComponent>()
            if (mullState?.cardsToBottom ?: 0 > 0) {
                MulliganActionResult.NeedsBottomCards(mullState!!.cardsToBottom)
            } else {
                MulliganActionResult.Success
            }
        }
    }

    /**
     * Player chooses to mulligan - shuffle hand and draw a new hand.
     * Routes through the engine's action processor.
     */
    fun takeMulligan(playerId: EntityId): MulliganActionResult {
        val state = gameState ?: return MulliganActionResult.Failure("Game not started")

        val action = TakeMulligan(playerId)
        val result = actionProcessor.process(state, action)

        val error = result.error
        return if (error != null) {
            MulliganActionResult.Failure(error)
        } else {
            gameState = result.state
            MulliganActionResult.Success
        }
    }

    /**
     * Player chooses which cards to put on the bottom of their library.
     * Routes through the engine's action processor.
     */
    fun chooseBottomCards(playerId: EntityId, cardIds: List<EntityId>): MulliganActionResult {
        val state = gameState ?: return MulliganActionResult.Failure("Game not started")

        val action = BottomCards(playerId, cardIds)
        val result = actionProcessor.process(state, action)

        val error = result.error
        return if (error != null) {
            MulliganActionResult.Failure(error)
        } else {
            gameState = result.state
            MulliganActionResult.Success
        }
    }

    /**
     * Get the mulligan decision message for a player.
     */
    fun getMulliganDecision(playerId: EntityId): ServerMessage.MulliganDecision {
        val hand = getHand(playerId)
        val count = getMulliganCount(playerId)
        val state = gameState
        val cards = if (state != null) {
            hand.associateWith { entityId ->
                val cardComponent = state.getEntity(entityId)?.get<CardComponent>()
                ServerMessage.MulliganCardInfo(
                    name = cardComponent?.name ?: "Unknown",
                    imageUri = null // TODO: Add image URI support
                )
            }
        } else {
            emptyMap()
        }
        return ServerMessage.MulliganDecision(
            hand = hand,
            mulliganCount = count,
            cardsToPutOnBottom = count,
            cards = cards
        )
    }

    /**
     * Get the choose bottom cards message for a player.
     */
    fun getChooseBottomCardsMessage(playerId: EntityId): ServerMessage.ChooseBottomCards? {
        val count = getCardsToBottom(playerId)
        if (count == 0) return null
        val hand = getHand(playerId)
        return ServerMessage.ChooseBottomCards(
            hand = hand,
            cardsToPutOnBottom = count
        )
    }

    sealed interface MulliganActionResult {
        data object Success : MulliganActionResult
        data class NeedsBottomCards(val count: Int) : MulliganActionResult
        data class Failure(val reason: String) : MulliganActionResult
    }

    /**
     * Execute a game action.
     *
     * Routes the action through the engine's ActionProcessor.
     */
    fun executeAction(playerId: EntityId, action: GameAction): ActionResult {
        val state = gameState ?: return ActionResult.Failure("Game not started")

        val result = actionProcessor.process(state, action)

        val error = result.error
        val pendingDecision = result.pendingDecision
        return when {
            error != null -> ActionResult.Failure(error)
            pendingDecision != null -> {
                gameState = result.state
                ActionResult.PausedForDecision(result.state, pendingDecision, result.events)
            }
            else -> {
                gameState = result.state
                ActionResult.Success(result.state, result.events)
            }
        }
    }

    /**
     * Handle player concession.
     */
    fun playerConcedes(playerId: EntityId): GameState? {
        val state = gameState ?: return null
        val action = Concede(playerId)
        val result = actionProcessor.process(state, action)

        gameState = result.state
        return result.state
    }

    /**
     * Get the client game state for a specific player.
     */
    fun getClientState(playerId: EntityId): ClientGameState? {
        val state = gameState ?: return null
        return stateTransformer.transform(state, playerId)
    }

    /**
     * Get legal actions for a player.
     *
     * Currently returns basic actions - in the future this could use
     * a dedicated LegalActionCalculator from the engine.
     */
    fun getLegalActions(playerId: EntityId): List<LegalActionInfo> {
        val state = gameState ?: return emptyList()

        // Only the player with priority can take actions
        if (state.priorityPlayerId != playerId) {
            return emptyList()
        }

        val result = mutableListOf<LegalActionInfo>()

        // Pass priority is always available when you have priority
        result.add(LegalActionInfo(
            actionType = "PassPriority",
            description = "Pass priority",
            action = PassPriority(playerId)
        ))

        // Check for playable lands (during main phase, with land drop available)
        val landDrops = state.getEntity(playerId)?.get<LandDropsComponent>()
        val canPlayLand = state.step.isMainPhase &&
            state.stack.isEmpty() &&
            state.activePlayerId == playerId &&
            (landDrops?.canPlayLand ?: false)

        if (canPlayLand) {
            val hand = state.getHand(playerId)
            for (cardId in hand) {
                val cardComponent = state.getEntity(cardId)?.get<CardComponent>() ?: continue
                if (cardComponent.typeLine.isLand) {
                    result.add(LegalActionInfo(
                        actionType = "PlayLand",
                        description = "Play ${cardComponent.name}",
                        action = PlayLand(playerId, cardId)
                    ))
                }
            }
        }

        // Check for castable spells (non-instant only at sorcery speed)
        val canPlaySorcerySpeed = state.step.isMainPhase &&
            state.stack.isEmpty() &&
            state.activePlayerId == playerId

        val hand = state.getHand(playerId)
        for (cardId in hand) {
            val cardComponent = state.getEntity(cardId)?.get<CardComponent>() ?: continue
            if (!cardComponent.typeLine.isLand) {
                // Check timing - sorcery-speed spells need main phase, empty stack, your turn
                val isInstant = cardComponent.typeLine.isInstant
                if (isInstant || canPlaySorcerySpeed) {
                    // Check mana affordability
                    val canAfford = manaSolver.canPay(state, playerId, cardComponent.manaCost)
                    if (canAfford) {
                        // Look up card definition for target requirements
                        val cardDef = cardRegistry.getCard(cardComponent.name)
                        if (cardDef == null) {
                            logger.warn("Card definition not found in registry: '${cardComponent.name}'. Registry has ${cardRegistry.size} cards.")
                        }
                        val targetReqs = cardDef?.script?.targetRequirements ?: emptyList()

                        logger.debug("Card '${cardComponent.name}': cardDef=${cardDef != null}, targetReqs=${targetReqs.size}")

                        if (targetReqs.isNotEmpty()) {
                            // Spell requires targets - find valid targets
                            val firstReq = targetReqs.first()
                            val validTargets = findValidTargets(state, playerId, firstReq)

                            logger.debug("Card '${cardComponent.name}': firstReq=$firstReq, validTargets=${validTargets.size}")

                            // Only add the action if there are valid targets
                            if (validTargets.isNotEmpty() || firstReq.effectiveMinCount == 0) {
                                result.add(LegalActionInfo(
                                    actionType = "CastSpell",
                                    description = "Cast ${cardComponent.name}",
                                    action = CastSpell(playerId, cardId),
                                    validTargets = validTargets,
                                    requiresTargets = true,
                                    targetCount = firstReq.count,
                                    targetDescription = firstReq.description
                                ))
                            }
                        } else {
                            // No targets required
                            result.add(LegalActionInfo(
                                actionType = "CastSpell",
                                description = "Cast ${cardComponent.name}",
                                action = CastSpell(playerId, cardId)
                            ))
                        }
                    }
                }
            }
        }

        // Check for combat actions
        if (state.step == Step.DECLARE_ATTACKERS && state.activePlayerId == playerId) {
            // Check if attackers have already been declared this combat
            val attackersAlreadyDeclared = state.getEntity(playerId)
                ?.get<com.wingedsheep.engine.state.components.combat.AttackersDeclaredThisCombatComponent>() != null

            if (!attackersAlreadyDeclared) {
                // Active player can declare attackers during declare attackers step
                // Include empty attackers map - client will fill with selected attackers
                result.add(LegalActionInfo(
                    actionType = "DeclareAttackers",
                    description = "Declare attackers",
                    action = DeclareAttackers(playerId, emptyMap())
                ))
            }
        }

        if (state.step == Step.DECLARE_BLOCKERS && state.activePlayerId != playerId) {
            // Check if blockers have already been declared this combat
            val blockersAlreadyDeclared = state.getEntity(playerId)
                ?.get<com.wingedsheep.engine.state.components.combat.BlockersDeclaredThisCombatComponent>() != null

            if (!blockersAlreadyDeclared) {
                // Defending player (non-active player) can declare blockers during declare blockers step
                // Include empty blockers map - client will fill with selected blockers
                result.add(LegalActionInfo(
                    actionType = "DeclareBlockers",
                    description = "Declare blockers",
                    action = DeclareBlockers(playerId, emptyMap())
                ))
            }
        }

        return result
    }

    /**
     * Create a state update message for a player.
     */
    fun createStateUpdate(playerId: EntityId, events: List<GameEvent>): ServerMessage.StateUpdate? {
        val clientState = getClientState(playerId) ?: return null
        val legalActions = getLegalActions(playerId)
        return ServerMessage.StateUpdate(clientState, events, legalActions)
    }

    /**
     * Check if the game is over.
     */
    fun isGameOver(): Boolean = gameState?.gameOver == true

    /**
     * Get the winner ID if the game is over.
     */
    fun getWinnerId(): EntityId? = gameState?.winnerId

    /**
     * Determine the reason for game over.
     */
    fun getGameOverReason(): GameOverReason? {
        val state = gameState ?: return null
        if (!state.gameOver) return null

        // TODO: Determine actual reason from game state events
        return GameOverReason.LIFE_ZERO
    }

    /**
     * Find valid targets based on a target requirement.
     */
    private fun findValidTargets(
        state: GameState,
        playerId: EntityId,
        requirement: TargetRequirement
    ): List<EntityId> {
        return when (requirement) {
            is TargetCreature -> findValidCreatureTargets(state, playerId, requirement.filter)
            is TargetPlayer -> state.turnOrder.toList()
            is TargetOpponent -> state.turnOrder.filter { it != playerId }
            is AnyTarget -> {
                // Any target = creatures + players
                val creatures = findValidCreatureTargets(state, playerId, CreatureTargetFilter.Any)
                val players = state.turnOrder.toList()
                creatures + players
            }
            is TargetCreatureOrPlayer -> {
                val creatures = findValidCreatureTargets(state, playerId, CreatureTargetFilter.Any)
                val players = state.turnOrder.toList()
                creatures + players
            }
            is TargetPermanent -> findValidPermanentTargets(state, playerId, requirement.filter)
            else -> emptyList() // Other target types not yet implemented
        }
    }

    /**
     * Find valid creature targets based on a filter.
     */
    private fun findValidCreatureTargets(
        state: GameState,
        playerId: EntityId,
        filter: CreatureTargetFilter
    ): List<EntityId> {
        val battlefield = state.getBattlefield()
        return battlefield.filter { entityId ->
            val container = state.getEntity(entityId) ?: return@filter false
            val cardComponent = container.get<CardComponent>() ?: return@filter false

            // Must be a creature
            if (!cardComponent.typeLine.isCreature) return@filter false

            // Apply filter
            val controllerId = container.get<ControllerComponent>()?.playerId
            when (filter) {
                CreatureTargetFilter.Any -> true
                CreatureTargetFilter.YouControl -> controllerId == playerId
                CreatureTargetFilter.OpponentControls -> controllerId != playerId
                CreatureTargetFilter.Tapped -> container.has<TappedComponent>()
                CreatureTargetFilter.Untapped -> !container.has<TappedComponent>()
                else -> true // Other filters not yet implemented
            }
        }
    }

    /**
     * Find valid permanent targets based on a filter.
     */
    private fun findValidPermanentTargets(
        state: GameState,
        playerId: EntityId,
        filter: PermanentTargetFilter
    ): List<EntityId> {
        val battlefield = state.getBattlefield()
        return battlefield.filter { entityId ->
            val container = state.getEntity(entityId) ?: return@filter false
            val cardComponent = container.get<CardComponent>() ?: return@filter false
            val controllerId = container.get<ControllerComponent>()?.playerId

            when (filter) {
                PermanentTargetFilter.Any -> true
                PermanentTargetFilter.YouControl -> controllerId == playerId
                PermanentTargetFilter.OpponentControls -> controllerId != playerId
                PermanentTargetFilter.Creature -> cardComponent.typeLine.isCreature
                PermanentTargetFilter.Land -> cardComponent.typeLine.isLand
                PermanentTargetFilter.NonCreature -> !cardComponent.typeLine.isCreature
                PermanentTargetFilter.NonLand -> !cardComponent.typeLine.isLand
                else -> true // Other filters not yet implemented
            }
        }
    }

    sealed interface ActionResult {
        data class Success(
            val state: GameState,
            val events: List<GameEvent>
        ) : ActionResult

        data class Failure(val reason: String) : ActionResult

        data class PausedForDecision(
            val state: GameState,
            val decision: PendingDecision,
            val events: List<GameEvent>
        ) : ActionResult
    }
}
