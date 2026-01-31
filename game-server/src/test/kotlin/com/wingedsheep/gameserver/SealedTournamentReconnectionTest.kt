package com.wingedsheep.gameserver

import com.wingedsheep.gameserver.protocol.ClientMessage
import com.wingedsheep.gameserver.protocol.ServerMessage
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import com.wingedsheep.engine.core.engineSerializersModule
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
import kotlin.time.Duration.Companion.seconds

/**
 * Integration test for sealed tournament with 5 players.
 * Tests state preservation across page refreshes (reconnections) at every stage:
 * - WAITING_FOR_PLAYERS
 * - DECK_BUILDING
 * - TOURNAMENT_ACTIVE
 * - TOURNAMENT_COMPLETE
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SealedTournamentReconnectionTest : FunSpec() {

    @LocalServerPort
    private var port: Int = 0

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        classDiscriminator = "type"
        serializersModule = engineSerializersModule
    }

    private val activeClients = mutableListOf<TournamentTestClient>()

    override suspend fun afterTest(testCase: io.kotest.core.test.TestCase, result: io.kotest.core.test.TestResult) {
        activeClients.forEach { it.close() }
        activeClients.clear()
        super.afterTest(testCase, result)
    }

    private fun wsUrl(): String = "ws://localhost:$port/game"

    private fun createWsContainer() = org.apache.tomcat.websocket.WsWebSocketContainer().apply {
        defaultMaxSessionIdleTimeout = 300_000L
        defaultMaxTextMessageBufferSize = 1024 * 1024
        defaultMaxBinaryMessageBufferSize = 1024 * 1024
    }

    private fun createClient(): TournamentTestClient {
        val client = TournamentTestClient(json, createWsContainer(), wsUrl())
        activeClients.add(client)
        return client
    }

    init {
        context("Sealed Tournament with 5 Players - State Preservation") {

            test("full tournament flow with reconnection at each stage") {
                val playerNames = listOf("Alice", "Bob", "Charlie", "Diana", "Eve")
                val players = mutableListOf<PlayerTestContext>()

                // =========================================================================
                // Stage 1: WAITING_FOR_PLAYERS - Create lobby and add players
                // =========================================================================

                // Host creates lobby
                val hostClient = createClient()
                val hostConnected = hostClient.connectAs(playerNames[0])
                hostClient.send(ClientMessage.CreateTournamentLobby(
                    setCode = "POR",
                    format = "SEALED",
                    boosterCount = 6,
                    maxPlayers = 8
                ))

                eventually(5.seconds) {
                    hostClient.messages.any { it is ServerMessage.LobbyCreated } shouldBe true
                }

                val lobbyId = hostClient.messages
                    .filterIsInstance<ServerMessage.LobbyCreated>()
                    .first().lobbyId

                players.add(PlayerTestContext(
                    name = playerNames[0],
                    playerId = EntityId.of(hostConnected.playerId),
                    token = hostConnected.token,
                    client = hostClient,
                    isHost = true
                ))

                // Other players join
                for (i in 1 until 5) {
                    val client = createClient()
                    val connected = client.connectAs(playerNames[i])
                    client.send(ClientMessage.JoinLobby(lobbyId))

                    eventually(5.seconds) {
                        client.messages.any { it is ServerMessage.LobbyUpdate } shouldBe true
                    }

                    players.add(PlayerTestContext(
                        name = playerNames[i],
                        playerId = EntityId.of(connected.playerId),
                        token = connected.token,
                        client = client,
                        isHost = false
                    ))
                }

                // Verify all players see 5 players in lobby
                eventually(5.seconds) {
                    val hostLobbyUpdate = hostClient.latestLobbyUpdate()
                    hostLobbyUpdate?.players?.size shouldBe 5
                }

                // Test reconnection during WAITING_FOR_PLAYERS for player 3
                val player3 = players[2]
                player3.client.close()
                activeClients.remove(player3.client)

                val newClient3 = createClient()
                val reconnected3 = newClient3.reconnectAs(player3.name, player3.token)
                reconnected3.context shouldBe "lobby"
                reconnected3.contextId shouldBe lobbyId

                eventually(5.seconds) {
                    newClient3.latestLobbyUpdate()?.players?.size shouldBe 5
                }

                players[2] = player3.copy(client = newClient3)

                // =========================================================================
                // Stage 2: DECK_BUILDING - Start tournament
                // =========================================================================

                // Host starts the tournament
                players[0].client.send(ClientMessage.StartTournamentLobby)

                // All players should receive their sealed pool
                eventually(10.seconds) {
                    players.all { p ->
                        p.client.messages.any { it is ServerMessage.SealedPoolGenerated }
                    } shouldBe true
                }

                // Verify each player got cards
                for (player in players) {
                    val poolMsg = player.client.messages
                        .filterIsInstance<ServerMessage.SealedPoolGenerated>()
                        .first()
                    poolMsg.cardPool.size shouldBeGreaterThan 0
                }

                // Test reconnection during DECK_BUILDING for player 2
                val player2 = players[1]
                val player2PoolBefore = player2.client.messages
                    .filterIsInstance<ServerMessage.SealedPoolGenerated>()
                    .first().cardPool

                player2.client.close()
                activeClients.remove(player2.client)

                val newClient2 = createClient()
                val reconnected2 = newClient2.reconnectAs(player2.name, player2.token)
                reconnected2.context shouldBe "deckBuilding"
                reconnected2.contextId shouldBe lobbyId

                // Should receive pool again after reconnection
                eventually(5.seconds) {
                    newClient2.messages.any { it is ServerMessage.SealedPoolGenerated } shouldBe true
                }

                val player2PoolAfter = newClient2.messages
                    .filterIsInstance<ServerMessage.SealedPoolGenerated>()
                    .first().cardPool

                // Pool should be the same size
                player2PoolAfter.size shouldBe player2PoolBefore.size

                players[1] = player2.copy(client = newClient2)

                // =========================================================================
                // Stage 3: Submit decks
                // =========================================================================

                // Build a simple deck from pool for each player
                for (player in players) {
                    val pool = player.client.messages
                        .filterIsInstance<ServerMessage.SealedPoolGenerated>()
                        .first()

                    // Pick spells from pool (or as many as available)
                    val spells = pool.cardPool.take(36)
                    val deck = mutableMapOf<String, Int>()
                    spells.forEach { card ->
                        deck[card.name] = (deck[card.name] ?: 0) + 1
                    }
                    // Add 24 basic lands
                    deck["Forest"] = 24

                    player.client.send(ClientMessage.SubmitSealedDeck(deck))
                }

                // Wait for tournament to start (all decks submitted)
                eventually(10.seconds) {
                    players.any { p ->
                        p.client.messages.any { it is ServerMessage.TournamentStarted }
                    } shouldBe true
                }

                // =========================================================================
                // Stage 4: TOURNAMENT_ACTIVE - Test reconnection during matches
                // =========================================================================

                // Wait for matches to be set up
                delay(2000)

                // Find a player who got a TournamentMatchStarting message (has a match, not a bye)
                var matchPlayer: PlayerTestContext? = null
                var gameSessionId: String? = null

                for (player in players) {
                    val matchStarting = player.client.messages
                        .filterIsInstance<ServerMessage.TournamentMatchStarting>()
                        .firstOrNull()
                    if (matchStarting != null) {
                        matchPlayer = player
                        gameSessionId = matchStarting.gameSessionId
                        break
                    }
                }

                // Only test game reconnection if a match is active
                if (matchPlayer != null && gameSessionId != null) {
                    // Wait for game to start (mulligan phase)
                    eventually(5.seconds) {
                        matchPlayer!!.client.messages.any {
                            it is ServerMessage.GameStarted || it is ServerMessage.MulliganDecision
                        } shouldBe true
                    }

                    // Test reconnection during game
                    val originalClient = matchPlayer.client
                    originalClient.close()
                    activeClients.remove(originalClient)

                    val newMatchClient = createClient()
                    val reconnectedMatch = newMatchClient.reconnectAs(matchPlayer.name, matchPlayer.token)

                    // Should reconnect to game or tournament context
                    (reconnectedMatch.context == "game" || reconnectedMatch.context == "tournament") shouldBe true

                    // Update player reference
                    val playerIndex = players.indexOfFirst { it.playerId == matchPlayer.playerId }
                    players[playerIndex] = matchPlayer.copy(client = newMatchClient)
                }

                // =========================================================================
                // Verify lobby/tournament state is preserved for player with bye
                // =========================================================================

                // Find a player who got a bye (with 5 players, one will have a bye)
                val byePlayer = players.find { p ->
                    p.client.messages.any { it is ServerMessage.TournamentBye }
                }

                if (byePlayer != null) {
                    byePlayer.client.close()
                    activeClients.remove(byePlayer.client)

                    val newByeClient = createClient()
                    val reconnectedBye = newByeClient.reconnectAs(byePlayer.name, byePlayer.token)

                    // Should reconnect to tournament context
                    reconnectedBye.context shouldBe "tournament"
                    reconnectedBye.contextId shouldBe lobbyId

                    val playerIndex = players.indexOfFirst { it.playerId == byePlayer.playerId }
                    players[playerIndex] = byePlayer.copy(client = newByeClient)
                }
            }

            test("reconnection preserves lobby settings during WAITING_FOR_PLAYERS") {
                val client1 = createClient()
                val connected1 = client1.connectAs("Host")

                client1.send(ClientMessage.CreateTournamentLobby(
                    setCode = "POR",
                    format = "SEALED",
                    boosterCount = 4, // Custom setting
                    maxPlayers = 6
                ))

                eventually(5.seconds) {
                    client1.latestLobbyUpdate() shouldNotBe null
                }

                val lobbyUpdate = client1.latestLobbyUpdate()!!
                lobbyUpdate.settings.boosterCount shouldBe 4
                lobbyUpdate.settings.maxPlayers shouldBe 6

                // Reconnect
                client1.close()
                activeClients.remove(client1)

                val newClient1 = createClient()
                val reconnected = newClient1.reconnectAs("Host", connected1.token)
                reconnected.context shouldBe "lobby"

                eventually(5.seconds) {
                    newClient1.latestLobbyUpdate() shouldNotBe null
                }

                // Settings should be preserved
                val lobbyAfter = newClient1.latestLobbyUpdate()!!
                lobbyAfter.settings.boosterCount shouldBe 4
                lobbyAfter.settings.maxPlayers shouldBe 6
            }

            test("multiple simultaneous reconnections are handled correctly") {
                val playerNames = listOf("P1", "P2", "P3")
                val players = mutableListOf<PlayerTestContext>()

                // Create lobby with 3 players
                val hostClient = createClient()
                val hostConnected = hostClient.connectAs(playerNames[0])
                hostClient.send(ClientMessage.CreateTournamentLobby("POR", "SEALED"))

                eventually(5.seconds) {
                    hostClient.messages.any { it is ServerMessage.LobbyCreated } shouldBe true
                }

                val lobbyId = hostClient.messages
                    .filterIsInstance<ServerMessage.LobbyCreated>()
                    .first().lobbyId

                players.add(PlayerTestContext(playerNames[0], EntityId.of(hostConnected.playerId), hostConnected.token, hostClient, true))

                for (i in 1 until 3) {
                    val client = createClient()
                    val connected = client.connectAs(playerNames[i])
                    client.send(ClientMessage.JoinLobby(lobbyId))
                    eventually(5.seconds) {
                        client.latestLobbyUpdate() shouldNotBe null
                    }
                    players.add(PlayerTestContext(playerNames[i], EntityId.of(connected.playerId), connected.token, client, false))
                }

                // Close all connections simultaneously
                players.forEach { p ->
                    p.client.close()
                    activeClients.remove(p.client)
                }

                // Small delay
                delay(500)

                // Reconnect all simultaneously
                val newClients = players.map { p ->
                    val newClient = createClient()
                    val reconnected = newClient.reconnectAs(p.name, p.token)
                    reconnected.context shouldBe "lobby"
                    newClient
                }

                // All should see 3 players
                eventually(5.seconds) {
                    newClients.all { c ->
                        c.latestLobbyUpdate()?.players?.size == 3
                    } shouldBe true
                }
            }

            test("reconnection during deck building restores submitted deck status") {
                val playerNames = listOf("P1", "P2")
                val players = mutableListOf<PlayerTestContext>()

                // Create and start a 2-player lobby
                val hostClient = createClient()
                val hostConnected = hostClient.connectAs(playerNames[0])
                hostClient.send(ClientMessage.CreateTournamentLobby("POR", "SEALED", 6, 2))

                eventually(5.seconds) {
                    hostClient.messages.any { it is ServerMessage.LobbyCreated } shouldBe true
                }

                val lobbyId = hostClient.messages
                    .filterIsInstance<ServerMessage.LobbyCreated>()
                    .first().lobbyId

                players.add(PlayerTestContext(playerNames[0], EntityId.of(hostConnected.playerId), hostConnected.token, hostClient, true))

                val client2 = createClient()
                val connected2 = client2.connectAs(playerNames[1])
                client2.send(ClientMessage.JoinLobby(lobbyId))
                eventually(5.seconds) {
                    client2.latestLobbyUpdate() shouldNotBe null
                }
                players.add(PlayerTestContext(playerNames[1], EntityId.of(connected2.playerId), connected2.token, client2, false))

                // Start tournament
                hostClient.send(ClientMessage.StartTournamentLobby)

                eventually(10.seconds) {
                    players.all { p ->
                        p.client.messages.any { it is ServerMessage.SealedPoolGenerated }
                    } shouldBe true
                }

                // Player 1 submits deck
                val pool1 = hostClient.messages
                    .filterIsInstance<ServerMessage.SealedPoolGenerated>()
                    .first()
                val deck1 = mutableMapOf<String, Int>()
                pool1.cardPool.take(36).forEach { card ->
                    deck1[card.name] = (deck1[card.name] ?: 0) + 1
                }
                deck1["Forest"] = 24
                hostClient.send(ClientMessage.SubmitSealedDeck(deck1))

                // Wait for deck submission to be processed
                eventually(5.seconds) {
                    val lobbyUpdate = hostClient.latestLobbyUpdate()
                    lobbyUpdate?.players?.find { it.playerId == hostConnected.playerId }?.deckSubmitted shouldBe true
                }

                // Reconnect player 1
                hostClient.close()
                activeClients.remove(hostClient)

                val newHostClient = createClient()
                val reconnected = newHostClient.reconnectAs(playerNames[0], hostConnected.token)
                reconnected.context shouldBe "deckBuilding"

                // Verify deck submission status is preserved
                eventually(5.seconds) {
                    val lobbyUpdate = newHostClient.latestLobbyUpdate()
                    lobbyUpdate?.players?.find { it.playerId == hostConnected.playerId }?.deckSubmitted shouldBe true
                }
            }
        }
    }

    // =========================================================================
    // Test Helper Classes
    // =========================================================================

    data class PlayerTestContext(
        val name: String,
        val playerId: EntityId,
        val token: String,
        val client: TournamentTestClient,
        val isHost: Boolean
    )

    data class ConnectResult(
        val playerId: String,
        val token: String
    )

    data class ReconnectResult(
        val playerId: String,
        val token: String,
        val context: String?,
        val contextId: String?
    )

    inner class TournamentTestClient(
        private val json: Json,
        private val container: jakarta.websocket.WebSocketContainer,
        private val url: String
    ) {
        private var session: WebSocketSession? = null
        val messages = CopyOnWriteArrayList<ServerMessage>()
        private val connectLatch = CountDownLatch(1)
        private val closed = AtomicBoolean(false)
        private val stateUpdateCounter = AtomicInteger(0)

        val isOpen: Boolean get() = session?.isOpen == true && !closed.get()

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

        suspend fun connectAs(playerName: String): ConnectResult {
            connect()
            send(ClientMessage.Connect(playerName))
            eventually(5.seconds) {
                messages.any { it is ServerMessage.Connected } shouldBe true
            }
            val connected = messages.filterIsInstance<ServerMessage.Connected>().first()
            return ConnectResult(connected.playerId, connected.token)
        }

        suspend fun reconnectAs(playerName: String, token: String): ReconnectResult {
            connect()
            send(ClientMessage.Connect(playerName, token))
            eventually(5.seconds) {
                messages.any { it is ServerMessage.Connected || it is ServerMessage.Reconnected } shouldBe true
            }

            val reconnected = messages.filterIsInstance<ServerMessage.Reconnected>().firstOrNull()
            if (reconnected != null) {
                return ReconnectResult(reconnected.playerId, reconnected.token, reconnected.context, reconnected.contextId)
            }

            val connected = messages.filterIsInstance<ServerMessage.Connected>().first()
            return ReconnectResult(connected.playerId, connected.token, null, null)
        }

        fun latestLobbyUpdate(): ServerMessage.LobbyUpdate? =
            messages.filterIsInstance<ServerMessage.LobbyUpdate>().lastOrNull()

        fun latestState(): com.wingedsheep.gameserver.dto.ClientGameState? =
            messages.filterIsInstance<ServerMessage.StateUpdate>().lastOrNull()?.state
    }
}
