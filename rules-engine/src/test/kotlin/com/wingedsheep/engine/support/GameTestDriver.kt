package com.wingedsheep.engine.support

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.TargetsResponse
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.LifeTotalComponent
import com.wingedsheep.engine.state.components.identity.PlayerComponent
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
class GameTestDriver {
    private val cardRegistry = CardRegistry()
    private val processor: ActionProcessor = ActionProcessor(cardRegistry)
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
            if (state.pendingDecision != null) {
                // Auto-resolve pending decisions (e.g., discard to hand size at cleanup)
                autoResolveDecision()
                stuckCount = 0
            } else if (state.priorityPlayerId != null) {
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
            if (state.pendingDecision != null) {
                autoResolveDecision()
            } else if (state.priorityPlayerId != null) {
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
     * Cast a spell with smart mana payment.
     * - Uses FromPool if player has mana in pool
     * - Falls back to AutoPay (tapping lands) otherwise
     * Targets can be players or permanents - the method auto-detects which type each is.
     */
    fun castSpell(
        playerId: EntityId,
        cardId: EntityId,
        targets: List<EntityId> = emptyList()
    ): ExecutionResult {
        // Check if player has mana in pool
        val pool = state.getEntity(playerId)
            ?.get<com.wingedsheep.engine.state.components.player.ManaPoolComponent>()
        val hasManaInPool = pool != null &&
            (pool.white > 0 || pool.blue > 0 || pool.black > 0 ||
             pool.red > 0 || pool.green > 0 || pool.colorless > 0)

        val paymentStrategy = if (hasManaInPool) {
            PaymentStrategy.FromPool
        } else {
            PaymentStrategy.AutoPay
        }

        return submit(
            CastSpell(
                playerId = playerId,
                cardId = cardId,
                targets = targets.map { targetId ->
                    // Detect if target is a player or a permanent
                    val entity = state.getEntity(targetId)
                    if (entity?.get<PlayerComponent>() != null) {
                        ChosenTarget.Player(targetId)
                    } else {
                        ChosenTarget.Permanent(targetId)
                    }
                },
                paymentStrategy = paymentStrategy
            )
        )
    }

    /**
     * Cast a spell using pre-built ChosenTarget list with smart payment.
     */
    fun castSpellWithTargets(
        playerId: EntityId,
        cardId: EntityId,
        targets: List<ChosenTarget>
    ): ExecutionResult {
        // Check if player has mana in pool
        val pool = state.getEntity(playerId)
            ?.get<com.wingedsheep.engine.state.components.player.ManaPoolComponent>()
        val hasManaInPool = pool != null &&
            (pool.white > 0 || pool.blue > 0 || pool.black > 0 ||
             pool.red > 0 || pool.green > 0 || pool.colorless > 0)

        val paymentStrategy = if (hasManaInPool) {
            PaymentStrategy.FromPool
        } else {
            PaymentStrategy.AutoPay
        }

        return submit(
            CastSpell(
                playerId = playerId,
                cardId = cardId,
                targets = targets,
                paymentStrategy = paymentStrategy
            )
        )
    }

    /**
     * Cast a spell with an X value (for X-cost spells like Hurricane).
     */
    fun castXSpell(
        playerId: EntityId,
        cardId: EntityId,
        xValue: Int,
        targets: List<EntityId> = emptyList()
    ): ExecutionResult {
        // Check if player has mana in pool
        val pool = state.getEntity(playerId)
            ?.get<com.wingedsheep.engine.state.components.player.ManaPoolComponent>()
        val hasManaInPool = pool != null &&
            (pool.white > 0 || pool.blue > 0 || pool.black > 0 ||
             pool.red > 0 || pool.green > 0 || pool.colorless > 0)

        val paymentStrategy = if (hasManaInPool) {
            PaymentStrategy.FromPool
        } else {
            PaymentStrategy.AutoPay
        }

        return submit(
            CastSpell(
                playerId = playerId,
                cardId = cardId,
                targets = targets.map { targetId ->
                    val entity = state.getEntity(targetId)
                    if (entity?.get<PlayerComponent>() != null) {
                        ChosenTarget.Player(targetId)
                    } else {
                        ChosenTarget.Permanent(targetId)
                    }
                },
                xValue = xValue,
                paymentStrategy = paymentStrategy
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
     * Give a player mana directly (test helper).
     */
    fun giveMana(playerId: EntityId, color: com.wingedsheep.sdk.core.Color, amount: Int = 1) {
        _state = _state.updateEntity(playerId) { container ->
            val pool = container.get<com.wingedsheep.engine.state.components.player.ManaPoolComponent>()
                ?: com.wingedsheep.engine.state.components.player.ManaPoolComponent()
            container.with(pool.add(color, amount))
        }
    }

    /**
     * Give a player colorless mana directly (test helper).
     */
    fun giveColorlessMana(playerId: EntityId, amount: Int) {
        _state = _state.updateEntity(playerId) { container ->
            val pool = container.get<com.wingedsheep.engine.state.components.player.ManaPoolComponent>()
                ?: com.wingedsheep.engine.state.components.player.ManaPoolComponent()
            container.with(pool.addColorless(amount))
        }
    }

    /**
     * Put a specific card directly into a player's hand (test helper).
     * Creates a new card entity from the registry and adds it to hand.
     * This is deterministic - always succeeds if the card is registered.
     */
    fun putCardInHand(playerId: EntityId, cardName: String): EntityId {
        val cardDef = cardRegistry.requireCard(cardName)
        val cardId = EntityId.generate()

        // Create card entity
        val cardComponent = CardComponent(
            cardDefinitionId = cardDef.name,
            name = cardDef.name,
            manaCost = cardDef.manaCost,
            typeLine = cardDef.typeLine,
            oracleText = cardDef.oracleText,
            baseStats = cardDef.creatureStats,
            baseKeywords = cardDef.keywords,
            colors = cardDef.colors,
            ownerId = playerId,
            spellEffect = cardDef.spellEffect
        )

        val container = com.wingedsheep.engine.state.ComponentContainer.of(
            cardComponent,
            com.wingedsheep.engine.state.components.identity.OwnerComponent(playerId),
            ControllerComponent(playerId)
        )

        _state = _state.withEntity(cardId, container)

        // Add to hand
        val handZone = ZoneKey(playerId, ZoneType.HAND)
        _state = _state.addToZone(handZone, cardId)

        return cardId
    }

    /**
     * Put a creature directly onto the battlefield (test helper).
     * Creates a new card entity from the registry and adds it to battlefield.
     */
    fun putCreatureOnBattlefield(playerId: EntityId, cardName: String): EntityId {
        val cardDef = cardRegistry.requireCard(cardName)
        val cardId = EntityId.generate()

        // Create card entity
        val cardComponent = CardComponent(
            cardDefinitionId = cardDef.name,
            name = cardDef.name,
            manaCost = cardDef.manaCost,
            typeLine = cardDef.typeLine,
            oracleText = cardDef.oracleText,
            baseStats = cardDef.creatureStats,
            baseKeywords = cardDef.keywords,
            colors = cardDef.colors,
            ownerId = playerId,
            spellEffect = cardDef.spellEffect
        )

        val container = com.wingedsheep.engine.state.ComponentContainer.of(
            cardComponent,
            com.wingedsheep.engine.state.components.identity.OwnerComponent(playerId),
            ControllerComponent(playerId),
            com.wingedsheep.engine.state.components.battlefield.SummoningSicknessComponent
        )

        _state = _state.withEntity(cardId, container)

        // Add to battlefield
        val battlefieldZone = ZoneKey(playerId, ZoneType.BATTLEFIELD)
        _state = _state.addToZone(battlefieldZone, cardId)

        return cardId
    }

    /**
     * Tap a permanent (test helper).
     */
    fun tapPermanent(entityId: EntityId) {
        _state = _state.updateEntity(entityId) { container ->
            container.with(TappedComponent)
        }
    }

    /**
     * Untap a permanent (test helper).
     */
    fun untapPermanent(entityId: EntityId) {
        _state = _state.updateEntity(entityId) { container ->
            container.without<TappedComponent>()
        }
    }

    /**
     * Remove summoning sickness from a creature (test helper).
     * This allows the creature to attack/tap immediately.
     */
    fun removeSummoningSickness(entityId: EntityId) {
        _state = _state.updateEntity(entityId) { container ->
            container.without<com.wingedsheep.engine.state.components.battlefield.SummoningSicknessComponent>()
        }
    }

    /**
     * Put a land directly onto the battlefield (test helper).
     * Creates a new card entity from the registry and adds it to battlefield.
     */
    fun putLandOnBattlefield(playerId: EntityId, cardName: String): EntityId {
        val cardDef = cardRegistry.requireCard(cardName)
        val cardId = EntityId.generate()

        // Create card entity
        val cardComponent = CardComponent(
            cardDefinitionId = cardDef.name,
            name = cardDef.name,
            manaCost = cardDef.manaCost,
            typeLine = cardDef.typeLine,
            oracleText = cardDef.oracleText,
            baseStats = cardDef.creatureStats,
            baseKeywords = cardDef.keywords,
            colors = cardDef.colors,
            ownerId = playerId,
            spellEffect = cardDef.spellEffect
        )

        val container = com.wingedsheep.engine.state.ComponentContainer.of(
            cardComponent,
            com.wingedsheep.engine.state.components.identity.OwnerComponent(playerId),
            ControllerComponent(playerId)
        )

        _state = _state.withEntity(cardId, container)

        // Add to battlefield
        val battlefieldZone = ZoneKey(playerId, ZoneType.BATTLEFIELD)
        _state = _state.addToZone(battlefieldZone, cardId)

        return cardId
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

    /**
     * Assert the stack has the expected size.
     */
    fun assertStackSize(expected: Int, message: String? = null) {
        val actual = state.stack.size
        if (actual != expected) {
            val msg = message ?: "Stack size mismatch"
            throw AssertionError("$msg: expected $expected but was $actual")
        }
    }

    // =========================================================================
    // Stack Queries
    // =========================================================================

    /**
     * Get the stack size.
     */
    val stackSize: Int get() = state.stack.size

    /**
     * Get spell names on the stack (top to bottom).
     */
    fun getStackSpellNames(): List<String> {
        return state.stack.reversed().mapNotNull { entityId ->
            state.getEntity(entityId)?.get<CardComponent>()?.name
        }
    }

    /**
     * Get the top spell on the stack.
     */
    fun getTopOfStack(): EntityId? = state.getTopOfStack()

    /**
     * Get the name of the top spell on the stack.
     */
    fun getTopOfStackName(): String? {
        val topId = state.getTopOfStack() ?: return null
        return state.getEntity(topId)?.get<CardComponent>()?.name
    }

    // =========================================================================
    // Setup Helpers
    // =========================================================================

    /**
     * Play multiple lands for a player (advances to main phase, plays lands).
     * Useful for setting up mana in tests.
     *
     * @param playerId The player to play lands for
     * @param landName The name of the land to play
     * @param count How many lands to play (across multiple turns if needed)
     */
    fun setupLands(playerId: EntityId, landName: String, count: Int) {
        repeat(count) { i ->
            // If not active player's turn, advance until it is
            while (activePlayer != playerId) {
                passPriorityUntil(Step.END)
                bothPass()
            }

            // Advance to main phase
            passPriorityUntil(Step.PRECOMBAT_MAIN)

            // Find and play the land
            val land = findCardInHand(playerId, landName)
            if (land != null) {
                val result = playLand(playerId, land)
                if (!result.isSuccess) {
                    // May have already played a land this turn - advance to next turn
                    passPriorityUntil(Step.END)
                    bothPass()
                    // Retry on next turn
                    while (activePlayer != playerId) {
                        passPriorityUntil(Step.END)
                        bothPass()
                    }
                    passPriorityUntil(Step.PRECOMBAT_MAIN)
                    val retryLand = findCardInHand(playerId, landName)
                    if (retryLand != null) {
                        playLand(playerId, retryLand)
                    }
                }
            }
        }
    }

    /**
     * Get untapped lands controlled by a player.
     */
    fun getUntappedLands(playerId: EntityId): List<EntityId> {
        return getLands(playerId).filter { !isTapped(it) }
    }

    // =========================================================================
    // Decision Handling
    // =========================================================================

    /**
     * Get the pending decision (if any).
     */
    val pendingDecision: PendingDecision? get() = state.pendingDecision

    /**
     * Check if the engine is paused awaiting a decision.
     */
    val isPaused: Boolean get() = state.isPaused()

    /**
     * Auto-resolve a pending decision by picking the first valid option.
     * Used by passPriorityUntil to handle cleanup discard and similar automatic decisions.
     */
    fun autoResolveDecision() {
        val decision = state.pendingDecision
            ?: throw IllegalStateException("No pending decision to auto-resolve")
        when (decision) {
            is SelectCardsDecision -> {
                // Pick the first N options (e.g., discard to hand size)
                val selected = decision.options.take(decision.minSelections)
                submitCardSelection(decision.playerId, selected)
            }
            is YesNoDecision -> {
                submitYesNo(decision.playerId, false)
            }
            is ReorderLibraryDecision -> {
                submitOrderedResponse(decision.playerId, decision.cards)
            }
            else -> throw IllegalStateException(
                "Cannot auto-resolve decision of type ${decision::class.simpleName}"
            )
        }
    }

    /**
     * Submit a decision response.
     */
    fun submitDecision(playerId: EntityId, response: DecisionResponse): ExecutionResult {
        return submit(SubmitDecision(playerId, response))
    }

    /**
     * Submit a card selection response (for discard, sacrifice, etc.).
     */
    fun submitCardSelection(playerId: EntityId, selectedCards: List<EntityId>): ExecutionResult {
        val decision = pendingDecision as? SelectCardsDecision
            ?: throw IllegalStateException("No pending SelectCardsDecision")
        return submitDecision(
            playerId,
            CardsSelectedResponse(decision.id, selectedCards)
        )
    }

    /**
     * Submit a yes/no response.
     */
    fun submitYesNo(playerId: EntityId, choice: Boolean): ExecutionResult {
        val decision = pendingDecision as? YesNoDecision
            ?: throw IllegalStateException("No pending YesNoDecision")
        return submitDecision(
            playerId,
            YesNoResponse(decision.id, choice)
        )
    }

    /**
     * Submit a target selection response (for targeted spells/abilities).
     */
    fun submitTargetSelection(playerId: EntityId, targets: List<EntityId>): ExecutionResult {
        val decision = pendingDecision as? ChooseTargetsDecision
            ?: throw IllegalStateException("No pending ChooseTargetsDecision")
        // Most abilities have a single target requirement at index 0
        return submitDecision(
            playerId,
            TargetsResponse(decision.id, mapOf(0 to targets))
        )
    }

    /**
     * Submit an ordered response (for reorder effects like look at top N and reorder).
     */
    fun submitOrderedResponse(playerId: EntityId, orderedObjects: List<EntityId>): ExecutionResult {
        val decision = pendingDecision as? ReorderLibraryDecision
            ?: throw IllegalStateException("No pending ReorderLibraryDecision")
        return submitDecision(
            playerId,
            OrderedResponse(decision.id, orderedObjects)
        )
    }

    /**
     * Get a player's graveyard.
     */
    fun getGraveyard(playerId: EntityId): List<EntityId> {
        return state.getGraveyard(playerId)
    }

    /**
     * Get the card names in a player's graveyard.
     */
    fun getGraveyardCardNames(playerId: EntityId): List<String> {
        return getGraveyard(playerId).mapNotNull { entityId ->
            state.getEntity(entityId)?.get<CardComponent>()?.name
        }
    }

    /**
     * Get hand size for a player.
     */
    fun getHandSize(playerId: EntityId): Int {
        return getHand(playerId).size
    }
}
