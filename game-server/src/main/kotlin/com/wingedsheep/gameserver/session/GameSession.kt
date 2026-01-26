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
import com.wingedsheep.engine.state.components.battlefield.SummoningSicknessComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.handlers.ConditionEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.CastRestriction
import com.wingedsheep.gameserver.priority.AutoPassManager
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
    private val stateTransformer: ClientStateTransformer = ClientStateTransformer(cardRegistry)
) {
    // Lock for synchronizing state modifications to prevent lost updates
    private val stateLock = Any()

    @Volatile
    private var gameState: GameState? = null
    private val players = mutableMapOf<EntityId, PlayerSession>()
    private val deckLists = mutableMapOf<EntityId, List<String>>()

    private val actionProcessor = ActionProcessor(cardRegistry)
    private val gameInitializer = GameInitializer(cardRegistry)
    private val manaSolver = ManaSolver(cardRegistry)
    private val conditionEvaluator = ConditionEvaluator()
    private val turnManager = TurnManager()
    private val autoPassManager = AutoPassManager()

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
     * Synchronized to prevent lost updates when multiple players act simultaneously.
     */
    fun keepHand(playerId: EntityId): MulliganActionResult = synchronized(stateLock) {
        val state = gameState ?: return MulliganActionResult.Failure("Game not started")

        val action = KeepHand(playerId)
        val result = actionProcessor.process(state, action)

        val error = result.error
        if (error != null) {
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
     * Synchronized to prevent lost updates when multiple players act simultaneously.
     */
    fun takeMulligan(playerId: EntityId): MulliganActionResult = synchronized(stateLock) {
        val state = gameState ?: return MulliganActionResult.Failure("Game not started")

        val action = TakeMulligan(playerId)
        val result = actionProcessor.process(state, action)

        val error = result.error
        if (error != null) {
            MulliganActionResult.Failure(error)
        } else {
            gameState = result.state
            MulliganActionResult.Success
        }
    }

    /**
     * Player chooses which cards to put on the bottom of their library.
     * Routes through the engine's action processor.
     * Synchronized to prevent lost updates when multiple players act simultaneously.
     */
    fun chooseBottomCards(playerId: EntityId, cardIds: List<EntityId>): MulliganActionResult = synchronized(stateLock) {
        val state = gameState ?: return MulliganActionResult.Failure("Game not started")

        val action = BottomCards(playerId, cardIds)
        val result = actionProcessor.process(state, action)

        val error = result.error
        if (error != null) {
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
     * Synchronized to prevent lost updates when multiple players act simultaneously.
     */
    fun executeAction(playerId: EntityId, action: GameAction): ActionResult = synchronized(stateLock) {
        val state = gameState ?: return ActionResult.Failure("Game not started")

        val result = actionProcessor.process(state, action)

        val error = result.error
        val pendingDecision = result.pendingDecision
        when {
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
     * Synchronized to prevent lost updates.
     */
    fun playerConcedes(playerId: EntityId): GameState? = synchronized(stateLock) {
        val state = gameState ?: return null
        val action = Concede(playerId)
        val result = actionProcessor.process(state, action)

        gameState = result.state
        result.state
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
                // Look up card definition for target requirements and cast restrictions
                val cardDef = cardRegistry.getCard(cardComponent.name)
                if (cardDef == null) {
                    logger.warn("Card definition not found in registry: '${cardComponent.name}'. Registry has ${cardRegistry.size} cards.")
                }

                // Check cast restrictions first
                val castRestrictions = cardDef?.script?.castRestrictions ?: emptyList()
                if (!checkCastRestrictions(state, playerId, castRestrictions)) {
                    continue // Skip this card if cast restrictions are not met
                }

                // Check timing - sorcery-speed spells need main phase, empty stack, your turn
                val isInstant = cardComponent.typeLine.isInstant
                if (isInstant || canPlaySorcerySpeed) {
                    // Check mana affordability
                    val canAfford = manaSolver.canPay(state, playerId, cardComponent.manaCost)
                    if (canAfford) {
                        val targetReqs = cardDef?.script?.targetRequirements ?: emptyList()

                        logger.debug("Card '${cardComponent.name}': cardDef=${cardDef != null}, targetReqs=${targetReqs.size}")

                        // Calculate X cost info if the spell has X in its cost
                        val hasXCost = cardComponent.manaCost.hasX
                        val maxAffordableX: Int? = if (hasXCost) {
                            val availableSources = manaSolver.getAvailableManaCount(state, playerId)
                            val fixedCost = cardComponent.manaCost.cmc  // X contributes 0 to CMC
                            (availableSources - fixedCost).coerceAtLeast(0)
                        } else null

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
                                    targetDescription = firstReq.description,
                                    hasXCost = hasXCost,
                                    maxAffordableX = maxAffordableX
                                ))
                            }
                        } else {
                            // No targets required
                            result.add(LegalActionInfo(
                                actionType = "CastSpell",
                                description = "Cast ${cardComponent.name}",
                                action = CastSpell(playerId, cardId),
                                hasXCost = hasXCost,
                                maxAffordableX = maxAffordableX
                            ))
                        }
                    }
                }
            }
        }

        // Check for mana abilities on battlefield permanents
        val playerBattlefieldZone = ZoneKey(playerId, ZoneType.BATTLEFIELD)
        val battlefieldPermanents = state.getZone(playerBattlefieldZone)
        for (entityId in battlefieldPermanents) {
            val container = state.getEntity(entityId) ?: continue
            val cardComponent = container.get<CardComponent>() ?: continue

            // Must be controlled by the player
            val controllerId = container.get<ControllerComponent>()?.playerId
            if (controllerId != playerId) continue

            // Look up card definition for mana abilities
            val cardDef = cardRegistry.getCard(cardComponent.name) ?: continue
            val manaAbilities = cardDef.script.activatedAbilities.filter { it.isManaAbility }

            for (ability in manaAbilities) {
                // Check if the ability can be activated
                when (ability.cost) {
                    is AbilityCost.Tap -> {
                        // Must be untapped
                        if (container.has<TappedComponent>()) continue

                        // Check summoning sickness for creatures (non-lands)
                        if (!cardComponent.typeLine.isLand && cardComponent.typeLine.isCreature) {
                            val hasSummoningSickness = container.has<SummoningSicknessComponent>()
                            val hasHaste = cardComponent.baseKeywords.contains(Keyword.HASTE)
                            if (hasSummoningSickness && !hasHaste) continue
                        }
                    }
                    else -> {
                        // Other cost types - allow for now, engine will validate
                    }
                }

                result.add(LegalActionInfo(
                    actionType = "ActivateAbility",
                    description = ability.description,
                    action = ActivateAbility(playerId, entityId, ability.id),
                    isManaAbility = true
                ))
            }
        }

        // Check for combat actions
        if (state.step == Step.DECLARE_ATTACKERS && state.activePlayerId == playerId) {
            // Check if attackers have already been declared this combat
            val attackersAlreadyDeclared = state.getEntity(playerId)
                ?.get<com.wingedsheep.engine.state.components.combat.AttackersDeclaredThisCombatComponent>() != null

            if (!attackersAlreadyDeclared) {
                // Get valid attackers using the engine's TurnManager (handles haste, defender, etc.)
                val validAttackers = turnManager.getValidAttackers(state, playerId)

                // Active player can declare attackers during declare attackers step
                result.add(LegalActionInfo(
                    actionType = "DeclareAttackers",
                    description = "Declare attackers",
                    action = DeclareAttackers(playerId, emptyMap()),
                    validAttackers = validAttackers
                ))
            }
        }

        if (state.step == Step.DECLARE_BLOCKERS && state.activePlayerId != playerId) {
            // Check if blockers have already been declared this combat
            val blockersAlreadyDeclared = state.getEntity(playerId)
                ?.get<com.wingedsheep.engine.state.components.combat.BlockersDeclaredThisCombatComponent>() != null

            if (!blockersAlreadyDeclared) {
                // Get valid blockers using the engine's TurnManager
                val validBlockers = turnManager.getValidBlockers(state, playerId)

                // Defending player (non-active player) can declare blockers during declare blockers step
                result.add(LegalActionInfo(
                    actionType = "DeclareBlockers",
                    description = "Declare blockers",
                    action = DeclareBlockers(playerId, emptyMap()),
                    validBlockers = validBlockers
                ))
            }
        }

        return result
    }

    /**
     * Create a state update message for a player.
     */
    fun createStateUpdate(playerId: EntityId, events: List<GameEvent>): ServerMessage.StateUpdate? {
        val state = gameState ?: return null
        val clientState = getClientState(playerId) ?: return null
        val legalActions = getLegalActions(playerId)

        // Include pending decision only for the player who needs to make it
        val pendingDecision = state.pendingDecision?.takeIf { it.playerId == playerId }

        return ServerMessage.StateUpdate(clientState, events, legalActions, pendingDecision)
    }

    /**
     * Check if the player with priority should automatically pass.
     * Returns the player ID that should auto-pass, or null if no auto-pass should occur.
     *
     * This implements Arena-style smart priority passing.
     */
    fun getAutoPassPlayer(): EntityId? = synchronized(stateLock) {
        val state = gameState ?: return null

        // Can't auto-pass if game is over
        if (state.gameOver) return null

        // Get the player with priority
        val priorityPlayer = state.priorityPlayerId ?: return null

        // Get legal actions for that player
        val legalActions = getLegalActions(priorityPlayer)

        // Check if they should auto-pass
        return if (autoPassManager.shouldAutoPass(state, priorityPlayer, legalActions)) {
            priorityPlayer
        } else {
            null
        }
    }

    /**
     * Execute auto-pass for a player.
     * Returns the result of the PassPriority action.
     */
    fun executeAutoPass(playerId: EntityId): ActionResult = synchronized(stateLock) {
        val state = gameState ?: return ActionResult.Failure("Game not started")

        // Verify this player has priority
        if (state.priorityPlayerId != playerId) {
            return ActionResult.Failure("Player does not have priority")
        }

        // Execute the PassPriority action
        val action = PassPriority(playerId)
        val result = actionProcessor.process(state, action)

        val error = result.error
        val pendingDecision = result.pendingDecision
        when {
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

    /**
     * Check if cast restrictions are satisfied for a spell.
     * Returns true if all restrictions are met, false otherwise.
     */
    private fun checkCastRestrictions(
        state: GameState,
        playerId: EntityId,
        restrictions: List<CastRestriction>
    ): Boolean {
        if (restrictions.isEmpty()) return true

        // Create an EffectContext for condition evaluation
        val opponentId = state.turnOrder.firstOrNull { it != playerId }
        val context = EffectContext(
            sourceId = null,
            controllerId = playerId,
            opponentId = opponentId,
            targets = emptyList(),
            xValue = 0
        )

        for (restriction in restrictions) {
            val satisfied = when (restriction) {
                is CastRestriction.OnlyDuringStep -> state.step == restriction.step
                is CastRestriction.OnlyDuringPhase -> state.phase == restriction.phase
                is CastRestriction.OnlyIfCondition -> conditionEvaluator.evaluate(state, restriction.condition, context)
                is CastRestriction.TimingRequirement -> {
                    // TimingRequirement is handled separately in the main timing check
                    true
                }
                is CastRestriction.All -> restriction.restrictions.all { subRestriction ->
                    checkCastRestrictions(state, playerId, listOf(subRestriction))
                }
            }
            if (!satisfied) return false
        }
        return true
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

    // =========================================================================
    // Test Support (for scenario-based testing)
    // =========================================================================

    /**
     * Inject a pre-built game state for testing purposes.
     * This allows tests to set up specific game scenarios without playing through.
     *
     * **WARNING:** This method is for testing only. Do not use in production code.
     *
     * @param state The game state to inject
     * @param testPlayers Map of player IDs to PlayerSession instances
     */
    fun injectStateForTesting(state: GameState, testPlayers: Map<EntityId, PlayerSession>) {
        synchronized(stateLock) {
            gameState = state
            players.clear()
            players.putAll(testPlayers)
            testPlayers.forEach { (_, session) ->
                session.currentGameSessionId = sessionId
            }
        }
    }

    /**
     * Get the raw game state for testing assertions.
     * **WARNING:** This method is for testing only.
     */
    fun getStateForTesting(): GameState? = gameState
}
