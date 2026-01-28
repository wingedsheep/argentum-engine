package com.wingedsheep.gameserver

import com.wingedsheep.gameserver.dto.ClientGameState
import com.wingedsheep.gameserver.dto.ClientPlayer
import com.wingedsheep.gameserver.dto.ClientZone
import com.wingedsheep.gameserver.protocol.ClientMessage
import com.wingedsheep.gameserver.protocol.LegalActionInfo
import com.wingedsheep.gameserver.protocol.ServerMessage
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.engine.core.*
import com.wingedsheep.sdk.core.ZoneType
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.ints.shouldBeGreaterThan
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
abstract class GameServerTestBase : FunSpec() {

    @LocalServerPort
    protected var port: Int = 0

    protected val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        classDiscriminator = "type"
        serializersModule = engineSerializersModule
    }

    protected val activeClients = mutableListOf<TestWebSocketClient>()

    override suspend fun afterTest(testCase: io.kotest.core.test.TestCase, result: io.kotest.core.test.TestResult) {
        activeClients.forEach { it.close() }
        activeClients.clear()
        super.afterTest(testCase, result)
    }

    protected fun createWsContainer() = org.apache.tomcat.websocket.WsWebSocketContainer().apply {
        defaultMaxSessionIdleTimeout = 300_000L
        defaultMaxTextMessageBufferSize = 1024 * 1024
        defaultMaxBinaryMessageBufferSize = 1024 * 1024
    }

    protected fun createClient(): TestWebSocketClient {
        val client = TestWebSocketClient(json, createWsContainer(), wsUrl())
        activeClients.add(client)
        return client
    }

    protected fun wsUrl(): String = "ws://localhost:$port/game"

    // Fixtures
    protected val monoGreenLands = mapOf("Forest" to 40)
    protected val greenCreatures = mapOf("Forest" to 24, "Grizzly Bears" to 16)
    protected val portalDeck = mapOf(
        "Forest" to 24,
        "Grizzly Bears" to 12,
        "Elite Cat Warrior" to 4
    )

    // =========================================================================
    // TestWebSocketClient (Nested Class with Helpers)
    // =========================================================================
    class TestWebSocketClient(
        private val json: Json,
        private val container: jakarta.websocket.WebSocketContainer,
        private val url: String
    ) {
        private var session: WebSocketSession? = null
        val messages = CopyOnWriteArrayList<ServerMessage>()
        private val connectLatch = CountDownLatch(1)
        private val closed = AtomicBoolean(false)
        private val stateUpdateCounter = java.util.concurrent.atomic.AtomicInteger(0)

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
                            } catch (e: Exception) {
                                // Ignore cleanup errors
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

        // --- Public Helper Methods ---

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

        fun allStateUpdates(): List<ServerMessage.StateUpdate> =
            messages.filterIsInstance<ServerMessage.StateUpdate>()

        fun allErrors(): List<ServerMessage.Error> =
            messages.filterIsInstance<ServerMessage.Error>()

        fun latestError(): ServerMessage.Error? =
            allErrors().lastOrNull()

        fun latestMulliganDecision(): ServerMessage.MulliganDecision? =
            messages.filterIsInstance<ServerMessage.MulliganDecision>().lastOrNull()

        fun latestChooseBottomCards(): ServerMessage.ChooseBottomCards? =
            messages.filterIsInstance<ServerMessage.ChooseBottomCards>().lastOrNull()

        fun mulliganComplete(): Boolean =
            messages.any { it is ServerMessage.MulliganComplete }

        fun latestLegalActions(): List<LegalActionInfo> =
            allStateUpdates().lastOrNull()?.legalActions ?: emptyList()

        suspend fun submitAndWait(action: GameAction): ClientGameState {
            val countBefore = stateUpdateCount()
            send(ClientMessage.SubmitAction(action))
            eventually(5.seconds) {
                stateUpdateCount() shouldBeGreaterThan countBefore
            }
            return requireLatestState()
        }
    }

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

    // Helper functions for setup
    protected suspend fun setupGame(
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

        val sessionId = client1.messages.filterIsInstance<ServerMessage.GameCreated>().first().sessionId
        client2.send(ClientMessage.JoinGame(sessionId, deck))

        eventually(5.seconds) {
            client1.messages.any { it is ServerMessage.GameStarted } shouldBe true
            client2.messages.any { it is ServerMessage.GameStarted } shouldBe true
        }

        eventually(5.seconds) {
            (client1.latestMulliganDecision() != null && client2.latestMulliganDecision() != null) shouldBe true
        }

        val ctx = GameContext(
            sessionId = sessionId,
            player1 = PlayerContext(EntityId.of(player1Id), client1, player1Name),
            player2 = PlayerContext(EntityId.of(player2Id), client2, player2Name)
        )

        if (skipMulligan) {
            client1.send(ClientMessage.KeepHand)
            client2.send(ClientMessage.KeepHand)
            eventually(5.seconds) {
                (client1.latestState() != null && client2.latestState() != null) shouldBe true
            }
        }
        return ctx
    }

    protected fun GameContext.activePlayer(): PlayerContext {
        val state = player1.client.requireLatestState()
        return if (state.activePlayerId == player1.id) player1 else player2
    }

    protected fun ClientGameState.hand(playerId: EntityId): ClientZone =
        zones.find { it.zoneId.zoneType == ZoneType.HAND && it.zoneId.ownerId == playerId }
            ?: error("Hand zone for $playerId not found")

    protected fun ClientGameState.battlefield(): ClientZone =
        zones.find { it.zoneId.zoneType == ZoneType.BATTLEFIELD }
            ?: error("Battlefield zone not found")

    protected fun ClientGameState.player(playerId: EntityId): ClientPlayer =
        players.find { it.playerId == playerId } ?: error("Player $playerId not found")
}
