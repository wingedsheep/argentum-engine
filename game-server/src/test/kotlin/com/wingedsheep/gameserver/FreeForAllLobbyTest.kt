package com.wingedsheep.gameserver

import com.wingedsheep.engine.core.engineSerializersModule
import com.wingedsheep.gameserver.protocol.ClientMessage
import com.wingedsheep.gameserver.protocol.ServerMessage
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
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

/**
 * Free-for-All lobby mode (multiplayer.md Phase 4), end-to-end over real WebSockets:
 * a 3-player premade-decks FFA pod plays one multiplayer game; a mid-game concede
 * continues the game 2-way (CR 800.4a); the second concede ends it; standings come back
 * as the elimination order; readying up starts a play-again game with the same pod.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class FreeForAllLobbyTest : FunSpec() {

    @LocalServerPort
    private var port: Int = 0

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        classDiscriminator = "type"
        serializersModule = engineSerializersModule
    }

    private val activeClients = mutableListOf<FfaTestClient>()
    private val activeContainers = mutableListOf<org.apache.tomcat.websocket.WsWebSocketContainer>()

    override suspend fun afterTest(testCase: io.kotest.core.test.TestCase, result: io.kotest.engine.test.TestResult) {
        activeClients.forEach { it.close() }
        activeClients.clear()
        activeContainers.forEach { runCatching { it.destroy() } }
        activeContainers.clear()
        super.afterTest(testCase, result)
    }

    private fun wsUrl(): String = "ws://localhost:$port/game"

    private fun createWsContainer() = org.apache.tomcat.websocket.WsWebSocketContainer().apply {
        defaultMaxSessionIdleTimeout = 300_000L
        defaultMaxTextMessageBufferSize = 1024 * 1024
        defaultMaxBinaryMessageBufferSize = 1024 * 1024
    }.also { activeContainers.add(it) }

    private fun createClient(): FfaTestClient {
        val client = FfaTestClient(json, createWsContainer(), wsUrl())
        activeClients.add(client)
        return client
    }

    init {
        test("3-player premade FFA pod: one game, mid-game concede continues, standings = elimination order, play again") {
            val forestDeck = mapOf("Forest" to 40)

            // ── Lobby: host creates an FFA premade lobby, two players join ──
            val alice = createClient()
            val aliceConnected = alice.connectAs("Alice")
            alice.send(ClientMessage.CreateTournamentLobby(
                setCodes = listOf("POR"),
                format = "PREMADE_DECKS",
                maxPlayers = 4,
                gameMode = "FREE_FOR_ALL",
            ))
            eventually(5.seconds) {
                alice.messages.any { it is ServerMessage.LobbyCreated } shouldBe true
            }
            val lobbyId = alice.messages.filterIsInstance<ServerMessage.LobbyCreated>().first().lobbyId

            val bob = createClient()
            val bobConnected = bob.connectAs("Bob")
            bob.send(ClientMessage.JoinLobby(lobbyId))
            val charlie = createClient()
            val charlieConnected = charlie.connectAs("Charlie")
            charlie.send(ClientMessage.JoinLobby(lobbyId))

            eventually(5.seconds) {
                alice.latestLobbyUpdate()?.players?.size shouldBe 3
                alice.latestLobbyUpdate()?.settings?.gameMode shouldBe "FREE_FOR_ALL"
            }

            // ── Decks: everyone submits a premade deck, host starts ──
            for (client in listOf(alice, bob, charlie)) {
                client.send(ClientMessage.SubmitSealedDeck(deckList = forestDeck))
            }
            eventually(5.seconds) {
                listOf(alice, bob, charlie).all { c ->
                    c.messages.any { it is ServerMessage.DeckSubmitted }
                } shouldBe true
            }
            alice.send(ClientMessage.StartTournamentLobby)

            // ── One multiplayer game seats all three players ──
            eventually(10.seconds) {
                listOf(alice, bob, charlie).all { c ->
                    c.messages.any { it is ServerMessage.FreeForAllGameStarting }
                } shouldBe true
            }
            val starting = alice.messages.filterIsInstance<ServerMessage.FreeForAllGameStarting>().first()
            starting.gameNumber shouldBe 1
            starting.players shouldHaveSize 3
            starting.players.count { it.isYou } shouldBe 1
            val gameSessionId = starting.gameSessionId

            // Every seat gets the 3-player roster and a mulligan decision.
            eventually(10.seconds) {
                listOf(alice, bob, charlie).all { c ->
                    c.messages.filterIsInstance<ServerMessage.GameStarted>().any { it.players.size == 3 } &&
                        c.messages.any { it is ServerMessage.MulliganDecision }
                } shouldBe true
            }
            for (client in listOf(alice, bob, charlie)) {
                client.send(ClientMessage.KeepHand)
            }
            eventually(10.seconds) {
                listOf(alice, bob, charlie).all { c ->
                    c.messages.any { it is ServerMessage.StateUpdate }
                } shouldBe true
            }

            // ── Mid-game concede: Charlie leaves, the game continues 2-way (CR 800.4a) ──
            charlie.send(ClientMessage.Concede)
            eventually(10.seconds) {
                charlie.messages.any { it is ServerMessage.PlayerEliminated } shouldBe true
            }
            // The remaining players see Charlie's elimination in the rebroadcast state.
            // (Routine updates arrive as deltas; request a resync to get a full state to assert on.)
            alice.send(ClientMessage.RequestResync)
            eventually(10.seconds) {
                val state = alice.latestState()
                state.shouldNotBeNull()
                state.players.first { it.playerId.value == charlieConnected.playerId }.hasLost shouldBe true
            }
            // No game over — Alice and Bob play on (CR 800.4a).
            alice.messages.none { it is ServerMessage.GameOver } shouldBe true
            bob.messages.none { it is ServerMessage.GameOver } shouldBe true

            // ── Second concede ends the game; standings are the elimination order ──
            bob.send(ClientMessage.Concede)
            eventually(10.seconds) {
                listOf(alice, bob, charlie).all { c ->
                    c.messages.any { it is ServerMessage.FreeForAllGameComplete }
                } shouldBe true
            }
            val gameOver = alice.messages.filterIsInstance<ServerMessage.GameOver>().first()
            gameOver.winnerId?.value shouldBe aliceConnected.playerId
            gameOver.gameId shouldBe gameSessionId

            val complete = alice.messages.filterIsInstance<ServerMessage.FreeForAllGameComplete>().first()
            complete.gamesPlayed shouldBe 1
            complete.standings shouldHaveSize 3
            complete.standings.map { it.placement } shouldBe listOf(1, 2, 3)
            complete.standings[0].playerId shouldBe aliceConnected.playerId   // winner
            complete.standings[1].playerId shouldBe bobConnected.playerId     // eliminated last
            complete.standings[2].playerId shouldBe charlieConnected.playerId // eliminated first

            // ── Play again: all three ready up, a second game starts with the same pod ──
            for (client in listOf(alice, bob, charlie)) {
                client.send(ClientMessage.ReadyForNextRound)
            }
            eventually(10.seconds) {
                listOf(alice, bob, charlie).all { c ->
                    c.messages.filterIsInstance<ServerMessage.FreeForAllGameStarting>()
                        .any { it.gameNumber == 2 }
                } shouldBe true
            }
            val secondGame = alice.messages.filterIsInstance<ServerMessage.FreeForAllGameStarting>()
                .first { it.gameNumber == 2 }
            secondGame.players shouldHaveSize 3
            (secondGame.gameSessionId == gameSessionId) shouldBe false
        }

        test("FFA lobby refuses AI seats and caps the pod at 6 players") {
            val host = createClient()
            host.connectAs("Host")
            host.send(ClientMessage.CreateTournamentLobby(
                setCodes = listOf("POR"),
                format = "SEALED",
                maxPlayers = 8,
                gameMode = "FREE_FOR_ALL",
            ))
            eventually(5.seconds) {
                host.messages.any { it is ServerMessage.LobbyCreated } shouldBe true
            }
            eventually(5.seconds) {
                // maxPlayers is mode-capped at 6 even though the client asked for 8
                host.latestLobbyUpdate()?.settings?.maxPlayers shouldBe 6
            }

            host.send(ClientMessage.AddAiToLobby)
            eventually(5.seconds) {
                host.messages.filterIsInstance<ServerMessage.Error>()
                    .any { it.message.contains("Free-for-All") } shouldBe true
            }
        }
    }

    // =========================================================================
    // Test client (mirrors SealedTournamentReconnectionTest's harness)
    // =========================================================================

    data class ConnectResult(val playerId: String, val token: String)

    inner class FfaTestClient(
        private val json: Json,
        private val container: jakarta.websocket.WebSocketContainer,
        private val url: String
    ) {
        private var session: WebSocketSession? = null
        val messages = CopyOnWriteArrayList<ServerMessage>()
        private val connectLatch = CountDownLatch(1)
        private val closed = AtomicBoolean(false)

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
                                messages.add(json.decodeFromString<ServerMessage>(message.payload))
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

        suspend fun connectAs(playerName: String): ConnectResult {
            connect()
            send(ClientMessage.Connect(playerName))
            eventually(5.seconds) {
                messages.any { it is ServerMessage.Connected } shouldBe true
            }
            val connected = messages.filterIsInstance<ServerMessage.Connected>().first()
            return ConnectResult(connected.playerId, connected.token)
        }

        fun latestLobbyUpdate(): ServerMessage.LobbyUpdate? =
            messages.filterIsInstance<ServerMessage.LobbyUpdate>().lastOrNull()

        fun latestState(): com.wingedsheep.engine.view.ClientGameState? =
            messages.filterIsInstance<ServerMessage.StateUpdate>().lastOrNull()?.state
    }
}
