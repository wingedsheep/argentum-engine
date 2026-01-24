package com.wingedsheep.engine.support

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.LifeTotalComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.ZoneType
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId

/**
 * Test driver for the rules engine.
 *
 * The GameTestDriver provides a higher-level interface for testing the engine.
 * It wraps the ActionProcessor and provides convenient methods for:
 * - Initializing games with specific configurations
 * - Submitting actions on behalf of players
 * - Querying game state
 * - Advancing the game through phases automatically
 *
 * ## Philosophy
 * Tests use the "driver model" - they submit GameAction objects to ActionProcessor
 * and verify the resulting GameState. Tests never manipulate state directly.
 *
 * ## Usage
 * ```kotlin
 * val driver = GameTestDriver()
 * driver.registerCard(GrizzlyBears)
 * driver.initGame(
 *     deck1 = Deck.of("Grizzly Bears" to 20, "Forest" to 20),
 *     deck2 = Deck.of("Grizzly Bears" to 20, "Forest" to 20)
 * )
 *
 * // Play a land
 * val forest = driver.findCardInHand(driver.player1, "Forest")!!
 * driver.playLand(driver.player1, forest)
 *
 * // Advance to combat
 * driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
 * ```
 */
class GameTestDriver(
    private val processor: ActionProcessor = ActionProcessor()
) {
    private val cardRegistry = CardRegistry()
    private var _state: GameState = GameState()
    private val _events = mutableListOf<GameEvent>()

    // Player IDs set during initialization (nullable until init is called)
    private var _player1: EntityId? = null
    private var _player2: EntityId? = null

    /** First player ID - throws if game not initialized */
    val player1: EntityId get() = _player1 ?: throw IllegalStateException("Game not initialized")

    /** Second player ID - throws if game not initialized */
    val player2: EntityId get() = _player2 ?: throw IllegalStateException("Game not initialized")

    /** Current game state */
    val state: GameState get() = _state

    /** All events since game start */
    val events: List<GameEvent> get() = _events.toList()

    /** Active player ID */
    val activePlayer: EntityId? get() = _state.activePlayerId

    /** Player with priority */
    val priorityPlayer: EntityId? get() = _state.priorityPlayerId

    /** Current step */
    val currentStep: Step get() = _state.step

    /** Current phase */
    val currentPhase: Phase get() = _state.phase

    // =========================================================================
    // Setup
    // =========================================================================

    /**
     * Register a card definition for use in tests.
     */
    fun registerCard(card: CardDefinition) {
        cardRegistry.register(card)
    }

    /**
     * Register multiple card definitions.
     */
    fun registerCards(cards: Iterable<CardDefinition>) {
        cardRegistry.register(cards)
    }

    /**
     * Initialize a two-player game.
     *
     * @param deck1 Deck for player 1
     * @param deck2 Deck for player 2
     * @param skipMulligans Whether to skip the mulligan phase (default: true for tests)
     * @param startingLife Starting life total for both players (default: 20)
     */
    fun initGame(
        deck1: Deck,
        deck2: Deck,
        skipMulligans: Boolean = true,
        startingLife: Int = 20
    ) {
        val initializer = GameInitializer(cardRegistry)
        val result = initializer.initializeGame(
            GameConfig(
                players = listOf(
                    PlayerConfig("Player 1", deck1, startingLife),
                    PlayerConfig("Player 2", deck2, startingLife)
                ),
                skipMulligans = skipMulligans
            )
        )

        _state = result.state
        _events.clear()
        _events.addAll(result.events)

        _player1 = result.playerIds[0]
        _player2 = result.playerIds[1]
    }

    /**
     * Initialize a game with both players using the same deck.
     */
    fun initMirrorMatch(deck: Deck, skipMulligans: Boolean = true, startingLife: Int = 20) {
        initGame(deck, deck, skipMulligans, startingLife)
    }

    // =========================================================================
    // Action Submission
    // =========================================================================

    /**
     * Submit an action and update state.
     *
     * @throws IllegalStateException if the action fails
     */
    fun submit(action: GameAction): ExecutionResult {
        val result = processor.process(_state, action)
        if (result.isSuccess || result.isPaused) {
            _state = result.newState
            _events.addAll(result.events)
        }
        return result
    }

    /**
     * Submit an action and assert it succeeds.
     */
    fun submitSuccess(action: GameAction): ExecutionResult {
        val result = submit(action)
        if (!result.isSuccess) {
            throw AssertionError("Expected action to succeed but got: ${result.error}")
        }
        return result
    }

    /**
     * Submit an action and expect it to fail.
     */
    fun submitExpectFailure(action: GameAction): ExecutionResult {
        val result = processor.process(_state, action)
        if (result.isSuccess) {
            throw AssertionError("Expected action to fail but it succeeded")
        }
        return result
    }

    // =========================================================================
    // Common Actions
    // =========================================================================

    /**
     * Player passes priority.
     */
    fun passPriority(playerId: EntityId): ExecutionResult {
        return submit(PassPriority(playerId))
    }

    /**
     * Both players pass priority (for stack resolution or phase advancement).
     */
    fun bothPass(): ExecutionResult {
        var result = passPriority(state.priorityPlayerId ?: player1)
        if (result.isSuccess && state.priorityPlayerId != null) {
            result = passPriority(state.priorityPlayerId!!)
        }
        return result
    }

    /**
     * Pass priority until reaching the specified step.
     * This automates the common pattern of skipping through phases.
     *
     * @param targetStep The step to advance to
     * @param maxPasses Safety limit to prevent infinite loops (default: 100)
     */
    fun passPriorityUntil(targetStep: Step, maxPasses: Int = 100) {
        var passes = 0
        var lastStep = state.step
        var stuckCount = 0

        while (state.step != targetStep && passes < maxPasses) {
            if (state.gameOver) {
                throw AssertionError("Game ended while advancing to $targetStep")
            }
            if (state.priorityPlayerId != null) {
                passPriority(state.priorityPlayerId!!)
                stuckCount = 0
            } else {
                // No priority - check if we're stuck
                stuckCount++
                if (stuckCount > 10) {
                    throw AssertionError(
                        "Stuck at step ${state.step} with no priority while trying to reach $targetStep"
                    )
                }
            }

            // Detect if we're making progress
            if (state.step != lastStep) {
                lastStep = state.step
                stuckCount = 0
            }

            passes++
        }

        if (passes >= maxPasses) {
            throw AssertionError("Failed to reach step $targetStep after $maxPasses passes (current: ${state.step})")
        }
    }

    /**
     * Pass priority until reaching the specified phase.
     */
    fun passPriorityUntil(targetPhase: Phase, maxPasses: Int = 100) {
        var passes = 0
        while (state.phase != targetPhase && passes < maxPasses) {
            if (state.gameOver) {
                throw AssertionError("Game ended while advancing to $targetPhase")
            }
            if (state.priorityPlayerId != null) {
                passPriority(state.priorityPlayerId!!)
            }
            passes++
        }

        if (passes >= maxPasses) {
            throw AssertionError("Failed to reach phase $targetPhase after $maxPasses passes (current: ${state.phase})")
        }
    }

    /**
     * Play a land.
     */
    fun playLand(playerId: EntityId, cardId: EntityId): ExecutionResult {
        return submit(PlayLand(playerId, cardId))
    }

    /**
     * Cast a spell with auto-pay.
     */
    fun castSpell(
        playerId: EntityId,
        cardId: EntityId,
        targets: List<EntityId> = emptyList()
    ): ExecutionResult {
        return submit(
            CastSpell(
                playerId = playerId,
                cardId = cardId,
                targets = targets.map { ChosenTarget.Permanent(it) },
                paymentStrategy = PaymentStrategy.AutoPay
            )
        )
    }

    /**
     * Declare attackers.
     */
    fun declareAttackers(playerId: EntityId, attackers: Map<EntityId, EntityId>): ExecutionResult {
        return submit(DeclareAttackers(playerId, attackers))
    }

    /**
     * Declare attackers (all attacking the same player).
     */
    fun declareAttackers(playerId: EntityId, attackers: List<EntityId>, defendingPlayer: EntityId): ExecutionResult {
        return declareAttackers(playerId, attackers.associateWith { defendingPlayer })
    }

    /**
     * Declare blockers.
     */
    fun declareBlockers(playerId: EntityId, blockers: Map<EntityId, List<EntityId>>): ExecutionResult {
        return submit(DeclareBlockers(playerId, blockers))
    }

    /**
     * Declare no blockers.
     */
    fun declareNoBlockers(playerId: EntityId): ExecutionResult {
        return declareBlockers(playerId, emptyMap())
    }

    /**
     * Concede the game.
     */
    fun concede(playerId: EntityId): ExecutionResult {
        return submit(Concede(playerId))
    }

    // =========================================================================
    // State Queries
    // =========================================================================

    /**
     * Find a card by name in a player's hand.
     */
    fun findCardInHand(playerId: EntityId, cardName: String): EntityId? {
        val handZone = ZoneKey(playerId, ZoneType.HAND)
        return state.getZone(handZone).find { entityId ->
            state.getEntity(entityId)?.get<CardComponent>()?.name == cardName
        }
    }

    /**
     * Find all cards by name in a player's hand.
     */
    fun findCardsInHand(playerId: EntityId, cardName: String): List<EntityId> {
        val handZone = ZoneKey(playerId, ZoneType.HAND)
        return state.getZone(handZone).filter { entityId ->
            state.getEntity(entityId)?.get<CardComponent>()?.name == cardName
        }
    }

    /**
     * Find a card by name on a player's battlefield.
     */
    fun findPermanent(playerId: EntityId, cardName: String): EntityId? {
        val battlefieldZone = ZoneKey(playerId, ZoneType.BATTLEFIELD)
        return state.getZone(battlefieldZone).find { entityId ->
            state.getEntity(entityId)?.get<CardComponent>()?.name == cardName
        }
    }

    /**
     * Find all permanents controlled by a player.
     */
    fun getPermanents(playerId: EntityId): List<EntityId> {
        val battlefieldZone = ZoneKey(playerId, ZoneType.BATTLEFIELD)
        return state.getZone(battlefieldZone)
    }

    /**
     * Find all creatures controlled by a player.
     */
    fun getCreatures(playerId: EntityId): List<EntityId> {
        return getPermanents(playerId).filter { entityId ->
            state.getEntity(entityId)?.get<CardComponent>()?.typeLine?.isCreature == true
        }
    }

    /**
     * Find all lands controlled by a player.
     */
    fun getLands(playerId: EntityId): List<EntityId> {
        return getPermanents(playerId).filter { entityId ->
            state.getEntity(entityId)?.get<CardComponent>()?.typeLine?.isLand == true
        }
    }

    /**
     * Get a player's hand.
     */
    fun getHand(playerId: EntityId): List<EntityId> {
        return state.getHand(playerId)
    }

    /**
     * Get a player's life total.
     */
    fun getLifeTotal(playerId: EntityId): Int {
        return state.getEntity(playerId)?.get<LifeTotalComponent>()?.life ?: 0
    }

    /**
     * Get a card's name.
     */
    fun getCardName(entityId: EntityId): String? {
        return state.getEntity(entityId)?.get<CardComponent>()?.name
    }

    /**
     * Check if a permanent is tapped.
     */
    fun isTapped(entityId: EntityId): Boolean {
        return state.getEntity(entityId)?.has<TappedComponent>() == true
    }

    /**
     * Get the controller of a permanent.
     */
    fun getController(entityId: EntityId): EntityId? {
        return state.getEntity(entityId)?.get<ControllerComponent>()?.playerId
    }

    /**
     * Get the opponent of a player (for 2-player games).
     */
    fun getOpponent(playerId: EntityId): EntityId {
        return if (playerId == player1) player2 else player1
    }

    // =========================================================================
    // Assertions
    // =========================================================================

    /**
     * Assert a player's life total.
     */
    fun assertLifeTotal(playerId: EntityId, expected: Int, message: String? = null) {
        val actual = getLifeTotal(playerId)
        if (actual != expected) {
            val msg = message ?: "Life total mismatch"
            throw AssertionError("$msg: expected $expected but was $actual")
        }
    }

    /**
     * Assert the current step.
     */
    fun assertStep(expected: Step, message: String? = null) {
        if (state.step != expected) {
            val msg = message ?: "Step mismatch"
            throw AssertionError("$msg: expected $expected but was ${state.step}")
        }
    }

    /**
     * Assert the current phase.
     */
    fun assertPhase(expected: Phase, message: String? = null) {
        if (state.phase != expected) {
            val msg = message ?: "Phase mismatch"
            throw AssertionError("$msg: expected $expected but was ${state.phase}")
        }
    }

    /**
     * Assert who has priority.
     */
    fun assertPriority(expected: EntityId, message: String? = null) {
        if (state.priorityPlayerId != expected) {
            val msg = message ?: "Priority mismatch"
            throw AssertionError("$msg: expected $expected but was ${state.priorityPlayerId}")
        }
    }

    /**
     * Assert the game is over.
     */
    fun assertGameOver(expectedWinner: EntityId? = null, message: String? = null) {
        if (!state.gameOver) {
            throw AssertionError(message ?: "Expected game to be over")
        }
        if (expectedWinner != null && state.winnerId != expectedWinner) {
            val msg = message ?: "Winner mismatch"
            throw AssertionError("$msg: expected $expectedWinner but was ${state.winnerId}")
        }
    }

    /**
     * Assert a permanent exists on the battlefield.
     */
    fun assertPermanentExists(playerId: EntityId, cardName: String, message: String? = null) {
        if (findPermanent(playerId, cardName) == null) {
            throw AssertionError(message ?: "Expected $cardName on ${playerId}'s battlefield")
        }
    }

    /**
     * Assert a card is in a player's graveyard.
     */
    fun assertInGraveyard(playerId: EntityId, cardName: String, message: String? = null) {
        val inGraveyard = state.getGraveyard(playerId).any { entityId ->
            state.getEntity(entityId)?.get<CardComponent>()?.name == cardName
        }
        if (!inGraveyard) {
            throw AssertionError(message ?: "Expected $cardName in ${playerId}'s graveyard")
        }
    }
}
