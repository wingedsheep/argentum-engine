package com.wingedsheep.gameserver.session

import com.wingedsheep.gameserver.dto.ClientEvent
import com.wingedsheep.gameserver.dto.ClientEventTransformer
import com.wingedsheep.gameserver.dto.ClientGameState
import com.wingedsheep.gameserver.dto.ClientStateTransformer
import com.wingedsheep.gameserver.protocol.GameOverReason
import com.wingedsheep.gameserver.protocol.AdditionalCostInfo
import com.wingedsheep.gameserver.protocol.ConvokeCreatureInfo
import com.wingedsheep.gameserver.protocol.LegalActionInfo
import com.wingedsheep.gameserver.protocol.LegalActionTargetInfo
import com.wingedsheep.gameserver.protocol.ServerMessage
import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.mechanics.mana.ManaSolver
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.state.components.identity.MorphDataComponent
import com.wingedsheep.engine.state.components.player.LandDropsComponent
import com.wingedsheep.engine.state.components.player.LossReason
import com.wingedsheep.engine.state.components.player.MulliganStateComponent
import com.wingedsheep.engine.state.components.player.PlayerLostComponent
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.targeting.*
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.battlefield.SummoningSicknessComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.combat.AttackingComponent
import com.wingedsheep.engine.state.components.combat.BlockedComponent
import com.wingedsheep.engine.state.components.stack.TargetsComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.engine.state.components.identity.LifeTotalComponent
import com.wingedsheep.engine.handlers.ConditionEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.TargetFilter
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ControllerPredicate
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.CastRestriction
import com.wingedsheep.sdk.scripting.DividedDamageEffect
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
    private val stateTransformer: ClientStateTransformer = ClientStateTransformer(cardRegistry),
    private val useHandSmoother: Boolean = false
) {
    // Lock for synchronizing state modifications to prevent lost updates
    private val stateLock = Any()

    @Volatile
    private var gameState: GameState? = null
    private val players = mutableMapOf<EntityId, PlayerSession>()
    private val deckLists = mutableMapOf<EntityId, List<String>>()
    private val spectators = mutableSetOf<PlayerSession>()

    /** Player info for persistence (playerId -> (playerName, token)) */
    private val playerPersistenceInfo = mutableMapOf<EntityId, PlayerPersistenceInfo>()

    data class PlayerPersistenceInfo(val playerName: String, val token: String)

    private val actionProcessor = ActionProcessor(cardRegistry)
    private val gameInitializer = GameInitializer(cardRegistry)
    private val manaSolver = ManaSolver(cardRegistry)
    private val conditionEvaluator = ConditionEvaluator()
    private val predicateEvaluator = PredicateEvaluator()
    private val stateProjector = StateProjector()
    private val turnManager = TurnManager()
    private val autoPassManager = AutoPassManager()

    /** Tracks the last processed messageId per player for idempotency */
    private val lastProcessedMessageId = java.util.concurrent.ConcurrentHashMap<EntityId, String>()

    /** Accumulated game log per player (player-specific due to masking) */
    private val gameLogs = java.util.concurrent.ConcurrentHashMap<EntityId, MutableList<ClientEvent>>()

    /** Per-player full control setting (disables auto-pass when enabled) */
    private val fullControlEnabled = java.util.concurrent.ConcurrentHashMap<EntityId, Boolean>()
    private val stopOverrides = java.util.concurrent.ConcurrentHashMap<EntityId, StopOverrideSettings>()

    data class StopOverrideSettings(
        val myTurnStops: Set<Step> = emptySet(),
        val opponentTurnStops: Set<Step> = emptySet()
    )

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

    // =========================================================================
    // Spectator Management
    // =========================================================================

    /**
     * Add a spectator to this game session.
     */
    fun addSpectator(spectator: PlayerSession) {
        spectators.add(spectator)
    }

    /**
     * Remove a spectator from this game session.
     */
    fun removeSpectator(spectator: PlayerSession) {
        spectators.remove(spectator)
    }

    /**
     * Get all spectators.
     */
    fun getSpectators(): Set<PlayerSession> = spectators.toSet()

    /**
     * Get player names for spectator display.
     */
    fun getPlayerNames(): Pair<String, String>? {
        val p1 = player1 ?: return null
        val p2 = player2 ?: return null
        return Pair(p1.playerName, p2.playerName)
    }

    /**
     * Get current life totals for spectator display.
     */
    fun getLifeTotals(): Pair<Int, Int>? {
        val state = gameState ?: return null
        val p1Id = player1?.playerId ?: return null
        val p2Id = player2?.playerId ?: return null
        val p1Life = state.getEntity(p1Id)?.get<LifeTotalComponent>()?.life ?: 20
        val p2Life = state.getEntity(p2Id)?.get<LifeTotalComponent>()?.life ?: 20
        return Pair(p1Life, p2Life)
    }

    /**
     * Build spectator state for sending to spectators.
     */
    fun buildSpectatorState(): ServerMessage.SpectatorStateUpdate? {
        val state = gameState ?: return null
        val p1 = player1 ?: return null
        val p2 = player2 ?: return null

        // Build full ClientGameState with both hands masked for GameBoard reuse
        val spectatorClientState = buildSpectatorClientGameState(state, p1.playerId, p2.playerId)

        // Build decision status if there's a pending decision
        val decisionStatus = state.pendingDecision?.let { decision ->
            val decidingPlayer = if (decision.playerId == p1.playerId) p1 else p2
            createSpectatorDecisionStatus(decision, decidingPlayer.playerName)
        }

        return ServerMessage.SpectatorStateUpdate(
            gameSessionId = sessionId,
            gameState = spectatorClientState,
            player1Id = p1.playerId.value,
            player2Id = p2.playerId.value,
            player1Name = p1.playerName,
            player2Name = p2.playerName,
            // Legacy fields for backward compatibility
            player1 = buildSpectatorPlayerState(state, p1),
            player2 = buildSpectatorPlayerState(state, p2),
            currentPhase = state.phase.name,
            activePlayerId = state.activePlayerId?.value,
            priorityPlayerId = state.priorityPlayerId?.value,
            combat = buildSpectatorCombatState(state),
            decisionStatus = decisionStatus
        )
    }

    /**
     * Create a decision status for spectators.
     * Similar to createOpponentDecisionStatus but includes the player name.
     */
    private fun createSpectatorDecisionStatus(decision: PendingDecision, playerName: String): ServerMessage.SpectatorDecisionStatus {
        val displayText = when (decision) {
            is SelectCardsDecision -> "Selecting cards"
            is ChooseTargetsDecision -> "Choosing targets"
            is YesNoDecision -> "Making a choice"
            is ChooseModeDecision -> "Choosing mode"
            is ChooseColorDecision -> "Choosing a color"
            is ChooseNumberDecision -> "Choosing a number"
            is DistributeDecision -> "Distributing"
            is OrderObjectsDecision -> "Ordering blockers"
            is SplitPilesDecision -> "Splitting piles"
            is SearchLibraryDecision -> "Searching library"
            is ReorderLibraryDecision -> "Reordering cards"
            is AssignDamageDecision -> "Assigning damage"
            is ChooseOptionDecision -> "Making a choice"
        }
        return ServerMessage.SpectatorDecisionStatus(
            playerName = playerName,
            playerId = decision.playerId.value,
            decisionType = decision::class.simpleName ?: "Unknown",
            displayText = displayText,
            sourceName = decision.context.sourceName
        )
    }

    /**
     * Build a ClientGameState for spectators with both players' hands masked.
     * Spectators see:
     * - Both battlefields with full card info
     * - Both graveyards with full card info
     * - The stack with full card info
     * - Both players' life totals and zone sizes
     * - No hand contents (only hand sizes)
     */
    private fun buildSpectatorClientGameState(
        state: GameState,
        player1Id: EntityId,
        player2Id: EntityId
    ): ClientGameState {
        // Use player1's perspective as the "viewing player" for the transform,
        // then mask player1's hand as well
        val baseState = stateTransformer.transform(state, player1Id)

        // Filter out hand cards from the cards map (spectators can't see either player's hand)
        val player1Hand = state.getHand(player1Id).toSet()
        val player2Hand = state.getHand(player2Id).toSet()
        val allHandCards = player1Hand + player2Hand
        val visibleCards = baseState.cards.filterKeys { it !in allHandCards }

        // Update zones to hide hand contents but keep sizes
        val maskedZones = baseState.zones.map { zone ->
            if (zone.zoneId.zoneType == Zone.HAND) {
                // Keep size but hide card IDs for both hands
                zone.copy(
                    cardIds = emptyList(),
                    isVisible = false
                )
            } else {
                zone
            }
        }

        return baseState.copy(
            cards = visibleCards,
            zones = maskedZones
        )
    }

    private fun buildSpectatorCombatState(state: GameState): ServerMessage.SpectatorCombatState? {
        // Only show combat state during combat phase
        if (state.step.phase != Phase.COMBAT) {
            return null
        }

        val attackers = mutableListOf<ServerMessage.SpectatorAttacker>()
        var attackingPlayerId: EntityId? = null
        var defendingPlayerId: EntityId? = null

        // Find all attackers on the battlefield
        for (entityId in state.getBattlefield()) {
            val container = state.getEntity(entityId) ?: continue
            val attackingComponent = container.get<AttackingComponent>() ?: continue

            // Track the attacking and defending players
            val controllerId = container.get<ControllerComponent>()?.playerId
            if (controllerId != null) {
                attackingPlayerId = controllerId
                defendingPlayerId = attackingComponent.defenderId
            }

            val blockedComponent = container.get<BlockedComponent>()
            attackers.add(
                ServerMessage.SpectatorAttacker(
                    creatureId = entityId.value,
                    blockedBy = blockedComponent?.blockerIds?.map { it.value } ?: emptyList()
                )
            )
        }

        if (attackers.isEmpty() || attackingPlayerId == null || defendingPlayerId == null) {
            return null
        }

        return ServerMessage.SpectatorCombatState(
            attackingPlayerId = attackingPlayerId.value,
            defendingPlayerId = defendingPlayerId.value,
            attackers = attackers
        )
    }

    private fun buildSpectatorPlayerState(state: GameState, playerSession: PlayerSession): ServerMessage.SpectatorPlayerState {
        val playerId = playerSession.playerId
        val playerEntity = state.getEntity(playerId)

        val life = playerEntity?.get<LifeTotalComponent>()?.life ?: 20
        val hand = state.getZone(playerId, Zone.HAND)
        val library = state.getZone(playerId, Zone.LIBRARY)
        val battlefield = state.getZone(playerId, Zone.BATTLEFIELD)
        val graveyard = state.getZone(playerId, Zone.GRAVEYARD)

        // Stack is shared between players - get all stack items
        val stack = state.stack

        return ServerMessage.SpectatorPlayerState(
            playerId = playerId.value,
            playerName = playerSession.playerName,
            life = life,
            handSize = hand.size,
            librarySize = library.size,
            battlefield = battlefield.mapNotNull { cardId -> buildSpectatorCardInfo(state, cardId) },
            graveyard = graveyard.mapNotNull { cardId -> buildSpectatorCardInfo(state, cardId) },
            stack = stack.mapNotNull { cardId -> buildSpectatorCardInfo(state, cardId) }
        )
    }

    private fun buildSpectatorCardInfo(state: GameState, cardId: EntityId): ServerMessage.SpectatorCardInfo? {
        val card = state.getEntity(cardId) ?: return null
        val cardComponent = card.get<CardComponent>() ?: return null
        val cardDef = cardRegistry.getCard(cardComponent.name)

        val tapped = card.has<TappedComponent>()
        val damage = card.get<DamageComponent>()?.amount ?: 0
        val cardTypes = cardComponent.typeLine.cardTypes.map { it.name }
        val isAttacking = card.has<AttackingComponent>()

        // Get targets for spells/abilities on the stack
        val targetsComponent = card.get<TargetsComponent>()
        val targets = targetsComponent?.targets?.mapNotNull { chosenTarget ->
            when (chosenTarget) {
                is ChosenTarget.Player -> ServerMessage.SpectatorTarget.Player(chosenTarget.playerId.value)
                is ChosenTarget.Permanent -> ServerMessage.SpectatorTarget.Permanent(chosenTarget.entityId.value)
                is ChosenTarget.Spell -> ServerMessage.SpectatorTarget.Spell(chosenTarget.spellEntityId.value)
                is ChosenTarget.Card -> null // Cards in zones not displayed as targets
            }
        } ?: emptyList()

        return ServerMessage.SpectatorCardInfo(
            entityId = cardId.value,
            name = cardComponent.name,
            imageUri = cardDef?.metadata?.imageUri,
            isTapped = tapped,
            power = cardComponent.baseStats?.basePower,
            toughness = cardComponent.baseStats?.baseToughness,
            damage = damage,
            cardTypes = cardTypes,
            isAttacking = isAttacking,
            targets = targets
        )
    }

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
            players = playerConfigs,
            useHandSmoother = useHandSmoother
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
                val imageUri = cardComponent?.cardDefinitionId?.let { defId ->
                    cardRegistry.getCard(defId)?.metadata?.imageUri
                }
                ServerMessage.MulliganCardInfo(
                    name = cardComponent?.name ?: "Unknown",
                    imageUri = imageUri
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
    fun executeAction(playerId: EntityId, action: GameAction, messageId: String? = null): ActionResult = synchronized(stateLock) {
        val state = gameState ?: return ActionResult.Failure("Game not started")

        // Idempotency check: if this messageId was already processed, skip
        if (messageId != null) {
            val lastId = lastProcessedMessageId[playerId]
            if (lastId == messageId) {
                return ActionResult.Failure("Duplicate message")
            }
        }

        val result = actionProcessor.process(state, action)

        val error = result.error
        val pendingDecision = result.pendingDecision
        when {
            error != null -> ActionResult.Failure(error)
            pendingDecision != null -> {
                gameState = result.state
                if (messageId != null) lastProcessedMessageId[playerId] = messageId
                ActionResult.PausedForDecision(result.state, pendingDecision, result.events)
            }
            else -> {
                gameState = result.state
                if (messageId != null) lastProcessedMessageId[playerId] = messageId
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

        // Check for morph cards that can be cast face-down (sorcery speed only)
        if (canPlaySorcerySpeed) {
            val morphCost = ManaCost.parse("{3}")
            val canAffordMorph = manaSolver.canPay(state, playerId, morphCost)
            for (cardId in hand) {
                val cardComponent = state.getEntity(cardId)?.get<CardComponent>() ?: continue
                val cardDef = cardRegistry.getCard(cardComponent.name) ?: continue

                // Check if card has Morph keyword
                val hasMorph = cardDef.keywordAbilities
                    .any { it is com.wingedsheep.sdk.scripting.KeywordAbility.Morph }

                if (hasMorph) {
                    // Add morph action (affordable or not) - client shows greyed out if unaffordable
                    result.add(LegalActionInfo(
                        actionType = "CastFaceDown",
                        description = "Cast ${cardComponent.name} face-down",
                        action = CastSpell(playerId, cardId, castFaceDown = true),
                        isAffordable = canAffordMorph,
                        manaCostString = "{3}"
                    ))

                    // Check if we can afford to cast normally - if not, add unaffordable cast action
                    // This ensures the player sees both options in the cast modal
                    val canAffordNormal = manaSolver.canPay(state, playerId, cardComponent.manaCost)
                    if (!canAffordNormal) {
                        result.add(LegalActionInfo(
                            actionType = "CastSpell",
                            description = "Cast ${cardComponent.name}",
                            action = CastSpell(playerId, cardId),
                            isAffordable = false,
                            manaCostString = cardComponent.manaCost.toString()
                        ))
                    }
                }
            }
        }

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
                    // Check additional cost payability
                    val additionalCosts = cardDef?.script?.additionalCosts ?: emptyList()
                    val sacrificeTargets = mutableListOf<EntityId>()
                    var canPayAdditionalCosts = true
                    for (cost in additionalCosts) {
                        when (cost) {
                            is com.wingedsheep.sdk.scripting.AdditionalCost.SacrificePermanent -> {
                                val validSacTargets = findSacrificeTargets(state, playerId, cost)
                                if (validSacTargets.size < cost.count) {
                                    canPayAdditionalCosts = false
                                }
                                sacrificeTargets.addAll(validSacTargets)
                            }
                            else -> {}
                        }
                    }
                    if (!canPayAdditionalCosts) continue

                    // Check mana affordability (including Convoke if available)
                    val hasConvoke = cardDef?.keywords?.contains(Keyword.CONVOKE) == true
                    val convokeCreatures = if (hasConvoke) {
                        findConvokeCreatures(state, playerId)
                    } else null

                    // For Convoke spells, check if affordable with creature help
                    val canAfford = if (hasConvoke && convokeCreatures != null && convokeCreatures.isNotEmpty()) {
                        // Can afford if: mana alone is enough, OR mana + convoke creatures cover the cost
                        manaSolver.canPay(state, playerId, cardComponent.manaCost) ||
                            canAffordWithConvoke(state, playerId, cardComponent.manaCost, convokeCreatures)
                    } else {
                        manaSolver.canPay(state, playerId, cardComponent.manaCost)
                    }

                    if (canAfford) {
                        val targetReqs = buildList {
                            addAll(cardDef?.script?.targetRequirements ?: emptyList())
                            cardDef?.script?.auraTarget?.let { add(it) }
                        }

                        logger.debug("Card '${cardComponent.name}': cardDef=${cardDef != null}, targetReqs=${targetReqs.size}")

                        // Build additional cost info for the client
                        val costInfo = if (sacrificeTargets.isNotEmpty()) {
                            val sacCost = additionalCosts.filterIsInstance<com.wingedsheep.sdk.scripting.AdditionalCost.SacrificePermanent>().firstOrNull()
                            AdditionalCostInfo(
                                description = sacCost?.description ?: "Sacrifice a creature",
                                costType = "SacrificePermanent",
                                validSacrificeTargets = sacrificeTargets,
                                sacrificeCount = sacCost?.count ?: 1
                            )
                        } else null

                        // Calculate X cost info if the spell has X in its cost
                        val hasXCost = cardComponent.manaCost.hasX
                        val maxAffordableX: Int? = if (hasXCost) {
                            val availableSources = manaSolver.getAvailableManaCount(state, playerId)
                            val fixedCost = cardComponent.manaCost.cmc  // X contributes 0 to CMC
                            (availableSources - fixedCost).coerceAtLeast(0)
                        } else null

                        // Always include mana cost string for cast actions
                        val manaCostString = cardComponent.manaCost.toString()

                        // Compute auto-tap preview for UI highlighting
                        val autoTapSolution = manaSolver.solve(state, playerId, cardComponent.manaCost)
                        val autoTapPreview = autoTapSolution?.sources?.map { it.entityId }

                        // Check for DividedDamageEffect to flag damage distribution requirement
                        val spellEffect = cardDef?.script?.spellEffect
                        val dividedDamageEffect = spellEffect as? DividedDamageEffect
                        val requiresDamageDistribution = dividedDamageEffect != null
                        val totalDamageToDistribute = dividedDamageEffect?.totalDamage
                        val minDamagePerTarget = if (dividedDamageEffect != null) 1 else null

                        if (targetReqs.isNotEmpty()) {
                            // Spell requires targets - find valid targets for all requirements
                            val targetReqInfos = targetReqs.mapIndexed { index, req ->
                                val validTargets = findValidTargets(state, playerId, req)
                                LegalActionTargetInfo(
                                    index = index,
                                    description = req.description,
                                    minTargets = req.effectiveMinCount,
                                    maxTargets = req.count,
                                    validTargets = validTargets,
                                    targetZone = getTargetZone(req)
                                )
                            }

                            // Check if all requirements can be satisfied
                            val allRequirementsSatisfied = targetReqInfos.all { reqInfo ->
                                reqInfo.validTargets.isNotEmpty() || reqInfo.minTargets == 0
                            }

                            val firstReq = targetReqs.first()
                            val firstReqInfo = targetReqInfos.first()

                            logger.debug("Card '${cardComponent.name}': targetReqs=${targetReqs.size}, firstReqValidTargets=${firstReqInfo.validTargets.size}")

                            // Only add the action if all requirements can be satisfied
                            if (allRequirementsSatisfied) {
                                // Check if we can auto-select player targets (single target, single valid choice)
                                // This applies to TargetPlayer and TargetOpponent - in a 2-player game with TargetOpponent,
                                // there's always exactly one choice so we skip the prompt for better UX.
                                val canAutoSelect = targetReqs.size == 1 &&
                                    shouldAutoSelectPlayerTarget(firstReq, firstReqInfo.validTargets)

                                if (canAutoSelect) {
                                    // Auto-select the single valid player target
                                    val autoSelectedTarget = ChosenTarget.Player(firstReqInfo.validTargets.first())
                                    result.add(LegalActionInfo(
                                        actionType = "CastSpell",
                                        description = "Cast ${cardComponent.name}",
                                        action = CastSpell(playerId, cardId, targets = listOf(autoSelectedTarget)),
                                        hasXCost = hasXCost,
                                        maxAffordableX = maxAffordableX,
                                        additionalCostInfo = costInfo,
                                        hasConvoke = hasConvoke,
                                        validConvokeCreatures = convokeCreatures,
                                        manaCostString = manaCostString,
                                        requiresDamageDistribution = requiresDamageDistribution,
                                        totalDamageToDistribute = totalDamageToDistribute,
                                        minDamagePerTarget = minDamagePerTarget,
                                        autoTapPreview = autoTapPreview
                                    ))
                                } else {
                                    result.add(LegalActionInfo(
                                        actionType = "CastSpell",
                                        description = "Cast ${cardComponent.name}",
                                        action = CastSpell(playerId, cardId),
                                        validTargets = firstReqInfo.validTargets,
                                        requiresTargets = true,
                                        targetCount = firstReq.count,
                                        minTargets = firstReq.effectiveMinCount,
                                        targetDescription = firstReq.description,
                                        targetRequirements = if (targetReqInfos.size > 1) targetReqInfos else null,
                                        hasXCost = hasXCost,
                                        maxAffordableX = maxAffordableX,
                                        additionalCostInfo = costInfo,
                                        hasConvoke = hasConvoke,
                                        validConvokeCreatures = convokeCreatures,
                                        manaCostString = manaCostString,
                                        requiresDamageDistribution = requiresDamageDistribution,
                                        totalDamageToDistribute = totalDamageToDistribute,
                                        minDamagePerTarget = minDamagePerTarget,
                                        autoTapPreview = autoTapPreview
                                    ))
                                }
                            }
                        } else {
                            // No targets required
                            result.add(LegalActionInfo(
                                actionType = "CastSpell",
                                description = "Cast ${cardComponent.name}",
                                action = CastSpell(playerId, cardId),
                                hasXCost = hasXCost,
                                maxAffordableX = maxAffordableX,
                                additionalCostInfo = costInfo,
                                hasConvoke = hasConvoke,
                                validConvokeCreatures = convokeCreatures,
                                manaCostString = manaCostString,
                                autoTapPreview = autoTapPreview
                            ))
                        }
                    }
                }
            }
        }

        // Check for cycling abilities on cards in hand
        for (cardId in hand) {
            val cardComponent = state.getEntity(cardId)?.get<CardComponent>() ?: continue
            val cardDef = cardRegistry.getCard(cardComponent.name)
            if (cardDef == null) {
                continue
            }

            // Check for cycling ability - log at info level to ensure visibility
            val allAbilities = cardDef.keywordAbilities
            val cyclingAbility = allAbilities
                .filterIsInstance<com.wingedsheep.sdk.scripting.KeywordAbility.Cycling>()
                .firstOrNull()
            if (cyclingAbility == null) {
                continue
            }

            // Add cycling action (affordable or not) - client shows greyed out if unaffordable
            val canAffordCycling = manaSolver.canPay(state, playerId, cyclingAbility.cost)
            result.add(LegalActionInfo(
                actionType = "CycleCard",
                description = "Cycle ${cardComponent.name}",
                action = CycleCard(playerId, cardId),
                isAffordable = canAffordCycling,
                manaCostString = cyclingAbility.cost.toString()
            ))

            // For cards with cycling, also add the normal cast option (matching morph pattern)
            // This ensures the player sees both options in the cast modal
            if (!cardComponent.typeLine.isLand) {
                val isInstant = cardComponent.typeLine.isInstant
                val canCastTiming = isInstant || canPlaySorcerySpeed
                if (canCastTiming) {
                    // Check if we can afford to cast normally
                    val canAffordNormal = manaSolver.canPay(state, playerId, cardComponent.manaCost)
                    // Check if a cast action was already added (affordable, with proper targeting)
                    val hasCastAction = result.any { it.action is CastSpell && (it.action as CastSpell).cardId == cardId }
                    if (!hasCastAction) {
                        // If the spell requires targets, check if valid targets exist.
                        // Without this, a spell with cycling+targeting would be castable
                        // without target selection when no valid targets are found.
                        val targetReqs = cardDef.script.targetRequirements
                        val hasRequiredTargets = targetReqs.any { it.effectiveMinCount > 0 }
                        val canSatisfyTargets = if (hasRequiredTargets) {
                            targetReqs.all { req ->
                                val validTargets = findValidTargets(state, playerId, req)
                                validTargets.isNotEmpty() || req.effectiveMinCount == 0
                            }
                        } else {
                            true
                        }

                        if (canAffordNormal && canSatisfyTargets && targetReqs.isNotEmpty()) {
                            // Spell is affordable and has valid targets — add with full targeting info
                            val targetReqInfos = targetReqs.mapIndexed { index, req ->
                                val validTargets = findValidTargets(state, playerId, req)
                                LegalActionTargetInfo(
                                    index = index,
                                    description = req.description,
                                    minTargets = req.effectiveMinCount,
                                    maxTargets = req.count,
                                    validTargets = validTargets,
                                    targetZone = getTargetZone(req)
                                )
                            }
                            val firstReq = targetReqs.first()
                            val firstReqInfo = targetReqInfos.first()
                            result.add(LegalActionInfo(
                                actionType = "CastSpell",
                                description = "Cast ${cardComponent.name}",
                                action = CastSpell(playerId, cardId),
                                validTargets = firstReqInfo.validTargets,
                                requiresTargets = true,
                                targetCount = firstReq.count,
                                minTargets = firstReq.effectiveMinCount,
                                targetDescription = firstReq.description,
                                targetRequirements = if (targetReqInfos.size > 1) targetReqInfos else null,
                                isAffordable = true,
                                manaCostString = cardComponent.manaCost.toString()
                            ))
                        } else {
                            // Spell is unaffordable or has no valid targets — show greyed out
                            result.add(LegalActionInfo(
                                actionType = "CastSpell",
                                description = "Cast ${cardComponent.name}",
                                action = CastSpell(playerId, cardId),
                                isAffordable = false,
                                manaCostString = cardComponent.manaCost.toString()
                            ))
                        }
                    }
                }
            }
        }

        // Check for mana abilities on battlefield permanents
        // Use projected state to find all permanents controlled by this player
        // (accounts for control-changing effects like Annex)
        val projectedState = stateProjector.project(state)
        val battlefieldPermanents = projectedState.getBattlefieldControlledBy(playerId)
        for (entityId in battlefieldPermanents) {
            val container = state.getEntity(entityId) ?: continue
            val cardComponent = container.get<CardComponent>() ?: continue

            // Face-down creatures have no abilities (Rule 707.2)
            if (container.has<FaceDownComponent>()) continue

            // Projected controller already verified - look up card definition for mana abilities
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

        // Check for face-down creatures that can be turned face-up (morph)
        // This is a special action that doesn't use the stack (can be done at any time with priority)
        for (entityId in battlefieldPermanents) {
            val container = state.getEntity(entityId) ?: continue

            // Must be face-down
            if (!container.has<FaceDownComponent>()) continue

            // Must have morph data (to get the morph cost)
            val morphData = container.get<MorphDataComponent>() ?: continue
            val cardComponent = container.get<CardComponent>() ?: continue

            // Check if player can afford the morph cost
            if (manaSolver.canPay(state, playerId, morphData.morphCost)) {
                result.add(LegalActionInfo(
                    actionType = "ActivateAbility",
                    description = "Turn face-up (${morphData.morphCost})",
                    action = TurnFaceUp(playerId, entityId),
                    manaCostString = morphData.morphCost.toString()
                ))
            }
        }

        // Check for non-mana activated abilities on battlefield permanents
        for (entityId in battlefieldPermanents) {
            val container = state.getEntity(entityId) ?: continue
            val cardComponent = container.get<CardComponent>() ?: continue

            // Face-down creatures have no abilities (Rule 707.2)
            if (container.has<FaceDownComponent>()) continue

            val cardDef = cardRegistry.getCard(cardComponent.name) ?: continue
            val nonManaAbilities = cardDef.script.activatedAbilities.filter { !it.isManaAbility }

            for (ability in nonManaAbilities) {
                // Check cost requirements and gather sacrifice/tap targets if needed
                var sacrificeTargets: List<EntityId>? = null
                var sacrificeCost: AbilityCost.Sacrifice? = null
                var tapTargets: List<EntityId>? = null
                var tapCost: AbilityCost.TapPermanents? = null

                when (ability.cost) {
                    is AbilityCost.Tap -> {
                        if (container.has<TappedComponent>()) continue
                        if (!cardComponent.typeLine.isLand && cardComponent.typeLine.isCreature) {
                            val hasSummoningSickness = container.has<SummoningSicknessComponent>()
                            val hasHaste = cardComponent.baseKeywords.contains(Keyword.HASTE)
                            if (hasSummoningSickness && !hasHaste) continue
                        }
                    }
                    is AbilityCost.Mana -> {
                        if (!manaSolver.canPay(state, playerId, (ability.cost as AbilityCost.Mana).cost)) continue
                    }
                    is AbilityCost.Sacrifice -> {
                        sacrificeCost = ability.cost as AbilityCost.Sacrifice
                        sacrificeTargets = findAbilitySacrificeTargets(state, playerId, sacrificeCost.filter)
                        if (sacrificeTargets.isEmpty()) continue
                    }
                    is AbilityCost.TapPermanents -> {
                        tapCost = ability.cost as AbilityCost.TapPermanents
                        tapTargets = findAbilityTapTargets(state, playerId, tapCost.filter)
                        if (tapTargets.size < tapCost.count) continue
                    }
                    is AbilityCost.SacrificeSelf -> {
                        // Source must be on battlefield (always true when iterating battlefield)
                        sacrificeTargets = listOf(entityId)
                    }
                    is AbilityCost.Composite -> {
                        val compositeCost = ability.cost as AbilityCost.Composite
                        var costCanBePaid = true
                        for (subCost in compositeCost.costs) {
                            when (subCost) {
                                is AbilityCost.Tap -> {
                                    if (container.has<TappedComponent>()) {
                                        costCanBePaid = false
                                        break
                                    }
                                    if (!cardComponent.typeLine.isLand && cardComponent.typeLine.isCreature) {
                                        val hasSummoningSickness = container.has<SummoningSicknessComponent>()
                                        val hasHaste = cardComponent.baseKeywords.contains(Keyword.HASTE)
                                        if (hasSummoningSickness && !hasHaste) {
                                            costCanBePaid = false
                                            break
                                        }
                                    }
                                }
                                is AbilityCost.Mana -> {
                                    if (!manaSolver.canPay(state, playerId, subCost.cost)) {
                                        costCanBePaid = false
                                        break
                                    }
                                }
                                is AbilityCost.Sacrifice -> {
                                    sacrificeCost = subCost
                                    sacrificeTargets = findAbilitySacrificeTargets(state, playerId, subCost.filter)
                                    if (sacrificeTargets.isEmpty()) {
                                        costCanBePaid = false
                                        break
                                    }
                                }
                                is AbilityCost.SacrificeSelf -> {
                                    // Source must be on battlefield (always true when iterating battlefield)
                                    sacrificeTargets = listOf(entityId)
                                }
                                is AbilityCost.TapPermanents -> {
                                    tapCost = subCost
                                    tapTargets = findAbilityTapTargets(state, playerId, subCost.filter)
                                    if (tapTargets.size < subCost.count) {
                                        costCanBePaid = false
                                        break
                                    }
                                }
                                else -> {}
                            }
                        }
                        if (!costCanBePaid) continue
                    }
                    else -> {}
                }

                // Check activation restrictions
                var restrictionsMet = true
                for (restriction in ability.restrictions) {
                    if (!checkActivationRestriction(state, playerId, restriction)) {
                        restrictionsMet = false
                        break
                    }
                }
                if (!restrictionsMet) continue

                // Build additional cost info for sacrifice or tap costs
                val costInfo = if (tapTargets != null && tapCost != null) {
                    AdditionalCostInfo(
                        description = tapCost.description,
                        costType = "TapPermanents",
                        validTapTargets = tapTargets,
                        tapCount = tapCost.count
                    )
                } else if (sacrificeTargets != null && sacrificeCost != null) {
                    AdditionalCostInfo(
                        description = sacrificeCost.description,
                        costType = "SacrificePermanent",
                        validSacrificeTargets = sacrificeTargets,
                        sacrificeCount = 1
                    )
                } else if (sacrificeTargets != null) {
                    // SacrificeSelf cost — sacrifice target is the source itself
                    AdditionalCostInfo(
                        description = "Sacrifice this permanent",
                        costType = "SacrificePermanent",
                        validSacrificeTargets = sacrificeTargets,
                        sacrificeCount = 1
                    )
                } else null

                // Check for target requirements
                val targetReq = ability.targetRequirement
                if (targetReq != null) {
                    val validTargets = findValidTargets(state, playerId, targetReq)
                    if (validTargets.isEmpty()) continue

                    // Check if we can auto-select player targets (single target, single valid choice)
                    if (shouldAutoSelectPlayerTarget(targetReq, validTargets)) {
                        val autoSelectedTarget = ChosenTarget.Player(validTargets.first())
                        result.add(LegalActionInfo(
                            actionType = "ActivateAbility",
                            description = ability.description,
                            action = ActivateAbility(playerId, entityId, ability.id, targets = listOf(autoSelectedTarget)),
                            additionalCostInfo = costInfo
                        ))
                    } else {
                        result.add(LegalActionInfo(
                            actionType = "ActivateAbility",
                            description = ability.description,
                            action = ActivateAbility(playerId, entityId, ability.id),
                            validTargets = validTargets,
                            requiresTargets = true,
                            targetCount = targetReq.count,
                            minTargets = targetReq.effectiveMinCount,
                            targetDescription = targetReq.description,
                            additionalCostInfo = costInfo
                        ))
                    }
                } else {
                    result.add(LegalActionInfo(
                        actionType = "ActivateAbility",
                        description = ability.description,
                        action = ActivateAbility(playerId, entityId, ability.id),
                        additionalCostInfo = costInfo
                    ))
                }
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

        // Transform raw engine events to client events
        val clientEvents = ClientEventTransformer.transform(events, playerId)

        // Accumulate into persistent game log (filter noisy events)
        val logEntries = clientEvents.filter { it !is ClientEvent.PermanentTapped && it !is ClientEvent.PermanentUntapped && it !is ClientEvent.ManaAdded }
        val playerLog = gameLogs.getOrPut(playerId) { mutableListOf() }
        playerLog.addAll(logEntries)

        // Include pending decision only for the player who needs to make it
        // Enrich with imageUri from card registry since engine doesn't have access to metadata
        val pendingDecision = state.pendingDecision?.takeIf { it.playerId == playerId }?.let {
            enrichDecision(it, state)
        }

        // Calculate next stop point for the Pass button (only if player has priority)
        val playerOverrides = getStopOverrides(playerId)
        val nextStopPoint = if (state.priorityPlayerId == playerId && !isFullControlEnabled(playerId)) {
            val hasMeaningfulActions = legalActions.any { action ->
                action.actionType != "PassPriority" && !action.isManaAbility
            }
            autoPassManager.getNextStopPoint(state, playerId, hasMeaningfulActions, stateProjector, playerOverrides.myTurnStops, playerOverrides.opponentTurnStops)
        } else {
            null
        }

        // Include opponent decision status for the OTHER player (so they know opponent is making a choice)
        val opponentDecisionStatus = state.pendingDecision?.takeIf { it.playerId != playerId }?.let {
            createOpponentDecisionStatus(it)
        }

        val stateWithLog = clientState.copy(gameLog = playerLog.toList())
        val stopOverrideInfo = if (playerOverrides.myTurnStops.isNotEmpty() || playerOverrides.opponentTurnStops.isNotEmpty()) {
            ServerMessage.StopOverrideInfo(playerOverrides.myTurnStops, playerOverrides.opponentTurnStops)
        } else {
            null
        }
        return ServerMessage.StateUpdate(stateWithLog, clientEvents, legalActions, pendingDecision, nextStopPoint, opponentDecisionStatus, stopOverrideInfo)
    }

    /**
     * Create a masked summary of a pending decision for the opponent.
     * This shows that a decision is being made without revealing private information.
     */
    private fun createOpponentDecisionStatus(decision: PendingDecision): ServerMessage.OpponentDecisionStatus {
        val displayText = when (decision) {
            is SelectCardsDecision -> "Selecting cards"
            is ChooseTargetsDecision -> "Choosing targets"
            is YesNoDecision -> "Making a choice"
            is ChooseModeDecision -> "Choosing mode"
            is ChooseColorDecision -> "Choosing a color"
            is ChooseNumberDecision -> "Choosing a number"
            is DistributeDecision -> "Distributing"
            is OrderObjectsDecision -> "Ordering blockers"
            is SplitPilesDecision -> "Splitting piles"
            is SearchLibraryDecision -> "Searching library"
            is ReorderLibraryDecision -> "Reordering cards"
            is AssignDamageDecision -> "Assigning damage"
            is ChooseOptionDecision -> "Making a choice"
        }
        return ServerMessage.OpponentDecisionStatus(
            decisionType = decision::class.simpleName ?: "Unknown",
            displayText = displayText,
            sourceName = decision.context.sourceName
        )
    }

    // =========================================================================
    // Full Control Settings
    // =========================================================================

    /**
     * Set full control mode for a player.
     * When enabled, auto-pass is disabled and the player receives priority at every possible point.
     */
    fun setFullControl(playerId: EntityId, enabled: Boolean) {
        fullControlEnabled[playerId] = enabled
        logger.info("Player $playerId set full control to $enabled")
    }

    /**
     * Check if a player has full control mode enabled.
     */
    fun isFullControlEnabled(playerId: EntityId): Boolean {
        return fullControlEnabled[playerId] ?: false
    }

    // =========================================================================
    // Stop Override Settings
    // =========================================================================

    /**
     * Set per-step stop overrides for a player.
     * When a stop is set for a step, auto-pass will not skip that step.
     */
    fun setStopOverrides(playerId: EntityId, myTurnStops: Set<Step>, opponentTurnStops: Set<Step>) {
        stopOverrides[playerId] = StopOverrideSettings(myTurnStops, opponentTurnStops)
        logger.info("Player $playerId set stop overrides: myTurn=$myTurnStops, opponentTurn=$opponentTurnStops")
    }

    /**
     * Get per-step stop overrides for a player.
     */
    fun getStopOverrides(playerId: EntityId): StopOverrideSettings {
        return stopOverrides[playerId] ?: StopOverrideSettings()
    }

    // =========================================================================
    // Auto-Pass Management
    // =========================================================================

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

        // Check if player has full control enabled - never auto-pass
        if (isFullControlEnabled(priorityPlayer)) {
            return null
        }

        // Get legal actions for that player
        val legalActions = getLegalActions(priorityPlayer)

        // Check if they should auto-pass
        val overrides = getStopOverrides(priorityPlayer)
        return if (autoPassManager.shouldAutoPass(state, priorityPlayer, legalActions, overrides.myTurnStops, overrides.opponentTurnStops)) {
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

        // If no winner, it's a draw (both players lost simultaneously)
        if (state.winnerId == null) {
            return GameOverReason.DRAW
        }

        // Find the losing player (the one who is not the winner)
        val loserId = state.turnOrder.find { it != state.winnerId }
        val lossComponent = loserId?.let { state.getEntity(it)?.get<PlayerLostComponent>() }

        return when (lossComponent?.reason) {
            LossReason.LIFE_ZERO -> GameOverReason.LIFE_ZERO
            LossReason.EMPTY_LIBRARY -> GameOverReason.DECK_OUT
            LossReason.POISON_COUNTERS -> GameOverReason.POISON_COUNTERS
            LossReason.CONCESSION -> GameOverReason.CONCESSION
            LossReason.CARD_EFFECT -> GameOverReason.CARD_EFFECT
            null -> GameOverReason.LIFE_ZERO // Fallback
        }
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
                val creatures = findValidCreatureTargets(state, playerId, TargetFilter.Creature)
                val players = state.turnOrder.toList()
                creatures + players
            }
            is TargetCreatureOrPlayer -> {
                val creatures = findValidCreatureTargets(state, playerId, TargetFilter.Creature)
                val players = state.turnOrder.toList()
                creatures + players
            }
            is TargetPermanent -> findValidPermanentTargets(state, playerId, requirement.filter)
            is TargetObject -> findValidObjectTargets(state, playerId, requirement.filter)
            is TargetSpell -> findValidSpellTargets(state, playerId, requirement.filter)
            is TargetSpellOrPermanent -> {
                val permanents = findValidPermanentTargets(state, playerId, TargetFilter.Permanent)
                val spells = findValidSpellTargets(state, playerId, TargetFilter.SpellOnStack)
                permanents + spells
            }
            else -> emptyList() // Other target types not yet implemented
        }
    }

    /**
     * Check if a target requirement should be auto-selected for player targets.
     *
     * Auto-selection applies when:
     * 1. The target requirement is TargetPlayer or TargetOpponent
     * 2. Exactly one target is required (count == 1, minCount == 1)
     * 3. Exactly one valid target exists
     *
     * This improves UX in 2-player games where TargetOpponent always has exactly
     * one choice, avoiding unnecessary clicks for spells like "Target opponent discards".
     */
    private fun shouldAutoSelectPlayerTarget(
        requirement: TargetRequirement,
        validTargets: List<EntityId>
    ): Boolean {
        val isPlayerTarget = requirement is TargetPlayer || requirement is TargetOpponent
        val requiresExactlyOne = requirement.count == 1 && requirement.effectiveMinCount == 1
        val hasExactlyOneChoice = validTargets.size == 1

        return isPlayerTarget && requiresExactlyOne && hasExactlyOneChoice
    }

    /**
     * Find valid creature targets based on a filter.
     * Uses PredicateEvaluator with projected state to correctly handle face-down creatures.
     */
    private fun findValidCreatureTargets(
        state: GameState,
        playerId: EntityId,
        filter: TargetFilter
    ): List<EntityId> {
        val projected = stateProjector.project(state)
        val battlefield = state.getBattlefield()
        val context = PredicateContext(controllerId = playerId)
        return battlefield.filter { entityId ->
            // Use projected state for correct face-down creature handling
            predicateEvaluator.matchesWithProjection(state, projected, entityId, filter.baseFilter, context)
        }
    }

    /**
     * Find valid permanent targets based on a filter.
     * Uses PredicateEvaluator with projected state for unified filter matching.
     */
    private fun findValidPermanentTargets(
        state: GameState,
        playerId: EntityId,
        filter: TargetFilter
    ): List<EntityId> {
        val projected = stateProjector.project(state)
        val battlefield = state.getBattlefield()
        val context = PredicateContext(controllerId = playerId)
        return battlefield.filter { entityId ->
            predicateEvaluator.matchesWithProjection(state, projected, entityId, filter.baseFilter, context)
        }
    }

    /**
     * Find valid graveyard card targets based on a filter.
     * Uses PredicateEvaluator for unified filter matching.
     */
    private fun findValidGraveyardTargets(
        state: GameState,
        playerId: EntityId,
        filter: TargetFilter
    ): List<EntityId> {
        // Determine which graveyards to search based on filter's controller predicate
        val playerIds = if (filter.baseFilter.controllerPredicate == ControllerPredicate.ControlledByYou) {
            listOf(playerId)
        } else {
            state.turnOrder.toList()
        }
        val context = PredicateContext(controllerId = playerId)
        return playerIds.flatMap { pid ->
            state.getGraveyard(pid).filter { entityId ->
                predicateEvaluator.matches(state, entityId, filter.baseFilter, context)
            }
        }
    }

    /**
     * Find valid targets for TargetObject, dispatching based on the filter's zone.
     */
    private fun findValidObjectTargets(
        state: GameState,
        playerId: EntityId,
        filter: TargetFilter
    ): List<EntityId> {
        return when (filter.zone) {
            Zone.BATTLEFIELD -> findValidPermanentTargets(state, playerId, filter)
            Zone.GRAVEYARD -> findValidGraveyardTargets(state, playerId, filter)
            Zone.STACK -> findValidSpellTargets(state, playerId, filter)
            else -> emptyList()
        }
    }

    /**
     * Find valid spell targets on the stack based on a filter.
     */
    private fun findValidSpellTargets(
        state: GameState,
        playerId: EntityId,
        filter: TargetFilter
    ): List<EntityId> {
        val context = PredicateContext(controllerId = playerId)
        return state.stack.filter { spellId ->
            predicateEvaluator.matches(state, spellId, filter.baseFilter, context)
        }
    }

    /**
     * Get the target zone for a requirement (e.g., "Graveyard" for graveyard targets).
     * Returns null for battlefield targets (the default).
     */
    private fun getTargetZone(requirement: TargetRequirement): String? {
        return when (requirement) {
            is TargetObject -> requirement.filter.zone.takeIf { it != Zone.BATTLEFIELD }?.name?.let {
                // Use the serialization name to match client Zone enum
                when (requirement.filter.zone) {
                    Zone.GRAVEYARD -> "Graveyard"
                    Zone.STACK -> "Stack"
                    Zone.EXILE -> "Exile"
                    Zone.HAND -> "Hand"
                    Zone.LIBRARY -> "Library"
                    Zone.COMMAND -> "Command"
                    else -> null
                }
            }
            else -> null
        }
    }

    /**
     * Check if an activation restriction is satisfied.
     */
    private fun checkActivationRestriction(
        state: GameState,
        playerId: EntityId,
        restriction: ActivationRestriction
    ): Boolean {
        return when (restriction) {
            is ActivationRestriction.OnlyDuringYourTurn -> state.activePlayerId == playerId
            is ActivationRestriction.BeforeStep -> state.step.ordinal < restriction.step.ordinal
            is ActivationRestriction.DuringPhase -> state.phase == restriction.phase
            is ActivationRestriction.DuringStep -> state.step == restriction.step
            is ActivationRestriction.OnlyIfCondition -> {
                val opponentId = state.turnOrder.firstOrNull { it != playerId }
                val context = EffectContext(
                    sourceId = null,
                    controllerId = playerId,
                    opponentId = opponentId,
                    targets = emptyList(),
                    xValue = 0
                )
                conditionEvaluator.evaluate(state, restriction.condition, context)
            }
            is ActivationRestriction.All -> restriction.restrictions.all {
                checkActivationRestriction(state, playerId, it)
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

    /**
     * Find valid sacrifice targets for an additional cost.
     * Uses PredicateEvaluator for unified filter matching.
     */
    private fun findSacrificeTargets(
        state: GameState,
        playerId: EntityId,
        cost: com.wingedsheep.sdk.scripting.AdditionalCost.SacrificePermanent
    ): List<EntityId> {
        val playerBattlefield = ZoneKey(playerId, Zone.BATTLEFIELD)
        val predicateContext = PredicateContext(controllerId = playerId)

        return state.getZone(playerBattlefield).filter { entityId ->
            val container = state.getEntity(entityId) ?: return@filter false
            container.get<CardComponent>() ?: return@filter false
            val controllerId = container.get<ControllerComponent>()?.playerId
            if (controllerId != playerId) return@filter false

            predicateEvaluator.matches(state, entityId, cost.filter, predicateContext)
        }
    }

    /**
     * Find valid sacrifice targets for an ability cost (AbilityCost.Sacrifice).
     * Uses PredicateEvaluator for unified filter matching.
     */
    private fun findAbilitySacrificeTargets(
        state: GameState,
        playerId: EntityId,
        filter: GameObjectFilter
    ): List<EntityId> {
        val playerBattlefield = ZoneKey(playerId, Zone.BATTLEFIELD)
        val predicateContext = PredicateContext(controllerId = playerId)

        return state.getZone(playerBattlefield).filter { entityId ->
            val container = state.getEntity(entityId) ?: return@filter false
            container.get<CardComponent>() ?: return@filter false
            val controllerId = container.get<ControllerComponent>()?.playerId
            if (controllerId != playerId) return@filter false

            predicateEvaluator.matches(state, entityId, filter, predicateContext)
        }
    }

    /**
     * Find valid untapped permanents that can be tapped for a TapPermanents ability cost.
     * Uses PredicateEvaluator for unified filter matching.
     */
    private fun findAbilityTapTargets(
        state: GameState,
        playerId: EntityId,
        filter: GameObjectFilter
    ): List<EntityId> {
        val playerBattlefield = ZoneKey(playerId, Zone.BATTLEFIELD)
        val predicateContext = PredicateContext(controllerId = playerId)

        return state.getZone(playerBattlefield).filter { entityId ->
            val container = state.getEntity(entityId) ?: return@filter false
            val cardComponent = container.get<CardComponent>() ?: return@filter false
            val controllerId = container.get<ControllerComponent>()?.playerId
            if (controllerId != playerId) return@filter false

            // Must be untapped
            if (container.has<TappedComponent>()) return@filter false

            // Creatures with summoning sickness can't be tapped for costs (unless they have haste)
            if (cardComponent.typeLine.isCreature) {
                val hasSummoningSickness = container.has<SummoningSicknessComponent>()
                val hasHaste = cardComponent.baseKeywords.contains(Keyword.HASTE)
                if (hasSummoningSickness && !hasHaste) return@filter false
            }

            predicateEvaluator.matches(state, entityId, filter, predicateContext)
        }
    }

    /**
     * Find untapped creatures that can be used for Convoke.
     * Creatures with summoning sickness CAN be used for Convoke.
     */
    private fun findConvokeCreatures(state: GameState, playerId: EntityId): List<ConvokeCreatureInfo> {
        val playerBattlefield = ZoneKey(playerId, Zone.BATTLEFIELD)
        return state.getZone(playerBattlefield).mapNotNull { entityId ->
            val container = state.getEntity(entityId) ?: return@mapNotNull null
            val cardComponent = container.get<CardComponent>() ?: return@mapNotNull null

            // Must be a creature
            if (!cardComponent.typeLine.isCreature) return@mapNotNull null

            // Must be controlled by the player
            val controllerId = container.get<ControllerComponent>()?.playerId
            if (controllerId != playerId) return@mapNotNull null

            // Must be untapped
            if (container.has<TappedComponent>()) return@mapNotNull null

            ConvokeCreatureInfo(
                entityId = entityId,
                name = cardComponent.name,
                colors = cardComponent.colors
            )
        }
    }

    /**
     * Check if a spell can be afforded using Convoke creatures to help pay the cost.
     *
     * This is a simplified check that determines if the player has enough resources
     * (mana sources + convoke creatures) to potentially pay the cost. The exact
     * color matching is handled by the client UI and engine during actual casting.
     */
    private fun canAffordWithConvoke(
        state: GameState,
        playerId: EntityId,
        manaCost: com.wingedsheep.sdk.core.ManaCost,
        convokeCreatures: List<ConvokeCreatureInfo>
    ): Boolean {
        // Get available mana from all sources
        val availableMana = manaSolver.getAvailableManaCount(state, playerId)

        // Total resources = mana + convoke creatures
        val totalResources = availableMana + convokeCreatures.size

        // Simple check: can we cover the total CMC?
        // This is a conservative estimate - colored requirements might still fail,
        // but it allows us to highlight the card as potentially castable.
        if (totalResources < manaCost.cmc) {
            return false
        }

        // More precise check: for each colored mana symbol, we need either:
        // - A mana source that can produce that color, OR
        // - A creature of that color to convoke
        // For simplicity, we'll use a greedy approach

        // Count colored requirements using the colorCount property
        val coloredRequirements = manaCost.colorCount

        // Count creatures by color for convoke
        val creatureColors = mutableMapOf<com.wingedsheep.sdk.core.Color, Int>()
        for (creature in convokeCreatures) {
            for (color in creature.colors) {
                creatureColors[color] = (creatureColors[color] ?: 0) + 1
            }
        }

        // Check if we can cover colored requirements with creatures
        // (mana sources can produce any color, so they're always valid)
        var creaturesUsedForColors = 0
        for ((color, needed) in coloredRequirements) {
            val creaturesOfColor = creatureColors[color] ?: 0
            // We can use creatures of this color, but need to track how many we use
            creaturesUsedForColors += minOf(needed, creaturesOfColor)
        }

        // Calculate generic mana requirement
        val genericRequired = manaCost.genericAmount

        // Creatures not used for colors can pay generic
        val creaturesForGeneric = convokeCreatures.size - creaturesUsedForColors

        // Total resources for generic = mana sources + unused creatures
        val resourcesForGeneric = availableMana + creaturesForGeneric

        // We can afford if we have enough for generic (colored is covered by creatures or mana)
        return resourcesForGeneric >= genericRequired
    }

    /**
     * Enrich a PendingDecision with imageUri data from the card registry.
     * The engine creates SearchCardInfo with null imageUri because it doesn't have access
     * to card metadata. This function populates the imageUri using the card registry.
     */
    private fun enrichDecision(decision: PendingDecision, state: GameState): PendingDecision {
        return when (decision) {
            is SearchLibraryDecision -> decision.copy(
                cards = decision.cards.mapValues { (entityId, cardInfo) ->
                    val cardComponent = state.getEntity(entityId)?.get<CardComponent>()
                    val imageUri = cardComponent?.cardDefinitionId?.let { defId ->
                        cardRegistry.getCard(defId)?.metadata?.imageUri
                    }
                    cardInfo.copy(imageUri = imageUri)
                }
            )
            is ReorderLibraryDecision -> decision.copy(
                cardInfo = decision.cardInfo.mapValues { (entityId, cardInfo) ->
                    val cardComponent = state.getEntity(entityId)?.get<CardComponent>()
                    val imageUri = cardComponent?.cardDefinitionId?.let { defId ->
                        cardRegistry.getCard(defId)?.metadata?.imageUri
                    }
                    cardInfo.copy(imageUri = imageUri)
                }
            )
            is SelectCardsDecision -> {
                val enrichedCardInfo = decision.cardInfo?.mapValues { (entityId, cardInfo) ->
                    val cardComponent = state.getEntity(entityId)?.get<CardComponent>()
                    val imageUri = cardComponent?.cardDefinitionId?.let { defId ->
                        cardRegistry.getCard(defId)?.metadata?.imageUri
                    }
                    cardInfo.copy(imageUri = imageUri)
                }
                decision.copy(cardInfo = enrichedCardInfo)
            }
            is OrderObjectsDecision -> {
                val enrichedCardInfo = decision.cardInfo?.mapValues { (entityId, cardInfo) ->
                    val cardComponent = state.getEntity(entityId)?.get<CardComponent>()
                    val imageUri = cardComponent?.cardDefinitionId?.let { defId ->
                        cardRegistry.getCard(defId)?.metadata?.imageUri
                    }
                    cardInfo.copy(imageUri = imageUri)
                }
                decision.copy(cardInfo = enrichedCardInfo)
            }
            // Other decision types don't have card info to enrich
            else -> decision
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
     * Inject a pre-built game state for dev scenario testing.
     * Unlike injectStateForTesting, this doesn't require PlayerSession objects,
     * allowing scenarios to be created before players connect via WebSocket.
     *
     * Players will be associated when they connect using associatePlayer().
     *
     * **WARNING:** This method is for development testing only.
     *
     * @param state The game state to inject
     */
    fun injectStateForDevScenario(state: GameState) {
        synchronized(stateLock) {
            gameState = state
            players.clear()
        }
    }

    /**
     * Reset the game state for dev scenario testing while preserving connected player sessions.
     * This allows resetting to a new scenario without requiring players to reconnect.
     *
     * **WARNING:** This method is for development testing only.
     *
     * @param state The new game state to inject
     */
    fun resetStateForDevScenario(state: GameState) {
        synchronized(stateLock) {
            gameState = state
            gameLogs.clear()
            lastProcessedMessageId.clear()
            // Players map is preserved so connected sessions remain valid
        }
    }

    /**
     * Get the raw game state for testing assertions.
     * **WARNING:** This method is for testing only.
     */
    fun getStateForTesting(): GameState? = gameState

    // =========================================================================
    // Persistence Support (for Redis caching)
    // =========================================================================

    /**
     * Get the current game state for persistence.
     */
    internal fun getStateForPersistence(): GameState? = gameState

    /**
     * Get the deck lists for persistence.
     */
    internal fun getDeckListsForPersistence(): Map<EntityId, List<String>> = deckLists.toMap()

    /**
     * Get the game logs for persistence.
     */
    internal fun getLogsForPersistence(): Map<EntityId, List<ClientEvent>> =
        gameLogs.mapValues { it.value.toList() }

    /**
     * Get the last processed message IDs for persistence.
     */
    internal fun getLastMessageIdsForPersistence(): Map<EntityId, String> =
        lastProcessedMessageId.toMap()

    /**
     * Restore session state from persistence.
     * Called when loading a session from Redis after server restart.
     *
     * Note: Player sessions are NOT restored here. Players reconnect and
     * re-associate with their identity via token.
     */
    internal fun restoreFromPersistence(
        state: GameState?,
        decks: Map<EntityId, List<String>>,
        logs: Map<EntityId, MutableList<ClientEvent>>,
        lastIds: Map<EntityId, String>
    ) {
        synchronized(stateLock) {
            gameState = state
            deckLists.clear()
            deckLists.putAll(decks)
            gameLogs.clear()
            gameLogs.putAll(logs)
            lastProcessedMessageId.clear()
            lastProcessedMessageId.putAll(lastIds)
        }
    }

    /**
     * Associate a player identity with this session (for reconnection after restore).
     */
    fun associatePlayer(playerSession: PlayerSession) {
        players[playerSession.playerId] = playerSession
        playerSession.currentGameSessionId = sessionId
    }

    /**
     * Store a player's info for persistence.
     * Should be called when a player joins the game.
     */
    fun setPlayerPersistenceInfo(playerId: EntityId, playerName: String, token: String) {
        playerPersistenceInfo[playerId] = PlayerPersistenceInfo(playerName, token)
    }

    /**
     * Get all stored player info for persistence.
     */
    fun getPlayerPersistenceInfo(): Map<EntityId, PlayerPersistenceInfo> = playerPersistenceInfo.toMap()

    /**
     * Restore player info from persistence.
     */
    internal fun restorePlayerPersistenceInfo(info: Map<EntityId, PlayerPersistenceInfo>) {
        playerPersistenceInfo.clear()
        playerPersistenceInfo.putAll(info)
    }
}
