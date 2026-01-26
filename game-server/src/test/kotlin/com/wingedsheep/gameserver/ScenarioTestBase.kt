package com.wingedsheep.gameserver

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.mtg.sets.definitions.portal.PortalSet
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.SummoningSicknessComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.LifeTotalComponent
import com.wingedsheep.engine.state.components.identity.OwnerComponent
import com.wingedsheep.engine.state.components.identity.PlayerComponent
import com.wingedsheep.engine.state.components.player.LandDropsComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.gameserver.dto.ClientGameState
import com.wingedsheep.gameserver.dto.ClientStateTransformer
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.ZoneType
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import java.util.concurrent.atomic.AtomicLong

/**
 * Base class for scenario-based tests that directly manipulate game state.
 *
 * This provides a cleaner testing API than the WebSocket-based GameServerTestBase
 * for testing game logic without network overhead.
 */
abstract class ScenarioTestBase : FunSpec() {

    protected val cardRegistry = CardRegistry().apply {
        register(PortalSet.allCards)
    }
    protected val actionProcessor = ActionProcessor(cardRegistry)
    protected val stateTransformer = ClientStateTransformer(cardRegistry)

    /**
     * Builder for constructing test scenarios with specific game states.
     */
    inner class ScenarioBuilder {
        private val entityIdCounter = AtomicLong(1000)
        private var state = GameState()

        private var player1Id: EntityId? = null
        private var player2Id: EntityId? = null

        /**
         * Initialize the scenario with two players.
         */
        fun withPlayers(
            player1Name: String = "Player1",
            player2Name: String = "Player2"
        ): ScenarioBuilder {
            player1Id = EntityId.of("player-1")
            player2Id = EntityId.of("player-2")

            // Create player entities
            val p1Container = ComponentContainer.of(
                PlayerComponent(player1Name),
                LifeTotalComponent(20),
                ManaPoolComponent(),
                LandDropsComponent(remaining = 1, maxPerTurn = 1)
            )

            val p2Container = ComponentContainer.of(
                PlayerComponent(player2Name),
                LifeTotalComponent(20),
                ManaPoolComponent(),
                LandDropsComponent(remaining = 1, maxPerTurn = 1)
            )

            state = state
                .withEntity(player1Id!!, p1Container)
                .withEntity(player2Id!!, p2Container)
                .copy(
                    turnOrder = listOf(player1Id!!, player2Id!!),
                    activePlayerId = player1Id,
                    priorityPlayerId = player1Id,
                    phase = Phase.PRECOMBAT_MAIN,
                    step = Step.PRECOMBAT_MAIN,
                    turnNumber = 1
                )

            // Initialize empty zones for both players
            for (playerId in listOf(player1Id!!, player2Id!!)) {
                for (zoneType in listOf(ZoneType.HAND, ZoneType.LIBRARY, ZoneType.GRAVEYARD, ZoneType.BATTLEFIELD)) {
                    val zoneKey = ZoneKey(playerId, zoneType)
                    state = state.copy(zones = state.zones + (zoneKey to emptyList()))
                }
            }

            return this
        }

        /**
         * Add a card to a player's hand.
         */
        fun withCardInHand(playerNumber: Int, cardName: String): ScenarioBuilder {
            val playerId = if (playerNumber == 1) player1Id!! else player2Id!!
            val cardId = createCard(cardName, playerId)
            state = state.addToZone(ZoneKey(playerId, ZoneType.HAND), cardId)
            return this
        }

        /**
         * Add multiple copies of a card to a player's hand.
         */
        fun withCardsInHand(playerNumber: Int, cardName: String, count: Int): ScenarioBuilder {
            repeat(count) { withCardInHand(playerNumber, cardName) }
            return this
        }

        /**
         * Add a card to the battlefield under a player's control.
         * By default, removes summoning sickness (as if it's been there).
         */
        fun withCardOnBattlefield(
            playerNumber: Int,
            cardName: String,
            tapped: Boolean = false,
            summoningSickness: Boolean = false
        ): ScenarioBuilder {
            val playerId = if (playerNumber == 1) player1Id!! else player2Id!!
            val cardId = createCard(cardName, playerId)

            // Add to battlefield
            state = state.addToZone(ZoneKey(playerId, ZoneType.BATTLEFIELD), cardId)

            // Update card entity with battlefield-specific components
            var container = state.getEntity(cardId)!!
            container = container.with(ControllerComponent(playerId))

            if (tapped) {
                container = container.with(TappedComponent)
            }

            if (summoningSickness) {
                container = container.with(SummoningSicknessComponent)
            }

            state = state.withEntity(cardId, container)
            return this
        }

        /**
         * Add multiple lands to the battlefield (untapped, ready to tap for mana).
         */
        fun withLandsOnBattlefield(playerNumber: Int, landName: String, count: Int): ScenarioBuilder {
            repeat(count) { withCardOnBattlefield(playerNumber, landName, tapped = false) }
            return this
        }

        /**
         * Add a card to a player's library.
         */
        fun withCardInLibrary(playerNumber: Int, cardName: String): ScenarioBuilder {
            val playerId = if (playerNumber == 1) player1Id!! else player2Id!!
            val cardId = createCard(cardName, playerId)
            state = state.addToZone(ZoneKey(playerId, ZoneType.LIBRARY), cardId)
            return this
        }

        /**
         * Add a card to a player's graveyard.
         */
        fun withCardInGraveyard(playerNumber: Int, cardName: String): ScenarioBuilder {
            val playerId = if (playerNumber == 1) player1Id!! else player2Id!!
            val cardId = createCard(cardName, playerId)
            state = state.addToZone(ZoneKey(playerId, ZoneType.GRAVEYARD), cardId)
            return this
        }

        /**
         * Set a player's life total.
         */
        fun withLifeTotal(playerNumber: Int, life: Int): ScenarioBuilder {
            val playerId = if (playerNumber == 1) player1Id!! else player2Id!!
            state = state.updateEntity(playerId) { container ->
                container.with(LifeTotalComponent(life))
            }
            return this
        }

        /**
         * Set the current phase and step.
         */
        fun inPhase(phase: Phase, step: Step): ScenarioBuilder {
            state = state.copy(phase = phase, step = step)
            return this
        }

        /**
         * Set the active player (whose turn it is).
         */
        fun withActivePlayer(playerNumber: Int): ScenarioBuilder {
            val playerId = if (playerNumber == 1) player1Id!! else player2Id!!
            state = state.copy(activePlayerId = playerId, priorityPlayerId = playerId)
            return this
        }

        /**
         * Set the priority player.
         */
        fun withPriorityPlayer(playerNumber: Int): ScenarioBuilder {
            val playerId = if (playerNumber == 1) player1Id!! else player2Id!!
            state = state.copy(priorityPlayerId = playerId)
            return this
        }

        /**
         * Build the scenario, returning a TestGame instance for assertions.
         */
        fun build(): TestGame {
            return TestGame(
                state = state,
                player1Id = player1Id!!,
                player2Id = player2Id!!,
                cardRegistry = cardRegistry,
                actionProcessor = actionProcessor,
                stateTransformer = stateTransformer
            )
        }

        private fun createCard(cardName: String, ownerId: EntityId): EntityId {
            val cardDef = cardRegistry.getCard(cardName)
                ?: error("Card not found in registry: $cardName")

            val cardId = EntityId.of("card-${entityIdCounter.incrementAndGet()}")

            val cardComponent = CardComponent(
                cardDefinitionId = cardDef.name,
                name = cardDef.name,
                manaCost = cardDef.manaCost,
                typeLine = cardDef.typeLine,
                oracleText = cardDef.oracleText,
                colors = cardDef.colors,
                baseKeywords = cardDef.keywords,
                baseStats = cardDef.creatureStats,
                ownerId = ownerId,
                spellEffect = cardDef.spellEffect
            )

            val container = ComponentContainer.of(
                cardComponent,
                OwnerComponent(ownerId),
                ControllerComponent(ownerId)
            )

            state = state.withEntity(cardId, container)
            return cardId
        }
    }

