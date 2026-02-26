package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.registry.CardRegistry
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
import com.wingedsheep.gameserver.dto.ClientCard
import com.wingedsheep.gameserver.dto.ClientGameState
import com.wingedsheep.gameserver.protocol.ClientMessage
import com.wingedsheep.gameserver.protocol.LegalActionInfo
import com.wingedsheep.gameserver.protocol.ServerMessage
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.core.spec.style.FunSpec
import io.kotest.core.test.TestCase
import io.kotest.core.test.TestResult
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import jakarta.websocket.WebSocketContainer
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
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import kotlin.time.Duration.Companion.seconds
import org.apache.tomcat.websocket.WsWebSocketContainer

/**
 * Base class for protocol-level integration tests.
 *
 * Provides:
 * - WebSocket client infrastructure
 * - Scenario builders for setting up specific game states
 * - Helper methods for common test operations
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
abstract class ProtocolTestBase : FunSpec() {

    @LocalServerPort
    protected var port: Int = 0

    protected val cardRegistry = CardRegistry()

    protected val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        classDiscriminator = "type"
        serializersModule = engineSerializersModule
    }

    protected val activeClients = mutableListOf<TestClient>()

    override suspend fun afterTest(testCase: TestCase, result: TestResult) {
        activeClients.forEach { it.close() }
        activeClients.clear()
        super.afterTest(testCase, result)
    }

    protected fun wsUrl(): String = "ws://localhost:$port/game"

    private fun createWsContainer() = WsWebSocketContainer().apply {
        defaultMaxSessionIdleTimeout = 300_000L
        defaultMaxTextMessageBufferSize = 1024 * 1024
        defaultMaxBinaryMessageBufferSize = 1024 * 1024
    }

    protected fun createClient(): TestClient {
        val client = TestClient(json, createWsContainer(), wsUrl())
        activeClients.add(client)
        return client
    }

    // =========================================================================
    // Scenario Builder
    // =========================================================================

    /**
     * Builder for constructing test scenarios with specific game states.
     */
    inner class ScenarioBuilder {
        private val entityIdCounter = AtomicLong(1000)
        private var state = GameState()

        private var player1Id: EntityId = EntityId.of("player-1")
        private var player2Id: EntityId = EntityId.of("player-2")
        private var player1Name: String = "Player1"
        private var player2Name: String = "Player2"

        /**
         * Initialize the scenario with two players.
         */
        fun withPlayers(
            p1Name: String = "Player1",
            p2Name: String = "Player2"
        ): ScenarioBuilder {
            player1Name = p1Name
            player2Name = p2Name

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
                .withEntity(player1Id, p1Container)
                .withEntity(player2Id, p2Container)
                .copy(
                    turnOrder = listOf(player1Id, player2Id),
                    activePlayerId = player1Id,
                    priorityPlayerId = player1Id,
                    phase = Phase.PRECOMBAT_MAIN,
                    step = Step.PRECOMBAT_MAIN,
                    turnNumber = 1
                )

            // Initialize empty zones for both players
            for (playerId in listOf(player1Id, player2Id)) {
                for (zoneType in listOf(Zone.HAND, Zone.LIBRARY, Zone.GRAVEYARD, Zone.BATTLEFIELD)) {
                    val zoneKey = ZoneKey(playerId, zoneType)
                    state = state.copy(zones = state.zones + (zoneKey to emptyList()))
                }
            }

            return this
        }

        fun withCardInHand(playerNumber: Int, cardName: String): ScenarioBuilder {
            val playerId = if (playerNumber == 1) player1Id else player2Id
            val cardId = createCard(cardName, playerId)
            state = state.addToZone(ZoneKey(playerId, Zone.HAND), cardId)
            return this
        }

        fun withCardsInHand(playerNumber: Int, cardName: String, count: Int): ScenarioBuilder {
            repeat(count) { withCardInHand(playerNumber, cardName) }
            return this
        }

        fun withCardOnBattlefield(
            playerNumber: Int,
            cardName: String,
            tapped: Boolean = false,
            summoningSickness: Boolean = false
        ): ScenarioBuilder {
            val playerId = if (playerNumber == 1) player1Id else player2Id
            val cardId = createCard(cardName, playerId)

            state = state.addToZone(ZoneKey(playerId, Zone.BATTLEFIELD), cardId)

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

        fun withLandsOnBattlefield(playerNumber: Int, landName: String, count: Int): ScenarioBuilder {
            repeat(count) { withCardOnBattlefield(playerNumber, landName, tapped = false) }
            return this
        }

        fun withCardInLibrary(playerNumber: Int, cardName: String): ScenarioBuilder {
            val playerId = if (playerNumber == 1) player1Id else player2Id
            val cardId = createCard(cardName, playerId)
            state = state.addToZone(ZoneKey(playerId, Zone.LIBRARY), cardId)
            return this
        }

        fun withCardInGraveyard(playerNumber: Int, cardName: String): ScenarioBuilder {
            val playerId = if (playerNumber == 1) player1Id else player2Id
            val cardId = createCard(cardName, playerId)
            state = state.addToZone(ZoneKey(playerId, Zone.GRAVEYARD), cardId)
            return this
        }

        fun withLifeTotal(playerNumber: Int, life: Int): ScenarioBuilder {
            val playerId = if (playerNumber == 1) player1Id else player2Id
            state = state.updateEntity(playerId) { container ->
                container.with(LifeTotalComponent(life))
            }
            return this
        }

        fun inPhase(phase: Phase, step: Step): ScenarioBuilder {
            state = state.copy(phase = phase, step = step)
            return this
        }

        fun withActivePlayer(playerNumber: Int): ScenarioBuilder {
            val playerId = if (playerNumber == 1) player1Id else player2Id
            state = state.copy(activePlayerId = playerId, priorityPlayerId = playerId)
            return this
        }

        /**
         * Build the scenario and connect WebSocket clients.
         * Returns a ProtocolGame for making protocol-level assertions.
         */
        suspend fun build(): ProtocolGame {
            val client1 = createClient()
            val client2 = createClient()

            // Connect both clients
            val p1Id = client1.connectAs(player1Name)
            val p2Id = client2.connectAs(player2Name)

            // Create game session
            client1.send(ClientMessage.CreateGame(mapOf("Forest" to 40)))  // Dummy deck
            eventually(5.seconds) {
                client1.messages.any { it is ServerMessage.GameCreated } shouldBe true
            }

            val sessionId = client1.messages
                .filterIsInstance<ServerMessage.GameCreated>()
                .first().sessionId

            client2.send(ClientMessage.JoinGame(sessionId, mapOf("Forest" to 40)))
            eventually(5.seconds) {
                client1.messages.any { it is ServerMessage.GameStarted } shouldBe true
                client2.messages.any { it is ServerMessage.GameStarted } shouldBe true
            }

            // Skip mulligans
            client1.send(ClientMessage.KeepHand)
            client2.send(ClientMessage.KeepHand)
            eventually(5.seconds) {
                (client1.latestState() != null) shouldBe true
            }

            // Now inject our scenario state
            // This requires accessing the game session - we'll use a test endpoint or reflection
            // For now, let's use direct state manipulation approach

            return ProtocolGame(
                sessionId = sessionId,
                player1 = PlayerContext(EntityId.of(p1Id), client1, player1Name),
                player2 = PlayerContext(EntityId.of(p2Id), client2, player2Name),
                initialState = state,
                player1Id = player1Id,
                player2Id = player2Id
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
                baseFlags = cardDef.flags,
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

    protected fun scenario(): ScenarioBuilder = ScenarioBuilder()

    // =========================================================================
    // Test Data Classes
    // =========================================================================

    data class ProtocolGame(
        val sessionId: String,
        val player1: PlayerContext,
        val player2: PlayerContext,
        val initialState: GameState,
        val player1Id: EntityId,
        val player2Id: EntityId
    ) {
        suspend fun passPriority() {
            val state = player1.client.requireLatestState()
            val priorityPlayer = if (state.priorityPlayerId == player1.id) player1 else player2
            priorityPlayer.client.submitAndWait(PassPriority(priorityPlayer.id))
        }

        suspend fun resolveStack() {
            var iterations = 0
            while (iterations++ < 20) {
                val state = player1.client.requireLatestState()
                val stack = state.zones.find { it.zoneId.zoneType == Zone.STACK }
                if (stack == null || stack.size == 0) break
                passPriority()
            }
        }

        fun findPermanent(name: String): ClientCard? {
            val state = player1.client.requireLatestState()
            val battlefield = state.zones.find { it.zoneId.zoneType == Zone.BATTLEFIELD }
                ?: return null
            return battlefield.cardIds
                .mapNotNull { state.cards[it] }
                .find { it.name == name }
        }

        fun isInGraveyard(playerNumber: Int, cardName: String): Boolean {
            val state = player1.client.requireLatestState()
            val playerId = if (playerNumber == 1) player1.id else player2.id
            val graveyard = state.zones.find {
                it.zoneId.zoneType == Zone.GRAVEYARD && it.zoneId.ownerId == playerId
            } ?: return false
            return graveyard.cardIds
                .mapNotNull { state.cards[it] }
                .any { it.name == cardName }
        }

        fun librarySize(playerNumber: Int): Int {
            val state = player1.client.requireLatestState()
            val playerId = if (playerNumber == 1) player1.id else player2.id
            return state.players.find { it.playerId == playerId }?.librarySize ?: 0
        }
    }

    data class PlayerContext(
        val id: EntityId,
        val client: TestClient,
        val name: String
    )

    // =========================================================================
    // WebSocket Test Client
    // =========================================================================

    class TestClient(
        private val json: Json,
        private val container: WebSocketContainer,
        private val url: String
    ) {
        private var session: WebSocketSession? = null
        val messages = CopyOnWriteArrayList<ServerMessage>()
        private val connectLatch = CountDownLatch(1)
        private val closed = AtomicBoolean(false)
        private val stateUpdateCounter = AtomicInteger(0)

        val isOpen: Boolean get() = session?.isOpen == true && !closed.get()

        fun stateUpdateCount(): Int = stateUpdateCounter.get()

        suspend fun connect() {
            withContext(Dispatchers.IO) {
                val client = StandardWebSocketClient(container)
                session = client.execute(
                    object : TextWebSocketHandler() {
                        override fun afterConnectionEstablished(session: WebSocketSession) {
                            connectLatch.countDown()
                        }

                        override fun handleTextMessage(session: WebSocketSession, message: TextMessage) {
                            if (closed.get()) return
                            try {
                                val serverMessage = json.decodeFromString<ServerMessage>(message.payload)
                                messages.add(serverMessage)
                                if (serverMessage is ServerMessage.StateUpdate) {
                                    stateUpdateCounter.incrementAndGet()
                                }
                            } catch (_: Exception) {}
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

        suspend fun connectAs(playerName: String): String {
            connect()
            send(ClientMessage.Connect(playerName))
            eventually(5.seconds) {
                messages.any { it is ServerMessage.Connected } shouldBe true
            }
            return messages.filterIsInstance<ServerMessage.Connected>().first().playerId
        }

        fun latestState(): ClientGameState? =
            messages.filterIsInstance<ServerMessage.StateUpdate>().lastOrNull()?.state

        fun requireLatestState(): ClientGameState =
            latestState() ?: error("No state update received")

        fun latestLegalActions(): List<LegalActionInfo> =
            messages.filterIsInstance<ServerMessage.StateUpdate>().lastOrNull()?.legalActions ?: emptyList()

        suspend fun submitAndWait(action: GameAction): ClientGameState {
            val countBefore = stateUpdateCount()
            send(ClientMessage.SubmitAction(action))
            eventually(5.seconds) {
                stateUpdateCount() shouldBeGreaterThan countBefore
            }
            return requireLatestState()
        }
    }
}
