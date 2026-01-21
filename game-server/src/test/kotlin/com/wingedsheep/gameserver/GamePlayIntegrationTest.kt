package com.wingedsheep.gameserver

import com.wingedsheep.gameserver.dto.ClientGameState
import com.wingedsheep.gameserver.dto.ClientPlayer
import com.wingedsheep.gameserver.dto.ClientZone
import com.wingedsheep.gameserver.protocol.ClientMessage
import com.wingedsheep.gameserver.protocol.ErrorCode
import com.wingedsheep.gameserver.protocol.GameOverReason
import com.wingedsheep.gameserver.protocol.ServerMessage
import com.wingedsheep.rulesengine.ecs.EntityId
import com.wingedsheep.rulesengine.ecs.ZoneId
import com.wingedsheep.rulesengine.ecs.action.*
import com.wingedsheep.rulesengine.ecs.action.gameActionSerializersModule
import com.wingedsheep.rulesengine.game.Phase
import com.wingedsheep.rulesengine.zone.ZoneType
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.ints.shouldBeGreaterThanOrEqual
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketHttpHeaders
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.client.standard.StandardWebSocketClient
import org.springframework.web.socket.handler.TextWebSocketHandler
import java.net.URI
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.Duration.Companion.seconds

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GamePlayIntegrationTest(
    @LocalServerPort private val port: Int
) : FunSpec({

    // =========================================================================
    // Test Configuration
    // =========================================================================

    val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        classDiscriminator = "type"
        serializersModule = gameActionSerializersModule
    }

    // Track clients for cleanup
    val activeClients = mutableListOf<TestWebSocketClient>()

    // Create a new WebSocket container for each test to ensure isolation
    fun createWsContainer() = org.apache.tomcat.websocket.WsWebSocketContainer().apply {
        defaultMaxSessionIdleTimeout = 300_000L // 5 minutes
        defaultMaxTextMessageBufferSize = 1024 * 1024 // 1MB buffer for large state updates
        defaultMaxBinaryMessageBufferSize = 1024 * 1024 // 1MB buffer
    }

    afterEach {
        activeClients.forEach { it.close() }
        activeClients.clear()
    }

    // =========================================================================
    // Test Fixtures
    // =========================================================================

    val monoGreenLands = mapOf("Forest" to 40)
    val greenCreatures = mapOf("Forest" to 24, "Grizzly Bears" to 16)

    // =========================================================================
    // Helper Functions
    // =========================================================================

    fun createClient(): TestWebSocketClient {
        val client = TestWebSocketClient(json, createWsContainer())
        activeClients.add(client)
        return client
    }

    fun wsUrl(): String = "ws://localhost:$port/game"

    suspend fun TestWebSocketClient.connectAs(playerName: String): String {
        connect(wsUrl())
        send(ClientMessage.Connect(playerName))

        eventually(5.seconds) {
            messages.any { it is ServerMessage.Connected } shouldBe true
        }

        return messages.filterIsInstance<ServerMessage.Connected>().first().playerId
    }

    fun TestWebSocketClient.latestState(): ClientGameState? =
        messages.filterIsInstance<ServerMessage.StateUpdate>().lastOrNull()?.state

    fun TestWebSocketClient.requireLatestState(): ClientGameState =
        latestState() ?: error("No state update received")

    fun TestWebSocketClient.latestError(): ServerMessage.Error? =
        messages.filterIsInstance<ServerMessage.Error>().lastOrNull()

    fun TestWebSocketClient.allErrors(): List<ServerMessage.Error> =
        messages.filterIsInstance<ServerMessage.Error>()

    fun TestWebSocketClient.allStateUpdates(): List<ServerMessage.StateUpdate> =
        messages.filterIsInstance<ServerMessage.StateUpdate>()

    fun TestWebSocketClient.stateUpdateCount(): Int = stateUpdateCountFast()

    fun TestWebSocketClient.clearMessages() {
        messages.clear()
    }

    fun TestWebSocketClient.latestMulliganDecision(): ServerMessage.MulliganDecision? =
        messages.filterIsInstance<ServerMessage.MulliganDecision>().lastOrNull()

    fun TestWebSocketClient.latestChooseBottomCards(): ServerMessage.ChooseBottomCards? =
        messages.filterIsInstance<ServerMessage.ChooseBottomCards>().lastOrNull()

    fun TestWebSocketClient.mulliganComplete(): Boolean =
        messages.any { it is ServerMessage.MulliganComplete }

    suspend fun TestWebSocketClient.keepHandAndWait() {
        send(ClientMessage.KeepHand)
        eventually(5.seconds) {
            (messages.any { it is ServerMessage.MulliganComplete || it is ServerMessage.ChooseBottomCards }) shouldBe true
        }
    }

    suspend fun TestWebSocketClient.mulliganAndWait() {
        val countBefore = messages.filterIsInstance<ServerMessage.MulliganDecision>().size
        send(ClientMessage.Mulligan)
        eventually(5.seconds) {
            (messages.filterIsInstance<ServerMessage.MulliganDecision>().size > countBefore) shouldBe true
        }
    }

    suspend fun TestWebSocketClient.chooseBottomCardsAndWait(cardIds: List<EntityId>) {
        send(ClientMessage.ChooseBottomCards(cardIds))
        eventually(5.seconds) {
            messages.any { it is ServerMessage.MulliganComplete } shouldBe true
        }
    }

    suspend fun TestWebSocketClient.submitAndWait(action: GameAction): ClientGameState {
        val countBefore = stateUpdateCount()
        send(ClientMessage.SubmitAction(action))
        eventually(5.seconds) {
            (stateUpdateCount() > countBefore) shouldBe true
        }
        return requireLatestState()
    }

    suspend fun TestWebSocketClient.submitAndExpectError(action: GameAction): ServerMessage.Error {
        val errorCountBefore = allErrors().size
        send(ClientMessage.SubmitAction(action))
        eventually(5.seconds) {
            (allErrors().size > errorCountBefore) shouldBe true
        }
        return allErrors().last()
    }

    fun ClientGameState.player(playerId: EntityId): ClientPlayer =
        players.find { it.playerId == playerId }
            ?: error("Player $playerId not found in state")

    fun ClientGameState.hand(playerId: EntityId): ClientZone =
        zones.find { it.zoneId.type == ZoneType.HAND && it.zoneId.ownerId == playerId }
            ?: error("Hand zone for $playerId not found")

    fun ClientGameState.library(playerId: EntityId): ClientZone =
        zones.find { it.zoneId.type == ZoneType.LIBRARY && it.zoneId.ownerId == playerId }
            ?: error("Library zone for $playerId not found")

    fun ClientGameState.graveyard(playerId: EntityId): ClientZone =
        zones.find { it.zoneId.type == ZoneType.GRAVEYARD && it.zoneId.ownerId == playerId }
            ?: error("Graveyard zone for $playerId not found")

    fun ClientGameState.battlefield(): ClientZone =
        zones.find { it.zoneId == ZoneId.BATTLEFIELD } ?: error("Battlefield zone not found")

    fun ClientGameState.stack(): ClientZone =
        zones.find { it.zoneId == ZoneId.STACK } ?: error("Stack zone not found")

    /**
     * Sets up a game and completes the mulligan phase with both players keeping their hands.
     * This returns a GameContext ready for normal gameplay.
     *
     * @param skipMulligan If true, both players automatically keep their opening hands.
     *                     If false, returns after MulliganDecision is sent (for mulligan tests).
     */
    suspend fun setupGame(
        deck: Map<String, Int> = greenCreatures,
        player1Name: String = "Alice",
        player2Name: String = "Bob",
        skipMulligan: Boolean = true
    ): GameContext {
        val client1 = createClient()
        val client2 = createClient()

        val player1Id = client1.connectAs(player1Name)
        val player2Id = client2.connectAs(player2Name)

        client1.send(ClientMessage.CreateGame(deck))
        eventually(5.seconds) {
            client1.messages.any { it is ServerMessage.GameCreated } shouldBe true
        }

        val sessionId = client1.messages
            .filterIsInstance<ServerMessage.GameCreated>().first().sessionId

        client2.send(ClientMessage.JoinGame(sessionId, deck))

        eventually(5.seconds) {
            client1.messages.any { it is ServerMessage.GameStarted } shouldBe true
            client2.messages.any { it is ServerMessage.GameStarted } shouldBe true
        }

        // Wait for mulligan decisions
        eventually(5.seconds) {
            (client1.latestMulliganDecision() != null &&
                client2.latestMulliganDecision() != null) shouldBe true
        }

        val ctx = GameContext(
            sessionId = sessionId,
            player1 = PlayerContext(EntityId.of(player1Id), client1, player1Name),
            player2 = PlayerContext(EntityId.of(player2Id), client2, player2Name)
        )

        if (skipMulligan) {
            // Both players keep their hands
            client1.keepHandAndWait()
            client2.keepHandAndWait()

            // Wait for state updates after mulligan completes
            eventually(5.seconds) {
                (client1.latestState() != null && client2.latestState() != null) shouldBe true
            }
        }

        return ctx
    }

    fun GameContext.activePlayer(): PlayerContext {
        val state = player1.client.requireLatestState()
        return if (state.activePlayerId == player1.id) player1 else player2
    }

    fun GameContext.inactivePlayer(): PlayerContext {
        val state = player1.client.requireLatestState()
        return if (state.activePlayerId == player1.id) player2 else player1
    }

    fun GameContext.priorityPlayer(): PlayerContext {
        val state = player1.client.requireLatestState()
        return if (state.priorityPlayerId == player1.id) player1 else player2
    }

    fun GameContext.nonPriorityPlayer(): PlayerContext {
        val state = player1.client.requireLatestState()
        return if (state.priorityPlayerId == player1.id) player2 else player1
    }

    suspend fun GameContext.passBothPriorities(): ClientGameState {
        val priority = priorityPlayer()
        val nonPriority = nonPriorityPlayer()

        priority.client.submitAndWait(PassPriority(priority.id))
        return nonPriority.client.submitAndWait(PassPriority(nonPriority.id))
    }

    /**
     * Pass priority and wait for both players to receive the state update.
     * This ensures the loop condition is always checked with updated state.
     */
    suspend fun GameContext.passPriorityConsistent(): ClientGameState {
        val countBefore1 = player1.client.stateUpdateCount()
        val countBefore2 = player2.client.stateUpdateCount()
        val errorCountBefore1 = player1.client.allErrors().size
        val errorCountBefore2 = player2.client.allErrors().size

        // Determine priority player from player1's state and submit through their client
        val state = player1.client.requireLatestState()
        val priority = if (state.priorityPlayerId == player1.id) player1 else player2


        priority.client.send(ClientMessage.SubmitAction(PassPriority(priority.id)))

        // Always wait for PLAYER1 to receive the state update since we use player1's state
        // for the loop condition in advanceToNextTurn
        eventually(10.seconds) {
            // Check for errors first
            val newErrors1 = player1.client.allErrors().size > errorCountBefore1
            val newErrors2 = player2.client.allErrors().size > errorCountBefore2
            if (newErrors1 || newErrors2) {
                val err1 = player1.client.allErrors().lastOrNull()
                val err2 = player2.client.allErrors().lastOrNull()
                error("Received error during passPriorityConsistent: p1=$err1, p2=$err2, state=T${state.turnNumber}/${state.currentPhase}/${state.currentStep}, priority=${state.priorityPlayerId}")
            }

            // Wait for player1 to receive the state update (player1 is used for loop conditions)
            (player1.client.stateUpdateCount() > countBefore1) shouldBe true
        }

        return player1.client.requireLatestState()
    }

    suspend fun GameContext.advanceToPhase(targetPhase: Phase, maxPasses: Int = 100): ClientGameState {
        repeat(maxPasses) {
            val state = player1.client.requireLatestState()
            if (state.currentPhase == targetPhase) return state
            if (state.isGameOver) error("Game ended while advancing to phase $targetPhase")

            passPriorityConsistent()
        }
        error("Failed to reach phase $targetPhase after $maxPasses priority passes")
    }

    suspend fun GameContext.advanceToNextTurn(maxPasses: Int = 100): ClientGameState {
        val currentTurn = player1.client.requireLatestState().turnNumber
        repeat(maxPasses) {
            val state = player1.client.requireLatestState()
            // Wait until turn advances AND we reach precombat main phase (where lands can be played)
            if (state.turnNumber > currentTurn && state.currentPhase == Phase.PRECOMBAT_MAIN) {
                return state
            }
            if (state.isGameOver) error("Game ended while advancing turn")

            passPriorityConsistent()
        }
        error("Failed to advance to next turn after $maxPasses priority passes")
    }

    suspend fun GameContext.playLandFromHand(player: PlayerContext): EntityId {
        val state = player.client.requireLatestState()
        val hand = state.hand(player.id)

        require(hand.cardIds.isNotEmpty()) { "No cards in hand to play" }
        require(state.priorityPlayerId == player.id) { "Player does not have priority" }

        val landId = hand.cardIds.first()
        player.client.submitAndWait(PlayLand(cardId = landId, playerId = player.id))
        return landId
    }

    suspend fun GameContext.tapLandForMana(player: PlayerContext, landId: EntityId) {
        player.client.submitAndWait(
            ActivateManaAbility(
                sourceEntityId = landId,
                abilityIndex = 0,
                playerId = player.id
            )
        )
    }

    // =========================================================================
    // Connection and Game Setup Tests
    // =========================================================================

    context("Connection and Game Setup") {

        test("player connects and receives player ID") {
            val client = createClient()
            val playerId = client.connectAs("TestPlayer")

            playerId shouldNotBe null
            playerId.isNotEmpty() shouldBe true

            // Verify the Connected message was received
            val connected = client.messages.filterIsInstance<ServerMessage.Connected>().first()
            connected.playerId shouldBe playerId
        }

        test("two players can create and join a game") {
            val ctx = setupGame()

            ctx.sessionId shouldNotBe null
            ctx.player1.id shouldNotBe ctx.player2.id

            // Verify GameStarted messages
            val started1 = ctx.player1.client.messages.filterIsInstance<ServerMessage.GameStarted>().first()
            val started2 = ctx.player2.client.messages.filterIsInstance<ServerMessage.GameStarted>().first()

            started1.opponentName shouldBe ctx.player2.name
            started2.opponentName shouldBe ctx.player1.name
        }

        test("initial game state has correct values") {
            val ctx = setupGame(monoGreenLands)
            val state = ctx.player1.client.requireLatestState()

            withClue("Both players should start at 20 life") {
                state.player(ctx.player1.id).life shouldBe 20
                state.player(ctx.player2.id).life shouldBe 20
            }

            withClue("Each player draws 7 cards for opening hand") {
                state.player(ctx.player1.id).handSize shouldBe 7
                state.player(ctx.player2.id).handSize shouldBe 7
            }

            withClue("Libraries should have 33 cards (40 - 7)") {
                state.player(ctx.player1.id).librarySize shouldBe 33
                state.player(ctx.player2.id).librarySize shouldBe 33
            }

            withClue("Graveyards should be empty") {
                state.player(ctx.player1.id).graveyardSize shouldBe 0
                state.player(ctx.player2.id).graveyardSize shouldBe 0
            }

            withClue("Game should start on turn 1") {
                state.turnNumber shouldBe 1
            }

            withClue("Game should not be over") {
                state.isGameOver shouldBe false
                state.winnerId shouldBe null
            }

            withClue("Battlefield should be empty") {
                state.battlefield().size shouldBe 0
            }

            withClue("Stack should be empty") {
                state.stack().size shouldBe 0
            }
        }

        test("joining non-existent game returns GAME_NOT_FOUND error") {
            val client = createClient()
            client.connectAs("Alice")

            client.send(ClientMessage.JoinGame("non-existent-session", monoGreenLands))

            eventually(5.seconds) {
                client.allErrors().isNotEmpty() shouldBe true
            }

            val error = client.latestError()!!
            error.code shouldBe ErrorCode.GAME_NOT_FOUND
            error.message.isNotEmpty() shouldBe true
        }

        test("creating game with empty deck returns INVALID_DECK error") {
            val client = createClient()
            client.connectAs("Alice")

            client.send(ClientMessage.CreateGame(emptyMap()))

            eventually(5.seconds) {
                client.allErrors().isNotEmpty() shouldBe true
            }

            client.latestError()?.code shouldBe ErrorCode.INVALID_DECK
        }

        test("connecting twice returns ALREADY_CONNECTED error") {
            val client = createClient()
            client.connectAs("Alice")

            // Try to connect again
            client.send(ClientMessage.Connect("Alice2"))

            eventually(5.seconds) {
                client.allErrors().isNotEmpty() shouldBe true
            }

            client.latestError()?.code shouldBe ErrorCode.ALREADY_CONNECTED
        }
    }

    // =========================================================================
    // Mulligan Tests
    // =========================================================================

    context("Mulligan") {

        test("players receive mulligan decision after game starts") {
            val ctx = setupGame(monoGreenLands, skipMulligan = false)

            withClue("Player 1 should receive MulliganDecision") {
                val decision1 = ctx.player1.client.latestMulliganDecision()
                decision1 shouldNotBe null
                decision1!!.hand shouldHaveSize 7
                decision1.mulliganCount shouldBe 0
                decision1.cardsToPutOnBottom shouldBe 0
            }

            withClue("Player 2 should receive MulliganDecision") {
                val decision2 = ctx.player2.client.latestMulliganDecision()
                decision2 shouldNotBe null
                decision2!!.hand shouldHaveSize 7
                decision2.mulliganCount shouldBe 0
                decision2.cardsToPutOnBottom shouldBe 0
            }
        }

        test("keeping opening hand with no mulligans completes immediately") {
            val ctx = setupGame(monoGreenLands, skipMulligan = false)

            ctx.player1.client.keepHandAndWait()

            withClue("Should receive MulliganComplete") {
                ctx.player1.client.mulliganComplete() shouldBe true
            }

            val mulliganComplete = ctx.player1.client.messages
                .filterIsInstance<ServerMessage.MulliganComplete>().first()
            mulliganComplete.finalHandSize shouldBe 7
        }

        test("mulligan gives new hand with same size") {
            val ctx = setupGame(monoGreenLands, skipMulligan = false)

            val originalHand = ctx.player1.client.latestMulliganDecision()!!.hand

            ctx.player1.client.mulliganAndWait()

            val newDecision = ctx.player1.client.latestMulliganDecision()!!

            withClue("Should still have 7 cards in hand after mulligan") {
                newDecision.hand shouldHaveSize 7
            }

            withClue("Mulligan count should be 1") {
                newDecision.mulliganCount shouldBe 1
            }

            withClue("Cards to put on bottom should match mulligan count") {
                newDecision.cardsToPutOnBottom shouldBe 1
            }

            withClue("New hand should be different from original") {
                // At least one card should be different (with high probability)
                (newDecision.hand != originalHand) shouldBe true
            }
        }

        test("keeping after mulligan requires choosing cards to put on bottom") {
            val ctx = setupGame(monoGreenLands, skipMulligan = false)

            // Mulligan once
            ctx.player1.client.mulliganAndWait()

            // Keep the hand
            ctx.player1.client.send(ClientMessage.KeepHand)

            eventually(5.seconds) {
                (ctx.player1.client.latestChooseBottomCards() != null) shouldBe true
            }

            val chooseMsg = ctx.player1.client.latestChooseBottomCards()!!
            chooseMsg.hand shouldHaveSize 7
            chooseMsg.cardsToPutOnBottom shouldBe 1
        }

        test("choosing cards to put on bottom completes mulligan") {
            val ctx = setupGame(monoGreenLands, skipMulligan = false)

            // Mulligan once
            ctx.player1.client.mulliganAndWait()

            // Keep the hand
            ctx.player1.client.send(ClientMessage.KeepHand)

            eventually(5.seconds) {
                (ctx.player1.client.latestChooseBottomCards() != null) shouldBe true
            }

            val chooseMsg = ctx.player1.client.latestChooseBottomCards()!!
            val cardToBottom = chooseMsg.hand.first()

            ctx.player1.client.chooseBottomCardsAndWait(listOf(cardToBottom))

            withClue("Should receive MulliganComplete") {
                ctx.player1.client.mulliganComplete() shouldBe true
            }

            val mulliganComplete = ctx.player1.client.messages
                .filterIsInstance<ServerMessage.MulliganComplete>().last()
            mulliganComplete.finalHandSize shouldBe 6
        }

        test("multiple mulligans increase cards to put on bottom") {
            val ctx = setupGame(monoGreenLands, skipMulligan = false)

            // Mulligan twice
            ctx.player1.client.mulliganAndWait()
            ctx.player1.client.mulliganAndWait()

            val decision = ctx.player1.client.latestMulliganDecision()!!

            decision.mulliganCount shouldBe 2
            decision.cardsToPutOnBottom shouldBe 2
        }

        test("game state update sent only after both players complete mulligan") {
            val ctx = setupGame(monoGreenLands, skipMulligan = false)

            // Player 1 keeps immediately
            ctx.player1.client.keepHandAndWait()

            withClue("Player 1 should not receive state update yet") {
                ctx.player1.client.latestState() shouldBe null
            }

            // Player 2 keeps
            ctx.player2.client.keepHandAndWait()

            // Now both should receive state updates
            eventually(5.seconds) {
                (ctx.player1.client.latestState() != null &&
                    ctx.player2.client.latestState() != null) shouldBe true
            }

            ctx.player1.client.latestState() shouldNotBe null
            ctx.player2.client.latestState() shouldNotBe null
        }

        test("cannot submit game action during mulligan phase") {
            val ctx = setupGame(monoGreenLands, skipMulligan = false)

            // Try to submit an action before completing mulligan
            ctx.player1.client.send(ClientMessage.SubmitAction(PassPriority(ctx.player1.id)))

            eventually(5.seconds) {
                ctx.player1.client.allErrors().isNotEmpty() shouldBe true
            }

            ctx.player1.client.latestError()?.code shouldBe ErrorCode.INVALID_ACTION
        }

        test("choosing wrong number of bottom cards returns error") {
            val ctx = setupGame(monoGreenLands, skipMulligan = false)

            // Mulligan once (need to put 1 card on bottom)
            ctx.player1.client.mulliganAndWait()
            ctx.player1.client.send(ClientMessage.KeepHand)

            eventually(5.seconds) {
                (ctx.player1.client.latestChooseBottomCards() != null) shouldBe true
            }

            val chooseMsg = ctx.player1.client.latestChooseBottomCards()!!

            // Try to put 2 cards on bottom (should fail)
            val twoCards = chooseMsg.hand.take(2)
            ctx.player1.client.send(ClientMessage.ChooseBottomCards(twoCards))

            eventually(5.seconds) {
                ctx.player1.client.allErrors().isNotEmpty() shouldBe true
            }

            ctx.player1.client.latestError()?.code shouldBe ErrorCode.INVALID_ACTION
        }

        test("hand size correct after mulligan with bottom cards") {
            val ctx = setupGame(monoGreenLands, skipMulligan = false)

            // Mulligan twice
            ctx.player1.client.mulliganAndWait()
            ctx.player1.client.mulliganAndWait()

            // Keep the hand
            ctx.player1.client.send(ClientMessage.KeepHand)

            eventually(5.seconds) {
                (ctx.player1.client.latestChooseBottomCards() != null) shouldBe true
            }

            val chooseMsg = ctx.player1.client.latestChooseBottomCards()!!
            val cardsToBottom = chooseMsg.hand.take(2)  // Put 2 cards on bottom

            ctx.player1.client.chooseBottomCardsAndWait(cardsToBottom)

            // Complete player 2's mulligan
            ctx.player2.client.keepHandAndWait()

            // Wait for state
            eventually(5.seconds) {
                (ctx.player1.client.latestState() != null) shouldBe true
            }

            val state = ctx.player1.client.requireLatestState()

            withClue("After 2 mulligans, hand should have 5 cards (7 - 2)") {
                state.player(ctx.player1.id).handSize shouldBe 5
            }

            withClue("Player 2 who kept should have 7 cards") {
                state.player(ctx.player2.id).handSize shouldBe 7
            }
        }
    }

    // =========================================================================
    // State Masking and Information Hiding Tests
    // =========================================================================

    context("State Masking") {

        test("player sees their own hand contents with full entity IDs") {
            val ctx = setupGame(monoGreenLands)
            val state = ctx.player1.client.requireLatestState()

            val ownHand = state.hand(ctx.player1.id)

            withClue("Own hand should be visible") {
                ownHand.isVisible shouldBe true
            }
            withClue("Own hand should contain 7 card entity IDs") {
                ownHand.cardIds shouldHaveSize 7
            }
            withClue("Hand size should match entity count") {
                ownHand.size shouldBe ownHand.cardIds.size
            }
        }

        test("player cannot see opponent hand contents but sees card count") {
            val ctx = setupGame(monoGreenLands)
            val state = ctx.player1.client.requireLatestState()

            val opponentHand = state.hand(ctx.player2.id)

            withClue("Opponent hand should be marked not visible") {
                opponentHand.isVisible shouldBe false
            }
            withClue("Opponent hand entity IDs should be hidden") {
                opponentHand.cardIds.shouldBeEmpty()
            }
            withClue("Opponent hand card count should still be visible") {
                opponentHand.size shouldBe 7
            }
        }

        test("own library is hidden (order unknown)") {
            val ctx = setupGame(monoGreenLands)
            val state = ctx.player1.client.requireLatestState()

            val ownLibrary = state.library(ctx.player1.id)

            ownLibrary.isVisible shouldBe false
            ownLibrary.cardIds.shouldBeEmpty()
            ownLibrary.size shouldBe 33
        }

        test("opponent library is hidden") {
            val ctx = setupGame(monoGreenLands)
            val state = ctx.player1.client.requireLatestState()

            val opponentLibrary = state.library(ctx.player2.id)

            opponentLibrary.isVisible shouldBe false
            opponentLibrary.cardIds.shouldBeEmpty()
            opponentLibrary.size shouldBe 33
        }

        test("battlefield is fully visible to both players") {
            val ctx = setupGame(monoGreenLands)

            // Play a land to put something on battlefield
            val active = ctx.activePlayer()
            val landId = ctx.playLandFromHand(active)

            val state1 = ctx.player1.client.requireLatestState()
            val state2 = ctx.player2.client.requireLatestState()

            withClue("Battlefield visible to player 1") {
                state1.battlefield().isVisible shouldBe true
                state1.battlefield().cardIds shouldContain landId
            }

            withClue("Battlefield visible to player 2") {
                state2.battlefield().isVisible shouldBe true
                state2.battlefield().cardIds shouldContain landId
            }
        }

        test("graveyards are visible to both players") {
            val ctx = setupGame()
            val state = ctx.player1.client.requireLatestState()

            state.graveyard(ctx.player1.id).isVisible shouldBe true
            state.graveyard(ctx.player2.id).isVisible shouldBe true
        }

        test("stack is visible to both players") {
            val ctx = setupGame()

            val state1 = ctx.player1.client.requireLatestState()
            val state2 = ctx.player2.client.requireLatestState()

            state1.stack().isVisible shouldBe true
            state2.stack().isVisible shouldBe true
        }

        test("each player receives state with their viewingPlayerId") {
            val ctx = setupGame()

            val state1 = ctx.player1.client.requireLatestState()
            val state2 = ctx.player2.client.requireLatestState()

            state1.viewingPlayerId shouldBe ctx.player1.id
            state2.viewingPlayerId shouldBe ctx.player2.id
        }
    }

    // =========================================================================
    // Land Playing Tests
    // =========================================================================

    context("Playing Lands") {

        test("active player can play a land during main phase") {
            val ctx = setupGame(monoGreenLands)

            val active = ctx.activePlayer()
            val initialState = active.client.requireLatestState()
            val initialHandSize = initialState.hand(active.id).size
            val initialBattlefieldSize = initialState.battlefield().size

            val landId = ctx.playLandFromHand(active)

            val newState = active.client.requireLatestState()

            withClue("Land should be on battlefield") {
                newState.battlefield().cardIds shouldContain landId
                newState.battlefield().size shouldBe initialBattlefieldSize + 1
            }

            withClue("Hand should have one fewer card") {
                newState.hand(active.id).size shouldBe initialHandSize - 1
            }

            withClue("Lands played counter should increment") {
                newState.player(active.id).landsPlayedThisTurn shouldBe 1
            }
        }

        test("cannot play more than one land per turn") {
            val ctx = setupGame(monoGreenLands)

            val active = ctx.activePlayer()
            ctx.playLandFromHand(active)

            // Get a second land from hand
            val state = active.client.requireLatestState()
            val secondLand = state.hand(active.id).cardIds.firstOrNull()

            if (secondLand != null) {
                val error = active.client.submitAndExpectError(
                    PlayLand(cardId = secondLand, playerId = active.id)
                )

                error.code shouldBe ErrorCode.INVALID_ACTION
            }
        }

        test("land play counter resets on new turn") {
            val ctx = setupGame(monoGreenLands)

            // Player 1's turn - play a land
            val active1 = ctx.activePlayer()
            ctx.playLandFromHand(active1)

            var state = active1.client.requireLatestState()
            state.player(active1.id).landsPlayedThisTurn shouldBe 1

            // Advance to next turn
            ctx.advanceToNextTurn()

            state = ctx.player1.client.requireLatestState()

            // New active player should have 0 lands played
            val newActive = ctx.activePlayer()
            state.player(newActive.id).landsPlayedThisTurn shouldBe 0
        }

        test("cannot play land without priority") {
            val ctx = setupGame(monoGreenLands)

            val nonPriority = ctx.nonPriorityPlayer()
            val state = nonPriority.client.requireLatestState()
            val hand = state.hand(nonPriority.id)

            if (hand.cardIds.isNotEmpty()) {
                val error = nonPriority.client.submitAndExpectError(
                    PlayLand(cardId = hand.cardIds.first(), playerId = nonPriority.id)
                )

                error.code shouldBe ErrorCode.INVALID_ACTION
            }
        }
    }

    // =========================================================================
    // Mana Ability Tests
    // =========================================================================

    context("Mana Abilities") {

        test("can tap land for mana after playing it") {
            val ctx = setupGame(monoGreenLands)

            val active = ctx.activePlayer()
            val landId = ctx.playLandFromHand(active)

            // Tap for mana - should succeed without error
            ctx.tapLandForMana(active, landId)

            // Land should still be on battlefield
            val state = active.client.requireLatestState()
            state.battlefield().cardIds shouldContain landId
        }

        test("cannot tap already tapped land") {
            val ctx = setupGame(monoGreenLands)

            val active = ctx.activePlayer()
            val landId = ctx.playLandFromHand(active)

            // Tap once - should succeed
            ctx.tapLandForMana(active, landId)

            // Tap again - should fail
            val error = active.client.submitAndExpectError(
                ActivateManaAbility(
                    sourceEntityId = landId,
                    abilityIndex = 0,
                    playerId = active.id
                )
            )

            error.code shouldBe ErrorCode.INVALID_ACTION
        }
    }

    // =========================================================================
    // Priority and Phase Tests
    // =========================================================================

    context("Priority System") {

        test("active player has priority at start of each phase") {
            val ctx = setupGame()
            val state = ctx.player1.client.requireLatestState()

            state.priorityPlayerId shouldBe state.activePlayerId
        }

        test("passing priority gives it to opponent") {
            val ctx = setupGame()

            val priority = ctx.priorityPlayer()
            val nonPriority = ctx.nonPriorityPlayer()

            val newState = priority.client.submitAndWait(PassPriority(priority.id))

            newState.priorityPlayerId shouldBe nonPriority.id
        }

        test("both players passing with empty stack advances phase") {
            val ctx = setupGame()

            val initialState = ctx.player1.client.requireLatestState()
            val initialPhase = initialState.currentPhase
            val initialStep = initialState.currentStep

            ctx.passBothPriorities()

            val newState = ctx.player1.client.requireLatestState()
            val phaseChanged = newState.currentPhase != initialPhase
            val stepChanged = newState.currentStep != initialStep

            (phaseChanged || stepChanged) shouldBe true
        }

        test("player without priority cannot pass") {
            val ctx = setupGame()

            val nonPriority = ctx.nonPriorityPlayer()

            val error = nonPriority.client.submitAndExpectError(
                PassPriority(nonPriority.id)
            )

            error.code shouldBe ErrorCode.INVALID_ACTION
        }

        test("can advance to combat phase") {
            val ctx = setupGame()

            val state = ctx.advanceToPhase(Phase.COMBAT)

            state.currentPhase shouldBe Phase.COMBAT
        }

        test("can advance to postcombat main phase") {
            val ctx = setupGame()

            val state = ctx.advanceToPhase(Phase.POSTCOMBAT_MAIN)

            state.currentPhase shouldBe Phase.POSTCOMBAT_MAIN
        }
    }

    // =========================================================================
    // Combat Tests
    // =========================================================================

    context("Combat") {

        test("dealing damage reduces life total") {
            val ctx = setupGame()

            val target = ctx.player2
            val initialLife = ctx.player1.client.requireLatestState().player(target.id).life

            val newState = ctx.player1.client.submitAndWait(
                DealDamageToPlayer(targetPlayerId = target.id, amount = 5)
            )

            newState.player(target.id).life shouldBe initialLife - 5
        }

        test("damage events are included in state update") {
            val ctx = setupGame()

            ctx.player1.client.clearMessages()

            ctx.player1.client.submitAndWait(
                DealDamageToPlayer(targetPlayerId = ctx.player2.id, amount = 3)
            )

            val lastUpdate = ctx.player1.client.allStateUpdates().last()
            lastUpdate.events.shouldNotBeEmpty()
        }

        test("lethal damage triggers game over") {
            val ctx = setupGame()

            ctx.player1.client.submitAndWait(
                DealDamageToPlayer(targetPlayerId = ctx.player2.id, amount = 20)
            )

            eventually(5.seconds) {
                val state = ctx.player1.client.latestState()
                (state?.isGameOver == true || state?.player(ctx.player2.id)?.life!! <= 0) shouldBe true
            }
        }

        test("damage below lethal does not end game") {
            val ctx = setupGame()

            ctx.player1.client.submitAndWait(
                DealDamageToPlayer(targetPlayerId = ctx.player2.id, amount = 10)
            )

            val state = ctx.player1.client.requireLatestState()
            state.isGameOver shouldBe false
            state.player(ctx.player2.id).life shouldBe 10
        }
    }

    // =========================================================================
    // Game End Tests
    // =========================================================================

    context("Game End Conditions") {

        test("conceding immediately ends game with opponent as winner") {
            val ctx = setupGame()

            ctx.player1.client.send(ClientMessage.Concede)

            eventually(5.seconds) {
                ctx.player1.client.messages.any { it is ServerMessage.GameOver } shouldBe true
            }

            val gameOver = ctx.player1.client.messages.filterIsInstance<ServerMessage.GameOver>().first()

            gameOver.winnerId shouldBe ctx.player2.id
            gameOver.reason shouldBe GameOverReason.CONCESSION
        }

        test("both players receive identical game over messages") {
            val ctx = setupGame()

            ctx.player1.client.send(ClientMessage.Concede)

            eventually(5.seconds) {
                ctx.player1.client.messages.any { it is ServerMessage.GameOver } shouldBe true
                ctx.player2.client.messages.any { it is ServerMessage.GameOver } shouldBe true
            }

            val gameOver1 = ctx.player1.client.messages.filterIsInstance<ServerMessage.GameOver>().first()
            val gameOver2 = ctx.player2.client.messages.filterIsInstance<ServerMessage.GameOver>().first()

            gameOver1.winnerId shouldBe gameOver2.winnerId
            gameOver1.reason shouldBe gameOver2.reason
        }

        test("cannot take actions after game ends") {
            val ctx = setupGame()

            ctx.player1.client.send(ClientMessage.Concede)

            eventually(5.seconds) {
                ctx.player1.client.messages.any { it is ServerMessage.GameOver } shouldBe true
            }

            // Try to take an action after game over
            ctx.player2.client.send(ClientMessage.SubmitAction(PassPriority(ctx.player2.id)))

            eventually(5.seconds) {
                ctx.player2.client.allErrors().isNotEmpty() shouldBe true
            }
        }

        test("disconnection causes forfeit") {
            val client1 = createClient()
            val client2 = createClient()

            client1.connectAs("Alice")  // player1Id not needed
            val player2Id = client2.connectAs("Bob")

            client1.send(ClientMessage.CreateGame(monoGreenLands))
            eventually(5.seconds) {
                client1.messages.any { it is ServerMessage.GameCreated } shouldBe true
            }

            val sessionId = client1.messages.filterIsInstance<ServerMessage.GameCreated>().first().sessionId
            client2.send(ClientMessage.JoinGame(sessionId, monoGreenLands))

            eventually(5.seconds) {
                client2.messages.any { it is ServerMessage.GameStarted } shouldBe true
            }

            // Player 1 disconnects
            client1.close()

            // Player 2 should receive game over
            eventually(10.seconds) {
                client2.messages.any { it is ServerMessage.GameOver } shouldBe true
            }

            val gameOver = client2.messages.filterIsInstance<ServerMessage.GameOver>().first()
            gameOver.winnerId shouldBe EntityId.of(player2Id)
            gameOver.reason shouldBe GameOverReason.DISCONNECTION
        }
    }

    // =========================================================================
    // Error Handling Tests
    // =========================================================================

    context("Error Handling") {

        test("action before connecting returns NOT_CONNECTED") {
            val client = createClient()
            client.connect(wsUrl())

            // Don't send Connect message, just try to create game
            client.send(ClientMessage.CreateGame(monoGreenLands))

            eventually(5.seconds) {
                client.allErrors().isNotEmpty() shouldBe true
            }

            client.latestError()?.code shouldBe ErrorCode.NOT_CONNECTED
        }

        test("action without being in game returns GAME_NOT_FOUND") {
            val client = createClient()
            client.connectAs("Alice")

            client.send(ClientMessage.SubmitAction(PassPriority(EntityId.generate())))

            eventually(5.seconds) {
                client.allErrors().isNotEmpty() shouldBe true
            }

            client.latestError()?.code shouldBe ErrorCode.GAME_NOT_FOUND
        }

        test("playing non-existent card returns INVALID_ACTION with message") {
            val ctx = setupGame()

            val fakeCardId = EntityId.generate()
            val active = ctx.activePlayer()

            val error = active.client.submitAndExpectError(
                PlayLand(cardId = fakeCardId, playerId = active.id)
            )

            error.code shouldBe ErrorCode.INVALID_ACTION
            error.message.isNotEmpty() shouldBe true
        }

        test("action with wrong player ID returns error") {
            val ctx = setupGame()

            val active = ctx.activePlayer()
            val inactive = ctx.inactivePlayer()

            // Active player tries to pass priority for the inactive player
            val error = active.client.submitAndExpectError(
                PassPriority(inactive.id)
            )

            error.code shouldBe ErrorCode.INVALID_ACTION
        }
    }

    // =========================================================================
    // Multi-Turn and Long Game Tests
    // =========================================================================

    context("Multi-Turn Gameplay") {

        test("turn counter increments correctly") {
            val ctx = setupGame(monoGreenLands)

            val initialTurn = ctx.player1.client.requireLatestState().turnNumber
            initialTurn shouldBe 1

            ctx.advanceToNextTurn()

            val turn2 = ctx.player1.client.requireLatestState().turnNumber
            turn2 shouldBe 2

            ctx.advanceToNextTurn()

            val turn3 = ctx.player1.client.requireLatestState().turnNumber
            turn3 shouldBe 3
        }

        test("active player alternates each turn") {
            val ctx = setupGame(monoGreenLands)

            val turn1Active = ctx.activePlayer().id

            ctx.advanceToNextTurn()
            val turn2Active = ctx.activePlayer().id

            ctx.advanceToNextTurn()
            val turn3Active = ctx.activePlayer().id

            turn1Active shouldNotBe turn2Active
            turn3Active shouldBe turn1Active
        }

        test("players can play lands over multiple turns") {
            val ctx = setupGame(monoGreenLands)

            // Turn 1: Player 1 plays land
            val active1 = ctx.activePlayer()
            ctx.playLandFromHand(active1)

            var battlefield = ctx.player1.client.requireLatestState().battlefield()
            battlefield.size shouldBe 1

            // Advance to turn 2
            ctx.advanceToNextTurn()

            // Turn 2: Player 2 plays land
            val active2 = ctx.activePlayer()
            ctx.playLandFromHand(active2)

            battlefield = ctx.player1.client.requireLatestState().battlefield()
            battlefield.size shouldBe 2

            // Advance to turn 3
            ctx.advanceToNextTurn()

            // Turn 3: Player 1 plays another land
            val active3 = ctx.activePlayer()
            ctx.playLandFromHand(active3)

            battlefield = ctx.player1.client.requireLatestState().battlefield()
            battlefield.size shouldBe 3
        }

        test("hand sizes change as expected during game") {
            val ctx = setupGame(monoGreenLands)

            // Initial hand size: 7
            val initialHand1 = ctx.player1.client.requireLatestState().player(ctx.player1.id).handSize
            initialHand1 shouldBe 7

            // Player 1 plays a land (assuming player 1 is active)
            if (ctx.activePlayer().id == ctx.player1.id) {
                ctx.playLandFromHand(ctx.player1)

                val afterPlay = ctx.player1.client.requireLatestState().player(ctx.player1.id).handSize
                afterPlay shouldBe 6
            }
        }
    }

    // =========================================================================
    // Concurrent Access Tests
    // =========================================================================

    context("Concurrent Access") {

        test("multiple games can run on the same server") {
            // Game 1
            val ctx1 = setupGame(player1Name = "Alice1", player2Name = "Bob1")

            // Game 2
            val ctx2 = setupGame(player1Name = "Alice2", player2Name = "Bob2")

            // Both games should be independent
            ctx1.sessionId shouldNotBe ctx2.sessionId

            // Actions in game 1 shouldn't affect game 2
            val game1InitialLife = ctx1.player1.client.requireLatestState().player(ctx1.player2.id).life
            val game2InitialLife = ctx2.player1.client.requireLatestState().player(ctx2.player2.id).life

            ctx1.player1.client.submitAndWait(
                DealDamageToPlayer(ctx1.player2.id, 5)
            )

            val game1Life = ctx1.player1.client.requireLatestState().player(ctx1.player2.id).life
            val game2Life = ctx2.player1.client.requireLatestState().player(ctx2.player2.id).life

            game1Life shouldBe game1InitialLife - 5
            game2Life shouldBe game2InitialLife  // Unchanged
        }
    }
})