    /**
     * Create a new scenario builder.
     */
    protected fun scenario(): ScenarioBuilder = ScenarioBuilder()

    /**
     * Represents a test game that can be queried and advanced.
     */
    class TestGame(
        var state: GameState,
        val player1Id: EntityId,
        val player2Id: EntityId,
        private val cardRegistry: CardRegistry,
        private val actionProcessor: ActionProcessor,
        private val stateTransformer: ClientStateTransformer
    ) {
        /**
         * Execute an action and update the state.
         */
        fun execute(action: GameAction): ExecutionResult {
            val result = actionProcessor.process(state, action)
            if (result.error == null) {
                state = result.state
            }
            return result
        }

        /**
         * Cast a spell by name from a player's hand, optionally targeting an entity.
         */
        fun castSpell(
            playerNumber: Int,
            spellName: String,
            targetId: EntityId? = null
        ): ExecutionResult {
            val playerId = if (playerNumber == 1) player1Id else player2Id
            val hand = state.getHand(playerId)
            val cardId = hand.find { entityId ->
                state.getEntity(entityId)?.get<CardComponent>()?.name == spellName
            } ?: error("Card '$spellName' not found in player $playerNumber's hand")

            val targets = if (targetId != null) {
                listOf(ChosenTarget.Permanent(targetId))
            } else {
                emptyList()
            }

            return execute(CastSpell(playerId, cardId, targets))
        }

        /**
         * Cast a spell with X in its mana cost.
         * @param playerNumber The player casting the spell (1 or 2)
         * @param spellName The name of the spell to cast
         * @param xValue The value chosen for X
         * @param targetId Optional target for targeted spells
         */
        fun castXSpell(
            playerNumber: Int,
            spellName: String,
            xValue: Int,
            targetId: EntityId? = null
        ): ExecutionResult {
            val playerId = if (playerNumber == 1) player1Id else player2Id
            val hand = state.getHand(playerId)
            val cardId = hand.find { entityId ->
                state.getEntity(entityId)?.get<CardComponent>()?.name == spellName
            } ?: error("Card '$spellName' not found in player $playerNumber's hand")

            val targets = if (targetId != null) {
                listOf(ChosenTarget.Permanent(targetId))
            } else {
                emptyList()
            }

            return execute(CastSpell(playerId, cardId, targets, xValue))
        }

        /**
         * Pass priority for the player who currently has it.
         */
        fun passPriority(): ExecutionResult {
            val playerId = state.priorityPlayerId ?: error("No player has priority")
            return execute(PassPriority(playerId))
        }

        /**
         * Pass priority for both players to resolve the stack.
         */
        fun resolveStack(): List<ExecutionResult> {
            val results = mutableListOf<ExecutionResult>()
            var iterations = 0
            while (state.stack.isNotEmpty() && iterations++ < 20) {
                results.add(passPriority())
                if (state.stack.isNotEmpty()) {
                    results.add(passPriority())
                }
            }
            return results
        }

        /**
         * Get the client-facing state for a player.
         */
        fun getClientState(playerNumber: Int): ClientGameState {
            val playerId = if (playerNumber == 1) player1Id else player2Id
            return stateTransformer.transform(state, playerId)
        }

        /**
         * Find a permanent on the battlefield by name.
         */
        fun findPermanent(name: String): EntityId? {
            return state.getBattlefield().find { entityId ->
                state.getEntity(entityId)?.get<CardComponent>()?.name == name
            }
        }

        /**
         * Check if a card with the given name is in a player's graveyard.
         */
        fun isInGraveyard(playerNumber: Int, cardName: String): Boolean {
            val playerId = if (playerNumber == 1) player1Id else player2Id
            return state.getGraveyard(playerId).any { entityId ->
                state.getEntity(entityId)?.get<CardComponent>()?.name == cardName
            }
        }

        /**
         * Check if a card with the given name is on the battlefield.
         */
        fun isOnBattlefield(cardName: String): Boolean {
            return findPermanent(cardName) != null
        }

        /**
         * Get a player's library size.
         */
        fun librarySize(playerNumber: Int): Int {
            val playerId = if (playerNumber == 1) player1Id else player2Id
            return state.getLibrary(playerId).size
        }

        /**
         * Get a player's graveyard size.
         */
        fun graveyardSize(playerNumber: Int): Int {
            val playerId = if (playerNumber == 1) player1Id else player2Id
            return state.getGraveyard(playerId).size
        }

        /**
         * Get a player's hand size.
         */
        fun handSize(playerNumber: Int): Int {
            val playerId = if (playerNumber == 1) player1Id else player2Id
            return state.getHand(playerId).size
        }

        /**
         * Check if there's a pending decision.
         */
        fun hasPendingDecision(): Boolean = state.pendingDecision != null

        /**
         * Get the pending decision, if any.
         */
        fun getPendingDecision(): PendingDecision? = state.pendingDecision

        /**
         * Submit a decision response.
         */
        fun submitDecision(response: DecisionResponse): ExecutionResult {
            val playerId = state.pendingDecision?.playerId
                ?: error("No pending decision to respond to")
            return execute(SubmitDecision(playerId, response))
        }

        /**
         * Submit a card selection decision (select specific cards).
         */
        fun selectCards(cardIds: List<EntityId>): ExecutionResult {
            val decisionId = state.pendingDecision?.id
                ?: error("No pending decision to respond to")
            return submitDecision(CardsSelectedResponse(decisionId, cardIds))
        }

        /**
         * Submit a "skip" decision (select no cards).
         */
        fun skipSelection(): ExecutionResult {
            val decisionId = state.pendingDecision?.id
                ?: error("No pending decision to respond to")
            return submitDecision(CardsSelectedResponse(decisionId, emptyList()))
        }

        /**
         * Submit a number choice decision.
         */
        fun chooseNumber(number: Int): ExecutionResult {
            val decisionId = state.pendingDecision?.id
                ?: error("No pending decision to respond to")
            return submitDecision(NumberChosenResponse(decisionId, number))
        }

        /**
         * Find cards in a player's hand by name.
         */
        fun findCardsInHand(playerNumber: Int, cardName: String): List<EntityId> {
            val playerId = if (playerNumber == 1) player1Id else player2Id
            return state.getHand(playerId).filter { entityId ->
                state.getEntity(entityId)?.get<CardComponent>()?.name == cardName
            }
        }

        /**
         * Check if a card with the given name is in a player's hand.
         */
        fun isInHand(playerNumber: Int, cardName: String): Boolean {
            return findCardsInHand(playerNumber, cardName).isNotEmpty()
        }

        /**
         * Advance the game to a specific phase and step by directly setting the state.
         * Use this when you need to "jump" to a specific step for testing.
         * For tests that need to go through normal priority flow, use passUntilPhase() instead.
         */
        fun advanceToPhase(phase: Phase, step: Step): GameState {
            state = state.copy(phase = phase, step = step)
            return state
        }

        /**
         * Advance the game to a specific phase and step by passing priority.
         * Both players pass priority repeatedly until the target phase/step is reached.
         * This goes through the actual game flow including combat damage processing.
         */
        fun passUntilPhase(phase: Phase, step: Step): GameState {
            var iterations = 0
            val maxIterations = 100

            while ((state.phase != phase || state.step != step) && iterations < maxIterations) {
                val priorityPlayer = state.priorityPlayerId
                if (priorityPlayer != null) {
                    execute(PassPriority(priorityPlayer))
                } else {
                    // No priority player - might be in a step transition, try advancing
                    break
                }
                iterations++
            }

            if (iterations >= maxIterations) {
                error("Failed to advance to $phase/$step after $maxIterations iterations. Current: ${state.phase}/${state.step}")
            }

            return state
        }

        /**
         * Declare attackers.
         * @param attackers Map of permanent names to the player number being attacked (1 or 2)
         */
        fun declareAttackers(attackers: Map<String, Int>): ExecutionResult {
            val attackingPlayer = state.activePlayerId!!
            val attackerMap = attackers.mapNotNull { (name, targetPlayerNum) ->
                val attackerId = findPermanent(name)
                val targetPlayerId = if (targetPlayerNum == 1) player1Id else player2Id
                if (attackerId != null) attackerId to targetPlayerId else null
            }.toMap()

            return execute(DeclareAttackers(attackingPlayer, attackerMap))
        }

        /**
         * Declare blockers.
         * @param blockers Map of blocker permanent names to list of attacker permanent names being blocked
         */
        fun declareBlockers(blockers: Map<String, List<String>>): ExecutionResult {
            val blockingPlayer = state.turnOrder.first { it != state.activePlayerId }
            val blockerMap = blockers.mapNotNull { (blockerName, attackerNames) ->
                val blockerId = findPermanent(blockerName)
                val attackerIds = attackerNames.mapNotNull { findPermanent(it) }
                if (blockerId != null && attackerIds.isNotEmpty()) blockerId to attackerIds else null
            }.toMap()

            return execute(DeclareBlockers(blockingPlayer, blockerMap))
        }

        /**
         * Declare no blockers (empty blocker declaration).
         */
        fun declareNoBlockers(): ExecutionResult {
            val blockingPlayer = state.turnOrder.first { it != state.activePlayerId }
            return execute(DeclareBlockers(blockingPlayer, emptyMap()))
        }

        /**
         * Find all permanents on the battlefield with a given name.
         */
        fun findAllPermanents(name: String): List<EntityId> {
            return state.getBattlefield().filter { entityId ->
                state.getEntity(entityId)?.get<CardComponent>()?.name == name
            }
        }

        /**
         * Get a player's life total.
         */
        fun getLifeTotal(playerNumber: Int): Int {
            val playerId = if (playerNumber == 1) player1Id else player2Id
            return state.getEntity(playerId)?.get<LifeTotalComponent>()?.life ?: 0
        }

        /**
         * Find cards in a player's graveyard by name.
         */
        fun findCardsInGraveyard(playerNumber: Int, cardName: String): List<EntityId> {
            val playerId = if (playerNumber == 1) player1Id else player2Id
            return state.getGraveyard(playerId).filter { entityId ->
                state.getEntity(entityId)?.get<CardComponent>()?.name == cardName
            }
        }

        /**
         * Submit a target selection for a triggered ability (e.g., Gravedigger's ETB).
         * @param targets List of entity IDs to select as targets
         */
        fun selectTargets(targets: List<EntityId>): ExecutionResult {
            val decisionId = state.pendingDecision?.id
                ?: error("No pending decision to respond to")
            return submitDecision(TargetsResponse(decisionId, mapOf(0 to targets)))
        }

        /**
         * Submit a "skip targets" decision (select no targets for optional ability).
         */
        fun skipTargets(): ExecutionResult {
            val decisionId = state.pendingDecision?.id
                ?: error("No pending decision to respond to")
            return submitDecision(TargetsResponse(decisionId, mapOf(0 to emptyList())))
        }
    }
}