// =========================================================================
// Test Support Classes
// =========================================================================

data class GameContext(
    val sessionId: String,
    val player1: PlayerContext,
    val player2: PlayerContext
)

data class PlayerContext(
    val id: EntityId,
    val client: TestWebSocketClient,
    val name: String
)

class TestWebSocketClient(private val json: Json, private val container: jakarta.websocket.WebSocketContainer) {
    private var session: WebSocketSession? = null
    val messages = CopyOnWriteArrayList<ServerMessage>()
    private val connectLatch = CountDownLatch(1)
    private val closed = AtomicBoolean(false)
    private val stateUpdateCounter = java.util.concurrent.atomic.AtomicInteger(0)

    val isOpen: Boolean get() = session?.isOpen == true && !closed.get()

    fun stateUpdateCountFast(): Int = stateUpdateCounter.get()

    suspend fun connect(url: String) {
        withContext(Dispatchers.IO) {
            val client = StandardWebSocketClient(container)
            session = client.execute(
                object : TextWebSocketHandler() {
                    override fun afterConnectionEstablished(session: WebSocketSession) {
                        connectLatch.countDown()
                    }

                    override fun handleTextMessage(session: WebSocketSession, message: TextMessage) {
                        if (closed.get()) return
                        val serverMessage = json.decodeFromString<ServerMessage>(message.payload)
                        messages.add(serverMessage)
                        // Increment counter for fast checking
                        if (serverMessage is ServerMessage.StateUpdate) {
                            stateUpdateCounter.incrementAndGet()
                        }
                    }

                    override fun afterConnectionClosed(session: WebSocketSession, status: CloseStatus) {
                        closed.set(true)
                    }

                    override fun handleTransportError(session: WebSocketSession, exception: Throwable) {
                        closed.set(true)
                    }
                },
                WebSocketHttpHeaders(),
                URI.create(url)
            ).get(10, TimeUnit.SECONDS)

            if (!connectLatch.await(10, TimeUnit.SECONDS)) {
                error("WebSocket connection timeout")
            }
        }
    }

    fun send(message: ClientMessage) {
        check(!closed.get()) { "Cannot send on closed connection" }
        check(session?.isOpen == true) { "WebSocket session is not open" }
        session?.sendMessage(TextMessage(json.encodeToString(message)))
    }

    fun close() {
        if (closed.compareAndSet(false, true)) {
            runCatching { session?.close() }
        }
    }
}
